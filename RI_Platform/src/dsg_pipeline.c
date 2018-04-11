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

#include <ri_pipeline_manager.h>
#include <ri_cablecard.h>
#include <ri_dsg_pipeline.h>
#include <glib.h>
#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#include <gst/app/gstappbuffer.h>

#include <stdlib.h>

#include <ri_log.h>

#include "gst_utils.h"
#include "dsg_section_filter.h"
#include "dsg.h"
#include "platform.h"

extern char *dateString(char *date, int dateSize);

// table_id            8 bits +
// zero                2 bits +
// reserved            2 bits +
// section_length      12 bits +
//
// = 3 bytes
#define TABLE_HEADER_LENGTH 3

// version             2 bits +
// reserved            1 bits +
// PID                13 bits +
//
// = 2 bytes
#define DSG_HEADER_LENGTH 2

#define CRC_LENGTH        4

#define RILOG_CATEGORY dsgPipelineLogCat
log4c_category_t* dsgPipelineLogCat = NULL;

typedef struct ri_dsg_pipeline_data_s
{
    GstElement* gst_ualsectionsrc;
    GstElement* gst_dsgpipeline;

    ri_section_filter_t* section_filter;

    GThread *cfd_Thread;

} ri_dsg_pipeline_data_t;

static ri_section_filter_t* get_section_filter(ri_dsg_pipeline_t* object)
{
    ri_section_filter_t* filter = NULL;

    if((NULL != object) && (NULL != object->data))
    {
        filter = ((ri_dsg_pipeline_data_t*)object->data)->section_filter;
        RILOG_TRACE("%s pipe(%p)->data(%p)->section_filter(%p)\n", __FUNCTION__,
               object, (ri_dsg_pipeline_data_t*)object->data, filter);
    }

    return filter;
}

int readCfdHeader(uint8_t *buf, size_t bufLen, ri_bool inSync)
{
    int numRead = 0;
    uint8_t expected[] = { 0x60, 0x00, 0x00, 0xB0 };  // find PAT if !inSync
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL == buf)
    {
        RILOG_ERROR("%s -- NULL input buffer!?\n", __func__);
        return -1;
    }
    else if (sizeof(expected) > bufLen)
    {
        RILOG_ERROR("%s -- input buffer too small (%d)!?\n", __func__, bufLen);
        return -2;
    }

    if (inSync)
    {
        numRead = CfdRead(buf, bufLen);
    }
    else
    {
        while (numRead < bufLen)
        {
            uint8_t tmp[2];
            int read = CfdRead(&tmp[0], 1);

            if (1 == read)
            {
                buf[numRead] = tmp[0];

                if (numRead < sizeof(expected))
                {
                    if (buf[numRead] != expected[numRead])
                    {
                        g_usleep(1);  // 1 usec yield between bytes...

                        if (numRead > 0)
                        {
                            numRead = 0;
                        }

                        continue;
                    }
                }

                numRead++;
            }
            else if (0 > read)
            {
                RILOG_ERROR("%s -- CfdRead error(%d)!?\n", __func__, read);
                numRead = read;
                break;
            }
        }
    }

    RILOG_DEBUG("%s -- Exit (%d)\n", __FUNCTION__, numRead);
    return numRead;
}

int readCfdSection(uint8_t *buf, size_t bufLen)
{
    static ri_bool inSync = FALSE;
    int numRead = 0;
    size_t headerSize = (TABLE_HEADER_LENGTH + DSG_HEADER_LENGTH);
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL == buf)
    {
        RILOG_ERROR("%s -- NULL input buffer!?\n", __func__);
        return -1;
    }
    else if (headerSize > bufLen)
    {
        RILOG_ERROR("%s -- input buffer too small (%d)!?\n", __func__, bufLen);
        return -2;
    }

    // read in enough data to get the section length...
    if (headerSize == (numRead = readCfdHeader(buf, headerSize, inSync)))
    {
        size_t len = (((buf[3] & 0x0F) << 8) | buf[4]);

        // read in the rest of the section...
        if (0 == len)
        {
            RILOG_ERROR("%s -- out-of-sync or header error!?\n", __func__);
            inSync = FALSE;
            return -3;
        }
        else if (headerSize+len > bufLen)
        {
            RILOG_ERROR("%s -- input buf too small (%d)!?\n", __func__, bufLen);
            inSync = FALSE;
            return -4;
        }
        else if (0 > (numRead = CfdRead(&buf[numRead], len)))
        {
            RILOG_WARN("%s -- CfdRead(%d) error!?\n", __FUNCTION__, len);
            inSync = FALSE;
            return -5;
        }
        else if (len != numRead)
        {
            RILOG_WARN("%s -- read %d bytes trying to get %d byte section!?\n",
                        __FUNCTION__, numRead, len);
            inSync = FALSE;
        }
        else if (inSync == FALSE)
        {
            inSync = TRUE;
            RILOG_INFO("%s -- in-sync!\n", __func__);
        }

        // set the number of bytes read to the total read during this call
        numRead += headerSize;
    }
    else
    {
        RILOG_WARN("%s -- read %d bytes trying to get %d byte header!?\n",
                    __FUNCTION__, numRead, headerSize);
    }

    RILOG_DEBUG("%s -- Exit (%d)\n", __FUNCTION__, numRead);
    return numRead;
}

