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
 * SECTION:element-sptsfilesrc
 *
 * FIXME:Describe sptsfilesrc here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m sptsfilesrc ! fakesink silent=TRUE
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
#include "gstsptsfilesrc.h"

GST_DEBUG_CATEGORY_STATIC(gst_spts_file_src_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_spts_file_src_debug

guint8 payload_unit_start_ind = 0;

enum
{
    PROP_0,
    PROP_URI,
    PROP_LOCATION,
    PROP_BLKSIZE,
    PROP_PROGRAM,
    PROP_LOOP,
    PROP_LAST,
};

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
                                            GST_PAD_SRC,
                                            GST_PAD_ALWAYS,
                                            GST_STATIC_CAPS_ANY);

// Forward declarations
static void gst_spts_file_src_uri_handler_init(gpointer, gpointer);
static void gst_spts_file_src_dispose(GObject* object);
static void gst_spts_file_src_finalize(GObject* object);

static void gst_spts_file_src_set_property(GObject* object,
                                            guint prop_id,
                                            const GValue* value,
                                            GParamSpec* pspec);
static void gst_spts_file_src_get_property(GObject* object,
                                            guint prop_id,
                                            GValue* value,
                                            GParamSpec* pspec);

static gboolean gst_spts_file_src_start(GstBaseSrc* basesrc);
static gboolean gst_spts_file_src_stop(GstBaseSrc* basesrc);

static GstFlowReturn gst_spts_file_src_create(GstPushSrc* src,
                                               GstBuffer** buf);

static gboolean gst_spts_file_src_open(GstSptsFileSrc* src);
static void gst_spts_file_src_close(GstSptsFileSrc* src);
static gboolean gst_spts_file_src_reset(GstSptsFileSrc* src);

static void
_do_init(GType type)
{
    static const GInterfaceInfo urihandler_info =
    {
        gst_spts_file_src_uri_handler_init,
        NULL,
        NULL
    };

    g_type_add_interface_static(type, GST_TYPE_URI_HANDLER, &urihandler_info);
    GST_DEBUG_CATEGORY_INIT(gst_spts_file_src_debug, "sptsfilesrc",
            0, "GST_TYPE_PUSH_SRC sptsfilesrc");
}

