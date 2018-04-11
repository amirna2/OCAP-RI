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


#include <mpe_ed.h>
#include <mpe_os.h>
#include <mpe_dbg.h>
#include <mpe_types.h>
#include <mpe_pod.h>
#include <mpe_types.h>
#include <mpe_pod.h>
#include <mpe_frontpanel.h>
#include <mpeos_pod.h>
#include <mpeos_dbg.h>
#include <mpeos_util.h>
#include <mpeos_mem.h>
#include <mpeos_thread.h>
#include <mpeos_media.h>
#include <mpeos_frontpanel.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include <podmgr.h>
#include "pod_util.h"
#include "simgr.h"

extern mpe_PODDatabase podDB;
extern uint32_t casSessionId;
extern uint16_t casResourceVersion;
extern mpe_Error podmgrCreateCAPMT_APDU(mpe_SiServiceHandle service_handle,
        uint8_t programIdx, uint8_t transactionId, uint8_t ltsid,
        uint16_t ca_system_id,
        uint8_t ca_pmt_cmd_id,
        uint8_t **apdu_buffer,
        uint32_t *apdu_buffer_size);
extern mpe_Bool mpe_siCheckCADescriptors(mpe_SiServiceHandle service_handle, uint16_t ca_system_id,
        uint32_t *numStreams, uint16_t *ecmPid, mpe_PODStreamDecryptInfo streamInfo[]);
extern mpe_Error mpe_siFindPidInPMT(mpe_SiServiceHandle service_handle, int pid);

/* local globals */
static mpe_ThreadId podThreadId = NULL;
static mpe_EventQueue podMpeosEvQ = 0;
static char *podMpeosEvQName = "POD_MPEOS_MPE_EvQ";
static mpe_EdEventInfo *podJniPodEventEdHandle = NULL;
static uint16_t ca_system_id = 0;
static uint8_t g_ltsid = 0;

/* logging text */
#define PODMODULE "<<POD_IMPL>>"
/* detailed logging info */
#define PODIMPL_DETAILED_DEBUG  (1)

/* front panel debugging output */
//#define PODIMPL_FRONTPANEL_DEBUG 1

#ifdef PODIMPL_FRONTPANEL_DEBUG
static char fp_debug_lines[10][5] = {" POD","","","","","","","","",""};
#endif

#define      ACTIVE_DECRYPT_STATE(state)        ((state == MPE_POD_DECRYPT_STATE_ISSUED_QUERY) \
                                              || (state == MPE_POD_DECRYPT_STATE_ISSUED_MMI) \
                                              || (state == MPE_POD_DECRYPT_STATE_DESCRAMBLING))

static mpe_Mutex table_mutex; /* Mutex for synchronous access */
typedef LINKHD * ListPIDs;

typedef struct
{
    uint8_t transactionId;
    uint8_t ltsid;
    uint16_t programNum;
    uint16_t sourceId;
    uint8_t ca_pmt_cmd_id;
    mpe_PODDecryptState state;
    uint8_t priority;
    ListPIDs authorizedPids;
    mpe_PodDecryptSessionEvent lastEvent;
    uint16_t ecmPid;
    mpe_PODCPSession cpSession;
    uint8_t tunerId;
} mpe_ProgramIndexTableRow;

typedef struct
{
    uint32_t transportStreamsUsed;
    uint32_t programsUsed;
    uint32_t elementaryStreamsUsed;
    uint32_t numRows;
    mpe_ProgramIndexTableRow* rows; /* array of rows */
} mpe_ProgramIndexTableStruct;

static mpe_ProgramIndexTableStruct programIndexTable = { 0, 0, 0, 0, NULL };

typedef struct
{
    uint8_t programIdx;
    mpe_SiServiceHandle serviceHandle;
    uint8_t tunerId;
    uint8_t priority;
    uint16_t sourceId;
    mpe_PODDecryptRequestState state;
    uint8_t ca_pmt_cmd_id;
    mpe_Bool mmiEnable;
    mpe_Cond requestCondition;
    uint32_t numPids;
    mpe_MediaPID *pids;
    mpe_EventQueue eventQ; /* queue to communicate session events to higher levels */
    void* act; /* edHandle or user data, always goes into optionaldata2 field of queue send */
    uint8_t ltsId;
} mpe_RequestTableRow;

typedef LINKHD * ListRequests;
static ListRequests requestTable;

typedef struct _QueueEntry
{
    mpe_EventQueue m_queue;
    struct _QueueEntry* next;
} QueueEntry;

static QueueEntry* podEventQueueList;
static mpe_Mutex podEventQueueListMutex;

/* forward references */
static void podEventHandlerThread(void *data);

#ifdef PODIMPL_FRONTPANEL_DEBUG
static void podFrontPanelDebugThread(void *data);
static mpe_ThreadId podFrontPanelDebugThreadId = NULL;
#endif

#define    VALID_PROGRAM_INDEX(index)    ((index >= 0) \
                                           && (index <= programIndexTable.numRows))
#define    LTSID_UNDEFINED  0
static mpe_Error initProgramIndexTable(void);
static void initRequestTable(void);

static mpe_Error sendPodEvent(mpe_Event nextEventId, void* edEventData1,
        uint32_t edEventData3);

static mpe_Error sendJniDecryptSessionEvent(mpe_PodDecryptSessionEvent event,
        mpe_PODDecryptSessionHandle sessionHandlePtr, uint32_t edEventData3);

static mpe_Error releaseDecryptRequest(mpe_RequestTableRow* requestPtr);

static mpe_Bool checkAvailableResources(uint8_t tunerId, uint8_t numStreams);
static mpe_Bool tunerInUse(uint8_t tunerId);
static mpe_Error releaseProgramIndexRow(uint8_t programIdx);

//static mpe_Error getActiveRequestTableRowForTunerIdProgramNumber(uint32_t tunerId, uint16_t programNumber, mpe_RequestTableRow **rowPtr);
//static mpe_RequestTableRow* getDecryptRequestPtrFromHandle(mpe_PODDecryptSessionHandle handle);
//static mpe_PODDecryptSessionHandle getDecryptRequestHandleFromPtr(mpe_RequestTableRow* requestPtr);

static void removeAllDecryptSessionsAndResources(mpe_PodDecryptSessionEvent event);

static int getNextAvailableProgramIndexTableRow(void);
//static mpe_Error setupDecryptSession();
static void commitProgramIndexTableRow(uint8_t programIdx, uint8_t ca_pmt_cmd_id,
                                uint16_t progNum, uint16_t sourceId,
                                uint8_t transactionId, uint8_t tunerId, uint8_t ltsId,
                                uint8_t priority, uint8_t numStreams, uint16_t ecmPid,
                                mpe_PODStreamDecryptInfo streamInfo[], uint8_t state);
static uint8_t getLtsid(void);
static mpe_Bool isLtsidInUse(uint8_t ltsid);
static uint8_t getLtsidForTunerId(uint8_t tuner);
static uint8_t getTransactionIdForProgramIndex(uint8_t programIdx);
static uint8_t getNextTransactionIdForProgramIndex(uint8_t programIdx);
static mpe_Bool isProgramIndexInUse(uint8_t programIdx);
static mpe_Error getDecryptRequestProgramIndex(mpe_PODDecryptSessionHandle handle, uint8_t* programIdxPtr);
static mpe_Error setPidArrayForProgramIndex(uint8_t programIdx, uint16_t numPids, uint16_t elemStreamPidArray[], uint8_t caEnableArray[]);
static mpe_PodDecryptSessionEvent getLastEventForProgramIndex(uint8_t programIndex);
static void updateProgramIndexTablePriority(uint8_t programIndex, uint8_t priority);
static void updatePriority(uint8_t programIndex);
static void logProgramIndexTable(char* localStr);
static char* programIndexTableStateString(uint8_t state);
static mpe_Error findNextSuspendedRequest(uint8_t *tunerId, mpe_SiServiceHandle  *handle);
static mpe_Error findActiveDecryptRequestForReuse(uint8_t tunerId, uint16_t programNumber, int *index);
static void addRequestToTable(mpe_RequestTableRow* newRequest);
static void removeRequestFromTable(mpe_RequestTableRow* newRequest);
static mpe_Bool findRequest(mpe_PODDecryptSessionHandle handle);
//static mpe_Error getDecryptSessionHandleForProgramIndex(uint8_t programIdx, mpe_PODDecryptSessionHandle *handle);
static mpe_Error getDecryptSessionsForProgramIndex(uint8_t programIdx, uint8_t *numHandles, mpe_PODDecryptSessionHandle handle[]);
static mpe_Error updatePODDecryptRequestAndResource(uint8_t programIdx,
        uint8_t transactionId, uint16_t numPids, uint16_t elemStreamPidArray[],
        uint8_t caEnableArray[]);
static void logRequestTable(char* localStr);
static char* requestTableStateString(uint8_t state);
static mpe_Error createAndSendCaPMTApdu(uint32_t serviceHandle, uint8_t programIdx,
                                uint8_t transactionId, uint8_t ltsid,
                                uint8_t ca_cmd_id);
static void sendEvent(mpe_PodDecryptSessionEvent event, uint8_t programIdx);
static mpe_Error findRequestProgramIndexWithLowerPriority(mpe_RequestTableRow* requestPtr, int *programIdx);
static void activateSuspendedRequests(uint8_t programIdx);
static uint8_t getSuspendedRequests(void);
static uint8_t caCommandToState(uint8_t cmd_id);
static uint16_t getCASystemIdFromCaReply(uint8_t* apdu);
static uint32_t getApduTag(uint8_t* apdu);
static mpe_Bool isCaInfoReplyAPDU(uint32_t sessionId, uint8_t* apdu);
static mpe_Bool isCaReplyAPDU(uint32_t sessionId, uint8_t* apdu);
static mpe_Bool isCaUpdateAPDU(uint32_t sessionId, uint8_t* apdu);
static uint16_t getApduDataOffset(uint8_t* apdu);
static int32_t getApduDataLen(uint8_t* apdu);

static char* caCommandString(uint8_t ca_pmt_cmd);

/* raw data, needs to be parsed to determine if there is valid data in byte */
static uint8_t getOuterCaEnableFromCaReply(uint8_t* apdu);
static void getInnerCaEnablesFromCaReply(uint8_t* apdu, uint16_t* numPids,
                                        uint16_t elemStreamPidArray[], uint8_t caEnableArray[]);
static uint8_t getProgramIdxFromCaReply(uint8_t* apdu);
static uint8_t getTransactionIdFromCaReply(uint8_t* apdu);

static uint8_t getProgramIdxFromCaUpdate(uint8_t* apdu);
static uint8_t getTransactionIdFromCaUpdate(uint8_t* apdu);
/* raw data, needs to be parsed to determine if there is valid data in byte */
static uint8_t getOuterCaEnableFromCaUpdate(uint8_t* apdu);
static void getInnerCaEnablesFromCaUpdate(uint8_t* apdu, uint16_t* numPids,
        uint16_t elemStreamPidArray[], uint8_t caEnableArray[]);

static mpe_Bool isValidReply(uint8_t programIdx, uint8_t transactionId);
static mpe_Bool isValidUpdate(uint8_t programIdx, uint8_t transactionId);

static void shutdownMPELevelRegisteredPodEvents();

static void notifyMPELevelRegisteredPodEventQueues(mpe_Event eventId,
        void *optionalEventData1, void *optionalEventData2, uint32_t eventFlag);

static mpe_Error suspendActiveCASessions(void);
static mpe_Error activateSuspendedCASessions(void);
#define DEFAULT_HEXDUMP_LINE_BUFFER_SIZE 128
static uint32_t g_mpe_ca_enable = 0;
static uint32_t g_mpe_ca_retry_count = 0;
static uint32_t g_mpe_ca_retry_timeout = 0;

static void hexDump(uint8_t *preamble, uint8_t *buffer, uint32_t buffer_length)
{
    uint8_t byLineBuf[DEFAULT_HEXDUMP_LINE_BUFFER_SIZE];

    size_t uiInputBufIndex = 0;
    size_t uiLineBufIndex;

    int iCharacter;

    while (uiInputBufIndex < buffer_length)
    {
        memset(byLineBuf, ' ', DEFAULT_HEXDUMP_LINE_BUFFER_SIZE );
        byLineBuf[DEFAULT_HEXDUMP_LINE_BUFFER_SIZE - 1] = 0;

        for (uiLineBufIndex = 0; (uiLineBufIndex < 16) && (uiInputBufIndex
                + uiLineBufIndex) < buffer_length; uiLineBufIndex++)
        {
            snprintf((char *) &byLineBuf[uiLineBufIndex * 3], 4, "%02x ",
                    buffer[uiLineBufIndex + uiInputBufIndex]);

            iCharacter = buffer[uiLineBufIndex + uiInputBufIndex];
            if (iCharacter < ' ' || iCharacter > '~')
                iCharacter = '.';

            snprintf((char *) &byLineBuf[(16 * 3) + uiLineBufIndex + 1], 2,
                    "%c", iCharacter);
        }

        byLineBuf[uiLineBufIndex * 3] = ' ';
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: [%06d]: %s\n", preamble,
                uiInputBufIndex, byLineBuf);

        uiInputBufIndex += uiLineBufIndex;
    }
}

mpe_Error mpe_podImplInit()
{
    mpe_Error retCode = MPE_SUCCESS;
    const char *pod_mpe_ca_enable = NULL;
    const char *pod_mpe_ca_retry_count = 0;
    const char *pod_mpe_ca_retry_timeout = 0;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: mpe_podImplInit\n", PODMODULE);

    // Create the global mutex
    mpeos_mutexNew(&table_mutex);

    // Init the structures
    podEventQueueList = NULL;
    mpeos_mutexNew(&podEventQueueListMutex);

    if ((retCode = mpe_threadCreate(podEventHandlerThread, NULL,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &podThreadId,
            "mpe_podThread")) != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_POD,
                "%s: mpe_podImplInit: failed to create pod worker thread (err=%d).\n",
                PODMODULE, retCode);
    }

#ifdef PODIMPL_FRONTPANEL_DEBUG
    if ( ( retCode = mpe_threadCreate( podFrontPanelDebugThread, NULL,
                                       MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE,
                                       &podFrontPanelDebugThreadId,
                                       "mpe_podFrontPanelDebugThread" ) )
         != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_POD,
                "%s: mpe_podImplInit: failed to create front panel debug thread (err=%d).\n",
                PODMODULE, retCode);
    }
#endif

    pod_mpe_ca_enable = mpeos_envGet("POD.MPE.CA.ENABLE");
    if ((pod_mpe_ca_enable != NULL) && (stricmp(pod_mpe_ca_enable, "TRUE") == 0))
    {
        g_mpe_ca_enable = TRUE;
    }
    else
    {
    	// Default value is FALSE
        // If MPE CA management is disabled the stack does not
        // send/process APDUs on the CAS session.
        g_mpe_ca_enable = FALSE;
    }
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "<%s::mpe_podImplInit> - MPE CA management is %s\n", PODMODULE,
            (g_mpe_ca_enable ? "ON" : "OFF"));

    pod_mpe_ca_retry_count = mpeos_envGet("POD.MPE.CA.APDU.SEND.RETRY.COUNT");
    if (pod_mpe_ca_retry_count != NULL)
    {
        g_mpe_ca_retry_count = atoi(pod_mpe_ca_retry_count);
    }
    else
    {
        // Default is 8
        g_mpe_ca_retry_count = 8;
    }

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "<%s::mpe_podImplInit> - MPE CA APDU retry count is %d\n", PODMODULE,
            g_mpe_ca_retry_count);

    pod_mpe_ca_retry_timeout = mpeos_envGet("POD.MPE.CA.APDU.SEND.RETRY.TIMEOUT");
    if (pod_mpe_ca_retry_timeout != NULL)
    {
        g_mpe_ca_retry_timeout = atoi(pod_mpe_ca_retry_timeout);
    }
    else
    {
        // Default is 4 sec
        g_mpe_ca_retry_timeout = 4000;
    }

    // Initialize the decrypt request table
    initRequestTable();

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "<%s::mpe_podImplInit> - MPE CA APDU retry timeout is %d\n", PODMODULE,
            g_mpe_ca_retry_timeout);

    /*
     * Seed for the random ltsid
     */
    srand((unsigned) time(NULL));

    g_ltsid = (uint8_t) rand();

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "<%s::mpe_podImplInit> - g_ltsid: %d\n", PODMODULE,
            g_ltsid);
    return retCode;
}

mpe_Error mpe_podImplRegister(mpe_EdEventInfo* jniEdHandle)
{
    mpe_Error retCode = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "%s: mpe_podImplRegister, jniEdHandle = %p\n", PODMODULE,
            jniEdHandle);

    /* if there is an existing EdListener, unregister it */
    if (podJniPodEventEdHandle != NULL)
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "%s: mpe_podImplRegister, overriding existing eventQ \n",
                PODMODULE);

        if ((retCode = mpe_podImplUnregister()) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "%s: mpe_podImplRegister, failure to Unregister existing queues \n",
                    PODMODULE);
            return retCode;
        }
    }

    podJniPodEventEdHandle = jniEdHandle;

    return MPE_SUCCESS;
}

mpe_Error mpe_podImplUnregister(void)
{
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: mpe_podImplUnregister \n",
            PODMODULE);

    if (podJniPodEventEdHandle != NULL)
    {
        /* remove the existing JNI listener */
        /* send off a 'last event' event to the JNI listener to clean things up */
        /* if (mpe_eventQueueSend(podJniEdHandle->eventQ, MPE_POD_EVENT_SHUTDOWN, NULL, podJniEvQAct, 0) != MPE_SUCCESS)  */
        if (sendPodEvent(MPE_POD_EVENT_SHUTDOWN, NULL, 0) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "%s: mpe_podImplUnregister sending LASTEVENT to JNI listener - edHandle(0x%p)\n",
                    PODMODULE, podJniPodEventEdHandle);
        }

        /* clear out saved info */
        podJniPodEventEdHandle = NULL;
        podThreadId = 0;
    }

    if (podMpeosEvQ != 0)
    {
        /* now shutdown the mpeos queue via the worker thread and close down the worker thread */
        if (mpe_eventQueueSend(podMpeosEvQ, MPE_POD_EVENT_SHUTDOWN, NULL, NULL,
                0) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "%s: podWorkerThread,  mpe_eventQueueSend to mpeos queue failed\n",
                    PODMODULE);
        }
    }

    shutdownMPELevelRegisteredPodEvents();
    return MPE_SUCCESS;
}