ri_bool handleCfdData(uint8_t *bufp, int size, ri_dsg_pipeline_t* dsg_pipeline)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    ri_dsg_pipeline_data_t *dsg_data = NULL;

    if (NULL == bufp || NULL == dsg_pipeline)
        return FALSE;

    // If we have data push it into the DSG pipeline 
    while (size > 0)
    {
        uint8_t *data = bufp;
        dsg_data = dsg_pipeline->data;
        GstAppSrc *appsrc = GST_APP_SRC(dsg_data->gst_ualsectionsrc);

        // Mask off the first 3 bits of the DSG header.  This leaves just the
        // PID as first 2bytes of this packet, which is what is required by
        // the section filter element
        data[0] = data[0] & 0x1F;
        uint16_t pid = ((data[0] << 8) | data[1]);
        uint16_t len = (((data[3] & 0x0F) << 8) | data[4]);
        uint32_t crc = ((data[5+(len-4)] << 24) | (data[5+(len-3)] << 16) |
                        (data[5+(len-2)] << 8)  |  data[5+(len-1)]);

        // By returning TRUE here, we are logging the error as well as not
        // passing on the data to the pipeline.  But we also do not force a
        // restart of the DSG connection...
        //
        // calculating the MPEG CRC32 over the data inclusive of the embedded
        // CRC results in 0 for a correct section; non-zero == fail
        if (0 != mpegCrc32(&data[2], len + TABLE_HEADER_LENGTH))
        {
            hex_dump(data, size);
            RILOG_ERROR("%s Computed section crc (%08lX) != read crc (%08X)\n",
                __FUNCTION__,
                mpegCrc32(&data[2], len + TABLE_HEADER_LENGTH - CRC_LENGTH),
                crc);
            return TRUE;
        }

        // Create a new GstBuffer with our section data
        len += (TABLE_HEADER_LENGTH + DSG_HEADER_LENGTH);
        GstBuffer *gstbuf = gst_buffer_new_and_alloc(len);
        memcpy(GST_BUFFER_DATA(gstbuf), data, len);

        //if ((DebugMode() & (UDM_DSG|UDM_VERBOSE)) == (UDM_DSG|UDM_VERBOSE))
        {
            hex_dump(data, len);
            printf("read section CRC = %08X\n",crc);
        }
#if 0
        if (UsingBocr() && CaptureDsg())
        {
            char *path, file[1024], dateStr[32];
            char *date = dateString(dateStr, 32);
            date[9] = 0; // lop off hours, minutes, and seconds...

            if (NULL != (path = ricfg_getValue("RIPlatform",
                    "RI.Headend.resources.directory")))
            {
                RILOG_DEBUG("%s -- path = %s\n", __FUNCTION__, path);
                sprintf(file, "%s/dsgdata/cfd-%04X-%08X%s.bin", path,
                            pid, crc, date);

                (void) AddSectionToFile(file, data, len);
            }
        }
#endif
        (void) gst_app_src_push_buffer(appsrc, gstbuf);
        RILOG_DEBUG("%s pushed %d bytes for PID %X (%d), CRC:%X\n",
                   __func__, size, pid, pid, crc);

        size -= len;
        bufp += len;

        if (size > 0)
        {
            RILOG_INFO("%s - processed %d, %d left...\n", __func__, len, size);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return TRUE;
}

gpointer getCfdData(gpointer pData)
{
    int numRead = 0;
    uint32_t appId = 0;
    uint8_t buf[RCVBUFSIZE + 1];
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);
    ri_dsg_pipeline_t* dsg_pipeline = (ri_dsg_pipeline_t*)pData;

    //while (UsingBocr() || UsingSimulatedDsg())
    {
        ri_section_filter_t* filter = get_section_filter(dsg_pipeline);

        if (NULL != filter)
        {
            if (RI_ERROR_NONE == filter->get_appID(filter, &appId))
            {
                if (0 != appId)
                {
                    while (CfdDataAvailable())
                    {
                        //if (UsingSimulatedDsg())
                        {
                            numRead = CfdRead((uint8_t *)buf, RCVBUFSIZE);
                        }
                        //else
                        //{
                        //    numRead = readCfdSection((uint8_t *)buf, RCVBUFSIZE);
                        //}

                        if ((numRead < 0) || (!handleCfdData(buf, numRead, dsg_pipeline)))
                        {
                            // log warning
                            break;
                        }

                        g_usleep(1000);  // 1 msec yield between sections...
                    }
                }
            }
        }

        g_usleep(100000);  // 100 msec yield between attempts...
    }

    RILOG_INFO("%s -- Exit\n", __FUNCTION__);
    return NULL;
}

