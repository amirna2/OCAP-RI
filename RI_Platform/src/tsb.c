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

//lint -e429 Suppress PC-Lint custodial pointer warning for pCommand


#include <glib.h>
#include <stdlib.h>
#include <string.h>

#include <ri_config.h>
#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>
#include <ri_types.h>
#include <ri_log.h>
#include <inttypes.h>
#include "gst_utils.h"

#include "gstreamer/gstpidfilter.h"
#include "pipeline_manager.h"
#include "pipeline.h"
#include "tsb.h"
#include "video_device.h"

// code associated with the following may be deleted once TSB functionality is complete
#define TEMP_DEBUG_AID

#define RILOG_CATEGORY tsbCategory
log4c_category_t* tsbCategory = NULL;

#define CONVERT_SEC_TO_NS 1000000000LL  // multiplier for converting seconds to nanoseconds
#define CONVERT_MS_TO_NS  1000000LL  // multiplier for converting milliseconds to nanoseconds
#define DEFAULT_TSB_THREAD_WAKE_INTERVAL 250000  // Interval for TSB notifications in uS (default to 1/4 second)
#define MAX_PIDS_PER_ACTION 32       // 32 pids for TSB and 32 pids for decode
#define ELEMENT_STATE_CHANGE_TIMEOUT 500000000 // Wait 0.5S for elements to change state
#ifndef max
#define max(a,b) (((a) > (b)) ? (a) : (b))
#endif
#ifndef min
#define min(a,b) (((a) < (b)) ? (a) : (b))
#endif

// these represent commands to the TSB thread
typedef enum _tsb_thread_action
{
    TSB_ACTION_START,
    TSB_ACTION_STOP,
    TSB_ACTION_CONVERT,
    TSB_ACTION_CONVERT_STOP,
    TSB_ACTION_WAKE_THREAD_ONLY,

} tsb_thread_action;

typedef struct _tsb_thread_command
{
    tsb_thread_action action;
    // TSB_ACTION_CONVERT_START & TSB_ACTION_SET_DURATION has data which is:
    //   uint64_t starttime;                 // Starttime in nS from original TSB creation time (not for set_duration)
    uint32_t duration; // Requested duration in seconds
} tsb_thread_command;

// Local prototypes
gpointer tsb_thread(gpointer data);

// forward TSB conversion state declarations
static doConversionState tsbConversionStateIdle(tsb_item_t* pTsbItem);
static doConversionState tsbConversionStateStarting(tsb_item_t* pTsbItem);
static doConversionState tsbConversionStateActive(tsb_item_t* pTsbItem);
static doConversionState tsbConversionStateStopping(tsb_item_t* pTsbItem);
//static doConversionState tsbConversionStateStopped(ri_pipeline_t* pPipeline);

// TODO: The number of elements in the TSB table must match that declared for
// MAX_TSBS in mpeos_dvr.c it should be moved to ri_pipeline.h for access
// by both source modules.
#define MAX_TSBS 50

/**
 * This table contains an entry for all TSBs known to the system.  It is used
 * to maintain transitory associations between a TSB and a pipeline
 */
static tsb_item_t tsb_table[MAX_TSBS] =
{
{ NULL, NULL }, };
/**
 * Utility function for obtaining the TSB table entry corresponding to the
 * specified tsb handle.
 */