/*lint -e(123)*/GST_BOILERPLATE_FULL(GstSptsFileSrc, gst_spts_file_src,
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

static void gst_spts_file_src_base_init(gpointer gclass)
{
    GstElementClass* element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "SptsFileSrc",
            "FIXME:Generic", "FIXME:Generic Template Element",
            " <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
}

/* initialize the sptsfilesrc's class */
static void gst_spts_file_src_class_init(GstSptsFileSrcClass* klass)
{
    GObjectClass* gobject_class;
    GstBaseSrcClass* gstbasesrc_class;
    GstPushSrcClass* gstpushsrc_class;


    gobject_class = (GObjectClass *) klass;
    gstbasesrc_class = (GstBaseSrcClass *) klass;
    gstpushsrc_class = (GstPushSrcClass *) klass;

    parent_class = g_type_class_peek_parent(klass);

    gobject_class->set_property = gst_spts_file_src_set_property;
    gobject_class->get_property = gst_spts_file_src_get_property;

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
                    MAX_BLKSIZE, DEFAULT_BLCKSIZE, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_PROGRAM,
            g_param_spec_uint("program", "Program number to tune to",
                    "Program number to tune to", 0,
                    65535, DEFAULT_PROGRAM, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_LOOP,
            g_param_spec_uint("loop", "Number of times to loop 0 = forever",
                    "Number of times to loop 0 = forever", 0,
                    65535, 0, G_PARAM_READWRITE));

    gobject_class->dispose = GST_DEBUG_FUNCPTR(gst_spts_file_src_dispose);
    gobject_class->finalize = GST_DEBUG_FUNCPTR(gst_spts_file_src_finalize);
    gstbasesrc_class->start = GST_DEBUG_FUNCPTR(gst_spts_file_src_start);
    gstbasesrc_class->stop = GST_DEBUG_FUNCPTR(gst_spts_file_src_stop);
    gstpushsrc_class->create = GST_DEBUG_FUNCPTR(gst_spts_file_src_create);
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_spts_file_src_init(GstSptsFileSrc* src,
                                    GstSptsFileSrcClass* gclass)
{
    int i = 0;

    src->props_lock = g_mutex_new();

    src->fp = NULL;
    src->uri = DEFAULT_FILEPATH;
    src->location = DEFAULT_FILENAME;
    src->blksize = DEFAULT_BLCKSIZE;
    src->current_time = GST_CLOCK_TIME_NONE;
    src->overflow_count = 0;
    src->filebufp = NULL;
    src->null_ts_packet = gst_buffer_new_and_alloc(TS_PACKET_SIZE);
    src->program = 1;
    src->pmt_pid = 0;
    src->video_pid = 0;

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

static void gst_spts_file_src_dispose(GObject* object)
{
    G_OBJECT_CLASS(parent_class)->dispose(object);
}

static void gst_spts_file_src_finalize(GObject* object)
{
    GstSptsFileSrc* src = GST_SPTSFILESRC(object);

    g_mutex_free(src->props_lock);
    g_free(src->uri);
    g_free(src->location);

    G_OBJECT_CLASS(parent_class)->finalize(object);
}

static gboolean
gst_spts_file_src_set_location(GstSptsFileSrc* src, const gchar* location)
{
    gboolean retVal = FALSE;
    g_mutex_lock(src->props_lock);

    if(NULL == src->location || 0 != strcmp(src->location, location))
    {
        gst_spts_file_src_close(src);
        g_free(src->location);
        src->location = g_strdup(location);
        GST_INFO_OBJECT(src, "New location set: \"%s\" (blksize:%u)",
                        src->location, src->blksize);
        retVal = gst_spts_file_src_open(src);
    }

    g_mutex_unlock(src->props_lock);
    return retVal;
}

static gboolean
gst_spts_file_src_set_uri(GstSptsFileSrc* src, const gchar* uri)
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
                    GST_INFO_OBJECT(src, "New URI set: \"%s\".", src->uri);
                }

                g_mutex_unlock(src->props_lock);
                retVal = gst_spts_file_src_set_location(src, location);
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

static void gst_spts_file_src_set_property(GObject* object,
                                            guint prop_id,
                                            const GValue* value,
                                            GParamSpec* pspec)
{
    GstSptsFileSrc* src = GST_SPTSFILESRC(object);

    switch (prop_id)
    {
    case PROP_URI:
    {
        (void)gst_spts_file_src_set_uri(src, g_value_get_string(value));
        break;
    }
    case PROP_LOCATION:
    {
        (void)gst_spts_file_src_set_location(src, g_value_get_string(value));
        break;
    }
    case PROP_BLKSIZE:
    {
        g_mutex_lock(src->props_lock);
        src->blksize = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "New blksize set: %u.", src->blksize);
        g_mutex_unlock(src->props_lock);
        break;
    }
    case PROP_PROGRAM:
    {
        g_mutex_lock(src->props_lock);
        src->program = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "New program set: %u.", src->program);
        gst_spts_file_src_reset(src);
        g_mutex_unlock(src->props_lock);
        break;
    }
    case PROP_LOOP:
    {
        g_mutex_lock(src->props_lock);
        src->loop = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "will loop %u times.", src->loop);
        g_mutex_unlock(src->props_lock);
        break;
    }
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_spts_file_src_get_property(GObject* object,
                                            guint prop_id,
                                            GValue* value,
                                            GParamSpec* pspec)
{
    GstSptsFileSrc* src = GST_SPTSFILESRC(object);

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
    case PROP_PROGRAM:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->program);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_LOOP:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->loop);
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
static gboolean gst_spts_file_src_start(GstBaseSrc* basesrc)
{
    GstSptsFileSrc* src = GST_SPTSFILESRC(basesrc);
    gboolean retval = TRUE;

    src->current_time = 0;
    src->overflow_count = 0;
    src->filebufp = NULL;

    GST_INFO_OBJECT(src, "returning %s", retval? "TRUE" : "FALSE");
    return retval;
}

/* stop processing, ideal for closing the resource */
static gboolean gst_spts_file_src_stop(GstBaseSrc * basesrc)
{
    GstSptsFileSrc* src = GST_SPTSFILESRC(basesrc);
    gboolean retval = TRUE;

    gst_spts_file_src_close(src);

    src->current_time = 0;
    src->overflow_count = 0;
    src->filebufp = NULL;

    GST_INFO_OBJECT(src, "returning %s", retval? "TRUE" : "FALSE");
    return retval;
}

/****************************************************/
/**********                                **********/
/********** GstSptsFileSrc IMPLEMENTATION **********/
/**********                                **********/
/****************************************************/

/* Should be called with prop_lock held. */
static gboolean gst_spts_file_src_reset(GstSptsFileSrc* src)
{
    int i;

    fseek(src->fp, 0, SEEK_SET);
    src->sysclock = gst_system_clock_obtain();
    src->pmt_pid = 0;
    src->video_pid = 0;
    src->pacing_pid = 0;
    src->current_time = 0;
    src->overflow_count = 0;
    src->filebufp = NULL;
    src->hSize = 0;
    src->vSize = 0;
    src->aspectRatioIndex = 0;
    src->frameRateIndex = 0;
    src->begPCR = 0;
    src->endPCR = 0;
    src->duration = 0;

    // Initialize Audio PID array
    for (i = 0; i < MAX_AUDIO_PIDS; i++)
    {
        src->audio_pids[i] = 0;
    }

    return TRUE;
}

/* Should be called with prop_lock held. */
static gboolean gst_spts_file_src_open(GstSptsFileSrc* src)
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

    return gst_spts_file_src_reset(src);
}

