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

/*
 *
 * Implementation of MPE OS POD API for the CableLabs Reference Implementation (RI) platform.
 *
 * For ease of maintenance the complete description of all POD functions are commented
 * in mpeos_pod.h and not in each of the platform specific implementation files such
 * as this one.
 *
 */

/* Header Files */
#include <stdio.h>
#include <string.h>
#include <mpe_types.h>
#include <mpeos_pod.h>
#include <mpeos_dbg.h>
#include <mpeos_util.h>
#include <mpeos_mem.h>
#include <mpeos_socket.h>
#include <stdlib.h>
#include <ri_cablecard.h>
#include <ri_test_interface.h>

#define MPE_MEM_DEFAULT MPE_MEM_POD

// Only need a single event queue
mpe_EventQueue gPodEventQueue;
void* gEventQueueACT = NULL;

// Global cache of CableCARD streaming capabilities
uint32_t gMaxCardElemStreams = 10;
uint32_t gMaxCardTransStreams = 2;
uint32_t gMaxCardPrograms = 3;

/**
 * Defines a doubly linked list node that holds APDU data
 */
typedef struct os_apdu_node
{
    uint32_t sessionID;
    uint8_t* data;
    int32_t length;
    struct os_apdu_node* next;

} os_apdu_node_t;

// Our list of outstanding APDUs received from the platform.  They are waiting
// for calls to mpeos_podReceiveAPDU() to retrieve them.
os_apdu_node_t* gAPDUQueue = NULL;
os_apdu_node_t* gAPDUQueueEnd = NULL;
mpe_Mutex gAPDUMutex;
mpe_Cond gAPDUReadyCond;

static int FAKE_SESSION_ID = 0xa1b1c1d1;
static mpe_Bool exitWorkerThread = false;
static mpe_Socket podSocket;
mpe_Error podReceiveAPDUsOverUDP();
static mpe_ThreadId podCASUDPReceiverThreadId = NULL;
static void podCASUDPReceiverThread(void *data);

// Allocates memory for a new APDU node and initializes it with the
// given data.  Returns NULL if the allocation fails
static os_apdu_node_t* allocateAPDU(uint32_t sessionID, uint8_t* data,
        int32_t length)
{
    os_apdu_node_t* node;

    if (mpe_memAlloc(sizeof(os_apdu_node_t), (void**) &node) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Could not allocate APDU node memory!\n", __FUNCTION__);
        return NULL;
    }

    node->sessionID = sessionID;
    node->data = data;
    node->length = length;
    node->next = NULL;

    return node;
}

// Add the given APDU to the end of the queue
static void addAPDU(os_apdu_node_t* apdu)
{
    // Just ignore NULL nodes.  This can happen if the allocation fails
    // for any reason
    if (apdu == NULL)
        return;

    (void) mpeos_mutexAcquire(gAPDUMutex);

    // If the queue is empty, add the node and set the condition var
    if (gAPDUQueue == NULL)
    {
        gAPDUQueue = gAPDUQueueEnd = apdu;
        mpeos_condSet(gAPDUReadyCond);
    }
    else
    {
        // Add to the end
        gAPDUQueueEnd->next = apdu;
        gAPDUQueueEnd = apdu;
    }

    (void) mpeos_mutexRelease(gAPDUMutex);
}

// Removes the next APDU node from the front of the queue and returns it
// to the caller. Returns NULL if the queue is empty
static os_apdu_node_t* removeAPDU()
{
    os_apdu_node_t* node;

    (void) mpeos_mutexAcquire(gAPDUMutex);

    // Empty queue
    if (gAPDUQueue == NULL)
    {
        (void) mpeos_mutexRelease(gAPDUMutex);
        return NULL;
    }

    // Always return the front node from the queue
    node = gAPDUQueue;

    // Last one in the queue?
    if (gAPDUQueue == gAPDUQueueEnd)
    {
        // Empty the queue and unset our condition var
        gAPDUQueue = gAPDUQueueEnd = NULL;
        mpeos_condUnset(gAPDUReadyCond);
    }
    else
    {
        // Just remove the front node
        gAPDUQueue = gAPDUQueue->next;
    }

    (void) mpeos_mutexRelease(gAPDUMutex);

    return node;
}

/**
 * Updates the given feature in the mpe_PODFeatureParams with the provided data.
 */
