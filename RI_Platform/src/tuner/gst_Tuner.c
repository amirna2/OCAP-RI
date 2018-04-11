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

#include <gst/gst.h>

#include <platform.h>
#include <net_utils.h>
#include <ri_config.h>
#include <ri_log.h>

#include "tuner.h"
#include "gstreamer/gstpacedfilesrc.h"

#undef GST_TUNER_DEBUG
#undef USE_GST_FILESRC

// Logging category
log4c_category_t* gst_RILogCategory = NULL;

// Use GST category for logs in this file
#define RILOG_CATEGORY gst_RILogCategory

#define NAME_SIZE   64

// contain all the module data gst_Tuner requires:
static struct Tuner
{
    // streamer file URL
    char mSrcUrl[MAXURLLEN];

    // set when streamer is active
    ri_bool mStreaming;
  
    // GStreamer tuner type uses these
    GstElement *mGstFileSrc;
    GstElement *mGstQueue;
    GstElement *mGstUdpSink;
    GstElement *mGstPipeline;

} tuner[MAX_TUNERS];

GstElement *gst_GetTunerElement(int index)
{
    return tuner[index].mGstFileSrc;
}

gboolean gst_tuner_bus_callback(GstBus* bus, GstMessage* msg, gpointer data)
{
    GError* err = NULL;
    gchar* debug = NULL;
    gchar* type = NULL;
    GstState old_state, new_state;
    int index = GPOINTER_TO_INT(data);

    switch (GST_MESSAGE_TYPE(msg))
    {
        case GST_MESSAGE_EOS:
            RILOG_DEBUG("%s: %s EOS\n", __func__, GST_OBJECT_NAME(msg->src));
            break;

        case GST_MESSAGE_ERROR:
            gst_message_parse_error(msg, &err, &debug);
            type = "ERROR";
            break;

        case GST_MESSAGE_WARNING:
            gst_message_parse_warning(msg, &err, &debug);
            type = "WARNING";
            break;

        case GST_MESSAGE_INFO:
            gst_message_parse_info(msg, &err, &debug);
            type = "INFO";
            break;

        case GST_MESSAGE_STATE_CHANGED:
            gst_message_parse_state_changed(msg, &old_state, &new_state, NULL);
            RILOG_DEBUG("%s: %s changed state from %s to %s\n", __func__,
                        GST_OBJECT_NAME(msg->src),
                        gst_element_state_get_name(old_state),
                        gst_element_state_get_name(new_state));
            break;

        case GST_MESSAGE_NEW_CLOCK:
            RILOG_DEBUG("%s: %s CLOCK\n", __func__, GST_OBJECT_NAME(msg->src));
            tuner[index].mStreaming = TRUE;
            break;

        default:
            RILOG_WARN("%s: %s unhandled %s\n", __func__,
                        GST_OBJECT_NAME(msg->src), GST_MESSAGE_TYPE_NAME(msg));
            break;
    }

    if (NULL != err)
    {
        RILOG_ERROR("%s: %s %s message - %s\n",
                    __func__, GST_OBJECT_NAME(msg->src), type, err->message);
        g_error_free(err);

        if (NULL != debug)
        {
            RILOG_DEBUG("-- debug details: %s\n", debug);
            g_free(debug);
        }
    }

    return TRUE;
}