static void gst_spts_file_src_close(GstSptsFileSrc* src)
{
    if (NULL != src->fp)
    {
        GST_INFO_OBJECT(src, "closing \"%s\"",
                       (src->location)? src->location:"NULL");
        gst_object_unref(src->sysclock);
        fclose(src->fp);
        src->fp = NULL;
        src->current_time = 0;
        src->overflow_count = 0;
        src->filebufp = NULL;
    }
}

               
static gint inAudioPidList(GstSptsFileSrc* src, guint16 pid) 
{
    gint i = 0;

    while (i < MAX_AUDIO_PIDS)
    {
        if (src->audio_pids[i] == pid)
        {
            return i;
        }

        i++;
    }

    return -1;
}

static gboolean parse_PAT(GstSptsFileSrc* src, GstBitReader *gbr, guint16 pid)
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

    GST_LOG_OBJECT(src, "%s", __func__);

    if (payload_unit_start_ind)
    {
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
    }
    else
    {
        GST_INFO_OBJECT(src, "PAT Continuation");
    }

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

            if (program == src->program)
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

static void parse_PMT(GstSptsFileSrc* src, GstBitReader *gbr, guint16 pid)
{
    gint i = 0;
    gint j = 0;
    guint8 temp = 0;
    guint8 pmt[1024] = {0};
    guint16 pcrPID = 0;
    guint16 program = 0;
    static guint16 esPID = 0;
    static guint16 bytes = 0;
    static guint16 pmtLen = 0;
    static guint16 pmtByte = 0;
    static guint16 esInfoLen = 0;
    static guint16 pgmInfoLen = 0;
    static guint8 stream_type = 0;
    static guint8 esDescriptor[1024] = {0};
    static guint8 pgmDescriptor[1024] = {0};

    GST_LOG_OBJECT(src, "%s", __func__);

    if (payload_unit_start_ind)
    {
        pmtLen = 0;
        pmtByte = 0;
        gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // PMT pointer
        GST_LOG_OBJECT(src, "PTR = %d (0x%02X)", temp, temp);
        temp += ((temp == 0)? 0 : 1);                        // point to next
        gst_bit_reader_skip(gbr, (8 * temp));                // move to PMT
        gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // Table ID

        if (PMT_TID != temp)
        {
            GST_DEBUG_OBJECT(src, "TID != 0x02 (0x%02X) not PMT", temp);
            return;
        }
        else 
        {
            GST_LOG_OBJECT(src, "TID = %d (0x%02X)", temp, temp);
        }

        gst_bit_reader_skip(gbr, 4);                         // SI + reserved
        gst_bit_reader_get_bits_uint16(gbr, &pmtLen, 12);    // PMT length
        GST_LOG_OBJECT(src, "PMT Section len = %d", pmtLen);
        gst_bit_reader_get_bits_uint16(gbr, &program, 16);   // program #
        GST_LOG_OBJECT(src, "PRGM = %d", program);
        pmtLen -= 2;
        gst_bit_reader_skip(gbr, 8);                         // reserved ++
        pmtLen -= 1;
        gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
        GST_LOG_OBJECT(src, "PMT Section number = %d", temp);
        pmtLen -= 1;
        gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
        GST_LOG_OBJECT(src, "PMT Last Section number = %d", temp);
        pmtLen -= 1;
        gst_bit_reader_skip(gbr, 3);                         // reserved
        gst_bit_reader_get_bits_uint16(gbr, &pcrPID, 13);    // PCR PID
        GST_LOG_OBJECT(src, "PCR PID = %d (0x%02X)", pcrPID, pcrPID);
        pmtLen -= 2;
        gst_bit_reader_skip(gbr, 4);                         // reserved
        gst_bit_reader_get_bits_uint16(gbr, &pgmInfoLen, 12);// Prgm Info Len
        GST_LOG_OBJECT(src, "Program Info Len = %d", pgmInfoLen);
        pmtLen -= 2;
    }
    else
    {
        GST_INFO_OBJECT(src, "PMT Continuation");
    }

    bytes = (gst_bit_reader_get_remaining(gbr) / 8);
    temp = (bytes >= (pmtLen - pmtByte))? (pmtLen - pmtByte) : bytes;
    GST_LOG_OBJECT(src, "read %d bytes of PMT from %d remaining",
                    temp, bytes);

    for (i = 0; i < temp; i++)
    {
        gst_bit_reader_get_bits_uint8(gbr, &pmt[pmtByte], 8);
        GST_LOG_OBJECT(src, "PMT[%d] = %02X", pmtByte, pmt[pmtByte]);
        pmtByte++;
    }

    if (pmtByte == pmtLen)
    {
        GST_LOG_OBJECT(src, "End of PMT[%d]", pmtLen);

        for (i = 0; i < pgmInfoLen; i++)
        {
            pgmDescriptor[i] = pmt[i];
            GST_LOG_OBJECT(src, "pgmDesc[%d] = %02X", i, pgmDescriptor[i]);
        }

        while (i < (pmtLen - 4))
        {
            stream_type = pmt[i];
            esPID = (pmt[i + 1] & 0x1f) << 8;
            esPID |= pmt[i + 2];
            esInfoLen = (pmt[i + 3] & 0x0f) << 8;
            esInfoLen |= pmt[i + 4];
            GST_LOG_OBJECT(src, "stream type = %d (0x%02x) "
                                "ES PID = %d (0x%04x) ES Info Len = %d",
                                stream_type, stream_type, esPID, esPID,
                                esInfoLen);
            if (stream_type == 0x1  ||
                stream_type == 0x2  ||
                stream_type == 0x80 ||
                stream_type == 0x1b ||
                stream_type == 0xea)
            {
                if (0 == src->video_pid)
                {
                    src->video_pid = esPID;
                    GST_INFO_OBJECT(src, "Video stream type = %d (0x%02x) "
                                         "Video PID = %d (0x%04x) ",
                                         stream_type, stream_type,
                                         esPID, esPID);
                }
            }
            else if (stream_type == 0x3  ||
                     stream_type == 0x4  ||
                     stream_type == 0x80 ||
                     stream_type == 0x81 ||
                     stream_type == 0x6  ||
                     stream_type == 0x82 ||
                     stream_type == 0x83 ||
                     stream_type == 0x84 ||
                     stream_type == 0x85 ||
                     stream_type == 0x86 ||
                     stream_type == 0xa1 ||
                     stream_type == 0xa2 ||
                     stream_type == 0x11)
            {
                if (-1 == inAudioPidList(src, esPID))
                {
                    src->audio_channel_count++;

                    if (0 == src->audio_pids[src->audio_channel_count])
                    {
                        GST_INFO_OBJECT(src, "Audio stream type: %d (0x%02x) "
                                             "Audio PID = %d (0x%04x) ",
                                             stream_type, stream_type,
                                             esPID, esPID);
                    }

                    src->audio_pids[src->audio_channel_count] = esPID;
                }
            }

            for (j = 0; j < esInfoLen; j++)
            {
                esDescriptor[j] = pmt[i + 5 + j];
                GST_LOG_OBJECT(src, "esDesc[%d] = %02X", j, esDescriptor[j]);
            }

            i += (5 + j);
        }
    }
}