static mpe_Error updateGenericFeature(mpe_PODFeatureParams *params,
        ri_cablecard_generic_feature featureID, uint8_t* data, uint8_t length)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: featureID = %d, data = %p, length = %d\n", __FUNCTION__,
            featureID, data, length);

    switch (featureID)
    {
    case RI_CCARD_GF_RF_OUTPUT_CHANNEL:
        if (length != 2)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid RF_OUTPUT_CHANNEL length - %d!\n",
                    __FUNCTION__, length);
            return MPE_EINVAL;
        }
        memcpy((void*) params->rf_output_channel, (void*) data, 2);
        break;

    case RI_CCARD_GF_PC_PIN:
        // Release old memory
        if (params->pc_pin != NULL)
        {
            mpe_memFree(params->pc_pin);
        }
        if (mpe_memAlloc(length, (void**) &params->pc_pin) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Error allocating pc_pin memory!\n", __FUNCTION__);
            return MPE_EINVAL;
        }
        memcpy((void*) params->pc_pin, (void*) data, length);
        break;

    case RI_CCARD_GF_PC_SETTINGS:
        // Release old memory
        if (params->pc_setting != NULL)
        {
            mpe_memFree(params->pc_setting);
        }
        if (mpe_memAlloc(length, (void**) &params->pc_setting) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Error allocating pc_setting memory!\n", __FUNCTION__);
            return MPE_EINVAL;
        }
        memcpy((void*) params->pc_setting, (void*) data, length);
        break;

    case RI_CCARD_GF_IPPV_PIN:
        // Release old memory
        if (params->ippv_pin != NULL)
        {
            mpe_memFree(params->ippv_pin);
        }
        if (mpe_memAlloc(length, (void**) &params->ippv_pin) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Error allocating ippv_pin memory!\n", __FUNCTION__);
            return MPE_EINVAL;
        }
        memcpy((void*) params->ippv_pin, (void*) data, length);
        break;

    case RI_CCARD_GF_TIME_ZONE:
        if (length != 2)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid TIME_ZONE length - %d!\n", __FUNCTION__,
                    length);
            return MPE_EINVAL;
        }
        memcpy((void*) params->time_zone_offset, (void*) data, 2);
        break;

    case RI_CCARD_GF_DAYLIGHT_SAVINGS:
        if (length != 10 && length != 1)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid DAYLIGHT_SAVINGS length - %d!\n",
                    __FUNCTION__, length);
            return MPE_EINVAL;
        }

        memset((void*) &params->daylight_savings, 0, 10);
        memcpy((void*) &params->daylight_savings, (void*) data, length);

        break;

    case RI_CCARD_GF_AC_OUTLET:
        if (length != 1)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid AC_OUTLET length - %d!\n", __FUNCTION__,
                    length);
            return MPE_EINVAL;
        }
        memcpy((void*) &params->ac_outlet_ctrl, (void*) data, 1);
        break;

    case RI_CCARD_GF_LANGUAGE:
        if (length != 3)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid LANGUAGE length - %d!\n", __FUNCTION__, length);
            return MPE_EINVAL;
        }
        memcpy((void*) params->language_ctrl, (void*) data, 3);
        break;

    case RI_CCARD_GF_RATING_REGION:
        if (length != 1)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid RATING_REGION length - %d!\n", __FUNCTION__,
                    length);
            return MPE_EINVAL;
        }
        memcpy((void*) &params->ratings_region, (void*) data, 1);
        break;

    case RI_CCARD_GF_RESET_PIN:
        if (length != 1)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid RESET_PIN length - %d!\n", __FUNCTION__,
                    length);
            return MPE_EINVAL;
        }
        memcpy((void*) &params->reset_pin_ctrl, (void*) data, 1);
        break;

    case RI_CCARD_GF_CABLE_URLS:
        // Release old memory
        if (params->cable_urls != NULL)
        {
            mpe_memFree(params->cable_urls);
        }
        if (mpe_memAlloc(length, (void**) &params->cable_urls) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Error allocating cable_urls memory!\n", __FUNCTION__);
            return MPE_EINVAL;
        }
        memcpy((void*) params->cable_urls, (void*) data, length);
        params->cable_urls_length = length;
        break;

    case RI_CCARD_GF_EA_LOCATION_CODE:
        if (length != 3)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid EA_LOCATION length - %d!\n", __FUNCTION__,
                    length);
            return MPE_EINVAL;
        }
        memcpy((void*) params->ea_location, (void*) data, 3);
        break;

    case RI_CCARD_GF_VCT_ID:
        if (length != 2)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid VCT_ID length - %d!\n", __FUNCTION__, length);
            return MPE_EINVAL;
        }
        memcpy((void*) params->vct_id, (void*) data, 2);
        break;

    case RI_CCARD_GF_TURN_ON_CHANNEL:
        if (length != 2)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid TURN_ON_CHANNEL length - %d!\n", __FUNCTION__,
                    length);
            return MPE_EINVAL;
        }
        memcpy((void*) params->turn_on_channel, (void*) data, 2);
        break;

    case RI_CCARD_GF_TERM_ASSOC:
        // Release old memory
        if (params->term_assoc != NULL)
        {
            mpe_memFree(params->term_assoc);
        }
        if (mpe_memAlloc(length, (void**) &params->term_assoc) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Error allocating term_assoc memory!\n", __FUNCTION__);
            return MPE_EINVAL;
        }
        memcpy((void*) params->term_assoc, (void*) data, length);
        break;

    case RI_CCARD_GF_DOWNLOAD_GROUP_ID:
        if (length != 2)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Invalid CDL_GROUP_ID length - %d!\n", __FUNCTION__,
                    length);
            return MPE_EINVAL;
        }
        memcpy((void*) params->cdl_group_id, (void*) data, 2);
        break;

    case RI_CCARD_GF_ZIP_CODE:
        // Release old memory
        if (params->zip_code != NULL)
        {
            mpe_memFree(params->zip_code);
        }
        if (mpe_memAlloc(length, (void**) &params->zip_code) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Error allocating zip_code memory!\n", __FUNCTION__);
            return MPE_EINVAL;
        }
        memcpy((void*) params->zip_code, (void*) data, length);
        break;

    default:
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD, "%s: Invalid feature ID!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * Update the basic card information.  We only update this information when
 * the card has been newly inserted and becomes ready.  Assumes that the
 * podDB mutex is held by the caller.
 */