tsb_item_t* getTsbEntry(ri_tsbHandle* tsb)
{
    int i;
    tsb_item_t* retVal = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // look for the entry in the tsb table
    for (i = 0; i < (int) (sizeof(tsb_table) / sizeof(tsb_table[0])); i++)
    {
        if ((tsb_table[i].pTsb != NULL) && ((IfsInfo*) tsb
                == tsb_table[i].pTsb->pIfsInfo))
        {
            // the tsb entry was found, so return it
            retVal = &tsb_table[i];
            break;
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return retVal;
}

/**
 * Utility function for obtaining the pipeline from the TSB table entry
 * corresponding to the specified tsb file name.
 */
ri_pipeline_t* getPipelineFromTsbFileName(char* filename)
{
    int i;
    ri_pipeline_t* retVal = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // look for the entry in the tsb table
    for (i = 0; i < (int) (sizeof(tsb_table) / sizeof(tsb_table[0])); i++)
    {
        if (tsb_table[i].pTsb != NULL)
        {
            if (0 == strcmp(tsb_table[i].pTsb->pIfsInfo->name, filename))
            {
                // the tsb entry was found, so return it
                retVal = tsb_table[i].pPipeline;
                break;
            }
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return retVal;
}

//-------------------------------------------------------------------------------------------
/**
 * Create a new timeshift buffer
 *
 * @param path The absolute path on disk under which all data for this TSB
 *             should be stored
 * @param duration The requested duration (in seconds) of this TSB
 * @param handle The location where the platform will return the unique
 *        identifier for this TSB.
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
 */
ri_error tsb_init(const char * path, uint64_t duration, ri_tsbHandle * tsb)
{
    int i;
    IfsHandle ifsHandle;
    IfsReturnCode ifsReturnCode;
    ri_error rc = RI_ERROR_GENERAL; // assume failure

    if (NULL == tsbCategory)
        tsbCategory = log4c_category_get("RI.Pipeline.TSB");

    RILOG_TRACE("%s -- Entry, duration = %"PRIu64" sec\n", __FUNCTION__, duration);

    // check input parameters
    if ((NULL != path) && (0 != duration) && (NULL != tsb))
    {
        // open an IFS file
        ifsReturnCode = IfsOpenWriter(path, // const char * path      Input
                NULL, // const char * name      Input  (if NULL the name is generated)
                duration, // IfsTime maxSize        Input  (in seconds, 0 = no max)
                &ifsHandle); // IfsHandle * pIfsHandle Output (use IfsClose() to free)

        // if the file was opened w/o error...
        if (IfsReturnCodeNoErrorReported == ifsReturnCode)
        {
            // ...get information about the IFS file
            ifsReturnCode = IfsHandleInfo(ifsHandle, (IfsInfo**) tsb);
            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                // the IFS file was opened w/o error
                RILOG_INFO(
                        "%s -- IfsHandleInfo: path = %s, name = %s, maxSize = %lu\n",
                        __FUNCTION__, ((IfsInfo*) *tsb)->path,
                        ((IfsInfo*) *tsb)->name, ((IfsInfo*) *tsb)->maxSize);

                // note that the handle info structure needs to be freed
                //  in the tsb_delete() function

                // now close the TSB handle
                ifsReturnCode = IfsClose(ifsHandle);
                if (IfsReturnCodeNoErrorReported == ifsReturnCode)
                {
                    // the TSB handle closed w/o error,
                    // look for an empty entry in the tsb table
                    for (i = 0; i < (int) (sizeof(tsb_table)
                            / sizeof(tsb_table[0])); i++)
                    {
                        if (NULL == tsb_table[i].pTsb)
                        {
                            // an empty entry was found, so allocate a new tsb entry
                            // and set all entries to 0
                            tsb_table[i].pTsb
                                    = g_try_malloc0(sizeof(ri_tsb_data_t));

                            if (NULL != tsb_table[i].pTsb)
                            {
                                // save the IfsInfo pointer as the TSB handle
                                tsb_table[i].pTsb->pIfsInfo = (IfsInfo*) *tsb;
                                tsb_table[i].pPipeline = NULL;
                                tsb_table[i].pTsb->tsb_maximum_duration_ns = 
                                    (duration * NSEC_PER_SEC);

                                g_static_rec_mutex_init(
                                        &(tsb_table[i].pTsb->tsb_mutex));

                                // indicate that everything is good
                                rc = RI_ERROR_NONE;
                            }
                            else
                            {
                                RILOG_ERROR(
                                        "%s -- Unable to allocate memory for TSB table entry\n",
                                        __FUNCTION__);
                            }
                            break;
                        }
                    }
                }
                else
                {
                    RILOG_WARN("%s --Error calling IfsClose(): %s\n",
                            __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
                }
            }
            else
            {
                RILOG_ERROR("%s --Error calling IfsHandleInfo(): %s\n",
                        __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
            }
        }
        else
        {
            RILOG_ERROR("%s -- Unable to open file: %s\n", __FUNCTION__,
                    IfsReturnCodeToString(ifsReturnCode));
        }
    }
    else
    {
        rc = RI_ERROR_ILLEGAL_ARG;
    } // endif mandatory parms are not NULL/zero

    if (NULL == tsb)
    {
        RILOG_TRACE("%s -- Exit, returned tsb handle = NULL\n", __FUNCTION__);
    }
    else
    {
        RILOG_TRACE("%s -- Exit, returned tsb handle = %p\n", __FUNCTION__,
                *tsb);
    }
    return rc;
    /* tsb_init */
}

/**
 * Modifies the duration of the given TSB.  For durations smaller that the current
 * duration (shrink):
 *     - If the TSB is currently wrapped (start time non-zero) OR if the new
 *       duration would cause the TSB to start wrapping, the buffer will be
 *       reduced from the start of recorded content
 *     - Otherwise, the buffer will be reduced from the end of the buffer
 * For durations larger than the current duration (grow):
 *     - If the TSB is currently wrapped, then it will no longer be wrapped
 *       and any future or current buffering will be into newly allocated buffer
 *       space
 *     - Otherwise, the buffer will be extended to the new size
 *
 * @param tsb The unique TSB identifier
 * @param duration The required duration (in seconds) of the TSB.
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 *    RI_ERROR_NO_TSB: No TSB has been started on this pipeline
 */
ri_error tsb_set_duration(ri_tsbHandle tsb, uint64_t duration)
{
    IfsHandle ifsHandle;
    IfsReturnCode ifsReturnCode;
    ri_error rc = RI_ERROR_GENERAL; // assume failure

    RILOG_TRACE("%s -- Entry, tsb = %p, duration = %"PRIu64" sec\n", __FUNCTION__, tsb, duration);

    if ((NULL != tsb) && (0 != duration))
    {
        // open the IFS file
        ifsReturnCode = IfsOpenWriter(((IfsInfo*) tsb)->path, // const char * path      Input
                ((IfsInfo*) tsb)->name, // const char * name      Input  (if NULL the name is generated)
                ((IfsInfo*) tsb)->maxSize, // IfsTime maxSize        Input  (in seconds, 0 = no max)
                &ifsHandle); // IfsHandle * pIfsHandle Output (use IfsClose() to free)

        // if the file was opened w/o error...
        if (IfsReturnCodeNoErrorReported == ifsReturnCode)
        {
            // ...get information about the IFS file
            ifsReturnCode = IfsSetMaxSize(ifsHandle, // IfsHandle ifsHandle Input (must be a writer)
                    duration); // IfsTime   maxSize   Input (in seconds, 0 is illegal)
            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                RILOG_INFO("%s IfsSetMaxSize succeeded\n", __FUNCTION__);

                // now close the TSB handle
                ifsReturnCode = IfsClose(ifsHandle);
                if (IfsReturnCodeNoErrorReported == ifsReturnCode)
                {
                    // the TSB handle closed w/o error, so say that everything is good
                    rc = RI_ERROR_NONE;
                }
                else
                {
                    RILOG_WARN("%s -- Error calling IfsClose(): %s\n",
                            __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
                }
            }
            else
            {
                RILOG_WARN("%s -- Error calling IfsSetMaxSize(): %s\n",
                        __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
            }
        }
        else
        {
            RILOG_ERROR("%s -- Unable to open file: %s\n", __FUNCTION__,
                    IfsReturnCodeToString(ifsReturnCode));
        }
    }
    else
    {
        rc = RI_ERROR_ILLEGAL_ARG;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Clears all data from the given TSB.  The TSB must not be currently in use
 * for the operation to succeed.
 *
 * @param tsb The unique TSB identifier
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_RECORDING_IN_USE: The given TSB is currently being recorded into
 *       or played back.
 */
ri_error tsb_flush(ri_tsbHandle tsb)
{
    IfsHandle ifsHandle;
    IfsReturnCode ifsReturnCode;
    ri_error rc = RI_ERROR_GENERAL; // assume failure

    RILOG_TRACE("%s -- Entry, tsb = %p\n", __FUNCTION__, tsb);

    if (NULL != tsb)
    {
        // open the IFS file
        ifsReturnCode = IfsOpenWriter(((IfsInfo*) tsb)->path, // const char * path      Input
                ((IfsInfo*) tsb)->name, // const char * name      Input  (if NULL the name is generated)
                ((IfsInfo*) tsb)->maxSize, // IfsTime maxSize        Input  (in seconds, 0 = no max)
                &ifsHandle); // IfsHandle * pIfsHandle Output (use IfsClose() to free)

        // if the file was opened w/o error...
        if (IfsReturnCodeNoErrorReported == ifsReturnCode)
        {
            ifsReturnCode = IfsStop(ifsHandle); // IfsHandle ifsHandle Input (must be a writer)
            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                // the IFS file was stopped w/o error
                RILOG_INFO("%s -- IfsStop succeeded\n", __FUNCTION__);

                // now close the TSB handle
                ifsReturnCode = IfsClose(ifsHandle);
                if (IfsReturnCodeNoErrorReported == ifsReturnCode)
                {
                    // the TSB handle closed w/o error, so say that everything is good
                    rc = RI_ERROR_NONE;
                }
                else
                {
                    RILOG_WARN("%s -- Error calling IfsClose(): %s\n",
                            __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
                }
            }
            else
            {
                RILOG_ERROR("%s -- Error calling IfsStop(): %s\n",
                        __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
            }
        }
        else
        {
            RILOG_ERROR("%s -- Unable to open file: %s\n", __FUNCTION__,
                    IfsReturnCodeToString(ifsReturnCode));
        }
    }
    else
    {
        rc = RI_ERROR_ILLEGAL_ARG;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Deletes the time shift buffer (TSB) associated with this pipeline including
 * all media and metadata files from the filesystem. If a TSB playback is
 * currently taking place, the playback is stopped exactly as if playback_stop()
 * was called.  If buffering is currently taking place, it will be stopped exactly
 * as if tsb_stop() was called.  If a TSB conversion is currently taking place, the
 * conversion will terminate exactly as if tsb_convert_stop() was called.
 *
 * @param tsb The unique TSB identifier
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_NO_TSB: The given TSB handle is invalid
 */
ri_error tsb_delete(ri_tsbHandle tsb)
{
    if (NULL == tsbCategory)
    {
        tsbCategory = log4c_category_get("RI.Pipeline.TSB");
    }

    RILOG_TRACE("%s -- Entry, tsb = %p\n", __FUNCTION__, tsb);

    IfsReturnCode ifsReturnCode;
    ri_error rc = RI_ERROR_GENERAL; // assume failure

    if (NULL != tsb)
    {
        tsb_item_t* pTsbItem = getTsbEntry(tsb);
        if (NULL != pTsbItem)
        {
            // the tsb entry was found, so free the mutex and...
            g_static_rec_mutex_free(&(pTsbItem->pTsb->tsb_mutex));

            // ...the memory, effectively removing the TSB from the table
            g_free(pTsbItem->pTsb);
            pTsbItem->pTsb = NULL;

            ifsReturnCode = IfsFreeInfo((IfsInfo*) tsb);
            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                rc = RI_ERROR_NONE;
            }
            else
            {
                RILOG_ERROR("%s -- Error calling IfsFreeInfo(): %s\n",
                        __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
            }
        }
        else
        {
            RILOG_ERROR("%s -- No valid TSB\n", __FUNCTION__);
        }
    }
    else
    {
        rc = RI_ERROR_ILLEGAL_ARG;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
} /* tsb_delete */

#ifdef TEMP_DEBUG_AID
char* getMediaTypeString(ri_media_type mediaType)
{
    char* retVal = "unknown";

    char
            * mediaTypeStrings[] =
            { "RI_MEDIA_TYPE_UNKNOWN", "RI_MEDIA_TYPE_VIDEO",
                    "RI_MEDIA_TYPE_AUDIO", "RI_MEDIA_TYPE_DATA",
                    "RI_MEDIA_TYPE_SUBTITLES", "RI_MEDIA_TYPE_SECTIONS",
                    "RI_MEDIA_TYPE_PCR", "RI_MEDIA_TYPE_PMT" };

    if (mediaType < (int) (sizeof(mediaTypeStrings)
            / sizeof(mediaTypeStrings[0])))
    {
        retVal = mediaTypeStrings[(int) mediaType];
    }
    return retVal;
}
#endif

//----------------------------------------------------------------------------

/**
 * Begin recording into this time shift buffer.
 *
 * @param pPipeline The "this" pointer
 * @param tsb The buffer that should be used for recording.
 * @param pids An array of pids that should be played back.  Upon return, this
 *             array is updated to contain the actual recorded PIDs
 * @param pid_count The number of pids in the array
 * @param callback A callback function that will receive all recording and
 *                 playback-related events involving this TSB
 * @param cb_data User data that will be passed in every callback invocation
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
 *    RI_ERROR_ALREADY_EXISTS: A TSB has already been created for this
 *    pipeline.
 */
ri_error tsb_start(ri_pipeline_t* pPipeline, ri_tsbHandle tsb,
        ri_pid_info_t* pids, uint32_t pid_count, ri_dvr_callback_f callback,
        void* cb_data)
{
    char* val = NULL;

    if (NULL == tsbCategory)
    {
        tsbCategory = log4c_category_get("RI.Pipeline.TSB");
    }
    GError *pEerror; // Used to collect error from thread create
    RILOG_TRACE("%s -- Entry, pipeline = %s, tsb = %p\n", __FUNCTION__,
            gst_element_get_name(pPipeline->data->gst_pipeline), tsb);

#ifdef TEMP_DEBUG_AID
    RILOG_INFO("%s -- Dump of PIDs:\n", __FUNCTION__);
    uint32_t j;
    for (j = 0; j < pid_count; j++)
    {
        RILOG_INFO("\t0x%4.4X, type is %s\n", pids[j].srcPid,
                getMediaTypeString(pids[j].mediaType));
    }
#endif

    uint32_t i;
    ri_error rc = RI_ERROR_NONE; // assume success
    tsb_thread_command *pCommand; // Used to tell thread we started buffering

    for (i = 0; i < pid_count; i++)
    {
        // can be called for pipelines w/o a tuner (playback)
        if (NULL != pPipeline->data->tuner)
        {
            (void)pPipeline->data->tuner->add_TS_pid(pPipeline->data->tuner,
                                                 (pids[i].srcPid & 0x1FFF));
        }
    }

    /* Parm checking */
    if (pPipeline && pids && pid_count && (pid_count <= MAX_PIDS_PER_ACTION))
    {
        tsb_item_t* pTsbItem = getTsbEntry(tsb);
        if (NULL != pTsbItem)
        {
            if (NULL == pTsbItem->pPipeline)
            {
                // associate the specified pipeline with the specified TSB
                pTsbItem->pPipeline = pPipeline;

                g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

                // Copy the pid information to tsb instance structure
                pPipeline->data->tsb_pids = g_try_malloc(pid_count
                        * sizeof(ri_pid_info_t));
                if (NULL != pPipeline->data->tsb_pids)
                {
                    memcpy(pPipeline->data->tsb_pids, pids, pid_count
                            * sizeof(ri_pid_info_t));
                    pPipeline->data->tsb_pid_count = pid_count;

                    // Update the preprocessor to allow the TSB pids through
                    update_preproc(pPipeline);

                    // Fix up the destination pids in the in/out pids structure to reflect 1:1
                    for (i = 0; i < pid_count; i++)
                    {
                        pids[i].recPid = pPipeline->data->tsb_pids[i].recPid;
                        pids[i].recFormat
                                = pPipeline->data->tsb_pids[i].recFormat;
                    }

                    g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
                    {
                        // Make note of callback info
                        if (NULL != callback)
                        {
                            pTsbItem->pTsb->callback = callback;
                            pTsbItem->pTsb->cb_data = cb_data;
                        }

                        // Create the tsb_thread to notify the client of status updates
                        // Set the TSB wake interval...
                        val = ricfg_getValue("RIPlatform",
                                             "RI.Platform.dvr.TsbWakeInterval");
                        if (NULL == val)
                        {
                            pTsbItem->pTsb->tsb_wake_interval =
                                        DEFAULT_TSB_THREAD_WAKE_INTERVAL;
                            RILOG_WARN("%s TSB wake interval not specified!\n",
                                        __FUNCTION__);
                        }
                        else
                        {
                            pTsbItem->pTsb->tsb_wake_interval = atoi(val);
                            RILOG_INFO("%s TSB wake interval set to %lu\n",
                                        __FUNCTION__,
                                        pTsbItem->pTsb->tsb_wake_interval);
                        }

                        pTsbItem->pTsb->tsb_queue = g_async_queue_new();
                        pTsbItem->pTsb->tsb_thread = g_thread_create(
                                tsb_thread, // func
                                (gpointer) pTsbItem, // data
                                TRUE, // joinable
                                &pEerror); // error

                        // Tell the thread we started
                        pCommand = g_try_malloc(sizeof(tsb_thread_command));
                        if (pCommand)
                        {
                            pCommand->action = TSB_ACTION_START;
                            g_async_queue_push(pTsbItem->pTsb->tsb_queue,
                                    pCommand);
                        }
                        else
                        {
                            RILOG_ERROR("%s -- Failed to start TSB thread",
                                    __FUNCTION__);

                            rc = RI_ERROR_GENERAL;
                        } /* endif allocation of thread command OK*/
                    }
                    g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
                }
                else
                {
                    rc = RI_ERROR_GENERAL;
                } /* endif alloc of pid structure OK */

                /* Finally release the pipeline mutex */
                g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));
            }
            else
            {
                // the pipeline is already associated with a TSB
                rc = RI_ERROR_ALREADY_EXISTS;
            }
        }
        else
        {
            RILOG_ERROR("%s -- No valid TSB\n", __FUNCTION__);
        }
    }
    else
    {
        rc = RI_ERROR_ILLEGAL_ARG;
    } /* endif mandatory parms are not NULL/zero */

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
} /* tsb_start */

/**
 * Request that the TSB associated with this pipeline stop buffering.  This will
 * reset the TSB to an empty state.
 *
 * @param object The "this" pointer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
 *    RI_ERROR_NO_TSB: If there is no TSB associated with this pipeline
 */
ri_error tsb_stop(ri_pipeline_t* pPipeline)
{
    if (NULL == tsbCategory)
    {
        tsbCategory = log4c_category_get("RI.Pipeline.TSB");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
            gst_element_get_name(pPipeline->data->gst_pipeline));

    // assume success
    ri_error rc = RI_ERROR_NONE;

    if (NULL != pPipeline)
    {
        // find the TSB table entry associated with the specified *pipeline*
        int i;
        tsb_item_t* pTsbItem = NULL;

        // look for the entry in the tsb table
        for (i = 0; i < (int) (sizeof(tsb_table) / sizeof(tsb_table[0])); i++)
        {
            if (pPipeline == tsb_table[i].pPipeline)
            {
                // the tsb entry was found, so quit
                pTsbItem = &tsb_table[i];
                break;
            }
        }

        if (NULL != pTsbItem)
        {
            tsb_thread_command *pCommand; // Used to tell thread to stop

            // send the TSB thread a TSB_ACTION_STOP command - note that this
            //  will signal the TSB state machine to begin shutting down
            pCommand = g_try_malloc(sizeof(tsb_thread_command));
            if (NULL != pCommand)
            {
                // Get the mutex so we can manipulate the pipeline
                g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));
                {
                    // delete any tsb PIDs for this pipeline
                    if (pPipeline->data->tsb_pids)
                    {
                        g_free(pPipeline->data->tsb_pids);
                        pPipeline->data->tsb_pids = NULL;
                        pPipeline->data->tsb_pid_count = 0;
                    } /* endif already had pids */

                    // Reconstruct the preproc pidlist with only decode pids
                    update_preproc(pPipeline);
                }
                g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

                // Tell the TSB thread to stop, and wait for it to do so
                RILOG_INFO("%s -- Sending stop command to tsb thread\n",
                        __FUNCTION__);
                pCommand->action = TSB_ACTION_STOP;
                g_async_queue_push(pTsbItem->pTsb->tsb_queue, pCommand);

                // Wait for the thread to finish, then delete the thread queue
                RILOG_DEBUG("%s -- Waiting for tsb thread to terminate\n",
                        __FUNCTION__);
                (void) g_thread_join(pTsbItem->pTsb->tsb_thread);

                RILOG_DEBUG(
                        "%s -- Finished waiting for tsb thread, deleting thread and queue\n",
                        __FUNCTION__);

                g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
                {
                    pTsbItem->pTsb->tsb_thread = NULL;
                    g_async_queue_unref(pTsbItem->pTsb->tsb_queue);
                    pTsbItem->pTsb->tsb_queue = NULL;

                    // dis-associate the specified TSB with the specified pipeline
                    pTsbItem->pPipeline = NULL;
                }
                g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
            }
            else
            {
                RILOG_ERROR(
                        "%s -- Failed to allocate memory for TSB_ACTION_STOP command\n",
                        __FUNCTION__);
                rc = RI_ERROR_GENERAL;
            }
        }
        else
        {
            RILOG_ERROR("%s -- No TSB associated with specified pipeline\n",
                    __FUNCTION__);
            rc = RI_ERROR_NO_TSB;
        }
    }
    else
    {
        rc = RI_ERROR_ILLEGAL_ARG;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Initiates the conversion of a time shift buffer recording to a permanent
 * recording.
 *
 * @param tsb The TSB from which to perform the conversion
 * @param rec_path An absolute path that indicates the directory under which all
 *                 files associated with this recording should be stored.
 * @param rec_name Upon successful return of this call, the platform will return the
 *                 unique recording name for this conversion
 * @param pids The pids from the TSB to convert (recPids only)
 * @param pid_count The number of pids in the pid array
 * @param inout_starttime The requested time (in nanoseconds), from the original
 *                        TSB start time, for the start of the conversion. Upon
 *                        successful return, the platform will return the actual
 *                        conversion start time.
 * @param duration The expected duration (in seconds) of the converted recording.
 * @param callback A callback function that will receive all events related to
 *                 the conversion of this TSB
 * @param cb_data User data that will be passed in every callback invocation
 *
 * @return Upon success, return RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 *    RI_ERROR_ALREADY_EXISTS:  A conversion is already taking place on this
 *                              TSB
 */
ri_error tsb_convert(ri_tsbHandle tsb, const char * rec_path,
        char rec_name[RI_MAX_RECORDING_NAME_LENGTH], ri_pid_info_t * pids,
        uint32_t pid_count, uint64_t * inout_starttime, uint32_t duration,
        ri_dvr_callback_f callback, void * cb_data)
{
    IfsInfo * pIfsInfo;
    IfsReturnCode ifsReturnCode;
    ri_error rc = RI_ERROR_GENERAL;
    tsb_thread_command *pCommand; // Used to tell thread to start conversion

    if (NULL == tsbCategory)
        tsbCategory = log4c_category_get("RI.Pipeline.TSB");

    RILOG_TRACE("%s -- Entry, tsb = %p, requested start time = %"PRIu64"\n",
            __FUNCTION__,
            tsb,
            *inout_starttime);

#ifdef TEMP_DEBUG_AID
    RILOG_INFO("%s -- Dump of PIDs:\n", __FUNCTION__);
    uint32_t j;
    for (j = 0; j < pid_count; j++)
    {
        RILOG_INFO("\t0x%4.4X, type is %s\n", pids[j].srcPid,
                getMediaTypeString(pids[j].mediaType));
    }
#endif

    // Parameter checking
    if ((NULL != tsb) && (NULL != rec_path) && (NULL != rec_name) && (NULL
            != pids) && (0 != pid_count) && (0 != inout_starttime) && (NULL
            != callback))
    {
        RILOG_INFO("%s -- Conversion file to be written to: %s, start time %"PRIu64" nS, duration %d \n",
                __FUNCTION__,
                rec_path,
                *inout_starttime,
                duration);

        // get the relevant TsbItem
        tsb_item_t * pTsbItem = getTsbEntry(tsb);
        if (NULL != pTsbItem)
        {
            g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));

            // Check a conversion is not already in progress
            if (FALSE == pTsbItem->pTsb->conversion_in_progress)
            {
                // no conversion in progress, so open a conversion file handle
                ifsReturnCode = IfsOpenWriter(rec_path, // const char * path      Input
                        NULL, // const char * name      Input  (if NULL the name is generated)
                        0, // IfsTime maxSize        Input  (in seconds, 0 = no max)
                        &pTsbItem->pTsb->ifsConvHandle); // IfsHandle * pIfsHandle Output (use IfsClose() to free)

                // get conversion file name
                if (IfsReturnCodeNoErrorReported == ifsReturnCode)
                {
                    // get the complete filename
                    ifsReturnCode = IfsHandleInfo(
                            pTsbItem->pTsb->ifsConvHandle, &pIfsInfo);

                    if (IfsReturnCodeNoErrorReported == ifsReturnCode)
                    {
                        RILOG_INFO(
                                "%s -- Conversion file (name = %s, path = %s) opened ok\n",
                                __FUNCTION__, pIfsInfo->name, pIfsInfo->path);

                        strncpy(rec_name, pIfsInfo->name,
                                RI_MAX_RECORDING_NAME_LENGTH);
                        (void) IfsFreeInfo(pIfsInfo);

                        //
                        // calculate the requested start and end times
                        //
                        ifsReturnCode =
                            IfsHandleInfo(pTsbItem->pTsb->ifsHandleTsb, &pIfsInfo);
                        
                        if (IfsReturnCodeNoErrorReported == ifsReturnCode)
                        {
                            // Starttime in nS from original TSB creation time
                            //*inout_starttime += pIfsInfo->begClock;
                            pTsbItem->pTsb->requested_convert_start_time_ns =
                                *inout_starttime + pIfsInfo->begClock;
                            RILOG_INFO("%s updated EPOCH start time = %"
                                       PRIu64"\n", __func__, *inout_starttime);
                            (void) IfsFreeInfo(pIfsInfo);
                        }
                        else
                        {
                            RILOG_FATAL(-1, "%s -- Could not open TSB file handle\n", __FUNCTION__);
                        }

                        // Requested recording duration in seconds (0 = unlimited)
                        pTsbItem->pTsb->requested_convert_duration_s = duration;

                        // Make note of when we should end
                        pTsbItem->pTsb->expected_convert_end_time_ns = 0;
                        if (0 != pTsbItem->pTsb->requested_convert_duration_s)
                        {
                            pTsbItem->pTsb->expected_convert_end_time_ns
                                    = pTsbItem->pTsb->requested_convert_start_time_ns
                                            + ((uint64_t) pTsbItem->pTsb->requested_convert_duration_s
                                                    * CONVERT_SEC_TO_NS);
                        }

                        pTsbItem->pTsb->convert_callback = callback;
                        pTsbItem->pTsb->convert_cb_data = cb_data;

                        // indicate that a conversion is in progress
                        pTsbItem->pTsb->conversion_in_progress = TRUE;

                        // if the TSB is associated with a pipeline...
                        if (NULL != pTsbItem->pPipeline)
                        {
                            RILOG_DEBUG(
                                    "%s -- TSB associated w/ pipeline, performing conversion asynchronously\n",
                                    __FUNCTION__);

                            g_static_rec_mutex_unlock(
                                    &(pTsbItem->pTsb->tsb_mutex));
                            // Tell the thread to start conversion
                            pCommand = g_try_malloc(sizeof(tsb_thread_command));
                            if (pCommand)
                            {
                                pCommand->action = TSB_ACTION_CONVERT;
                                g_async_queue_push(pTsbItem->pTsb->tsb_queue,
                                        pCommand);

                                // indicate success
                                rc = RI_ERROR_NONE;
                            }
                            g_static_rec_mutex_lock(
                                    &(pTsbItem->pTsb->tsb_mutex));
                        }
                        else
                        {
                            // the TSB is not associated with an active pipeline, so perform conversion in the
                            //  context of this thread
                            RILOG_DEBUG(
                                    "%s -- TSB not associated w/ pipeline, performing conversion synchronously\n",
                                    __FUNCTION__);

                            doConversionState tsbConversionState =
                                    (doConversionState) tsbConversionStateIdle;
                            g_static_rec_mutex_lock(
                                    &(pTsbItem->pTsb->tsb_mutex));
                            {
                                for (tsbConversionState
                                        = (doConversionState) tsbConversionStateStarting; tsbConversionState
                                        != (doConversionState) tsbConversionStateIdle;)
                                {
                                    tsbConversionState = tsbConversionState(
                                            pTsbItem);
                                }
                            }

                            rc = RI_ERROR_NONE;
                        } /* endif a TSB exists */
                    }
                    else
                    {
                        RILOG_ERROR(
                                "%s -- Failed to start TSB conversion, unable to get conversion filename \n",
                                __FUNCTION__);
                    }
                }
                else
                {
                    RILOG_ERROR(
                            "%s -- Error opening handle on conversion file: %s\n",
                            __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
                }
            }
            else
            {
                RILOG_WARN("%s -- TSB conversion already in progress\n",
                        __FUNCTION__);
                rc = RI_ERROR_ALREADY_EXISTS;
            } /* endif conversion not already in progress */

            g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
        }
        else
        {
            // no valid TSB, so say so
            RILOG_ERROR("%s -- No valid TSB\n", __FUNCTION__);
            rc = RI_ERROR_NO_TSB;
        }
    }
    else
    {
        RILOG_ERROR("%s -- Bad input arguments\n", __FUNCTION__);
        rc = RI_ERROR_ILLEGAL_ARG;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
} /* tsb_convert */

/**
 * Terminates any existing TSB recording conversion currently taking place.
 *
 * @param tsb The TSB
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 *    RI_ERROR_NO_TSB: No TSB has been started on this pipeline
 *    RI_ERROR_NO_CONVERSION: No TSB-conversion has been initiated on this TSB
 */
ri_error tsb_convert_stop(ri_tsbHandle tsb)
{
    if (NULL == tsbCategory)
    {
        tsbCategory = log4c_category_get("RI.Pipeline.TSB");
    }

    RILOG_TRACE("%s -- Entry, tsb = %p\n", __FUNCTION__, tsb);

    ri_error rc = RI_ERROR_NONE; // assume success
    tsb_thread_command *command; // Used to tell thread to stop conversion

    // Parameter checking
    if (NULL != tsb)
    {
        tsb_item_t* pTsbItem = getTsbEntry(tsb);

        // check for valid input parameters
        if (NULL != pTsbItem)
        {
            // Lock the mutex before attempting to manipulate the TSB object
            g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
            {
                // if the TSB is associated with a pipeline...
                if (NULL != pTsbItem->pPipeline)
                {
                    // ...Check a conversion is in progress
                    if (TRUE == pTsbItem->pTsb->conversion_in_progress)
                    {
                        // Tell the thread to stop conversion
                        command = g_try_malloc(sizeof(tsb_thread_command));
                        if (command)
                        {
                            command->action = TSB_ACTION_CONVERT_STOP;
                            g_async_queue_push(pTsbItem->pTsb->tsb_queue,
                                    command);
                        }
                        else
                        {
                            RILOG_ERROR("%s -- Failed to stop TSB conversion",
                                    __FUNCTION__);
                            rc = RI_ERROR_GENERAL;
                        } /* endif allocation of thread command OK*/
                    }
                    else
                    {
                        rc = RI_ERROR_NO_CONVERSION;
                    } /* endif conversion not already in progress */
                }
                else
                {
                    rc = RI_ERROR_NO_TSB;
                } /* endif a TSB in progress */
            }
            g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
        }
        else
        {
            // no valid TSB, so say so
            RILOG_ERROR("%s -- No valid TSB\n", __FUNCTION__);
            rc = RI_ERROR_NO_TSB;
        }
    }
    else
    {
        RILOG_ERROR("%s -- No TSB specified\n", __FUNCTION__);
        rc = RI_ERROR_ILLEGAL_ARG;
    }
    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;

} /* tsb_convert_stop */

/**
 * Begins playing back media from this pipeline's TSB.
 *
 * @param tsb The buffer that should be used for playback
 * @param video_device platform decode display device to use for play back
 * @param pids An array of pids that should be played back (recPids only)
 * @param pid_count The number of pids in the array
 * @param position The requested time (in nanoseconds), from the original
 *                 TSB start time, at which the playback should begin
 * @param rate The desired playback rate
 * @param callback A callback function that will receive all events related to
 *                 the playback from this TSB
 * @param cb_data User data that will be passed in every callback invocation
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 */
ri_error tsb_playback_start(ri_tsbHandle tsb, ri_video_device_t* video_device,
        ri_pid_info_t* pids, uint32_t pid_count, uint64_t position, float rate,
        ri_dvr_callback_f callback, void* cb_data)
{
    if (NULL == tsbCategory)
    {
        tsbCategory = log4c_category_get("RI.Pipeline.TSB");
    }

    RILOG_TRACE("%s -- Entry, tsb = %p position %"PRIu64" nS, rate = %f \n",
            __FUNCTION__,
            tsb, position, rate);

    ri_error rc = RI_ERROR_NONE;

#ifdef TEMP_DEBUG_AID
    RILOG_INFO("%s -- Dump of PIDs:\n", __FUNCTION__);
    uint32_t j;
    for (j = 0; j < pid_count; j++)
    {
        RILOG_INFO("\t0x%4.4X, type is %s\n", pids[j].recPid,
                getMediaTypeString(pids[j].mediaType));
    }
#endif

    // Parameter checking
    if ((NULL != tsb) && (NULL != video_device) && (NULL != pids) && (0
            < pid_count) && (pid_count <= MAX_PIDS_PER_ACTION))
    {
        // cast the tsb parameter so that it is more sensical
        IfsInfo* pIfsHandleInfo = (IfsInfo*) tsb;

        // start playback from the TSB
        rc = recording_playback_start(video_device, pIfsHandleInfo->path,
                pIfsHandleInfo->name, position, rate, pids, pid_count,
                callback, cb_data);
    }
    else
    {
        rc = RI_ERROR_ILLEGAL_ARG;
    } /* endif params are OK */

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
} /* tsb_playback_start */

//****************************************************************************
//****************************************************************************
// Begin TSB operational states
// The following states are used to implement a state machine that is executed
//  by the tsb thread.
//****************************************************************************
//****************************************************************************

// forward state declarations
static doTsbState tsbStateIdle(tsb_item_t* pTsbItem);
static doTsbState tsbStateInitFilesink(tsb_item_t* pTsbItem);
static doTsbState tsbStateStarting(tsb_item_t* pTsbItem);
static doTsbState tsbStateStarted(tsb_item_t* pTsbItem);
static doTsbState tsbStateActive(tsb_item_t* pTsbItem);
static doTsbState tsbStateStopping(tsb_item_t* pTsbItem);
static doTsbState tsbStateStopped(tsb_item_t* pTsbItem);
static doTsbState tsbStateTerminating(tsb_item_t* pTsbItem);

/**
 * This is the state that the TSB state machine is in when it is not
 * actively doing anything else.
 *
 * Next state = <code>tsbStateIdle</code>
 */
static doTsbState tsbStateIdle(tsb_item_t* pTsbItem)
{
    RILOG_DEBUG("%s -- Conversion state = TSB_STATE_IDLE\n", __FUNCTION__);

    return (doTsbState) tsbStateIdle;
}

/**
 * Initializes the indexing file sink element for operation.
 * This state waits for the indexing file sink to transition to the NULL
 * state, as commanded by the TSB_ACTION_START action, then does the following:
 * 1. Set the audio and video pids to be indexed
 * 2. Set the path and name of the TSB file
 * 3. Makes the element operational by exiting the <code>bitBucket</code> mode.
 *
 * Next state = <code>tsbStateStarting</code>
 */
static doTsbState tsbStateInitFilesink(tsb_item_t* pTsbItem)
{
    tsb_thread_command *pCommand = NULL;
    doTsbState retVal = (doTsbState) tsbStateInitFilesink;
 
    GstState state =
            GST_STATE((GstElement*) pTsbItem->pPipeline->data->tsbsink);
    RILOG_DEBUG(
            "%s -- Current state = TSB_STATE_INIT_FILESINK, tsbsink state = %s\n",
            __FUNCTION__, gst_element_state_get_name(state));

    pTsbItem->pTsb->tsb_status = (ri_tsb_status_t)
    {
        0, 0, 0
    };

    // set operational parameters on the indexing file sink
    // use fixed (normalized) pid numbers here
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "videoPid",
            VIDEO_DECODE_PID, NULL);
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "audioPid",
            AUDIO_DECODE_PID, NULL);

    // set the root filesystem path
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "filePath",
            pTsbItem->pTsb->pIfsInfo->path, NULL);

    // set the file name
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "fileName",
            pTsbItem->pTsb->pIfsInfo->name, NULL);

    // set the max size
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "maxSize",
            pTsbItem->pTsb->pIfsInfo->maxSize, NULL);

    // no longer a bit bucket
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "bitBucket",
            FALSE, NULL);

    // place pipeline into the playing state - this will cause the tsbsink
    //  element to transition into the playing state, which will cause it to
    //  process data in the pipeline

    state = GST_STATE((GstElement*) pTsbItem->pPipeline->data->gst_pipeline);
    RILOG_DEBUG(
            "%s -- Current state = TSB_STATE_INIT_FILESINK, gst_pipeline state = %s\n",
            __FUNCTION__, gst_element_state_get_name(state));

    // transition TSB state machine to STARTING state
    retVal = (doTsbState) tsbStateStarting;

    //Getting thread attention immediatley without delay during state transfer
    pCommand = malloc(sizeof(tsb_thread_command));
    if (pCommand)
    {
       pCommand->action = TSB_ACTION_WAKE_THREAD_ONLY;
       g_async_queue_push(pTsbItem->pTsb->tsb_queue, pCommand);
    }    

    return retVal;
}

/**
 * Transitions the TSB to active use.
 * This state does the following:
 * 1. Waits until the file sink element has transitioned to the
 * <code>playing</code> state.
 * 2. Opens a read handle on the TSB that is created by the tsbsink
 * gstreamer element as a result of its transition to the 'ready' state (note
 * that the 'ready' state is commanded by the corresponding TSB_ACTION_START
 * stimulus).
 * 3. Transitions the tsbsink gstreamer element to the 'playing' state
 *
 * Next state = <code>tsbStateStarted</code>
 * On error, next state = <code>tsbStateIdle</code>
 */
static doTsbState tsbStateStarting(tsb_item_t* pTsbItem)
{   
    tsb_thread_command *pCommand = NULL;  
    doTsbState retVal = (doTsbState) tsbStateStarting;

    GstState state =
            GST_STATE((GstElement*) pTsbItem->pPipeline->data->tsbsink);
    RILOG_DEBUG(
            "%s -- Current state = TSB_STATE_STARTING, tsbsink state = %s\n",
            __FUNCTION__, gst_element_state_get_name(state));

    if (GST_STATE_PLAYING == state)
    {
        g_object_set(G_OBJECT(pTsbItem->pPipeline->data->recordswitch),
                "pidlist", ALL_PIDS, NULL);

        // obtain a read handle on the TSB for monitoring progress
        IfsReturnCode ifsReturnCode;
        ifsReturnCode = IfsOpenReader(pTsbItem->pTsb->pIfsInfo->path, // const char * path      Input
                pTsbItem->pTsb->pIfsInfo->name, // const char * name      Input
                &pTsbItem->pTsb->ifsHandleTsb); // IfsHandle * pIfsHandle Output (use IfsClose() to free)

        if (IfsReturnCodeNoErrorReported == ifsReturnCode)
        {
            RILOG_INFO(
                    "%s -- Current state = TSB_STATE_STARTING, buffering to: %s/%s\n",
                    __FUNCTION__, pTsbItem->pTsb->pIfsInfo->path,
                    pTsbItem->pTsb->pIfsInfo->name);

            // transition to the started state
            retVal = (doTsbState) tsbStateStarted;

            //Getting thread attention immediatley without delay during state transfer
            pCommand = malloc(sizeof(tsb_thread_command));
            if (pCommand)
            {
                pCommand->action = TSB_ACTION_WAKE_THREAD_ONLY;
                g_async_queue_push(pTsbItem->pTsb->tsb_queue, pCommand);
            }       
        }
        else
        {
            RILOG_ERROR("%s -- Error opening handle on TSB file: %s\n",
                    __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));

            // on error, go to the idle state
            retVal = (doTsbState) tsbStateIdle;
        }
    }

    return retVal;
}

/**
 * Notifies upper layers that the TSB has started.
 * This state waits until the TSB reports that it has a non-zero
 * <code>beginClock</code>, then reports that value to upper layers via
 * a callback function.
 *
 * Next state = <code>tsbStateActive</code>
 * On error, next state = <code>tsbStateIdle</code>
 */
static doTsbState tsbStateStarted(tsb_item_t* pTsbItem)
{   
    tsb_thread_command *pCommand = NULL;   
    doTsbState retVal = (doTsbState) tsbStateStarted; //
    GstState state =
            GST_STATE((GstElement*) pTsbItem->pPipeline->data->tsbsink);
    RILOG_INFO("%s -- Current state = TSB_STATE_STARTED, tsbsink state = %s\n",
            __FUNCTION__, gst_element_state_get_name(state));

    pTsbItem->pTsb->tsb_status.start_time = 0;
    IfsInfo* pIfsHandleInfo;
    IfsReturnCode ifsReturnCode;

    // get the handle info structure for access to the path and file name
    ifsReturnCode
            = IfsHandleInfo(pTsbItem->pTsb->ifsHandleTsb, &pIfsHandleInfo);
    if (IfsReturnCodeNoErrorReported == ifsReturnCode)
    {
        char temp_start_time[32] = { '\0' };

        // now get the start time of the TSB (in ns)
        pTsbItem->pTsb->tsb_status.start_time = pIfsHandleInfo->begClock;
        RILOG_INFO("%s -- %p tsb_status.start_time = %s\n",
                    __FUNCTION__, pTsbItem->pTsb->ifsHandleTsb,
                    IfsLongLongToString(pTsbItem->pTsb->tsb_status.start_time,
                                        temp_start_time));

        // handle info no longer needed, so free its memory
        (void) IfsFreeInfo(pIfsHandleInfo);
    }
    else
    {
        RILOG_ERROR("%s -- Error obtaining handle info for file: %s\n",
                __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));

        // on error, go to the idle state
        retVal = (doTsbState) tsbStateIdle;
    }

    // report the current pipeline state
    state = GST_STATE((GstElement*) pTsbItem->pPipeline->data->gst_pipeline);
    RILOG_DEBUG("%s -- Pipeline state state = %s\n", __FUNCTION__,
            gst_element_state_get_name(state));

    // a non-zero start time implies that the TSB has started its operation so...
    if (0 != pTsbItem->pTsb->tsb_status.start_time)
    {
        // ...let the callback know of the start time (world time in msec)
        if (NULL != pTsbItem->pTsb->callback)
        {
#ifndef PRODUCTION_BUILD
            char temp[32] =
            { '\0' };
            RILOG_INFO(
                    "%s -- Sending RI_DVR_EVENT_TSB_START with time %s mS\n",
                    __FUNCTION__, IfsLongLongToString(
                            pTsbItem->pTsb->tsb_status.start_time, temp));
#endif

            g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));

            pTsbItem->pTsb->callback(RI_DVR_EVENT_TSB_START,
                    &pTsbItem->pTsb->tsb_status.start_time,
                    pTsbItem->pTsb->cb_data);

            g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
        }
        // transition to the active state
        retVal = (doTsbState) tsbStateActive;

        //Getting thread attention immediatley without delay during state transfer
        pCommand = malloc(sizeof(tsb_thread_command));
        if (pCommand)
        {
            pCommand->action = TSB_ACTION_WAKE_THREAD_ONLY;
            g_async_queue_push(pTsbItem->pTsb->tsb_queue, pCommand);
        }        
    }
    else
    {
        RILOG_INFO("%s -- Waiting for TSB to start operation\n", __FUNCTION__);
    }

    return retVal;
}