mpe_Error mpe_podImplReceiveAPDU(uint32_t *sessionId, uint8_t **apdu,
        int32_t *len)
{
    mpe_Error retCode;
    int8_t outerCaEnable;

    /* these two together make up the unique identifier for the originating message */
    int8_t programIdx;
    int8_t transactionId;
    mpe_Bool internallyProcessedAPDU = FALSE;

    do
    {
        retCode = mpeos_podReceiveAPDU(sessionId, apdu, len);

        if(retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU returned error = %d\n",
                PODMODULE, retCode);
            mpeos_threadSleep(200, 0);
            continue;
        }
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: mpe_podImplReceiveAPDU sessionId=0x%x apdu=0x%p, len=%d\n",
                PODMODULE, *sessionId, apdu, *len);
        /*
        for(i=0;i<10;i++)
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "%s: mpe_podImplReceiveAPDU apdu[%d]=0x%02x \n", PODMODULE, i, (*apdu)[i]);
        }
        */

        /* CA APDUs do not need to be propagated upto Java layer */
        if (*sessionId == casSessionId)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                    "%s: mpe_podImplReceiveAPDU received CA APDU \n", PODMODULE);

            internallyProcessedAPDU = TRUE;

            if(g_mpe_ca_enable == FALSE)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU CA APDU processing is OFF...\n",
                        PODMODULE);

                (void)mpe_podReleaseAPDU(*apdu);
                continue;
            }
            else
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU CA APDU processing is ON, continuing...\n",
                        PODMODULE);
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU not CA-PMT apdu...\n",
                    PODMODULE);

            internallyProcessedAPDU = FALSE;

            continue;
        }

        // Assert: CA_PMT processing is ON and we're processing a CAS APDU

        /* Acquire mutex */
        if (mpe_mutexAcquire(table_mutex) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: mpe_podImplReceiveAPDU error acquiring table mutex..\n", PODMODULE);
        }

        // is this a ca_info_reply message?
        if (isCaInfoReplyAPDU(*sessionId, *apdu))
        {
            // This is in response to the ca_info_inquiry apdu
            // Extract the CA_System_id (2 bytes after the tag and length fields)
            ca_system_id = getCASystemIdFromCaReply(*apdu);

            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU ca_system_id=%d\n",
                    PODMODULE, ca_system_id);
        }
        // is this a ca_reply message? (ca_pmt reply)
        else if (isCaReplyAPDU(*sessionId, *apdu))
        {
            // This is in response to ca_pmt apdu
            // with ca_cmd_id = 0x03 (query)

            // Use random size arrays for elementary streams
            uint8_t innerCaEnableArray[512];
            uint16_t elemStreamPidArray[512];
            uint16_t numPids = 0;

            programIdx = getProgramIdxFromCaReply(*apdu);
            transactionId = getTransactionIdFromCaReply(*apdu);

            // Check if the reply is valid
            if(!isValidReply(programIdx, transactionId))
            {
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU received CA_PMT_REPLY, INVALID programIdx:%d, or transactionId:%d .\n",
                        PODMODULE, programIdx, transactionId);
                goto doneProcessingAPDU;
            }

            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU received CA_PMT_REPLY, programIdx:%d, transactionId:%d.\n",
                    PODMODULE, programIdx, transactionId);

            outerCaEnable = getOuterCaEnableFromCaReply(*apdu);
            if ((outerCaEnable & CA_ENABLE_SET) > 0)
            {
                uint8_t caEnableArray[] = { outerCaEnable & 0x7f };

                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU CA_PMT_REPLY, outerCaEnable:0x%02x, caEnable(program level):0x%x.\n",
                        PODMODULE, outerCaEnable, caEnableArray[0]);

                updatePODDecryptRequestAndResource(programIdx, transactionId, 0,
                        NULL, caEnableArray);
            }

            // Both outer code and for inner codes can exist
            getInnerCaEnablesFromCaReply(*apdu, &numPids, elemStreamPidArray,
                    innerCaEnableArray);
            if(numPids != 0)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU CA_PMT_REPLY, getInnerCaEnablesFromCaReply returned numPids:%d\n",
                        PODMODULE, numPids);

                updatePODDecryptRequestAndResource(programIdx, transactionId,
                        numPids, elemStreamPidArray, innerCaEnableArray);
            }
        }
        // is this a ca_update message? (ca_pmt update)
        else if (isCaUpdateAPDU(*sessionId, *apdu))
        {
            // use a large # for array size.
            uint8_t caEnableArray[512];
            uint16_t elemStreamPidArray[512];
            uint16_t numPids = 0;

            programIdx = getProgramIdxFromCaUpdate(*apdu);
            transactionId = getTransactionIdFromCaUpdate(*apdu);

            // Check if the update is valid
            if(!isValidUpdate(programIdx, transactionId))
            {
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU received CA_PMT_UPDATE, INVALID programIdx:%d, or transactionId:%d .\n",
                        PODMODULE, programIdx, transactionId);
                goto doneProcessingAPDU;
            }

            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU received CA_PMT_UPDATE programIdx:%d, transactionId:%d.\n",
                    PODMODULE, programIdx, transactionId);

            // if so, is it a success or failure message
            // get outer code if it exists
            outerCaEnable = getOuterCaEnableFromCaUpdate(*apdu);
            if ((outerCaEnable & CA_ENABLE_SET) > 0)
            {
                uint8_t caEnableArray[] = { outerCaEnable & 0x7f };

                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU CA_PMT_UPDATE, outerCaEnable:0x%02x, caEnable(program level):0x%x.\n",
                        PODMODULE, outerCaEnable, caEnableArray[0]);

                updatePODDecryptRequestAndResource(programIdx, transactionId, 0,
                        NULL, caEnableArray);
            }

            // Both inner and outer level authorizations may exist
            // Stream level authorization over-rides the program level
            // authorization
            getInnerCaEnablesFromCaUpdate(*apdu, &numPids, elemStreamPidArray,
                    caEnableArray);

            if(numPids != 0)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU CA_PMT_UPDATE, getInnerCaEnablesFromCaUpdate returned numPids:%d\n",
                        PODMODULE, numPids);

                updatePODDecryptRequestAndResource(programIdx, transactionId,
                        numPids, elemStreamPidArray, caEnableArray);
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: mpe_podImplReceiveAPDU not CA-PMT apdu...\n",
                    PODMODULE);
        }

        doneProcessingAPDU:

        mpe_mutexRelease(table_mutex);
        /* Release mutex */

        /* CAS APDUs do not need to be propagated up to Java layer - just release and read again */
        (void)mpe_podReleaseAPDU(*apdu);

        logProgramIndexTable("ReceiveAPDU");
        logRequestTable("ReceiveAPDU");

        internallyProcessedAPDU = TRUE;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: mpe_podImplReceiveAPDU completed processing CA APDU. Issuing new read...\n", PODMODULE);
    } while (internallyProcessedAPDU);

    return retCode;
} // END mpe_podImplReceiveAPDU()

mpe_Error mpe_podImplStartDecrypt(
		mpe_PodDecryptRequestParams *decryptRequestPtr,
        mpe_EventQueue queueId, void *act,
        mpe_PODDecryptSessionHandle *sessionHandlePtr)
{
    if(g_mpe_ca_enable == FALSE)
    {
        // If MPE CA management is disabled the stack does not
        // send/process APDUs on the CAS session.

        *sessionHandlePtr = NULL;
        return MPE_SUCCESS;
    }
    else
    {
        mpe_Error retCode;
        mpe_SiServiceHandle si_entry_handle = MPE_SI_INVALID_HANDLE;
        mpe_SiModulationMode mode;
        uint32_t sourceId;
        uint32_t programNum;
        int programIndex = -1;
        uint8_t transactionId;
        uint16_t ecmPid = 0;

        uint32_t numStreams = 0;
        mpe_PODStreamDecryptInfo *streamInfo;

        uint32_t sizeOfRequest = 0;
        int i=0;

        mpe_RequestTableRow *newRequest = NULL;
        sizeOfRequest = sizeof(mpe_RequestTableRow);

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::mpe_podImplStartDecrypt Enter..\n",
                PODMODULE);

        /* start routine */
        *sessionHandlePtr = NULL;

        /* SI DB read lock */
        if ((retCode = mpe_siLockForRead()) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<%s::startDecrypt> - Unable to acquire SI lock, err=%d\n", PODMODULE,
                    retCode);
            return retCode;
        }

        /* Retrieve SI information */
        if(decryptRequestPtr->handleType == MPE_POD_SERVICE_DETAILS_HANDLE)
        {
            si_entry_handle = (mpe_SiServiceHandle) decryptRequestPtr->handle;
        }
        else if(decryptRequestPtr->handleType == MPE_POD_TRANSPORT_STREAM_HANDLE)
        {
        	mpe_SiTransportStreamHandle ts_handle = MPE_SI_INVALID_HANDLE;
        	uint32_t num_services = 0;
        	mpe_SiServiceHandle *array_service_handle;
        	ts_handle = (mpe_SiTransportStreamHandle) decryptRequestPtr->handle;

        	// Get all Service handles for this transport stream handle
        	// Find one that contains the 'pid' of interest
            if(mpe_siGetNumberOfServicesForTransportStreamHandle(ts_handle, &num_services) == MPE_SI_SUCCESS)
            {
                if ((retCode = mpe_memAllocP(MPE_MEM_POD, (num_services*sizeof(mpe_SiServiceHandle)), (void**) &array_service_handle)) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                            "<%s::startDecrypt> - Could not malloc memory for service handle array\n",
                            PODMODULE);
                    if (mpe_siUnLock() != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                                "<%s::startDecrypt> - Could not unlock SI\n", PODMODULE);
                    }
                    return retCode;
                }

                if(mpe_siGetAllServicesForTransportStreamHandle(ts_handle, array_service_handle, &num_services)== MPE_SI_SUCCESS)
            	{
            		int i=0;

                    for(i=0;i<num_services;i++)
                    {
                    	if(mpe_siFindPidInPMT(array_service_handle[i], decryptRequestPtr->pids[0].pid)
                    			== MPE_SI_SUCCESS)
                    	{
                            si_entry_handle = array_service_handle[i];
                            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                                    "<%s::startDecrypt> - Found service handle:0x%x with pid:%d\n",
                                    PODMODULE, si_entry_handle, decryptRequestPtr->pids[0].pid);
                    		break;
                    	}
                    }
            	}
            }
        }

        // If the handle is invalid at this point its an error..
        if(si_entry_handle == MPE_SI_INVALID_HANDLE)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                    "<%s::startDecrypt> - SI handle invalid..\n", PODMODULE);

            if (mpe_siUnLock() != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::startDecrypt> - Could not unlock SI\n", PODMODULE);
            }
            return MPE_EINVAL;
        }

        if ((retCode = mpe_siGetSourceIdForServiceHandle(si_entry_handle,
                    &sourceId)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Could not get sourceId for service handle 0x%x\n",
                    PODMODULE, si_entry_handle);

            if (mpe_siUnLock() != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::startDecrypt> - Could not unlock SI\n", PODMODULE);
            }
            return retCode;
        }

        if ((retCode = mpe_siGetProgramNumberForServiceHandle(si_entry_handle,
                    &programNum)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Could not get program number for service handle 0x%x\n",
                    PODMODULE, si_entry_handle);

            if (mpe_siUnLock() != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::startDecrypt> - Could not unlock SI\n", PODMODULE);
            }
            return retCode;
        }

        if ((retCode = mpe_siGetModulationFormatForServiceHandle(si_entry_handle,
                    &mode)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Could not get modulation mode for service handle 0x%x\n",
                    PODMODULE, si_entry_handle);

            if (mpe_siUnLock() != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::startDecrypt> - Could not unlock SI\n", PODMODULE);
            }
            return retCode;
        }

        /* if analog, don't need to do anything in this method */
        if (mode == MPE_SI_MODULATION_QAM_NTSC)
        {
            MPE_LOG(MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "<%s::startDecrypt> - Analog, No CA_PMT created (due to no CA descriptors)\n",
                    PODMODULE );
            if (mpe_siUnLock() != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::startDecrypt> - Could not unlock SI\n", PODMODULE);
            }
            /* tell the caller no CA data was found and that there is no need for a decrypt session */
            return MPE_ENODATA;
        }

        if ((retCode = mpe_siGetNumberOfComponentsForServiceHandle(si_entry_handle,
                    &numStreams)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Could not get number of components for service handle 0x%x\n",
                    PODMODULE, si_entry_handle);

            if (mpe_siUnLock() != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::startDecrypt> - Could not unlock SI\n", PODMODULE);
            }
            return retCode;
        }

        if ((retCode = mpe_memAllocP(MPE_MEM_POD, (numStreams * sizeof(mpe_PODStreamDecryptInfo)), (void**) &streamInfo)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<%s::startDecrypt> - Could not malloc memory for streamInfo \n",
                    PODMODULE);
        }

        // Check if there are any CA descriptors for the given service handle
        // This method populates the input table with elementary stream Pids associated with
        // the service. The CA status field is set to unknown. It will
        // be filled when ca reply/update is received from CableCARD.
        if(mpe_siCheckCADescriptors(si_entry_handle, ca_system_id,
                                          &numStreams, &ecmPid, streamInfo) != TRUE)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                    "<%s::startDecrypt> - No CA descriptors for service handle: 0x%x (could be clear channel..)\n",
                    PODMODULE, si_entry_handle);
            // CCIF 2.0 section 9.7.3
            // Even if the PMT does not contain any CA descriptors
            // a CA_PMT still needs to be sent to the card but with no CA descriptors
            // This path can no longer be used as an early return but used just
            // to retrieve the component information
        }

        /* Done with SIDB, release the read lock */
        if (mpe_siUnLock() != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<%s::startDecrypt> - Could not unlock SI\n", PODMODULE);
        }
        /* end read lock for SI DB*/

        /* Acquire table mutex */
        mpe_mutexAcquire(table_mutex);

        /* Create a new request session */
        {
            /* allocate request session
             */
            if ((retCode = mpe_memAllocP(MPE_MEM_POD, sizeOfRequest, (void**) &newRequest)) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::startDecrypt> - Could not malloc memory for newRequest\n",
                        PODMODULE);

                goto freeSessionReturn;
            }

            memset(newRequest, 0x0, sizeOfRequest);

            newRequest->state = MPE_POD_DECRYPT_REQUEST_STATE_UNKNOWN;
            newRequest->tunerId = decryptRequestPtr->tunerId;
            newRequest->mmiEnable = decryptRequestPtr->mmiEnable;
            newRequest->priority = decryptRequestPtr->priority;
            newRequest->eventQ = queueId;
            // always passed to optionalParam2 since this could be the edHandle
            newRequest->act = act;
            // Set the service handle
            newRequest->serviceHandle = si_entry_handle;
            // Set the sourceId in the request
            newRequest->sourceId = sourceId;
            // use a random number as ltsid
            // ltsid value for the new request is set below after determining
            // if an existing CA session can be shared
            if ((retCode = mpe_memAllocP(MPE_MEM_POD, (decryptRequestPtr->numPids * sizeof(mpe_MediaPID)), (void**) &newRequest->pids)) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::startDecrypt> - Could not malloc memory for pids\n",
                        PODMODULE);

                goto freeSessionReturn;
            }

            // Copy PIDs from decryptRequestParams to newRequest
            for (i = 0; i < decryptRequestPtr->numPids; i++)
            {
                newRequest->pids[i].pid = decryptRequestPtr->pids[i].pid;
                newRequest->pids[i].pidType = decryptRequestPtr->pids[i].pidType;
                MPE_LOG(MPE_LOG_DEBUG,
                        MPE_MOD_POD,
                        "<%s::startDecrypt> - Pod decrypt request Pids, pid=0x%x, type=%0x\n",
                        PODMODULE, newRequest->pids[i].pid, newRequest->pids[i].pidType);
            }
        }

        /* Find a matching entry in the request table to see
         * if an existing session can be shared (Ex: between broadcast and TSB) */
        if((retCode = findActiveDecryptRequestForReuse(decryptRequestPtr->tunerId, programNum, &programIndex))
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "<%s::startDecrypt> - No active decrypt session for tunerId=%d programNumber=%d\n",
                    PODMODULE, decryptRequestPtr->tunerId, programNum);
        }
        else
        {
            // Active session found
            mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[programIndex];

            /* If active session found, add the new request to the request table,
             * set the state to active. Return the request session pointer
             * as a session handle to the caller */

            MPE_LOG(MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "<%s::startDecrypt> - Found a active decrypt session for tunerId=%d programNumber=%d\n",
                    PODMODULE, decryptRequestPtr->tunerId, programNum);

            // Set the program index for the request
            newRequest->programIdx = programIndex;
            // Set the state to 'active'
            newRequest->state = MPE_POD_DECRYPT_REQUEST_STATE_ACTIVE;
            newRequest->ca_pmt_cmd_id = rowPtr->ca_pmt_cmd_id;
            // set the new request ltsid to be same as the rowptr ltsid
            newRequest->ltsId = rowPtr->ltsid;
            // Add new request to the table
            addRequestToTable(newRequest);

            // Update priority of program table index row to reflect higher of the priorities
            updateProgramIndexTablePriority(programIndex, newRequest->priority);

            // Set the session handle and send an event
            *sessionHandlePtr = (mpe_PODDecryptSessionHandle) newRequest;

            // Get the state of the existing request and post event based on that
            // Send decryption event
            sendJniDecryptSessionEvent(getLastEventForProgramIndex(programIndex),
                    (mpe_PODDecryptSessionHandle)*sessionHandlePtr,
                    rowPtr->ltsid);

            // Done here, return the handle
            goto sessionReturn;
        }

        // No active requests found to share with
        // Check if there are resources needed to service the new request
        if (checkAvailableResources(newRequest->tunerId, numStreams) != TRUE)
        {
            /* When the incoming request has a higher
             * priority than an existing request the lower priority request will be
             * pre-empted and placed in a waiting_for_resources state and the new request
             * will take its place
             */
            {
                mpe_Bool done = FALSE;
                mpe_SiServiceHandle serviceHandle = MPE_SI_INVALID_HANDLE;
                uint8_t ltsid = 0;
                uint8_t transId;

                MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                        "<%s::startDecrypt> - No decrypt resources available...\n", PODMODULE);

                while (!done)
                {
                    retCode = findRequestProgramIndexWithLowerPriority(newRequest, &programIndex);

                    if(retCode == MPE_SUCCESS)
                    {
                        // 0. Found low priority request (program index) to preempt
                        // 1. Find all requests for this program index
                        // 2. Stop decrypt session (program index table row) if active
                        // 3. Send 'not_selected' apdu
                        // 4. Release program index table row
                        // 5. Send JNI events (resource not available) to preempted requests
                        // (Requests remain in the request table but their state is set to 'WAITING_FOR_RESOURCES')
                        // 6. Check if we have enough resources
                        // 7. If not repeat step 0 next request with lower priority
                        // 8. If yes, assign the released program index to new request

                        // Random size array
                        mpe_PODDecryptSessionHandle handles[10];
                        uint8_t numHandles = 0;
                        int i;
                        mpe_PODDecryptSessionHandle rHandle;

                        mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[programIndex];

                        // There may be multiple requests per program index table row
                        if ((retCode = getDecryptSessionsForProgramIndex(programIndex, &numHandles, handles))
                                != MPE_SUCCESS)
                        {
                            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "<%s::startDecrypt> error retrieving decrypt sessions for program index:%d.\n",
                                    PODMODULE, programIndex);
                            goto freeSessionReturn;
                        }

                        for(i=0;i<numHandles;i++)
                        {
                            mpe_RequestTableRow *request = (mpe_RequestTableRow*)handles[i];

                            serviceHandle = request->serviceHandle;
                            rHandle = handles[i];

                            // Reset the requests state and program index
                            request->state = MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES;
                            request->programIdx = -1;
                            // Save the command id so that it can be re-activated when
                            // resources are available
                            request->ca_pmt_cmd_id = rowPtr->ca_pmt_cmd_id;

                            // Send an event to notify of resource loss
                            sendJniDecryptSessionEvent(MPE_POD_DECRYPT_EVENT_RESOURCE_LOST, rHandle, ltsid);
                        }

                        ltsid = rowPtr->ltsid;
                        transId = getNextTransactionIdForProgramIndex(programIndex);

                        // Send a ca_pmt apdu with 'not_selected' (0x04) when decrypt session
                        // is no longer in use
                        if((retCode = createAndSendCaPMTApdu(serviceHandle, programIndex, transId, ltsid, MPE_MPOD_CA_NOT_SELECTED)
                                != MPE_SUCCESS))
                        {
                             MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                                     "<%s::startDecrypt> - Error sending CA_PMT apdu (MPE_MPOD_CA_NOT_SELECTED) (error %d)\n",
                                     PODMODULE, retCode);
                             goto freeSessionReturn;
                        }

                        // Release the CP session
                        if(rowPtr->cpSession != NULL)
                        {
                            if(MPE_SUCCESS != mpeos_podStopCPSession(rowPtr->cpSession))
                            {
                                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                                        "%s::startDecrypt Error stopping CP session...\n",
                                        PODMODULE);
                            }
                        }

                        if ((retCode = releaseProgramIndexRow(programIndex))
                                != MPE_SUCCESS)
                        {
                            goto freeSessionReturn;
                        }

                        // Restore the transaction id
                        rowPtr->transactionId = transId;

                        // Check again for resources and repeat if not done
                        if(checkAvailableResources(newRequest->tunerId, numStreams) == TRUE)
                        {
                            done = TRUE;
                        }
                    }
                    else
                    {
                        // Did not find a request to preempt, the new request will be added to table but will be in
                        // waiting_for_resources state.
                        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                                "<%s::startDecrypt> - Did not find a lower priority requests to preempt..\n", PODMODULE);

                        newRequest->state = MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES;
                        newRequest->programIdx = -1;

                        // Add new request to the request table
                        addRequestToTable(newRequest);

                        // Send an event to notify of resource loss
                        sendJniDecryptSessionEvent(MPE_POD_DECRYPT_EVENT_RESOURCE_LOST,
                                (mpe_PODDecryptSessionHandle)newRequest, 0);

                        goto sessionReturn;
                    }
                } // END while (!done)
            }
        }

        if (mpe_podmgrIsReady() != MPE_SUCCESS)
        {
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_POD,
                     "%s::mpe_podImplStartDecrypt() POD is not READY.\n",
                     PODMODULE );

            newRequest->state = MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES;
            newRequest->programIdx = -1;

            // Add new request to the request table
            addRequestToTable(newRequest);

            // Send an event to notify of resource loss
            sendJniDecryptSessionEvent(MPE_POD_DECRYPT_EVENT_RESOURCE_LOST,
                    (mpe_PODDecryptSessionHandle)newRequest, 0);

            goto sessionReturn;
        }

        /* Get the next available program index table row that's not in use */
        programIndex = getNextAvailableProgramIndexTableRow();

        /* Now begin creating CA_PMT and go through
         * steps. Once sendAPDU is successful, set the 'state' and other fields in
         * program index table. Set the 'programsUsed' field. */

        /* begin creating a CA_PMT */
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "<%s::startDecrypt> - Attempting CA_PMT creation...\n", PODMODULE);

        // use a random number as ltsid
        // 'ltsid' requirements:
        // 1. Not already in use (since we are starting at 8 bit random number and incrementing,
        // when it wraps around at 255 make sure the the number is not being used by another
        // program index row)
        // 2. It is unique per transport stream(CCIF 2.0)
        // Even when two separate CA sessions are opened on the same tuner they
        // need to have the same ltsid
        newRequest->ltsId = getLtsidForTunerId(newRequest->tunerId);
        // If an existing ltsid for this tuner is not found get the next 'random' ltsid
        if(newRequest->ltsId == LTSID_UNDEFINED)
        {
        	// getLtsid() validates that the new ltsid is not in use by any other
        	// program index rows
            newRequest->ltsId = getLtsid();
        }

        // Set the program index for the request
        newRequest->programIdx = programIndex;

        // Add new request to the request table
        addRequestToTable(newRequest);

        // Set the transaction id
        transactionId = getNextTransactionIdForProgramIndex(programIndex);

        retCode = createAndSendCaPMTApdu(si_entry_handle,
                  programIndex, transactionId,
                  newRequest->ltsId,
                  MPE_MPOD_CA_QUERY);

        // now deal with the various return conditions.
        if(retCode == MPE_SUCCESS)
        {
            // set state to MPE_POD_DECRYPT_STATE_ISSUED_QUERY
            commitProgramIndexTableRow(programIndex, MPE_MPOD_CA_QUERY,
                                       programNum, sourceId,
                                       transactionId, decryptRequestPtr->tunerId, newRequest->ltsId,
                                       newRequest->priority, numStreams, ecmPid,
                                       streamInfo, MPE_POD_DECRYPT_STATE_ISSUED_QUERY);

            // Set the state of request table entry based on the return code from sendAPDU
            newRequest->state = MPE_POD_DECRYPT_REQUEST_STATE_ACTIVE;
            newRequest->ca_pmt_cmd_id = MPE_MPOD_CA_QUERY;

#ifdef PODIMPL_FRONTPANEL_DEBUG
		{
			int i;

			for (i=0; i<numStreams; i++)
			{
				MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
						"<%s::startDecrypt> - streamInfo[%d].pid: %d\n", PODMODULE, i, streamInfo[i].pid);
				snprintf(fp_debug_lines[i+1],5,"%d",streamInfo[i].pid);
			}
		}