static mpe_Error updatePODDatabase(mpe_PODDatabase *podDB)
{
    mpe_Error err = MPE_SUCCESS;
    int i;

    ri_cablecard_generic_feature* cc_feature_list;
    uint8_t cc_feature_list_length;
    mpe_PODFeatures* podFeatureList;

    /** Generic Features **/

    // Retrieve card generic feature list
    if (ri_cablecard_get_supported_features(&cc_feature_list,
            &cc_feature_list_length) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error retrieving CableCARD feature list!\n", __FUNCTION__);
        return MPE_EINVAL;
    }

    // Allocate a new generic feature list and copy in the feature IDs retrieved
    // from the platform
    if (mpe_memAlloc(sizeof(mpe_PODFeatures), (void**) &podFeatureList)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error allocating podFeatureList!\n", __FUNCTION__);
        err = MPE_ENOMEM;
        goto error1;
    }
    if (mpe_memAlloc(sizeof(uint8_t) * cc_feature_list_length,
            (void**) &podFeatureList->featureIds) != MPE_SUCCESS)
    {
        err = MPE_ENOMEM;
        goto error2;
    }
    podFeatureList->number = cc_feature_list_length;
    for (i = 0; i < cc_feature_list_length; i++)
        podFeatureList->featureIds[i] = cc_feature_list[i];

    /** Update the POD DB **/

    (void) mpeos_mutexAcquire(podDB->pod_mutex);

    // Update card capability information
    /* GREG:  Add new RI Platform APIs to get this information
    podDB->pod_maxTransportStreams = cc_info->max_transport_streams;
    podDB->pod_maxElemStreams = cc_info->max_elementary_streams;
    podDB->pod_maxPrograms = cc_info->max_programs;
    */

    // Update the generic feature list.  Release old memory if necessary
    if (podDB->pod_features != NULL)
    {
        (void) mpe_memFree(podDB->pod_features->featureIds);
        (void) mpe_memFree(podDB->pod_features);
        podDB->pod_features = NULL;
    }
    podDB->pod_features = podFeatureList;

    // Update each generic feature
    for (i = 0; i < cc_feature_list_length; i++)
    {
        if (mpeos_podGetFeatureParam(podDB, cc_feature_list[i]) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Error in mpeos_podGetFeatureParam() %d!\n", __FUNCTION__,
                    cc_feature_list[i]);
            err = MPE_EINVAL;
            goto error3;
        }
    }
    ri_cablecard_release_data((void*) cc_feature_list);

    (void) mpeos_mutexRelease(podDB->pod_mutex);

    return MPE_SUCCESS;

 error3:
    (void) mpeos_mutexRelease(podDB->pod_mutex);
    (void) mpe_memFree(podFeatureList->featureIds);

 error2:
    (void) mpe_memFree(podFeatureList);
    
 error1: ri_cablecard_release_data((void*) cc_feature_list);

    return err;
}

static void sendEvent(int event, void* eventData)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: mpeos_eventQueueSend: Event 0x%x, eventData 0x%p\n",
            __FUNCTION__, event, eventData);

    if (gPodEventQueue)
    {
        if (mpeos_eventQueueSend(gPodEventQueue, event, eventData,
                gEventQueueACT, 0) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: mpeos_eventQueueSend failed!\n", __FUNCTION__);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: mpeos_eventQueueSend: No event queue registered\n",
                __FUNCTION__);

    }
}

/**
 * Receive general CableCARD events from the platform
 */
static void cablecard_event_callback(ri_cablecard_event event,
        void* event_data, void* cb_data)
{
    mpe_PODDatabase *podDB = (mpe_PODDatabase*) cb_data;

    switch (event)
    {
    case RI_CCARD_EVENT_CARD_INSERTED:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: RI_CCARD_EVENT_CARD_INSERTED\n", __FUNCTION__);
        sendEvent(MPE_POD_EVENT_INSERTED, NULL);
        break;

    case RI_CCARD_EVENT_CARD_REMOVED:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: RI_CCARD_EVENT_CARD_REMOVED\n", __FUNCTION__);
        sendEvent(MPE_POD_EVENT_REMOVED, NULL);
        break;

    case RI_CCARD_EVENT_CARD_READY:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: RI_CCARD_EVENT_CARD_READY\n", __FUNCTION__);
        sendEvent(MPE_POD_EVENT_READY, NULL);
        break;

    case RI_CCARD_EVENT_GF_CHANGED:
    {
        uint8_t* featureData;
        uint8_t featureLength;
        ri_cablecard_generic_feature* featureID =
                (ri_cablecard_generic_feature*) event_data;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: RI_CCARD_EVENT_GF_CHANGED (%d)\n",
                  __FUNCTION__, *featureID);

        if (ri_cablecard_get_generic_feature(*featureID, &featureData,
                &featureLength) != RI_ERROR_NONE)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: Error retrieving feature (%d)!\n",
                      __FUNCTION__, *featureID);
            break;
        }

        // Update the POD Database
        (void) mpeos_mutexAcquire(podDB->pod_mutex);
        updateGenericFeature(&podDB->pod_featureParams, *featureID,
                featureData, featureLength);
        (void) mpeos_mutexRelease(podDB->pod_mutex);

        // Tell the platform to release the data associated with this
        // generic feature
        ri_cablecard_release_data(featureData);
    }
        sendEvent(MPE_POD_EVENT_GF_UPDATE, NULL);
        break;

    case RI_CCARD_EVENT_SAS_CONNECTION_AVAIL:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: RI_CCARD_EVENT_SAS_CONNECTION_AVAIL\n", __FUNCTION__);
        sendEvent(MPE_POD_EVENT_RESOURCE_AVAILABLE, NULL);
        break;

    default:
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_POD,
                  "%s: Unknown platform CCARD event (%d)\n", __FUNCTION__, event);
        break;
    }
}