/**
 * Notifies upper layers of TSB progress.
 * This is a 'steady' state, that queries the IFS system for its current
 * <code>begClock</code> and <code>endClock</code> values and reports them
 * to upper layers via a callback function.
 *
 * Next state = <code>tsbStateActive</code> (the current state)
 * On error, next state = <code>tsbStateIdle</code>
 */
static doTsbState tsbStateActive(tsb_item_t* pTsbItem)
{
    doTsbState retVal = (doTsbState) tsbStateActive;
    RILOG_DEBUG("%s -- Current state = TSB_STATE_ACTIVE\n", __FUNCTION__);

    // let the callback know the current status
    if (NULL != pTsbItem->pTsb->callback)
    {
        IfsInfo* pIfsHandleInfo;
        IfsReturnCode ifsReturnCode;

        // open a handle on the existing TSB session
        ifsReturnCode = IfsHandleInfo(pTsbItem->pTsb->ifsHandleTsb,
                &pIfsHandleInfo);
        if (IfsReturnCodeNoErrorReported == ifsReturnCode)
        {
            // update the tsb status (in ns)

            if (pIfsHandleInfo->endClock - pIfsHandleInfo->begClock >
                pTsbItem->pTsb->tsb_maximum_duration_ns)
            {
                pTsbItem->pTsb->tsb_status.start_time =
                    pIfsHandleInfo->endClock -
                    pTsbItem->pTsb->tsb_maximum_duration_ns;
            }
            else
            {
                pTsbItem->pTsb->tsb_status.start_time = pIfsHandleInfo->begClock;
            }

            pTsbItem->pTsb->tsb_status.end_time = pIfsHandleInfo->endClock;
            pTsbItem->pTsb->tsb_status.size = pIfsHandleInfo->mpegSize
                    + pIfsHandleInfo->ndexSize; // May want one or both...

            (void) IfsFreeInfo(pIfsHandleInfo);

#ifndef PRODUCTION_BUILD
            char temp_start_time[32] =
            { '\0' };
            char temp_end_time[32] =
            { '\0' };
            RILOG_DEBUG(
                    "%s -- Sending RI_DVR_EVENT_TSB_STATUS with start %s nS, end %s nS\n",
                    __FUNCTION__, IfsLongLongToString(
                            pTsbItem->pTsb->tsb_status.start_time,
                            temp_start_time), IfsLongLongToString(
                            pTsbItem->pTsb->tsb_status.end_time, temp_end_time));
#endif

            g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
            {
                pTsbItem->pTsb->callback(RI_DVR_EVENT_TSB_STATUS,
                        &pTsbItem->pTsb->tsb_status, pTsbItem->pTsb->cb_data);
            }
            g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
        }
        else
        {
            RILOG_ERROR("%s -- IfsHandleInfo returned error: %s\n", __FUNCTION__,
                    IfsReturnCodeToString(ifsReturnCode));

            // on error, go to the idle state
            retVal = (doTsbState) tsbStateIdle;
        }
    }
    else
    {
        RILOG_ERROR("%s -- gst_element_query_position error\n", __FUNCTION__);

        // on error, go to the idle state
        retVal = (doTsbState) tsbStateIdle;
    }

    return retVal;
}

