/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2008 U-HATHORRyan <<user@hostname.org>>
 * Copyright (C) 2009 Cable Television Laboratories, Inc.
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
 * SECTION:element-passthru
 *
 * FIXME:Describe passthru here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! passthru ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>
#include <glib/gprintf.h>
#include <string.h>

#include "gstpassthru.h"

GST_DEBUG_CATEGORY_STATIC ( gst_pass_thru_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_pass_thru_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0, PROP_SILENT
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE("sink",
        GST_PAD_SINK, GST_PAD_ALWAYS, GST_STATIC_CAPS("ANY"));

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC, GST_PAD_ALWAYS, GST_STATIC_CAPS("ANY"));

/*lint -e(123)*/GST_BOILERPLATE (GstPassThru, gst_pass_thru, GstElement, GST_TYPE_ELEMENT)

static void gst_pass_thru_set_property (GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec);
static void gst_pass_thru_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec);

static gboolean gst_pass_thru_set_caps(GstPad * pad, GstCaps * caps);
static GstFlowReturn gst_pass_thru_chain(GstPad * pad, GstBuffer * buf);
static gboolean gst_pass_thru_sink_event(GstPad *pad, GstEvent *event);
static gboolean gst_pass_thru_src_event(GstPad *pad, GstEvent *event);
static GstStateChangeReturn gst_pass_thru_change_state(GstElement *element,
        GstStateChange transition);
/* GObject vmethod implementations */

static void gst_pass_thru_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "PassThru",
            "FIXME:Generic", "FIXME:Generic Template Element",
            "U-HATHORRyan <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the passthru's class */
static void gst_pass_thru_class_init(GstPassThruClass * klass)
{
    GObjectClass *gobject_class;
    GstElementClass *gstelement_class;

    GST_DEBUG_CATEGORY_INIT(gst_pass_thru_debug, "passthru", 0,
            "Template passthru");

    gobject_class = (GObjectClass *) klass;
    gstelement_class = (GstElementClass *) klass;

    gobject_class->set_property = gst_pass_thru_set_property;
    gobject_class->get_property = gst_pass_thru_get_property;
    gstelement_class->change_state = gst_pass_thru_change_state;

    g_object_class_install_property(gobject_class, PROP_SILENT,
            g_param_spec_boolean("silent", "Silent",
                    "Produce verbose output ?", FALSE, G_PARAM_READWRITE));
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_pass_thru_init(GstPassThru * filter, GstPassThruClass * gclass)
{
    filter->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    gst_pad_set_setcaps_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_pass_thru_set_caps));
    gst_pad_set_getcaps_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_pad_proxy_getcaps));
    gst_pad_set_chain_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_pass_thru_chain));
    gst_pad_set_event_function(filter->sinkpad, gst_pass_thru_sink_event);

    filter->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    gst_pad_set_getcaps_function(filter->srcpad, GST_DEBUG_FUNCPTR(
            gst_pad_proxy_getcaps));
    gst_pad_set_event_function(filter->srcpad, gst_pass_thru_src_event);

    gst_element_add_pad(GST_ELEMENT(filter), filter->sinkpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->srcpad);
    filter->silent = FALSE;

    //gst_debug_set_threshold_for_name("passthru", GST_LEVEL_INFO);

}