/**
 * Receive APDUs and other session-related events from the platform
 */
static void resource_session_callback(ri_cablecard_session_event event,
        ri_session_id id, void* event_data, uint32_t data_length, void* cb_data)
{
    switch (event)
    {
    case RI_CCARD_EVENT_APDU_RECV:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: CCARD_EVENT_APDU_RECV -- id = %x, event_data = %p!\n",
                __FUNCTION__, (uint32_t) id, event_data);
        addAPDU(allocateAPDU((uint32_t) id, (uint8_t*) event_data, data_length));
        sendEvent(MPE_POD_EVENT_RECV_APDU, (void*) id);
        break;

    case RI_CCARD_EVENT_SESSION_CLOSED:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: CCARD_EVENT_SESSION_CLOSED -- id = %x!\n", __FUNCTION__,
                (uint32_t) id);
        break;

    default:
        break;
    }

}

// For RI test interface (telnet interface)
#define POD_TESTS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| 1 | signal RESET_PENDING\r\n" \
    "|---+-----------------------\r\n" \
    "| 2 | signal POD_READY\r\n"


static int podMenuInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    mpe_Event eventCode;
    void * optionData1;
    uint32_t optionData3;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    if (!gPodEventQueue)
    {
        ri_test_SendString(sock, "\r\n\nNO POD QUEUE REGISTERED!\r\n");
        *retCode = MENU_FAILURE;
        return 0;
    }

    optionData1 = NULL;
    optionData3 = 0;

    eventCode = 0;
    switch (rxBuf[0])
    {
        case '1':
        {
            eventCode = MPE_POD_EVENT_RESET_PENDING;
            break;
        }
        case '2':
        {
            eventCode = MPE_POD_EVENT_READY;
            break;
        }
        default:
        {
            strcat(rxBuf, " - unrecognized\r\n\n");
            ri_test_SendString(sock, rxBuf);
            *retCode = MENU_INVALID;
        }
    } // END switch (rxBuf[0])

    if (eventCode != 0)
    {
        if ( mpeos_eventQueueSend( gPodEventQueue,
                                   eventCode,
                                   optionData1,
                                   gEventQueueACT,
                                   optionData3)
             != MPE_SUCCESS )
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "%s: mpeos_eventQueueSend failed!\n", __FUNCTION__);
        }
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s - Exit 0\n", __FUNCTION__);
    return 0;
} // END podMenuInputHandler()


static MenuItem MpeosPODMenuItem =
{ false, "p", "POD", POD_TESTS, podMenuInputHandler };


/**
 * <i>mpeos_podInit()</i>
 *
 * Perform any target specific operations to enable interfacing with the POD
 * device via the target HOST-POD devices stack interface.  Depending on the platform
 * implementation this may include stack API call(s) to get the HOST-POD stack resources
 * initialized, or it may simply involve stack API calls(s) to access the data
 * already exhanged between the HOST and POD during the initial platform bootstrap
 * process.
 *
 * @param podDB is a pointer to the MPE layer platform-independent POD information
 *              database used to cache the POD-Host information.
 *
 * @return MPE_SUCCESS if the initialization process was successful.
 */