/**
 * Notifies upper layers that the TSB is no longer processing data.
 * This state does the following:
 * 1. Notifies upper layers that the TSB is no longer processing
 * data by sending RI_DVR_EVENT_TSB_STOPPED to a callback function.
 *
 * Next state = <code>tsbStateStopped</code>
 * On error, next state = <code>tsbStateIdle</code>
 */
static doTsbState tsbStateStopping(tsb_item_t* pTsbItem)
{
    doTsbState retVal = (doTsbState) tsbStateStopping;

#ifndef PRODUCTION_BUILD
    GstState state =
            GST_STATE((GstElement*) pTsbItem->pPipeline->data->tsbsink);
    RILOG_DEBUG(
            "%s -- Current state = TSB_STATE_STOPPING, tsbsink state = %s\n",
            __FUNCTION__, gst_element_state_get_name(state));
#endif

    // Call the callback
    if (NULL != pTsbItem->pTsb->callback)
    {
        IfsInfo* pIfsHandleInfo;
        IfsReturnCode ifsReturnCode;

        // open a handle on the existing TSB session
        ifsReturnCode = IfsHandleInfo(pTsbItem->pTsb->ifsHandleTsb,
                &pIfsHandleInfo);
        if (IfsReturnCodeNoErrorReported == ifsReturnCode)
        {
            // update the tsb status
            pTsbItem->pTsb->tsb_status.start_time = pIfsHandleInfo->begClock;
            pTsbItem->pTsb->tsb_status.end_time = pIfsHandleInfo->endClock;
            pTsbItem->pTsb->tsb_status.size = pIfsHandleInfo->mpegSize
                    + pIfsHandleInfo->ndexSize; // May want one or both...

            (void) IfsFreeInfo(pIfsHandleInfo);

#ifndef PRODUCTION_BUILD
            char temp_start_time[32] =
            { '\0' };
            char temp_end_time[32] =
            { '\0' };
            char temp_size[32] =
            { '\0' };
            RILOG_INFO(
                    "%s -- Sending RI_DVR_EVENT_TSB_STOPPED with start %s nS, end %s nS, size %s bytes\n",
                    __FUNCTION__, IfsLongLongToString(
                            pTsbItem->pTsb->tsb_status.start_time,
                            temp_start_time),
                    IfsLongLongToString(pTsbItem->pTsb->tsb_status.end_time,
                            temp_end_time), IfsLongLongToString(
                            pTsbItem->pTsb->tsb_status.size, temp_size));
#endif

            g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));

            pTsbItem->pTsb->callback(RI_DVR_EVENT_TSB_STOPPED,
                    &pTsbItem->pTsb->tsb_status, pTsbItem->pTsb->cb_data);
            g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));

            // the TSB handle is no longer needed, so close it
            ifsReturnCode = IfsClose(pTsbItem->pTsb->ifsHandleTsb);
            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                pTsbItem->pTsb->ifsHandleTsb = NULL;
                retVal = (doTsbState) tsbStateStopped;
            }
            else
            {
                RILOG_ERROR("%s -- Error closing TSB handle: %s\n",
                        __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));

                // on error, go to the idle state
                retVal = (doTsbState) tsbStateIdle;
            }
        }
        else
        {
            RILOG_ERROR("%s -- IfsHandleInfo returned error: %s", __FUNCTION__,
                    IfsReturnCodeToString(ifsReturnCode));

            // on error, go to the idle state
            retVal = (doTsbState) tsbStateIdle;
        }
    }

    // set the tsbsink state to null in preparation for setting its
    //    properties in the 'stopped' state
    (void) g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink),
            "bitBucket", TRUE, NULL);

    return retVal;
}