#endif
            goto sessionReturn;
        }
        else if (retCode == MPE_ENODATA)
        {
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "<%s::startDecrypt> - No CA_PMT created (due to no CA descriptors)\n",
                    PODMODULE );

            // We didn't fail - there's just no session required
            *sessionHandlePtr = NULL;
            retCode = MPE_SUCCESS;

            /* clear channel (no CA_Descriptor). Not really an error but the session memory needs to be freed */
            goto freeSessionReturn;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<%s::startDecrypt> - Error generating CA_PMT (error 0x%x)\n",
                    PODMODULE, retCode);
            goto freeSessionReturn;
        }

        sessionReturn:
        if ((retCode = mpe_mutexRelease(table_mutex) != MPE_SUCCESS))
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "<%s::startDecrypt> - sessionReturn, failed to release programIndexTable mutex, error=%d\n",
                    PODMODULE, retCode);
        }

        /* don't setup session unless everything successful */
        *sessionHandlePtr = (mpe_PODDecryptSessionHandle) newRequest;

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "<%s::startDecrypt> - sessionHandle=0x%p\n", PODMODULE, newRequest);

        // De-allocate streamInfo
        if(streamInfo != NULL)
        {
            mpe_memFreeP(MPE_MEM_POD, streamInfo);
            streamInfo = NULL;
        }

        logProgramIndexTable("startDecrypt sessionReturn");
        logRequestTable("startDecrypt sessionReturn");

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "<%s::startDecrypt> - done..\n", PODMODULE);
        return retCode;

        /* Generally called when there an error */
        freeSessionReturn:

        if (mpe_mutexRelease(table_mutex) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "<%s::startDecrypt> - freeSessionReturn, failed to release programIndexTable mutex, error=%d\n",
                    PODMODULE, retCode);
        }

        // Remove request from list
        removeRequestFromTable(newRequest);

        // De-allocate request
        if(newRequest != NULL)
        {
            mpe_memFreeP(MPE_MEM_POD, newRequest);
            newRequest = NULL;
        }

        // De-allocate streamInfo
        if(streamInfo != NULL)
        {
            mpe_memFreeP(MPE_MEM_POD, streamInfo);
            streamInfo = NULL;
        }

        return retCode;
    }
}

mpe_Error mpe_podImplStopDecrypt(mpe_PODDecryptSessionHandle sessionHandle)
{
    if(g_mpe_ca_enable == FALSE)
    {
        // If MPE CA management is disabled the stack does not
        // send/process APDUs on the CAS session.

        return MPE_SUCCESS;
    }
    else
    {
        uint32_t programIdx;
        mpe_Error retCode = MPE_SUCCESS;
        mpe_ProgramIndexTableRow *rowPtr = NULL;
        mpe_RequestTableRow *request = NULL;
        mpe_SiServiceHandle serviceHandle;
        //uint8_t tunerId = 0;
        uint8_t ltsId;
        uint8_t transId;

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "%s:mpe_podImplStopDecrypt sessionHandle=0x%p\n", PODMODULE,
                sessionHandle);

        /* Acquire mutex */
        mpe_mutexAcquire(table_mutex);

        // Find the request in the request table
        if(!findRequest(sessionHandle))
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<%s::mpe_podImplStopDecrypt> - Did not find the session handle: 0x%p\n",
                    PODMODULE, sessionHandle);
            retCode = MPE_EINVAL;
            goto stopReturn;
        }

        // Termination of decrypt session, no more events will be sent on this queue.
        retCode = sendJniDecryptSessionEvent(
                MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN, sessionHandle, 0);

        request = (mpe_RequestTableRow *) sessionHandle;
        // Retrieve the corresponding program index, service handle and tunerId
        programIdx = request->programIdx;
        serviceHandle = request->serviceHandle;
        //tunerId = request->tunerId;

        // Remove the decrypt request from the table and de-allocate memory
        if ((retCode = releaseDecryptRequest((mpe_RequestTableRow *) sessionHandle)) != MPE_SUCCESS)
        {
            goto stopReturn;
        }

        if(!VALID_PROGRAM_INDEX(programIdx))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                    "<%s::mpe_podImplStopDecrypt> invalid programIdx:%d\n", PODMODULE, programIdx);
            goto stopReturn;
        }

        rowPtr = &programIndexTable.rows[programIdx];

        // Update priority in the program index array
        updatePriority(programIdx);

        // Now remove the corresponding entry from the program index table if no other
        // uses remain (based on the program index)
        // check if this programIndex is being used by any other decrypt request
        if(!isProgramIndexInUse(programIdx))
        {
            transId = getNextTransactionIdForProgramIndex(programIdx);
            ltsId = rowPtr->ltsid;
            // Send a ca_pmt apdu with 'not_selected' (0x04) when decrypt session
            // is no longer in use
            if((retCode = createAndSendCaPMTApdu(serviceHandle, programIdx,
                    transId, ltsId,
                    MPE_MPOD_CA_NOT_SELECTED)!= MPE_SUCCESS))
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::mpe_podImplStopDecrypt> - Error sending CA_PMT apdu (MPE_MPOD_CA_NOT_SELECTED) (error %d)\n",
                        PODMODULE, retCode);
                goto stopReturn;
            }

            // Release the CP session
            if(rowPtr->cpSession != NULL)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::mpe_podImplStopDecrypt stopping CP session:0x%x\n",
                        PODMODULE, rowPtr->cpSession);
                if(MPE_SUCCESS != mpeos_podStopCPSession(rowPtr->cpSession))
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                            "%s::mpe_podImplStopDecrypt Error stopping CP session...\n",
                            PODMODULE);
                }
            }

            rowPtr->transactionId = transId;

            // Shutting down the decrypt session
            rowPtr->lastEvent = MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN;

            if ((retCode = releaseProgramIndexRow(programIdx))
                    != MPE_SUCCESS)
            {
                goto stopReturn;
            }

            // Re-activate a suspended request. There may be multiple requests that can share this
            // program index table row
            activateSuspendedRequests(programIdx);
        }

        logProgramIndexTable("stopDecrypt");
        logRequestTable("stopDecrypt");

        stopReturn:
        mpe_mutexRelease(table_mutex);
        /* Release mutex */

        return retCode;
    }
}

mpe_Error mpe_podImplGetDecryptStreamStatus(mpe_PODDecryptSessionHandle handle, uint8_t numStreams, mpe_PODStreamDecryptInfo streamInfo[])
{
    if(g_mpe_ca_enable == FALSE)
    {
        // If MPE CA management is disabled the stack does not
        // send/process APDUs on the CAS session.

        return MPE_SUCCESS;
    }
    else
    {
        mpe_Error retCode = MPE_SUCCESS;
        uint8_t programIdx;
        LINK *lp;
        int i = 0;
        mpe_ProgramIndexTableRow *rowPtr;

        if(handle == NULL || numStreams == 0 || streamInfo == NULL)
        {
           MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "%s::mpe_podImplGetDecryptStreamStatus - invalid parameter..\n",
                    PODMODULE);
            return MPE_EINVAL;
        }

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::mpe_podImplGetDecryptStreamStatus handle:0x%p\n",
                PODMODULE, handle);

        /* Acquire mutex */
        mpe_mutexAcquire(table_mutex);

        // Find the request in the request table
        if(!findRequest(handle))
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<%s::mpe_podImplStopDecrypt> - Did not find the session handle: 0x%p\n",
                    PODMODULE, handle);
            retCode = MPE_EINVAL;
            goto cleanupAndExit;
        }

        if((retCode = getDecryptRequestProgramIndex(handle, &programIdx)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "%s::mpe_podImplGetDecryptStreamStatus failed - unable to retrieve requested program index %d..\n",
                    PODMODULE, retCode);
            goto cleanupAndExit;
        }

        if(((mpe_RequestTableRow *)handle)->state != MPE_POD_DECRYPT_REQUEST_STATE_ACTIVE)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::mpe_podImplGetDecryptStreamStatus session is not active...\n", PODMODULE);
            for (i=0;i<numStreams;++i)
            {
                streamInfo[i].status = CA_ENABLE_DESCRAMBLING_TECH_FAIL;
            }
            goto cleanupAndExit;
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::mpe_podImplGetDecryptStreamStatus programIdx:%d\n", PODMODULE, programIdx);

        rowPtr = &programIndexTable.rows[programIdx];

        for (i=0;i<numStreams;++i)
        {
            if(rowPtr->authorizedPids != NULL  && llist_cnt(rowPtr->authorizedPids) != 0)
            {
                lp = llist_first(rowPtr->authorizedPids);
                while(lp)
                {
                    mpe_PODStreamDecryptInfo *pidInfo = (mpe_PODStreamDecryptInfo *)llist_getdata(lp);
                    if(pidInfo)
                    {
                        if (streamInfo[i].pid == pidInfo->pid)
                        {
                            streamInfo[i].status = pidInfo->status;
                            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::mpe_podImplGetDecryptStreamStatus pid:0x%x status:%d\n",
                                    PODMODULE, pidInfo->pid, pidInfo->status);
                        }
                    }
                    lp = llist_after(lp);
                }
            }
            else
            {
               MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::mpe_podImplGetDecryptStreamStatus pid list empty..\n",
                        PODMODULE);
            }
        }

        cleanupAndExit:
        mpe_mutexRelease(table_mutex);
        /* Release mutex */

        return retCode;
    }
}

#ifdef PODIMPL_FRONTPANEL_DEBUG

static void podFrontPanelDebugThread(void* data)
{
    int counter = 0;
    mpe_FpBlinkSpec blinkSpec = {0, 0};
    mpe_FpScrollSpec scrollSpec = {0, 0, 0};

    while (TRUE)
    {
        while (fp_debug_lines[counter][0] == '\0')
        { // Skip blank entries
            counter = (counter+1) % 10;
        }
        char scratch[6];
        const char * text[] = {scratch};
        strncpy(scratch, fp_debug_lines[counter], sizeof(scratch));

        mpe_fpSetText(MPE_FP_TEXT_MODE_STRING, 1,       text,     1,    1,         blinkSpec,scrollSpec);
        //           (mode                     numLines textLines color brightness blinkSpec scrollSpec)
        mpe_threadSleep(2000,0);
        counter = (counter+1) % 10;
    }
} // END podFrontPanelDebugThread()

#endif