mpe_Error mpeos_podInit(mpe_PODDatabase *podDB)
{
    mpe_Error err;

    // Create our APDU list mutex
    if ((err = mpeos_mutexNew(&gAPDUMutex)) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error creating APDU list mutex!\n", __FUNCTION__);
        return err;
    }

    // Create our APDU "available" cond var
    if ((err = mpeos_condNew(FALSE, FALSE, &gAPDUReadyCond)) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error creating APDU ready cond var!\n", __FUNCTION__);
        return err;
    }

    podDB->pod_isReady = TRUE;
    if ((err = updatePODDatabase(podDB)) != MPE_SUCCESS)
    {
        return err;
    }
    sendEvent(MPE_POD_EVENT_READY, NULL);

    // Register our event callback
    if (ri_cablecard_register_for_events(cablecard_event_callback,
            (void*) podDB) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error registering for platform CableCARD events!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    // Register the test
    ri_test_RegisterMenu(&MpeosPODMenuItem);

    podReceiveAPDUsOverUDP();

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_podRegister()</i>
 *
 * Handles registration for pod-specific asynchronous events. The platform-specific
 * layer is responsible for notifying the stack of asynchronous changes to the POD
 * generic features list or app info.  An asynchronous completion token and
 * eventQueueID are provided to the native layer to facilitate this communication
 *
 * Either of the following 2 events can be sent (defined in mpe_pod.h)
 *
 *            MPE_POD_EVENT_GF_UPDATE
 *            MPE_POD_EVENT_APPINFO_UPDATE
 *
 * @param act is a unique token that identifies the Java component that is listening
 *            for asynchronous POD notifications
 * @param eventQueueID is the event queue ID to which POd events should be sent
 *
 * @return MPE_SUCCESS if the registration process was successful.
 */
mpe_Error mpeos_podRegister(mpe_EventQueue eventQueueID, void* act)
{
    gPodEventQueue = eventQueueID;
    gEventQueueACT = act;
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_podUnregister()</i>
 *
 * Removes any previous event handler for pod-specific asynchronous events
 *
 * @return MPE_SUCCESS if the unregistration process was successful.
 */
mpe_Error mpeos_podUnregister(void)
{
    gPodEventQueue = 0;
    gEventQueueACT = 0;
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_podGetAppInfo</i>
 *
 * Get the pointer to the POD application information.  The application information
 * should have been acquired from the POD-Host interface during the initialization
 * phase and is usually cached within the MPE layer POD database.  But, on some
 * platforms it may be necessary to take additional steps to acquire the application
 * information.  This API provides that on-demand support if necessary.
 *
 * @param podDB is a pointer to the MPE layer platform-independent POD information
 *              database used to cache the POD-Host information.
 *
 * @return MPE_SUCCESS if the application information was successfully returned.
 */
mpe_Error mpeos_podGetAppInfo(mpe_PODDatabase *podDB)
{
    MPE_UNUSED_PARAM(podDB);
    // TODO: Implement this.
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_podGetFeatures</i>
 *
 * Get the POD's generic features list.  Note the generic features list
 * can be updated by the HOST-POD interface asynchronously and since there
 * is no mechanism in the PTV POD support APIs to get notification of this
 * asynchronous update, the list must be refreshed from the interface every
 * the list is requested. The last list acquired will be buffered within the
 * POD database, but it may not be up to date.  Therefore, the MPEOS layer will
 * release any previous list and refresh with a new one whenever a request for
 * the list is made.
 *
 * @param podDB is a pointer to the MPE layer platform-independent POD information
 *              database used to cache the POD-Host information.
 *
 * @return MPE_SUCCESS if the features list was successfully acquired.
 */
mpe_Error mpeos_podGetFeatures(mpe_PODDatabase *podDB)
{
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_podGetFeatureParam</i>
 *
 * Populate the internal POD database with the specified feature parameter value.
 * Ideally this routine can acquire the full set of feature parameters from the
 * HOST-POD interface, but the interface may only support acquisition of a single
 * parameter value at a time.  Unfortunately, at this time PTV does not support
 * acquisition of the full set of feature parameters with a single call (SCTE-28
 * does), so this routine will acquire each feature parameter upon individual
 * request.  Either way, the target value will be cached within the internal database
 * and the calling mpeos_ API will return the value from the internal database.
 *
 * @param featureId is the identifier of the feature parameter of interest.
 *
 * @return MPE_SUCCESS if the value of the feature was acquired successfully.
 */
mpe_Error mpeos_podGetFeatureParam(mpe_PODDatabase *podDBPtr,
        uint32_t featureId)
{
    uint8_t* featureData;
    uint8_t featureDataLength;

    // Retrieve feature from platform
    if (ri_cablecard_get_generic_feature(featureId, &featureData,
            &featureDataLength) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error retrieving feature (%d) from platform!\n",
                __FUNCTION__, featureId);
        return MPE_EINVAL;
    }

    // Update POD DB
    if (updateGenericFeature(&podDBPtr->pod_featureParams, featureId,
            featureData, featureDataLength) != MPE_SUCCESS)
    {
        ri_cablecard_release_data((void*) featureData);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error updating generic feature database (%d)!\n",
                __FUNCTION__, featureId);
        return MPE_EINVAL;
    }

    ri_cablecard_release_data((void*) featureData);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_podSetFeatureParam<i/>
 *
 * Perform actual Generic Feature parameter set operation to POD-HOST interface.
 * If the POD accepts the proposed change in value, return TRUE to call to indicate
 * successful set operation.
 *
 * @param featureId is the generic feature parameter to set
 * @param param is a pointer to the value of the generic feature
 * @param size is the size in bytes of the parameter value
 *
 * @return TRUE if the values was accepted by the POD.
 */
mpe_Error mpeos_podSetFeatureParam(uint32_t featureId, uint8_t *param,
        uint32_t size)
{
    if (ri_cablecard_set_generic_feature(featureId, param, size)
            != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error attempting to set generic feature %d!\n",
                __FUNCTION__, featureId);
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * The mpeos_podCASConnect() function shall establish a connection between a private
 * Host application and the POD Conditional Access Support (CAS) resource.  It is the MPEOS call's responsibility
 * determine the correct resource ID based upon whether the card is M or S mode.
 *
 * @param sessionId points to a location where the session ID can be returned. The session
 *          ID is implementation dependent and represents the CAS session to the POD application.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podCASConnect(uint32_t *sessionId, uint16_t *resourceVersion)
{
	*sessionId = FAKE_SESSION_ID;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: - session: 0x%x\n", __FUNCTION__, *sessionId);

    return MPE_SUCCESS;
}

/**
 * The mpeos_podCASClose() function provides an optional API for systems that may
 * require additional work to maintain session resources when an application unregisters
 * its handler from POD.  The implementation of this function may
 * need to: 1) update internal implementation resources, 2) make an OS API call to
 * allow the OS to update session related resources or 3) do nothing since it's
 * entirely possible that the sessions can be maintained as "connected" upon
 * deregistration and simply reused if the same host or another host application makes a
 * later request to connect to the CAS application.
 *
 * @param sessionId the session identifier of the target CAS session.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podCASClose(uint32_t sessionId)
{
    // TODO: Implement this.
    MPE_UNUSED_PARAM(sessionId);

    return MPE_SUCCESS;
}

/**
 * The mpeos_podSASConnect() function shall establish a connection between a private
 * Host application and the corresponding POD Module vendor-specific application.
 *
 * @param appID specifies a unique identifier of the private Host application.
 * @param sessionID points to a location where the session ID can be returned. The session
 *          ID is established by the POD to enable further communications.
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unuseable.
 * </ul>
 */
mpe_Error mpeos_podSASConnect(uint8_t *appId, uint32_t *sessionId, uint16_t *version)
{
    ri_session_id sess;

    if (ri_cablecard_open_SAS_connection(&sess, appId,
            resource_session_callback, NULL) != RI_ERROR_NONE)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_POD,
                "%s: Error attempting to connect to SAS app (%02x %02x %02x %02x %02x %02x %02x %02x)!\n",
                __FUNCTION__, appId[0], appId[1], appId[2], appId[3], appId[4],
                appId[5], appId[6], appId[7]);
        return MPE_EINVAL;
    }

    *sessionId = (uint32_t) sess;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: SAS session successfully opened. ID = %x!\n", __FUNCTION__,
            *sessionId);

    return MPE_SUCCESS;
}

/**
 * The mpeos_podSASClose() function provides an optional API for systems that may
 * require additional work to maintain session resources when an application unregisters
 * its handler from an SAS application.  The implementation of this function may
 * need to: 1) update internal implementation resouces, 2) make an OS API call to
 * allow the OS to update session related resources or 3) do nothing since it's
 * entirely possible that the sessions can be maintained as "connected" upon
 * deregistration and simply reused if the same host or another host application makes a
 * later request to connect to the SAS application.
 *
 * @param sessionId the session identifier of the target SAS session (i.e. the Ocm_Sas_Handle).
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_EINVAL - One or more parameters were out of range or unuseable.
 * </ul>
 */
mpe_Error mpeos_podSASClose(uint32_t sessionId)
{
    if (ri_cablecard_close_SAS_connection((ri_session_id) sessionId)
            != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Could not close SAS session! ID = %d!\n", __FUNCTION__,
                sessionId);
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: SAS session successfully closed. ID = %d!\n", __FUNCTION__,
            sessionId);

    return MPE_SUCCESS;
}

/**
 * The mpeos_podMMIConnect() function shall establish a connection with the MMI
 * resource on the POD device.
 *
 * @param sessionId the session identifier of the target MMI session.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS.
 *         Otherwise, one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - insufficient memory available to complete the operation
 * <li>     MPE_EINVAL - One or more parameters were out of range or unuseable.
 * </ul>
 */
mpe_Error mpeos_podMMIConnect(uint32_t *sessionId, uint16_t *version)
{
    if (ri_cablecard_open_MMI_connection((ri_session_id*)sessionId,
            resource_session_callback, NULL) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                  "%s: Error attempting to connect to MMI!?\n", __FUNCTION__);
        return MPE_EINVAL;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: MMI session successfully opened. ID = %p!\n", __FUNCTION__,
            sessionId);

    return MPE_SUCCESS;
}

/**
 * The mpeos_podMMIClose() function provides an optional API for systems that
 * may require additional work to maintain MMI resources when an application
 * unregisters its MMI handler from the MMI POD application.  The implementation
 * of this function may need to: 1) update internal implementation resouces,
 * 2) make an OS API call to allow the OS to update MMI session related
 * resources or 3) do nothing since it's entirely possible that the MMI
 * sessions can be maintained as "connected" upon deregistration.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS.
 *         Otherwise, one of the errors below is returned.
 * <ul>
 * <li>     MPE_EINVAL - One or more parameters were out of range or unuseable.
 * </ul>
 */
mpe_Error mpeos_podMMIClose(void)
{
    if (ri_cablecard_close_MMI_connection() != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Could not close MMI session!!\n", __FUNCTION__);
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: MMI session successfully closed!\n", __FUNCTION__);

    return MPE_SUCCESS;
}

/**
 * The mpeos_podAIConnect() function shall establish a connection with the
 * Application Information resource on the POD device.
 *
 * @param sessionId the session identifier of the target application information session.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podAIConnect(uint32_t *sessionId, uint16_t *version)
{
    if (ri_cablecard_open_AI_connection((ri_session_id*)sessionId,
            resource_session_callback, NULL) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                  "%s: Error attempting to connect to MMI!?\n", __FUNCTION__);
        return MPE_EINVAL;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: AI session successfully opened. ID = %p!\n", __FUNCTION__,
            sessionId);

    return MPE_SUCCESS;
}

/**
 * The mpeos_podGetParam() function gets a resource parameter from the POD.
 *
 * @param paramId       MPE defined identifier.
 * @param paramValue    pointer to the parameter value.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podGetParam(mpe_podParamId paramId, uint32_t* paramValue)
{
    switch (paramId)
    {
    case MPE_POD_PARAM_ID_MAX_NUM_ELEMENTARY_STREAM:
        *paramValue = gMaxCardElemStreams;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: MAX_NUM_ELEMENTARY_STREAMS = %d!\n", __FUNCTION__,
                *paramValue);
        break;

    case MPE_POD_PARAM_ID_MAX_NUM_PROGRAMS:
        *paramValue = gMaxCardPrograms;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: MAX_NUM_PROGRAMS = %d!\n",
                __FUNCTION__, *paramValue);
        break;

    case MPE_POD_PARAM_ID_MAX_NUM_TRANSPORT_STREAMS:
        *paramValue = gMaxCardTransStreams;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "%s: MAX_NUM_TRANSPORT_STREAMS = %d!\n", __FUNCTION__,
                *paramValue);
        break;

    default:
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_POD, "%s: Invalid param ID! (%d)!\n",
                __FUNCTION__, paramId);
        break;
    }

    return MPE_SUCCESS;
}

/**
 * The mpeos_podReceiveAPDU() function retrieves the next available APDU from the POD.
 *
 * @param sessionId is a pointer for returning the associated session Id
 * @param apdu the next available APDU from the POD
 * @param len the length of the APDU in bytes
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unuseable.
 * <li>     MPE_ENODATA - Indicates that the APDU received is actually the last APDU that
 *                       failed to be sent.
 * </ul>
 */
mpe_Error mpeos_podReceiveAPDU(uint32_t* sessionId, uint8_t **apdu,
        int32_t *len)
{
    *sessionId = FAKE_SESSION_ID;
    os_apdu_node_t* apdu_node = NULL;

    while (apdu_node == NULL)
    {
        // Wait indefinitely for an APDU to be available
        mpeos_condWaitFor(gAPDUReadyCond, 0);

        if ((apdu_node = removeAPDU()) != NULL)
        {
            // Set return values
            *sessionId = apdu_node->sessionID;
            *apdu = apdu_node->data;
            *len = apdu_node->length;

            // Release our APDU node memory.  Actuall APDU memory will be released
            // by mpeos_podReleaseAPDU()
            mpe_memFree(apdu_node);
        }
    }

    return MPE_SUCCESS;
}

/**
 * The mpeos_podReleaseAPDU() function
 *
 * This will release an APDU retrieved via mpeos_podReceiveAPDU()
 *
 * @param apdu the APDU pointer retrieved via mpeos_podReceiveAPDU()
 * @return Upon successful completion, this function shall return
 * <ul>
 * <li>     MPE_EINVAL - The apdu pointer was invalid.
 * <li>     MPE_SUCCESS - The apdu was successfully deallocated.
 * </ul>
 */
mpe_Error mpeos_podReleaseAPDU(uint8_t *apdu)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD, "%s: buffer = %p!\n", __FUNCTION__,
            apdu);
    ri_cablecard_release_data(apdu);
    return MPE_SUCCESS;
}

