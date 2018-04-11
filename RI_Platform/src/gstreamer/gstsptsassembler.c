/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2012 Cable Television Laboratories, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * Alternatively, the contents of this file may be used under the
 * GNU Lesser General Public License Version 2.1 (the "LGPL"), in
 * which case the following provisions apply instead of the ones
 * mentioned above:
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * PID:element-sptsassembler
 *
 * FIXME:Describe sptsassembler here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! sptsassembler videoPID="0x0041" audioPID="0x0051"
 * ! fakesink silent=TRUE ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <gst/gst.h>
#include <gst/base/gstbitreader.h>
#include <glib/gprintf.h>
#include <stdlib.h>
#include <string.h>
#include "gstmpeg.h"
#include "gstpidfilter.h"
#include "gstsptsassembler.h"

GST_DEBUG_CATEGORY_STATIC ( gst_spts_assembler_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_spts_assembler_debug

#define PSI_INTERVAL    100000000
#define INIT_VER        32          // valid versions are 0 to 31

#undef DEBUG_INPUT_PSI
#undef DEBUG_OUTPUT_PSI

/* Assembler signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0,
    PROP_PROGRAM,
    PROP_PMTPID,
    PROP_VIDEOPID,
    PROP_AUDIOPID,
    PROP_INSERT_0_PRGM_PATS,
    PROP_REWRITE_ORIG_PAT_PMT,
    PROP_INSERT_CANNED_PAT_PMT,
    PROP_LOG_PAT_PMT,
    PROP_LAST
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE("sink",
        GST_PAD_SINK, GST_PAD_ALWAYS, GST_STATIC_CAPS("video/mpegts,"
            "packetsize=(int)188,"
            "systemstream=(boolean)true"));

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC, GST_PAD_ALWAYS, GST_STATIC_CAPS("video/mpegts,"
            "packetsize=(int)188,"
            "systemstream=(boolean)true"));

/*lint -e(123) -esym(551,parent_class)*/GST_BOILERPLATE (GstSptsAssembler, gst_spts_assembler, GstElement, GST_TYPE_ELEMENT)

//
// Forward declarations
//
static void gst_spts_assembler_set_property (GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec);
static void gst_spts_assembler_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec);

static gboolean gst_spts_assembler_src_event(GstPad *pad, GstEvent *event);
static GstFlowReturn gst_spts_assembler_chain(GstPad * pad, GstBuffer * buf);

static gboolean gst_spts_assembler_event(GstPad *pad, GstEvent *event);

static void createZeroPrgmPAT(GstSptsAssembler* assembler);
static void createCannedPAT(GstSptsAssembler* assembler);
static void createCannedPMT(GstSptsAssembler* assembler);

void gst_hexdump(GstObject *object, guint8* ppkt, int len)
{
#define DUMP_BYTES_PER_LINE 16
#define DUMP_CHARS_PER_BYTE  3
#define BYTE_LINE_LENGTH    (DUMP_BYTES_PER_LINE * DUMP_CHARS_PER_BYTE)

    guint i = 0;
    guint offset = 0;
    gchar buffer[BYTE_LINE_LENGTH + 1];

    GST_INFO_OBJECT(object, "===== data %p:", ppkt);

    while (i < len)
    {
        g_snprintf(&buffer[offset], DUMP_CHARS_PER_BYTE + 1, "%02X ", ppkt[i]);
        offset = (offset + DUMP_CHARS_PER_BYTE) % BYTE_LINE_LENGTH;
        i++;

        if (offset == 0)
        {
            GST_INFO_OBJECT(object, "%s", buffer);
        }
    }

    if (offset != 0)
    {
        buffer[offset] = '\0';
        GST_INFO_OBJECT(object, "%s", buffer);
    }

#undef BYTE_LINE_LENGTH
#undef DUMP_CHARS_PER_BYTE
#undef DUMP_BYTES_PER_LINE
}