static void podEventHandlerThread(void* data)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event nextEventId;
    void *edEventData1, *edEventData2;
    uint32_t edEventData3;
    uint32_t timeout = 0; /* infinite timeout */
    mpe_Bool exitWorkerThread = FALSE;
    uint32_t paramValue = 0;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: podWorkerThread started.\n",
            PODMODULE);

    /* now create the event queue going between MPE and MPEOS. Don't need to pass the EdHandle down*/
    if ((retCode = mpe_eventQueueNew(&podMpeosEvQ, podMpeosEvQName))
            != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_POD,
                "%s: mpePodThreadEntry unable to create event queue, error=%d... terminating thread.\n",
                PODMODULE, retCode);
        return;
    }

    /* register with mpeos layer (mpeos_ is correct) */
    if ((retCode = mpeos_podRegister(podMpeosEvQ, NULL)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: podWorkerThread, mpeos_podRegister failed %d\n",
                PODMODULE, retCode);
        return;
    }

    if (podDB.pod_isReady)
    {
        MPE_LOG( MPE_LOG_INFO, MPE_MOD_POD,
                 "%s: podWorkerThread POD READY at startup - pushing event\n",
                 PODMODULE );
        // Temporarily set the isReady flag to FALSE while we process
        // POD_READY event in the handler below
        podDB.pod_isReady = FALSE;
        mpe_eventQueueSend(podMpeosEvQ, MPE_POD_EVENT_READY, NULL, NULL, 0);
    }

    while (!exitWorkerThread)
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_POD,
                "%s: podWorkerThread starting waitNext -- podMpeosEvQ 0x%x (%d)\n",
                PODMODULE, podMpeosEvQ, podMpeosEvQ);

        if ((retCode = mpe_eventQueueWaitNext(podMpeosEvQ, &nextEventId,
                &edEventData1, &edEventData2, &edEventData3, timeout))
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: podWorkerThread, mpe_eventQueueWaitNext failed %d\n",
                    PODMODULE, retCode);
            return;
        }

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "%s: podWorkerThread -- received message, nextEventId=%d\n",
                PODMODULE, nextEventId);

        mpe_mutexAcquire(podDB.pod_mutex);

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "%s: podWorkerThread -- acquired DB mutex..\n", PODMODULE);

        {   // This should be a rare case. But we'll treat consecutive
            //  POD_READYs as a degenerate POD reset
            mpe_Bool podWasAlreadyReady = podDB.pod_isReady;

            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "%s: podWorkerThread: Processing event:0x%x\n",
                    PODMODULE, nextEventId);

            if (podWasAlreadyReady && (nextEventId == MPE_POD_EVENT_READY))
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_POD,
                        "%s: podWorkerThread, received POD_READY when POD is already ready. Synthesizing a RESET_PENDING event\n",
                        PODMODULE );
                if ( mpe_eventQueueSend(podMpeosEvQ, MPE_POD_EVENT_RESET_PENDING,
                        NULL, NULL, 0) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR,
                            MPE_MOD_POD,
                            "%s: podWorkerThread,  mpeos_eventQueueSend MPE_POD_EVENT_RESET_PENDING failed.\n",
                            PODMODULE);
                }

                if ( mpe_eventQueueSend(podMpeosEvQ, MPE_POD_EVENT_READY,
                        NULL, NULL, 0) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR,
                            MPE_MOD_POD,
                            "%s: podWorkerThread,  mpeos_eventQueueSend MPE_POD_EVENT_READY failed.\n", PODMODULE);
                }
                continue;
            } // END if (podWasAlreadyReady)
        }

        if (sendPodEvent(nextEventId, edEventData1, edEventData3)
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "%s: podWorkerThread,  mpeos_eventQueueSend failed - podJniEdHandle=%p, nextEventId=%d\n",
                    PODMODULE, podJniPodEventEdHandle, nextEventId);
        }

        switch (nextEventId)
        {
        case MPE_POD_EVENT_SHUTDOWN:
        {
            /* check for shutdown sent by the unregister method */
            /* the unregister code that sent the shutdown handles closing down the JNI level */
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "%s: podWorkerThread: Processing MPE_POD_EVENT_SHUTDOWN\n",
                    PODMODULE);

            /* now unregister with the mpeos layer (mpeos_ call is correct) */
            mpeos_podUnregister();

            /* this level created the queue, it will also delete it */
            if ((retCode = mpe_eventQueueDelete(podMpeosEvQ)) != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpeos_eventQueueDelete of mpeos queue failed\n",
                        PODMODULE);
                mpe_mutexRelease(podDB.pod_mutex);
                return;
            }

            /* clear out saved info */
            podMpeosEvQ = 0;

            exitWorkerThread = true;

            break;
        } // END case MPE_POD_EVENT_SHUTDOWN
        case MPE_POD_EVENT_RECV_APDU:
        {
            /*
             * do nothing here. Pass the event up to java where mpe_podRecvAPDU will be called.
             * Do resource management in the mpe_podRecvAPDU call.
             */
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "%s: podWorkerThread: Processing MPE_POD_EVENT_RECV_APDU \n",
                    PODMODULE);
            break;
        } // END case MPE_POD_EVENT_RECV_APDU
        case MPE_POD_EVENT_INSERTED:
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "%s: podWorkerThread,  MPE_POD_EVENT_INSERTED\n", PODMODULE);
            podDB.pod_isPresent = true;
            break;
        } // END case MPE_POD_EVENT_INSERTED
        case MPE_POD_EVENT_READY:
        {
            uint8_t apdu_buf[1] = {0};
            uint32_t apdu_length = 0;

            // TODO, TODO_POD how do we handle the Generic Features etc. when the POD is added.
            // Best guess that it is the mpeos layer's responsibility to send these and MPE will echo them on.

            /* get data on the card. mpeos_ is correct */
            if ((retCode = mpeos_podGetParam(
                    MPE_POD_PARAM_ID_MAX_NUM_ELEMENTARY_STREAM, &paramValue))
                    != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpeos_podGetParam(MPE_POD_PARAM_ID_MAX_NUM_ELEMENTARY_STREAM) failed.  Error=%d\n",
                        PODMODULE, retCode);
            }
            else
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpeos_podGetParam(MPE_POD_PARAM_ID_MAX_NUM_ELEMENTARY_STREAM)=%d\n",
                        PODMODULE, paramValue);
                podDB.pod_maxElemStreams = paramValue;
                paramValue = 0;
            }

            if ((retCode = mpeos_podGetParam(MPE_POD_PARAM_ID_MAX_NUM_PROGRAMS,
                    &paramValue)) != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpeos_podGetParam(MPE_POD_PARAM_ID_MAX_NUM_PROGRAMS) failed.  Error=%d\n",
                        PODMODULE, retCode);
            }
            else
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpeos_podGetParam(MPE_POD_PARAM_ID_MAX_NUM_PROGRAMS)=%d\n",
                        PODMODULE, paramValue);
                podDB.pod_maxPrograms = paramValue;
                paramValue = 0;
            }
            if ((retCode = mpeos_podGetParam(
                    MPE_POD_PARAM_ID_MAX_NUM_TRANSPORT_STREAMS, &paramValue))
                    != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpeos_podGetParam(MPE_POD_PARAM_ID_MAX_NUM_TRANSPORT_STREAMS) failed.  Error=%d\n",
                        PODMODULE, retCode);
            }
            else
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpeos_podGetParam(MPE_POD_PARAM_ID_MAX_NUM_TRANSPORT_STREAMS)=%d\n",
                        PODMODULE, paramValue);
                podDB.pod_maxTransportStreams = paramValue;
                paramValue = 0;
            }

            /* this is done last so that code that check this w/o grabbing the mutex will not be fooled */
            podDB.pod_isReady = TRUE;

            // Initialize the program index table and request table
            mpe_mutexAcquire(table_mutex);
            initProgramIndexTable();
            mpe_mutexRelease(table_mutex);

            if ((retCode = mpe_podCASConnect(&casSessionId, &casResourceVersion)) != MPE_SUCCESS)
            {
                casSessionId = 0;
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpe_podCASConnect.  Error=%d\n",
                        PODMODULE, retCode);
                break;
            }

            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "<%s::podWorkerThread> casSessionId: 0x%x...\n", PODMODULE, casSessionId);

            // Send ca_info_inquiry apdu
            // to retrieve the CA_System_id
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "<%s::podWorkerThread> - Sending CA_INFO_INQUIRY APDU...\n", PODMODULE);

            {
                mpe_Bool done = false;
                uint8_t retry_count = g_mpe_ca_retry_count;
                while(!done)
                {
                    retCode = mpe_podSendAPDU(casSessionId, CA_INFO_INQUIRY_TAG, apdu_length, apdu_buf);
                    if (retCode == MPE_SUCCESS)
                    {
                        done = true;
                    }
                    else
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                                "<%s::podWorkerThread> - Error sending CA_INFO_INQUIRY (error %d)\n",
                                PODMODULE, retCode);
                        // Re-attempt sending the apdu if retry count is set
                        if(retry_count)
                        {
                            retry_count--;
                        }

                        if(retry_count == 0)
                        {
                            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "<%s::createAndSendCaPMTApdu> - Error sending CA_INFO_INQUIRY apdu after %d retries..\n", PODMODULE, g_mpe_ca_retry_count);
                            done = true;
                        }
                        else
                        {
                            // Wait here before attempting a re-send
                            // Configurable via ini variable
                            mpe_threadSleep(g_mpe_ca_retry_timeout, 0);
                        }
                    }
                } // End while(!done)
            }

            // v  v  v  v  v  v
            // TEST CODE - REMOVE!!!!
            //ca_system_id = 0x02;
            // END TEST CODE!!!!
            // ^  ^  ^  ^  ^  ^

            mpeos_memStats(FALSE, MPE_MOD_POD,
                    "DUMP AFTER PROGRAMINDEXTABLE MALLOC");

            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "%s: podWorkerThread: Completed programIndexTable initialization\n",
                    PODMODULE);

            logProgramIndexTable("podWorkerThread POD_READY");

            // A POD_READY event may be received following CableCARD reset
            // Activate all suspended CA sessions
            mpe_mutexAcquire(table_mutex);
            activateSuspendedCASessions();
            mpe_mutexRelease(table_mutex);

            break;
        } // END case MPE_POD_EVENT_READY
        case MPE_POD_EVENT_REMOVED:
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "%s: podWorkerThread: Processing MPE_POD_EVENT_REMOVED\n",
                    PODMODULE);

            removeAllDecryptSessionsAndResources
                    ( MPE_POD_DECRYPT_EVENT_POD_REMOVED);

            if ((retCode = mpe_mutexAcquire(table_mutex)
                    != MPE_SUCCESS))
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread, failed to get table mutex, error=%d\n",
                        PODMODULE, retCode);
                return;
            }

            if (programIndexTable.rows != NULL)
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_POD,
                        "%s: programIndexTable row memory free, programIndexTable.rows=%p\n",
                        PODMODULE, programIndexTable.rows);

                /*
                 * Release the programIndexTable mutex in memory free failure case.
                 */
                if((retCode = mpe_memFreeP(MPE_MEM_POD, programIndexTable.rows)) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "<%s::startDecrypt> - Could not free memory for programIndexTable.rows, error=%d\n", PODMODULE, retCode);

                    mpe_mutexRelease(table_mutex);
                    mpe_mutexRelease(podDB.pod_mutex);
                    return;
                }

                programIndexTable.rows = NULL;
                programIndexTable.numRows = 0;
                programIndexTable.elementaryStreamsUsed = 0;
                programIndexTable.programsUsed = 0;
                programIndexTable.transportStreamsUsed = 0;
            }

            if ((retCode = mpe_mutexRelease(table_mutex) != MPE_SUCCESS))
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread, failed to release table mutex, error=%d\n",
                        PODMODULE, retCode);
                mpe_mutexRelease(podDB.pod_mutex);
                return;
            }

            podDB.pod_isPresent = false;
            podDB.pod_isReady = false;
            podDB.pod_maxElemStreams = 0;
            podDB.pod_maxPrograms = 0;
            podDB.pod_maxTransportStreams = 0;

            ca_system_id = 0;

            if ((retCode = mpe_podCASClose(casSessionId)) != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpe_podCASClose.  Error=%d\n",
                        PODMODULE, retCode);
            }

            casSessionId = 0;
            break;
        } // END case MPE_POD_EVENT_REMOVED
        case MPE_POD_EVENT_RESET_PENDING:
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "%s: podWorkerThread: Processing MPE_POD_EVENT_RESET_PENDING\n",
                    PODMODULE);

            podDB.pod_isReady = FALSE;
            podDB.pod_maxElemStreams = 0;
            podDB.pod_maxPrograms = 0;
            podDB.pod_maxTransportStreams = 0;

            if ((retCode = mpe_mutexAcquire(table_mutex)
                    != MPE_SUCCESS))
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread, failed to get table mutex, error=%d\n",
                        PODMODULE, retCode);
                mpe_mutexRelease(podDB.pod_mutex);
                return;
            }

            // Suspend all active CA sessions
            suspendActiveCASessions();

            if (programIndexTable.rows != NULL)
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_POD,
                        "%s: programIndexTable row memory free, programIndexTable.rows=%p\n",
                        PODMODULE, programIndexTable.rows);

                /*
                 * Release the programIndexTable mutex in memory free failure case.
                 */
                if((retCode = mpe_memFreeP(MPE_MEM_POD, programIndexTable.rows)) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "<%s::startDecrypt> - Could not free memory for programIndexTable.rows, error=%d\n", PODMODULE, retCode);

                    mpe_mutexRelease(table_mutex);
                    mpe_mutexRelease(podDB.pod_mutex);
                    return;
                }

                programIndexTable.rows = NULL;
                programIndexTable.numRows = 0;
                programIndexTable.elementaryStreamsUsed = 0;
                programIndexTable.programsUsed = 0;
                programIndexTable.transportStreamsUsed = 0;
            }

            if ((retCode = mpe_mutexRelease(table_mutex) != MPE_SUCCESS))
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread, failed to release table mutex, error=%d\n",
                        PODMODULE, retCode);
            }

            ca_system_id = 0;

            if ((retCode = mpe_podCASClose(casSessionId)) != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "%s: podWorkerThread,  mpe_podCASClose.  Error=%d\n",
                        PODMODULE, retCode);
            }

            casSessionId = 0;
            break;
        } // END case MPE_POD_EVENT_RESET_PENDING
        case MPE_POD_EVENT_SEND_APDU_FAILURE:
        {
            //MPE_LOG(MPE_LOG_INFO,
            //        MPE_MOD_POD,
            //        "%s: podWorkerThread: Ignoring MPE_POD_EVENT_SEND_APDU_FAILURE\n",
            //        PODMODULE);
            break;
        }
        case MPE_POD_EVENT_RESOURCE_AVAILABLE:
        {
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "%s: podWorkerThread: Ignoring MPE_POD_EVENT_RESOURCE_AVAILABLE\n",
                    PODMODULE);
            break;
        }
        default:
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<%s::startDecrypt: Received unknown event (%d)\n",
                    PODMODULE, nextEventId);
            break;
        }

        } // END switch (nextEventId)

        mpe_mutexRelease(podDB.pod_mutex);

    }
    /* exit out of thread */
}

static mpe_Error initProgramIndexTable()
{
    mpe_Error retCode = MPE_SUCCESS;

    // Allocate program index table
    uint32_t programIndexTableLength = 0;
    programIndexTableLength = sizeof(mpe_ProgramIndexTableRow)
            * podDB.pod_maxPrograms;

    // Allocate program index table rows (size is equal to number of
    // programs the CableCARD can de-scramble simultaneously)
    programIndexTable.numRows = podDB.pod_maxPrograms;

    if (programIndexTable.rows == NULL)
    {
        if ((retCode = mpe_memAllocP(MPE_MEM_POD, programIndexTableLength,
                (void**) &programIndexTable.rows)) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "%s: initProgramIndexTable, failed malloc memory for programIndexTable, error=%d\n",
                    PODMODULE, retCode);
            return retCode;
        }

        memset(programIndexTable.rows, 0, programIndexTableLength);
    }

    return MPE_SUCCESS;
}

static void initRequestTable()
{
    // Create new request table
    requestTable = llist_create();
}

void commitProgramIndexTableRow(uint8_t programIdx, uint8_t ca_pmt_cmd_id,
                                uint16_t progNum, uint16_t sourceId,
                                uint8_t transactionId, uint8_t tunerId, uint8_t ltsid,
                                uint8_t priority, uint8_t numStreams, uint16_t ecmPid,
                                mpe_PODStreamDecryptInfo streamInfo[], uint8_t state)
{
    int i=0;
    mpe_PODStreamDecryptInfo *pidInfo;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::commitProgramIndexTableRow, programIdx=%d\n",
            PODMODULE, programIdx);

    // Update the number of transport streams used
    if(!tunerInUse(tunerId))
        programIndexTable.transportStreamsUsed++;

    // Update the number of programs used
    programIndexTable.programsUsed++;

    // Update the number of elementary streams used
    programIndexTable.elementaryStreamsUsed += numStreams;

    mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[programIdx];

    rowPtr->ca_pmt_cmd_id = ca_pmt_cmd_id;
    rowPtr->programNum = progNum;
    rowPtr->transactionId = transactionId;
    rowPtr->ltsid = ltsid;
    rowPtr->sourceId = sourceId;
    rowPtr->priority = priority;
    rowPtr->state = state;
    rowPtr->ecmPid = ecmPid;
    rowPtr->tunerId = tunerId;
    // Set authorized pids after receiving ca_pmt_reply/update
    rowPtr->authorizedPids = llist_create();

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::commitProgramIndexTableRow, numStreams=%d\n",
            PODMODULE, numStreams);

    for(i=0;i<numStreams;i++)
    {
        LINK *lp;
        // Allocate a new entry
        if(mpeos_memAllocP(MPE_MEM_POD, sizeof(mpe_PODStreamDecryptInfo), (void**) &pidInfo) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "commitProgramIndexTableRow error allocating memory\n");
            return;
        }
        pidInfo->pid = streamInfo[i].pid;
        pidInfo->status = streamInfo[i].status;

        lp = llist_mklink((void *) pidInfo);
        llist_append(rowPtr->authorizedPids, lp);

        MPEOS_LOG(MPE_LOG_DEBUG,
                MPE_MOD_POD,
                "commitProgramIndexTableRow - pid:%d ca_status:%d \n", pidInfo->pid, pidInfo->status);
    }

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::commitProgramIndexTableRow transportStreamsUsed=%d programsUsed=%d elementaryStreamsUsed=%d\n", PODMODULE,
            programIndexTable.transportStreamsUsed, programIndexTable.programsUsed, programIndexTable.elementaryStreamsUsed);
}

static mpe_Bool tunerInUse(uint8_t tunerId)
{
    int index=0;
    for(index=0;index<programIndexTable.numRows;index++)
    {
        mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[index];
        if(!rowPtr->tunerId && (rowPtr->tunerId == tunerId))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::tunerInUse returning TRUE\n", PODMODULE);
            return TRUE;
        }
    }
    return FALSE;
}