/**
 * Uninitializes the indexing file sink from operation.
 * This state does the following:
 * 1. Waits until the tsb sink transitions to the 'null' state (note that
 * the 'null' state was requested by the TSB_ACTION_STOP stimulus).
 * 2. Sets the <code>bitBucket</code> property to 'true'.
 * 3. Clears the path and name of the TSB file
 * 4. Setting the <code>pidList</code> property of the recordswitch element
 *  to pass no PIDs.
 * 5. Sets the inputselect element to play live content
 *
 * Next state = <code>tsbStateTerminating</code>
 * On error, next state = <code>tsbStateIdle</code>
 */
static doTsbState tsbStateStopped(tsb_item_t* pTsbItem)
{
    doTsbState retVal = (doTsbState) tsbStateStopped;

#ifndef PRODUCTION_BUILD
    GstState state =
            GST_STATE((GstElement*) pTsbItem->pPipeline->data->tsbsink);

    RILOG_DEBUG(
            "%s -- Current state = TSB_STATE_STOPPED, tsbsink state = %s\n",
            __FUNCTION__, gst_element_state_get_name(state));
#endif

    // make the file sink a bit bucket so that the playing state does not result in writing to a file
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "bitBucket",
            TRUE, NULL);

    // set the root filesystem path to ""
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "filePath", "",
            NULL);
    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "fileName", "",
            NULL);

    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->tsbsink), "maxSize", 0,
            NULL);

    g_object_set(G_OBJECT(pTsbItem->pPipeline->data->recordswitch), "pidlist",
            NO_PIDS, NULL);

    // element will be placed back into the playing state at the end of of the tsb_stop() routine

    // transition to terminating state
    retVal = (doTsbState) tsbStateTerminating;

    return retVal;
}

