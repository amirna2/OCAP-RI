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

#include <ri_tuner.h>
#include <gst/gst.h>
#include <net_utils.h>
#include <ri_log.h>
#include <pipeline.h>
#include <platform.h>

#include <stdlib.h>
#include <string.h>

#include "tuner.h"
#include "test_interface.h"

#define RILOG_CATEGORY riTunerCat
log4c_category_t* riTunerCat = NULL;

struct ri_tuner_data_s
{
    GstElement* gst_udpsrc;

    // Mutex to protect access to tuner data
    GMutex* mutex;

    // tuner index
    int mIndex;
    // tuner type
    int mType;
    // tuner (stream receiving) IP address, typically localhost
    char mIpAddr[INET6_ADDRSTRLEN];
    //  tuner (stream receiving) port
    int mPort;
    //  PIDs for this tune
    uint16_t mPidlist[MAX_PID_STORAGE];    // a refcount for all possible PIDs

    unsigned long ate_frequency;

    // The pipeline this tuner is associated with
    ri_pipeline_t* pipeline;

    // Pending tuner request
    ri_tune_params_t pending_params;

    // Current tuner request
    ri_tune_params_t params;

    // Current tuner status
    ri_tuner_status_t status;

    // Currently registered tuner event callback
    ri_tuner_event_cb_f event_cb;
    void* cb_data;

};

void set_tuner_udp_port(ri_tuner_t* tuner, uint16_t udp_port);
gboolean getTunerStatus(ri_tuner_t* object);
void tunerStop(ri_tuner_data_t* tuner_data);
void tunerExit(ri_tuner_data_t* tuner_data);
void tunerRetune(ri_tuner_data_t* tuner_data);
static ri_bool tune(ri_tuner_data_t* tuner_data);

// Keep this counter for the tuners that we are creating.  We need an index
// for the particular tuner we are requesting.  If we ever go to dynamic
// pipeline creation/destruction, this will have to change
static int tuner_index = 0;
static ri_tuner_t _tuners[MAX_TUNERS];

#define TUNER_TESTS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| e | tuner Exit            \r\n" \
    "|---+-----------------------\r\n" \
    "| q | Query tuner status    \r\n" \
    "|---+-----------------------\r\n" \
    "| r | tuner Re-tune         \r\n" \
    "|---+-----------------------\r\n" \
    "| s | tuner Stop            \r\n" \
    "|---+-----------------------\r\n" \
    "| t | tuner Tune            \r\n" \


static int testInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    char buf[1024];
    int index = 0;
    ri_tuner_t* tuner = NULL;
    RILOG_TRACE("%s -- Entry, received: %s\n", __FUNCTION__, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        RILOG_TRACE("%s -- Exit", __FUNCTION__);
        return -1;
    }

    test_GetNumber(sock, buf, sizeof(buf), "tuner index", 0);

    if (index < MAX_TUNERS)
    {
        tuner = &_tuners[index];
    }

    if (tuner == NULL)
    {
        RILOG_ERROR("%s -- couldn't find tuneri[%d]!?\n", __FUNCTION__, index);
        return 0;
    }

    if (strstr(rxBuf, "e"))
    {
        test_SendString(sock, "\r\n\ntuner Exit...\r\n");
        tunerExit(tuner->data);
        return 0;
    }
    else if (strstr(rxBuf, "q"))
    {
        test_SendString(sock, "\r\n\nQuery tuner status: ");

        switch (tuner->data->mType)
        {
            case VLC:
                test_SendString(sock, vlc_TunerStatus(tuner->data->mIndex));
                break;

            case HDHR:
                test_SendString(sock, hdhr_TunerStatus(tuner->data->mIndex));
                break;

            case GST:
                test_SendString(sock, gst_TunerStatus(tuner->data->mIndex));
                break;

            default:
                RILOG_ERROR("%s -- unrecognized tuner type: %d\n",
                            __FUNCTION__, tuner->data->mType);
                break;
        }

        test_SendString(sock, "\r\n\n");
        return 0;
    }
    else if (strstr(rxBuf, "r"))
    {
        test_SendString(sock, "\r\n\ntuner Re-tune...\r\n");
        tunerRetune(tuner->data);
        tune(tuner->data);
        return 0;
    }
    else if (strstr(rxBuf, "s"))
    {
        test_SendString(sock, "\r\n\ntuner Stop...\r\n");
        tunerStop(tuner->data);
        return 0;
    }
    else if (strstr(rxBuf, "t"))
    {
        test_SendString(sock, "\r\n\ntuner tune...\r\n");
        tune(tuner->data);
        return 0;
    }
    else
    {
        strcat(rxBuf, " - unrecognized\r\n\n");
        test_SendString(sock, rxBuf);

        RILOG_TRACE("%s -- %s\n", __FUNCTION__, rxBuf);
        *retCode = MENU_INVALID;
        return 0;
    }
}