// Combination of programIndex and transactionId uniquely identifies a response
// on an active decrypt session
// (caller has the table mutex)
static mpe_Bool isValidReply(uint8_t programIdx, uint8_t transactionId)
{
    int index=0;
    for(index=0;index<programIndexTable.numRows;index++)
    {
        // Reply is always a response to a query
        mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[index];
        if( (index == programIdx) && (rowPtr->transactionId == transactionId)
                && (rowPtr->state == MPE_POD_DECRYPT_STATE_ISSUED_QUERY))
                return TRUE;
    }
    return FALSE;
}

// Combination of programIndex and transactionId uniquely identifies a response
// on an active decrypt session
// (caller has the table mutex)
static mpe_Bool isValidUpdate(uint8_t programIdx, uint8_t transactionId)
{
    int index=0;
    for(index=0;index<programIndexTable.numRows;index++)
    {
        // Update always is response to a ca pmt that is not a query
        // (what about MPE_POD_DECRYPT_STATE_NOT_SELECTED?)
        mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[index];
        if( (index == programIdx) && (rowPtr->transactionId == transactionId)
                && (rowPtr->state != MPE_POD_DECRYPT_STATE_ISSUED_QUERY))
                return TRUE;
    }
    return FALSE;
}

static int getNextAvailableProgramIndexTableRow(void)
{
    int idx;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "%s: getNextAvailableProgramIndexTableRow, programIndexTable.numRows=%d\n",
            PODMODULE, programIndexTable.numRows);

    /* add a new session to an unused row */
    for (idx = 0; idx < programIndexTable.numRows; idx++)
    {
        mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[idx];
        MPE_LOG(MPE_LOG_DEBUG,
                MPE_MOD_POD,
                "%s: getNextAvailableProgramIndexTableRow, &programIndexTable.rows[%d]=%p\n",
                PODMODULE, idx, rowPtr);

        // Find a program index table row which is not in use
        if (rowPtr->state == MPE_POD_DECRYPT_STATE_NOT_SELECTED)
        {
            MPE_LOG(MPE_LOG_DEBUG,
                    MPE_MOD_POD,
                    "%s: getNextAvailableProgramIndexTableRow, available index=%d\n",
                    PODMODULE, idx);
            return idx;
        }
    }
    return -1;
}

static mpe_Bool checkAvailableResources(uint8_t tunerId, uint8_t numStreams)
{
    uint8_t tunersNeeded;
    if(!programIndexTable.transportStreamsUsed) // None used
        tunersNeeded = 1;
    else
        tunersNeeded = tunerInUse(tunerId) ?  programIndexTable.transportStreamsUsed
                : (programIndexTable.transportStreamsUsed)++;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: checkAvailableResources, numTuners=%d numPrograms=%d numStreams=%d\n",
            PODMODULE, tunersNeeded, ((programIndexTable.programsUsed)+1),
            ((programIndexTable.elementaryStreamsUsed)+numStreams));

       /* TODO - check the total number of discrete tuners in use and test against
              podDB.pod_maxTransportStreams*/
           /* tunersNeeded >= podDB.pod_maxTransportStreams || */
    if( ((programIndexTable.programsUsed)+1) <= podDB.pod_maxPrograms
            &&  ((programIndexTable.elementaryStreamsUsed)+numStreams) <= podDB.pod_maxElemStreams )
    {
        return TRUE;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: getResourcesNeededForRequest, resource limit exceeded..\n", PODMODULE);
    return FALSE;
}

static mpe_Error releaseDecryptRequest(mpe_RequestTableRow *request)
{
    mpe_Error retCode = MPE_SUCCESS;
    LINK *lp = NULL;

    if(request == NULL)
        return MPE_EINVAL;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: releaseDecryptRequest, request=0x%p\n", PODMODULE, request);

    lp = llist_linkof(requestTable, (void *)request);
    llist_rmlink(lp);

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: releaseDecryptRequest, requestTable count=%lu\n", PODMODULE, llist_cnt(requestTable));

    if ((retCode = mpe_memFreeP(MPE_MEM_POD, request)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_POD,
                "%s: releasePODDecryptSession, FAIL to free memory located at request=%p\n",
                PODMODULE, request);
    }

    return retCode;
}

static mpe_Error releaseProgramIndexRow(uint8_t programIdx)
{
    uint8_t transId;
    mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[programIdx];

    if(programIndexTable.programsUsed)
    {
    	programIndexTable.programsUsed--;
    }
    else
    {
    	MPE_LOG(MPE_LOG_WARN, MPE_MOD_POD, "%s::releaseProgramIndexRow programIdx = %d, rowPtr->programsUsed was 0...\n",
    			PODMODULE, programIdx);
    }

    if(rowPtr->authorizedPids != NULL)
    {
    	if(llist_cnt(rowPtr->authorizedPids) > programIndexTable.elementaryStreamsUsed)
    	{
    		MPE_LOG(MPE_LOG_WARN, MPE_MOD_POD,
    				"%s::releaseProgramIndexRow programIdx = %d, authorizedPids count invalid...\n",
    				PODMODULE, programIdx);
    		programIndexTable.elementaryStreamsUsed = 0;
		}
		else
		{
			programIndexTable.elementaryStreamsUsed -= llist_cnt(rowPtr->authorizedPids);
		}

        // De-allocate authorized Pids list
        llist_free(rowPtr->authorizedPids);
        rowPtr->authorizedPids = NULL;
    }
    else
    {
		MPE_LOG(MPE_LOG_WARN, MPE_MOD_POD,
				"%s::releaseProgramIndexRow programIdx = %d, rowPtr->authorizedPids was NULL...\n",
				PODMODULE, programIdx);
    }

    if(programIndexTable.transportStreamsUsed)
    {
    	programIndexTable.transportStreamsUsed--;
    }
	else
	{
		MPE_LOG(MPE_LOG_WARN, MPE_MOD_POD,
				"%s::releaseProgramIndexRow programIdx = %d, rowPtr->transportStreamsUsed was 0...\n",
				PODMODULE, programIdx);
	}

    // save transaction Id
    transId = rowPtr->transactionId;

    memset(rowPtr, 0, sizeof(mpe_ProgramIndexTableRow));

    // restore transaction Id
    rowPtr->transactionId = transId;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::releaseProgramIndexRow programsUsed=%d elementaryStreamsUsed=%d\n", PODMODULE, programIndexTable.programsUsed, programIndexTable.elementaryStreamsUsed);

    return MPE_SUCCESS;
}

mpe_Error releasePODDecryptSessionAndResources(uint8_t programIdx,
        mpe_Bool sendSessionEvent, mpe_PodDecryptSessionEvent event,
        mpe_Bool sendResourceEvent)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_PODDecryptSessionHandle releasedSessionHandle;
    uint8_t numHandles = 0;
    int i;
    // Random size array
    mpe_PODDecryptSessionHandle handles[10];

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::releasePODDecryptSessionAndResources for programIdx:%d .\n",
            PODMODULE, programIdx);

    // There may be multiple requests per program index table row
    if ((retCode = getDecryptSessionsForProgramIndex(programIdx, &numHandles, handles)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable error retrieving decrypt sessions for program index:%d.\n",
                PODMODULE, programIdx);
        return retCode;
    }

    for(i=0;i<numHandles;i++)
    {
        releasedSessionHandle = handles[i];

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "%s: releasePODDecryptSession, releasedSessionHandle=%p\n", PODMODULE,
                releasedSessionHandle);

        if (sendSessionEvent && retCode == MPE_SUCCESS)
        {
            retCode = sendJniDecryptSessionEvent(event, releasedSessionHandle, 0);
        }

        if (sendResourceEvent && retCode == MPE_SUCCESS)
        {
            sendPodEvent(MPE_POD_EVENT_RESOURCE_AVAILABLE, releasedSessionHandle, 0);
        }
    }
    return retCode;
}

/* designed to be called inside a acquire mutex block */
void removeAllDecryptSessionsAndResources(mpe_PodDecryptSessionEvent event)
{
    int idx;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "%s: releasePODDecryptSession, programIndexTable.numRows=%d\n",
            PODMODULE, programIndexTable.numRows);

    /*  When card is removed, signal to all sessions that the cable card has been removed */
    for (idx = 0; idx < programIndexTable.numRows; idx++)
    {
        mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[idx];
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_POD,
                "%s: releasePODDecryptSession, &programIndexTable.rows[%d]=%p\n",
                PODMODULE, idx, rowPtr);

        //if (rowPtr->sessionHandle != NULL)
       // {
            /* remove and signal event passed in. */
        //    releasePODDecryptSessionAndResources(idx, TRUE, event, FALSE);
        //}
    }

}

static mpe_Error getDecryptSessionsForProgramIndex(uint8_t programIdx, uint8_t *numHandles, mpe_PODDecryptSessionHandle handle[])
{
    LINK *lp = NULL;
    int i=0;

    if(handle == NULL)
        return MPE_EINVAL;

    if(requestTable == NULL)
    {
        return MPE_SUCCESS;
    }

    // Finds all the matching requests with the program index
    lp = llist_first(requestTable);
    while(lp)
    {
        mpe_RequestTableRow *row = (mpe_RequestTableRow *)llist_getdata(lp);
        if(row && (row->programIdx == programIdx))
        {
            handle[i++]= (mpe_PODDecryptSessionHandle)row;
        }
        lp = llist_after(lp);
    }

    *numHandles = i;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::getDecryptSessionsForProgramIndex numHandles:%d\n", PODMODULE,
                                        *numHandles);
    return MPE_SUCCESS;
}

static mpe_Error getDecryptRequestProgramIndex(mpe_PODDecryptSessionHandle handle, uint8_t *programIdxPtr)
{
    mpe_RequestTableRow *row = (mpe_RequestTableRow *)handle;

    if (handle == NULL)
        return MPE_EINVAL;

    *programIdxPtr = row->programIdx;

    return MPE_SUCCESS;
}

/*
 * This methods checks if the given program index is in use
 * by any of the decryt requests.
 */
static mpe_Bool isProgramIndexInUse(uint8_t programIdx)
{
    mpe_RequestTableRow *row = NULL;
    LINK *lp = NULL;
    mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[programIdx];

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::isProgramIndexInUse programIdx:%d\n", PODMODULE,
            programIdx);

    lp = llist_first(requestTable);
    while (lp)
    {
        // State check is not necessary
        row = (mpe_RequestTableRow *) llist_getdata(lp);
        if(row->programIdx == programIdx
                && ACTIVE_DECRYPT_STATE(rowPtr->state))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::isProgramIndexInUse returning TRUE\n", PODMODULE);
            return TRUE;
        }
        lp = llist_after(lp);
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::isProgramIndexInUse returning FALSE\n", PODMODULE);
    return FALSE;
}

static void updateProgramIndexTablePriority(uint8_t idx, uint8_t priority)
{
    mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[idx];
    if(rowPtr->priority < priority)
        rowPtr->priority = priority;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::updateProgramIndexTablePriority priority:%d\n", PODMODULE,
                                       rowPtr->priority);
}

static void updatePriority(uint8_t idx)
{
    uint8_t saved = 0;
    mpe_RequestTableRow *request = NULL;
    mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[idx];
    LINK *lp = NULL;

    saved = rowPtr->priority;
    lp = llist_first(requestTable);
    while (lp)
    {
        request = (mpe_RequestTableRow *) llist_getdata(lp);
        if(request->programIdx == idx)
        {
            if(request->priority < saved)
            {
                saved = rowPtr->priority = request->priority;
            }
        }
        lp = llist_after(lp);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::updatePriority priority:%d\n", PODMODULE,
                                       rowPtr->priority);
}

/*
 * This methods dispatches a decrypt event with the associated session handle
 * to callers
 */
mpe_Error sendJniDecryptSessionEvent(mpe_PodDecryptSessionEvent event,
        mpe_PODDecryptSessionHandle handle, uint32_t edEventData3)
{
    mpe_RequestTableRow *row = (mpe_RequestTableRow *)handle;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "%s: sendJniDecryptSessionEvent() Event=0x%x handle=0x%p\n",
            PODMODULE, event, handle);

    // now send it to the JNI level, the EdHandle is by convention the second parameter
    return mpe_eventQueueSend(row->eventQ, event,
            (void*) handle,
            row->act, edEventData3);

    return MPE_SUCCESS;
}

mpe_Error sendPodEvent(mpe_PodEvent event, void *edEventData1,
        uint32_t edEventData3)
{
    mpe_Error retCode = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "%s: sendPodEvent: eventData1 0x%p, eventData3 0x%x\n",
            PODMODULE, edEventData1, edEventData3);

    /* notify sideways (MPE level) listeners */
    notifyMPELevelRegisteredPodEventQueues(event, edEventData1, NULL,
            edEventData3);

    if (podJniPodEventEdHandle)
    {
        /* now send it to the JNI level, the EdHandle is by convention the fourth parameter */
        retCode = mpe_eventQueueSend(podJniPodEventEdHandle->eventQ, event,
                edEventData1, (void*) podJniPodEventEdHandle, edEventData3);
    }

    return retCode;
}

/*
 * This methods finds an active request based on the tuner number and program number
 * for sharing with an incoming request.
 */
static mpe_Error findActiveDecryptRequestForReuse(uint8_t tunerId, uint16_t programNumber, int *index)
{
    mpe_RequestTableRow *request = NULL;
    LINK *lp = NULL;

    if (tunerId == 0 || programNumber == 0)
        return MPE_EINVAL;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::findActiveDecryptRequestForReuse tunerId:%d programNumber:%d\n", PODMODULE,
            tunerId, programNumber);

    lp = llist_first(requestTable);
    while (lp)
    {
        request = (mpe_RequestTableRow *) llist_getdata(lp);
        if (request != NULL)
        {
            mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[request->programIdx];

            // Match based on tunerId, program number and state
            if(request->tunerId == tunerId
                    &&  rowPtr->programNum == programNumber
                    &&  request->state == MPE_POD_DECRYPT_REQUEST_STATE_ACTIVE)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::findActiveDecryptRequestForReuse returning program index:%d \n", PODMODULE, request->programIdx);
                *index = request->programIdx;
                return MPE_SUCCESS;
            }
        }
        lp = llist_after(lp);
    }
    return MPE_EINVAL;
}

static uint8_t getSuspendedRequests()
{
    LINK *lp = NULL;
    uint8_t count = 0;
    mpe_RequestTableRow *request = NULL;
    lp = llist_first(requestTable);

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::getSuspendedRequests...\n", PODMODULE);

    while (lp)
    {
        request = (mpe_RequestTableRow *) llist_getdata(lp);
        if (request != NULL
                && request->state == MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES)
        {
            count++;
        }
        lp = llist_after(lp);
    }
    return count;
}

/*
 * This methods finds a suspended request with the highest priority.
 * Returns the associated tunerId and service handle.
 */
static mpe_Error findNextSuspendedRequest(uint8_t *tunerId, mpe_SiServiceHandle  *handle)
{
    mpe_RequestTableRow *request = NULL;
    uint8_t saved = 0;
    int found = 0;
    LINK *lp = NULL;
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t numStreams=0;

    if(requestTable == NULL)
    {
        return MPE_SUCCESS;
    }
    lp = llist_first(requestTable);

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::findSuspendedRequest...\n", PODMODULE);

    while (lp)
    {
        request = (mpe_RequestTableRow *) llist_getdata(lp);
        if (request != NULL
                && request->state == MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES)
        {
            if(request->priority > saved)
            {
                saved = request->priority;
                *tunerId = request->tunerId;
                *handle = request->serviceHandle;

                /* Check resource availability */
                if ((retCode = mpe_siLockForRead()) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                            "<%s::findSuspendedRequest> - Could not lock SI err=%d\n", PODMODULE,
                            retCode);
                    return MPE_EINVAL;
                }

                if ((retCode = mpe_siGetNumberOfComponentsForServiceHandle(request->serviceHandle,
                            &numStreams)) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                            "%s::findSuspendedRequest Could not get number of components for service handle 0x%x\n",
                            PODMODULE, request->serviceHandle);
                }

                if ((retCode = mpe_siUnLock()) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                            "<%s::findSuspendedRequest> - Could not unlock SI err=%d\n", PODMODULE,
                            retCode);
                }
                /* Done with SI DB */

                if(checkAvailableResources(*tunerId, numStreams))
                {
                    // We can service this request with available resources
                    found = 1;
                }
            }
        }
        lp = llist_after(lp);
    }

    if(found)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::findSuspendedRequest...returning tunerId:%d handle:0x%x priority:%d\n",
                                             PODMODULE, *tunerId, *handle, saved);
        return MPE_SUCCESS;
    }
    return MPE_EINVAL;
}

/*
 * This methods appends the new request to the request table
 */
static void addRequestToTable(mpe_RequestTableRow *newRequest)
{
    LINK *lp = llist_mklink((void *) newRequest);
    llist_append(requestTable, lp);

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_POD,
            "<%s::addRequestToTable> - New request added to table, count=%lu\n",
            PODMODULE, llist_cnt(requestTable));
}


/*
 * This methods removes a request from the request table.
 * The link is removed from the table but not de-allocated.
 */
static void removeRequestFromTable(mpe_RequestTableRow *request)
{
    LINK *lp = NULL;
    if(request == NULL)
        return;

    lp = llist_linkof(requestTable, (void *)request);
    if(lp)
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: removeRequestFromTable, request=0x%p\n", PODMODULE, request);

        llist_rmlink(lp);

        MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_POD,
            "<%s::removeRequestFromTable> - request table count=%lu\n",
            PODMODULE, llist_cnt(requestTable));
    }
}

/*
 * This methods finds the given decrypt request (identified by the handle)
 * in the request table.
 */
static mpe_Bool findRequest(mpe_PODDecryptSessionHandle handle)
{
    mpe_RequestTableRow *row = NULL;
    LINK *lp = llist_first(requestTable);
    while (lp)
    {
        row = (mpe_RequestTableRow *) llist_getdata(lp);
        if (row != NULL)
        {
            if((mpe_PODDecryptSessionHandle)row == handle)
            {
                return TRUE;
            }
        }
        lp = llist_after(lp);
    }
    return FALSE;
}