ri_dsg_pipeline_t* create_dsg_pipeline()
{
    // Get logging category
    dsgPipelineLogCat = log4c_category_get("RI.Pipeline.DSG");
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    GstElement *cfdsectionsrc = gst_load_element("appsrc", "cfdsectionsrc");
    GstElement *sectionfilter = gst_load_element("sectionfilter",
            "dsgsectionfilter");
    GstElement *sectionsink = gst_load_element("sectionsink", "dsgsectionsink");

    // Setup the section source element
    // The allowed caps for the src pad
    g_object_set(cfdsectionsrc, "caps", gst_caps_new_simple(
            "application/x-sections", NULL), NULL);
    // The format of the segment events and seek
    g_object_set(cfdsectionsrc, "format", GST_FORMAT_BYTES, NULL);
    // The size of the data stream (-1 if unknown)
    g_object_set(cfdsectionsrc, "size", (gint64) - 1, NULL);
    // The type of the stream
    g_object_set(cfdsectionsrc, "stream-type", GST_APP_STREAM_TYPE_STREAM, NULL);
    // The maximum number of bytes to queue internally (0 = unlimited)
    g_object_set(cfdsectionsrc, "max-bytes", (guint64)(100 * 4096), NULL);
    // Block push-buffer when max-bytes are queued
    g_object_set(cfdsectionsrc, "block", FALSE, NULL);

    // Allocate pipeline object
    ri_dsg_pipeline_t* dsg_pipeline = g_try_malloc0(sizeof(ri_dsg_pipeline_t));

    if (NULL == dsg_pipeline)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Assign function implementations
    dsg_pipeline->get_section_filter = get_section_filter;

    // Allocate object private data
    dsg_pipeline->data = g_try_malloc0(sizeof(ri_dsg_pipeline_data_t));

    if (NULL == dsg_pipeline->data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    ri_dsg_pipeline_data_t* data = dsg_pipeline->data;

    // Create section filter and section source
    data->section_filter
            = create_dsg_section_filter(sectionfilter, sectionsink);
    RILOG_INFO("%s - pipe(%p)->data(%p)->section_filter(%p)\n", __FUNCTION__,
               dsg_pipeline, data, data->section_filter);
    data->gst_ualsectionsrc = cfdsectionsrc;

    // Create a new GStreamer pipeline for our elements
    data->gst_dsgpipeline = gst_pipeline_new("dsg_pipeline");

    // FIRST add elements to the bin/pipeline, THEN link them.
    // Otherwise the element pads won't connect.
    gst_bin_add_many(GST_BIN(data->gst_dsgpipeline), cfdsectionsrc,
            sectionfilter, sectionsink, NULL);
    (void) gst_element_link_many(cfdsectionsrc, sectionfilter, sectionsink,
            NULL);
    (void) gst_element_set_state(data->gst_dsgpipeline, GST_STATE_PLAYING);

    // start CFD data thread...
    data->cfd_Thread = g_thread_create(getCfdData, dsg_pipeline, FALSE, 0);

    if (NULL == data->cfd_Thread)
    {
        RILOG_ERROR("%s -- g_thread_create() returned NULL?!\n", __FUNCTION__);
    }

    RILOG_INFO("%s -- Exit\n", __FUNCTION__);
    return dsg_pipeline;
}

void destroy_dsg_pipeline(ri_dsg_pipeline_t *dsg_pipeline)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL != dsg_pipeline)
    {
        ri_dsg_pipeline_data_t* data = dsg_pipeline->data;

        destroy_dsg_section_filter(data->section_filter);
        (void) gst_element_set_state(data->gst_dsgpipeline, GST_STATE_NULL);
        gst_object_unref(data->gst_dsgpipeline);
        g_free(dsg_pipeline->data);
        g_free(dsg_pipeline);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

