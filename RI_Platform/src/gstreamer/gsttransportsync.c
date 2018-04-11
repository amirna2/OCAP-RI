/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
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
 * SECTION:element-transportsync
 *
 * FIXME:Describe transportsync here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! transportsync ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "gsttransportsync.h"

GST_DEBUG_CATEGORY_STATIC ( gst_transport_sync_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_transport_sync_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0, PROP_SILENT, PROP_SYNC_BUFFER_SIZE
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE("sink",
        GST_PAD_SINK, GST_PAD_ALWAYS, GST_STATIC_CAPS("ANY"));

static GstStaticPadTemplate
        src_factory =
                GST_STATIC_PAD_TEMPLATE(
                        "src",
                        GST_PAD_SRC,
                        GST_PAD_ALWAYS,
                        GST_STATIC_CAPS(
                                "video/mpegts," "packetsize=(int)188," "systemstream=(boolean)true"));

/*lint -e(123)*/GST_BOILERPLATE (GstTransportSync, gst_transport_sync, GstElement, GST_TYPE_ELEMENT)

static void gst_transport_sync_set_property (GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec);
static void gst_transport_sync_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec);

static gboolean gst_transport_sync_set_caps(GstPad * pad, GstCaps * caps);
static GstFlowReturn gst_transport_sync_chain(GstPad * pad, GstBuffer * buf);

/* GObject vmethod implementations */

static void gst_transport_sync_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "TransportSync",
            "FIXME:Generic", "FIXME:Generic Template Element",
            "U-PRESTOMarcin <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the transportsync's class */
static void gst_transport_sync_class_init(GstTransportSyncClass * klass)
{
    GObjectClass *gobject_class;

    GST_DEBUG_CATEGORY_INIT(gst_transport_sync_debug, "transportsync", 0,
            "MPEG-2 Transport Stream Packet Synchronizer");

    gobject_class = (GObjectClass *) klass;

    gobject_class->set_property = gst_transport_sync_set_property;
    gobject_class->get_property = gst_transport_sync_get_property;

    g_object_class_install_property(gobject_class, PROP_SILENT,
            g_param_spec_boolean("silent", "Silent",
                    "Produce verbose output ?", FALSE, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_SYNC_BUFFER_SIZE,
            g_param_spec_uint("sync-buffer-size", "Sync Buffer Size",
                    "Specify sync buffer size to use", 2 * TS_PACKET_SIZE,
                    G_MAXUINT16, DEFAULT_SYNC_BUFFER_SIZE, G_PARAM_READWRITE
                            | G_PARAM_CONSTRUCT));

    GST_DEBUG_CATEGORY_INIT(gst_transport_sync_debug, "transport_sync", 0,
            "MPEG Transport Stream synchronizer");
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_transport_sync_init(GstTransportSync * filter,
        GstTransportSyncClass * gclass)
{
    filter->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    gst_pad_set_setcaps_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_transport_sync_set_caps));
    gst_pad_set_getcaps_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_pad_proxy_getcaps));
    gst_pad_set_chain_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_transport_sync_chain));

    filter->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    gst_pad_set_getcaps_function(filter->srcpad, GST_DEBUG_FUNCPTR(
            gst_pad_proxy_getcaps));

    gst_element_add_pad(GST_ELEMENT(filter), filter->sinkpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->srcpad);
    filter->silent = FALSE;

    filter->adapter = gst_adapter_new();
    filter->state = STATE_LOOKING_FOR_SYNC;
}