static mpe_Error findRequestProgramIndexWithLowerPriority(mpe_RequestTableRow* requestPtr, int *programIdx)
{
    uint8_t saved;
    int found = 0;
    int idx;
    saved = requestPtr->priority;

    MPE_LOG(MPE_LOG_INFO,
            MPE_MOD_POD,
            "<%s::findRequestProgramIndexWithLowerPriority> ...\n", PODMODULE);

    for (idx = 0; idx < programIndexTable.numRows; idx++)
    {
        mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[idx];
        // Find a program index table row that has a lower priority.
        // and state is one of the active states
        if(rowPtr->priority && (rowPtr->priority < saved)
                        && ACTIVE_DECRYPT_STATE(rowPtr->state))
        {
            saved = rowPtr->priority;
            *programIdx = idx;
            found = 1;
        }
    }
    if(found)
    {
        MPE_LOG(MPE_LOG_DEBUG,
                MPE_MOD_POD,
                "<%s::findRequestProgramIndexWithLowerPriority> - Found priority = %d @ programIdx = %d\n", PODMODULE, saved, *programIdx);
        return MPE_SUCCESS;
    }
    return MPE_EINVAL;
}

static void activateSuspendedRequests(uint8_t programIndex)
{
    uint8_t tunerId=0;
    uint32_t progNum=0;
    uint32_t sourceId=0;
    uint8_t transactionId;
    uint8_t priority=0;
    mpe_SiServiceHandle seviceHandle = MPE_SI_INVALID_HANDLE;
    uint8_t ca_cmd_id = 0;
    mpe_RequestTableRow *request = NULL;
    mpe_Error retCode = MPE_SUCCESS;
    LINK *lp;
    uint32_t numStreams = 0;
    mpe_PODStreamDecryptInfo *streamInfo;
    uint16_t ecmPid;
    uint8_t ltsId = LTSID_UNDEFINED;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "<%s::activateSuspendedRequests> - Enter...programIndex: %d\n", PODMODULE, programIndex);

    // Find a suspended request that can be serviced with the available resources
    if(findNextSuspendedRequest(&tunerId, &seviceHandle) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "<%s::activateSuspendedRequests> - No suspended requests found...\n", PODMODULE);
        return;
    }

    lp = llist_first(requestTable);

    while (lp)
    {
        request = (mpe_RequestTableRow *) llist_getdata(lp);
        if (request != NULL)
        {
            // Find requests that share common data (servicehandle, tunerId etc.)
            // Priorities may be different, but the higher priority will be
            // represented in the program index table row
            if(request->tunerId == tunerId && request->serviceHandle == seviceHandle)
            {
                // Set the program index and state
                request->programIdx = programIndex;
                request->state = MPE_POD_DECRYPT_REQUEST_STATE_ACTIVE;
                // priority of the program index table row should be higher of the request priorities
                // if there are multiple requests sharing a session
                if(request->priority > priority)
                    priority = request->priority;
                tunerId = request->tunerId;
                // When re-activating suspended requests
                // Use a new 'random' ltsid
                if(ltsId == LTSID_UNDEFINED)
                {
                    ltsId = getLtsidForTunerId(request->tunerId);
                    // If an existing ltsid for this tuner is not found get the next 'random' ltsid
                    if(request->ltsId == LTSID_UNDEFINED)
                    {
                    	// getLtsid() validates that the new ltsid is not in use by any other
                    	// program index rows
                    	ltsId = getLtsid();
                    }
                }
                // Requests sharing servicehandle and tunerId share the same ltsid
                request->ltsId = ltsId;
                seviceHandle = request->serviceHandle;
                ca_cmd_id = request->ca_pmt_cmd_id;
            }
        }
        lp = llist_after(lp);
    }

    /* SI DB read lock */
    if ((retCode = mpe_siLockForRead()) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "<%s::activateSuspendedRequests> - Could not lock SI err=%d\n", PODMODULE,
                retCode);
        return;
    }
    if ((retCode = mpe_siGetSourceIdForServiceHandle(seviceHandle,
                &sourceId)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s::activateSuspendedRequests Could not get sourceId for service handle 0x%x\n",
                PODMODULE, seviceHandle);
    }

    if ((retCode = mpe_siGetProgramNumberForServiceHandle(seviceHandle,
                &progNum)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s::activateSuspendedRequests Could not get program number for service handle 0x%x\n",
                PODMODULE, seviceHandle);
    }

    if ((retCode = mpe_siGetNumberOfComponentsForServiceHandle(seviceHandle,
                &numStreams)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s::activateSuspendedRequests Could not get number of components for service handle 0x%x\n",
                PODMODULE, seviceHandle);
    }

    if ((retCode = mpe_memAllocP(MPE_MEM_POD, (numStreams * sizeof(mpe_PODStreamDecryptInfo)), (void**) &streamInfo)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s::activateSuspendedRequests - Could not malloc memory for streamInfo \n",
                PODMODULE);
    }

    // Check if there are any CA descriptors for this handle
    // This method populates elementary stream Pids associated with
    // the service. The CA status field is set to unknown. It will
    // be filled when ca reply/update is received from CableCARD.
    if(mpe_siCheckCADescriptors(seviceHandle, ca_system_id,
                                      &numStreams, &ecmPid, streamInfo) != TRUE)
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                "<%s::activateSuspendedRequests> - No CA descriptors for service handle: 0x%x (could be analog, or clear channel..\n",
                PODMODULE, seviceHandle);
        // What to do here?
    }

    if (mpe_siUnLock() != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "<%s::activateSuspendedRequests> - Could not unlock SI\n", PODMODULE);
    }
    // Done with SI DB, release the lock

    // Set the transaction id
    transactionId = getNextTransactionIdForProgramIndex(programIndex);

    // If the request was never started the command_id is 0
    // Set it to query
    if(ca_cmd_id == 0)
        ca_cmd_id = MPE_MPOD_CA_QUERY;
        
    retCode = createAndSendCaPMTApdu(seviceHandle,
              programIndex, transactionId,
              ltsId,
              ca_cmd_id);

    if(retCode == MPE_SUCCESS)
    {
        // set state based on the previous command id
        commitProgramIndexTableRow(programIndex, ca_cmd_id,
                                   progNum, sourceId,
                                   transactionId, tunerId, ltsId,
                                   priority, numStreams, ecmPid,
                                   streamInfo, caCommandToState(ca_cmd_id));

        // If it was de-scrambling earlier we are going to issue a
        // MPE_MPOD_CA_OK_DESCRAMBLE apdu and signal an event
        if(ca_cmd_id == MPE_MPOD_CA_OK_DESCRAMBLE)
        {
            // Set all Pids state to authorized
            LINK *lp1;
            mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[programIndex];
            lp1 = llist_first(rowPtr->authorizedPids);
            while(lp1)
            {
                mpe_PODStreamDecryptInfo *pidInfo = (mpe_PODStreamDecryptInfo *)llist_getdata(lp1);
                if(pidInfo)
                {
                    pidInfo->status = CA_ENABLE_DESCRAMBLING_NO_CONDITIONS;
                }
                lp1 = llist_after(lp1);
            }
            sendEvent(MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED, programIndex);
        }
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "<%s::activateSuspendedRequests> - Error generating CA_PMT (error 0x%x)\n",
                PODMODULE, retCode);
    }

    // De-allocate streamInfo
    if(streamInfo != NULL)
    {
        mpe_memFreeP(MPE_MEM_POD, streamInfo);
        streamInfo = NULL;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "<%s::activateSuspendedRequests> - programIndex:%d, done...\n", PODMODULE, programIndex);
}

static uint8_t caCommandToState(uint8_t cmd_id)
{
    if(cmd_id == MPE_MPOD_CA_OK_DESCRAMBLE)
        return MPE_POD_DECRYPT_STATE_DESCRAMBLING;
    else if(cmd_id == MPE_MPOD_CA_OK_MMI)
        return MPE_POD_DECRYPT_STATE_ISSUED_MMI;
    else if(cmd_id == MPE_MPOD_CA_QUERY)
        return MPE_POD_DECRYPT_STATE_ISSUED_QUERY;
    else if(cmd_id == MPE_MPOD_CA_NOT_SELECTED)
        return MPE_POD_DECRYPT_STATE_NOT_SELECTED;
    else
        return MPE_POD_DECRYPT_STATE_NOT_SELECTED;
}

/*
 * This methods returns the current transaction id for the given program index.
 */
static uint8_t getTransactionIdForProgramIndex(uint8_t programIdx)
{
    mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[programIdx];

    return rowPtr->transactionId;
}

/*
 * This methods returns the next transaction id for the given program index.
 *
 * Transaction id is an 8-bit value generated by the host and is returned
 * in the ca_pmt_reply() and/or ca_update() apdu from the CableCARD.
 * The host should increment the value, modulo 255, with every message.
 * A separate transaction id counter is maintained for each program index,
 * so that the transaction ids increment independently for each index.
 */
static uint8_t getNextTransactionIdForProgramIndex(uint8_t programIdx)
{
    uint8_t transactionId = getTransactionIdForProgramIndex(programIdx) + 1;

    MPE_LOG(
        MPE_LOG_DEBUG,
        MPE_MOD_POD,
        "<%s::getNextTransactionIdForProgramIndex> - transaction id=%d\n",
        PODMODULE, (transactionId % 255));

    return (transactionId % 255);
}

static void logProgramIndexTable(char* localStr)
{
    int idx;
    int i;
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::programIndexTable %s\n", PODMODULE,
            localStr);

    MPE_LOG(
            MPE_LOG_INFO,
            MPE_MOD_POD,
            "\t<programIndexTable>\t\t: rowsUsed=%d, numRows=%d\n",
            programIndexTable.programsUsed, programIndexTable.numRows);

    for (idx = 0; idx < programIndexTable.numRows; idx++)
    {
        mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[idx];
        LINK *lp;

        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_POD,
                "\t<programIndexTable.row[%d]>\t: transactionId=%d, ca_pmt_cmd_id=%s, ltsid=%d, programNum=%d, sourceId=0x%x, priority=%d, state=%s\n",
                idx, rowPtr->transactionId, caCommandString(rowPtr->ca_pmt_cmd_id),
                rowPtr->ltsid, rowPtr->programNum,
                rowPtr->sourceId, rowPtr->priority, programIndexTableStateString(rowPtr->state));

        if(rowPtr->authorizedPids == NULL)
            continue;

        lp = llist_first(rowPtr->authorizedPids);
        i=0;
        while(lp)
        {
            mpe_PODStreamDecryptInfo *pidInfo = (mpe_PODStreamDecryptInfo *)llist_getdata(lp);
            if(pidInfo)
            {
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "\t\t\t\t\t<authorized Pids>[%d] pid:0x%x status:%d\n",
                        i, pidInfo->pid, pidInfo->status);
                i++;
            }
            lp = llist_after(lp);
        }
    }
}

static char* programIndexTableStateString(uint8_t state)
{
    if (state == MPE_POD_DECRYPT_STATE_NOT_SELECTED)
        return "MPE_POD_DECRYPT_STATE_NOT_SELECTED";
    else if (state == MPE_POD_DECRYPT_STATE_ISSUED_QUERY)
        return "MPE_POD_DECRYPT_STATE_ISSUED_QUERY";
    else if (state == MPE_POD_DECRYPT_STATE_ISSUED_MMI)
        return "MPE_POD_DECRYPT_STATE_ISSUED_MMI";
    else if (state == MPE_POD_DECRYPT_STATE_DESCRAMBLING)
        return "MPE_POD_DECRYPT_STATE_DESCRAMBLING";
    else if (state == MPE_POD_DECRYPT_STATE_FAILED_DESCRAMBLING)
        return "MPE_POD_DECRYPT_STATE_FAILED_DESCRAMBLING";
    else
        return "MPE_POD_DECRYPT_STATE_NOT_SELECTED ";
}

static void logRequestTable(char* localStr)
{
    int idx = 0;
    mpe_RequestTableRow *row = NULL;
    LINK *lp = NULL;

    if(requestTable == NULL)
    {
        return;
    }
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::logRequestTable %s\n", PODMODULE,
            localStr);

    lp = llist_first(requestTable);
    while (lp)
    {
        row = (mpe_RequestTableRow *) llist_getdata(lp);
        if (row != NULL)
        {
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "\t<requestTable[%d]>\t: programIndex=%d, tunerId=%d, ltsId=%d, priority=%d, sourceId=0x%x, state=%s mmiEnable=%d serviceHandle=0x%x\n",
                    idx, row->programIdx, row->tunerId, row->ltsId, row->priority, row->sourceId,
                    requestTableStateString(row->state), row->mmiEnable, row->serviceHandle);
            idx++;
        }
        lp = llist_after(lp);
    }
}

static char* requestTableStateString(uint8_t state)
{
    if (state == MPE_POD_DECRYPT_REQUEST_STATE_ACTIVE)
        return "MPE_POD_DECRYPT_REQUEST_STATE_ACTIVE";
    else if (state == MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES)
        return "MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES";
    else
        return "MPE_POD_DECRYPT_REQUEST_STATE_UNKNOWN";
}

char* mpe_podCAEventString(mpe_PodDecryptSessionEvent event)
{
    if (event == MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_ENTITLEMENT)
        return "MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_ENTITLEMENT";
    else if (event == MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_RESOURCES)
        return "MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_RESOURCES";
    if (event == MPE_POD_DECRYPT_EVENT_MMI_PURCHASE_DIALOG)
        return "MPE_POD_DECRYPT_EVENT_MMI_PURCHASE_DIALOG";
    else if (event == MPE_POD_DECRYPT_EVENT_MMI_TECHNICAL_DIALOG)
        return "MPE_POD_DECRYPT_EVENT_MMI_TECHNICAL_DIALOG";
    if (event == MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED)
        return "MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED";
    else if (event == MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN)
        return "MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN";
    else
        return "UNKNOWN ";
}

static char* caCommandString(uint8_t ca_pmt_cmd)
{
    if (ca_pmt_cmd == MPE_MPOD_CA_OK_DESCRAMBLE)
        return "MPE_MPOD_CA_OK_DESCRAMBLE";
    else if (ca_pmt_cmd == MPE_MPOD_CA_OK_MMI)
        return "MPE_MPOD_CA_OK_MMI";
    else if(ca_pmt_cmd == MPE_MPOD_CA_QUERY)
        return "MPE_MPOD_CA_QUERY";
    else if (ca_pmt_cmd == MPE_MPOD_CA_NOT_SELECTED)
        return "MPE_MPOD_CA_NOT_SELECTED";
    else
        return " ";
}

#define APDU_LEN_SIZE_INDICATOR_IDX APDU_TAG_SIZE /* Beginning of length section, Zero based index */
#define CA_REPLY_OUTER_CA_ENABLE_DATA_OFFSET     (7)   /* if data starts at 6, the outer ca_enable will be at 13 (6+7) */
#define CA_UPDATE_OUTER_CA_ENABLE_DATA_OFFSET    (7)   /* if data starts at 6, the outer ca_enable will be at 13 (6+7) */

static mpe_Bool isCaInfoReplyAPDU(uint32_t sessionId, uint8_t* apdu)
{
    if (sessionId == casSessionId)
    {
        uint32_t apduTag = getApduTag(apdu);

        if (apduTag == CA_INFO_TAG)
            return TRUE;
    }
    return FALSE;
}

static mpe_Bool isCaReplyAPDU(uint32_t sessionId, uint8_t* apdu)
{
    if (sessionId == casSessionId)
    {
        uint32_t apduTag = getApduTag(apdu);

        if (apduTag == CA_PMT_REPLY_TAG)
            return TRUE;
    }
    return FALSE;
}

static mpe_Bool isCaUpdateAPDU(uint32_t sessionId, uint8_t* apdu)
{
    if (sessionId == casSessionId)
    {
        uint32_t apduTag = getApduTag(apdu);

        if (apduTag == CA_PMT_UPDATE_TAG)
            return TRUE;
    }
    return FALSE;
}

static uint32_t getApduTag(uint8_t *apdu)
{
    uint32_t tag = (apdu[0] << 16) & 0x00FF0000;
    tag |= (apdu[1] << 8);
    tag |= apdu[2];

    return tag;
}

/* length does not include tag or length_field() itself.
 * maximum valid length returned is 0xFFFF
 */
static int32_t getApduDataLen(uint8_t *apdu)
{
    uint8_t size_byte = apdu[APDU_LEN_SIZE_INDICATOR_IDX];
    uint32_t length_field_size = size_byte & 0x7F;
    uint32_t length_value = 0;

    if (size_byte & 0x80)
    {
        int idx;

        if (length_field_size > 2)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: getApduLen, length_field_size > 2, size=%d\n",
                    PODMODULE, programIndexTable.numRows);

            return -1;
        }

        /* CCIF2.0:  "bytes shall be concatenated, first byte at the most significant end, to encode an integer value." */
        for (idx = 1; idx <= 2; idx++)
        {
            /* 8 bits bslbf */
            length_value |= apdu[APDU_LEN_SIZE_INDICATOR_IDX + idx]
                    << ((length_field_size - idx) * 8);
        }
    }
    else
    {
        length_value = length_field_size;
    }
    return length_value;
}

static uint16_t getApduDataOffset(uint8_t* apdu)
{
    uint16_t dataIdx = APDU_LEN_SIZE_INDICATOR_IDX + 1;

    int8_t size_field = apdu[APDU_LEN_SIZE_INDICATOR_IDX];
    if (size_field & 0x80)
    {
        dataIdx += (size_field & 0x7F);
    }

    return dataIdx;
}

static uint8_t getProgramIdxFromCaReply(uint8_t* apdu)
{
    uint16_t program_idx_offset = getApduDataOffset(apdu);

    return apdu[program_idx_offset];
}

static uint8_t getTransactionIdFromCaReply(uint8_t* apdu)
{
    uint16_t program_idx_offset = getApduDataOffset(apdu);
    return apdu[program_idx_offset + 1];
}

static uint16_t getCASystemIdFromCaReply(uint8_t* apdu)
{
    uint16_t system_id = 0;
    system_id =    apdu[APDU_TAG_SIZE + 1] << 8;
    system_id |= apdu[APDU_TAG_SIZE + 2];

    return system_id;
}

static uint8_t getOuterCaEnableFromCaReply(uint8_t* apdu)
{
    uint16_t data_offset = getApduDataOffset(apdu);
    uint16_t outer_offset = data_offset + CA_REPLY_OUTER_CA_ENABLE_DATA_OFFSET;
    return apdu[outer_offset];
}

