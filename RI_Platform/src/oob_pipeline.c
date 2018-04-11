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
#include <ri_oob_pipeline.h>
#include <glib.h>
#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#include <gst/app/gstappbuffer.h>

#include <stdlib.h>

#include <ri_log.h>

#include "gst_utils.h"
#include "oob_section_filter.h"
#include "fdc.h"
#include "platform.h"

// table_id            8 bits +
// zero                2 bits +
// reserved            2 bits +
// section_length      12 bits +
//
// = 3 bytes
#define TABLE_HEADER_LENGTH 3

// 2-byte OOB data PID (0x1FFC)
#define OOB_DATA_HEADER_LENGTH 2

#define RILOG_CATEGORY oobPipelineLogCat
log4c_category_t* oobPipelineLogCat = NULL;


typedef struct ri_oob_pipeline_data_s
{
    GstElement* gst_fdcsectionsrc;
    GstElement* gst_oobpipeline;

    ri_section_filter_t* section_filter;

    GMutex* fdcDataMutex;

    gboolean requestOOBTablesComplete;

} ri_oob_pipeline_data_t;

static ri_section_filter_t* get_section_filter(ri_oob_pipeline_t* object)
{
    ri_oob_pipeline_data_t* data = object->data;
    return data->section_filter;
}

extern char *dateString(char *date, int dateSize);
extern uint16_t cablecard_get_vct_id(void);