/**
 * The mpeos_podSendAPDU() function sends an APDU packet.
 *
 * @param sessionId is the native handle for the SAS session for the APDU.
 * @param apduTag APDU identifier
 * @param length is the length of the data buffer portion of the APDU
 * @param apdu is a pointer to the APDU data buffer
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unuseable.
 * </ul>
 */
mpe_Error mpeos_podSendAPDU(uint32_t sessionId, uint32_t apduTag,
        uint32_t length, uint8_t *apdu)
{
    // Data length + 3-byte APDU tag
    uint32_t bufferLength = length + 3;
    uint8_t* buffer;
    uint8_t* bufferPtr;
    ri_error error = RI_ERROR_NONE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "%s: sessionId = %x, apduTag = %x, length = %d, apdu = %p!\n",
            __FUNCTION__, sessionId, apduTag, length, apdu);

    if (sessionId == FAKE_SESSION_ID)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s: ignoring APDU for FAKE_SESSION_ID!\n", __FUNCTION__);
        return MPE_SUCCESS;
    }

    // Determine length of length_field()
    if (length < 128)
        bufferLength += 1;
    else if (length < 256)
        bufferLength += 2;
    else if (length <= 65535)
        bufferLength += 3;
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Invalid APDU length! (%d)!\n", __FUNCTION__, length);
        return MPE_EINVAL;
    }

    // Allocate full APDU buffer
    if (mpe_memAlloc(bufferLength, (void**) &buffer) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Could not allocate buffer!\n", __FUNCTION__);
        return MPE_ENOMEM;
    }
    bufferPtr = buffer;

    // Copy the least significant 3 bytes of apduTag
    *(bufferPtr++) = (apduTag & 0xFF0000) >> 16;
    *(bufferPtr++) = (apduTag & 0xFF00) >> 8;
    *(bufferPtr++) = (apduTag & 0xFF);

    // Create length_field()
    if (length < 128)
    {
        *(bufferPtr++) = (length & 0x7F);
    }
    else if (length < 256)
    {
        *(bufferPtr++) = 0x81;
        *(bufferPtr++) = (length & 0xFF);
    }
    else
    {
        *(bufferPtr++) = 0x82;
        *(bufferPtr++) = (length & 0xFF00) << 8;
        *(bufferPtr++) = (length & 0xFF);
    }

    // Copy the APDU data
    memcpy(bufferPtr, apdu, length);

    // Send the APDU
    error = ri_cablecard_send_APDU((ri_session_id) sessionId, buffer);

    if (error != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s: Error (%d) sending APDU! Posting failure event.\n",
                __FUNCTION__, error);
        sendEvent(MPE_POD_EVENT_SEND_APDU_FAILURE, apdu);
        // Do not return error code, the sending of the event itself
        // signifies the error
    }

    mpe_memFree(buffer);
    return MPE_SUCCESS;
}