static void getInnerCaEnablesFromCaReply(uint8_t *apdu, uint16_t *numPids,
        uint16_t elemStreamPidArray[], uint8_t caEnableArray[])
{
    uint16_t data_offset = getApduDataOffset(apdu);

    // outer data is not valid, but space is still used.
    uint16_t outer_offset = data_offset + CA_REPLY_OUTER_CA_ENABLE_DATA_OFFSET;

    // beginning of inner data loop
    uint16_t apdu_idx = outer_offset + 1;

    uint16_t array_idx = 0;
    uint16_t data_size = getApduDataLen(apdu);
    uint16_t apdu_size = data_offset + data_size;
    uint16_t es_pid = 0;
    uint8_t ca_enable_byte = 0;
    uint8_t ca_enable = 0;

    while (apdu_idx < apdu_size)
    {
        es_pid = (apdu[apdu_idx++] << 8); // & 0x1FFF;  // upper 3 bits are reserved, so set them to zero
        es_pid |= apdu[apdu_idx++];     // lower 8 bits
        ca_enable_byte = apdu[apdu_idx++];
        if ((ca_enable_byte & CA_ENABLE_SET) > 0)
        {
            ca_enable = ca_enable_byte & 0x7F;
            elemStreamPidArray[array_idx] = es_pid;
            caEnableArray[array_idx] = ca_enable;
            array_idx++;
        }
    }
    *numPids = array_idx;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::getInnerCaEnablesFromCaReply numPids:%d.\n",
            PODMODULE, *numPids);
}

static mpe_Error setPidArrayForProgramIndex(uint8_t programIdx, uint16_t numPids, uint16_t elemStreamPidArray[], uint8_t caEnableArray[])
{
    int i=0;
    LINK *lp;
    uint8_t ca_enable = 0;
    mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[programIdx];

    MPEOS_LOG(MPE_LOG_INFO,
            MPE_MOD_POD,
            "%s::setPidArrayForProgramIndex - programIdx=%d numPids=%d\n", PODMODULE, programIdx, numPids);

    lp = llist_first(rowPtr->authorizedPids);
    i=0;
    while(lp)
    {
        mpe_PODStreamDecryptInfo *pidInfo = (mpe_PODStreamDecryptInfo *)llist_getdata(lp);
        if(pidInfo)
        {
            if (elemStreamPidArray == NULL && numPids == 0) /* program level outer ca_enable byte */
            {
                ca_enable = caEnableArray[0] & 0x7F;
                pidInfo->status = ca_enable;
            }
            else if(numPids >0 && (elemStreamPidArray != NULL) && (caEnableArray != NULL))
            {
                pidInfo->pid = elemStreamPidArray[i];
                pidInfo->status = caEnableArray[i];
                i++;

                if(i == numPids)
                    break;
            }

            MPEOS_LOG(MPE_LOG_DEBUG,
                    MPE_MOD_POD,
                    "setPidArrayForProgramIndex - pid:%d ca_status:%d \n", pidInfo->pid, pidInfo->status);
        }
        lp = llist_after(lp);
    }

    MPEOS_LOG(MPE_LOG_DEBUG,
            MPE_MOD_POD,
            "setPidArrayForProgramIndex - pid count:%lu \n", llist_cnt(rowPtr->authorizedPids));
    return MPE_SUCCESS;
}

static mpe_PodDecryptSessionEvent getLastEventForProgramIndex(uint8_t programIndex)
{
    mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[programIndex];
    return  rowPtr->lastEvent;
}

static uint8_t getProgramIdxFromCaUpdate(uint8_t* apdu)
{
    uint16_t program_idx_offset = getApduDataOffset(apdu);
    return apdu[program_idx_offset];
}

static uint8_t getTransactionIdFromCaUpdate(uint8_t* apdu)
{
    uint16_t program_idx_offset = getApduDataOffset(apdu);
    return apdu[program_idx_offset + 1];
}

static uint8_t getOuterCaEnableFromCaUpdate(uint8_t* apdu)
{
    uint16_t data_offset = getApduDataOffset(apdu);
    uint16_t outer_offset = data_offset + CA_UPDATE_OUTER_CA_ENABLE_DATA_OFFSET;
    return apdu[outer_offset];
}

static void getInnerCaEnablesFromCaUpdate(uint8_t *apdu, uint16_t *numPids,
        uint16_t elemStreamPidArray[], uint8_t caEnableArray[])
{
    uint16_t data_offset = getApduDataOffset(apdu);

    // outer data is not valid, but space is still used.
    uint16_t outer_offset = data_offset + CA_UPDATE_OUTER_CA_ENABLE_DATA_OFFSET;

    // beginning of inner data loop
    uint16_t apdu_idx = outer_offset + 1;

    uint16_t array_idx = 0;
    uint16_t data_size = getApduDataLen(apdu);
    uint16_t apdu_size = data_offset + data_size;
    uint16_t es_pid = 0;
    uint8_t ca_enable_byte = 0;
    uint8_t ca_enable = 0;

    while (apdu_idx < apdu_size)
    {
        es_pid = (apdu[apdu_idx++] << 8); // & 0x1FFF;  // upper 3 bits are reserved, so set them to zero
        es_pid |= apdu[apdu_idx++]; // lower 8 bits
        ca_enable_byte = apdu[apdu_idx++];
        if ((ca_enable_byte & CA_ENABLE_SET) > 0)
        {
            ca_enable = ca_enable_byte & 0x7F;
            elemStreamPidArray[array_idx] = es_pid;
            caEnableArray[array_idx] = ca_enable;
            array_idx++;
        }
    }
    *numPids = array_idx;
}

static mpe_Error handleCaEnable(uint8_t ca_enable, uint8_t programIdx)
{
    mpe_Error retCode;
    mpe_PodDecryptSessionEvent event;
    uint8_t numHandles = 0;
    int i;
    mpe_Bool resourcesAvailable = FALSE;
    // Random size array
    mpe_PODDecryptSessionHandle handles[10];

    mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[programIdx];

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::handleCaEnable programIdx:%d ca_enable:0x%x.\n",
            PODMODULE, programIdx, ca_enable);

    // There may be multiple requests per program index table row
    if ((retCode = getDecryptSessionsForProgramIndex(programIdx, &numHandles, handles)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable error retrieving decrypt sessions for program index:%d.\n",
                PODMODULE, programIdx);
        return retCode;
    }

    switch (ca_enable)
    {
        case CA_ENABLE_DESCRAMBLING_NO_CONDITIONS: /* De-scrambling */
        {
            uint8_t transId;
            mpe_RequestTableRow *request;

            // Check the current state of program index table row
            if(rowPtr->state == MPE_POD_DECRYPT_STATE_DESCRAMBLING)
            {
                // Nothing to do, return...
                return MPE_SUCCESS;
            }

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, ca_enable:CA_ENABLE_DESCRAMBLING_NO_CONDITIONS.\n",
                    PODMODULE);
            
            event = MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED;

            if(rowPtr->state != MPE_POD_DECRYPT_STATE_DESCRAMBLING)
            {
                // If this is in response to a query or a ca_update() and
                // we have not sent a ca_pmt() APDU with 'ok_descramble' before
                // send one now and setup a CP session
                // otherwise just signal an event
                if(rowPtr->ca_pmt_cmd_id != MPE_MPOD_CA_OK_DESCRAMBLE)
                {
                    transId = getNextTransactionIdForProgramIndex(programIdx);

                    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                            "<%s::handleCaEnable> - Sending CA_PMT apdu (MPE_MPOD_CA_OK_DESCRAMBLE)\n",
                            PODMODULE);
                    // Even if multiple requests (handles) are sharing a decrypt session,
                    // the service handle and tuner Id will be same
                    request = (mpe_RequestTableRow *)handles[0];
                    if((retCode = createAndSendCaPMTApdu(request->serviceHandle, programIdx,
                            transId, rowPtr->ltsid, MPE_MPOD_CA_OK_DESCRAMBLE)) == MPE_SUCCESS)
                    {
                        // Start the CP session

                        // rowPtr->authorizedPids should never be null
                        if(rowPtr->authorizedPids != NULL  && llist_cnt(rowPtr->authorizedPids) != 0)
                        {
                            int index=0;
                            uint32_t numPids = llist_cnt(rowPtr->authorizedPids);
                            uint32_t *pidArray = NULL;
                            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable pidArray size:%d\n",
                                    PODMODULE, numPids);
                            if (mpe_memAllocP(MPE_MEM_POD, (sizeof(uint32_t) * numPids),
                                    (void**) &pidArray) == MPE_SUCCESS)
                            {
                                LINK *lp = llist_first(rowPtr->authorizedPids);
                                while(lp)
                                {
                                     mpe_PODStreamDecryptInfo *pidInfo = (mpe_PODStreamDecryptInfo *)llist_getdata(lp);
                                     if(pidInfo)
                                     {
                                         pidArray[index++] = pidInfo->pid;
                                         MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable pidArray[%d]:0x%x\n",
                                                 PODMODULE, index-1, pidArray[index-1]);
                                     }
                                     lp = llist_after(lp);
                                }

                                 // Setup CP session here
                                 retCode = mpeos_podStartCPSession( request->tunerId,
                                                                    rowPtr->programNum,
                                                                    rowPtr->ltsid,
                                                                    rowPtr->ecmPid,
                                                                    programIdx,
                                                                    numPids,
                                                                    pidArray,
                                                                    &(rowPtr->cpSession));
                                 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable created CP session:0x%x\n",
                                         PODMODULE, rowPtr->cpSession);
                                 if (retCode != MPE_SUCCESS)
                                 {
                                      MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "%s::handleCaEnable error setting up CP session...%d\n",
                                              PODMODULE, retCode);
                                      // What to do here?
                                 }
                            }
                        }
                        rowPtr->ca_pmt_cmd_id = MPE_MPOD_CA_OK_DESCRAMBLE;
                        rowPtr->transactionId = transId;
                        request->ca_pmt_cmd_id = MPE_MPOD_CA_OK_DESCRAMBLE;
                    }
                    else
                    {
                        // What to do here?
                        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                                "<%s::handleCaEnable> - Sending CA_PMT apdu failed..in state: %s\n",
                                PODMODULE, programIndexTableStateString(rowPtr->state));
                    }
                }

                // Log (SNMP) when de-scrambling is initiated!
                {
                    char *ocStbHostSystemLoggingEventTable_oid = "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.5";
                    mpe_TimeMillis current_time = 0;
                    char timeStampString[256];
                    char message[256] = " ";

                    // Record the current time
                    mpeos_timeGetMillis(&current_time);
                    // Start time = %"PRIu64" %"PRIu64"\n"
                    sprintf(timeStampString, "%"PRIu64, (uint64_t)current_time);
                    sprintf(message, "(Performance.ServiceSelection-INFO) De-scrambling Initiated: Tuner %d", rowPtr->tunerId);
                    // Log when de-scrambling begins
                    mpeos_dbgAddLogEntry(ocStbHostSystemLoggingEventTable_oid, timeStampString, message);
                }

                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, programIndexTableRow current state: %s, new state: MPE_POD_DECRYPT_STATE_DESCRAMBLING\n",
                        PODMODULE, programIndexTableStateString(rowPtr->state));

                rowPtr->state = MPE_POD_DECRYPT_STATE_DESCRAMBLING; /* Set the program index table entry state */
                rowPtr->lastEvent = event;

                sendEvent(event, programIdx);
            }

        }
        break; // END case:CA_ENABLE_DESCRAMBLING_NO_CONDITIONS

        case CA_ENABLE_DESCRAMBLING_WITH_PAYMENT:  /* De-scrambling possible with purchase */
        case CA_ENABLE_DESCRAMBLING_WITH_TECH:     /* De-scrambling possible with technical dialog */
        {
            uint8_t transId;
            mpe_RequestTableRow *request;

            // Signal events such that JMF can correctly enter into
            // alternate content presentation state
            if(ca_enable == CA_ENABLE_DESCRAMBLING_WITH_PAYMENT)
            {
                event = MPE_POD_DECRYPT_EVENT_MMI_PURCHASE_DIALOG;
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, ca_enable:CA_ENABLE_DESCRAMBLING_WITH_PAYMENT (0x2).\n",
                        PODMODULE);
            }
            else
            {
                event = MPE_POD_DECRYPT_EVENT_MMI_TECHNICAL_DIALOG;
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, ca_enable:CA_ENABLE_DESCRAMBLING_WITH_TECH (0x3).\n",
                        PODMODULE);
            }

            // Issue a ca_pmt() APDU with cmd_id 'ok_descramble'
            transId = getNextTransactionIdForProgramIndex(programIdx);

            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
                    "<%s::handleCaEnable> - Sending CA_PMT apdu (MPE_MPOD_CA_OK_DESCRAMBLE)\n",
                    PODMODULE);
            // Even if multiple requests (handles) are sharing a decrypt session,
            // the service handle and tuner Id will be same
            request = (mpe_RequestTableRow *)handles[0];
            if((retCode = createAndSendCaPMTApdu(request->serviceHandle, programIdx,
                    transId, rowPtr->ltsid, MPE_MPOD_CA_OK_DESCRAMBLE)) == MPE_SUCCESS)
            {
                // Start the CP session

                // rowPtr->authorizedPids should never be null
                if(rowPtr->authorizedPids != NULL  && llist_cnt(rowPtr->authorizedPids) != 0)
                {
                    int index=0;
                    uint32_t numPids = llist_cnt(rowPtr->authorizedPids);
                    uint32_t *pidArray = NULL;
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable pidArray size:%d\n",
                            PODMODULE, numPids);
                    if (mpe_memAllocP(MPE_MEM_POD, (sizeof(uint32_t) * numPids),
                            (void**) &pidArray) == MPE_SUCCESS)
                    {
                        LINK *lp = llist_first(rowPtr->authorizedPids);
                        while(lp)
                        {
                             mpe_PODStreamDecryptInfo *pidInfo = (mpe_PODStreamDecryptInfo *)llist_getdata(lp);
                             if(pidInfo)
                             {
                                 pidArray[index++] = pidInfo->pid;
                                 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable pidArray[%d]:0x%x\n",
                                         PODMODULE, index-1, pidArray[index-1]);
                             }
                             lp = llist_after(lp);
                        }

                         // Setup CP session here
                         retCode = mpeos_podStartCPSession( request->tunerId,
                                                            rowPtr->programNum,
                                                            rowPtr->ltsid,
                                                            rowPtr->ecmPid,
                                                            programIdx,
                                                            numPids,
                                                            pidArray,
                                                            &(rowPtr->cpSession));
                         MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable created CP session:0x%x\n",
                                 PODMODULE, rowPtr->cpSession);
                         if (retCode != MPE_SUCCESS)
                         {
                              MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "%s::handleCaEnable error setting up CP session...%d\n",
                                      PODMODULE, retCode);
                              // What to do here?
                         }
                    }
                }
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, programIndexTableRow current state: %s, new state: MPE_POD_DECRYPT_STATE_ISSUED_MMI\n",
                        PODMODULE, programIndexTableStateString(rowPtr->state));

                rowPtr->state = MPE_POD_DECRYPT_STATE_ISSUED_MMI; /* Set the program index table entry state */
                rowPtr->lastEvent = event;
                rowPtr->ca_pmt_cmd_id = MPE_MPOD_CA_OK_DESCRAMBLE;
                rowPtr->transactionId = transId;
                request->ca_pmt_cmd_id = MPE_MPOD_CA_OK_DESCRAMBLE;

                sendEvent(event, programIdx);
            }
            else
            {
                // What to do here?
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                        "<%s::handleCaEnable> - Sending CA_PMT apdu failed..in state: %s\n",
                        PODMODULE, programIndexTableStateString(rowPtr->state));
            }
        }
        break; // END case:CA_ENABLE_DESCRAMBLING_WITH_PAYMENT
               // END case:CA_ENABLE_DESCRAMBLING_WITH_TECH

        /*
         * De-scrambling not possible since there is no entitlement
         */
        case CA_ENABLE_DESCRAMBLING_PAYMENT_FAIL:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, ca_enable:CA_ENABLE_DESCRAMBLING_PAYMENT_FAIL (0x71).\n",
                    PODMODULE);

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, programIndexTableRow current state: %s, new state: MPE_POD_DECRYPT_STATE_FAILED_DESCRAMBLING\n",
                    PODMODULE, programIndexTableStateString(rowPtr->state));

            rowPtr->state = MPE_POD_DECRYPT_STATE_FAILED_DESCRAMBLING; // De-scrambling failed
            event = MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_ENTITLEMENT;
            rowPtr->lastEvent = event;
            rowPtr->ca_pmt_cmd_id = 0;

            sendEvent(event, programIdx);
            break;
        }
        /* de-scrambling not possible for technical reasons.  For example, all elementary stream available are being used */
        case CA_ENABLE_DESCRAMBLING_TECH_FAIL:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, ca_enable:CA_ENABLE_DESCRAMBLING_TECH_FAIL (0x73).\n",
                    PODMODULE);

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::handleCaEnable, programIndexTableRow current state: %s, new state: MPE_POD_DECRYPT_STATE_FAILED_DESCRAMBLING\n",
                    PODMODULE, programIndexTableStateString(rowPtr->state));

            rowPtr->state = MPE_POD_DECRYPT_STATE_FAILED_DESCRAMBLING; // De-scrambling failed
            event = MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_RESOURCES;
            rowPtr->lastEvent = event;
            rowPtr->ca_pmt_cmd_id = 0;

            sendEvent(event, programIdx);
            break;
        }
        default:
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::handleCaEnable unexpected error...\n", PODMODULE);
            break;
        }
    } // End switch (ca_enable)

    // If there are requests that are waiting for resources
    // Service those
    if(resourcesAvailable  && (getSuspendedRequests() > 0))
    {
        mpe_RequestTableRow *request = NULL;

        for(i=0;i<numHandles;i++)
        {
            request = (mpe_RequestTableRow*)handles[i];
            // Reset the requests state (Is this the right state here?)
            request->state = MPE_POD_DECRYPT_REQUEST_STATE_UNKNOWN;
            // Reset the program index??
            request->programIdx = -1;
            // Save the command id
            request->ca_pmt_cmd_id = rowPtr->ca_pmt_cmd_id;
        }

        // Now remove the corresponding entry from the program index table if no other
        // uses remain (based on the program index)
        // check if this programIndex is being used by any other decrypt request
        if(!isProgramIndexInUse(programIdx) && request)
        {
            uint8_t transId = getNextTransactionIdForProgramIndex(programIdx);
            // Send a ca_pmt apdu with 'not_selected' (0x04) when decrypt session
            // is no longer in use
            if((retCode = createAndSendCaPMTApdu(request->serviceHandle, programIdx,
                    transId, request->ltsId,
                    MPE_MPOD_CA_NOT_SELECTED)!= MPE_SUCCESS))
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::handleCaEnable> - Error sending CA_PMT apdu (MPE_MPOD_CA_NOT_SELECTED) (error %d)\n",
                        PODMODULE, retCode);
            }

            // Release the CP session
            if(rowPtr->cpSession != NULL)
            {
                if(MPE_SUCCESS != mpeos_podStopCPSession(rowPtr->cpSession))
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                            "%s::handleCaEnable Error stopping CP session...\n",
                            PODMODULE);
                }
            }

            rowPtr->transactionId = transId;
            if ((retCode = releaseProgramIndexRow(programIdx))
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "<%s::handleCaEnable> - Error releasing program index row (error %d)\n",
                        PODMODULE, retCode);
            }

            // Re-activate a suspended request. There may be multiple requests that can share this
            // program index table row
            activateSuspendedRequests(programIdx);
        }
    }

    return retCode;
}

