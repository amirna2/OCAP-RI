/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2009  <<user@hostname.org>>
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
 * SECTION:element-trickplayfilesrc
 *
 * FIXME:Describe trickplayfilesrc here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! trickplayfilesrc ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdlib.h> // free
#include <string.h> // memcpy
#include <gst/gst.h>

#include "gsttrickplayfilesrc.h"

GST_DEBUG_CATEGORY_STATIC ( gst_trick_play_file_src_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_trick_play_file_src_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0,
    PROP_FILEPATH,
    PROP_FILENAME,
    PROP_FRAMERATE,
    PROP_PLAYRATE,
    PROP_POSITION_TIME,
    PROP_POSITION_BYTES,
    PROP_BLOCKSIZE,
    PROP_EOS_SLEEP_NSECS,
    PROP_TIMESTAMP_WITH_POSITION,
    PROP_TEST_THREAD
};

static GstStaticPadTemplate
        src_factory =
                GST_STATIC_PAD_TEMPLATE(
                        "src",
                        GST_PAD_SRC,
                        GST_PAD_ALWAYS,
                        GST_STATIC_CAPS(
                                "video/mpegts," "packetsize=(int)188," "systemstream=(boolean)true"));

/*lint -e(123)*/GST_BOILERPLATE (GstTrickPlayFileSrc, gst_trick_play_file_src, GstPushSrc, GST_TYPE_PUSH_SRC)

// Forward declarations
static void gst_trick_play_file_src_dispose (GObject * object);
static void gst_trick_play_file_src_finalize(GObject * object);

static void gst_trick_play_file_src_set_property(GObject * object,
        guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_trick_play_file_src_get_property(GObject * object,
        guint prop_id, GValue * value, GParamSpec * pspec);

static gboolean gst_trick_play_file_src_start(GstBaseSrc * basesrc);
static gboolean gst_trick_play_file_src_stop(GstBaseSrc * basesrc);

static GstFlowReturn gst_trick_play_file_src_create(GstPushSrc *src,
        GstBuffer **buf);

static gboolean gst_trick_play_file_src_open(GstTrickPlayFileSrc *src);
static void gst_trick_play_file_src_close(GstTrickPlayFileSrc *src);
static GstFlowReturn gst_trick_play_file_src_create_frame(
        GstTrickPlayFileSrc *src, gboolean calculate_new_position);
static GstFlowReturn gst_trick_play_file_src_create_read(
        GstTrickPlayFileSrc *src);
static GstFlowReturn gst_trick_play_file_src_seek_to_time(
        GstTrickPlayFileSrc *src, GstClockTime time);
static GstFlowReturn gst_trick_play_file_src_seek_to_bytes(
        GstTrickPlayFileSrc *src, guint64 bytes);
static void gst_trick_play_file_src_update_outgoing_timestamp(
        GstTrickPlayFileSrc *src);
static gpointer gst_trick_play_file_src_test_thread(
        gpointer user_data_trick_play_file_src);

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

static void gst_trick_play_file_src_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "TrickPlayFileSrc",
            "FIXME:Generic", "FIXME:Generic Template Element",
            " <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
}