void gst_TunerInit(int index, unsigned short port)
{
    char* element = NULL;
    char elementName[NAME_SIZE];
    GstBus* bus;

    RILOG_DEBUG("%s(%d);\n", __FUNCTION__, index);

    // Create our logging category and other one-time initializations...
    if(NULL == gst_RILogCategory)
    {
        gst_RILogCategory = log4c_category_get("RI.Tuner.GST");
        tuner[index].mStreaming = FALSE;
        tuner[index].mGstFileSrc = NULL;
        tuner[index].mGstQueue = NULL;
        tuner[index].mGstUdpSink = NULL;
        tuner[index].mGstPipeline = NULL;
    }

    // Create GStreamer filesrc element
    if (tuner[index].mGstFileSrc == NULL)
    {
#ifdef USE_GST_FILESRC
        element = "filesrc";
#else
        element = "pacedfilesrc";
#endif
        // Create element alias based on tuner index
        snprintf(elementName, NAME_SIZE, "tuner[%d]-%s", index, element);

        if (NULL == (tuner[index].mGstFileSrc =
            gst_element_factory_make(element, elementName)))
        {
            RILOG_ERROR("%s Could not create GStreamer %s for tuner %d!\n",
                        __FUNCTION__, element, index);
            return;
        }

        g_object_set(G_OBJECT(tuner[index].mGstFileSrc), "location", "", NULL);
    }

    // see if we are connecting directly to the input pipeline...
    if (ricfg_getBoolValue("RIPlatform", "RI.Platform.tunerConnectDirect"))
    {
        RILOG_INFO("%s connecting %s directly to input pipeline...\n",
                        __FUNCTION__, elementName);
        // we are - no need to init the queue and UDP sink, just return early
        return;
    }

    // Create GStreamer queue element
    if (tuner[index].mGstQueue == NULL)
    {
        // Create element alias based on tuner index
        snprintf(elementName, NAME_SIZE, "tuner[%d]-queue", index);

        if (NULL == (tuner[index].mGstQueue =
            gst_element_factory_make("queue", elementName)))
        {
            RILOG_ERROR("%s Could not create GStreamer queue for tuner %d!\n",
                        __FUNCTION__, index);
            return;
        }
    }

    // Create GStreamer udpsink element
    if (tuner[index].mGstUdpSink == NULL)
    {
        char portStr[8] = {0};
        struct addrinfo hints = {0};
        struct addrinfo* srvrInfo = NULL;
        struct addrinfo* pSrvr = NULL;
        int sock = 0;
        int yes = 1;
        int ret = 0;

        // Create element alias based on tuner index
        snprintf(elementName, NAME_SIZE, "tuner[%d]-udpsink", index);

        if (NULL == (tuner[index].mGstUdpSink =
           gst_element_factory_make("udpsink", elementName)))
        {
            RILOG_ERROR("%s Could not create GStreamer udpsink for tuner %d\n",
                        __FUNCTION__, index);
            return;
        }

        hints.ai_family = AF_INET;
        hints.ai_socktype = SOCK_DGRAM;
        hints.ai_flags = AI_PASSIVE;
        snprintf(portStr, sizeof(portStr), "%d", port);

        if (0 != (ret = getaddrinfo(NULL, portStr, &hints, &srvrInfo)))
        {
            RILOG_ERROR("t%d %s: getaddrinfo[%s]\n", index, __FUNCTION__,
                                 gai_strerror(ret));
            return;
        }

        for(pSrvr = srvrInfo; pSrvr != NULL; pSrvr = pSrvr->ai_next)
        {
            if (0 > (sock = socket(pSrvr->ai_family, pSrvr->ai_socktype,
                                                     pSrvr->ai_protocol)))
            {
                RILOG_WARN("t%d %s socket() failed?\n", index, __FUNCTION__);
                continue;
            }

            if (0 > setsockopt(sock, SOL_SOCKET, SO_REUSEADDR,
                               (char*) &yes, sizeof(yes)))
            {
                RILOG_ERROR("t%d %s setsockopt failed?\n", index, __FUNCTION__);
                return;
            }

            if (0 > (bind(sock, pSrvr->ai_addr, pSrvr->ai_addrlen)))
            {
                CLOSESOCK(sock);
                RILOG_WARN("t%d %s bind() failed?\n", index, __FUNCTION__);
                continue;
            }

            // We are successfully bound!
            break;
        }

        if (NULL == pSrvr)
        {
            RILOG_WARN("t%d %s failed to bind for GST\n", index, __FUNCTION__);
            return;
        }
        else
        {
            char str[INET6_ADDRSTRLEN] = {0};
            net_ntop(pSrvr->ai_family, pSrvr->ai_addr, str, sizeof(str));
            g_object_set(G_OBJECT(tuner[index].mGstUdpSink),
                                  "async", FALSE,
                                  "port", port,
                                  "host", str,
                                  "buffer-size", (24*188),
                                  "sockfd", sock,
                                  NULL);
            RILOG_INFO("t%d %s bind for GST on %s:%d\n", index, __FUNCTION__,
                       str, port);
            freeaddrinfo(srvrInfo);
        }
    }

    // Create GStreamer pipeline
    if (tuner[index].mGstPipeline == NULL)
    {
        // Create element alias based on tuner index
        snprintf(elementName, NAME_SIZE, "tuner[%d]-pipeline",index);

        if (NULL == (tuner[index].mGstPipeline = gst_pipeline_new(elementName)))
        {
            RILOG_ERROR("%s Could not create GStreamer pipeline for tuner %d\n",
                        __FUNCTION__, index);
            return;
        }
    }

    // Construct pipeline
    gst_bin_add_many(GST_BIN(tuner[index].mGstPipeline),
                             tuner[index].mGstFileSrc,
                             tuner[index].mGstQueue,
                             tuner[index].mGstUdpSink, NULL);

    if (!gst_element_link_many(tuner[index].mGstFileSrc,
                               tuner[index].mGstQueue,
                               tuner[index].mGstUdpSink, NULL))
    {
        GST_ERROR_OBJECT(GST_BIN(tuner[index].mGstPipeline), "Linking failed!");
        // for now we will return (like the other fatal errors in this method),
        // we should (in many cases) be logging FATAL and exiting the program.
        // It eppears we aren't doing this in the event that we have multiple
        // tuners being initialized and prior tuners have succeeded to be
        // initialized.
        return;
    }

    // Register for BUS messages
    bus = gst_pipeline_get_bus(GST_PIPELINE(tuner[index].mGstPipeline));
    gst_bus_add_watch(bus, gst_tuner_bus_callback, GINT_TO_POINTER(index));
    gst_object_unref(bus);
}