static double indexToFrameRate(GstSptsFileSrc* src, guint8 index)
{
    switch (index)
    {
        case 1:
            return 24.0 * (1000.0 / 1001.0);
        case 2:
            return 24.0;
        case 3:
            return 25.0;
        case 4:
            return 30.0 * (1000.0 / 1001.0);
        case 5:
            return 30.0;
        case 6:
            return 50.0;
        case 7:
            return 60.0 * (1000.0 / 1001.0);
        case 8:
            return 60.0;
        default:
            GST_INFO_OBJECT(src, "unknown rate index = %d", index);
            return 0.0;    // reserved
    }
}

static char* indexToAspectRatioStr(guint8 index)
{
    switch (index)
    {
        case 0:
            return "forbidden";
        case 1:
            return "1:1 square samples";
        case 2:
            return "4:3";
        case 3:
            return "16:9";
        case 4:
            return "2.21:1";
        default:
            return "reserved";
    }
}

static char* indexToFrameTypeStr(GstSptsFileSrc* src, guint8 index)
{
    switch (index)
    {
        case 1:
            return "I";
        case 2:
            return "P";
        case 3:
            return "B";
        case 4:
            return "D";
        default:
            GST_INFO_OBJECT(src, "unknown frame type index = %d", index);
            return "unknown";
    }
}

guint32 findStartCode(GstSptsFileSrc* src, GstBitReader *gbr,
                      guint32 startCode, guint8* numZeros, guint16* bytes)
{
    guint8 temp = 0;

    while((startCode <= 1) && (*bytes > 0))
    {
        gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
        *bytes -= 1;

        if (2 == *numZeros)
        {
            if (1 == startCode)
            {
                GST_LOG_OBJECT(src, "%d zeros, SC: %X", *numZeros, startCode);
                startCode <<= 8;
                startCode |= temp;
                *numZeros = 0;           // start over for next SC

                if ((0x000001C0 <= startCode) && (startCode <= 0x000001DF))
                {
                    GST_DEBUG_OBJECT(src, "start audio stream");
                    startCode = 0;
                }
                else if ((0x00001E0 <= startCode) && (startCode <= 0x00001EF))
                {
                    GST_DEBUG_OBJECT(src, "start video stream");
                    startCode = 0;
                }
                else
                {
                    break;
                }
            }
            else if (1 == temp)
            {
                GST_LOG_OBJECT(src, "SC: %X, temp: %02X", startCode, temp);
                startCode |= temp;
            }
            else
            {
                GST_LOG_OBJECT(src, "%d zeros, start over 1, temp: %02X",
                               *numZeros, temp);
                *numZeros = 0;           // start over SC not found
            }
        }
        else if (0 == temp)
        {
            *numZeros += 1;
        }
        else
        {
            GST_LOG_OBJECT(src, "%d zeros, start over 2, temp: %02X",
                           *numZeros, temp);
            *numZeros = 0;               // start over zeros not found
        }
    }

    GST_LOG_OBJECT(src, "startCode = (0x%08X) bytes left = %d",
                         startCode, *bytes);
    return startCode;
}