static MenuItem TunerMenuItem =
{ FALSE, "t", "TunerTests", TUNER_TESTS, testInputHandler };

/**
 * modulationString: A method used to extract the modulation mode string
 *      @param mode: the modulation mode to convert
 *          @return: the string representation of the modulation mode
 */
static const char* modulationString(ri_tuner_modulation_mode mode)
{
    switch (mode)
    {
    case RI_MODULATION_UNKNOWN:
        return "UNKNOWN";
    case RI_MODULATION_QAM64:
        return "64QAM";
    case RI_MODULATION_QAM256:
        return "256QAM";
    case RI_MODULATION_QAM_NTSC:
        return "ANALOG";
    default:
        return "INVALID";
    }
}

/**
 *     modulationMode: A method used to extract the modulation mode from a string
 * @param mode_string: the modulation mode string to convert
 *            @return: the modulation mode
 */
ri_tuner_modulation_mode modulationMode(const char* mode_string)
{
    if (strcmp(mode_string, "UNKNOWN") == 0)
        return RI_MODULATION_UNKNOWN;
    if (strcmp(mode_string, "64QAM") == 0)
        return RI_MODULATION_QAM64;
    if (strcmp(mode_string, "256QAM") == 0)
        return RI_MODULATION_QAM256;
    if (strcmp(mode_string, "ANALOG") == 0)
        return RI_MODULATION_QAM_NTSC;

    return RI_MODULATION_UNKNOWN;
}

/**
 * -- ri_tuner_t function implementation --
 * Request a tune.  The request completes synchronously, but the
 * the actual success/failure of the tune will be report async
 */