#define MPEOS_RI_FAKE_CP_SESSION_HANDLE ((mpe_PODCPSession)0xBAADF00D)

/**
  * <i>mpeos_podStartCPSession<i/>
  *
  * Start the CP (Copy Protection) session for the specified service.
  *
  * This method will be called after the initiation of CA (a ca_pmt
  * sent via the CAS session) and will precede initiation of any
  * MPEOS functions which operate on encrypted content. (e.g.
  * mpeos_mediaDecode(), mpeos_dvrTsbBufferingStart(),
  * and mpeos_filterSetFilter() for in-band sources)
  *
  * @param tunerId that's tuned to the transport stream on which
  * the desired service is carried.
  *
  * @param programNumber the program_number of the associated
  * service.
  *
  * @param ltsid The Logical Transport Stream ID associated with
  * the CA resource (setup via the ca_pmt).
  *
  * @param ecmPid the ECM PID associated with the CableCARD
  * program to monitor.
  *
  * @param programIndex the ca_pmt program index. The program
  * index is used to uniquely identify a service when multiple
  * programs are being decrypted for the same transport stream.
  *
  * @param numPids the number of PIDs in the pids array.
  *
  * @param pids array of PIDs as supplied in the CA_PMT APDU
  * used to initiate the CA session.
  *
  * @return MPE_SUCCESS if the Copy Protection session is
  * successfully started for the identified program.
  */