/**
 * Terminates a TSB session.
 * This state triggers the termination of the TSB thread.
 *
 * Next state:
 *  tsbStateTerminating
 */
static doTsbState tsbStateTerminating(tsb_item_t* pTsbItem)
{
    RILOG_DEBUG("%s -- Current state = TSB_STATE_TERMINATING\n", __FUNCTION__);

    return (doTsbState) tsbStateTerminating;
}

//****************************************************************************
//****************************************************************************
// Begin conversion operational states
// The following states are used to implement a state machine that is executed
//  by the tsb thread.  The state machine will convert some portion of the TSB
//  into a persistent recording.
//****************************************************************************
//****************************************************************************

/**
 * This is the state that the TSB conversion state machine is in when it is not
 * actively doing anything else.
 */
static doConversionState tsbConversionStateIdle(tsb_item_t* pTsbItem)
{
    RILOG_DEBUG("%s -- Conversion state = TSB_STATE_CONVERSION_IDLE\n",
            __FUNCTION__);

    return (doConversionState) tsbConversionStateIdle;
}

/**
 * A transitional state that starts the conversion process.
 * This state does the following:
 * 1. Waits until the TSB has data to convert.
 * 2. Converts the first portion of the recording.
 * 3. Notifies upper layers that conversion has begun by sending a
 * RI_DVR_EVENT_TSB_CONVERSION_STATUS message to a callback function
 *
 * Next state:
 *  tsbConversionStateActive
 * On error:
 *
 */