ri_error request_tune(ri_tuner_t* object, ri_tune_params_t tune_param)
{
    ri_tuner_data_t* data = object->data;

    RILOG_INFO(
            "%s Entry, tuner = 0x%p, frequency = %d, mode = %d, program = %d\n",
            __FUNCTION__, object, tune_param.frequency, tune_param.mode,
            tune_param.program_num);

    // fill in the current tune request information
    g_mutex_lock(data->mutex);
    data->params = tune_param;
    memset(data->mPidlist, 0, sizeof(data->mPidlist));
    g_mutex_unlock(data->mutex);

    // Request tune
    if (FALSE == tune(data))
    {
        RILOG_ERROR("%s -- Exit - Tune request failed\n", __FUNCTION__);
        return RI_ERROR_INVALID_TUNE_REQUEST;
    }
    else
    {
        RILOG_INFO("%s -- SUCCESS! Awaiting signal lock.\n", __FUNCTION__);
    }

    // poll (every 125 ms) and get/send the tuner status to the stack...
    (void) g_timeout_add(125, (GSourceFunc) getTunerStatus, object);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * -- ri_tuner_t function implementation --
 * Request the status of the tuner
 */
void request_status(ri_tuner_t* object, ri_tuner_status_t* tuner_status)
{
    ri_tuner_data_t* data = object->data;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    g_mutex_lock(data->mutex);

    // Populate our current tuner status
    tuner_status->frequency = data->status.frequency;
    tuner_status->mode = data->status.mode;
    tuner_status->program_num = data->status.program_num;
    g_mutex_unlock(data->mutex);

    RILOG_DEBUG(
            "%s -- tuner = 0x%p, frequency = %d, mode = %d, program = %d\n",
            __FUNCTION__, object, data->status.frequency, data->status.mode,
            data->status.program_num);

    // Right now we don't need it, but in the future we might need the rest
    // of the values in the ri_tuner_status_t structure.  For that we will
    // need to make another call for "SignalDiagnostics" here.

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * -- ri_tuner_t function implementation --
 * Register a callback that will notified of tuner events
 */
void register_tuner_event_cb(ri_tuner_t* object, ri_tuner_event_cb_f event_cb,
        void* cb_data)
{
    ri_tuner_data_t* data = object->data;

    RILOG_TRACE("%s -- Entry, tuner = 0x%p, function = 0x%p, cb_data = 0x%p\n",
            __FUNCTION__, object, event_cb, cb_data);

    g_mutex_lock(data->mutex);

    // Can only unregister if the callback functions match
    if (data->event_cb != NULL)
    {
        RILOG_ERROR(
                "%s -- Cannot register a new tuner event callback until the previous one is unregistered!\n",
                __FUNCTION__);
        g_mutex_unlock(data->mutex);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    data->event_cb = event_cb;
    data->cb_data = cb_data;

    g_mutex_unlock(data->mutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * -- ri_tuner_t function implementation --
 * Unregister the tuner event callback
 */
ri_error unregister_tuner_event_cb(ri_tuner_t* object,
        ri_tuner_event_cb_f event_cb)
{
    ri_tuner_data_t* data = object->data;

    RILOG_TRACE("%s -- Entry, tuner = 0x%p, function = 0x%p\n", __FUNCTION__,
            object, event_cb);

    g_mutex_lock(data->mutex);

    // Can only unregister if the callback functions match
    if (data->event_cb != event_cb)
    {
        RILOG_ERROR(
                "%s -- Attempted to unregister an invalid event callback\n",
                __FUNCTION__);
        g_mutex_unlock(data->mutex);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_GENERAL;
    }

    // Unregister
    data->event_cb = NULL;
    data->cb_data = NULL;

    g_mutex_unlock(data->mutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

gboolean getTunerStatus(ri_tuner_t* object)
{
    gboolean retVal = TRUE;   // default to 'TRUE'; i.e. keep calling this...
    ri_tuner_event event = RI_TUNER_EVENT_FAIL;
    ri_bool streaming = FALSE;
    ri_bool lock = FALSE;
    char* result = NULL;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL != object)
    {
        ri_tuner_data_t* tuner_data = object->data;

        switch (tuner_data->mType)
        {
            case VLC:
                result = vlc_TunerStatus(tuner_data->mIndex);
                streaming = vlc_TunerIsStreaming(tuner_data->mIndex);

                if (strstr(result, "state : playing"))
                {
                    lock = TRUE;
                }
                break;

            case HDHR:
                result = hdhr_TunerStatus(tuner_data->mIndex);
                streaming = hdhr_TunerIsStreaming(tuner_data->mIndex);

                if (strstr(result, "lock=qam"))
                {
                    lock = TRUE;
                }
                break;

            case GST:
                result = gst_TunerStatus(tuner_data->mIndex);
                streaming = gst_TunerIsStreaming(tuner_data->mIndex);

                if (strstr(result, "streaming"))
                {
                    lock = TRUE;
                }
                break;

            default:
                RILOG_ERROR("%s -- unrecognized tuner type: %d\n",
                            __FUNCTION__, tuner_data->mType);
                break;
        }

        RILOG_INFO("%s -- RESULT: %s\n", __FUNCTION__, result);

        if ((TRUE == lock) || (TRUE == streaming))
        {
            g_mutex_lock(tuner_data->mutex);
            tuner_data->status.frequency = tuner_data->params.frequency;
            tuner_data->status.program_num = tuner_data->params.program_num;
            tuner_data->status.mode = tuner_data->params.mode;
            tuner_data->pipeline->set_decode_prog_num(tuner_data->pipeline,
                                              tuner_data->params.program_num);
            g_mutex_unlock(tuner_data->mutex);
            event = RI_TUNER_EVENT_SYNC;
            RILOG_DEBUG("%s -- Got Signal Lock!  UDP port is %d\n",
                        __FUNCTION__, tuner_data->mPort);
            // return 'FALSE'; i.e. stop calling as soon as we have QAM lock...
            retVal = FALSE;
        }
        else
        {
            // Lost signal lock
            event = RI_TUNER_EVENT_NOSYNC;
        }

        // If we have a tune callback, send notification 
        if (tuner_data->event_cb != NULL)
        {
            tuner_data->event_cb(event, tuner_data->cb_data);
        }
    }

    RILOG_TRACE("%s -- Exit - %s\n", __FUNCTION__, boolStr(retVal));
    return retVal;
}

/**
 * Get the current PID list for this tune
 *
 * @param tuner The "this" pointer
 * @param list The destination of the PID list, caller must provide memory
 * @param list_size The size of the destination PID list the caller provided
 * @return An error code detailing the success or failure of the request.
 */
ri_error get_tuner_pidlist(ri_tuner_t* tuner, uint16_t* list,
                                              uint32_t* list_size)
{
    RILOG_DEBUG("%s(object:%p, %p, %p);\n", __func__, tuner, list, list_size);

    if ((NULL != tuner) && (NULL != list) && (NULL != list_size))
    {
        ri_tuner_data_t* data = tuner->data;

        if (NULL != data)
        {
            int i, pid;

            for (i = pid = 0; (i < *list_size) && pid < 0x1FFF; i++)
            {
                while (pid++ < 0x1FFF)
                {
                    if (0 != data->mPidlist[pid])
                    {
                        list[i] = pid;
                        RILOG_INFO("%s PID[%d] = 0x%X\n", __func__, i, list[i]);
                        break;
                    }
                }
            }

            *list_size = i-1;
            RILOG_DEBUG("%s list_size = %d\n", __func__, *list_size);
            return RI_ERROR_NONE;
        }
        else
        {
            RILOG_WARN("%s -- Illegal Argument, tuner->data = NULL\n",
                    __FUNCTION__);
        }
    }
    else
    {
        RILOG_WARN("%s -- Illegal Argument, tuner = NULL\n", __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_ILLEGAL_ARG;
}

/**
 * Add transport stream PID
 *
 * @param object The "this" pointer
 * @param pid the PID to add to the currently tuned transport stream
 * @return An error code detailing the success or failure of the request.
 */
static ri_error add_TS_pid(ri_tuner_t* object, uint16_t pid)
{
    ri_error retVal = RI_ERROR_ILLEGAL_ARG;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    RILOG_DEBUG("%s(object:%p, PID:%X);\n", __func__, object, pid);

    if (object)
    {
        ri_tuner_data_t* data = object->data;

        if (NULL != data)
        {
            g_mutex_lock(data->mutex);
            data->mPidlist[pid]++;

            if (1 == data->mPidlist[pid])
            {
                RILOG_DEBUG("%s - added PID:0x%X for tuner %d\n",
                            __func__, pid, data->mIndex);
                switch (data->mType)
                {
                    case VLC:
                        retVal = vlc_TunerUpdatePidList(data->mIndex,
                                                        data->mPidlist);
                        g_mutex_unlock(data->mutex);
                        return retVal;
                    case HDHR:
                        retVal = hdhr_TunerUpdatePidList(data->mIndex,
                                                         data->mPidlist);
                        g_mutex_unlock(data->mutex);
                        return retVal;
                    case GST:
                        retVal = gst_TunerUpdatePidList(data->mIndex,
                                                        data->mPidlist);
                        g_mutex_unlock(data->mutex);
                        return retVal;
                    default:
                        RILOG_ERROR("%s -- unrecognized tuner type: %d\n",
                                    __FUNCTION__, data->mType);
                        break;
                }
            }
            else
            {
                RILOG_DEBUG("%s - incremented PID:0x%X count for tuner %d\n",
                            __func__, pid, data->mIndex);
                g_mutex_unlock(data->mutex);
                return RI_ERROR_NONE;
            }

            g_mutex_unlock(data->mutex);
            retVal = RI_ERROR_GENERAL;
        }
        else
        {
            RILOG_WARN("%s -- Illegal Argument, object->data = NULL\n",
                    __FUNCTION__);
        }
    }
    else
    {
        RILOG_WARN("%s -- Illegal Argument, object = NULL\n", __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

/**
 * Remove transport stream PID
 *
 * @param object The "this" pointer
 * @param pid the PID to remove from the currently tuned transport stream
 * @return An error code detailing the success or failure of the request.
 */
static ri_error remove_TS_pid(ri_tuner_t* object, uint16_t pid)
{
    ri_error retVal = RI_ERROR_ILLEGAL_ARG;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    RILOG_DEBUG("%s -- (object:%p, PID:%X);\n", __FUNCTION__, object, pid);

    if (object)
    {
        ri_tuner_data_t* data = object->data;

        if (NULL != data)
        {
            g_mutex_lock(data->mutex);

            if (1 == data->mPidlist[pid])
            {
                RILOG_DEBUG("%s - removed PID:0x%X for tuner %d\n",
                            __func__, pid, data->mIndex);
                data->mPidlist[pid]--;

                switch (data->mType)
                {
                    case VLC:
                        retVal = vlc_TunerUpdatePidList(data->mIndex,
                                                        data->mPidlist);
                        g_mutex_unlock(data->mutex);
                        return retVal;
                    case HDHR:
                        retVal = hdhr_TunerUpdatePidList(data->mIndex,
                                                         data->mPidlist);
                        g_mutex_unlock(data->mutex);
                        return retVal;
                    case GST:
                        retVal = gst_TunerUpdatePidList(data->mIndex,
                                                        data->mPidlist);
                        g_mutex_unlock(data->mutex);
                        return retVal;
                    default:
                        RILOG_ERROR("%s -- unrecognized tuner type: %d\n",
                                    __FUNCTION__, data->mType);
                        break;
                }
            }
            else if (0 != data->mPidlist[pid])
            {
                RILOG_DEBUG("%s - decremented PID:0x%X count for tuner %d\n",
                            __func__, pid, data->mIndex);
                data->mPidlist[pid]--;
                g_mutex_unlock(data->mutex);
                return RI_ERROR_NONE;
            }

            g_mutex_unlock(data->mutex);
            retVal = RI_ERROR_GENERAL;
        }
        else
        {
            RILOG_WARN("%s -- Illegal Argument, object->data = NULL\n",
                    __FUNCTION__);
        }
    }
    else
    {
        RILOG_WARN("%s -- Illegal argument, object = NULL", __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

/**
 * Add OOB PID
 *
 * @param pid the PID to add to the currently tuned transport stream on tuner 0
 * @return An error code detailing the success or failure of the request.
 */
ri_error add_OOB_pid(uint16_t pid)
{
    RILOG_DEBUG("%s(PID:%X);\n", __func__, pid);
    ri_tuner_t* tuner = &_tuners[0];	// OOB is always on tuner 0

    return add_TS_pid(tuner, pid);
}

/**
 * Remove OOB PID
 *
 * @param pid PID to remove from the currently tuned transport stream on tuner 0
 * @return An error code detailing the success or failure of the request.
 */
ri_error remove_OOB_pid(uint16_t pid)
{
    RILOG_DEBUG("%s(PID:%X);\n", __func__, pid);
    ri_tuner_t* tuner = &_tuners[0];	// OOB is always on tuner 0

    return remove_TS_pid(tuner, pid);
}

ri_tuner_t* create_tuner(GstElement** input, ri_pipeline_t* pipeline)
{
    char cfgVar[80] = {0};
    ri_tuner_t* tuner = NULL;
    ri_tuner_data_t* data;
    long controlPort;
    char* ate_freq_str = NULL;
    char *ipAddr = NULL;
    char* srvrIp = NULL;
    char* type = NULL;
    char* port = NULL;

    riTunerCat = log4c_category_get("RI.Tuner");

    RILOG_TRACE("%s -- Entry", __FUNCTION__);

    if (tuner_index < MAX_TUNERS)
    {
        tuner = &_tuners[tuner_index];
    }

    if (tuner == NULL)
    {
        RILOG_ERROR("%s -- couldn't find tuner!?\n", __FUNCTION__);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return NULL;
    }

    // Register implementation functions
    tuner->request_tune = request_tune;
    tuner->request_status = request_status;
    tuner->register_tuner_event_cb = register_tuner_event_cb;
    tuner->unregister_tuner_event_cb = unregister_tuner_event_cb;
    tuner->add_TS_pid = add_TS_pid;
    tuner->remove_TS_pid = remove_TS_pid;

    // Allocate our data structure
    data = g_try_malloc0(sizeof(ri_tuner_data_t));

    if (data == NULL)
    {
        RILOG_ERROR("%s -- couldn't allocate data!?\n", __FUNCTION__);
        g_free(tuner);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return NULL;
    }

    data->event_cb = NULL;
    data->mutex = g_mutex_new();
    data->mIndex = tuner_index;

    // Get the RI Platform IP address the Streamer should talk to...
    if (NULL == (ipAddr = ricfg_getValue("RIPlatform", "RI.Platform.IpAddr")))
    {
        ipAddr = "127.0.0.1";
        RILOG_WARN("%s -- RI Platform IP address not specified!\n",
                __FUNCTION__);
    }

    if (NULL == (type = ricfg_getValue("RIPlatform", "RI.Headend.tunerType")))
    {
#ifdef WIN32
        type = "VLC";       // default to VLC tuner type for win32 builds
#else
        type = "GST";       // default to GST tuner type for all others (linux)
#endif
        RILOG_WARN("%s -- tuner type not specified!\n", __FUNCTION__);
    }

    sprintf(cfgVar, "RI.Headend.tuner.%d.%s", data->mIndex, "TunerRxPort");
    if (NULL == (port = ricfg_getValue("RIPlatform", cfgVar)))
    {
        port = "4140";
        RILOG_WARN("%s -- %s not specified!\n", __FUNCTION__, cfgVar);
    }

    RILOG_INFO("%s -- got %s, %s:%s\n", __FUNCTION__, type, ipAddr, port);
    strncpy(data->mIpAddr, ipAddr, INET6_ADDRSTRLEN);
    data->mPort = atoi(port);

    sprintf(cfgVar, "RI.Headend.tuner.%d.%s", tuner_index, "StreamerIp");
    if (NULL == (srvrIp = ricfg_getValue("RIPlatform", cfgVar)))
    {
        srvrIp = "127.0.0.1";
        RILOG_WARN("%s -- %s not specified!\n", __FUNCTION__, cfgVar);
    }

    sprintf(cfgVar, "RI.Headend.tuner.%d.%s", tuner_index, "StreamerPort");
    if (NULL == (port = ricfg_getValue("RIPlatform", cfgVar)))
    {
        port = "4212";
        RILOG_WARN("%s -- %s not specified!\n", __FUNCTION__, cfgVar);
    }

    RILOG_INFO("%s -- got %s, %s:%s\n", __FUNCTION__, type, srvrIp, port);

    // Get the ATE frequency (if used)
    if (NULL == (ate_freq_str = ricfg_getValue("RIPlatform",
                                               "RI.Headend.ate.frequency")))
    {
        ate_freq_str = "0";
        RILOG_WARN("%s -- ATE frequency not specified!\n", __FUNCTION__);
    }

    // convert frequency string to a long and divide down to KHz
    data->ate_frequency = atol(ate_freq_str) / 1000;

    if (strstr(type, "VLC"))
    {
        data->mType = VLC;
        controlPort = atoi(port);
        vlc_TunerInit(tuner_index, srvrIp, (int)controlPort, StreamerPwd);
    }
    else if (strstr(type, "HDHR"))
    {
        data->mType = HDHR;
        controlPort = strtol(port, NULL, 16);
        hdhr_TunerInit(tuner_index, controlPort);
    }
    else if (strstr(type, "GST"))
    {
        data->mType = GST;
        gst_TunerInit(tuner_index, data->mPort);

        // see if we are connecting directly to the input pipeline...
        if (ricfg_getBoolValue("RIPlatform","RI.Platform.tunerConnectDirect"))
        {
            // only override the input element if we are not going over UDP...
            *input = gst_GetTunerElement(tuner_index);
        }
    }
    else
    {
        RILOG_ERROR("%s -- unrecognized tuner type: %s\n", __FUNCTION__, type);
    }

    RILOG_DEBUG("%s -- tuner_index = %d, tuner = %p\n", __FUNCTION__,
            tuner_index, tuner);

    tuner_index++;

    // Assign GStreamer pipeline elements and data
    data->gst_udpsrc = *input;
    data->pipeline = pipeline;
    data->status.frequency = 0;
    data->status.mode = RI_MODULATION_UNKNOWN;
    data->status.program_num = -1;
    tuner->data = data;

    test_RegisterMenu(&TunerMenuItem);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return tuner;
}

void set_tuner_udp_port(ri_tuner_t* tuner, uint16_t udp_port)
{
    GValue prop =
    { 0, };

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    ri_tuner_data_t* data = (ri_tuner_data_t*) tuner->data;

    RILOG_DEBUG("%s -- tuner = %p, port = %d\n", __FUNCTION__, tuner, udp_port);

    // Set the port for the udp src
    (void) g_value_init(&prop, G_TYPE_INT);
    g_value_set_int(&prop, udp_port);
    g_object_set_property(G_OBJECT(data->gst_udpsrc), "port", &prop);
    g_value_unset(&prop);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void set_tuner_udp_buffer_size(ri_tuner_t* tuner, uint32_t buffer_size)
{
    GValue prop =
    { 0, };

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    ri_tuner_data_t* data = (ri_tuner_data_t*) tuner->data;

    RILOG_DEBUG("%s -- tuner = %p, buffer size = %d\n", __FUNCTION__, tuner,
            buffer_size);

    // Set the kernel buffer size for the udp src
    (void) g_value_init(&prop, G_TYPE_INT);
    g_value_set_int(&prop, buffer_size);
    g_object_set_property(G_OBJECT(data->gst_udpsrc), "buffer-size", &prop);
    g_value_unset(&prop);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Orders a stream to stop.  Not a normal tuner operation,
 * as it typically is tuned somewhere.
 */
void tunerStop(ri_tuner_data_t* tuner_data)
{
    RILOG_DEBUG("%s -- Entry, (%p)\n", __FUNCTION__, tuner_data);

    switch (tuner_data->mType)
    {
        case VLC:
            if (vlc_TunerIsStreaming(tuner_data->mIndex))
            {
                vlc_TunerStop(tuner_data->mIndex);
            }
            else
            {
                RILOG_INFO("%s -- nothing to do, tuner not streaming...\n",
                        __FUNCTION__);
            }
            break;

        case HDHR:
            if (hdhr_TunerIsStreaming(tuner_data->mIndex))
            {
                hdhr_TunerStop(tuner_data->mIndex);
            }
            else
            {
                RILOG_INFO("%s -- nothing to do, tuner not streaming...\n",
                        __FUNCTION__);
            }
            break;

        case GST:
            if (gst_TunerIsStreaming(tuner_data->mIndex))
            {
                gst_TunerStop(tuner_data->mIndex);
            }
            else
            {
                RILOG_INFO("%s -- nothing to do, tuner not streaming...\n",
                        __FUNCTION__);
            }
            break;

        default:
            RILOG_ERROR("%s -- unrecognized tuner type: %d\n",
                        __FUNCTION__, tuner_data->mType);
            break;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Shut-down Tuner instance
 */
void tunerExit(ri_tuner_data_t* tuner_data)
{
    RILOG_DEBUG("%s -- Entry, (%p)\n", __FUNCTION__, tuner_data);

    switch (tuner_data->mType)
    {
        case VLC:
            vlc_TunerExit(tuner_data->mIndex);
            break;

        case HDHR:
            hdhr_TunerExit(tuner_data->mIndex);
            break;

        case GST:
            gst_TunerExit(tuner_data->mIndex);
            break;

        default:
            RILOG_ERROR("%s -- unrecognized tuner type: %d\n",
                        __FUNCTION__, tuner_data->mType);
            break;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Re-tune Tuner instance
 */
void tunerRetune(ri_tuner_data_t* tuner_data)
{
    riTunerCat = log4c_category_get("RI.Tuner");

    RILOG_TRACE("%s -- Entry", __FUNCTION__);

    switch (tuner_data->mType)
    {
        case VLC:
            vlc_TunerRetune(tuner_data->mIndex);
            break;

        case HDHR:
            RILOG_WARN("%s called for HDHR tuner!?\n", __FUNCTION__);
            break;

        case GST:
            gst_TunerRetune(tuner_data->mIndex);
            break;

        default:
            RILOG_ERROR("%s -- unrecognized tuner type: %d\n",
                        __FUNCTION__, tuner_data->mType);
            break;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

static ri_bool tune(ri_tuner_data_t* tuner_data)
{
    ri_bool retVal = FALSE;
    char cfgVar[80] = {0};
    Stream stream;
    RILOG_TRACE("%s -- Entry, (%p)\n", __FUNCTION__, tuner_data);

    if (NULL != tuner_data)
    {
        // fill in the current stream information
        stream.program = tuner_data->params.program_num;
        strcpy(stream.modulation, modulationString(tuner_data->params.mode));
        stream.frequency = tuner_data->params.frequency/1000;

        RILOG_INFO("%s -- got %lu, %s, %d\n", __FUNCTION__, stream.frequency,
                stream.modulation, stream.program);

        if (tuner_data->mType == HDHR)
        {
            sprintf(stream.srcUrl, "auto:%u", tuner_data->params.frequency);
        }
        else if (stream.frequency == tuner_data->ate_frequency)
        {
            strncpy(stream.srcUrl, vlc_GetTspFileURL(), MAXURLLEN - 1);
        }
        else
        {
            char* url;

            // Query the configuration file for a stream URL that matches
            // the requested frequency/qam
            sprintf(cfgVar, "RI.Headend.vlc.frequency.%u_%s",
                    tuner_data->params.frequency, stream.modulation);

            if (NULL != (url = ricfg_getValue("RIPlatform", cfgVar)))
            {
                strncpy(stream.srcUrl, url, MAXURLLEN - 1);
            }
            else
            {
                RILOG_ERROR("%s -- Exit - INVALID TUNE REQUEST! Frequency(%u) "
                           "modulation(%s) not specified in config file!\n",
                           __func__, tuner_data->params.frequency,
                           stream.modulation);
                return FALSE;
            }
        }

        strcpy(stream.destinationAddress, tuner_data->mIpAddr);
        stream.destinationPort = tuner_data->mPort;
        RILOG_INFO("%s -- %s:%d\n", __FUNCTION__, 
                   stream.destinationAddress, stream.destinationPort);

        switch (tuner_data->mType)
        {
            case VLC:
                retVal = vlc_TunerTune(tuner_data->mIndex, &stream);
                break;

            case HDHR:
                retVal = hdhr_TunerTune(tuner_data->mIndex, &stream);
                break;

            case GST:
                retVal = gst_TunerTune(tuner_data->mIndex, &stream);
                break;

            default:
                RILOG_ERROR("%s -- unrecognized tuner type: %d\n",
                            __FUNCTION__, tuner_data->mType);
                break;
        }
    }

    RILOG_DEBUG("%s -- Exit - %s\n", __FUNCTION__, boolStr(retVal));
    return retVal;
}

