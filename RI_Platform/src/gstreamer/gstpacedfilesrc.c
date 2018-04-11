/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2012 CableLabs
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
 * SECTION:element-pacedfilesrc
 *
 * FIXME:Describe pacedfilesrc here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m pacedfilesrc ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <string.h> // memcpy
#include <sys/stat.h>
#include <gst/gst.h>
#include <gst/base/gstbitreader.h>

#include "gstesassembler.h"
#include "gstpacedfilesrc.h"
#include "gstsptsfilesrc.h"

// CACHE_TUNER_FILE enables reading the whole file into memory as an
// optimization, undef this to read chunks directly from the file rather
// than the RAM CACHE.
#define CACHE_TUNER_FILE

// yield the CPU if our read loop is less than 500000 uSeconds
#define MIN_READ_DURATION 500000

GST_DEBUG_CATEGORY_STATIC(gst_paced_file_src_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_paced_file_src_debug

enum
{
    PROP_0,
    PROP_URI,
    PROP_LOCATION,
    PROP_BLKSIZE,
    PROP_PIDLIST,
    PROP_LOOP,
    PROP_INSERT_0_PRGM_PATS,
    PROP_PCR_PACING,
    PROP_TUNER_PID_FILTERING,
    PROP_PMTPIDLIST,
    PROP_REWRITE_PCR_AND_CC,
    PROP_LAST,
};

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
                                            GST_PAD_SRC,
                                            GST_PAD_ALWAYS,
                                            GST_STATIC_CAPS_ANY);

// Forward declarations
static void gst_paced_file_src_uri_handler_init(gpointer, gpointer);
static void gst_paced_file_src_dispose(GObject* object);
static void gst_paced_file_src_finalize(GObject* object);

static void gst_paced_file_src_set_property(GObject* object,
                                            guint prop_id,
                                            const GValue* value,
                                            GParamSpec* pspec);
static void gst_paced_file_src_get_property(GObject* object,
                                            guint prop_id,
                                            GValue* value,
                                            GParamSpec* pspec);

static gboolean gst_paced_file_src_start(GstBaseSrc* basesrc);
static gboolean gst_paced_file_src_stop(GstBaseSrc* basesrc);

static GstFlowReturn gst_paced_file_src_create(GstPushSrc* src,
                                               GstBuffer** buf);

static gboolean gst_paced_file_src_open(GstPacedFileSrc* src);
static void gst_paced_file_src_close(GstPacedFileSrc* src);
static void gst_paced_file_src_set_pidlist(GstPacedFileSrc* src, gchar *pids);
static guint gst_paced_file_src_hexPIDstr_to_int(const gchar *val);

static void
_do_init(GType type)
{
    static const GInterfaceInfo urihandler_info =
    {
        gst_paced_file_src_uri_handler_init,
        NULL,
        NULL
    };

    g_type_add_interface_static(type, GST_TYPE_URI_HANDLER, &urihandler_info);
    GST_DEBUG_CATEGORY_INIT(gst_paced_file_src_debug, "pacedfilesrc",
            0, "GST_TYPE_PUSH_SRC pacedfilesrc");
}

/*lint -e(123)*/GST_BOILERPLATE_FULL(GstPacedFileSrc, gst_paced_file_src,
                                     GstPushSrc, GST_TYPE_PUSH_SRC, _do_init)

//
//
//
// INTERNAL IMPLEMENTATION
//
//
//

/********************************************/
/**********                        **********/
/********** GObject IMPLEMENTATION **********/
/**********                        **********/
/********************************************/

static void gst_paced_file_src_base_init(gpointer gclass)
{
    GstElementClass* element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "PacedFileSrc",
            "FIXME:Generic", "FIXME:Generic Template Element",
            " <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
}