static doConversionState tsbConversionStateStarting(tsb_item_t* pTsbItem)
{
    doConversionState retVal = (doConversionState) tsbConversionStateStarting;

    // Check to see if there is any data to convert
    // if so, convert the first chunk
    RILOG_DEBUG("%s -- Conversion state = TSB_STATE_CONVERSION_STARTING\n",
            __FUNCTION__);

    // obtain a read handle on the TSB
    IfsInfo* pIfsHandleInfo;
    IfsReturnCode ifsReturnCode;
    IfsClock begClock;
    IfsClock endClock;

    ifsReturnCode
            = IfsHandleInfo(pTsbItem->pTsb->ifsHandleTsb, &pIfsHandleInfo);

    if (IfsReturnCodeNoErrorReported == ifsReturnCode)
    {
        // check that there is data to convert...
        if ((0 != pIfsHandleInfo->begClock) && (pIfsHandleInfo->endClock >=
            pTsbItem->pTsb->requested_convert_start_time_ns))
        {
            //... there is, so convert the first chunk
            if (0 == pTsbItem->pTsb->requested_convert_start_time_ns)
            {
                pTsbItem->pTsb->requested_convert_start_time_ns
                        = pIfsHandleInfo->begClock;
                pTsbItem->pTsb->expected_convert_end_time_ns
                        = pTsbItem->pTsb->requested_convert_start_time_ns
                                + ((uint64_t) pTsbItem->pTsb->requested_convert_duration_s
                                        * CONVERT_SEC_TO_NS);

                RILOG_INFO("%s -- TSB_CONVERSION_STARTING, initializing start time (ns) = %"PRIu64", end time (ns) = %"PRIu64"\n",
                    __FUNCTION__,
                    pTsbItem->pTsb->requested_convert_start_time_ns,
                    pTsbItem->pTsb->expected_convert_end_time_ns);
            }

            // start conversion
            begClock = pTsbItem->pTsb->requested_convert_start_time_ns;
            endClock = pTsbItem->pTsb->expected_convert_end_time_ns;

            ifsReturnCode = IfsConvert(pTsbItem->pTsb->ifsHandleTsb, // IfsHandle  srcHandle Input
                    pTsbItem->pTsb->ifsConvHandle, // IfsHandle  dstHandle Input (must be a writer)
                    &begClock, // IfsClock * pBegClock Input requested/Output actual, in nanoseconds
                    &endClock); // IfsClock * pEndClock Input requested/Output actual, in nanoseconds
            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                RILOG_DEBUG("%s -- IfsConvert returned success\n", __FUNCTION__);

                // report conversion status end time relative to recording start time
                pTsbItem->pTsb->conversion_status.start_time = begClock;
                pTsbItem->pTsb->conversion_status.end_time = endClock;

                IfsInfo* ifsHandleInfo;
                (void) IfsHandleInfo(pTsbItem->pTsb->ifsConvHandle, &ifsHandleInfo);
                pTsbItem->pTsb->conversion_status.size = 
                        ifsHandleInfo->mpegSize + ifsHandleInfo->ndexSize;
                (void)IfsFreeInfo(ifsHandleInfo);

                g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
                pTsbItem->pTsb->convert_callback(RI_DVR_EVENT_TSB_CONVERSION_STATUS,
                        &pTsbItem->pTsb->conversion_status,
                        pTsbItem->pTsb->convert_cb_data);
                g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));

                // transition to conversion active state
                retVal = (doConversionState)tsbConversionStateActive;
            }
            else
            {
                RILOG_ERROR("%s -- IfsConvert error: %s\n",
                        __FUNCTION__,
                        IfsReturnCodeToString(ifsReturnCode));
                retVal = (doConversionState)tsbConversionStateIdle;
            }
        }
        else
        {
            RILOG_DEBUG(
            "%s Nothing to convert yet (bc:%"PRIu64" ec:%"PRIu64" cst:%"PRIu64")\n",
                   __FUNCTION__, pIfsHandleInfo->begClock, pIfsHandleInfo->endClock,
                   pTsbItem->pTsb->requested_convert_start_time_ns);
        }
        (void)IfsFreeInfo(pIfsHandleInfo);
    }
    else
    {
        RILOG_ERROR("%s -- IfsHandleInfo error: %s\n",
                __FUNCTION__,
                IfsReturnCodeToString(ifsReturnCode));

        retVal = (doConversionState)tsbConversionStateIdle;
    }

    return retVal;
}

/**
 * Monitors the conversion, notifying upper layers of the progress.
 * This state does the following:
 * 1. Checks that there is more data to convert.
 * 2. Appends the next chunk of data to the conversion file.
 * 3. Notifies upper layers that conversion has begun by sending a
 * RI_DVR_EVENT_TSB_CONVERSION_STATUS message to a callback function
 *
 * Next state:
 *  tsbConversionStateActive
 *
 * On error:
 *  tsbConversionStateIdle
 */