/* initialize the trickplayfilesrc's class */
static void gst_trick_play_file_src_class_init(GstTrickPlayFileSrcClass * klass)
{
    GObjectClass *gobject_class;
    GstBaseSrcClass *gstbasesrc_class;
    GstPushSrcClass *gstpushsrc_class;

    GST_DEBUG_CATEGORY_INIT(gst_trick_play_file_src_debug, "trickplayfilesrc",
            0, "Template trickplayfilesrc");

    gobject_class = (GObjectClass *) klass;
    gstbasesrc_class = (GstBaseSrcClass *) klass;
    gstpushsrc_class = (GstPushSrcClass *) klass;

    parent_class = g_type_class_peek_parent(klass);

    gobject_class->set_property = gst_trick_play_file_src_set_property;
    gobject_class->get_property = gst_trick_play_file_src_get_property;

    g_object_class_install_property(gobject_class, PROP_FILEPATH,
            g_param_spec_string("filepath", "Path to IFS recording",
                    "Path to IFS recording", DEFAULT_FILEPATH,
                    G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property(gobject_class, PROP_FILENAME,
            g_param_spec_string("filename", "Name of IFS recording",
                    "Name of IFS recording", DEFAULT_FILENAME,
                    G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property(gobject_class, PROP_FRAMERATE,
            g_param_spec_uint("framerate", "Frame rate",
                    "Frame rate: 1 < i < 30", MIN_FRAMERATE, MAX_FRAMERATE,
                    DEFAULT_FRAMERATE, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_PLAYRATE,
            g_param_spec_float("playrate", "Playback rate",
                    "Playback rate: [-]X[.X], eg. -1.0, 64, -0.5",
                    MIN_PLAYRATE, MAX_PLAYRATE, DEFAULT_PLAYRATE,
                    G_PARAM_READWRITE));

    g_object_class_install_property(
            gobject_class,
            PROP_POSITION_TIME,
            g_param_spec_uint64(
                    "position_time",
                    "Time position to seek to",
                    "Starting position in nanoseconds relative to the beginning of the recording",
                    0, G_MAXUINT64, DEFAULT_POSITION_TIME, G_PARAM_READWRITE));

    g_object_class_install_property(
            gobject_class,
            PROP_POSITION_BYTES,
            g_param_spec_uint64(
                    "position_bytes",
                    "Byte position to seek to",
                    "Starting position in bytes relative to the beginning of the recording",
                    0, G_MAXUINT64, DEFAULT_POSITION_BYTES, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_BLOCKSIZE,
            g_param_spec_uint("blocksize", "Generated GstBuffer size",
                    "Size in bytes to read per buffer", MIN_BLOCKSIZE,
                    MAX_BLOCKSIZE, DEFAULT_BLOCKSIZE, G_PARAM_READWRITE));

    g_object_class_install_property(
            gobject_class,
            PROP_EOS_SLEEP_NSECS,
            g_param_spec_uint64(
                    "eos_sleep_nsecs",
                    "Time to wait for more data when EOS",
                    "Time in nanoseconds to sleep when end of stream is encountered while waiting for more data",
                    0, G_MAXUINT64, DEFAULT_EOS_SLEEP_NSECS, G_PARAM_READWRITE));

    g_object_class_install_property(
            gobject_class,
            PROP_TIMESTAMP_WITH_POSITION,
            g_param_spec_boolean(
                    "timestamp_with_position",
                    "Timestamp buffers with current time position",
                    "Timestamp buffers with current time position which is used for HN Server Content",
                    FALSE, G_PARAM_READWRITE));

    g_object_class_install_property(
            gobject_class,
            PROP_TEST_THREAD,
            g_param_spec_boolean(
                    "test_thread",
                    "Create a test thread",
                    "Create a thread for testing dynamic property changes on the source",
                    FALSE, G_PARAM_READWRITE));

    gobject_class->dispose = GST_DEBUG_FUNCPTR(gst_trick_play_file_src_dispose);
    gobject_class->finalize = GST_DEBUG_FUNCPTR(
            gst_trick_play_file_src_finalize);

    gstbasesrc_class->start = GST_DEBUG_FUNCPTR(gst_trick_play_file_src_start);
    gstbasesrc_class->stop = GST_DEBUG_FUNCPTR(gst_trick_play_file_src_stop);

    gstpushsrc_class->create
            = GST_DEBUG_FUNCPTR(gst_trick_play_file_src_create);
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_trick_play_file_src_init(GstTrickPlayFileSrc * src,
        GstTrickPlayFileSrcClass * gclass)
{
    guint i = 0;

    src->props_lock = g_mutex_new();

    src->filepath = DEFAULT_FILEPATH;
    src->filename = DEFAULT_FILENAME;
    src->filechanged = FALSE;
    src->framerate = DEFAULT_FRAMERATE;
    src->playrate = DEFAULT_PLAYRATE;
    src->playrate_changed = FALSE;
    src->position_time = INVALID_POSITION_TIME;
    src->position_bytes = INVALID_POSITION_BYTES;
    src->blocksize = DEFAULT_BLOCKSIZE;
    src->eos_sleep_nsecs = DEFAULT_EOS_SLEEP_NSECS;

    src->current_time = GST_CLOCK_TIME_NONE;
    src->current_bytes = 0;
    src->blocksize_adapter = gst_adapter_new();
    src->outgoing_timestamp = GST_CLOCK_TIME_NONE;
    src->outgoing_duration = GST_CLOCK_TIME_NONE;
    src->timestamp_with_position = FALSE;
    src->null_ts_packet = gst_buffer_new_and_alloc(TS_PACKET_SIZE);

    // Initialize our NULL TS packet.
    GST_BUFFER_DATA(src->null_ts_packet)[0] = 0x47;
    GST_BUFFER_DATA(src->null_ts_packet)[1] = 0x1F;
    GST_BUFFER_DATA(src->null_ts_packet)[2] = 0xFF;
    GST_BUFFER_DATA(src->null_ts_packet)[3] = 0x1F;
    for (i = 4; i < TS_PACKET_SIZE; i++)
    {
        GST_BUFFER_DATA(src->null_ts_packet)[i] = 0xFF;
    }

    src->ifs_handle = NULL;
    src->ifs_offset = IFS_UNDEFINED_CLOCK;
    src->end_offset = IFS_UNDEFINED_CLOCK;

    gst_base_src_set_live(GST_BASE_SRC(src), TRUE);
}

static void gst_trick_play_file_src_dispose(GObject * object)
{
    GstTrickPlayFileSrc *src = GST_TRICKPLAYFILESRC(object);
    g_object_unref(src->blocksize_adapter);
    src->blocksize_adapter = NULL;
    gst_buffer_unref(src->null_ts_packet);
    src->null_ts_packet = NULL;
    G_OBJECT_CLASS(parent_class)->dispose(object);
}

static void gst_trick_play_file_src_finalize(GObject * object)
{
    GstTrickPlayFileSrc *src = GST_TRICKPLAYFILESRC(object);

    g_mutex_free(src->props_lock);

    g_free(src->filepath);
    g_free(src->filename);

    G_OBJECT_CLASS(parent_class)->finalize(object);
}

static void gst_trick_play_file_src_set_property(GObject * object,
        guint prop_id, const GValue * value, GParamSpec * pspec)
{
    GstTrickPlayFileSrc *src = GST_TRICKPLAYFILESRC(object);

    switch (prop_id)
    {
    case PROP_FILEPATH:
    {
        const gchar* filepath = g_value_get_string(value);
        g_mutex_lock(src->props_lock);
        g_free(src->filepath);
        src->filepath = g_strdup(filepath);
        GST_INFO_OBJECT(src, "New filepath set: \"%s\".", src->filepath);
        src->filechanged = TRUE;
        g_mutex_unlock(src->props_lock);
        break;
    }
    case PROP_FILENAME:
    {
        const gchar* filename = g_value_get_string(value);
        g_mutex_lock(src->props_lock);
        g_free(src->filename);
        src->filename = g_strdup(filename);
        GST_INFO_OBJECT(src, "New filename set: \"%s\".", src->filename);
        src->filechanged = TRUE;
        g_mutex_unlock(src->props_lock);
        break;
    }

    case PROP_FRAMERATE:
        g_mutex_lock(src->props_lock);
        src->framerate = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "New framerate set: %u.", src->framerate);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PLAYRATE:
        g_mutex_lock(src->props_lock);
        const gfloat newRate = g_value_get_float(value);
        if (src->playrate != newRate)
        {
            src->playrate = newRate;
            src->playrate_changed = TRUE;
            GST_INFO_OBJECT(src, "New playrate set: %f.", src->playrate);
        }
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_POSITION_TIME:
        g_mutex_lock(src->props_lock);
        src->position_time = g_value_get_uint64(value);
        GST_INFO_OBJECT(src, "New time position set: %" GST_TIME_FORMAT ".",
                GST_TIME_ARGS(src->position_time));
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_POSITION_BYTES:
        g_mutex_lock(src->props_lock);
        src->position_bytes = g_value_get_uint64(value);
        GST_INFO_OBJECT(src, "New byte position set: %llu.",
                src->position_bytes);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_BLOCKSIZE:
        g_mutex_lock(src->props_lock);
        src->blocksize = g_value_get_uint(value);
        GST_INFO_OBJECT(src, "New blocksize set: %u.", src->blocksize);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_EOS_SLEEP_NSECS:
        g_mutex_lock(src->props_lock);
        src->eos_sleep_nsecs = g_value_get_uint64(value);
        GST_INFO_OBJECT(src, "New eos sleep nsecs set: %llu.",
                src->eos_sleep_nsecs);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_TIMESTAMP_WITH_POSITION:
        g_mutex_lock(src->props_lock);
        src->timestamp_with_position = g_value_get_boolean(value);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_TEST_THREAD:
        g_mutex_lock(src->props_lock);
        src->test_thread = g_value_get_boolean(value);
        if (src->test_thread == TRUE)
        {
            (void) g_thread_create(gst_trick_play_file_src_test_thread, src,
                    TRUE, NULL);
        }
        g_mutex_unlock(src->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_trick_play_file_src_get_property(GObject * object,
        guint prop_id, GValue * value, GParamSpec * pspec)
{
    GstTrickPlayFileSrc *src = GST_TRICKPLAYFILESRC(object);

    switch (prop_id)
    {
    case PROP_FILEPATH:
        g_mutex_lock(src->props_lock);
        g_value_set_string(value, src->filepath);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_FILENAME:
        g_mutex_lock(src->props_lock);
        g_value_set_string(value, src->filename);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_FRAMERATE:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->framerate);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PLAYRATE:
        g_mutex_lock(src->props_lock);
        g_value_set_float(value, src->playrate);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_POSITION_TIME:
        g_mutex_lock(src->props_lock);
        g_value_set_uint64(value, src->current_time);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_POSITION_BYTES:
        g_mutex_lock(src->props_lock);
        g_value_set_uint64(value, src->current_bytes);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_BLOCKSIZE:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->blocksize);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_EOS_SLEEP_NSECS:
        g_mutex_lock(src->props_lock);
        g_value_set_uint64(value, src->eos_sleep_nsecs);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_TIMESTAMP_WITH_POSITION:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->timestamp_with_position);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_TEST_THREAD:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->test_thread);
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
static gboolean gst_trick_play_file_src_start(GstBaseSrc * basesrc)
{
    GstTrickPlayFileSrc *src = GST_TRICKPLAYFILESRC(basesrc);
    gboolean retval = FALSE;

    g_mutex_lock(src->props_lock);
    retval = gst_trick_play_file_src_open(src);
    g_mutex_unlock(src->props_lock);

    return retval;
}

/* stop processing, ideal for closing the resource */
static gboolean gst_trick_play_file_src_stop(GstBaseSrc * basesrc)
{
    GstTrickPlayFileSrc *src = GST_TRICKPLAYFILESRC(basesrc);

    gst_trick_play_file_src_close(src);

    src->current_time = 0;
    src->current_bytes = 0;
    gst_adapter_clear(src->blocksize_adapter);
    src->outgoing_timestamp = GST_CLOCK_TIME_NONE;
    src->outgoing_duration = GST_CLOCK_TIME_NONE;

    src->ifs_handle = NULL;
    src->ifs_offset = IFS_UNDEFINED_CLOCK;
    src->end_offset = IFS_UNDEFINED_CLOCK;

    return TRUE;
}

/***********************************************/
/**********                           **********/
/********** GstPushSrc IMPLEMENTATION **********/
/**********                           **********/
/***********************************************/

/* ask the subclass to create a buffer */
static GstFlowReturn gst_trick_play_file_src_create(GstPushSrc *pushsrc,
        GstBuffer **buf)
{
    GstTrickPlayFileSrc *src = GST_TRICKPLAYFILESRC(pushsrc);
    GstFlowReturn retval = GST_FLOW_OK;
    guint remaining_bytes = gst_adapter_available(src->blocksize_adapter);
    gboolean trick_play_seek = FALSE;
    gboolean sendNewSeg = FALSE;

    g_mutex_lock(src->props_lock);

    // push the current frame out, even if the settings have changed
    // the new settings will be accounted for during next read/frame creation
    if (remaining_bytes == 0)
    {
        if (src->filechanged == TRUE)
        {
            GST_INFO_OBJECT(src,
                    "File settings change detected, opening new resource.");
            gst_trick_play_file_src_close(src);
            if (gst_trick_play_file_src_open(src) != TRUE)
            {
                retval = GST_FLOW_ERROR;
            }
            src->filechanged = FALSE;
        }

        if (src->ifs_handle == NULL)
        {
            guint i = 0;
            guint num_packets = src->blocksize / TS_PACKET_SIZE;
            for (i = 0; i < num_packets; i++)
            {
                (void) gst_buffer_ref(src->null_ts_packet);
                gst_adapter_push(src->blocksize_adapter, src->null_ts_packet);
            }
        }
        else
        {
            if (src->position_time != INVALID_POSITION_TIME
                    && src->position_bytes != INVALID_POSITION_BYTES)
            {
                GST_WARNING_OBJECT(src, "Both position_time and position_bytes properties have been changed: "
                        "will seek to the new time position of %" GST_TIME_FORMAT ".", GST_TIME_ARGS(src->position_time));
                src->position_bytes = INVALID_POSITION_BYTES;
            }

            if (src->position_time != INVALID_POSITION_TIME)
            {
                // Someone requested a new time position on us (via GObject property set)
                GST_INFO_OBJECT(src, "Attemping to execute requested time seek to %" GST_TIME_FORMAT ".",
                        GST_TIME_ARGS(src->position_time));
                retval = gst_trick_play_file_src_seek_to_time(src,
                        src->position_time);
                src->position_time = INVALID_POSITION_TIME;
                trick_play_seek = FALSE;
                sendNewSeg = TRUE;
            }
            else if (src->position_bytes != INVALID_POSITION_BYTES)
            {
                // Someone requested a new position_bytes on us (via GObject property set)
                GST_INFO_OBJECT(src,
                        "Attempting to execute requested byte seek to %llu.",
                        src->position_bytes);
                retval = gst_trick_play_file_src_seek_to_bytes(src,
                        src->position_bytes);
                src->position_bytes = INVALID_POSITION_BYTES;
                trick_play_seek = FALSE;
                sendNewSeg = TRUE;
            }
            else
            {
                trick_play_seek = TRUE;
            }

            // Check for normal playback rate
            if (src->playrate == 1.0)
            {
                // regular, smooth 1x playback or requested a new seek to time position so new seg needs to be sent
                if ((src->playrate_changed) || (sendNewSeg))
                {
                    // Clear playrate changed flag
                    src->playrate_changed = FALSE;

                    // We are switching from trick-play (buffers are timestamped)
                    // to 1x playback (timestamped with pipeline clock). Signal that to the
                    // downstream elements using the "applied_rate" property
                    // that we are no longer performing adjustments.
                    (void) gst_pad_push_event(GST_BASE_SRC_PAD(src),
                            gst_event_new_flush_start());
                    (void) gst_pad_push_event(GST_BASE_SRC_PAD(src),
                            gst_event_new_flush_stop());
                    (void) gst_pad_push_event(GST_BASE_SRC_PAD(src),
                            gst_event_new_new_segment_full(TRUE, 1.0, 1.0,
                                    GST_FORMAT_TIME, 0, -1, 0));

                    GST_INFO_OBJECT(src, "Switch to 1x playback");
                }

                // Set outgoing time and duration using pipeline clock.
                // Buffer timestamps are used by HN pipeline.
                if (src->timestamp_with_position)
                {
                    src->outgoing_timestamp = src->current_time;
                }
                else
                {
                    gst_trick_play_file_src_update_outgoing_timestamp(src);
                }
                retval = gst_trick_play_file_src_create_read(src);
            }
            else
            {
                // trick-mode playback
                if (src->playrate_changed)
                {
                    // Clear playrate changed flag
                     src->playrate_changed = FALSE;

                    // We are switching from 1x playback (no buffer timestamping)
                    // to trick play - need to start time-stamping the buffers
                    // with the current stream time (now - element base time)
                    // Also signal to the downstream elements that we are going
                    // to change the rate via the "applied_rate" property.

                    // GStreamer does not allow the applied_rate parameter to be set to 0.
                    // Pick something close to that number.
                    gdouble playrate = (src->playrate == 0.) ? (1.
                            / ((gdouble) PLAYRATE_PRECISION)) : (src->playrate);

                    (void) gst_pad_push_event(GST_BASE_SRC_PAD(src),
                            gst_event_new_flush_start());
                    (void) gst_pad_push_event(GST_BASE_SRC_PAD(src),
                            gst_event_new_flush_stop());
                    (void) gst_pad_push_event(GST_BASE_SRC_PAD(src),
                            gst_event_new_new_segment_full(TRUE, 1.0, playrate,
                                    GST_FORMAT_TIME, 0, -1, 0));

                    if (src->timestamp_with_position)
                    {
                        src->outgoing_timestamp = src->current_time;
                    }
                    else
                    {
                        gst_trick_play_file_src_update_outgoing_timestamp(src);
                    }

                    GST_INFO_OBJECT(src, "Switch from 1x playback to trick-play: buffer timestamps will"
                            " restart from %" GST_TIME_FORMAT ".", GST_TIME_ARGS(src->outgoing_timestamp));
                }
                retval = gst_trick_play_file_src_create_frame(src, trick_play_seek);
            }
        }
    }

    if (retval == GST_FLOW_OK)
    {
        remaining_bytes = gst_adapter_available(src->blocksize_adapter);
        if (remaining_bytes >= src->blocksize)
        {
            *buf = gst_adapter_take_buffer(src->blocksize_adapter, src->blocksize);
        }
        else
        {
            *buf = gst_adapter_take_buffer(src->blocksize_adapter, remaining_bytes);
        }
        GST_BUFFER_TIMESTAMP(*buf) = src->outgoing_timestamp;
        GST_BUFFER_DURATION(*buf) = src->outgoing_duration;
        gst_buffer_set_caps (*buf, GST_PAD_CAPS (GST_BASE_SRC_PAD (src)));
    }

    g_mutex_unlock (src->props_lock);

    if (retval == GST_FLOW_OK)
    {
        GST_LOG_OBJECT(src, "Pushing buffer %p, size %u, timestamp %" GST_TIME_FORMAT ", duration %"
                GST_TIME_FORMAT ".", *buf, GST_BUFFER_SIZE(*buf), GST_TIME_ARGS(GST_BUFFER_TIMESTAMP(*buf)),
                GST_TIME_ARGS(GST_BUFFER_DURATION(*buf)));
    }

    return retval;
}

/********************************************************/
/**********                                    **********/
/********** GstTrickPlayFileSrc IMPLEMENTATION **********/
/**********                                    **********/
/********************************************************/

/* Should be called with prop_lock held. */
static gboolean gst_trick_play_file_src_open(GstTrickPlayFileSrc * src)
{
    IfsReturnCode val = IfsReturnCodeNoErrorReported;
    IfsInfo *info = NULL;

    if (src->filename == NULL || strncmp(src->filename, "", 1) == 0)
    {
        GST_INFO_OBJECT(src,
                "Empty filename - TrickPlayFileSrc will generate NULL TS packets"
                    " in PLAYING state.");
        return TRUE;
    }

    if (src->ifs_handle != NULL)
    {
        GST_ERROR_OBJECT(src, "ifs_handle is not NULL?!");
        return FALSE;
    }

    val = IfsOpenReader(src->filepath, // const char * path      Input
            src->filename, // const char * name      Input
            &src->ifs_handle); // IfsHandle * pIfsHandle Output (use IfsClose() to free)

    if (val != IfsReturnCodeNoErrorReported)
    {
        GST_ERROR_OBJECT(
                src,
                "Problem while opening IFS path \"%s\", IFS name \"%s\": IfsOpenReader returned %s.",
                (src->filepath) ? src->filepath : "NULL",
                (src->filename) ? src->filename : "NULL",
                IfsReturnCodeToString(val));
        return FALSE;
    }

    GST_INFO_OBJECT(src, "IfsOpenReader succeeded, handle: %p.",
            src->ifs_handle);

    if (src->ifs_offset != IFS_UNDEFINED_CLOCK)
    {
        GST_WARNING_OBJECT(src, "ifs_offset is not IFS_UNDEFINED_CLOCK.");
    }

    val = IfsHandleInfo(src->ifs_handle, &info);
    if (val != IfsReturnCodeNoErrorReported)
    {
        GST_ERROR_OBJECT(
                src,
                "Error obtaining info for IFS handle %p, IfsHandleInfo returned  %s.",
                src->ifs_handle, IfsReturnCodeToString(val));
        return FALSE;
    }

    src->ifs_offset = info->begClock;

    // Check to see if end offset has been set to a valid value, if it has it means an EOS was encountered
    // and need to start playback from the end of the file
    if (src->end_offset != IFS_UNDEFINED_CLOCK)
    {
        GST_WARNING_OBJECT(src,
                "calling IfsSeekToTime using end_offset since it is not IFS_UNDEFINED_CLOCK.");
        val = IfsSeekToTime(src->ifs_handle, IfsDirectEnd, &src->end_offset,
                NULL);
        if (val != IfsReturnCodeNoErrorReported)
        {
            GST_WARNING_OBJECT        (src, "Initial IfsSeek to end position %" GST_TIME_FORMAT " returned %s for IFS handle %p.",
                    GST_TIME_ARGS(src->ifs_offset), IfsReturnCodeToString(val), src->ifs_handle);
        }
        src->end_offset = IFS_UNDEFINED_CLOCK;
    }
    else
    {
        GST_WARNING_OBJECT(src, "ignoring end_offset");

        GST_INFO_OBJECT(src, "calling IfsSeekToTime IFS Recording: path \"%s\", name \"%s\", start %" GST_TIME_FORMAT ", end %" GST_TIME_FORMAT ".",
                info->path, info->name, GST_TIME_ARGS(info->begClock), GST_TIME_ARGS(info->endClock));

        val = IfsSeekToTime(src->ifs_handle, IfsDirectEither, &src->ifs_offset, NULL);
        if (val != IfsReturnCodeNoErrorReported)
        {
            GST_WARNING_OBJECT(src, "Initial IfsSeek to absolute position %" GST_TIME_FORMAT " returned %s for IFS handle %p.",
                    GST_TIME_ARGS(src->ifs_offset), IfsReturnCodeToString(val), src->ifs_handle);
        }
    }

    val = IfsFreeInfo(info);
    if (val != IfsReturnCodeNoErrorReported)
    {
        GST_WARNING_OBJECT(src, "IfsFreeInfo returned %s for IFS handle %p.",
                IfsReturnCodeToString(val), src->ifs_handle);
    }

    src->current_time = 0;
    src->current_bytes = 0;
    g_assert(gst_adapter_available(src->blocksize_adapter) == 0);
    src->outgoing_timestamp = GST_CLOCK_TIME_NONE;
    src->outgoing_duration = GST_CLOCK_TIME_NONE;

    return TRUE;
}

static void gst_trick_play_file_src_close(GstTrickPlayFileSrc *src)
{
    if (src->ifs_handle != NULL)
    {
        IfsReturnCode val = IfsReturnCodeNoErrorReported;

        GST_INFO_OBJECT(src, "Non-NULL IFS handle %p, closing.",
                src->ifs_handle);
        val = IfsClose(src->ifs_handle);
        if (val != IfsReturnCodeNoErrorReported)
        {
            GST_WARNING_OBJECT(src, "IfsClose returned %s for IFS handle %p.",
                    IfsReturnCodeToString(val), src->ifs_handle);
        }
        else
        {
            GST_INFO_OBJECT(src, "IfsClose succeeded for handle %p.",
                    src->ifs_handle);
        }

        src->current_time = 0;
        src->current_bytes = 0;
        gst_adapter_clear(src->blocksize_adapter);
        src->outgoing_timestamp = GST_CLOCK_TIME_NONE;
        src->outgoing_duration = GST_CLOCK_TIME_NONE;

        src->ifs_handle = NULL;
    }
    else
    {
        GST_WARNING_OBJECT(src, "IFS handle already NULL.");
    }
}

/**
 * Updates the outgoing timestamp using the pipeline clock and base time
 * to determine value.
 */
static void gst_trick_play_file_src_update_outgoing_timestamp(GstTrickPlayFileSrc *src)
{
    GstClock * pClock = gst_element_get_clock(GST_ELEMENT(src));
    if (pClock == NULL)
    {
        src->outgoing_timestamp = 0;
    }
    else
    {
        GstClockTime now = gst_clock_get_time(pClock);
        GstClockTime base_time = gst_element_get_base_time(
                GST_ELEMENT(src));
        src->outgoing_timestamp = now - base_time;

        gst_object_unref(pClock);
    }
}

/* Create and return a frame based on the current IFS position.
 props_lock should be held when calling this function. */
static GstFlowReturn gst_trick_play_file_src_create_frame(
        GstTrickPlayFileSrc *src, gboolean calculate_new_position)
{
    GstClockTime duration = GST_CLOCK_TIME_NONE;
    GstFlowReturn retval = GST_FLOW_OK;

    GST_DEBUG_OBJECT(src, "Framerate: %u, Playrate: %f, Time position: %" GST_TIME_FORMAT
            ", Byte position: %llu.", src->framerate, src->playrate, GST_TIME_ARGS(src->current_time),
            src->current_bytes);

    duration = gst_util_uint64_scale(GST_SECOND, 1, src->framerate);
    if (calculate_new_position == TRUE)
    {
        // No pending seek, we have to figure out where to go next
        // based on our current time and duration/playrate settings
        gdouble multiplier = (src->playrate < 0.) ? (0. - src->playrate)
                : (src->playrate);
        GstClockTime jump = gst_util_uint64_scale(((guint64)(multiplier
                * ((gdouble) PLAYRATE_PRECISION) * ((gdouble) duration))), 1,
                PLAYRATE_PRECISION);
        GstClockTime seek = GST_CLOCK_TIME_NONE;

        if (src->playrate > 0.)
        {
             seek = src->current_time + jump;
             GST_DEBUG_OBJECT(src, "Seeking forward, jump %" GST_TIME_FORMAT ", to time %" GST_TIME_FORMAT ".",
                       GST_TIME_ARGS(jump), GST_TIME_ARGS(seek));
        }
        else // if (src->playrate < 0.)
        {
            GST_DEBUG_OBJECT(src, "Seeking backwards %" GST_TIME_FORMAT ".", GST_TIME_ARGS(jump));
            seek = src->current_time - jump;
        }

        retval = gst_trick_play_file_src_seek_to_time(src, seek);
    }

    if (retval == GST_FLOW_OK)
    {
        IfsReturnCode val = IfsReturnCodeNoErrorReported;
        NumPackets ifs_packets = IFS_UNDEFINED_PACKET;
        IfsPacket *ifs_frame = NULL;

        val = IfsReadNearestPicture(src->ifs_handle, 0, 0, &ifs_packets,
                &ifs_frame);
        if (val != IfsReturnCodeNoErrorReported)
        {
            GST_ERROR_OBJECT(src, "Error reading nearest frame at IFS position %" GST_TIME_FORMAT "."
                    "IfsReadNearestPicture returned %s.", GST_TIME_ARGS(src->ifs_offset + src->current_time),
                    IfsReturnCodeToString(val));
            retval = GST_FLOW_ERROR;
        }
        else
        {
            GstBuffer *buf = gst_buffer_new();

            GST_BUFFER_DATA(buf) = (guint8*) ifs_frame;
            GST_BUFFER_SIZE(buf) = IFS_TRANSPORT_PACKET_SIZE * ifs_packets;
            GST_BUFFER_MALLOCDATA(buf) = GST_BUFFER_DATA(buf);
            GST_BUFFER_FREE_FUNC(buf) = free;

            if (src->timestamp_with_position)
            {
                src->outgoing_timestamp = src->current_time;
            }
            else
            {
                src->outgoing_timestamp += duration;
            }
            src->outgoing_duration = duration;

            g_assert(gst_adapter_available(src->blocksize_adapter) == 0);
            gst_adapter_push(src->blocksize_adapter, buf);
        }
    }

    return retval;
}

/* Create and return a buffer for non-trick mode 1x-playback
 based on the current IFS position and blocksize setting.
 props_lock should be held when calling this function. */
static GstFlowReturn gst_trick_play_file_src_create_read(
        GstTrickPlayFileSrc *src)
{
    IfsReturnCode val = IfsReturnCodeNoErrorReported;
    NumPackets ifs_packets;
    IfsPacket *ifs_frame = NULL;
    IfsClock ifs_clock = IFS_UNDEFINED_CLOCK;
    GstBuffer *buf = NULL;

    if (src->playrate == DEFAULT_PLAYRATE && src->framerate
            != DEFAULT_FRAMERATE)
    {
        GST_WARNING_OBJECT(src, "Ignoring framerate setting in 1.0x playback.");
    }

    do
    {
        ifs_packets = src->blocksize / IFS_TRANSPORT_PACKET_SIZE;
        val = IfsRead(src->ifs_handle, &ifs_packets, &ifs_clock, &ifs_frame);
        if (val != IfsReturnCodeNoErrorReported)
        {
            if (val == IfsReturnCodeReadPastEndOfFile)
            {
                // Determine if this is a "real" EOS based on if we still have an active writer
                IfsBoolean isStillWriting = IfsHasWriter(src->ifs_handle);
                if (isStillWriting)
                {
                    // Sleep for a short time to determine if this is a real EOS or more data is available
                    GST_LOG_OBJECT(src, "writing, sleeping waiting for data: %llu\n", src->eos_sleep_nsecs);
                    g_usleep(src->eos_sleep_nsecs);
                    ifs_packets = 0;
                }
                else
                {
                    GST_INFO_OBJECT(src,
                        "Attempt to read past the end of the file, generating EOS.");
                    gst_trick_play_file_src_reset(src);
                    return GST_FLOW_UNEXPECTED;
                }
            }
            else
            {
                GST_ERROR_OBJECT(src,
                        "Error reading %u bytes at IFS byte position %llu. "
                            "IfsRead returned %s.", src->blocksize,
                        src->current_bytes, IfsReturnCodeToString(val));

                gst_trick_play_file_src_reset(src);
                return GST_FLOW_ERROR;
            }
        }
        GST_LOG_OBJECT(src, "ifs_packets = %lu\n", ifs_packets);

    } while (0 == ifs_packets);

    buf = gst_buffer_new();
    GST_BUFFER_DATA(buf) = (guint8*) ifs_frame;
    GST_BUFFER_SIZE(buf) = IFS_TRANSPORT_PACKET_SIZE * ifs_packets;
    GST_BUFFER_MALLOCDATA(buf) = GST_BUFFER_DATA(buf);
    GST_BUFFER_FREE_FUNC(buf) = free;

    GST_LOG_OBJECT(src, "Read %u bytes (requested %u).", GST_BUFFER_SIZE(buf),
            src->blocksize);

    g_assert(gst_adapter_available(src->blocksize_adapter) == 0);
    gst_adapter_push(src->blocksize_adapter, buf);

    src->current_time = ifs_clock - src->ifs_offset;
    src->current_bytes += GST_BUFFER_SIZE(buf);

    return GST_FLOW_OK;
}

static GstFlowReturn gst_trick_play_file_src_seek_to_time(
        GstTrickPlayFileSrc *src, GstClockTime gstClockTime)
{
    IfsReturnCode val = IfsReturnCodeNoErrorReported;
    IfsClock ifs_seek = src->ifs_offset + gstClockTime;
    NumPackets ifs_pkt = IFS_UNDEFINED_PACKET;

    // Work-around for OCORI-1604.
    IfsInfo *info = NULL;
    (void) IfsHandleInfo(src->ifs_handle, &info);
    (void) IfsFreeInfo(info);

    val = IfsSeekToTime(src->ifs_handle, IfsDirectEither, &ifs_seek, &ifs_pkt);
    if (val != IfsReturnCodeNoErrorReported)
    {
        if (val == IfsReturnCodeSeekOutsideFile)
        {
            GST_INFO_OBJECT(src,
                    "Attempt to seek outside of the file, generating EOS at clock time %" GST_TIME_FORMAT ".",
                    GST_TIME_ARGS(gstClockTime));
            gst_trick_play_file_src_reset(src);
            return GST_FLOW_UNEXPECTED;
        }
        else
        {
            GST_ERROR_OBJECT(src, "Error seeking to IFS position %" GST_TIME_FORMAT " (%"
                    GST_TIME_FORMAT " relative to the start of recording). IfsSeek returned %s.",
                    GST_TIME_ARGS(ifs_seek), GST_TIME_ARGS(gstClockTime), IfsReturnCodeToString(val));
            gst_trick_play_file_src_reset(src);
            return GST_FLOW_ERROR;
        }
    }

    src->current_time = gstClockTime;
    src->current_bytes = ifs_pkt * (guint64) IFS_TRANSPORT_PACKET_SIZE;

    GST_DEBUG_OBJECT(src, "IfsSeekToTime succeeded, new time position %" GST_TIME_FORMAT
            ", new byte position %llu.", GST_TIME_ARGS(src->current_time), src->current_bytes);

    return GST_FLOW_OK;
}

static GstFlowReturn gst_trick_play_file_src_seek_to_bytes(
        GstTrickPlayFileSrc *src, guint64 bytes)
{
    IfsReturnCode val = IfsReturnCodeNoErrorReported;
    NumPackets ifs_seek = bytes / IFS_TRANSPORT_PACKET_SIZE;
    IfsClock ifs_clock = IFS_UNDEFINED_CLOCK;

    val = IfsSeekToPacket(src->ifs_handle, ifs_seek, &ifs_clock);
    if (val != IfsReturnCodeNoErrorReported)
    {
        if (val == IfsReturnCodeSeekOutsideFile)
        {
            GST_INFO_OBJECT(src,
                    "Attempt to seek outside of the file, generating EOS.");
            return GST_FLOW_UNEXPECTED;
        }
        else
        {
            GST_ERROR_OBJECT(src,
                    "Error seeking to IFS packet %lu. IfsSeek returned %s.",
                    ifs_seek, IfsReturnCodeToString(val));
            return GST_FLOW_ERROR;
        }
    }

    src->current_time = ifs_clock - src->ifs_offset;
    src->current_bytes = bytes;

    GST_DEBUG_OBJECT(src, "IfsSeekToPacket succeeded, new time position %" GST_TIME_FORMAT
            ", new byte position %llu.", GST_TIME_ARGS(src->current_time), src->current_bytes);

    return GST_FLOW_OK;
}

static gpointer gst_trick_play_file_src_test_thread(
        gpointer user_data_trick_play_file_src)
{
    GstTrickPlayFileSrc *src = NULL;
    g_return_val_if_fail(
            GST_IS_TRICKPLAYFILESRC(user_data_trick_play_file_src), NULL);
    src = GST_TRICKPLAYFILESRC(user_data_trick_play_file_src);

    GST_INFO_OBJECT(src, "a-HA!");

    return NULL;
}

void gst_trick_play_file_src_reset(GstTrickPlayFileSrc *src)
{
    GST_INFO_OBJECT(src, "resetting");

    IfsInfo *info = NULL;
    IfsReturnCode val = IfsReturnCodeNoErrorReported;

    // If playing forward and we've got an EOS, save the last valid position
    if (src->playrate > 0.)
    {
        GST_INFO_OBJECT(src,
                "Was seeking forward with playrate %f, but now at end of file",
                src->playrate);

        // Save the end clock which is the last valid offset in the file
        val = IfsHandleInfo(src->ifs_handle, &info);
        if (val != IfsReturnCodeNoErrorReported)
        {
            GST_ERROR_OBJECT(
                    src,
                    "Error obtaining info for IFS handle %p, IfsHandleInfo returned  %s.",
                    src->ifs_handle, IfsReturnCodeToString(val));
            return;
        }

        GST_INFO_OBJECT(src,
                "Setting end offset to be used by IfsSeekTime() when restarted");
        src->end_offset = info->endClock;

        val = IfsFreeInfo(info);
        if (val != IfsReturnCodeNoErrorReported)
        {
            GST_WARNING_OBJECT(src,
                    "IfsFreeInfo returned %s for IFS handle %p.",
                    IfsReturnCodeToString(val), src->ifs_handle);
        }
    }
    else
    {
        src->current_time = 0;
    }

    // Reset all the parameter values

    src->current_bytes = 0;
    gst_adapter_clear(src->blocksize_adapter);
    src->outgoing_timestamp = GST_CLOCK_TIME_NONE;
    src->outgoing_duration = GST_CLOCK_TIME_NONE;
}