/* initialize the pacedfilesrc's class */
static void gst_paced_file_src_class_init(GstPacedFileSrcClass* klass)
{
    GObjectClass* gobject_class;
    GstBaseSrcClass* gstbasesrc_class;
    GstPushSrcClass* gstpushsrc_class;


    gobject_class = (GObjectClass *) klass;
    gstbasesrc_class = (GstBaseSrcClass *) klass;
    gstpushsrc_class = (GstPushSrcClass *) klass;

    parent_class = g_type_class_peek_parent(klass);

    gobject_class->set_property = gst_paced_file_src_set_property;
    gobject_class->get_property = gst_paced_file_src_get_property;

    g_object_class_install_property(gobject_class, PROP_URI,
            g_param_spec_string("uri", "URI",
                    "URI in the form of file://location", DEFAULT_FILEPATH,
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_LOCATION,
            g_param_spec_string("location", "location of MPEG file",
                    "Location of MPEG file", DEFAULT_FILENAME,
                    G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property(gobject_class, PROP_BLKSIZE,
            g_param_spec_uint("blksize", "Generated GstBuffer size",
                    "Size in bytes to read per buffer", MIN_BLKSIZE,
                    MAX_BLKSIZE, DEFAULT_BLKSIZE, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_PIDLIST,
            g_param_spec_string("pidlist", "PIDList",
                    "List of included PIDs as 0xnnnn 0xnnnn", "0x0000",
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_LOOP,
            g_param_spec_uint("loop", "Number of times to loop 0 = forever",
                    "Number of times to loop 0 = forever", 0,
                    65535, 0, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_PCR_PACING,
            g_param_spec_uint("pcr_pacing", "pace output on PCR flag",
                    "TRUE = pace output on PCR, FALSE = bitrate-only pacing", 0,
                    1, 0, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_TUNER_PID_FILTERING,
            g_param_spec_uint("tuner_pid_filtering", "filter on PIDs flag",
                    "TRUE = filter on PIDs FALSE = pass PIDs", 0,
                    1, 0, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_PMTPIDLIST,
            g_param_spec_string("pmtpidlist", "PMT PID List",
                    "List of PMT PIDs as 0xnnnn=P1, 0xnnnn=P2", "0x0000=P0",
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_REWRITE_PCR_AND_CC,
            g_param_spec_uint("rewrite_pcr_and_cc", "rewrite PCR and CC flag",
                    "TRUE = rewrite PCR and CC, "
                    "FALSE = pass original PCR and CC", 0,
                    1, 0, G_PARAM_READWRITE));

    gobject_class->dispose = GST_DEBUG_FUNCPTR(gst_paced_file_src_dispose);
    gobject_class->finalize = GST_DEBUG_FUNCPTR(gst_paced_file_src_finalize);
    gstbasesrc_class->start = GST_DEBUG_FUNCPTR(gst_paced_file_src_start);
    gstbasesrc_class->stop = GST_DEBUG_FUNCPTR(gst_paced_file_src_stop);
    gstpushsrc_class->create = GST_DEBUG_FUNCPTR(gst_paced_file_src_create);
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_paced_file_src_init(GstPacedFileSrc* src,
                                    GstPacedFileSrcClass* gclass)
{
    int i = 0;

    src->props_lock = g_mutex_new();

    src->fp = NULL;
    src->uri = DEFAULT_FILEPATH;
    src->location = DEFAULT_FILENAME;
    src->blksize = DEFAULT_BLKSIZE;
    src->current_time = GST_CLOCK_TIME_NONE;
    src->curr_tsid = 0;
    src->filebufp = NULL;
    src->null_ts_packet = gst_buffer_new_and_alloc(TS_PACKET_SIZE);
    src->loop = 0;

    // Initialize NULL TS packet.
    GST_BUFFER_DATA(src->null_ts_packet)[0] = 0x47;
    GST_BUFFER_DATA(src->null_ts_packet)[1] = 0x1F;
    GST_BUFFER_DATA(src->null_ts_packet)[2] = 0xFF;
    GST_BUFFER_DATA(src->null_ts_packet)[3] = 0x1F;

    for (i = 4; i < TS_PACKET_SIZE; i++)
    {
        GST_BUFFER_DATA(src->null_ts_packet)[i] = 0xFF;
    }

    GST_BUFFER_SIZE(src->null_ts_packet) = TS_PACKET_SIZE;
    gst_buffer_set_caps(src->null_ts_packet, GST_PAD_CAPS(GST_BASE_SRC_PAD(src)));

    gst_base_src_set_live(GST_BASE_SRC(src), TRUE);
    gst_base_src_set_format(GST_BASE_SRC(src), GST_FORMAT_TIME);
    gst_base_src_set_do_timestamp(GST_BASE_SRC(src), TRUE);
}

static void gst_paced_file_src_dispose(GObject* object)
{
    G_OBJECT_CLASS(parent_class)->dispose(object);
}

static void gst_paced_file_src_finalize(GObject* object)
{
    GstPacedFileSrc* src = GST_PACEDFILESRC(object);

    g_mutex_free(src->props_lock);
    g_free(src->uri);
    g_free(src->location);

    G_OBJECT_CLASS(parent_class)->finalize(object);
}

static void parse_PAT(GstPacedFileSrc* src, GstBitReader *gbr)
{
    gint i = 0;
    guint8 temp = 0;
    guint8 pat[1024] = {0};
    guint16 pmtPID = 0;
    guint16 program = 0;
    guint16 tsid = 0;
    guint16 bytes = 0;
    guint16 patLen = 0;
    guint16 patByte = 0;

    GST_LOG_OBJECT(src, "%s", __func__);
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
    src->curr_tsid = tsid;
    GST_LOG_OBJECT(src, "TSID = %X", src->curr_tsid);
    patLen -= 2;
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // RES/VER/CNI
    GST_LOG_OBJECT(src, "RES %02X", temp & 0xC0);
    GST_LOG_OBJECT(src, "VER %02X", (temp & 0x3E) >> 1);
    GST_LOG_OBJECT(src, "CNI %02X", temp & 0x01);
    patLen -= 1;
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
    GST_LOG_OBJECT(src, "PAT Section number = %d", temp);
    patLen -= 1;
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
    GST_LOG_OBJECT(src, "PAT Last Section number = %d", temp);
    patLen -= 1;
    bytes = (gst_bit_reader_get_remaining(gbr) / 8);
    temp = (bytes >= (patLen - patByte))? (patLen - patByte) : bytes;
    GST_LOG_OBJECT(src, "read %d bytes of PAT from %d remaining",
                   temp, bytes);

    for (i = 0; i < temp; i++)
    {
        gst_bit_reader_get_bits_uint8(gbr, &pat[patByte], 8);
        GST_LOG_OBJECT(src, "PAT[%d] = %02X", patByte, pat[patByte]);
        patByte++;
    }

    if (patByte == patLen)
    {
        gchar* p = src->pmtpidlist;
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
            GST_LOG_OBJECT(src, "Program Number = %d (0x%04x) "
                                 "PMT PID = %d (0x%04x)",
                                 program, program, pmtPID, pmtPID);
            if ((p - src->pmtpidlist) < (PIDLIST_SIZE - PIDFORMAT_SIZE))
            {
                // NOTE: the format of this string must match update_preproc()
                p += sprintf(p, "0x%4.4X=P%d ", pmtPID, program);
            }
            else
            {
                GST_ERROR_OBJECT(src, "%s 0x%4.4X=P%d wouldn't fit into list",
                                     __func__, pmtPID, program);
            }
        }
    }
}

static gboolean
gst_paced_file_src_set_location(GstPacedFileSrc* src, const gchar* location)
{
    gboolean retVal = FALSE;
    g_mutex_lock(src->props_lock);

    if(NULL == src->location || 0 != strcmp(src->location, location))
    {
        gst_paced_file_src_close(src);
        g_free(src->location);
        src->location = g_strdup(location);
        GST_DEBUG_OBJECT(src, "New location set: \"%s\".", src->location);
        retVal = gst_paced_file_src_open(src);
    }

    g_mutex_unlock(src->props_lock);
    return retVal;
}

static gboolean
gst_paced_file_src_set_uri(GstPacedFileSrc* src, const gchar* uri)
{
    gboolean retVal = FALSE;
    gchar *location = NULL;
    gchar *protocol = gst_uri_get_protocol(uri);

    if (NULL != protocol)
    {
        if (strcmp(protocol, "file") == 0)
        {
            if (NULL != (location = gst_uri_get_location(uri)))
            {
                g_mutex_lock(src->props_lock);

                if(NULL == src->uri || 0 != strcmp(src->uri, uri))
                {
                    g_free(src->uri);
                    src->uri = g_strdup(uri);
                    GST_DEBUG_OBJECT(src, "New URI set: \"%s\".", src->uri);
                }

                g_mutex_unlock(src->props_lock);
                retVal = gst_paced_file_src_set_location(src, location);
                g_free(location);
            }
        }
        else
        {
            GST_ELEMENT_ERROR(src, RESOURCE, READ, (NULL),
                ("error parsing URI %s: %s != file", uri, protocol));
        }

        g_free(protocol);
    }

    return retVal;
}

static void gst_paced_file_src_set_property(GObject* object,
                                            guint prop_id,
                                            const GValue* value,
                                            GParamSpec* pspec)
{
    GstPacedFileSrc* src = GST_PACEDFILESRC(object);

    switch (prop_id)
    {
    case PROP_URI:
        (void)gst_paced_file_src_set_uri(src, g_value_get_string(value));
        break;
    case PROP_LOCATION:
        (void)gst_paced_file_src_set_location(src, g_value_get_string(value));
        break;
    case PROP_BLKSIZE:
        g_mutex_lock(src->props_lock);
        src->blksize = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "New blksize set: %u.", src->blksize);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PIDLIST:
        g_mutex_lock(src->props_lock);
        gst_paced_file_src_set_pidlist(src, (gchar*)g_value_get_string(value));
        if (src->tuner_pid_filtering)
        {
            GST_DEBUG_OBJECT(src, "New pidlist set: %s.", src->pidlist);
        }
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_LOOP:
        g_mutex_lock(src->props_lock);
        src->loop = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "will loop %u times.", src->loop);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PCR_PACING:
        g_mutex_lock(src->props_lock);
        src->pcr_pacing = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "will pace on%sbitrate",
                        src->pcr_pacing? " PCR and " : "ly on ");
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_TUNER_PID_FILTERING:
        g_mutex_lock(src->props_lock);
        src->tuner_pid_filtering = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "will pass %s PIDs",
                        src->tuner_pid_filtering? "filtered" : "all");
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PMTPIDLIST:
        GST_ERROR_OBJECT(src, "pmtpidlist is read only!");
        break;
    case PROP_REWRITE_PCR_AND_CC:
        g_mutex_lock(src->props_lock);
        src->rewrite_pcr_and_cc = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "will%sre-write PCR and continuity counter",
                        src->rewrite_pcr_and_cc? " " : " not ");
        g_mutex_unlock(src->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_paced_file_src_get_property(GObject* object,
                                            guint prop_id,
                                            GValue* value,
                                            GParamSpec* pspec)
{
    GstPacedFileSrc* src = GST_PACEDFILESRC(object);

    switch (prop_id)
    {
    case PROP_URI:
        g_mutex_lock(src->props_lock);
        g_value_set_string(value, src->uri);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_LOCATION:
        g_mutex_lock(src->props_lock);
        g_value_set_string(value, src->location);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_BLKSIZE:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->blksize);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PIDLIST:
        g_mutex_lock(src->props_lock);
        g_value_set_string(value, src->pidlist);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_LOOP:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->loop);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PCR_PACING:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->pcr_pacing);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_TUNER_PID_FILTERING:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->tuner_pid_filtering);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PMTPIDLIST:
        g_mutex_lock(src->props_lock);
        GST_INFO_OBJECT(src, "returning: %s", src->pmtpidlist);
        g_value_set_string(value, src->pmtpidlist);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_REWRITE_PCR_AND_CC:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->rewrite_pcr_and_cc);
        g_mutex_unlock(src->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/***********************************************/
/**********                           **********/
/********** GstBaseSrc IMPLEMENTATION **********/
/**********                           **********/
/***********************************************/

/* start processing, ideal for opening the resource */
static gboolean gst_paced_file_src_start(GstBaseSrc* basesrc)
{
    GstPacedFileSrc* src = GST_PACEDFILESRC(basesrc);
    gboolean retval = TRUE;

    src->current_time = 0;

    GST_INFO_OBJECT(src, "returning %s", retval? "TRUE" : "FALSE");
    return retval;
}

/* stop processing, ideal for closing the resource */
static gboolean gst_paced_file_src_stop(GstBaseSrc * basesrc)
{
    GstPacedFileSrc* src = GST_PACEDFILESRC(basesrc);
    gboolean retval = TRUE;

    gst_paced_file_src_close(src);

    src->current_time = 0;
    src->filebufp = NULL;

    GST_INFO_OBJECT(src, "returning %s", retval? "TRUE" : "FALSE");
    return retval;
}

/****************************************************/
/**********                                **********/
/********** GstPacedFileSrc IMPLEMENTATION **********/
/**********                                **********/
/****************************************************/

/* Should be called with prop_lock held. */
static gboolean gst_paced_file_src_open(GstPacedFileSrc* src)
{
    if (src->location == NULL || strncmp(src->location, "", 1) == 0)
    {
        return FALSE;
    }

    // strip off any CR LF at the end of the file location
    int len = strlen(src->location);
    len -= 1;   // look at last char

    while (('\r' == src->location[len]) || ('\n' == src->location[len]))
    {
        src->location[len] = 0;
        len -= 1;   // look at last char
    }

    GST_INFO_OBJECT(src, "opening %s", (src->location)? src->location:"NULL");

    /* open the file */
    if (NULL == (src->fp = fopen(src->location, "rb")))
    {
        GST_ERROR_OBJECT(src, "Problem opening file \"%s\": fopen returned %d",
                        (src->location) ? src->location : "NULL", errno);
        return FALSE;
    }

    fseek(src->fp, 0, SEEK_END);
    len = ftell(src->fp);
    GST_INFO_OBJECT(src, "\"%s\" is %d bytes", src->location, len);

    if (0 >= len)
    {
        GST_ERROR_OBJECT(src, "file \"%s\": has len %d!?", src->location, len);
        return FALSE;
    }

    fseek(src->fp, 0, SEEK_SET);
    src->sysclock = gst_system_clock_obtain();
    src->last_clock_time = gst_clock_get_time(src->sysclock);
    src->bytes_sent = 0;
    src->pacing_pid = 0;
    src->current_time = 0;
    src->pmtpidlist[0] = 0;
#ifdef CACHE_TUNER_FILE
    src->filebuf = g_try_malloc(len);

    if (NULL != src->filebuf)
    {
        src->filebufsz = fread(src->filebuf, 1, len, src->fp);
        src->filebufp = src->filebuf;   // point to the beginning of the buffer
    }
    else
    {
        // TODO: deal with low memory situation...
        GST_ERROR_OBJECT(src, "file \"%s\" len %d too big", src->location, len);
        src->filebufsz = 0;
        src->filebuf = NULL;
        src->filebufp = NULL;
    }

    fclose(src->fp);
    src->fp = NULL;
#else
    src->filebufp = NULL;
#endif
    return TRUE;
}

static void gst_paced_file_src_close(GstPacedFileSrc* src)
{
#ifdef CACHE_TUNER_FILE
    if (NULL != src->filebufp)
    {
        if (NULL != src->filebuf)
        {
            GST_INFO_OBJECT(src, "releasing \"%s\"",
                           (src->location)? src->location:"NULL");
            g_free(src->filebuf);
        }
#else
    if (NULL != src->fp)
    {
        GST_INFO_OBJECT(src, "closing \"%s\"",
                       (src->location)? src->location:"NULL");
        fclose(src->fp);
        src->fp = NULL;
#endif
        gst_object_unref(src->sysclock);
        src->current_time = 0;
        src->filebufp = NULL;
    }
}

// convert a hex string describing a PID to an unsigned int
static guint gst_paced_file_src_hexPIDstr_to_int(const gchar *val)
{
    gchar *endptr;
    gint64 pid64;

    if (g_str_has_prefix(val, "0X"))
    {
        pid64 = g_ascii_strtoll(&(val[2]), &endptr, 16);

        if (endptr == &(val[2]))
        {
            pid64 = 0xFFFFFFFF;
        }
    }
    else
    {
        pid64 = g_ascii_strtoll(val, &endptr, 10);

        if (endptr == val)
        {
            pid64 = 0xFFFFFFFF;
        }
    }

    //g_printf("Converted to value %lld \n", pid64);

    /* Ignore invalid PID strings */
    if (endptr != val)
    {
        if (pid64 <= TS_MAX_PIDS)
        {
            return (guint) pid64;
        }
    }

    return 0xFFFFFFFF;
}

static void gst_paced_file_src_set_pidlist(GstPacedFileSrc* src, gchar *pids)
{
    gchar *upperpids;
    gchar **p_pids;
    guint pid;
    gint i;

    /* always pass the PAT PID */
    SETPIDBIT(0);
    
    if (pids != NULL)
    {
        //g_printf("pacedfilesrc: provided with pidlist=%s\n", pids);

        /* clear out old pidlist string */
        if (src->pidlist)
        {
            g_free(src->pidlist);
            src->pidlist = NULL;
        }

        /* clear out old pidlist bitfield */
        memset(src->PIDbitfield, 0, sizeof(src->PIDbitfield));
        /* Make a copy of the passed string */
        src->pidlist = g_strdup(pids);
        /* Convert the string to all upper case*/
        upperpids = g_ascii_strup(src->pidlist, -1);
        /* Break up the string based on space delimiters */
        p_pids = g_strsplit(upperpids, " ", 0);

        /* Cycle through the list of text pids */
        i = 0;

        while (p_pids[i] != NULL)
        {
            //g_printf("Processing PID string[%d] : %s\n", i, p_pids[i]);
            pid = gst_paced_file_src_hexPIDstr_to_int(p_pids[i]);

            if (pid <= TS_MAX_PIDS)
            {
                GST_LOG_OBJECT(src, "activate pid 0x%4.4X", pid);
                SETPIDBIT(pid);
            }

            i++;
        }

        /* Free the upper and tokenized list. No need to check if NULL */
        g_free(upperpids);
        g_strfreev(p_pids);
    }
}

guint8 pace_from_PCR(GstPacedFileSrc* src, GstBitReader *gbr, guint16 pid)
{
    static guint64 last_PCR;
    guint64 temp64;
    guint64 PCR = 0;
    guint64 delta = 0;
    guint8 af_len;
    guint8 pcrf = 0;

    //
    // Assumes gbr is pointing to start of AF, and AF is present
    // Should never have been called without enough data to consume/process AF
    //
    gst_bit_reader_get_bits_uint8(gbr, &af_len, 8);

    if (0 == af_len)    // no AF, just pass it...
    {
        return af_len;
    }

    gst_bit_reader_skip(gbr, 3);    // skip the beginning AF flags to PCRF
    gst_bit_reader_get_bits_uint8(gbr, &pcrf, 1);        // PCR_flag

    if (pcrf)
    {
        GstClockTime sysclocktime = 0;
        GstClockTimeDiff sysclockdiff = 0;

        if (src->pacing_pid == 0)
        {
            src->pacing_pid = pid;
            GST_INFO_OBJECT(src, "will pace on PID 0x%04X PCR", pid);
        }
        else if (pid != src->pacing_pid)
        {
            return 0;
        }

        gst_bit_reader_skip(gbr, 4);    // skip the remaining AF flags
        sysclocktime = gst_clock_get_time(src->sysclock);
        sysclockdiff = GST_CLOCK_DIFF(src->current_time, sysclocktime);
        src->current_time = sysclocktime;
        //GST_LOG_OBJECT(src, "clock diff: %llu", sysclockdiff);

        // read in the PCR (across 6 bytes)
        gst_bit_reader_get_bits_uint64(gbr, &temp64, 8); // PCR[32-25]
        PCR = temp64 << 8;                               // shift for next 8
        gst_bit_reader_get_bits_uint64(gbr, &temp64, 8); // PCR[24-17]
        PCR |= temp64;                                   // add in low 8
        PCR <<= 8;                                       // shift for next 8
        gst_bit_reader_get_bits_uint64(gbr, &temp64, 8); // PCR[16-9]
        PCR |= temp64;                                   // add in low 8
        PCR <<= 8;                                       // shift for next 8
        gst_bit_reader_get_bits_uint64(gbr, &temp64, 8); // PCR[8-1]
        PCR |= temp64;                                   // add in low 8
        PCR <<= 1;                                       // shift for next 1
        gst_bit_reader_get_bits_uint64(gbr, &temp64, 1); // PCR[0]
        PCR |= temp64;                                   // 90kHz PCR complete!

        if (0 == src->begPCR)
        {
            src->begPCR = PCR;
        }

        if (PCR > last_PCR)
        {
            delta = (PCR - last_PCR);
        }
        else
        {
            src->endPCR = last_PCR;
            src->duration += (src->endPCR - src->begPCR);
            GST_DEBUG_OBJECT(src, "duration %llu of stream on PID 0x%04X",
                            src->duration, pid);
            temp64 = 0x200000000LL; // 33 bits + 1
            delta = (PCR + (temp64 - last_PCR));
        }

        src->curPCR = last_PCR = PCR;
        //GST_LOG_OBJECT(src, "PCR %llu detected on PID 0x%04X", PCR, pid);
        //GST_LOG_OBJECT(src, "PID:%04X, PCR delta:%llu", pid, delta);
        delta = ((delta * NS_PER_90KHZ_TICK_X256) >> 8);
        //GST_DEBUG_OBJECT(src, "sysclockdiff:%llu delta:%llu",
        //                 sysclockdiff, delta);

        if (sysclockdiff < delta)
        {
            // remove realtime diff and sleep for no more than 25000 uSecs
            delta -= sysclockdiff;
            delta = GST_TIME_AS_USECONDS(delta);
            //GST_DEBUG_OBJECT(src, "PCR corrective sleep:%llu", delta);
            g_usleep((delta > 25000)? 25000 : delta);
        }
    }

    return af_len;
}

gboolean performPacing(GstPacedFileSrc* src, guint8* pdata, int len)
{
    static guint32 packet = 0;
    static guint8 last_cc = 255;
    gboolean retVal = FALSE;
    guint8 transport_error_indicator;
    guint8 adaptation_field_control;
    GstBitReader gbr;
    guint16 pid;
    guint8 cc;

    //
    // WARNING: this method is assuming big endian!
    //
    while ((len >= TS_PACKET_SIZE) && (NULL != pdata))
    {
        if (*pdata == 0x47)
        {
            packet++;
            gst_bit_reader_init(&gbr, pdata, TS_PACKET_SIZE);
            gst_bit_reader_skip(&gbr, 8);   // skip the sync byte
            gst_bit_reader_get_bits_uint8(&gbr, &transport_error_indicator, 1);
            gst_bit_reader_skip(&gbr, 2);   // skip the PUSI and Priority
            gst_bit_reader_get_bits_uint16(&gbr, &pid, 13);
            gst_bit_reader_skip(&gbr, 2);   // skip scrambling control
            gst_bit_reader_get_bits_uint8(&gbr, &adaptation_field_control, 2);
            gst_bit_reader_get_bits_uint8(&gbr, &cc, 4);

            if (transport_error_indicator)
            {
                GST_WARNING_OBJECT(src, "TS error detected");
            }
            else if ((FALSE == src->tuner_pid_filtering) || TESTPIDBIT(pid))
            {
                retVal = TRUE;

                if (0 == pid)
                {
                    parse_PAT(src, &gbr);
                }
                else
                {
                    GST_LOG_OBJECT(src, "passed TS packet for PID %X", pid);

                    if (adaptation_field_control & 0x01)
                    {
                       if (src->pacing_pid == pid)
                       {
                           last_cc = (255==last_cc? cc : ((last_cc+1) & 0x0F));

                           if (last_cc != cc)
                           {
                               GST_LOG_OBJECT(src,
                                              "discont. at pkt %d (%d != %d)",
                                              packet, last_cc, cc);

                               if (TRUE == src->rewrite_pcr_and_cc)
                               {
                                   // re-write the continuity counter...
                                   pdata[3] = ((pdata[3]&0xF0)|(last_cc&0x0F));
                               }
                           }
                       }
                    }

                    if (adaptation_field_control & 0x02)
                    {
                        //gst_bit_reader_skip(&gbr, 4);   // point to AF_Len
                        guint8 afLen = pace_from_PCR(src, &gbr, pid);

                        if ((0 != src->duration) && (0 != afLen))
                        {
                            // re-write the AF PCR (loop -> continuous)
                            GST_LOG_OBJECT(src, "rewrite PCR: AF(%X) len:%d",
                                           adaptation_field_control, afLen);
                            pdata += 4;         // skip the TS header

                            if (*pdata != afLen)
                            {
                                GST_WARNING_OBJECT(src, "pdata:%d != len:%d",
                                               *pdata, afLen);
                            }

                            pdata += 1;         // skip over the AF length

                            if ((0x10 == (*pdata & 0x10)) &&
                               (TRUE == src->rewrite_pcr_and_cc))
                            {
                                pdata += 1;     // skip over the AF flags
                                src->curPCR += src->duration;
                                *pdata = (src->curPCR >> ((4 * 8) +1)) & 0xFF;
                                pdata += 1;
                                *pdata = (src->curPCR >> ((3 * 8) +1)) & 0xFF;
                                pdata += 1;
                                *pdata = (src->curPCR >> ((2 * 8) +1)) & 0xFF;
                                pdata += 1;
                                *pdata = (src->curPCR >> ((1 * 8) +1)) & 0xFF;
                                pdata += 1;
                                *pdata = (*pdata & 0x7F)|(src->curPCR & 0x80);
                            }
                        }
                    }
                }
            }
            else
            {
                GST_LOG_OBJECT(src, "replace TS packet (PID %X) w/NULL", pid);
                g_usleep(100);
                memcpy(pdata, GST_BUFFER_DATA(src->null_ts_packet),
                       TS_PACKET_SIZE);
                retVal = TRUE;
            }
        }
        else
        {
            GST_WARNING_OBJECT(src, "Bad TS Sync (0x%X) in pkt:%p, len: %d!?",
                               *pdata, pdata, len);
            break;
        }

        pdata += TS_PACKET_SIZE;
        len -= TS_PACKET_SIZE;
    }

    return retVal;
}

static GstFlowReturn gst_paced_file_src_create(GstPushSrc* pushsrc,
                                               GstBuffer** bufp)
{
    g_assert(pushsrc != NULL);
    GstPacedFileSrc* src = GST_PACEDFILESRC(pushsrc);
    GstFlowReturn retval = GST_FLOW_OK;
    GstBuffer *buf = NULL;
    guint8 *pBufData = NULL;
    int max_tries = (2 * MAX_BLKSIZE);
    int bytes = 0;
    GstClockTime sysclocktime = 0;
    GstClockTime end = 0;
    GstClockTimeDiff clock_diff = 0;

    // return the NULL TS packet by default
    *bufp = src->null_ts_packet;
    g_mutex_lock(src->props_lock);

    // We need to yield the CPU while we wait for a file to be available
    // (or to be "tuned")
#ifdef CACHE_TUNER_FILE
    while (NULL == src->filebufp)
#else
    while (NULL == src->fp)
#endif
    {
        g_mutex_unlock(src->props_lock);
        g_usleep(1000);
        g_mutex_lock(src->props_lock);
    }

    buf = gst_buffer_new_and_alloc(src->blksize);
    pBufData = GST_BUFFER_DATA(buf);
    sysclocktime = gst_clock_get_time(src->sysclock);

    do
    {
        // read a packet from the file into the buffer...
#ifdef CACHE_TUNER_FILE
        bytes = TS_PACKET_SIZE;

        if ((src->filebufsz - (src->filebufp - src->filebuf)) >= bytes)
        {
            memcpy(pBufData, src->filebufp, bytes);
            src->filebufp += bytes;
        }
        else
        {
            bytes = (src->filebufsz - (src->filebufp - src->filebuf));
        }
#else
        bytes = fread(pBufData, 1, TS_PACKET_SIZE, src->fp);
#endif

        if (bytes < 0)
        {
            GST_WARNING_OBJECT(src, "file read error? (%d)", bytes);
            retval = GST_FLOW_ERROR;
            break;
        }
        else if (bytes < TS_PACKET_SIZE)
        {
            if (src->loop == 0)
            {
                GST_DEBUG_OBJECT(src, "%d = fread() - looping...", bytes);
            }
            else if (src->loop >= 1)
            {
                if (--src->loop == 0)
                {
                    GST_INFO_OBJECT(src, "exiting - last loop");
                    retval = GST_FLOW_RESEND;
                }
                else
                {
                    GST_INFO_OBJECT(src, "%d = fread() - loop %d",
                                     bytes, src->loop);
                }
            }

            // Loop back to the beginning of the content...
#ifdef CACHE_TUNER_FILE
            src->filebufp = src->filebuf;
#else
            fseek(src->fp, 0, SEEK_SET);
#endif
            // yield the CPU on each loop to prevent short files from hogging
            end = gst_clock_get_time(src->sysclock);
            clock_diff = GST_CLOCK_DIFF(sysclocktime, end);

            if (GST_TIME_AS_USECONDS(clock_diff) < MIN_READ_DURATION)
            {
                g_usleep(MIN_READ_DURATION - GST_TIME_AS_USECONDS(clock_diff));
            }

            continue;
        }

        if ((FALSE == src->pcr_pacing) || performPacing(src, pBufData, bytes))
        {
            GST_LOG_OBJECT(src, "add %d bytes at %p", bytes, pBufData);
        }

        pBufData += bytes;   // point to the next pkt position in buf

        if ((pBufData - GST_BUFFER_DATA(buf)) >= src->blksize)
        {
            end = gst_clock_get_time(src->sysclock);
            clock_diff = GST_CLOCK_DIFF(sysclocktime, end);

            // generate the bit rate as a function of block size
            if (GST_TIME_AS_USECONDS(clock_diff) < BLKSIZE_CLOCKS)
            {
                g_usleep(BLKSIZE_CLOCKS - GST_TIME_AS_USECONDS(clock_diff));
            }

            *bufp = buf;
            break;
        }

    } while (--max_tries);

    if (*bufp == src->null_ts_packet)
    {
        gst_buffer_ref(*bufp);
        GST_LOG_OBJECT(src, "%s - sending null_ts_packet...", __func__);
    }
    else
    {
        GST_LOG_OBJECT(src, "Push buffer %p, size %u", buf, bytes);
        GST_BUFFER_SIZE(buf) = src->blksize;
        gst_buffer_set_caps(buf, GST_PAD_CAPS(GST_BASE_SRC_PAD(src)));
    }

    src->bytes_sent += GST_BUFFER_SIZE(*bufp);
    sysclocktime = gst_clock_get_time(src->sysclock);
    src->clock_diff += GST_CLOCK_DIFF(src->last_clock_time, sysclocktime);
    src->last_clock_time = sysclocktime;

    if (src->clock_diff >= 1000000000)
    {
        gint64 bps = ((src->bytes_sent << 3) / (src->clock_diff / 1000000000));
        GST_DEBUG_OBJECT(src, "diff: %llu, bps: %lld", src->clock_diff, bps);
        src->clock_diff = 0;
        src->bytes_sent = 0;
    }

    g_mutex_unlock(src->props_lock);
    return retval;
}


/*********************************************/
/**********                         **********/
/********** GstUriHandler INTERFACE **********/
/**********                         **********/
/*********************************************/

static GstURIType
gst_paced_file_src_uri_get_type(void)
{
    return GST_URI_SRC;
}

static gchar **
gst_paced_file_src_uri_get_protocols(void)
{
    static gchar *protocols[] = { "file", NULL };
    return protocols;
}

static const gchar *
gst_paced_file_src_uri_get_uri(GstURIHandler* handler)
{
    GstPacedFileSrc* src = GST_PACEDFILESRC(handler);
    return src->uri;
}

static gboolean
gst_paced_file_src_uri_set_uri(GstURIHandler* handler, const gchar* uri)
{
    GstPacedFileSrc* src = GST_PACEDFILESRC(handler);
    return gst_paced_file_src_set_uri(src, uri);
}

static void
gst_paced_file_src_uri_handler_init(gpointer g_iface, gpointer iface_data)
{
  GstURIHandlerInterface *iface = (GstURIHandlerInterface *)g_iface;

  iface->get_type = gst_paced_file_src_uri_get_type;
  iface->get_protocols = gst_paced_file_src_uri_get_protocols;
  iface->get_uri = gst_paced_file_src_uri_get_uri;
  iface->set_uri = gst_paced_file_src_uri_set_uri;
}