static doConversionState tsbConversionStateActive(tsb_item_t* pTsbItem)
{
    RILOG_DEBUG("%s -- Conversion state = TSB_STATE_CONVERSION_ACTIVE\n",
            __FUNCTION__);

    doConversionState retVal = (doConversionState) tsbConversionStateActive;

    // obtain a read handle on the TSB
    IfsInfo* pIfsHandleInfo;
    IfsReturnCode ifsReturnCode;
    IfsClock endClock;

    ifsReturnCode
            = IfsHandleInfo(pTsbItem->pTsb->ifsHandleTsb, &pIfsHandleInfo);
    if (IfsReturnCodeNoErrorReported == ifsReturnCode)
    {
        //... there is, so convert the next chunk
        endClock = pTsbItem->pTsb->expected_convert_end_time_ns;

        // continue conversion
        ifsReturnCode = IfsAppend(pTsbItem->pTsb->ifsHandleTsb, // IfsHandle  srcHandle Input
                pTsbItem->pTsb->ifsConvHandle, // IfsHandle  dstHandle Input (must be a writer)
                &endClock); // IfsClock * pEndClock Input requested/Output actual, in nanoseconds
        if (IfsReturnCodeNoErrorReported == ifsReturnCode)
        {
#ifndef PRODUCTION_BUILD
            char temp[32] =
            { '\0' };
            RILOG_DEBUG("%s -- IfsAppend returned success, now at: %s \n",
                    __FUNCTION__, IfsToSecs(endClock, temp));
#endif

            // report conversion status end time relative to recording start time
            pTsbItem->pTsb->conversion_status.end_time = endClock;

            IfsInfo* ifsHandleInfo;
            (void) IfsHandleInfo(pTsbItem->pTsb->ifsConvHandle, &ifsHandleInfo);
            pTsbItem->pTsb->conversion_status.size = 
                    ifsHandleInfo->mpegSize + ifsHandleInfo->ndexSize;
            (void)IfsFreeInfo(ifsHandleInfo);

            g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
            pTsbItem->pTsb->convert_callback(
                    RI_DVR_EVENT_TSB_CONVERSION_STATUS,
                    &pTsbItem->pTsb->conversion_status,
                    pTsbItem->pTsb->convert_cb_data);
            g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
        }
        else
        {
            RILOG_ERROR("%s -- IfsAppend error: %s\n", __FUNCTION__,
                    IfsReturnCodeToString(ifsReturnCode));

            retVal = (doConversionState) tsbConversionStateIdle;
        }

        (void) IfsFreeInfo(pIfsHandleInfo);
    }
    else
    {
        RILOG_ERROR("%s -- IfsHandleInfo error: %s\n", __FUNCTION__,
                IfsReturnCodeToString(ifsReturnCode));

        retVal = (doConversionState) tsbConversionStateIdle;
    }

    return retVal;
}

/**
 * Transition state for stopping the conversion.
 * This state does the following:
 * 1. Gets the size of the conversion in bytes.
 * 2. Closes the conversion file handle.
 * 3. Notifies upper layers that conversion has begun by sending a
 * RI_DVR_EVENT_TSB_CONVERSION_COMPLETE message to a callback function
 *
 * Next state:
 *  tsbConversionStateStopped
 */
static doConversionState tsbConversionStateStopping(tsb_item_t* pTsbItem)
{
    RILOG_INFO("%s -- Conversion state = TSB_STATE_CONVERSION_STOPPING\n",
            __FUNCTION__);

    pTsbItem->pTsb->conversion_in_progress = FALSE;
    ri_conversion_results_t conversion_complete; // Used for status reporting

    IfsInfo* pIfsHandleInfo;
    IfsReturnCode ifsReturnCode;

    // get the size of the conversion file in bytes
    ifsReturnCode = IfsHandleInfo(pTsbItem->pTsb->ifsConvHandle,
            &pIfsHandleInfo);
    if (IfsReturnCodeNoErrorReported == ifsReturnCode)
    {
        conversion_complete.size = pIfsHandleInfo->mpegSize
                + pIfsHandleInfo->ndexSize; // May want one or both...
        (void) IfsFreeInfo(pIfsHandleInfo);
    }
    else
    {
        conversion_complete.size = 0;
        RILOG_ERROR("%s -- Error reading conversion file size: %s\n",
                __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
    }

    // close conversion file handle
    ifsReturnCode = IfsClose(pTsbItem->pTsb->ifsConvHandle);
    if (IfsReturnCodeNoErrorReported == ifsReturnCode)
    {
        pTsbItem->pTsb->ifsConvHandle = NULL;
    }
    else
    {
        RILOG_ERROR("%s -- Error closing conversion file handle: %s\n",
                __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
    }

    // if there is a callback function...
    if (NULL != pTsbItem->pTsb->callback)
    {
        // ...report actual conversion duration in seconds.
        // The duration is rounded up to the nearest digit to make up for the truncation that
        // happens when converting from NanoSeconds to Seconds.
        conversion_complete.duration
                = pTsbItem->pTsb->conversion_status.end_time
                  - pTsbItem->pTsb->conversion_status.start_time;

        RILOG_INFO("%s -- Sending RI_DVR_EVENT_TSB_CONVERSION_COMPLETE with duration %"PRIu64"ms, %"PRIu64" bytes\n",
                __FUNCTION__, conversion_complete.duration/CONVERT_MS_TO_NS, conversion_complete.size);

        g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
        {
            pTsbItem->pTsb->convert_callback(
                    RI_DVR_EVENT_TSB_CONVERSION_COMPLETE,
                    (void *) &conversion_complete,
                    pTsbItem->pTsb->convert_cb_data);
        }
        g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
    }

    return (doConversionState) tsbConversionStateIdle;
}

///**
// * Steady state indicating that conversion has stopped
// */
//static doConversionState tsbConversionStateStopped(ri_pipeline_t* pPipeline)
//{
//    RILOG_INFO("%s Conversion state = TSB_STATE_CONVERSION_STOPPED\n", __FUNCTION__);
//
//    return (doConversionState)tsbConversionStateIdle;
//}

/**
 * This thread hosts both the TSB and conversion state machines.  Included
 * here is a switch statement that accepts commands from external threads.
 */
gpointer tsb_thread(gpointer data)
{
    if (NULL == tsbCategory)
    {
        tsbCategory = log4c_category_get("RI.Pipeline.TSB");
    }

    /* Make note of pipeline we are associated with */
    tsb_item_t *pTsbItem = data; // This is our TSB item
    tsb_thread_command *command = NULL;
    GTimeVal wake_at;
    gboolean terminate = FALSE; // Signals thread is exiting
#ifndef PRODUCTION_BUILD
    char temp[32] =
    { '\0' }; // used for formatting long long time values
#endif

    // set state machine states to their initial values
    doTsbState tsbState = (doTsbState) tsbStateIdle;
    doConversionState tsbConversionState =
            (doConversionState) tsbConversionStateIdle;

    (void) g_async_queue_ref(pTsbItem->pTsb->tsb_queue);

    while (FALSE == terminate)
    {
        g_get_current_time(&wake_at);

        g_time_val_add(&wake_at, pTsbItem->pTsb->tsb_wake_interval);
        command = g_async_queue_timed_pop(pTsbItem->pTsb->tsb_queue, &wake_at);

        //
        // Handle any commands requested from outside of this thread
        //
        if (command)
        {
            switch (command->action)
            {
            case TSB_ACTION_START:
                RILOG_DEBUG("%s -- Received TSB_ACTION_START\n", __FUNCTION__);

                (void) g_object_set(
                        G_OBJECT(pTsbItem->pPipeline->data->tsbsink),
                        "bitBucket", TRUE, NULL);

                tsbState = (doTsbState) tsbStateInitFilesink;
                break;

            case TSB_ACTION_STOP:
                RILOG_DEBUG("%s -- Received TSB_ACTION_STOP\n", __FUNCTION__);

                // transition TSB state maching to STOPPING state
                tsbState = (doTsbState) tsbStateStopping;
                break;

            case TSB_ACTION_CONVERT:
#ifndef PRODUCTION_BUILD
                RILOG_DEBUG(
                        "%s -- Received TSB_ACTION_CONVERT, start time = %s ns, duration (s) = %u\n",
                        __FUNCTION__,
                        IfsLongLongToString(
                                pTsbItem->pTsb->requested_convert_start_time_ns,
                                temp),
                        pTsbItem->pTsb->requested_convert_duration_s);
#endif

                // transition conversion state machine to the STARTING state
                tsbConversionState
                        = (doConversionState) tsbConversionStateStarting;
                break;

            case TSB_ACTION_CONVERT_STOP:
                RILOG_DEBUG("%s -- Received TSB_ACTION_CONVERT_STOP\n",
                        __FUNCTION__);

                // transition conversion state machine to the STOPPING state
                tsbConversionState
                        = (doConversionState) tsbConversionStateStopping;
                break;

            case TSB_ACTION_WAKE_THREAD_ONLY:
                RILOG_DEBUG("%s -- Received TSB_ACTION_WAKE_THREAD_ONLY\n",
                        __FUNCTION__);
                break;

            default:
                RILOG_WARN("%s -- TSB thread received invalid command action\n",
                        __FUNCTION__);
                break;
            }
            // Free the command structure
            g_free(command);
        }

        //
        // process TSB state machine states
        //
        g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
        {
            tsbState = tsbState(pTsbItem);
        }
        g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));

        // check for our termination condition
        if (((doTsbState) tsbStateTerminating) == tsbState)
        {
            terminate = TRUE;
        }

        //
        // process conversion state machine states only while TSB is in ACTIVE state
        //
        if (((doTsbState) tsbStateActive) == tsbState)
        {
            g_static_rec_mutex_lock(&(pTsbItem->pTsb->tsb_mutex));
            {
                tsbConversionState = tsbConversionState(pTsbItem);
            }
            g_static_rec_mutex_unlock(&(pTsbItem->pTsb->tsb_mutex));
        }
    } /* endwhile not terminating */

    RILOG_DEBUG("%s -- Thread terminating\n", __FUNCTION__);

    //   g_async_queue_unref(pPipeline->data->tsb.tsb_queue);
    g_async_queue_unref(pTsbItem->pTsb->tsb_queue);
    return NULL;

} /* tsb_thread */