static void gst_spts_assembler_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class,
            "MPEG-2 transport stream assembler.", "Codec/Demuxer",
            "Outputs a Single Program Transport Stream. ",
            "steve@secondstryke.com");
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the sptsassembler's class */
static void gst_spts_assembler_class_init(GstSptsAssemblerClass * klass)
{
    GObjectClass *gobject_class;

    GST_DEBUG_CATEGORY_INIT(gst_spts_assembler_debug, "sptsassembler", 0,
                            "SPTS Assembler element");
    gobject_class = (GObjectClass *)klass;
    gobject_class->set_property = gst_spts_assembler_set_property;
    gobject_class->get_property = gst_spts_assembler_get_property;

    g_object_class_install_property(gobject_class, PROP_PROGRAM,
            g_param_spec_uint("program", "Generated PMT program number",
                    "The program number to use for the generated PMT", 1,
                    65535, 1, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_PMTPID,
            g_param_spec_uint("pmtpid", "Generated PMT PID",
                    "The PID to use for the generated PMT", 0,
                    0x1EFF, PMT_PID, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_VIDEOPID,
            g_param_spec_uint("videopid", "videoPID",
                    "video PID to add to the SPTS", 1, 0x1EFF, 0x01E0,
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_AUDIOPID,
            g_param_spec_uint("audiopid", "audioPID",
                    "audio PID to add to the SPTS", 1, 0x1EFF, 0x02E0,
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_INSERT_0_PRGM_PATS,
            g_param_spec_uint("zero_prgm_pats", "Zero prgm PAT insertion flag",
                    "Zero program PAT insertion flag", 0,
                    1, 0, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_REWRITE_ORIG_PAT_PMT,
            g_param_spec_uint("rewrite_pat_pmt", "rewrite_pat_pmt flag",
                    "Rewrite PAT/PMT flag", 0,
                    1, 0, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_INSERT_CANNED_PAT_PMT,
            g_param_spec_uint("insert_canned_pat_pmt", "insert canned pat pmt",
                    "Insert canned PAT/PMT flag", 0,
                    1, 0, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_LOG_PAT_PMT,
            g_param_spec_uint("log_pat_pmt", "log_pat_pmt flag",
                    "log PAT/PMT flag", 0,
                    1, 0, G_PARAM_READWRITE));
}

/* initialize the new element
 * instantiate pads and add them to element GstSptsAssembler
 * set pad calback functions
 * initialize instance structure
 */
static void gst_spts_assembler_init(GstSptsAssembler * assembler,
        GstSptsAssemblerClass * gclass)
{
    gint i = 0;

    GST_INFO_OBJECT(assembler, "%s(%p, %p)", __func__, assembler, gclass);
    assembler->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    gst_pad_set_chain_function(assembler->sinkpad,
                               GST_DEBUG_FUNCPTR(gst_spts_assembler_chain));
    gst_pad_use_fixed_caps(assembler->sinkpad);
    assembler->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    gst_pad_use_fixed_caps(assembler->srcpad);
    gst_element_add_pad(GST_ELEMENT(assembler), assembler->sinkpad);
    gst_element_add_pad(GST_ELEMENT(assembler), assembler->srcpad);

    /* Add event handler to the sink pad */
    gst_pad_set_event_function(assembler->sinkpad, gst_spts_assembler_event);

    /* Add event handler to the src pad */
    gst_pad_set_event_function(assembler->srcpad, gst_spts_assembler_src_event);

    /* Set up any other data in this assembler */
    assembler->props_lock = g_mutex_new();
    assembler->prgm = 1;
    assembler->pmt_pid = 0xE0;
    assembler->pcr_pid = 0x01E0;
    assembler->video_pid = 0x01E0;
    assembler->audio_pid = 0x02E0;
    assembler->sysclock = gst_system_clock_obtain();
    assembler->last_psi_time = gst_clock_get_time(assembler->sysclock);
    assembler->log_pat_pmt = FALSE;
    assembler->rewrite_pat_pmt = FALSE;
    assembler->insert_canned_pat_pmt = FALSE;
    assembler->insert_zero_prgm_pats = FALSE;
    assembler->send_zero_prgm_pat = FALSE;
    assembler->pat_ver = INIT_VER;
    assembler->pmt_ver = INIT_VER;

    assembler->null_ts_packet = gst_buffer_new_and_alloc(TS_PACKET_SIZE);
    assembler->pat_ts_packet = gst_buffer_new_and_alloc(TS_PACKET_SIZE);
    assembler->pmt_ts_packet = gst_buffer_new_and_alloc(TS_PACKET_SIZE);
    assembler->zero_prgm_pat_ts_packet = gst_buffer_new_and_alloc(TS_PACKET_SIZE);
    // Initialize canned NULL TS packet.
    GST_BUFFER_DATA(assembler->null_ts_packet)[0] = 0x47;
    GST_BUFFER_DATA(assembler->null_ts_packet)[1] = 0x1F;
    GST_BUFFER_DATA(assembler->null_ts_packet)[2] = 0xFF;
    GST_BUFFER_DATA(assembler->null_ts_packet)[3] = 0x1F;

    // Initialize 0 program PAT TS packet.
    GST_BUFFER_DATA(assembler->zero_prgm_pat_ts_packet)[0] = 0x47;
    GST_BUFFER_DATA(assembler->zero_prgm_pat_ts_packet)[1] = 0x60;
    GST_BUFFER_DATA(assembler->zero_prgm_pat_ts_packet)[2] = 0x00;
    GST_BUFFER_DATA(assembler->zero_prgm_pat_ts_packet)[3] = 0x1F;

    // Initialize canned PAT TS packet.
    GST_BUFFER_DATA(assembler->pat_ts_packet)[0] = 0x47;
    GST_BUFFER_DATA(assembler->pat_ts_packet)[1] = 0x60;
    GST_BUFFER_DATA(assembler->pat_ts_packet)[2] = 0x00;
    GST_BUFFER_DATA(assembler->pat_ts_packet)[3] = 0x1F;

    // Initialize canned PMT TS packet.
    GST_BUFFER_DATA(assembler->pmt_ts_packet)[0] = 0x47;
    GST_BUFFER_DATA(assembler->pmt_ts_packet)[1] = 0x60;
    GST_BUFFER_DATA(assembler->pmt_ts_packet)[2] = 0xE0;
    GST_BUFFER_DATA(assembler->pmt_ts_packet)[3] = 0x10;

    for (i = 4; i < TS_PACKET_SIZE; i++)
    {
        GST_BUFFER_DATA(assembler->null_ts_packet)[i] = 0xFF;
        GST_BUFFER_DATA(assembler->zero_prgm_pat_ts_packet)[i] = 0xFF;
        GST_BUFFER_DATA(assembler->pat_ts_packet)[i] = 0xFF;
        GST_BUFFER_DATA(assembler->pmt_ts_packet)[i] = 0xFF;
    }

    GST_BUFFER_SIZE(assembler->null_ts_packet) = TS_PACKET_SIZE;
    gst_buffer_set_caps(assembler->null_ts_packet, GST_PAD_CAPS(assembler->srcpad));
    GST_BUFFER_SIZE(assembler->zero_prgm_pat_ts_packet) = TS_PACKET_SIZE;
    gst_buffer_set_caps(assembler->zero_prgm_pat_ts_packet, GST_PAD_CAPS(assembler->srcpad));
    GST_BUFFER_SIZE(assembler->pat_ts_packet) = TS_PACKET_SIZE;
    gst_buffer_set_caps(assembler->pat_ts_packet, GST_PAD_CAPS(assembler->srcpad));
    GST_BUFFER_SIZE(assembler->pmt_ts_packet) = TS_PACKET_SIZE;
    gst_buffer_set_caps(assembler->pmt_ts_packet, GST_PAD_CAPS(assembler->srcpad));
    createZeroPrgmPAT(assembler);
    createCannedPAT(assembler);
    createCannedPMT(assembler);
}

static void gst_spts_assembler_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec)
{
    GstSptsAssembler *assembler = GST_SPTSASSEMBLER(object);

    switch (prop_id)
    {
    case PROP_PROGRAM:
        g_mutex_lock(assembler->props_lock);
        assembler->prgm = g_value_get_uint(value);
        GST_INFO_OBJECT(assembler, "program set: 0x%X", assembler->prgm);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_PMTPID:
        g_mutex_lock(assembler->props_lock);
        assembler->pmt_pid = g_value_get_uint(value);
        GST_INFO_OBJECT(assembler, "pmt_pid set: 0x%X", assembler->pmt_pid);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_VIDEOPID:
        g_mutex_lock(assembler->props_lock);
        assembler->video_pid = g_value_get_uint(value);
        GST_INFO_OBJECT(assembler, "video_pid set: 0x%X", assembler->video_pid);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_AUDIOPID:
        g_mutex_lock(assembler->props_lock);
        assembler->audio_pid = g_value_get_uint(value);
        GST_INFO_OBJECT(assembler, "audio_pid set: 0x%X", assembler->audio_pid);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_INSERT_0_PRGM_PATS:
        g_mutex_lock(assembler->props_lock);
        assembler->insert_zero_prgm_pats = g_value_get_uint(value);
        GST_INFO_OBJECT(assembler, "%s insert 0-prgm PATs.",
                        assembler->insert_zero_prgm_pats? "will" : "will not" );
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_REWRITE_ORIG_PAT_PMT:
        g_mutex_lock(assembler->props_lock);
        assembler->rewrite_pat_pmt = g_value_get_uint(value);
        GST_INFO_OBJECT(assembler, "%s rewrite PATs and PMTs.",
                        assembler->rewrite_pat_pmt? "will" : "will not" );
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_INSERT_CANNED_PAT_PMT:
        g_mutex_lock(assembler->props_lock);
        assembler->insert_canned_pat_pmt = g_value_get_uint(value);
        GST_INFO_OBJECT(assembler, "%s insert_canned PATs and PMTs.",
                        assembler->insert_canned_pat_pmt? "will" : "will not" );
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_LOG_PAT_PMT:
        g_mutex_lock(assembler->props_lock);
        assembler->log_pat_pmt = g_value_get_uint(value);
        GST_INFO_OBJECT(assembler, "%s log PATs and PMTs.",
                        assembler->log_pat_pmt? "will" : "will not" );
        g_mutex_unlock(assembler->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_spts_assembler_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec)
{
    GstSptsAssembler *assembler = GST_SPTSASSEMBLER(object);

    switch (prop_id)
    {
    case PROP_PROGRAM:
        g_mutex_lock(assembler->props_lock);
        g_value_set_uint(value, assembler->prgm);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_PMTPID:
        g_mutex_lock(assembler->props_lock);
        g_value_set_uint(value, assembler->pmt_pid);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_VIDEOPID:
        g_mutex_lock(assembler->props_lock);
        g_value_set_uint(value, assembler->video_pid);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_AUDIOPID:
        g_mutex_lock(assembler->props_lock);
        g_value_set_uint(value, assembler->audio_pid);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_INSERT_0_PRGM_PATS:
        g_mutex_lock(assembler->props_lock);
        g_value_set_uint(value, assembler->insert_zero_prgm_pats);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_REWRITE_ORIG_PAT_PMT:
        g_mutex_lock(assembler->props_lock);
        g_value_set_uint(value, assembler->rewrite_pat_pmt);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_INSERT_CANNED_PAT_PMT:
        g_mutex_lock(assembler->props_lock);
        g_value_set_uint(value, assembler->insert_canned_pat_pmt);
        g_mutex_unlock(assembler->props_lock);
        break;
    case PROP_LOG_PAT_PMT:
        g_mutex_lock(assembler->props_lock);
        g_value_set_uint(value, assembler->log_pat_pmt);
        g_mutex_unlock(assembler->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean gst_spts_assembler_event(GstPad *pad, GstEvent *event)
{
    GstSptsAssembler *assembler = GST_SPTSASSEMBLER(gst_pad_get_parent(pad));

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_EOS:
        GST_INFO_OBJECT(assembler, "Received EOS event");
        assembler->send_zero_prgm_pat = TRUE;
        assembler->pat_ver = ((assembler->pat_ver + 1) % 32);
        assembler->pmt_ver = ((assembler->pmt_ver + 1) % 32);
        break;
    case GST_EVENT_NEWSEGMENT:
        GST_INFO_OBJECT(assembler, "Received NEWSEGMENT event");
        break;
    case GST_EVENT_FLUSH_START:
        GST_INFO_OBJECT(assembler, "Received FLUSH_START event");
        break;
    case GST_EVENT_FLUSH_STOP:
        GST_INFO_OBJECT(assembler, "Received FLUSH_STOP event");
        break;
    default:
        GST_INFO_OBJECT(assembler, "Received other (0x%X) event",
                        GST_EVENT_TYPE(event));
        break;
    }

    gst_object_unref(assembler);
    return gst_pad_event_default(pad, event);
}

static gboolean gst_spts_assembler_src_event(GstPad *pad, GstEvent *event)
{
    GstSptsAssembler *assembler = GST_SPTSASSEMBLER(GST_OBJECT_PARENT(pad));

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_QOS:
        gst_event_parse_qos(event, &assembler->qos_proportion,
                &assembler->qos_time_diff, &assembler->qos_timestamp);
        GST_DEBUG_OBJECT(assembler, "QOS: proportion %f, diff %lld, time %llu",
                         assembler->qos_proportion, assembler->qos_time_diff,
                         assembler->qos_timestamp);
        GST_DEBUG_OBJECT(assembler, "QOS diff %lldms",
                         assembler->qos_time_diff / 1000000);
        break;
    case GST_EVENT_SEEK:
        GST_INFO_OBJECT(assembler, "Received SEEK event");
        break;
    case GST_EVENT_NAVIGATION:
        GST_INFO_OBJECT(assembler, "Received NAVIGATION event");
        break;
    case GST_EVENT_LATENCY:
        GST_INFO_OBJECT(assembler, "Received LATENCY event");
        break;
    case GST_EVENT_FLUSH_START:
        GST_INFO_OBJECT(assembler, "Received FLUSH_START event");
        break;
    case GST_EVENT_FLUSH_STOP:
        GST_INFO_OBJECT(assembler, "Received FLUSH_STOP event");
        break;
    default:
        GST_INFO_OBJECT(assembler, "Received other (0x%X) event",
                        GST_EVENT_TYPE(event));
        break;
    }

    return gst_pad_event_default(pad, event);
}

#define LOG(format, ...) \
{ \
    if (TRUE == assembler->log_pat_pmt) \
        GST_INFO_OBJECT(assembler, format, ## __VA_ARGS__); \
    else \
        GST_LOG_OBJECT(assembler, format, ## __VA_ARGS__); \
}

#define WRITE(p, v) \
{ \
    GST_LOG_OBJECT(assembler, "%X <- %X", *p, v); \
    *p = v; \
}

static void createZeroPrgmPAT(GstSptsAssembler* src)
{
    guint8* pdata = NULL;
    guint8* ppat = NULL;
    guint16 patLen = 0;
    guint16 tsid = 0x0123;
    guint8 version = 1;
    guint32 crc = 0;

    GST_INFO_OBJECT(src, "%s(%p)", __func__, src);
    pdata = GST_BUFFER_DATA(src->zero_prgm_pat_ts_packet);

    //
    // WARNING: this method is assuming big endian!
    //
    if (NULL != pdata)
    {
        //patLen = 13;
        patLen = 9;
        pdata += 4;         // skip the TS header
        *pdata = 0;         // write 0 to the pointer val
        pdata += 1;         // point to the start of the PAT
        ppat = pdata;       // remember this position for CRC32
        *pdata = 0;         // write 0 to the TID
        pdata += 1;         // point beyond the table ID
        *pdata = (0x80) | ((patLen >> 8) & 0x0F);
        pdata += 1;         // point to 2nd half of length
        *pdata = (patLen & 0xFF);
        GST_INFO_OBJECT(src, "wrote len %d", patLen);
        pdata += 1;         // skip to the TSID MSB
        *pdata = ((tsid >> 8) & 0x0F);
        pdata += 1;         // skip to the TSID LSB
        *pdata = (tsid & 0xFF);
        GST_INFO_OBJECT(src, "wrote TSID %04X", tsid);
        pdata += 1;         // skip to the ver/curr/next
        *pdata = (0xC0 | ((version << 1) & 0x3E) | 0x01);
        GST_INFO_OBJECT(src, "wrote RES %02X", (*pdata & 0xC0) >> 6);
        GST_INFO_OBJECT(src, "wrote VER %02X", (*pdata & 0x3E) >> 1);
        GST_INFO_OBJECT(src, "wrote CNI %02X", *pdata & 0x01);
        pdata += 1;         // skip to the sect_num
        *pdata = 0;         // write 0 to the section number
        GST_INFO_OBJECT(src, "Sect %d", *pdata);
        pdata += 1;         // skip to the last sect_num
        *pdata = 0;         // write 0 to the last section number
        GST_INFO_OBJECT(src, "Last Sect %d", *pdata);

        pdata += 1;         // point to CRC
        crc32_init();
        crc = crc32_calc(ppat, (patLen - 4) + PAT_HDR_SZ);
        *pdata++ = ((crc >> 24) & 0xFF);
        *pdata++ = ((crc >> 16) & 0xFF);
        *pdata++ = ((crc >> 8) & 0xFF);
        *pdata++ = (crc & 0xFF);
        GST_INFO_OBJECT(src, "wrote CRC %08X", crc);
    }
    else
    {
        GST_WARNING_OBJECT(src, "Bad pkt: pdata = %p!?", pdata);
    }
}

static void createCannedPAT(GstSptsAssembler* assembler)
{
    guint8* pdata = NULL;
    guint8* ppat = NULL;
    guint16 patLen = 0;
    guint16 tsid = 0x1234;
    guint8 version = 1;
    guint32 crc = 0;

    GST_INFO_OBJECT(assembler, "%s(%p)", __func__, assembler);
    pdata = GST_BUFFER_DATA(assembler->pat_ts_packet);

    //
    // WARNING: this method is assuming big endian!
    //
    if (NULL != pdata)
    {
        patLen = 13;
        pdata += 4;         // skip the TS header
        *pdata = 0;         // write 0 to the pointer val
        pdata += 1;         // point to the start of the PAT
        ppat = pdata;       // remember this position for CRC32
        *pdata = 0;         // write 0 to the TID
        pdata += 1;         // point beyond the table ID
        *pdata = (0xB0) | ((patLen >> 8) & 0x0F);
        pdata += 1;         // point to 2nd half of length
        *pdata = (patLen & 0xFF);
        GST_INFO_OBJECT(assembler, "wrote len %d", patLen);
        pdata += 1;         // skip to the TSID MSB
        *pdata = ((tsid >> 8) & 0xFF);
        pdata += 1;         // skip to the TSID LSB
        *pdata = (tsid & 0xFF);
        GST_INFO_OBJECT(assembler, "wrote TSID %04X", tsid);
        pdata += 1;         // skip to the ver/curr/next
        *pdata = (0xC0 | ((version << 1) & 0x3E) | 0x01);
        GST_INFO_OBJECT(assembler, "wrote RES %02X", (*pdata & 0xC0) >> 6);
        GST_INFO_OBJECT(assembler, "wrote VER %02X", (*pdata & 0x3E) >> 1);
        GST_INFO_OBJECT(assembler, "wrote CNI %02X", *pdata & 0x01);
        pdata += 1;         // skip to the sect_num
        *pdata = 0;         // write 0 to the section number
        GST_INFO_OBJECT(assembler, "Sect %d", *pdata);
        pdata += 1;         // skip to the last sect_num
        *pdata = 0;         // write 0 to the last section number
        GST_INFO_OBJECT(assembler, "Last Sect %d", *pdata);

        pdata += 1;         // skip to the program number
        *pdata = ((assembler->prgm >> 8) & 0xFF);
        pdata += 1;         // point to 2nd half of program
        *pdata = (assembler->prgm & 0xFF);
        GST_INFO_OBJECT(assembler, "wrote prgm %d", assembler->prgm);
        pdata += 1;         // point to 1st half of PID
        *pdata = ((assembler->pmt_pid >> 8) & 0xFF);
        pdata += 1;         // point to 2nd half of PID
        *pdata = (assembler->pmt_pid & 0xFF);
        GST_INFO_OBJECT(assembler, "wrote PID 0x%04X", assembler->pmt_pid);

        pdata += 1;         // point to CRC
        crc32_init();
        crc = crc32_calc(ppat, (patLen - 4) + PAT_HDR_SZ);
        *pdata++ = ((crc >> 24) & 0xFF);
        *pdata++ = ((crc >> 16) & 0xFF);
        *pdata++ = ((crc >> 8) & 0xFF);
        *pdata++ = (crc & 0xFF);
        GST_INFO_OBJECT(assembler, "wrote CRC %08X", crc);
    }
    else
    {
        GST_WARNING_OBJECT(assembler, "Bad pkt: pdata = %p!?", pdata);
    }
}

static void createCannedPMT(GstSptsAssembler* assembler)
{
    guint8* pdata = NULL;
    guint8* ppmt = NULL;
    guint16 pmtLen = 0;
    guint16 piLen = 0;
    guint16 eiLen = 0;
    guint32 crc = 0;

    GST_INFO_OBJECT(assembler, "%s(%p)", __func__, assembler);
    pdata = GST_BUFFER_DATA(assembler->pmt_ts_packet);
    assembler->pcr_pid = assembler->video_pid;
    
    pmtLen = 23;        // PMT_HDR_LEN + SECT_HDR_LEN + (2 * ES_DESC_LEN)
    pdata += 4;         // skip the TS header
    WRITE(pdata, 0);    // write 0 to the pointer val
    pdata += 1;         // point to the start of the PMT
    ppmt = pdata;       // remember this position for CRC32
    WRITE(pdata, 2);    // write 2 to the PMT TID
    pdata += 1;         // point beyond the table ID to the reserved + length
    WRITE(pdata, (0xB0) | ((pmtLen >> 8) & 0x0F));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (pmtLen & 0xFF));
    GST_INFO_OBJECT(assembler, "wrote len %d", pmtLen);
    pdata += 1;         // point to the 1st half of program number
    WRITE(pdata, ((assembler->prgm >> 8) & 0xFF));
    pdata += 1;         // point to the 2nd half of program number
    WRITE(pdata, (assembler->prgm & 0xFF));
    GST_INFO_OBJECT(assembler, "wrote program %d", assembler->prgm);
    pdata += 1;         // skip to the ver/curr/next
    GST_INFO_OBJECT(assembler, "RES %02X", (*pdata & 0xC0) >> 6);
    GST_INFO_OBJECT(assembler, "VER %02X", (*pdata & 0x3E) >> 1);
    GST_INFO_OBJECT(assembler, "CNI %02X", *pdata & 0x01);
    pdata += 1;         // skip to the sect_num
    WRITE(pdata, 0);    // write 0 to the section number
    GST_INFO_OBJECT(assembler, "Sect %d", *pdata);
    pdata += 1;         // skip to the last sect_num
    WRITE(pdata, 0);    // write 0 to the last section number
    GST_INFO_OBJECT(assembler, "Last Sect %d", *pdata);
    pdata += 1;         // point to the reserved + PCR PID
    WRITE(pdata, (0xE0) | ((assembler->pcr_pid >> 8) & 0x1F));
    pdata += 1;         // point to 2nd half of PCR PID
    WRITE(pdata, (assembler->pcr_pid & 0xFF));
    GST_INFO_OBJECT(assembler, "wrote PCR PID 0x%X", assembler->pcr_pid);
    pdata += 1;         // point to the program info length
    WRITE(pdata, (0xF0) | ((piLen >> 8) & 0x0F));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (piLen & 0xFF));
    GST_INFO_OBJECT(assembler, "wrote len %d", piLen);
    pdata += 1;         // point to the ES stream type
    WRITE(pdata, 2);    // write 2 (MPEG-2 video)
    GST_INFO_OBJECT(assembler, "wrote stream type 2");
    pdata += 1;         // point to the reserved + video PID
    WRITE(pdata, (0xE0) | ((assembler->video_pid >> 8) & 0x1F));
    pdata += 1;         // point to 2nd half of video PID
    WRITE(pdata, (assembler->video_pid & 0xFF));
    GST_INFO_OBJECT(assembler, "wrote video PID 0x%X", assembler->video_pid);
    pdata += 1;         // point to the ES info length
    WRITE(pdata, (0xF0) | ((eiLen >> 8) & 0x0F));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (eiLen & 0xFF));
    GST_INFO_OBJECT(assembler, "wrote len %d", eiLen);
    pdata += 1;         // point to the ES stream type
    WRITE(pdata, 0x81); // write 0x81 (audio)
    GST_INFO_OBJECT(assembler, "wrote stream type 0x81");
    pdata += 1;         // point to the reserved + audio PID
    WRITE(pdata, (0xE0) | ((assembler->audio_pid >> 8) & 0x1F));
    pdata += 1;         // point to 2nd half of audio PID
    WRITE(pdata, (assembler->audio_pid & 0xFF));
    GST_INFO_OBJECT(assembler, "wrote audio PID 0x%X", assembler->audio_pid);
    pdata += 1;         // point to the ES info length
    WRITE(pdata, (0xF0) | ((eiLen >> 8) & 0x0F));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (eiLen & 0xFF));
    GST_INFO_OBJECT(assembler, "wrote len %d", eiLen);
    pdata += 1;         // point to CRC

    crc32_init();
    crc = crc32_calc(ppmt, (pmtLen - 4) + PMT_HDR_SZ);
    WRITE(pdata, ((crc >> 24) & 0xFF));
    pdata++;
    WRITE(pdata, ((crc >> 16) & 0xFF));
    pdata++;
    WRITE(pdata, ((crc >> 8) & 0xFF));
    pdata++;
    WRITE(pdata, (crc & 0xFF));
    pdata++;
    GST_INFO_OBJECT(assembler, "wrote CRC %08X", crc);
}

static guint8* handleAdaptationField(GstSptsAssembler* assembler, guint8* ppkt)
{
    guint8* pdata = ppkt;
    guint8 afFlags = 0;
    guint8 afLen = 0;

    pdata += 4;         // skip the TS header
    afLen = *pdata;     // get the Adaptation Field Length
    pdata += 1;         // skip the AF len
    afFlags = *pdata;   // get the Adaptation Field Flags

    if (afFlags != 0)
    {
 
        // TODO: perform special AF handling here in the future if ever needed.

        pdata += afLen; // skip the entire AF for now (pass all)
    }
    else
    {
        // using Adaptation Field to end-justify the data, remove AF bit and
        // let handlePtrField() relocate data to the front...
        ppkt[3] &= ~0x20;   // clear out the AF bit
        pdata = ppkt;       // reset the pdata pointer to the head of the pkt
    }

    return pdata;
}

static guint8* handlePtrField(GstSptsAssembler* assembler, guint8* ppkt)
{
    guint8* pdata = ppkt;
    guint8 ptr = 0;
    gint i = 0;

    pdata += 4;         // skip the TS header
    ptr = *pdata;       // save the ptr value
    WRITE(pdata, 0);    // write 0 to the PTR
    pdata += 1;         // point to the start of the data (TID)
    ptr += 1;           // index to the start of the data at the end of the ptr
    LOG("moving data - PTR: %02X", ptr)

    for(i = 0; &pdata[ptr+i] < (ppkt + TS_PACKET_SIZE); i++)
    {
        LOG("pdata[%d]%X <- pdata[%d]%X", i, pdata[i], ptr+i, pdata[ptr+i])
        pdata[i] = pdata[ptr+i];
        pdata[ptr+i] = 0xFF;
    }

    return pdata;
}

static guint8* handleHeader(GstSptsAssembler* assembler, guint8* ppkt)
{
    guint8* pdata = ppkt;
    LOG("HDR: %02X, %02X, %02X, %02X", pdata[0], pdata[1], pdata[2], pdata[3]);

    // check the AF bit in the TS header
    if (pdata[3] & 0x20)
    {
        pdata = handleAdaptationField(assembler, ppkt);
    }
    else
    {
        pdata += 4;     // skip the TS header
    }

    // check the PTR field
    if (*pdata != 0)
    {
        pdata = handlePtrField(assembler, ppkt);
    }
    else
    {
        pdata += 1;     // point to the start of the data (TID)
    }

    return pdata;
}

static void rewritePAT(GstSptsAssembler* assembler, guint8* ppkt)
{
    guint8* pdata = handleHeader(assembler, ppkt);
    guint8* ppat = NULL;
    guint16 patLen = 0;
    guint16 pmtPID = 0;
    guint16 tsid = 0;
    guint32 crc = 0;

    // re-write the PAT (MPTS -> SPTS)
    GST_DEBUG_OBJECT(assembler, "re-writing PAT...");

    patLen = 13;        // the canned length of a PAT with 1 program
    ppat = pdata;       // remember this position for CRC32
    LOG("TID: %02X", pdata[0]);
    pdata += 1;         // point beyond the table ID to the length
    WRITE(pdata, (*pdata & 0xF0) | ((patLen >> 8) & 0x0F));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (patLen & 0xFF));
    LOG("LEN: %d", patLen);
    pdata += 1;         // skip to the TSID MSB
    tsid = (pdata[0] & 0x1f) << 8;
    tsid |= pdata[1];
    pdata += 1;         // skip to the TSID LSB
    LOG("TSID: %04X", tsid);
    pdata += 1;         // skip to the ver/curr/next
    LOG("RES: %02X", (*pdata & 0xC0) >> 6);
    LOG("VER: %02X (orig)", (*pdata & 0x3E) >> 1);

    // if the current version hasn't been set yet, obtain and assign it...
    if (INIT_VER <= assembler->pat_ver)
    {
        assembler->pat_ver = ((*pdata & 0x3E) >> 1);
    }

    *pdata &= ~0x3E;    // clear old version and assign our version
    *pdata |= ((assembler->pat_ver << 1) & 0x3E);
    LOG("VER: %02X", (*pdata & 0x3E) >> 1);

    LOG("CNI: %02X", *pdata & 0x01);
    pdata += 1;         // skip to the sect_num
    LOG("Sect: %d", *pdata);
    pdata += 1;         // skip to the last sect_num
    LOG("Last Sect: %d", *pdata);
    pdata += 1;         // skip to the program number
    WRITE(pdata,((assembler->prgm >> 8) & 0xFF));
    pdata += 1;         // point to 2nd half of program
    WRITE(pdata,(assembler->prgm & 0xFF));
    LOG("PRG: %d", assembler->prgm);
    pdata += 1;         // point to 1st half of PID
    pmtPID = (pdata[0] & 0x1f) << 8;
    pmtPID |= pdata[1];

    // if we are supposed to "discover" the PMT PID...   do so
    if (0 == assembler->pmt_pid)
    {
        GST_INFO_OBJECT(assembler, "got PMT PID: %d (0x%04x)", pmtPID, pmtPID);
        assembler->pmt_pid = pmtPID;
    }
    else if (pmtPID != assembler->pmt_pid)
    {
        // it is too verbose to log this with MPTS to SPTS conversion
        GST_LOG_OBJECT(assembler, "%s PMT PID (have 0x%04x != read 0x%04x)",
                        __func__, assembler->pmt_pid, pmtPID);
    }

    WRITE(pdata, (0xE0 | ((assembler->pmt_pid >> 8) & 0x1F)));
    pdata += 1;         // point to 2nd half of PID
    WRITE(pdata, (assembler->pmt_pid & 0xFF));
    LOG("PID: 0x%04X", assembler->pmt_pid);
    pdata += 1;         // point to CRC

    crc32_init();
    crc = crc32_calc(ppat, (patLen - 4) + PAT_HDR_SZ);
    WRITE(pdata, ((crc >> 24) & 0xFF));
    pdata++;
    WRITE(pdata, ((crc >> 16) & 0xFF));
    pdata++;
    WRITE(pdata, ((crc >> 8) & 0xFF));
    pdata++;
    WRITE(pdata, (crc & 0xFF));
    pdata++;
    LOG("CRC: %08X", crc);

    while (pdata < (ppkt + TS_PACKET_SIZE))
    {
        *pdata++ = 0xFF;
    }
}

static void rewritePMT(GstSptsAssembler* assembler, guint8* ppkt)
{
    guint8* pdata = handleHeader(assembler, ppkt);
    guint8* ppmt = NULL;
    guint16 pmtLen = 0;
    guint16 piLen = 0;
    guint16 eiLen = 0;
    guint32 crc = 0;

    // re-write the PMT (MPTS -> SPTS)
    assembler->pcr_pid = assembler->video_pid;
    GST_DEBUG_OBJECT(assembler, "re-writing PMT...");

    pmtLen = 23;        // PMT_HDR_LEN + SECT_HDR_LEN + (2 * ES_DESC_LEN)
    ppmt = pdata;       // remember this position for CRC32
    LOG("TID: %02X", pdata[0]);
    pdata += 1;         // point beyond the table ID to the reserved + length
    WRITE(pdata, (*pdata & 0xF0) | ((pmtLen >> 8) & 0x0F));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (pmtLen & 0xFF));
    LOG("LEN: %d", pmtLen);
    pdata += 1;         // point to the 1st half of program number
    WRITE(pdata, ((assembler->prgm >> 8) & 0xFF));
    pdata += 1;         // point to the 2nd half of program number
    WRITE(pdata, (assembler->prgm & 0xFF));
    LOG("PRG: %d", assembler->prgm);
    pdata += 1;         // skip to the ver/curr/next
    LOG("RES: %02X", (*pdata & 0xC0) >> 6);
    LOG("VER: %02X", (*pdata & 0x3E) >> 1);
    LOG("VER: %02X (orig)", (*pdata & 0x3E) >> 1);
#if 0
    if (INIT_VER <= assembler->pmt_ver)
    {
        assembler->pmt_ver = ((*pdata & 0x3E) >> 1);
    }

    *pdata &= ~0x3E;    // clear old version
    *pdata |= ((assembler->pmt_ver << 1) & 0x3E);
    LOG("VER: %02X", (*pdata & 0x3E) >> 1);
#endif
    LOG("CNI: %02X", *pdata & 0x01);
    pdata += 1;         // skip to the sect_num
    LOG("Sect: %d", *pdata);
    pdata += 1;         // skip to the last sect_num
    LOG("Last Sect: %d", *pdata);
    pdata += 1;         // point to the reserved + PCR PID
    WRITE(pdata, (0xE0 | ((assembler->pcr_pid >> 8) & 0x1F)));
    pdata += 1;         // point to 2nd half of PCR PID
    WRITE(pdata, (assembler->pcr_pid & 0xFF));
    LOG("PCR PID: 0x%04X", assembler->pcr_pid);
    pdata += 1;         // point to the program info length
    WRITE(pdata, (0xF0 | ((piLen >> 8) & 0x0F)));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (piLen & 0xFF));
    LOG("LEN: %d", piLen);
    pdata += 1;         // point to the ES stream type
    WRITE(pdata, 2);    // write 2 (MPEG-2 video)
    LOG("stream type 2");
    pdata += 1;         // point to the reserved + video PID
    WRITE(pdata, (0xE0 | ((assembler->video_pid >> 8) & 0x1F)));
    pdata += 1;         // point to 2nd half of video PID
    WRITE(pdata, (assembler->video_pid & 0xFF));
    LOG("video PID: 0x%04X", assembler->video_pid);
    pdata += 1;         // point to the ES info length
    WRITE(pdata, (0xF0 | ((eiLen >> 8) & 0x0F)));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (eiLen & 0xFF));
    LOG("LEN: %d", eiLen);
    pdata += 1;         // point to the ES stream type
    WRITE(pdata, 0x81); // write 0x81 (audio)
    LOG("stream type 0x81");
    pdata += 1;         // point to the reserved + audio PID
    WRITE(pdata, (0xE0 | ((assembler->audio_pid >> 8) & 0x1F)));
    pdata += 1;         // point to 2nd half of audio PID
    WRITE(pdata, (assembler->audio_pid & 0xFF));
    LOG("audio PID: 0x%04X", assembler->audio_pid);
    pdata += 1;         // point to the ES info length
    WRITE(pdata, (0xF0 | ((eiLen >> 8) & 0x0F)));
    pdata += 1;         // point to 2nd half of length
    WRITE(pdata, (eiLen & 0xFF));
    LOG("LEN: %d", eiLen);
    pdata += 1;         // point to CRC

    crc32_init();
    crc = crc32_calc(ppmt, (pmtLen - 4) + PMT_HDR_SZ);
    WRITE(pdata, ((crc >> 24) & 0xFF));
    pdata++;
    WRITE(pdata, ((crc >> 16) & 0xFF));
    pdata++;
    WRITE(pdata, ((crc >> 8) & 0xFF));
    pdata++;
    WRITE(pdata, (crc & 0xFF));
    pdata++;
    LOG("CRC: %08X", crc);

    while (pdata < (ppkt + TS_PACKET_SIZE))
    {
        *pdata++ = 0xFF;
    }
}

#ifdef DEBUG_INPUT_PSI
static gboolean parse_PAT(GstSptsAssembler* src, GstBitReader *gbr)
{
    gint i = 0;
    guint8 temp = 0;
    guint8 pat[1024] = {0};
    guint16 pmtPID = 0;
    guint16 program = 0;
    gboolean retVal = TRUE;
    static guint16 tsid = 0;
    static guint16 bytes = 0;
    static guint16 patLen = 0;
    static guint16 patByte = 0;

    GST_INFO_OBJECT(src, "%s", __func__);
    patLen = 0;
    patByte = 0;
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // PAT pointer
    GST_LOG_OBJECT(src, "PTR = %d (0x%02X)", temp, temp);
    temp += ((temp == 0)? 0 : 1);                        // point to next
    gst_bit_reader_skip(gbr, (8 * temp));                // move to PAT
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // Table ID
    GST_LOG_OBJECT(src, "TID = %d (0x%02X)", temp, temp);
    gst_bit_reader_skip(gbr, 4);                         // SI + reserved
    gst_bit_reader_get_bits_uint16(gbr, &patLen, 12);    // PAT length
    GST_LOG_OBJECT(src, "PAT Section len = %d", patLen);
    gst_bit_reader_get_bits_uint16(gbr, &tsid, 16);      // TSID
    GST_LOG_OBJECT(src, "TSID = %X", tsid);
    patLen -= 2;
    gst_bit_reader_skip(gbr, 8);                         // reserved ++
    patLen -= 1;
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
    GST_LOG_OBJECT(src, "PAT Section number = %d", temp);
    patLen -= 1;
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
    GST_LOG_OBJECT(src, "PAT Last Section number = %d", temp);
    patLen -= 1;
    bytes = (gst_bit_reader_get_remaining(gbr) / 8);
    temp = (bytes >= (patLen - patByte))? (patLen - patByte) : bytes;
    GST_LOG_OBJECT(src, "read %d bytes of PAT from %d remaining", temp, bytes);

    for (i = 0; i < temp; i++)
    {
        gst_bit_reader_get_bits_uint8(gbr, &pat[patByte], 8);
        GST_LOG_OBJECT(src, "PAT[%d] = %02X", patByte, pat[patByte]);
        patByte++;
    }

    if (patByte == patLen)
    {
        GST_LOG_OBJECT(src, "End of PAT[%d]", patLen);
        GST_LOG_OBJECT(src, "    PAT CRC %02X%02X%02X%02X",
                       pat[patLen-4], pat[patLen-3],
                       pat[patLen-2], pat[patLen-1]);

        for (i = 0; i < (patLen - 4); i += 4)
        {
            program = pat[i] << 8;
            program |= pat[i + 1];
            pmtPID = (pat[i + 2] & 0x1f) << 8;
            pmtPID |= pat[i + 3];

            if (program == src->prgm)
            {
                if (0 == src->pmt_pid)
                {
                    src->pmt_pid = pmtPID;
                    GST_INFO_OBJECT(src, "Program Number = %d (0x%04x) "
                                         "PMT PID = %d (0x%04x)",
                                         program, program, pmtPID, pmtPID);
                }
            }
            else
            {
                retVal = FALSE;     // we need to re-write the PAT
                GST_LOG_OBJECT(src, "Skipped Program = %d (0x%04x) "
                                    "PMT PID = %d (0x%04x)",
                                    program, program, pmtPID, pmtPID);
            }
        }
    }

    return retVal;
}

static gboolean parse_PMT(GstSptsAssembler* src, GstBitReader *gbr)
{
    gint i = 0;
    gint j = 0;
    guint8 temp = 0;
    guint8 pmt[1024] = {0};
    guint16 pcrPID = 0;
    guint16 program = 0;
    gboolean retVal = FALSE;
    guint16 esPID = 0;
    guint16 bytes = 0;
    guint16 pmtLen = 0;
    guint16 pmtByte = 0;
    guint16 esInfoLen = 0;
    guint16 pgmInfoLen = 0;
    guint8 stream_type = 0;
    guint8 esDescriptor[1024] = {0};
    guint8 pgmDescriptor[1024] = {0};

    GST_INFO_OBJECT(src, "%s", __func__);
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // PMT pointer
    GST_LOG_OBJECT(src, "PTR = %d (0x%02X)", temp, temp);
    temp += ((temp == 0)? 0 : 1);                        // point to next
    gst_bit_reader_skip(gbr, (8 * temp));                // move to PMT
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // Table ID

    if (PMT_TID != temp)
    {
        GST_DEBUG_OBJECT(src, "TID != 0x02 (0x%02X) not PMT", temp);
        return TRUE;
    }
    else 
    {
        GST_LOG_OBJECT(src, "TID = %d (0x%02X)", temp, temp);
    }

    gst_bit_reader_skip(gbr, 4);                         // SI + reserved
    gst_bit_reader_get_bits_uint16(gbr, &pmtLen, 12);    // PMT length
    GST_INFO_OBJECT(src, "PMT Section len = %d", pmtLen);
    gst_bit_reader_get_bits_uint16(gbr, &program, 16);   // program #
    GST_INFO_OBJECT(src, "PRGM = %d", program);
    pmtLen -= 2;
    gst_bit_reader_skip(gbr, 8);                         // reserved ++
    pmtLen -= 1;
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
    GST_INFO_OBJECT(src, "PMT Section number = %d", temp);
    pmtLen -= 1;
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
    GST_INFO_OBJECT(src, "PMT Last Section number = %d", temp);
    pmtLen -= 1;
    gst_bit_reader_skip(gbr, 3);                         // reserved
    gst_bit_reader_get_bits_uint16(gbr, &pcrPID, 13);    // PCR PID
    GST_INFO_OBJECT(src, "PCR PID = %d (0x%02X)", pcrPID, pcrPID);
    pmtLen -= 2;
    gst_bit_reader_skip(gbr, 4);                         // reserved
    gst_bit_reader_get_bits_uint16(gbr, &pgmInfoLen, 12);// Prgm Info Len
    GST_INFO_OBJECT(src, "Program Info Len = %d", pgmInfoLen);
    pmtLen -= 2;

    bytes = (gst_bit_reader_get_remaining(gbr) / 8);
    temp = (bytes >= (pmtLen - pmtByte))? (pmtLen - pmtByte) : bytes;
    GST_INFO_OBJECT(src, "read %d bytes of PMT from %d remaining", temp, bytes);

    for (i = 0; i < temp; i++)
    {
        gst_bit_reader_get_bits_uint8(gbr, &pmt[pmtByte], 8);
        GST_INFO_OBJECT(src, "PMT[%d] = %02X", pmtByte, pmt[pmtByte]);
        pmtByte++;
    }

    if (pmtByte == pmtLen)
    {
        GST_INFO_OBJECT(src, "End of PMT[%d]", pmtLen);

        for (i = 0; i < pgmInfoLen; i++)
        {
            pgmDescriptor[i] = pmt[i];
            GST_INFO_OBJECT(src, "pgmDesc[%d] = %02X", i, pgmDescriptor[i]);
        }

        while (i < (pmtLen - 4))
        {
            stream_type = pmt[i];
            esPID = (pmt[i + 1] & 0x1f) << 8;
            esPID |= pmt[i + 2];
            esInfoLen = (pmt[i + 3] & 0x0f) << 8;
            esInfoLen |= pmt[i + 4];
            GST_INFO_OBJECT(src, "stream type = %d (0x%02x) "
                                "ES PID = %d (0x%04x) ES Info Len = %d",
                                stream_type, stream_type, esPID, esPID,
                                esInfoLen);
            if (stream_type == 0x1  ||
                stream_type == 0x2  ||
                stream_type == 0x80 ||
                stream_type == 0x1b ||
                stream_type == 0xea)
            {
                if (esPID == src->video_pid)
                {
                    GST_INFO_OBJECT(src, "Video stream type = %d (0x%02x) "
                                         "Video PID = %d (0x%04x) ",
                                         stream_type, stream_type,
                                         esPID, esPID);
                    retVal = TRUE;
                }
            }
            else if (stream_type == 0x3  ||
                     stream_type == 0x4  ||
                     stream_type == 0x6  ||
                     stream_type == 0x11 ||
                     stream_type == 0x81 ||
                     stream_type == 0x82 ||
                     stream_type == 0x83 ||
                     stream_type == 0x84 ||
                     stream_type == 0x85 ||
                     stream_type == 0x86 ||
                     stream_type == 0xa1 ||
                     stream_type == 0xa2)
            {
                if (esPID == src->audio_pid)
                {
                    GST_INFO_OBJECT(src, "Audio stream type = %d (0x%02x) "
                                         "Audio PID = %d (0x%04x) ",
                                         stream_type, stream_type,
                                         esPID, esPID);
                    retVal = TRUE;
                }
            }

            for (j = 0; j < esInfoLen; j++)
            {
                esDescriptor[j] = pmt[i + 5 + j];
                GST_INFO_OBJECT(src, "esDesc[%d] = %02X", j, esDescriptor[j]);
            }

            i += (5 + j);
        }
    }

    GST_INFO_OBJECT(src, "returning: %s", retVal? "TRUE" : "FALSE");
    return retVal;
}
#endif

static gboolean processInput(GstSptsAssembler* assembler, guint8* ppkt, int len)
{
    static guint8 audio_cc = 0;
    static guint8 video_cc = 0;
    static guint32 packet = 0;
    static guint32 pat = 0;
    static guint32 pmt = 0;
    gboolean retVal = FALSE;
    guint8 transport_error_ind;
    guint8 payload_unit_start_ind;
    guint8 adaptation_field_control;
    GstBitReader gbr;
    guint16 pid;
    guint8* pdata;
    guint8 cc;
#ifdef DEBUG_OUTPUT_PSI
    GstObject* pObj = GST_OBJECT(assembler);
#endif
    GST_LOG_OBJECT(assembler, "%s(%p, %p, %d)", __func__, assembler, ppkt, len);
    //
    // WARNING: this method is assuming big endian!
    //
    if ((len >= TS_PACKET_SIZE) && (NULL != ppkt))
    {
        pdata = ppkt;

        if (*pdata == 0x47)
        {
            pid = 0;
            packet++;
            GST_LOG_OBJECT(assembler, "SYNC detected");
            gst_bit_reader_init(&gbr, pdata, TS_PACKET_SIZE);
            gst_bit_reader_skip(&gbr, 8);   // skip the sync byte
            gst_bit_reader_get_bits_uint8(&gbr, &transport_error_ind, 1);
            gst_bit_reader_get_bits_uint8(&gbr, &payload_unit_start_ind, 1);
            gst_bit_reader_skip(&gbr, 1);   // skip the Priority
            gst_bit_reader_get_bits_uint16(&gbr, &pid, 13);
            gst_bit_reader_skip(&gbr, 2);   // skip scrambling control
            gst_bit_reader_get_bits_uint8(&gbr, &adaptation_field_control, 2);
            gst_bit_reader_get_bits_uint8(&gbr, &cc, 4);

            if (transport_error_ind)
            {
                GST_WARNING_OBJECT(assembler, "TS error detected");
            }
            else
            {
                // we have a valid packet...
                GST_LOG_OBJECT(assembler, "PID 0x%04X detected", pid);

                if (0x1FF0 == (pid & 0x1FF0)) // pass NULLs and PSI
                {
                    retVal = TRUE;
                }
                else if (PAT_PID == pid)
                {
                    pat++;
                    GST_LOG_OBJECT(assembler, "PAT %d found at packet %d",
                                   pat, packet);
#ifdef DEBUG_INPUT_PSI
                    parse_PAT(assembler, &gbr);
#endif
                    if (TRUE == assembler->rewrite_pat_pmt)
                    {
#ifdef DEBUG_OUTPUT_PSI
                        // before
                        gst_hexdump(pObj, pdata, TS_PACKET_SIZE+4);
#endif
                        rewritePAT(assembler, pdata);
                        pdata[3] = ((pdata[3] & 0xF0)|(pat & 0x0F));
                        retVal = TRUE;
#ifdef DEBUG_OUTPUT_PSI
                        // after
                        gst_hexdump(pObj, pdata, TS_PACKET_SIZE+4);
#endif
                    }
                }
                else if (assembler->pmt_pid == pid)
                {
                    pmt++;
                    GST_LOG_OBJECT(assembler, "PMT %d found at packet %d",
                                   pmt, packet);
#ifdef DEBUG_INPUT_PSI
                    parse_PMT(assembler, &gbr);
#endif
                    if (TRUE == assembler->rewrite_pat_pmt)
                    {
#ifdef DEBUG_OUTPUT_PSI
                        // before
                        gst_hexdump(pObj, pdata, TS_PACKET_SIZE+4);
#endif
                        rewritePMT(assembler, pdata);
                        pdata[3] = ((pdata[3] & 0xF0)|(pmt & 0x0F));
                        retVal = TRUE;
#ifdef DEBUG_OUTPUT_PSI
                        // after
                        gst_hexdump(pObj, pdata, TS_PACKET_SIZE+4);
#endif
                    }
                }
                else if (assembler->video_pid == pid)
                {
                    GST_LOG_OBJECT(assembler,
                                   "video 0x%X cc:%d found at packet %d",
                                   pid, cc, packet);
                    // re-write the video continuity counter...
                    pdata[3] = ((pdata[3] & 0xF0)|(video_cc++ & 0x0F));
                    retVal = TRUE;
                }
                else if (assembler->audio_pid == pid)
                {
                    GST_LOG_OBJECT(assembler,
                                   "audio 0x%X cc:%d found at packet %d",
                                   pid, cc, packet);
                    // re-write the video continuity counter...
                    pdata[3] = ((pdata[3] & 0xF0)|(audio_cc++ & 0x0F));
                    retVal = TRUE;
                }
                else
                {
                    GST_LOG_OBJECT(assembler, "Skipped PID 0x%X for pkt: %p",
                                   pid, pdata);
                }
            }
        }
        else
        {
            GST_WARNING_OBJECT(assembler, "Bad Sync 0x%X for pkt: %p, len: %d",
                               *pdata, pdata, len);
        }
    }

    return retVal;
}

/* chain function
 * this function does the actual processing
 */
static GstFlowReturn gst_spts_assembler_chain(GstPad * pad, GstBuffer * buf)
{
    static gint psi = 0;
    gint size = GST_BUFFER_SIZE(buf);
    guint8* pData = GST_BUFFER_DATA(buf);
    GstFlowReturn retVal = GST_FLOW_OK;
    GstSptsAssembler *assembler = GST_SPTSASSEMBLER(GST_OBJECT_PARENT(pad));

    // If we're inserting canned PATs and PMTs...
    if (TRUE == assembler->insert_canned_pat_pmt)
    {
        GstClockTime sysclocktime = gst_clock_get_time(assembler->sysclock);
        GstClockTimeDiff sysclockdiff =
                         GST_CLOCK_DIFF(assembler->last_psi_time, sysclocktime);
        if (sysclockdiff >= PSI_INTERVAL)
        {
            guint8* ppkt = NULL;
            psi++;
            GST_LOG_OBJECT(assembler, "psi:%d clock diff:%llu",
                                       psi, sysclockdiff);
            assembler->last_psi_time = sysclocktime;
    
            // Send a PAT
            gst_buffer_ref(assembler->pat_ts_packet);
            // re-write the PAT continuity counter...
            ppkt = GST_BUFFER_DATA(assembler->pat_ts_packet);
            ppkt[3] = ((ppkt[3] & 0xF0)|(psi & 0x0F));
            gst_pad_push(assembler->srcpad, assembler->pat_ts_packet);
    
            // Send a PMT
            gst_buffer_ref(assembler->pmt_ts_packet);
            // re-write the PMT continuity counter...
            ppkt = GST_BUFFER_DATA(assembler->pmt_ts_packet);
            ppkt[3] = ((ppkt[3] & 0xF0)|(psi & 0x0F));
            gst_pad_push(assembler->srcpad, assembler->pmt_ts_packet);
        }
    }

    if (TRUE == assembler->insert_zero_prgm_pats &&
        TRUE == assembler->send_zero_prgm_pat)
    {
        assembler->send_zero_prgm_pat = FALSE;
        gst_buffer_ref(assembler->null_ts_packet);
        gst_pad_push(assembler->srcpad, assembler->null_ts_packet);
        gst_buffer_ref(assembler->zero_prgm_pat_ts_packet);
        gst_pad_push(assembler->srcpad, assembler->zero_prgm_pat_ts_packet);
        gst_buffer_ref(assembler->null_ts_packet);
        gst_pad_push(assembler->srcpad, assembler->null_ts_packet);
        GST_INFO_OBJECT(assembler, "%s sent zero_prgm_pat_ts_packet", __func__);
    }

    // we are not guaranteed to get TS_PACKET_SIZE buffers, loop through what
    // we are given and output TS_PACKET_SIZE buffers
    while (size >= TS_PACKET_SIZE && retVal == GST_FLOW_OK)
    {
        if (TRUE == processInput(assembler, pData, TS_PACKET_SIZE))
        {
            GstBuffer* pBuf = gst_buffer_new_and_alloc(TS_PACKET_SIZE);
            memcpy(GST_BUFFER_DATA(pBuf), pData, TS_PACKET_SIZE);
#ifdef DEBUG_PUSH
            if (TRUE == assembler->rewrite_pat_pmt)
            {
                guint16 pid = (pData[1] & 0x1f) << 8;
                pid |= pData[2];
                GST_INFO_OBJECT(assembler,
                    "push pkt with PID[0x%04X], TID[0x%02X]", pid, pData[5]);
            }
#endif
            retVal = gst_pad_push(assembler->srcpad, pBuf);
        }

        size -= TS_PACKET_SIZE;
        pData += TS_PACKET_SIZE;
    }

    gst_buffer_unref(buf);
    return retVal;
}

void gst_spts_assembler_finalize(GObject * object)
{
    GstSptsAssembler *assembler = GST_SPTSASSEMBLER(object);
    g_mutex_free(assembler->props_lock);
}