static void gst_transport_sync_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec)
{
    GstTransportSync *filter = GST_TRANSPORTSYNC(object);

    switch (prop_id)
    {
    case PROP_SILENT:
        filter->silent = g_value_get_boolean(value);
        break;
    case PROP_SYNC_BUFFER_SIZE:
        filter->sync_buffer_size = g_value_get_uint(value);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_transport_sync_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec)
{
    GstTransportSync *filter = GST_TRANSPORTSYNC(object);

    switch (prop_id)
    {
    case PROP_SILENT:
        g_value_set_boolean(value, filter->silent);
        break;
    case PROP_SYNC_BUFFER_SIZE:
        g_value_set_uint(value, filter->sync_buffer_size);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/* GstElement vmethod implementations */

/* this function handles the link with other elements */
static gboolean gst_transport_sync_set_caps(GstPad * pad, GstCaps * caps)
{
    GstTransportSync *filter;
    GstPad *otherpad;

    filter = GST_TRANSPORTSYNC(gst_pad_get_parent(pad));
    otherpad = (pad == filter->srcpad) ? filter->sinkpad : filter->srcpad;
    gst_object_unref(filter);

    return gst_pad_set_caps(otherpad, caps);
}

//
//
//
//
//
// START MY CODE
//
//
//
//
//

static gboolean handle_state_looking_for_sync(GstTransportSync *filter)
{
    GstAdapter *adapter = filter->adapter;
    guint sync_buffer_size = filter->sync_buffer_size;
    gboolean done = FALSE;

    if (gst_adapter_available(adapter) < sync_buffer_size)
    {
        done = TRUE;
    }
    else
    {
        guint position_index = 0;
        guint packets_to_inspect = sync_buffer_size / TS_PACKET_SIZE;
        gboolean found_sync = FALSE;
        const guint8 *peek_buffer = gst_adapter_peek(adapter, sync_buffer_size);

        while (!found_sync && position_index < TS_PACKET_SIZE)
        {
            guint packet_index = 0;
            while (!found_sync && packet_index < packets_to_inspect)
            {
                if (peek_buffer[position_index
                        + (packet_index * TS_PACKET_SIZE)] != TS_SYNC_BYTE)
                {
                    position_index++;
                    break;
                }
                else
                {
                    packet_index++;
                    if (packet_index == packets_to_inspect)
                    {
                        found_sync = TRUE;
                    }
                }
            }
        }

        if (found_sync)
        {
            gst_adapter_flush(adapter, position_index);
            filter->state = STATE_SYNCHRONIZED;
        }
        else
        {
            guint bytes = packets_to_inspect * TS_PACKET_SIZE;
            GST_DEBUG_OBJECT(filter,
                    "Unable to sync to the stream: skipping %d bytes", bytes);
            gst_adapter_flush(adapter, bytes);
        }
    }

    return done;
}

static gboolean handle_state_synchronized(GstTransportSync *filter,
        GstBuffer **out)
{
    GstAdapter *adapter = filter->adapter;
    gboolean done = FALSE;

    if (gst_adapter_available(adapter) < TS_PACKET_SIZE)
    {
        done = TRUE;
    }
    else
    {
        const guint8 *peek_buffer = gst_adapter_peek(adapter, 1);
        if (*peek_buffer == TS_SYNC_BYTE)
        {
            *out = gst_adapter_take_buffer(adapter, TS_PACKET_SIZE);
        }
        else
        {
            *out = NULL;
            filter->state = STATE_LOOKING_FOR_SYNC;
        }
    }

    return done;
}

/* chain function
 * this function does the actual processing
 */
static GstFlowReturn gst_transport_sync_chain(GstPad * pad, GstBuffer * buf)
{
    GstTransportSync *filter = GST_TRANSPORTSYNC(GST_OBJECT_PARENT(pad));
    GstFlowReturn ret = GST_FLOW_OK;
    gboolean done = FALSE;

    gst_adapter_push(filter->adapter, buf);

    while (!done)
    {
        switch (filter->state)
        {
        case STATE_LOOKING_FOR_SYNC:
        {
            done = handle_state_looking_for_sync(filter);
            break;
        }
        case STATE_SYNCHRONIZED:
        {
            GstBuffer *out = NULL;
            done = handle_state_synchronized(filter, &out);
            if (out != NULL)
            {
                ret = gst_pad_push(filter->srcpad, out);
                if (ret != GST_FLOW_OK)
                {
                    done = TRUE;
                }
            }
            break;
        }
        default:
            break;
        }
    }

    return ret;
}