static void processPictureHdr(GstSptsFileSrc* src, GstBitReader *gbr,
                              guint32 startCode, guint16* bytes)
{
    char *str = NULL;
    guint8 temp = 0;

    if (*bytes < 2)
    {
        GST_INFO_OBJECT(src, "insufficient bytes (%d) left for pic hdr",
                        *bytes);
        return;
    }

    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // temp seq num
    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // type
    str = indexToFrameTypeStr(src, (temp & 0x38) >> 3);
    GST_DEBUG_OBJECT(src, "frameType = %s (%d)", str, (temp & 0x38) >> 3);
}

static void processSequenceHdr(GstSptsFileSrc* src, GstBitReader *gbr,
                               guint32 startCode, guint16* bytes)
{
    double frameRate = 1.0;
    guint32 temp32 = 0;
    char *str = NULL;
    guint16 hSize = 0;
    guint16 vSize = 0;
    guint8 aspectRatioIndex = 0;
    guint8 frameRateIndex = 0;

    if (*bytes < 8)
    {
        GST_INFO_OBJECT(src, "insufficient bytes (%d) for seq hdr", *bytes);
        return;
    }

    gst_bit_reader_get_bits_uint32(gbr, &temp32, 32);    // get seq hdr
    *bytes -= 4;
    hSize = (temp32 >> 20);
    vSize = ((temp32 & 0x000FFF00) >> 8);
    aspectRatioIndex = ((temp32 & 0x000000F0) >> 4);
    frameRateIndex = (temp32 & 0x0000000F);
    gst_bit_reader_get_bits_uint32(gbr, &temp32, 32);    // get bit rate
    *bytes -= 4;

    if ((hSize != src->hSize) || (vSize != src->vSize) ||
        (aspectRatioIndex != src->aspectRatioIndex) ||
        (frameRateIndex != src->frameRateIndex))
    {
        GST_INFO_OBJECT(src, "%s - updated sequence header", __func__);
        GST_INFO_OBJECT(src, "H-size:%d", hSize);
        GST_INFO_OBJECT(src, "V-size:%d", vSize);
        GST_LOG_OBJECT(src, "aspect:%d", aspectRatioIndex);
        GST_LOG_OBJECT(src, "F-rate:%d", frameRateIndex);
        str = indexToAspectRatioStr(aspectRatioIndex);
        GST_INFO_OBJECT(src, "Aspect ratio = %s", str);
        frameRate = indexToFrameRate(src, frameRateIndex);
        GST_INFO_OBJECT(src, "Frame rate = %4.2lf", frameRate);
        src->hSize = hSize;
        src->vSize = vSize;
        src->aspectRatioIndex = aspectRatioIndex;
        src->frameRateIndex = frameRateIndex;
        GST_INFO_OBJECT(src, "Sequence header bitrate = %d bps",
                       ((temp32 & 0xFFFFC000) >> 14) * 400);
    }
}

static void processPesHdr(GstSptsFileSrc* src, GstBitReader *gbr,
                          guint32 startCode, guint16* bytes, char* typeStr)
{
    GST_INFO_OBJECT(src, "%s for %s", __func__, typeStr);
}

static void processExtensionHdr(GstSptsFileSrc* src, GstBitReader *gbr,
                                guint32 startCode, guint16* bytes)
{
    guint8 temp = 0;

    if (*bytes < 1)
    {
        GST_INFO_OBJECT(src, "insufficient bytes (%d) left for ext hdr",
                        *bytes);
        return;
    }

    gst_bit_reader_get_bits_uint8(gbr, &temp, 8);        // get ext hdr
    GST_INFO_OBJECT(src, "%s - sequence extension: %02X", __func__, temp);
}