ri_bool gst_TunerTune(int index, Stream *stream)
{
    ri_bool retVal = FALSE;
    ri_bool pcr_pacing = FALSE;
    ri_bool tuner_pid_filtering = FALSE;
    ri_bool rewrite_pcr_and_cc = FALSE;
    char* blkSizeStr = NULL;
    int blksize = 0;
    GstState state;

    if ((blkSizeStr = ricfg_getValue("RILaunch", "RI.Platform.tunerBlkSize")))
    {
        blksize = atoi(blkSizeStr) * TS_PACKET_SIZE;
    }

    pcr_pacing = ricfg_getBoolValue("RIPlatform", "RI.Platform.tunerPCRpacing");
    tuner_pid_filtering = ricfg_getBoolValue("RIPlatform",
                                             "RI.Platform.tunerPIDfiltering");
    rewrite_pcr_and_cc = ricfg_getBoolValue("RIPlatform",
                                            "RI.Platform.tunerRewritePcrAndCC");
    RILOG_DEBUG("%s Entering - i:%d, s:%p\n", __FUNCTION__, index, stream);
    g_object_set(G_OBJECT(tuner[index].mGstFileSrc),
                 "location", stream->srcUrl, "pidlist", "0x0000 ",
                 "blksize", blksize,
                 "pcr_pacing", pcr_pacing,
                 "tuner_pid_filtering", tuner_pid_filtering,
                 "rewrite_pcr_and_cc", rewrite_pcr_and_cc, NULL);

    // see if we are connecting directly to the input pipeline...
    if (ricfg_getBoolValue("RIPlatform", "RI.Platform.tunerConnectDirect"))
    {
        // we are - no need to setup the UDP sink
        tuner[index].mStreaming = TRUE;
        retVal = TRUE;
    }
    else
    {
        // Set UDP IP and port on the sink
        g_object_set(G_OBJECT(tuner[index].mGstUdpSink), 
                              "port", stream->destinationPort,
                              "host", stream->destinationAddress, NULL);
        RILOG_DEBUG("%s set - port:%d - host:%s\n", __FUNCTION__,
                   stream->destinationPort, stream->destinationAddress);

        // Run the pipeline
        gst_element_set_state(tuner[index].mGstPipeline, GST_STATE_PLAYING);

        // Requesting the state will cause this thread to block
        //  (for up to the timeout time) while any elements
        //  transition from the async state to their final states.
        if (GST_STATE_CHANGE_SUCCESS == gst_element_get_state(
                        tuner[index].mGstPipeline,
                        &state,         // state
                        NULL,           // pending
                        10000000000LL)) // timeout 1 second = 10^9 nanoseconds
        {
            RILOG_DEBUG("%s -- %s: state read ok\n", __FUNCTION__,
                        GST_OBJECT_NAME(tuner[index].mGstPipeline));
            retVal = TRUE;
        }
        else
        {
            RILOG_WARN("%s -- %s: state was NOT read ok\n", __FUNCTION__,
                        GST_OBJECT_NAME(tuner[index].mGstPipeline));
        }
    }

    RILOG_DEBUG("%s Returning: %s\n", __FUNCTION__, boolStr(retVal));
    return retVal;
}

char *gst_TunerStatus(int index)
{
    if(tuner[index].mStreaming)
    {
        return "gst_TunerStatus streaming";
    }
    else
    {
        return "gst_TunerStatus idle";
    }
}

void gst_TunerStop(int index)
{
    RILOG_DEBUG("%s(%d);\n", __FUNCTION__, index);

    // Stop the pipeline
    gst_element_set_state(tuner[index].mGstPipeline, GST_STATE_PAUSED);
    tuner[index].mStreaming = FALSE;
}

void gst_TunerExit(int index)
{
    RILOG_DEBUG("%s(%d);\n", __FUNCTION__, index);

    if(tuner[index].mStreaming)
    {
        gst_TunerStop(index);
    }
}