static void gst_pass_thru_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec)
{
    GstPassThru *filter = GST_PASSTHRU(object);

    switch (prop_id)
    {
    case PROP_SILENT:
        filter->silent = g_value_get_boolean(value);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_pass_thru_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec)
{
    GstPassThru *filter = GST_PASSTHRU(object);

    switch (prop_id)
    {
    case PROP_SILENT:
        g_value_set_boolean(value, filter->silent);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/* GstElement vmethod implementations */

/* this function handles the link with other elements */
static gboolean gst_pass_thru_set_caps(GstPad * pad, GstCaps * caps)
{
    GstPassThru *filter;
    GstPad *otherpad;

    filter = GST_PASSTHRU(gst_pad_get_parent(pad));
    otherpad = (pad == filter->srcpad) ? filter->sinkpad : filter->srcpad;
    gst_object_unref(filter);

    return gst_pad_set_caps(otherpad, caps);
}

void gst_buffer_hexdump(GstObject *object, GstBuffer *buf)
{
#define DUMP_BYTES_PER_LINE 16
#define DUMP_CHARS_PER_BYTE  3
#define BYTE_LINE_LENGTH    (DUMP_BYTES_PER_LINE * DUMP_CHARS_PER_BYTE)

    guint index = 0;
    guint offset = 0;
    GstDebugLevel level;

    gchar buffer[BYTE_LINE_LENGTH + 1];

    /* Batch up the level check, or else the filter is always too slow */
    level = gst_debug_category_get_threshold(gst_pass_thru_debug);
    if (level >= GST_LEVEL_DEBUG)
    {
        GST_DEBUG_OBJECT(object, "GstBuffer %p:", buf);

        while (index < GST_BUFFER_SIZE(buf))
        {
            g_snprintf(&buffer[offset], DUMP_CHARS_PER_BYTE + 1, "%02X ",
                    GST_BUFFER_DATA(buf)[index]);
            index++;
            offset = (offset + DUMP_CHARS_PER_BYTE) % BYTE_LINE_LENGTH;
            if (offset == 0)
            {
                GST_DEBUG_OBJECT(object, "%s", buffer);
            }
        }

        if (offset != 0)
        {
            buffer[offset] = '\0';
            GST_DEBUG_OBJECT(object, "%s", buffer);
        }
    }

#undef BYTE_LINE_LENGTH
#undef DUMP_CHARS_PER_BYTE
#undef DUMP_BYTES_PER_LINE
}

/* chain function
 * this function does the actual processing
 */
static GstFlowReturn gst_pass_thru_chain(GstPad * pad, GstBuffer * buf)
{
    GstPassThru *filter;

    filter = GST_PASSTHRU(GST_OBJECT_PARENT(pad));

    //if (filter->silent == FALSE)
    //  g_print ("I'm plugged, therefore I'm in.\n");

    if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
    {
        GstClockTime buftime = GST_BUFFER_TIMESTAMP(buf);
        GST_LOG_OBJECT(filter, "Buffer Timestamp = %lld ns", buftime);

    }
    if (GST_BUFFER_DURATION_IS_VALID(buf))
    {
        GstClockTime buftime = GST_BUFFER_DURATION(buf);
        GST_LOG_OBJECT(filter, "Buffer duration = %lld ns", buftime);

    }
    gst_buffer_hexdump(GST_OBJECT(filter), buf);

    /* just push out the incoming buffer without touching it */
    return gst_pad_push(filter->srcpad, buf);
}

static gboolean gst_pass_thru_sink_event(GstPad *pad, GstEvent *event)
{
    GstPassThru *filter;

    filter = GST_PASSTHRU(GST_OBJECT_PARENT(pad));

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_EOS:
        /* end-of-stream, we should close down all stream leftovers here */
        GST_INFO_OBJECT(filter, "Received EOS event");
        break;
    case GST_EVENT_NEWSEGMENT:
        GST_INFO_OBJECT(filter, "Received NEWSEGMENT event");
        {
            gboolean update;
            gdouble rate;
            GstFormat format;
            gint64 start;
            gint64 stop;
            gint64 position;
            gst_event_parse_new_segment(event, &update, &rate, &format, &start,
                    &stop, &position);
            switch (format)
            {
            case GST_FORMAT_UNDEFINED:
                GST_INFO_OBJECT(filter, "FORMAT_UNDEFINED");
                break;
            case GST_FORMAT_DEFAULT:
                GST_INFO_OBJECT(filter, "FORMAT_DEFAULT");
                break;
            case GST_FORMAT_BYTES:
                GST_INFO_OBJECT(filter, "FORMAT_BYTES");
                break;
            case GST_FORMAT_TIME:
                GST_INFO_OBJECT(
                        filter,
                        "FORMAT_TIME, start %lld ns, stop %lld ns, pos %lld ns",
                        start, stop, position);
                break;
            case GST_FORMAT_BUFFERS:
                GST_INFO_OBJECT(filter, "FORMAT_BUFFERS");
                break;
            case GST_FORMAT_PERCENT:
                GST_INFO_OBJECT(filter, "FORMAT_PERCENT");
                break;
            default:
                GST_INFO_OBJECT(filter, "unrecognized format (%d)", format);
                break;
            }

        }
        break;
    case GST_EVENT_FLUSH_START:
        GST_INFO_OBJECT(filter, "Received FLUSH_START event");
        break;
    case GST_EVENT_FLUSH_STOP:
        GST_INFO_OBJECT(filter, "Received FLUSH_STOP event");
        break;
    default:
        GST_LOG_OBJECT(filter, "Received other (0x%X) event.", GST_EVENT_TYPE(
                event));
        break;
    }

    return gst_pad_event_default(pad, event);
} /* gst_pass_thru_sink_event */

static gboolean gst_pass_thru_src_event(GstPad *pad, GstEvent *event)
{
    GstPassThru *filter;

    filter = GST_PASSTHRU(GST_OBJECT_PARENT(pad));

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_QOS:
        GST_LOG_OBJECT(filter, "Received QOS event");
        {
            gdouble proportion;
            GstClockTimeDiff diff;
            GstClockTime timestamp;
            gst_event_parse_qos(event, &proportion, &diff, &timestamp);
            GST_LOG_OBJECT(filter, "Proportion %f, diff %lld, time %llu",
                    proportion, diff, timestamp);

        }
        break;
    case GST_EVENT_SEEK:
        GST_INFO_OBJECT(filter, "Received SEEK event");
        break;
    case GST_EVENT_NAVIGATION:
        GST_INFO_OBJECT(filter, "Received NAVIGATION event");
        break;
    case GST_EVENT_LATENCY:
        GST_INFO_OBJECT(filter, "Received LATENCY event");
        break;
    case GST_EVENT_FLUSH_START:
        GST_INFO_OBJECT(filter, "Received FLUSH_START event");
        break;
    case GST_EVENT_FLUSH_STOP:
        GST_INFO_OBJECT(filter, "Received FLUSH_STOP event");
        break;
    default:
        GST_INFO_OBJECT(filter, "Received other (0x%X) event", GST_EVENT_TYPE(
                event));
        break;
    }

    return gst_pad_event_default(pad, event);
}

static GstStateChangeReturn gst_pass_thru_change_state(GstElement *element,
        GstStateChange transition)
{
    GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;
    GstPassThru *filter = GST_PASSTHRU(element);
    gchar *str;

    switch (transition)
    {
    case GST_STATE_CHANGE_NULL_TO_READY:
        str = "NULL to READY";
        break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
        str = "READY to PAUSED";
        break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
        str = "PAUSED to PLAYING";
        break;
    default:
        str = NULL;
        break;
    }

    if (str)
    {
        GST_INFO_OBJECT(filter, "Received %s state change", str);
    }

    //    ret = (GST_ELEMENT_CLASS (filter->gclass->parent_class))->change_state (element, transition);
    //    ret = GST_ELEMENT_CLASS(filter->gclass->parent_class).change_state(element, transition);
    ret = GST_ELEMENT_CLASS(parent_class)->change_state(element, transition);
    if (ret == GST_STATE_CHANGE_FAILURE)
    {
        GST_INFO_OBJECT(filter, "State change in parent class failed");
        return ret;
    }

    switch (transition)
    {
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
        str = "PLAYING to PAUSED";
        break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
        str = "PAUSED to READY";
        break;
    case GST_STATE_CHANGE_READY_TO_NULL:
        str = "READY to NULL";
        break;
    default:
        str = NULL;
        break;
    }

    if (str)
    {
        GST_INFO_OBJECT(filter, "Received %s state change", str);
    }

    return ret;
}