static void trackVCT(uint16_t id)
{
    static uint16_t ids[] =
    { 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF };
    ri_bool log = FALSE;
    char *p, buf[32];
    int i;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    buf[0] = 0;
    p = buf;

    for (i = 0; i < sizeof(ids) / sizeof(id); i++)
    {
        if (ids[i] == id)
        {
            break;
        }
        else if (ids[i] == 0xFFFF)
        {
            ids[i] = id;
            p += sprintf(p, "%04X", ids[i]);
            log = TRUE;
            break;
        }

        p += sprintf(p, "%04X,", ids[i]);
    }

    if (log)
    {
        RILOG_INFO("%s -- $ VCT_IDS: %s\n", __FUNCTION__, buf);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

static void trackTID(unsigned char tid)
{
    static unsigned char tids[9] =
    { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    size_t i;

    RILOG_TRACE("%s -- Entry (%X)\n", __FUNCTION__, tid);

    if (NULL == strchr((char *) tids, (char) tid))
    {
        if ((i = strlen((char *) tids)) < sizeof(tids))
        {
            *(tids + i) = tid;
            RILOG_INFO("%s $ TIDS: %02X,%02X,%02X,%02X,%02X,%02X,%02X,%02X\n",
                    __FUNCTION__, tids[0], tids[1], tids[2], tids[3], tids[4],
                    tids[5], tids[6], tids[7]);
        }
    }

    //fprintf(stdout, ".");
    //(void) fflush(stdout);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_bool handleSectionData(uint8_t *data, int len, ri_oob_pipeline_t* pipeline)
{
    size_t binSize = 0;
    ri_oob_pipeline_data_t *oob_data = NULL;

    if (NULL == data || NULL == pipeline)
        return FALSE;

    data = g_base64_decode((char *) data, &binSize);
    oob_data = pipeline->data;
    //hex_dump(data, binSize);

    // If we have data, place it the SCTE65 table cache
    // and push it into the oob pipeline 
    if (binSize != 0)
    {
        char *table_type_str = "unknown";
        char *tid_str = "unknown";
        uint8_t table_type = 0;
        // TID is first byte past OOB header
        uint8_t tid = data[OOB_DATA_HEADER_LENGTH];
        uint16_t vct_id = 0;

        switch (tid)
        {
        case XAIT_TID:
            tid_str = "XAIT";
            break;
        case NIT_TID:
            tid_str = "NIT";
            break;
        case NTT_TID:
            tid_str = "NTT";
            break;
        case SVCT_TID:
            tid_str = "SVCT";
            table_type = (data[6] & 0x0F);
            switch (table_type)
            {
            case 0:
                table_type_str = "VCM";
                break;
            case 1:
                table_type_str = "DCM";
                break;
            case 2:
                table_type_str = "ICM";
                break;
            }
            vct_id = ((data[7] << 8) | data[8]);
            RILOG_DEBUG("%s - SVCT: VCT_ID = %X TT(%X) = %s\n", __FUNCTION__,
                    vct_id, table_type, table_type_str);
            trackVCT(vct_id);

            //if (FilterSvct() && (vct_id != FilterSvctId()))
            //{
            //    RILOG_DEBUG("%s ignoring %s for VCT_ID = %X\n", __FUNCTION__,
            //            table_type_str, vct_id);
            //    return TRUE;
            //}
            break;
        case STT_TID:
            tid_str = "STT";
            break;
        case MGT_TID:
            tid_str = "MGT";
            break;
        case RRT_TID:
            tid_str = "RRT";
            break;
        case LVCT_TID:
            tid_str = "LVCT";
            break;
        case EAS_TID:
            tid_str = "EAS";
            break;
            break;
        default:
            RILOG_DEBUG("%s -- unrecognized TID = %X\n", __FUNCTION__, tid);
            break; // still process for now...
        }

        GstAppSrc *appsrc = GST_APP_SRC(oob_data->gst_fdcsectionsrc);

        // Create a new GstBuffer with our section data
        GstBuffer *gstbuf = gst_buffer_new_and_alloc(binSize);
        memcpy(GST_BUFFER_DATA(gstbuf), data, binSize);

        size_t size = 0;
        size_t headerAdjustment = 0;
        uint32_t crc = 0;
        
        // set size to the section length field
        size = (((data[3] & 0x0F) << 8) | data[4]);

        headerAdjustment = (OOB_DATA_HEADER_LENGTH + TABLE_HEADER_LENGTH);
 
        if (size != binSize - headerAdjustment)
        {
            RILOG_ERROR("%s -- Computed section size (%d) != data size (%d)\n",
                        __FUNCTION__, size, binSize - headerAdjustment);
            g_free(data);
            return TRUE;
        }

        // Our actual section buffer size should include the table header, but
        // not the OOB data header. This is because the code that reads the
        // data from files expects to have to add the OOB data header since our
        // real headend data captures do not have this header.
        size += TABLE_HEADER_LENGTH;
        
        crc |= data[2 + (size - 4)] & 0xFF;
        crc <<= 8;
        crc |= data[2 + (size - 3)] & 0xFF;
        crc <<= 8;
        crc |= data[2 + (size - 2)] & 0xFF;
        crc <<= 8;
        crc |= data[2 + (size - 1)] & 0xFF;

        //fprintf(stderr,"TID: %4s CRC = %08X\n", tid_str, crc);
        //(void) fflush(stdout);
            
        trackTID(tid);
        RILOG_DEBUG("%s -- received Table: %s(%02x)\n", __func__, tid_str, tid);

        (void) gst_app_src_push_buffer(appsrc, gstbuf);
        g_free(data);
    }

    return TRUE;
}

/**
 * getFdcData: method used to get the data from the FDC carousel
 * @param index: 
 * @returns 
 */
static gboolean getFdcData(ri_oob_pipeline_t* oob_pipeline)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    ri_oob_pipeline_data_t* data = oob_pipeline->data;
    SectionCache* fdcSects = GetFdcSectionCache();
    uint8_t *section = NULL;
    int sect = 0;
    int len = 0;

    // reload FDC sections...
    FdcExit();

    if (!FdcInit())
    {
        RILOG_ERROR("%s -- FdcInit failed?!\n", __FUNCTION__);
    }

    
    g_mutex_lock(data->fdcDataMutex);

    for (sect = 0; sect < GetNumSections(fdcSects); sect++)
    {
        section = (uint8_t*) g_try_malloc0(RCVBUFSIZE);

        if (NULL == section)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        if (0 != (len = GetSection(sect, section, RCVBUFSIZE, fdcSects)))
        {
            if (!handleSectionData(section, len, oob_pipeline))
            {
                RILOG_DEBUG("%s FALSE = handleSectionData()?!\n", __FUNCTION__);
                break;
            }

        }

        g_free(section);
    }

    g_mutex_unlock(data->fdcDataMutex);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return TRUE;
}

ri_oob_pipeline_t* create_oob_pipeline()
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstElement *fdcsectionsrc = gst_load_element("appsrc", "fdcsectionsrc");
    GstElement *sectionfilter = gst_load_element("sectionfilter",
            "oobsectionfilter");
    GstElement *sectionsink = gst_load_element("sectionsink", "oobsectionsink");

    // Get logging category
    oobPipelineLogCat = log4c_category_get("RI.Pipeline.OOB");

    // Setup the section source element
    // The allowed caps for the src pad
    g_object_set(fdcsectionsrc, "caps", gst_caps_new_simple(
            "application/x-sections", NULL), NULL);
    // The format of the segment events and seek
    g_object_set(fdcsectionsrc, "format", GST_FORMAT_BYTES, NULL);
    // The size of the data stream (-1 if unknown)
    g_object_set(fdcsectionsrc, "size", (gint64) - 1, NULL);
    // The type of the stream
    g_object_set(fdcsectionsrc, "stream-type", GST_APP_STREAM_TYPE_STREAM, NULL);
    // The maximum number of bytes to queue internally (0 = unlimited)
    g_object_set(fdcsectionsrc, "max-bytes", (guint64)(100 * 4096), NULL);
    // Block push-buffer when max-bytes are queued
    g_object_set(fdcsectionsrc, "block", FALSE, NULL);

    // Allocate pipeline object
    ri_oob_pipeline_t* oob_pipeline = g_try_malloc0(sizeof(ri_oob_pipeline_t));

    if (NULL == oob_pipeline)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Assign function implementations
    oob_pipeline->get_section_filter = get_section_filter;

    // Allocate object private data
    oob_pipeline->data = g_try_malloc0(sizeof(ri_oob_pipeline_data_t));

    if (NULL == oob_pipeline->data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    ri_oob_pipeline_data_t* data = oob_pipeline->data;

    // Create section filter and section source
    data->section_filter
            = create_oob_section_filter(sectionfilter, sectionsink);
    data->gst_fdcsectionsrc = fdcsectionsrc;

    // Create a new GStreamer pipeline for our elements
    data->gst_oobpipeline = gst_pipeline_new("oob_pipeline");

    // FIRST add elements to the bin/pipeline, THEN link them.
    // Otherwise the element pads won't connect.
    gst_bin_add_many(GST_BIN(data->gst_oobpipeline), fdcsectionsrc,
            sectionfilter, sectionsink, NULL);
    (void) gst_element_link_many(fdcsectionsrc, sectionfilter, sectionsink,
            NULL);
    (void) gst_element_set_state(data->gst_oobpipeline, GST_STATE_PLAYING);

    // Pipeline is ready, now set-up FDC
    data->fdcDataMutex = g_mutex_new();
    data->requestOOBTablesComplete = TRUE;

    // Get the OOB Table Request Period
    int period = 3; // default value
    char *p;
    if ((p = ricfg_getValue("RIPlatform", "RI.Platform.OOBtableRequestPeriod")))
    {
        period = atoi(p);
    }

    // Finally, schedule our function to repeatedly request OOB tables
    RILOG_INFO("%s -- setting OOB table refresh to %d\n", __FUNCTION__, period);
    (void)g_timeout_add_seconds(period, (GSourceFunc) getFdcData, oob_pipeline);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return oob_pipeline;
}

void destroy_oob_pipeline(ri_oob_pipeline_t *oob_pipeline)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL != oob_pipeline)
    {
        ri_oob_pipeline_data_t* data = oob_pipeline->data;

        destroy_oob_section_filter(data->section_filter);
        (void) gst_element_set_state(data->gst_oobpipeline, GST_STATE_NULL);
        gst_object_unref(data->gst_oobpipeline);
        g_free(oob_pipeline->data);
        g_free(oob_pipeline);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