mpe_Error mpeos_podStartCPSession( uint32_t tunerId,
                                   uint16_t programNumber,
                                   uint32_t ltsid,
                                   uint16_t ecmPid,
                                   uint8_t programIndex,
                                   uint32_t numPids,
                                   uint32_t pids[],
                                   mpe_PODCPSession * session )
{
    MPE_UNUSED_PARAM(tunerId);
    MPE_UNUSED_PARAM(programNumber);
    MPE_UNUSED_PARAM(ltsid);
    MPE_UNUSED_PARAM(ecmPid);
    MPE_UNUSED_PARAM(programIndex);
    MPE_UNUSED_PARAM(numPids);
    MPE_UNUSED_PARAM(pids);
    MPE_UNUSED_PARAM(session);

    // Nothing really to do on the RI. Just setup a token session.

    *session = MPEOS_RI_FAKE_CP_SESSION_HANDLE;
    return MPE_SUCCESS;
} // END mpeos_podStartCPSession


/**
  * <i>mpeos_podStopCPSession<i/>
  *
  * Stop the CP (Copy Protection) session for the specified service.
  *
  * This method will be called after the termination of any MPEOS
  * functions which operate on encrypted content and precede the
  * termination of the ca_pmt session.
  *
  * @return MPE_SUCCESS if the Copy Protection session is
  * successfully stopped.
  */
mpe_Error mpeos_podStopCPSession(mpe_PODCPSession session)
{
    if (session != MPEOS_RI_FAKE_CP_SESSION_HANDLE)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_POD,
                "%s: Bad session ID %p (expected %p)\n",
                __FUNCTION__, session, MPEOS_RI_FAKE_CP_SESSION_HANDLE );
        return MPE_EINVAL;
    }
    return MPE_SUCCESS;
}


/**
 * Create a UDP socket and receive APDU datagrams
 *
 * @return MPE_SUCCESS	         if successful
 *         MPE_EINVAL		     os specific failures
 */
mpe_Error podReceiveAPDUsOverUDP()
{
	mpe_SocketIPv4SockAddr hostAddr;
	uint32_t port = 5555;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s() - called\n", __FUNCTION__);

    // Create socket
    podSocket = mpeos_socketCreate(AF_INET, SOCK_DGRAM, 0);
    if (podSocket == MPE_SOCKET_INVALID_SOCKET)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s() - failed to create socket\n", __FUNCTION__);
        return MPE_EINVAL;
    }

    // Formulate host and port number to use
    (void) memset((uint8_t *) &hostAddr, 0, sizeof(hostAddr));
    hostAddr.sin_family = AF_INET;
    int status = mpeos_socketAtoN((char*) "127.0.0.1", &hostAddr.sin_addr);
    if (status == 0)
    {
        // invalid or unknown host name
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s() - invalid or unknown host: %s\n", __FUNCTION__,
                "127.0.0.1");
        return MPE_EINVAL;
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s() - port number = %u\n",
            __FUNCTION__, port);
    hostAddr.sin_port = htons((uint16_t) port);

    // Connect to host
    status = mpeos_socketBind(podSocket, (mpe_SocketSockAddr*) &hostAddr, sizeof(hostAddr));

    if (status != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "%s() - error failed to connect socket - %d\n", __FUNCTION__, status);
        return MPE_EINVAL;
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s() - connected on socket %d\n",
                __FUNCTION__, podSocket);
    }

	int retCode = MPE_SUCCESS;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s() - called\n", __FUNCTION__);
	if ((retCode = mpe_threadCreate(podCASUDPReceiverThread, NULL,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &podCASUDPReceiverThreadId,
            "podCASUDPReceiverThread")) != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_POD,
                "failed to create CAS UDP Receiver thread.\n");
    }
    return retCode;
}

static void podCASUDPReceiverThread(void* data)
{
    uint8_t buf[MAX_UDP_DATAGRAM_SIZE];
    int numBytes = 0;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "podCASUDPReceiverThread started.\n");

    while (!exitWorkerThread)
    {
		if ((numBytes = mpeos_socketRecv(podSocket, buf, MAX_UDP_DATAGRAM_SIZE, 0)) > 0)
        {
            uint8_t *apduBuf = malloc(numBytes);
            //allocating here but we're relying on cablecard.c release_apdu to free
            memcpy(apduBuf, buf, numBytes);
            addAPDU(allocateAPDU(FAKE_SESSION_ID, apduBuf, numBytes));
        }
		else
		{
		    //MPE_LOG(MPE_LOG_INFO, MPE_MOD_POD, "mpeos_socketRecv returned %d.\n", numBytes);
		}
	}
}