static void parse_Video(GstSptsFileSrc* src, GstBitReader *gbr, guint16 pid)
{
    static guint32 startCode = 0;
    static guint8 numZeros = 0;
    guint16 bytes = (gst_bit_reader_get_remaining(gbr) / 8);

    startCode = findStartCode(src, gbr, startCode, &numZeros, &bytes);

    if (0 == bytes)
    {
        GST_LOG_OBJECT(src, "next buffer... (SC: %08X)", startCode);
        return;
    }

    if (0x00000100 == startCode)                             // picture
    {
        processPictureHdr(src, gbr, startCode, &bytes);
    }
    else if ((0x00000101 <= startCode) && (0x000001AF >= startCode)) // slice
    {
        GST_LOG_OBJECT(src, "%s - slice", __func__);
    }
    else if (0x000001B2 == startCode)                        // user data
    {
        GST_INFO_OBJECT(src, "%s - user data", __func__);
    }
    else if (0x000001B3 == startCode)                        // sequence hdr
    {
        processSequenceHdr(src, gbr, startCode, &bytes);
    }
    else if (0x000001B4 == startCode)                        // sequence error
    {
        GST_INFO_OBJECT(src, "%s - sequence error", __func__);
    }
    else if (0x000001B5 == startCode)                        // extension
    {
        processExtensionHdr(src, gbr, startCode, &bytes);
    }
    else if (0x000001B7 == startCode)                        // sequence end
    {
        GST_INFO_OBJECT(src, "%s - sequence end", __func__);
    }
    else if (0x000001B8 == startCode)                        // GOP
    {
        GST_INFO_OBJECT(src, "%s - GOP", __func__);
    }
    else if (0x000001B9 == startCode)                        // program end
    {
        GST_INFO_OBJECT(src, "%s - program end", __func__);
    }
    else if (0x000001BA == startCode)                        // pack hdr
    {
        GST_INFO_OBJECT(src, "%s - pack header", __func__);
    }
    else if (0x000001BB == startCode)                        // system hdr
    {
        GST_INFO_OBJECT(src, "%s - system header", __func__);
    }
    else if (0x000001BC == startCode)                        // PS map
    {
        GST_INFO_OBJECT(src, "%s - PS map", __func__);
    }
    else if ((0x000001BD == startCode) || (0x000001BF == startCode))// private
    {
        GST_INFO_OBJECT(src, "%s - private", __func__);
    }
    else if (0x000001BE == startCode)                        // padding
    {
        GST_INFO_OBJECT(src, "%s - padding", __func__);
    }
    else if ((0x000001C0 <= startCode) && (0x000001DF >= startCode)) // audio
    {
        processPesHdr(src, gbr, startCode, &bytes, "audio");
    }
    else if ((0x000001E0 <= startCode) && (0x000001EF >= startCode)) // video
    {
        processPesHdr(src, gbr, startCode, &bytes, "video");
    }
    else if (0x000001F0 == startCode)                        // ECM
    {
        GST_INFO_OBJECT(src, "%s - ECM", __func__);
    }
    else if (0x000001F1 == startCode)                        // EMM
    {
        GST_INFO_OBJECT(src, "%s - EMM", __func__);
    }
    else if (0x000001FF == startCode)                        // PS Directory
    {
        GST_INFO_OBJECT(src, "%s - PS directory", __func__);
    }

    // we have processed this sequence, look for the next...
    startCode = 0;
}

static void parse_Audio(GstSptsFileSrc* src, GstBitReader *gbr, guint16 pid)
{
    GST_LOG_OBJECT(src, "%s", __func__);
}

static guint8 parse_AF(GstSptsFileSrc* src, GstBitReader *gbr, guint16 pid)
{
    static guint64 last_PCR = 0;
    guint64 temp64 = 0;
    guint64 PCR = 0;
    guint64 delta = 0;
    guint8 af_len = 0;
    guint8 pcrf = 0;

    GST_LOG_OBJECT(src, "%s", __func__);
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
    gst_bit_reader_skip(gbr, 4);    // skip the remaining AF flags

    if (pcrf)
    {
        GstClockTime sysclocktime = 0;
        GstClockTimeDiff sysclockdiff = 0;

        if ((src->pacing_pid == 0) && (src->video_pid != 0))
        {
            src->pacing_pid = pid;
            GST_INFO_OBJECT(src, "will pace on PID 0x%04X PCR", pid);
        }
        else if (pid != src->pacing_pid)
        {
            return af_len;
        }

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
            GST_INFO_OBJECT(src, "duration %llu of stream on PID 0x%04X",
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
            // remove realtime diff and sleep for no more than 30000 uSecs
            delta -= sysclockdiff;
            delta = GST_TIME_AS_USECONDS(delta);
            GST_DEBUG_OBJECT(src, "PCR corrective sleep:%llu", delta);
            g_usleep((delta > 30000)? 30000 : delta);
        }
    }

    return af_len;
}