ri_bool gst_TunerRetune(int index)
{
    ri_bool retVal = FALSE;
    char *srcUrl = NULL;
    ri_bool tuner_pid_filtering = FALSE;
    ri_bool rewrite_pcr_and_cc = FALSE;
    GstState state;

    tuner_pid_filtering = ricfg_getBoolValue("RIPlatform",
                                             "RI.Platform.tunerPIDfiltering");
    rewrite_pcr_and_cc = ricfg_getBoolValue("RIPlatform",
                                            "RI.Platform.tunerRewritePcrAndCC");
    //
    // NOTE:
    // This method is called from ate_if.exe where all logging is to stderr
    // so don't use RILOG_XXXX, use fprintf followed by fflush.
    //

    if (tuner[index].mStreaming)
    {
        // Stop the previous stream
        gst_TunerStop(index);
        g_usleep(GST_STOP_DELAY);

        // Set-up the next...
        if (NULL == (srcUrl = gst_GetTspFileURL()))
        {
            fprintf(stderr, "ERROR: %s Tuner%d couldn't get TspFileURL\n",
                    __func__, index);
            (void) fflush(stderr);
        }
        else
        {
            fprintf(stderr, "%s Tuner%d got TspFileURL: %s\n",
                    __func__, index, srcUrl);
            (void) fflush(stderr);
        }

        // Set the file name into the source element and start the pipeline
        g_object_set(G_OBJECT(tuner[index].mGstFileSrc),
                     "location", srcUrl, "pidlist", "0x0000 ",
                     "tuner_pid_filtering", tuner_pid_filtering,
                     "rewrite_pcr_and_cc", rewrite_pcr_and_cc, NULL);

        // see if we are connecting directly to the input pipeline...
        if (ricfg_getBoolValue("RIPlatform","RI.Platform.tunerConnectDirect"))
        {
            tuner[index].mStreaming = TRUE;
            retVal = TRUE;
        }
        else
        {
            // and play
            gst_element_set_state(tuner[index].mGstPipeline,
                                  GST_STATE_PLAYING);
            // Requesting the state will cause this thread to block
            //  (for up to the timeout time) while any elements
            //  transition from the async state to their final states.
            if (GST_STATE_CHANGE_SUCCESS == gst_element_get_state(
                                tuner[index].mGstPipeline,
                                &state,         // state
                                NULL,           // pending
                                5000000000LL))  // timeout(1/2 second
            {
                RILOG_DEBUG("%s -- %s: state read ok\n", __FUNCTION__,
                            GST_OBJECT_NAME(tuner[index].mGstPipeline));
                tuner[index].mStreaming = TRUE;
                retVal = TRUE;
            }
            else
            {
                RILOG_WARN("%s -- %s: state was NOT read ok\n", __FUNCTION__,
                            GST_OBJECT_NAME(tuner[index].mGstPipeline));
            }
        }
    }

    fprintf(stderr, "DEBUG: %s Returning %s\n", __func__, boolStr(retVal));
    (void) fflush(stderr);
    return retVal;
}

char *gst_GetTspFileURL(void)
{
    int index = 0;
    int bytesRead;
    char *path, file[512];
    FILE *fp = NULL;

    // Get the RI Platform IP address the Streamer should talk to...
    if (NULL == (path = ricfg_getValue("RIPlatform",
            "RI.Headend.resources.directory")))
    {
        path = "c:\\resources\\tsplayer-file.txt";
        RILOG_WARN("%s TS Player file not specified!\n", __FUNCTION__);
    }

    RILOG_DEBUG("%s got %s\n", __FUNCTION__, path);
    sprintf(file, "%s/tsplayer-file.txt", path);

    if (NULL != (fp = fopen(file, "r")))
    {
        if (0 != (bytesRead = fread(tuner[index].mSrcUrl, 1, MAXURLLEN-1, fp)))
        {
            tuner[index].mSrcUrl[bytesRead] = 0;
        }

        fclose(fp);
    }

    RILOG_INFO("%s returning %s\n", __FUNCTION__, tuner[index].mSrcUrl);
    return tuner[index].mSrcUrl;
}

ri_bool gst_TunerIsStreaming(int index)
{
    return tuner[index].mStreaming;
}

/**
 * update transport stream PID list
 *
 * @param object The tuner "this" pointer
 * @param pids The list of PID refcounts
 * @return An error code detailing the success or failure of the request.
 */
ri_error gst_TunerUpdatePidList(int index, guint16 pids[8192])
{
    char list[MAX_PIDS * 8];    // max pids * "0xXXXX" + ' ' + nullch
    char* p = list;
    int i, pid;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    p += sprintf(p, "0x0000 0x1FFF ");

    for (i = pid = 0; i < MAX_PIDS && pid < 0x1FFF; i++)
    {
        while (pid++ < 0x1FFF)
        {
            if (0 != pids[pid])
            {
                RILOG_DEBUG("%s list[%d] = 0x%X\n", __func__, i, pid);
                p += sprintf(p, "0x%04X ", pid);
                break;
            }
        }
    }

    g_object_set(G_OBJECT(tuner[index].mGstFileSrc), "pidlist", list, NULL);
    return RI_ERROR_NONE;
}