static void sendEvent(mpe_PodDecryptSessionEvent event, uint8_t programIdx)
{
    mpe_PODDecryptSessionHandle sessionHandle;
    uint8_t numHandles = 0;
    int i;
    // Random size array
    mpe_PODDecryptSessionHandle handles[10];

    mpe_ProgramIndexTableRow *rowPtr = &programIndexTable.rows[programIdx];

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::sendEvent programIdx:%d\n", PODMODULE, programIdx);

    // There may be multiple requests per program index table row
    if (getDecryptSessionsForProgramIndex(programIdx, &numHandles, handles) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s::sendEvent error retrieving decrypt sessions for program index:%d.\n",
                PODMODULE, programIdx);
        return;
    }

    // Send events to all requests corresponding to the program index array
    for(i=0; i<numHandles; i++)
    {
        sessionHandle = handles[i];

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s::sendEvent event %s for sessionHandle:0x%p.\n",
                PODMODULE, mpe_podCAEventString(event), sessionHandle);

        sendJniDecryptSessionEvent(event, sessionHandle, rowPtr->ltsid);
    }
}

static mpe_Error createAndSendCaPMTApdu(uint32_t serviceHandle, uint8_t programIdx, uint8_t transactionId, uint8_t ltsid,
                               uint8_t ca_cmd_id)
{
    uint8_t *apdu_buf = NULL;
    uint8_t *apdu_buf_data = NULL;
    uint32_t apdu_length = 0;
    uint8_t apdu_length_field_length;
    mpe_Bool done = false;
    uint8_t retry_count = g_mpe_ca_retry_count;
    mpe_Error retCode = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "<%s::createAndSendCaPMTApdu> - Enter.. \n", PODMODULE);

    /* SI DB read lock */
    if ((retCode = mpe_siLockForRead()) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "<%s::createAndSendCaPMTApdu> - Could not lock SI err=%d\n", PODMODULE,
                retCode);
        return retCode;
    }

    if ((retCode = podmgrCreateCAPMT_APDU((mpe_SiServiceHandle)serviceHandle,
               programIdx, transactionId, ltsid,
               ca_system_id,
               ca_cmd_id,
               &apdu_buf,
               &apdu_length)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "<%s::createAndSendCaPMTApdu> - Error creating CA_PMT (error 0x%x)\n",
                PODMODULE, retCode);

        if (mpe_siUnLock() != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<%s::createAndSendCaPMTApdu> - Could not unlock SI err=%d\n", PODMODULE,
                    retCode);
        }
        return retCode;
    }

    // Done with SI DB lock
    if (mpe_siUnLock() != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "<%s::createAndSendCaPMTApdu> - Could not unlock SI.\n", PODMODULE);
    }

    // Length is either 1 byte or n bytes, depending on high order bit of length_field
    apdu_length_field_length = (apdu_buf[3] & 0x80) ? (apdu_buf[3] & 0x7F)
                                                    : 1;
    // The length of the APDU header length needs to take into account the tag field (3 bytes).
    apdu_length_field_length += 3;
    apdu_buf_data = &(apdu_buf[apdu_length_field_length]);

    if (retCode == MPE_SUCCESS && apdu_buf != NULL)
    {
        // Dump the contents of the CA PMT APDU
        hexDump((uint8_t *) (PODMODULE " DUMP_CA_PMT"), apdu_buf, apdu_length);
    }

    while(!done)
    {
        retCode = mpe_podSendAPDU(casSessionId, CA_PMT_TAG, (apdu_length-apdu_length_field_length), apdu_buf_data);
        if (retCode == MPE_SUCCESS)
        {
            done = true;
        }
        else
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "<%s::createAndSendCaPMTApdu> - Error sending CA_PMT, retCode: %d ..\n", PODMODULE, retCode);

            // Re-attempt sending the apdu if retry count is set
            if(retry_count)
            {
                retry_count--;
            }

            if(retry_count == 0)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "<%s::createAndSendCaPMTApdu> - Error sending CA_PMT apdu after %d retries..\n", PODMODULE, g_mpe_ca_retry_count);
                done = true;
            }
            else
            {
                // Wait here before attempting a re-send
                // Configurable via ini variable
                mpe_threadSleep(g_mpe_ca_retry_timeout, 0);
            }
        }
    } // End while(!done)

    if (apdu_buf != NULL)
    {
        mpe_memFreeP(MPE_MEM_POD, apdu_buf);
        apdu_buf = NULL;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "<%s::createAndSendCaPMTApdu> - done.. \n", PODMODULE);

    if(retCode == MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "******************** ISSUED CA PMT command = %s, WAITING FOR REPLY/UPDATE for transaction Id: %d **********\n", caCommandString(ca_cmd_id), transactionId);
    }

#ifdef PODIMPL_FRONTPANEL_DEBUG
    {
        snprintf(fp_debug_lines[0],30,"%d-%d",programIdx,transactionId);
    }
#endif

    return retCode;
}

static mpe_Error updatePODDecryptRequestAndResource(uint8_t programIdx,
        uint8_t transactionId, uint16_t numPids, uint16_t elemStreamPidArray[],
        uint8_t caEnableArray[])
{
    mpe_Error retCode = MPE_SUCCESS;
    uint8_t ca_enable = 0;

    MPEOS_LOG(MPE_LOG_INFO,
            MPE_MOD_POD,
            "%s::updatePODDecryptRequestAndResource - programIdx=%d\n", PODMODULE, programIdx);

    if((retCode = setPidArrayForProgramIndex(programIdx, numPids, elemStreamPidArray, caEnableArray)) != MPE_SUCCESS)
    {
        return retCode;
    }

    if (elemStreamPidArray == NULL && numPids == 0) /* deal with outer ca_enable byte */
    {
        ca_enable = caEnableArray[0] & 0x7F;
        retCode = handleCaEnable(ca_enable, programIdx);
    }
    else /* else, inner ca_enable bytes */
    {
        uint8_t successValue = CA_ENABLE_NO_VALUE;
        uint8_t mmiValue = CA_ENABLE_NO_VALUE;
        uint8_t failValue = CA_ENABLE_NO_VALUE;
        int i;
        /*
         * report any failure first, but if failure is not found report any MMI activity.  If everything is okay, report
         * decrypt started
         */
        for (i = 0; i < numPids && failValue == CA_ENABLE_NO_VALUE; i++)
        {
            ca_enable = caEnableArray[i] & 0x7F;

            switch (ca_enable)
            {
            case CA_ENABLE_DESCRAMBLING_NO_CONDITIONS: /* descrambling is occurring*/
                successValue = ca_enable;
                break;
            case CA_ENABLE_DESCRAMBLING_WITH_PAYMENT: /* descrambling possible with purchase */
            case CA_ENABLE_DESCRAMBLING_WITH_TECH: /* descrambling possible with technical intervention. */
                mmiValue = ca_enable;
                break;
            case CA_ENABLE_DESCRAMBLING_PAYMENT_FAIL: /* descrambling not possible since there is no entitlement */
            case CA_ENABLE_DESCRAMBLING_TECH_FAIL: /* descrambling not possible for technical reasons.  For example, all elementary stream available are being used */
                failValue = ca_enable;
                break;
            }
        }

        if (failValue > CA_ENABLE_NO_VALUE)
        {
            retCode = handleCaEnable(failValue, programIdx);
        }
        else if (mmiValue > CA_ENABLE_NO_VALUE)
        {
            retCode = handleCaEnable(mmiValue, programIdx);
        }
        else if (successValue > CA_ENABLE_NO_VALUE)
        {
            retCode = handleCaEnable(successValue, programIdx);
        }
    }

    return retCode;
}

static void shutdownMPELevelRegisteredPodEvents()
{
    QueueEntry* p;
    QueueEntry* save;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD,
            "<MPE_LEVEL_POD_QUEUES> shutdownRegisteredPodEvents \n");

    if (podEventQueueListMutex == NULL)
        return;

    mpeos_mutexAcquire(podEventQueueListMutex);

    // Shutdown the queues and delete the entries
    p = podEventQueueList;
    while (NULL != p)
    {
        mpeos_eventQueueSend(p->m_queue, MPE_ETHREADDEATH, NULL, NULL, 0);
        save = p;
        p = p->next;
        mpeos_memFreeP(MPE_MEM_POD, save);
    }

    // Release and kill the mutex
    mpeos_mutexRelease(podEventQueueListMutex);
    mpeos_mutexDelete(podEventQueueListMutex);
}

static void notifyMPELevelRegisteredPodEventQueues(mpe_Event eventId,
        void *optionalEventData1, void *optionalEventData2, uint32_t eventFlag)
{
    QueueEntry* p;
    mpe_Error err;

    MPEOS_LOG(
            MPE_LOG_INFO,
            MPE_MOD_POD,
            "<MPE_LEVEL_POD_QUEUES> notifyMPELevelRegisteredPodEventQueues - eventId = %d, podEventQueueList=0x%p\n",
            eventId, podEventQueueList);

    mpeos_mutexAcquire(podEventQueueListMutex);

    // Just run thru the list and send events.
    for (p = podEventQueueList; NULL != p; p = p->next)
    {
        // Send the event.
        err = mpe_eventQueueSend(p->m_queue, eventId, optionalEventData1,
                optionalEventData2, eventFlag);
        if (MPE_SUCCESS != err)
        {
            // Report any error to the log.
            MPEOS_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_POD,
                    "<MPE_LEVEL_POD_QUEUES> notifyMPELevelRegisteredPodEventQueues() - error sending POD event %d to queue 0x%x: %d\n",
                    eventId, p->m_queue, err);
        }
    }

    mpeos_mutexRelease(podEventQueueListMutex);
}

/**
 * Designed to be called even before this manager is fully initialized
 */
mpe_Error podImplRegisterMPELevelQueueForPodEvents(mpe_EventQueue queueId)
{
    QueueEntry* newEntry;
    mpe_Error err;

    MPEOS_LOG(
            MPE_LOG_INFO,
            MPE_MOD_POD,
            "<MPE_LEVEL_POD_QUEUES> podImplRegisterMPELevelQueueForPodEvents - queue 0x%x\n",
            queueId);

    // Allocate a new entry
    err = mpeos_memAllocP(MPE_MEM_POD, sizeof(QueueEntry), (void**) &newEntry);

    if (MPE_SUCCESS != err)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_POD,
                "<MPE_LEVEL_POD_QUEUES> podImplRegisterMPELevelQueueForPodEvents - failure, queue 0x%x, %d\n",
                queueId, err);
        return err;
    }

    // Init it.
    newEntry->m_queue = queueId;

    // Add it to the list.  In front is OK.
    mpeos_mutexAcquire(podEventQueueListMutex);
    newEntry->next = podEventQueueList;
    podEventQueueList = newEntry;
    mpeos_mutexRelease(podEventQueueListMutex);

    MPEOS_LOG(
            MPE_LOG_INFO,
            MPE_MOD_POD,
            "<MPE_LEVEL_POD_QUEUES> podImplRegisterMPELevelQueueForPodEvents - podEventQueueList=0x%p\n",
            podEventQueueList);

    return MPE_SUCCESS;
}

mpe_Error podImplUnregisterMPELevelQueueForPodEvents(mpe_EventQueue queueId)
{
    QueueEntry** p;
    QueueEntry* save;
    mpe_Error err = MPE_EINVAL;

    MPEOS_LOG(
            MPE_LOG_INFO,
            MPE_MOD_POD,
            "<MPE_LEVEL_POD_QUEUES> podImplUnregisterMPELevelQueueForPodEvents - queue 0x%x\n",
            queueId);

    // Lock the list for modification
    mpeos_mutexAcquire(podEventQueueListMutex);

    // Find the entry with this ID
    for (p = &podEventQueueList; NULL != *p; p = &((*p)->next))
    {
        // Find it?
        if ((*p)->m_queue == queueId)
        {
            // Remove it and exit
            // Save it
            save = *p;
            // Reset the current pointer to skip it.
            (*p) = (*p)->next;

            // Free the node
            mpeos_memFreeP(MPE_MEM_POD, save);

            // Set up the successful return
            err = MPE_SUCCESS;

            // Drop out of the loop
            break;
        }
    }

    // Release the structure
    mpeos_mutexRelease(podEventQueueListMutex);

    return err;
}

static mpe_Error suspendActiveCASessions(void)
{
    int programIndex=0;
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "<%s::suspendActiveCASessions> ...\n",
            PODMODULE);

    for (programIndex = 0; programIndex < programIndexTable.numRows; programIndex++)
    {
        mpe_PODDecryptSessionHandle handles[10];
        uint8_t numHandles = 0;
        int i;
        mpe_PODDecryptSessionHandle rHandle;
        uint8_t ltsid = 0;
        uint8_t transId;
        mpe_Error retCode = MPE_SUCCESS;
        mpe_SiServiceHandle serviceHandle = MPE_SI_INVALID_HANDLE;
        mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[programIndex];
        // Find active session
        if(ACTIVE_DECRYPT_STATE(rowPtr->state))
        {
            // There may be multiple requests per program index table row
            if ((retCode = getDecryptSessionsForProgramIndex(programIndex, &numHandles, handles))
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "<%s::suspendActiveCASessions> error retrieving decrypt sessions for program index:%d.\n",
                        PODMODULE, programIndex);
                //continue;
            }

            for(i=0;i<numHandles;i++)
            {
                mpe_RequestTableRow *request = (mpe_RequestTableRow*)handles[i];

                serviceHandle = request->serviceHandle;
                rHandle = handles[i];

                // Reset the requests state and program index
                request->state = MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES;
                request->programIdx = -1;
                // Save the command id so that it can be re-activated when
                // resources are available
                //request->ca_pmt_cmd_id = rowPtr->ca_pmt_cmd_id;
                request->ca_pmt_cmd_id = MPE_MPOD_CA_QUERY;

                // Send an event to notify of resource loss
                sendJniDecryptSessionEvent(MPE_POD_DECRYPT_EVENT_RESOURCE_LOST, rHandle, ltsid);
            }

            ltsid = rowPtr->ltsid;
            transId = getNextTransactionIdForProgramIndex(programIndex);

            // Send a ca_pmt apdu with 'not_selected' (0x04) when decrypt session
            // is no longer in use
            if((retCode = createAndSendCaPMTApdu(serviceHandle, programIndex, transId, ltsid, MPE_MPOD_CA_NOT_SELECTED)
                    != MPE_SUCCESS))
            {
                 MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                         "<%s::suspendActiveCASessions> - Error sending CA_PMT apdu (MPE_MPOD_CA_NOT_SELECTED) (error %d)\n",
                         PODMODULE, retCode);
            }

            // Release the CP session
            if(rowPtr->cpSession != NULL)
            {
                if(MPE_SUCCESS != mpeos_podStopCPSession(rowPtr->cpSession))
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                            "%s::suspendActiveCASessions Error stopping CP session...\n",
                            PODMODULE);
                }
            }

            if ((retCode = releaseProgramIndexRow(programIndex))
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "<%s::suspendActiveCASessions> error releasing program index table row:%d.\n",
                        PODMODULE, programIndex);
            }

            // Restore the transaction id
            //rowPtr->transactionId = transId;
            rowPtr->transactionId = 0;
        } // End if(ACTIVE_DECRYPT_STATE(rowPtr->state))
    } // End for (programIndex = 0; programIndex < programIndexTable.numRows; programIndex++)

    logProgramIndexTable("suspendActiveCASessions");
    logRequestTable("suspendActiveCASessions");
    return MPE_SUCCESS;
}

static mpe_Error activateSuspendedCASessions(void)
{
    int programIndex=0;
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "<%s::activateSuspendedCASessions> ...\n",
            PODMODULE);
    if(requestTable == NULL)
    {
        // There are no suspended requests
        return MPE_SUCCESS;
    }

    for (programIndex = 0; programIndex < programIndexTable.numRows; programIndex++)
    {
        activateSuspendedRequests(programIndex);
    }
    logProgramIndexTable("activateSuspendedCASessions");
    logRequestTable("activateSuspendedCASessions");
    return MPE_SUCCESS;
}


static uint8_t getLtsid(void)
{
    do
    {
        // 'g_ltsid' starts as a random number
        g_ltsid = (g_ltsid%255)+1; // Range: 1..255
    } while(isLtsidInUse(g_ltsid)); // If ltsid is in use get the next ltsid

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "<%s::getLtsid> ...ltsid:%d\n",
            PODMODULE, g_ltsid);
    return g_ltsid;
}

static mpe_Bool isLtsidInUse(uint8_t ltsid)
{
    int programIndex=0;
    for (programIndex = 0; programIndex < programIndexTable.numRows; programIndex++)
    {
    	mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[programIndex];
    	//Check if ltsid is already in use
    	if(rowPtr->ltsid == ltsid)
    	{
    		return TRUE;
    	}
    }
    return FALSE;
}

static uint8_t getLtsidForTunerId(uint8_t tuner)
{
    int programIndex=0;
    for (programIndex = 0; programIndex < programIndexTable.numRows; programIndex++)
    {
    	mpe_ProgramIndexTableRow* rowPtr = &programIndexTable.rows[programIndex];
        if(rowPtr->tunerId == tuner)
        {
        	return rowPtr->ltsid;
        }
    }
    return LTSID_UNDEFINED; // invalid ltsid
}