static gboolean processMPTS(GstSptsFileSrc* src, guint8* ppkt, int len)
{
    static guint32 packet = 0;
    static guint32 pat = 0;
    static guint32 pmt = 0;
    static guint8 last_cc = 255;
    gboolean retVal = FALSE;
    guint8 transport_error_ind;
    guint8 adaptation_field_control;
    GstBitReader gbr;
    guint16 pid;
    guint8* pdata;
    guint8* ppat;
    guint16 patLen;
    guint32 crc;
    guint8 cc;
    gint i;

    GST_LOG_OBJECT(src, "%s(%p, %p, %d)", __func__, src, ppkt, len);
    //
    // WARNING: this method is assuming big endian!
    //
    while ((len >= TS_PACKET_SIZE) && (NULL != ppkt))
    {
        pdata = ppkt;

        if (*pdata == 0x47)
        {
            pid = 0;
            packet++;
            GST_LOG_OBJECT(src, "SYNC detected");
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
                GST_WARNING_OBJECT(src, "TS error detected");
                break;
            }
            else
            {
                // we have a valid packet...
                GST_LOG_OBJECT(src, "PID %04X detected", pid);

                if (0x1FF0 == (pid & 0x1FF0)) // pass NULLs and PSI
                {
                    retVal = TRUE;
                }
                else if (PAT_PID == pid)
                {
                    pat++;
                    retVal = TRUE;
                    GST_DEBUG_OBJECT(src, "PAT %d found at packet %d",
                                    pat, packet);
                    if (TRUE == parse_PAT(src, &gbr, pid))
                    {
                        // PAT may be passed as is (no re-writing)
#ifdef VALIDATE_CRC
                        guint8* pcrc = NULL;
                        ppat = pdata+4;
                        ppat += *ppat? *ppat + 1 : 0;
                        patLen = ((*(ppat+2) & 0x0F) << 8) | *(ppat+3);
                        GST_DEBUG_OBJECT(src, "read len %d", patLen);
                        ppat++;
                        pcrc = ppat + (patLen - 4) + PAT_HDR_SZ;
                        crc = (((pcrc[0]) << 24) | ((pcrc[1]) << 16) |
                               ((pcrc[2]) << 8) | (pcrc[3]));
                        GST_DEBUG_OBJECT(src, " read CRC %08X", crc);

                        crc32_init();
                        crc = crc32_calc(ppat, (patLen - 4) + PAT_HDR_SZ);
                        GST_DEBUG_OBJECT(src, " calc CRC %08X", crc);
#endif
                    }
                    else
                    {
                        // re-write the PAT (MPTS -> SPTS)
                        GST_DEBUG_OBJECT(src, "re-writing PAT...");
                        patLen = 13;
                        pdata += 4;         // skip the TS header
                        *pdata = 0;         // write 0 to the pointer val
                        pdata += 1;         // point to the start of the PAT
                        ppat = pdata;       // remember this position for CRC32
                        *pdata = 0;         // write 0 to the TID
                        pdata += 1;         // point beyond the table ID
                        *pdata = (*pdata & 0xF0) & ((patLen >> 8) & 0x0F);
                        pdata += 1;         // point to 2nd half of length
                        *pdata = (patLen & 0xFF);
                        GST_LOG_OBJECT(src, "wrote len %d", patLen);
                        pdata += 1;         // skip to the TSID MSB
                        GST_LOG_OBJECT(src, "TSID %d", *pdata<<8 | *(pdata+1));
                        pdata += 1;         // skip to the TSID LSB
                        pdata += 1;         // skip to the ver/curr/next
                        GST_LOG_OBJECT(src, "RES %02X", *pdata & 0xC0);
                        GST_LOG_OBJECT(src, "VER %02X", *pdata & 0x3E);
                        GST_LOG_OBJECT(src, "CNI %02X", *pdata & 0x01);
                        pdata += 1;         // skip to the sect_num
                        GST_LOG_OBJECT(src, "Sect %d", *pdata);
                        pdata += 1;         // skip to the last sect_num
                        GST_LOG_OBJECT(src, "Last Sect %d", *pdata);
                        pdata += 1;         // skip to the program number
                        *pdata = ((src->program >> 8) & 0xFF);
                        pdata += 1;         // point to 2nd half of program
                        *pdata = (src->program & 0xFF);
                        GST_LOG_OBJECT(src, "wrote prgm %d", src->program);
                        pdata += 1;         // point to 1st half of PID
                        *pdata = ((src->pmt_pid >> 8) & 0xFF);
                        pdata += 1;         // point to 2nd half of PID
                        *pdata = (src->pmt_pid & 0xFF);
                        GST_LOG_OBJECT(src, "wrote PID %d", src->pmt_pid);
                        pdata += 1;         // point to CRC

                        crc32_init();
                        crc = crc32_calc(ppat, (patLen - 4) + PAT_HDR_SZ);
                        *pdata++ = ((crc >> 24) & 0xFF);
                        *pdata++ = ((crc >> 16) & 0xFF);
                        *pdata++ = ((crc >> 8) & 0xFF);
                        *pdata++ = (crc & 0xFF);
                        GST_LOG_OBJECT(src, "wrote CRC %08X", crc);

                        for (i = 0;
                             i < (TS_PACKET_SIZE -
                                 (TS_HDR_SZ + PAT_HDR_SZ + patLen + 4));
                             i++)
                        {
                            *pdata++ = 0;
                        }
                    }
                }
                else if (src->pmt_pid == pid)
                {
                    pmt++;
                    GST_DEBUG_OBJECT(src, "PMT %d found at packet %d",
                                    pmt, packet);
                    retVal = TRUE;
                    parse_PMT(src, &gbr, pid);
                }
                else if (src->video_pid == pid)
                {
                    retVal = TRUE;
                    last_cc = ((255 == last_cc)? cc : ((last_cc + 1) & 0x0F));

                    if (last_cc != cc)
                    {
                        GST_LOG_OBJECT(src, "discont. at pkt %d (%d != %d)",
                                       packet, last_cc, cc);
                        // re-write the continuity counter...
                        pdata[3] = ((pdata[3] & 0xF0) | (last_cc & 0x0F));
                    }

                    if (adaptation_field_control & 0x02)
                    {
                        guint8 afLen = parse_AF(src, &gbr, pid);

                        if (0 != src->duration)
                        {
                            // re-write the AF PCR (loop -> continuous)
                            GST_LOG_OBJECT(src, "rewrite PCR: AF(%X) len:%d",
                                           adaptation_field_control, afLen);
                            pdata += 4;         // skip the TS header

                            if (*pdata != afLen)
                            {
                                GST_LOG_OBJECT(src, "pdata:%d != len:%d",
                                               *pdata, afLen);
                            }

                            pdata += 4;         // skip over the AF length

                            if (0x10 == (*pdata & 0x10))
                            {
                                pdata += 4;     // skip over the AF flags
                                src->curPCR += src->duration;
                                *pdata = (src->curPCR >> ((4 * 8) +1)) & 0xFF;
                                pdata += 4;
                                *pdata = (src->curPCR >> ((3 * 8) +1)) & 0xFF;
                                pdata += 4;
                                *pdata = (src->curPCR >> ((2 * 8) +1)) & 0xFF;
                                pdata += 4;
                                *pdata = (src->curPCR >> ((1 * 8) +1)) & 0xFF;
                                pdata += 4;
                                *pdata = (*pdata & 0x7F)|(src->curPCR & 0x80);
                            }
                            else
                            {
                                GST_LOG_OBJECT(src, "pcrf:%02X != 0x10",
                                               *pdata);
                            }
                        }
                    }

                    parse_Video(src, &gbr, pid);
                }
                else if (-1 != inAudioPidList(src, pid))
                {
                    retVal = TRUE;
                    parse_Audio(src, &gbr, pid);
                }
                else
                {
                    GST_LOG_OBJECT(src, "Skipped PID (0x%X) for pkt: %p",
                                   pid, pdata);
                }
            }
        }
        else
        {
            GST_WARNING_OBJECT(src, "Bad Sync (0x%X) for pkt: %p, len: %d!?",
                               *pdata, pdata, len);
        }

        ppkt += TS_PACKET_SIZE;
        len -= TS_PACKET_SIZE;
    }

    return retVal;
}

static GstFlowReturn gst_spts_file_src_create(GstPushSrc* pushsrc,
                                               GstBuffer** bufp)
{
    g_assert(pushsrc != NULL);
    GstSptsFileSrc* src = GST_SPTSFILESRC(pushsrc);
    GstFlowReturn retval = GST_FLOW_OK;
    GstBuffer *buf = NULL;
    guint8 *pBufData = NULL;
    int bytes = 0;

    // return the NULL TS packet by default
    *bufp = src->null_ts_packet;
    g_mutex_lock(src->props_lock);

    if(NULL != src->fp)
    {
        int max_tries = 30 * src->blksize;

        buf = gst_buffer_new_and_alloc(src->blksize);
        pBufData = GST_BUFFER_DATA(buf);

        do
        {
            // read a packet from the file into the buffer...
            bytes = fread(pBufData, 1, TS_PACKET_SIZE, src->fp);

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
                        retval = GST_FLOW_ERROR;
                    }
                    else
                    {
                        GST_INFO_OBJECT(src, "%d = fread() - loop %d",
                                         bytes, src->loop);
                    }
                }

                fseek(src->fp, 0, SEEK_SET);
                g_usleep(10000);
                break;
            }
            else if (processMPTS(src, pBufData, bytes))
            {
                GST_LOG_OBJECT(src, "add %d bytes at %p", bytes, pBufData);
                pBufData += bytes;   // point to the next pkt position in buf

                if ((pBufData - GST_BUFFER_DATA(buf)) >= src->blksize)
                {
                    *bufp = buf;
                    break;
                }
            }
            else
            {
                g_usleep(175);
            }

        } while (--max_tries);
    }

    g_mutex_unlock(src->props_lock);

    if (*bufp == src->null_ts_packet)
    {
        gst_buffer_ref(*bufp);
        GST_INFO_OBJECT(src, "%s - sending null_ts_packet...", __func__);
    }
    else
    {
        GST_LOG_OBJECT(src, "Push buffer %p, size %u", buf, bytes);
        GST_BUFFER_SIZE(buf) = src->blksize;
        gst_buffer_set_caps(buf, GST_PAD_CAPS(GST_BASE_SRC_PAD(src)));
    }

    return retval;
}


/*********************************************/
/**********                         **********/
/********** GstUriHandler INTERFACE **********/
/**********                         **********/
/*********************************************/

static GstURIType
gst_spts_file_src_uri_get_type(void)
{
    return GST_URI_SRC;
}

static gchar **
gst_spts_file_src_uri_get_protocols(void)
{
    static gchar *protocols[] = { "file", NULL };
    return protocols;
}

static const gchar *
gst_spts_file_src_uri_get_uri(GstURIHandler* handler)
{
    GstSptsFileSrc* src = GST_SPTSFILESRC(handler);
    return src->uri;
}

static gboolean
gst_spts_file_src_uri_set_uri(GstURIHandler* handler, const gchar* uri)
{
    GstSptsFileSrc* src = GST_SPTSFILESRC(handler);
    return gst_spts_file_src_set_uri(src, uri);
}

static void
gst_spts_file_src_uri_handler_init(gpointer g_iface, gpointer iface_data)
{
  GstURIHandlerInterface *iface = (GstURIHandlerInterface *)g_iface;

  iface->get_type = gst_spts_file_src_uri_get_type;
  iface->get_protocols = gst_spts_file_src_uri_get_protocols;
  iface->get_uri = gst_spts_file_src_uri_get_uri;
  iface->set_uri = gst_spts_file_src_uri_set_uri;
}

