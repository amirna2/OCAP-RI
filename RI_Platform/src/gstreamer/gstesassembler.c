/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2009 Steve Glennon <<s.glennon@cablelabs.com>>
 * Copyright (C) 2009 U-PRESTOMarcin <<user@hostname.org>>
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
 * SECTION:element-es_assembler
 *
 * FIXME:Describe es_assembler here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! es_assembler ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
    #include <config.h>
#endif

#include <gst/gst.h>
#include <glib/gprintf.h>
#include <string.h>
//#include <stdlib.h>

#include "gstesassembler.h"
#include <gst/base/gstbitreader.h>

GST_DEBUG_CATEGORY_STATIC (gst_es_assembler_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_es_assembler_debug

#ifndef min
    #define min(a,b) (((a) < (b)) ? (a) : (b))
#endif


#ifndef llabs
    #define llabs(a) (((a)>0) ? (a) : (-(a)))
#endif


#define UNBOUNDED_PES_PACKET_SIZE (1024*1024)
// #define BE_16_AT(ptr) ((((unsigned short)(*(unsigned char *)ptr))<<8)+(*(unsigned char *)(ptr+1)))
#define BE_16_AT(ptr) GST_READ_UINT16_BE(ptr)
//#define BE_32_AT(ptr) ((((unsigned short)(*(unsigned char *)ptr))<<8)+(*(unsigned char *)(ptr+1)))
#define BE_32_AT(ptr) GST_READ_UINT32_BE(ptr)


/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0,
    PROP_SILENT,
    PROP_IS_PCR_PID,
    PROP_IGNORE_CC_ERROR,
    PROP_DO_TIMESTAMP,
    PROP_PLAYRATE
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
                                                                    GST_PAD_SINK,
                                                                    GST_PAD_ALWAYS,
                                                                    GST_STATIC_CAPS ("video/mpegts," "packetsize=(int)188," "systemstream=(boolean)true")
                                                                   );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src",
                                                                   GST_PAD_SRC,
                                                                   GST_PAD_ALWAYS,
                                                                   GST_STATIC_CAPS ("video/mpeg;"         \
                                                                                    "video/x-h264;"       \
                                                                                    "audio/mpeg,mpegversion=(int){1,4};"         \
                                                                                    "audio/x-ac3;"        \
                                                                                    "audio/x-eac3;"       ) 
                                                                  );

static char *caps_formats[] = {
NULL,
"video/mpeg",
"video/x-h264",
"audio/mpeg",
"audio/x-ac3",
"audio/x-eac3"
};

/*lint -e(123)*/ GST_BOILERPLATE (GstESAssembler, gst_es_assembler, GstElement, GST_TYPE_ELEMENT)

static guint64 frame_delta_ns[] =
{
0,                      // Forbidden
41708333,               // 24000 / 1001 (23.976...)
41666666,               // 24
40000000,               // 25
33366666,               // 30000 / 1001 (29.97...)
33333333,               // 30
20000000,               // 50
16683333,               // 60000 / 1001 (59.94...)
16666666,               // 60
0,                      // Reserved
0,                      // Reserved
0,                      // Reserved
0,                      // Reserved
0,                      // Reserved
0,                      // Reserved
0                       // Reserved
};


// Forward declarations
static void gst_es_assembler_dispose (GObject * object);
static void gst_es_assembler_finalize (GObject * object);

static void gst_es_assembler_set_property (GObject * object, guint prop_id,
                                           const GValue * value, GParamSpec * pspec);
static void gst_es_assembler_get_property (GObject * object, guint prop_id,
                                           GValue * value, GParamSpec * pspec);

static gboolean gst_es_assembler_set_caps (GstPad * pad, GstCaps * caps);
static GstStateChangeReturn gst_es_assembler_change_state (GstElement * element,
                                                           GstStateChange transition);

static GstFlowReturn gst_es_assembler_chain (GstPad * pad, GstBuffer * buf);
static gboolean gst_es_assembler_event (GstPad *pad, GstEvent *event);
static GstBuffer *gst_es_assembler_process_ts_packet(GstESAssembler *filter,
                                                     guint8 *pdata,
                                                     GstClockTime buftime);

static gboolean gst_es_assembler_process_tp_af(GstESAssembler *filter,
                                               GstBitReader *gbr,
                                               guint16 pid,
                           
                    gboolean cc_error);
static GstBuffer *gst_es_assembler_process_tp_payload(GstESAssembler *filter,
                                                      GstBitReader *gbr,
                                                      guint8 payload_unit_start, guint16 pid,
                                                      guint8 transport_scrambling_control,
                                                      gboolean discontinuity,
                                                      GstClockTime buftime);
static GstESAssemberState gst_es_assembler_process_stream_id(GstESAssembler *filter, guint8 stream_id);

static void gst_es_assembler_append_data(GstESAssembler *filter, GstBitReader *gbr, guint8 bytes_avail);
static GstBuffer *gst_es_assembler_process_es_packet(GstESAssembler *filter, GstBuffer *buf);
static GstClock * gst_es_assembler_get_pipeline_clock(GstObject *object);
static GstClockTime gst_es_assembler_get_pipeline_time(GstESAssembler *filter);
static GstClockTime gst_es_assembler_get_running_time(GstESAssembler *filter);

static guint8 *gst_es_assembler_consume_sequence_header(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
// static guint8 *gst_es_assembler_consume_sequence_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 * gst_es_assembler_look_for_start_codes(GstESAssembler *filter, GstBuffer *buf);

static void gst_es_assembler_reset(GstESAssembler * filter);

static void gst_es_assembler_trickmode_discontinuity_check(GstESAssembler *filter,
                                                           GstBuffer *buf,
                                                           GstClockTime pts,
                                                           GstClockTime running_time,
                                                           GstClockTime input_buf_time);

static void gst_es_assembler_trickmode_timestamp_buf(GstESAssembler *filter,
                                                     GstBuffer *buf,
                                                     GstClockTime input_buf_time,
                                                     gboolean reset_times);

#ifdef DEBUG_STREAM
static char * gst_es_assembler_decode_frame_rate(int frame_rate_code);
static char * gst_es_assembler_decode_profile_and_level(int profile_and_level);
static char * gst_es_assembler_decode_picture_structure(int picture_structure);
#endif

static guint8 *gst_es_assembler_consume_sequence_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_sequence_display_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_quant_matrix_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_copyright_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_sequence_scalable_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_picture_display_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_picture_coding_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_picture_spatial_scalable_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_picture_temporal_scalable_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_camera_parameters_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_itu_t_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen);

static guint8 *gst_es_assembler_consume_picture_header(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
static guint8 *gst_es_assembler_consume_gop_header(GstESAssembler *filter, guint8 *pdata, guint32 *plen);
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

static void
gst_es_assembler_base_init (gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS (gclass);

    gst_element_class_set_details_simple(element_class,
                                         "ESAssembler",
                                         "Codec/Demuxer",
                                         "Outputs an elementary stream from a single PID transport stream",
                                         "Steve Glennon <s.glennon@cablelabs.com>");

    gst_element_class_add_pad_template (element_class,
                                        gst_static_pad_template_get (&src_factory));
    gst_element_class_add_pad_template (element_class,
                                        gst_static_pad_template_get (&sink_factory));
}

/* initialize the es_assembler's class */
static void
gst_es_assembler_class_init (GstESAssemblerClass * klass)
{
    GObjectClass *gobject_class;
    GstElementClass *gstelement_class;

    GST_DEBUG_CATEGORY_INIT (gst_es_assembler_debug, "esassembler",
                             0, "esassembler");

    gobject_class = (GObjectClass *) klass;
    gstelement_class = (GstElementClass *) klass;

    gobject_class->dispose = gst_es_assembler_dispose;
    gobject_class->finalize = gst_es_assembler_finalize;

    gobject_class->set_property = gst_es_assembler_set_property;
    gobject_class->get_property = gst_es_assembler_get_property;

    gstelement_class->change_state = gst_es_assembler_change_state;

    g_object_class_install_property (gobject_class, PROP_SILENT,
                                     g_param_spec_boolean ("silent", "Silent", "Produce verbose output ?",
                                                           TRUE, G_PARAM_READWRITE));
    g_object_class_install_property (gobject_class, PROP_IS_PCR_PID,
                                     g_param_spec_boolean ("is_pcr_pid", "Is PCR", "Input PID stream is PCR PID",
                                                           FALSE, G_PARAM_READWRITE));
    g_object_class_install_property (gobject_class, PROP_IGNORE_CC_ERROR,
                                     g_param_spec_boolean ("ignore_cc_error", "Ignore CC Error", "Ignore Continuity Count Errors",
                                                           TRUE, G_PARAM_READWRITE));
    g_object_class_install_property (gobject_class, PROP_DO_TIMESTAMP,
                                     g_param_spec_boolean ("do_timestamp", "Timestamp buffers", "Timestamp outgoing assembled buffers",
                                                           TRUE, G_PARAM_READWRITE));
    g_object_class_install_property (gobject_class, PROP_PLAYRATE,
                                     g_param_spec_float ("playrate", "Playback rate", "Playback rate: [-]X[.X], eg. -1.0, 64, -0.5",
                                                         MIN_ESA_PLAYRATE, MAX_ESA_PLAYRATE, DEFAULT_ESA_PLAYRATE, G_PARAM_READWRITE));
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void
gst_es_assembler_init (GstESAssembler * filter,
                       GstESAssemblerClass * gclass)
{

    filter->props_lock = g_mutex_new();

    // sink (input) pad
    filter->sinkpad = gst_pad_new_from_static_template (&sink_factory, "sink");
    gst_pad_set_chain_function (filter->sinkpad, GST_DEBUG_FUNCPTR(gst_es_assembler_chain));
    gst_pad_use_fixed_caps (filter->sinkpad);
    gst_pad_set_setcaps_function (filter->sinkpad,
                                  GST_DEBUG_FUNCPTR(gst_es_assembler_set_caps));
    gst_pad_set_getcaps_function (filter->sinkpad,
                                  GST_DEBUG_FUNCPTR(gst_pad_proxy_getcaps));
    gst_element_add_pad (GST_ELEMENT (filter), filter->sinkpad);
    gst_pad_set_event_function(filter->sinkpad, gst_es_assembler_event);


    // source (output) pad
    filter->srcpad = gst_pad_new_from_static_template (&src_factory, "src");
//  gst_pad_use_fixed_caps (filter->srcpad);
    gst_element_add_pad (GST_ELEMENT (filter), filter->srcpad);

    // ES assembler specific initialization
    filter->silent = TRUE;
    filter->state  = ESA_STATE_WAITING_PUSI;            // ES Assembler State
    filter->pstate = ESP_STATE_WAITING_FOR_SEQ_START;   // ES Processor State
    filter->ignore_cc_error = TRUE;
    filter->do_timestamp = TRUE;
    filter->applied_rate = 1.0;
    filter->is_pcr_pid = FALSE;
    filter->outbuf = NULL;
    filter->PTS = 0;
    filter->DTS = 0;
    filter->baseline_pts = 0;
    filter->last_buffer_time_ns = GST_CLOCK_TIME_NONE;
    filter->buffer_time_delta_ns = 0;
    filter->stream_format = ES_FORMAT_NONE;
    filter->pipeline_clock = NULL;
    filter->playrate = DEFAULT_ESA_PLAYRATE;
    filter->playrate_updated = FALSE;

    //gst_debug_set_threshold_for_name("esassembler", GST_LEVEL_ERROR);
    //gst_debug_set_threshold_for_name("esassembler", GST_LEVEL_INFO);

} /* gst_es_assembler_init */


/* this function resets this element to an initial state */
static void gst_es_assembler_reset(GstESAssembler * filter)
{
    GST_INFO_OBJECT(filter,"Resetting");

    filter->silent = TRUE;
    filter->state  = ESA_STATE_WAITING_PUSI;            // ES Assembler State
    filter->pstate = ESP_STATE_WAITING_FOR_SEQ_START;   // ES Processor State
    filter->ignore_cc_error = TRUE;
    filter->do_timestamp = TRUE;
    filter->is_pcr_pid = FALSE;
    filter->outbuf = NULL;
    filter->PTS = 0;
    filter->DTS = 0;
    filter->baseline_pts = 0;
    filter->last_buffer_time_ns = GST_CLOCK_TIME_NONE;
    filter->last_media_time_ns = 0;
    filter->buffer_time_delta_ns = 0;
    filter->stream_format = ES_FORMAT_NONE;
    filter->pipeline_clock = NULL;
}

/* this function handles the link with other elements */
static gboolean
gst_es_assembler_set_caps (GstPad * pad, GstCaps * caps)
{
    GstESAssembler *filter;
    GstPad *otherpad;

    filter = GST_ES_ASSEMBLER (gst_pad_get_parent (pad));
    otherpad = (pad == filter->srcpad) ? filter->sinkpad : filter->srcpad;
    gst_object_unref (filter);

    return gst_pad_set_caps (otherpad, caps);
}

static void
gst_es_assembler_dispose (GObject * object)
{
    GstESAssembler *filter = GST_ES_ASSEMBLER (object);

    // From http://library.gnome.org/devel/gobject/2.16/gobject-memory.html
    //
    // When dispose ends, the object should not hold any reference to any other
    // member object. The object is also expected to be able to answer client
    // method invocations (with possibly an error code but no memory violation)
    // until finalize is executed. dispose can be executed more than once.
    // dispose should chain up to its parent implementation just before returning
    // to the caller.
    filter = filter;
    if (filter->outbuf)
    {
        gst_buffer_unref(filter->outbuf);
        filter->outbuf = NULL;
    }

    /*lint -e(123)*/GST_CALL_PARENT (G_OBJECT_CLASS, dispose, (object));
}

static void
gst_es_assembler_finalize (GObject * object)
{
    GstESAssembler *filter = GST_ES_ASSEMBLER (object);

    // From http://library.gnome.org/devel/gobject/2.16/gobject-memory.html
    //
    // Finalize is expected to complete the destruction process initiated by
    // dispose. It should complete the object's destruction. finalize will be
    // executed only once. finalize should chain up to its parent implementation
    // just before returning to the caller. The reason why the destruction
    // process is split is two different phases is explained in the section
    // called "Reference counts and cycles".
    g_mutex_free(filter->props_lock);

    /*lint -e(123)*/ GST_CALL_PARENT (G_OBJECT_CLASS, finalize, (object));
}

static void
gst_es_assembler_set_property (GObject * object, guint prop_id,
                               const GValue * value, GParamSpec * pspec)
{
    GstESAssembler *filter = GST_ES_ASSEMBLER (object);

    switch (prop_id)
    {
    case PROP_SILENT:
        filter->silent = g_value_get_boolean (value);
        break;
    case PROP_IS_PCR_PID:
        filter->is_pcr_pid = g_value_get_boolean (value);
        break;
    case PROP_IGNORE_CC_ERROR:
        filter->ignore_cc_error = g_value_get_boolean (value);
        break;
    case PROP_DO_TIMESTAMP:
        GST_OBJECT_LOCK(filter);
        filter->do_timestamp = g_value_get_boolean (value);
        GST_OBJECT_UNLOCK(filter);
        break;
    case PROP_PLAYRATE:
        g_mutex_lock(filter->props_lock);
        {
           GST_INFO_OBJECT(filter, "Setting new playrate");

            gfloat new_playrate = g_value_get_float(value);
            if (new_playrate == 0.0)
            {
                GST_INFO_OBJECT(filter, "Ignoring new playrate of zero.");
            }
            else 
            {
                if (new_playrate != filter->playrate)
                {
                    filter->playrate = g_value_get_float(value);
                    filter->playrate_updated = TRUE;
                    filter->baseline_pts = 0;
                    GST_WARNING_OBJECT(filter, "New playrate set: %f.", filter->playrate);
                }
            }
        }
        g_mutex_unlock(filter->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
        break;
    }
}

static void
gst_es_assembler_get_property (GObject * object, guint prop_id,
                               GValue * value, GParamSpec * pspec)
{
    GstESAssembler *filter = GST_ES_ASSEMBLER (object);

    switch (prop_id)
    {
    case PROP_SILENT:
        g_value_set_boolean (value, filter->silent);
        break;
    case PROP_IS_PCR_PID:
        g_value_set_boolean (value, filter->is_pcr_pid);
        break;
    case PROP_IGNORE_CC_ERROR:
        g_value_set_boolean (value, filter->ignore_cc_error);
        break;
    case PROP_DO_TIMESTAMP:
        g_value_set_boolean (value, filter->do_timestamp);
        break;
    case PROP_PLAYRATE:
        g_mutex_lock(filter->props_lock);
        g_value_set_float(value, filter->playrate);
        g_mutex_unlock(filter->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
        break;
    }
}



static gboolean gst_es_assembler_event (GstPad *pad,
                                        GstEvent *event)
{
    GstESAssembler *filter = GST_ES_ASSEMBLER (gst_pad_get_parent (pad));
    gboolean ret = FALSE;

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_EOS:
        /* end-of-stream, we should close down all stream leftovers here */
        GST_INFO_OBJECT(filter,"Received EOS event");

        g_mutex_lock(filter->props_lock);

        if (filter->outbuf != NULL)
        {
            gst_buffer_unref(filter->outbuf);
            filter->outbuf = NULL;
        }
        filter->state = ESA_STATE_WAITING_PUSI;
        ret = gst_pad_event_default(pad,event);
        gst_es_assembler_reset(filter);

        g_mutex_unlock(filter->props_lock);
        break;
    case GST_EVENT_NEWSEGMENT:
        {
            GST_INFO_OBJECT(filter, "Received new segment event");

            // Get the lock since manipulating element
            g_mutex_lock(filter->props_lock);

            gboolean      update = FALSE;
            gdouble         rate = 0.;
            GstFormat     format = GST_FORMAT_UNDEFINED;
            gint64         start = 0;
            gint64          stop = 0;
            gint64      position = 0;
            gst_event_parse_new_segment_full(event, &update, &rate, &(filter->applied_rate), &format, &start, &stop, &position);
            GST_DEBUG_OBJECT(filter, "Received NEWSEGMENT event - update:%s, rate:%g, applied_rate:%g, format:%s, start:%lli, stop:%lli, position:%lli",
                             (update==TRUE)?"true":"false", rate, filter->applied_rate, gst_format_get_name(format), start, stop, position);
            if (update == TRUE)
            {
                /* do not re-timestamp buffers when the incoming applied rate is other than 1.0 */
                if (filter->applied_rate == 1.)
                {
                    filter->do_timestamp = TRUE;
                    GST_INFO_OBJECT(filter, "ENABLING timestamping");
                }
                else
                {
                    filter->do_timestamp = FALSE;
                    GST_INFO_OBJECT(filter, "DISABLING timestamping");
                }
            }
            else
            {
                // New Segment some times implies a new clock so reset timing related
                // parameters in order to sync up with possible new clock
                GST_INFO_OBJECT(filter, "Reset timing related parameters in response to new segment");
                filter->baseline_pts = 0;
                filter->baseline_running_time = 0;
                filter->pts_since_baseline = 0;
                filter->PTS = 0;
                filter->pipeline_clock = NULL;
            }

            // Pass event on otherwise other elements such as display sink report internal data flow error
            ret = gst_pad_event_default(pad,event);

            // Release lock since done manipulating element
            g_mutex_unlock(filter->props_lock);
        }
        break;
    case GST_EVENT_FLUSH_START:
        GST_INFO_OBJECT(filter,"Received FLUSH_START event");
        ret = gst_pad_event_default(pad,event);
        break;
    case GST_EVENT_FLUSH_STOP:
        GST_INFO_OBJECT(filter,"Received FLUSH_STOP event");
        ret = gst_pad_event_default(pad,event);
        break;
    default:
        GST_INFO_OBJECT(filter,"Received other (%d) event", GST_EVENT_TYPE(event));
        ret = gst_pad_event_default(pad,event);
        break;
    }
    gst_object_unref (filter);
    return ret;
}

/***********************************************/
/**********                           **********/
/********** GstElement IMPLEMENTATION **********/
/**********                           **********/
/***********************************************/

static GstStateChangeReturn
gst_es_assembler_change_state (GstElement * element, GstStateChange transition)
{
    GstStateChangeReturn result = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);
    if (result == TRUE && transition == GST_STATE_CHANGE_PLAYING_TO_PAUSED)
    {
        GST_INFO_OBJECT(GST_ES_ASSEMBLER(element), "Resetting do_timestamp property to TRUE.");
        GST_ES_ASSEMBLER(element)->do_timestamp = TRUE;
    }
    return result;
}

/*
 * chain function
 * this function does the actual processing
 *
 *       IN: one or more complete transport packets
 *
 * ---------------
 * transport_packet()
 * {
 *   sync_byte                                                                    8 bslbf
 *   transport_error_indicator                                                    1 bslbf
 *   payload_unit_start_indicator                                                 1 bslbf
 *   transport_priority                                                           1 bslbf
 *   PID                                                                         13 uimsbf
 *   transport_scrambling_control                                                 2 bslbf
 *   adaptation_field_control                                                     2 bslbf
 *   continuity_counter                                                           4 uimsbf
 *   if(adaptation_field_control = = '10' || adaptation_field_control = = '11')
 *   {
 *     adaptation_field()
 *   }
 *   if(adaptation_field_control = = '01' || adaptation_field_control = = '11')
 *   {
 *     for (i = 0; i < N; i++)
 *     {
 *       data_byte                                                                8 bslbf
 *     }
 *   }
 * }
 *
 *       OUT:
 * ----------------
 * TBD...
 *
 */
static GstFlowReturn
gst_es_assembler_chain (GstPad * pad, GstBuffer * buf)
{
    GstESAssembler *filter = GST_ES_ASSEMBLER (GST_OBJECT_PARENT (pad));
    gint       buflen = GST_BUFFER_SIZE(buf);
    guint8    *pdata  = GST_BUFFER_DATA(buf);
    GstBuffer *outbuf;
    GstFlowReturn ret = GST_FLOW_OK;
    GstClockTime buf_input_time;
    GstClockTime buf_duration;

    /* Debug info */
    GST_LOG_OBJECT(filter, "Buffer received, %d bytes.", buflen);

    /* Make note of the input buffer timestamp */
    buf_input_time = GST_BUFFER_TIMESTAMP(buf);
    buf_duration = GST_BUFFER_DURATION(buf);

    g_mutex_lock(filter->props_lock);

    /* Process each transport packet in the GstBuffer */
    while ((buflen >= 188) && (ret >= GST_FLOW_OK))
    {
        outbuf = gst_es_assembler_process_ts_packet(filter, pdata, buf_input_time);
        if (outbuf)
        {
            if (filter->do_timestamp == FALSE)
            {
                if (GST_BUFFER_TIMESTAMP(outbuf) == GST_CLOCK_TIME_NONE)
                {
                    GST_DEBUG_OBJECT(filter, "Incoming buffer %p did not have a valid timestamp.", buf);
                }
                if (GST_BUFFER_DURATION(outbuf) == GST_CLOCK_TIME_NONE)
                {
                    GST_DEBUG_OBJECT(filter, "Incoming buffer %p did not have a valid duration.", buf);
                }

                GST_BUFFER_TIMESTAMP(outbuf) = buf_input_time;
                GST_BUFFER_DURATION(outbuf) = buf_duration;
                GST_BUFFER_FLAG_SET(outbuf, GST_BUFFER_FLAG_DISCONT);
            }
            else
            {
                /* if timestamping and the playrate was changed, need to send a new segment event */
                if (filter->playrate_updated)
                {    
                    GST_WARNING_OBJECT(filter, "Sending new segment event due to playrate updated");

                    GstEvent *new_segment;
                    filter->playrate_updated = FALSE;

                    // Send a flush start & flush stop prior to new segment to clear out previous buffers
                    (void)gst_pad_push_event(filter->srcpad, gst_event_new_flush_start());
                    (void)gst_pad_push_event(filter->srcpad, gst_event_new_flush_stop());

                    new_segment = gst_event_new_new_segment_full(FALSE, 1.0, 
                                                                 (gdouble)filter->playrate,
                                                                  GST_FORMAT_TIME, 0,-1, 0);
                    (void)gst_pad_push_event(filter->srcpad, new_segment);
                }
            
            }
            ret = gst_pad_push(filter->srcpad, outbuf);
        }

        /* move on to the next packet */
        buflen -= 188;
        pdata  += 188;
    } /* endwhile packets to process and any pushes have succeeded */

    g_mutex_unlock (filter->props_lock);

    gst_buffer_unref(buf);
    return ret;

} /* gst_es_assembler_chain */

static GstBuffer *gst_es_assembler_process_ts_packet(GstESAssembler *filter,
                                                     guint8 *pdata,
                                                     GstClockTime buftime)
{
    /* Use reference to filter instance data here */
    GstBuffer *outbuf = (GstBuffer *)NULL;
    GstBitReader gbr;      /* GstBitReader to reference buffer */
    guint8     transport_error_indicator;
    guint8     payload_unit_start;
    guint16    pid;
    guint8     transport_scrambling_control;
    guint8     adaptation_field_control;
    guint8     continuity_counter;
    guint8     expected_continuity_counter;
    gboolean   discontinuity;

    /* Check for packet sync still good */
    if ( *pdata == 0x47 )
    {
        /* Point the bitreader to the packet */
        gst_bit_reader_init(&gbr, pdata+1, 187);
        /* Now go and get tranport packet header info */
        gst_bit_reader_get_bits_uint8(&gbr,&transport_error_indicator, 1);
        gst_bit_reader_get_bits_uint8(&gbr,&payload_unit_start, 1);
        gst_bit_reader_skip(&gbr,1);
        gst_bit_reader_get_bits_uint16(&gbr,&pid, 13);
        gst_bit_reader_get_bits_uint8(&gbr,&transport_scrambling_control, 2);
        gst_bit_reader_get_bits_uint8(&gbr,&adaptation_field_control, 2);
        gst_bit_reader_get_bits_uint8(&gbr,&continuity_counter, 4);

        /* Generate expected continuity count */
        expected_continuity_counter = filter->last_continuity_counter + 1;
        expected_continuity_counter &= 0x0F;
        filter->last_continuity_counter = continuity_counter;

        discontinuity = (continuity_counter != expected_continuity_counter);
        if (discontinuity)
        {
            GST_DEBUG_OBJECT(filter, "Transport packet discontinuity signalled (received %d, expected %d) on PID 0x%4.4X",
                             continuity_counter, expected_continuity_counter, pid);
        }

        /* If adaptation field is marked as present, process it */
        if (adaptation_field_control & 0x02)
        {
            /* The adaptation field may have the discontinuity indicator set */
            discontinuity = gst_es_assembler_process_tp_af(filter, &gbr, pid, discontinuity);

        } /* endif adaptation field present */

        /* TBD - what to do if PID changes? Is this a discontinuity?*/

        /* If payload is marked as present, process it */
        if (adaptation_field_control & 0x01)
        {
            outbuf = gst_es_assembler_process_tp_payload(filter,
                                                         &gbr,
                                                         payload_unit_start,
                                                         pid,
                                                         transport_scrambling_control,
                                                         discontinuity,
                                                         buftime);
        } /* endif payload present */

    }
    else
    {
        GST_WARNING_OBJECT(filter, "gstesassembler: Transport Packet Sync Bad.");
    } /* endif sync byte was correct */

    return outbuf;

} /* gst_es_assembler_process_ts_packet */

/* Process the adaptation field (AF) in the transport packet */
/* Returns TRUE if discontinuity signalled */
/* Takes appropriate action based upon AF contents */
static gboolean gst_es_assembler_process_tp_af(GstESAssembler *filter,
                                               GstBitReader *gbr, guint16 pid, gboolean cc_error)
{

    guint8 af_len;
    guint8 temp;
    guint8 discontinuity_indicator = 0;
    guint8 pcr_flag;

    /* Assumes gbr is pointing to start of AF, and AF is present */

    /* Should never have been called without enough data to consume/process AF */
    gst_bit_reader_get_bits_uint8(gbr, &af_len, 8);

    if (af_len)
    {
        gst_bit_reader_get_bits_uint8(gbr, &discontinuity_indicator, 1); // discontinuity_indicator
        gst_bit_reader_get_bits_uint8(gbr, &temp, 1);                    // random_access_indicator
        if ((filter->state == ESA_STATE_WAITING_RAI) && temp)
        {
            filter->state = ESA_STATE_WAITING_PUSI;
        }
        gst_bit_reader_get_bits_uint8(gbr, &temp, 1);     // elementary_stream_priority_indicator
        gst_bit_reader_get_bits_uint8(gbr, &pcr_flag, 1); // PCR_flag
        gst_bit_reader_get_bits_uint8(gbr, &temp, 1);     // OPCR_flag
        gst_bit_reader_get_bits_uint8(gbr, &temp, 1);     // splicing_point_flag
        gst_bit_reader_get_bits_uint8(gbr, &temp, 1);     // transport_private_data_indicator
        gst_bit_reader_get_bits_uint8(gbr, &temp, 1);     // adaptation_field_extension_flag
        af_len--;
    }
    /* TODO - process the remaining (optional) AF contents*/

    /* Simple for now - just skip past the AF */
    /* aflen can be 0 for single stuffing byte */
    gst_bit_reader_skip(gbr, af_len*8);

    if (discontinuity_indicator)
    {
        GST_WARNING_OBJECT(filter, "Transport packet adaptation field discontinuity signalled on PID 0x%4.4X", pid);
    }

    return(((gboolean)discontinuity_indicator)  || cc_error);

} /* gst_es_assembler_process_tp_af */

/*
    Function: gst_es_assembler_process_tp_payload

    Description:
    This function is called for each transport packet payload in the PID stream.
    The PID stream should be for a single PID, but we do not enforce that.

    We are processing a PES stream, conveyed in transport packet payloads. Since we have no
    guarantees as to how many bytes are in any TP payload, we have to operate as a byte-oriented
    state machine. The state machine operates through the following states, and can drop back
    to the WAITING_PUSI state at various points. The states in general correspond to individual
    bytes in the PES header. Certain states consume multiple bytes.

    ESA_STATE_WAITING_RAI,      // waiting for a random access indicator in transport packet af
    ESA_STATE_WAITING_PUSI,     // waiting for pusi and first byte of packet_start_code_prefix
    ESA_STATE_WAITING_H1,       // waiting for second byte of packet_start_code_prefix
    ESA_STATE_WAITING_H2,       // waiting for third byte of packet_start_code_prefix
    ESA_STATE_WAITING_H3,       // waiting for stream_id
    ESA_STATE_WAITING_H4,       // waiting for first byte of PES_packet_length
    ESA_STATE_WAITING_H5,       // waiting for second byte of PES_packet_length
    ESA_STATE_WAITING_H6,       // waiting for PES_sc, PES_prio header byte
    ESA_STATE_WAITING_H7,       // waiting for PTS_DTS_flags header byte
    ESA_STATE_WAITING_H8,       // waiting for PES_header_data_length byte
    ESA_STATE_WAITING_H9,       // Consuming optional PES hdr and stuffing
                                // Other header states will go here for optional fields
    ESA_STATE_WAITING_STUFFING, // waiting until stuffing bytes consumed
    ESA_STATE_PRE_ASSEMBLY,     // Pseudo-state to allocate output buffer
    ESA_STATE_ASSEMBLING,       // Assembling the PES payload into a buffer

    The transport packet payload is passed in as a "GstBitReader" which is a handy way
    to allow us to consume random numbers of bits rather than having to manually dissect
    bytes from the buffer.


    */
static GstBuffer *gst_es_assembler_process_tp_payload(GstESAssembler *filter,
                                                      GstBitReader *gbr,
                                                      guint8 payload_unit_start,
                                                      guint16 pid,
                                                      guint8 transport_scrambling_control,
                                                      gboolean discontinuity,
                                                      GstClockTime buftime)
{
    guint8  temp;
    guint64 temp64;             // Used for constructing PTS/DTS
    guint  bytes_avail;         // bytes available in this bitreader
    guint  bytes_to_skip;       // bytes to skip in this bitreader
    GstBuffer *outbuf = NULL;   // Buffer for us to return
    GstFlowReturn  alloc_ret = GST_FLOW_OK; // return code from buffer allocation
    int buf_size = 0;           // size of buffer in bytes to request
    GstPad* peer_pad = NULL;    // peer pad which is queried for buffer allocation

    bytes_avail = gst_bit_reader_get_remaining(gbr) / 8;
    while (bytes_avail)
    {

        switch (filter->state)
        {
        case ESA_STATE_WAITING_RAI:
            /* dump this packet if still awaiting random_access_indicator in tp af */
            /* RAIs are not guaranteed, and in general are not present. */
            bytes_avail = 0;
            break;
        case ESA_STATE_WAITING_PUSI:
            if (payload_unit_start)
            {
                /* process PUSI once only */
                payload_unit_start = FALSE;
                gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
                bytes_avail--;
                if (temp == 0)
                {
                    filter->state = ESA_STATE_WAITING_H1;
                    filter->pusi_arrival_time = buftime;
                }
                else
                {
                    /* Start code not where expected, dump this packet */
                    bytes_avail = 0;
                }
            }
            else
            {
                bytes_avail = 0;
            }
            break;
        case ESA_STATE_WAITING_H1:  // second 0 in start code
            gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
            bytes_avail--;
            if (temp == 0x00)
            {
                filter->state = ESA_STATE_WAITING_H2;
            }
            else
            {
                /* Start code not where expected, dump this packet */
                bytes_avail = 0;
                filter->state = ESA_STATE_WAITING_PUSI;
            }
            break;
        case ESA_STATE_WAITING_H2:  // 0x01 in start code
            gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
            bytes_avail--;
            if (temp == 0x01)
            {
                filter->state = ESA_STATE_WAITING_H3;
            }
            else
            {
                /* Start code not where expected, dump this packet */
                bytes_avail = 0;
                filter->state = ESA_STATE_WAITING_PUSI;
            }
            break;
        case ESA_STATE_WAITING_H3:  // stream_id
            gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
            bytes_avail--;
            filter->state = gst_es_assembler_process_stream_id(filter, temp);
            if (filter->state == ESA_STATE_WAITING_PUSI)
            {
                /* dump this packet*/
                bytes_avail = 0;
            }
            break;
        case ESA_STATE_WAITING_H4:  // packet_length msb
            gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
            bytes_avail--;
            filter->packet_length = ((guint16)temp) << 8;
            filter->state = ESA_STATE_WAITING_H5;
            break;
        case ESA_STATE_WAITING_H5:  // packet_length lsb
            gst_bit_reader_get_bits_uint8(gbr, &temp, 8);
            bytes_avail--;
            filter->packet_length |= ((guint16)temp);
            filter->state = ESA_STATE_WAITING_H6;
            /* if packet length specified and long enough, make note less fixed header */
            if (filter->packet_length > 3)
            {
                filter->packet_length_remaining = (gint32)(guint32)(filter->packet_length) - 3;
            }
            else if (filter->packet_length == 0)
            {
                /* packet length unbounded */
                filter->packet_length_remaining = 0;
            }
            else
            {
                /* packet length too short, bail now */
                bytes_avail = 0;
                filter->state = ESA_STATE_WAITING_PUSI;
            }
            break;
        case ESA_STATE_WAITING_H6:  // PES_Scrambling_control byte
            gst_bit_reader_get_bits_uint8(gbr, &temp, 2); // Fixed '10'
            gst_bit_reader_get_bits_uint8(gbr, &temp, 2); // PES Scrambling Control
            if (temp == 0)
            {
                gst_bit_reader_get_bits_uint8(gbr, &temp, 1);   // PES_priority
                gst_bit_reader_get_bits_uint8(gbr, &(filter->data_alignment_indicator), 1); // data_alignment_indicator
                gst_bit_reader_get_bits_uint8(gbr, &temp, 1);   // copyright
                gst_bit_reader_get_bits_uint8(gbr, &temp, 1);   // original_or_copy
                bytes_avail--;
                filter->state = ESA_STATE_WAITING_H7;
            }
            else
            {
                /* Scrambled packet,dump this packet */
                bytes_avail = 0;
                filter->state = ESA_STATE_WAITING_PUSI;
            } /* endif unscrambled */
            break;
        case ESA_STATE_WAITING_H7:  // PTS_DTS_flags byte
            gst_bit_reader_get_bits_uint8(gbr, &(filter->PTS_DTS_flags), 2);
            gst_bit_reader_get_bits_uint8(gbr, &(filter->ESCR_flag), 1);
            gst_bit_reader_get_bits_uint8(gbr, &(filter->ES_rate_flag), 1);
            gst_bit_reader_get_bits_uint8(gbr, &(filter->DSM_trick_mode_flag), 1);
            gst_bit_reader_get_bits_uint8(gbr, &(filter->additional_copy_info_flag), 1);
            gst_bit_reader_get_bits_uint8(gbr, &(filter->PES_CRC_flag), 1);
            gst_bit_reader_get_bits_uint8(gbr, &(filter->PES_extension_flag), 1);
            bytes_avail--;
            filter->state = ESA_STATE_WAITING_H8;
            break;
        case ESA_STATE_WAITING_H8: // PES_header_data_length
            gst_bit_reader_get_bits_uint8(gbr, &(filter->PES_header_data_length), 8);
            bytes_avail--;
            if (filter->PES_header_data_length)
            {
                /* check we do not go off the end of the PES packet */
                if (filter->packet_length)
                {
                    /* if no payload or not enough for the signalled header, bail now */
                    /* TODO - what if we get a no-payload, header only PES packet? */
                    if (filter->packet_length_remaining <= filter->PES_header_data_length)
                    {
                        bytes_avail = 0;
                        filter->state = ESA_STATE_WAITING_PUSI;
                    }
                    else
                    {
                        /* pre-remove variable header from PES packet len */
                        filter->packet_length_remaining -= filter->PES_header_data_length;
                    } /* endif packet length insufficient */
                }
                filter->PES_header_remaining = filter->PES_header_data_length;
                /* Decide whether to look for PTS/DTS*/
                if ((filter->PTS_DTS_flags & 2) && (filter->PES_header_remaining >= 5))
                {
                    filter->state = ESA_STATE_WAITING_H9; // Look for PTS
                }
                else
                {
                    filter->state = ESA_STATE_WAITING_HDR_DONE; // Just consume the remaining PES header
                }
            }
            else
            {
                filter->state = ESA_STATE_PRE_ASSEMBLY;
            } /* endif bounded packet length */
            break;
        case ESA_STATE_WAITING_H9: // Consuming PTS byte 1
            gst_bit_reader_skip(gbr, 4);                     // Marker bits
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 3); // PTS[32-30]
            gst_bit_reader_skip(gbr, 1);                     // Marker bit
            bytes_avail--;
            filter->PTS = temp64 << 8;                       // pre-shift ready for next 8 bits
            filter->state = ESA_STATE_WAITING_H10;
            break;
        case ESA_STATE_WAITING_H10: // Consuming PTS byte 2
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 8); // PTS[29-22]
            bytes_avail--;
            filter->PTS |= temp64;
            filter->PTS <<= 7;                               // pre-shift ready for next 7 bits
            filter->state = ESA_STATE_WAITING_H11;
            break;
        case ESA_STATE_WAITING_H11: // Consuming PTS byte 3
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 7); // PTS[21-15]
            gst_bit_reader_skip(gbr, 1);                     // Marker bit
            bytes_avail--;
            filter->PTS |= temp64;
            filter->PTS <<= 8;                               // pre-shift ready for next 8 bits
            filter->state = ESA_STATE_WAITING_H12;
            break;
        case ESA_STATE_WAITING_H12: // Consuming PTS byte 4
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 8); // PTS[14-7]
            bytes_avail--;
            filter->PTS |= temp64;
            filter->PTS <<= 7;                               // pre-shift ready for next 7 bits
            filter->state = ESA_STATE_WAITING_H13;
            break;
        case ESA_STATE_WAITING_H13: // Consuming PTS byte 5
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 7); // PTS[6-0]
            gst_bit_reader_skip(gbr, 1);                     // Marker bit
            bytes_avail--;
            filter->PES_header_remaining -= 5;               // Mark PTS as consumed from header
            filter->PTS |= temp64;                           // PTS now complete
            GST_DEBUG_OBJECT(filter, "Received buffer PTS of time %llu", ((filter->PTS * NS_PER_90KHZ_TICK_X256)>>8));
            if ((filter->PTS_DTS_flags & 1) && (filter->PES_header_remaining >= 5))
            {
                filter->state = ESA_STATE_WAITING_H14; // Look for DTS
            }
            else
            {
                filter->state = ESA_STATE_WAITING_HDR_DONE; // Just consume the remaining PES header
            }
            break;
        case ESA_STATE_WAITING_H14: // Consuming DTS byte 1
            gst_bit_reader_skip(gbr, 4);                     // Marker bits
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 3); // DTS[32-30]
            gst_bit_reader_skip(gbr, 1);                     // Marker bit
            bytes_avail--;
            filter->DTS = temp64 << 8;                       // pre-shift ready for next 8 bits
            filter->state = ESA_STATE_WAITING_H15;
            break;
        case ESA_STATE_WAITING_H15: // Consuming DTS byte 2
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 8); // DTS[29-22]
            bytes_avail--;
            filter->DTS |= temp64;
            filter->DTS <<= 7;                               // pre-shift ready for next 7 bits
            filter->state = ESA_STATE_WAITING_H16;
            break;
        case ESA_STATE_WAITING_H16: // Consuming DTS byte 3
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 7); // DTS[21-15]
            gst_bit_reader_skip(gbr, 1);                     // Marker bit
            bytes_avail--;
            filter->DTS |= temp64;
            filter->DTS <<= 8;                               // pre-shift ready for next 8 bits
            filter->state = ESA_STATE_WAITING_H17;
            break;
        case ESA_STATE_WAITING_H17: // Consuming DTS byte 4
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 8); // DTS[14-7]
            bytes_avail--;
            filter->DTS |= temp64;
            filter->DTS <<= 7;                               // pre-shift ready for next 7 bits
            filter->state = ESA_STATE_WAITING_H18;
            break;
        case ESA_STATE_WAITING_H18: // Consuming DTS byte 5
            gst_bit_reader_get_bits_uint64(gbr, &temp64, 7); // DTS[6-0]
            gst_bit_reader_skip(gbr, 1);                     // Marker bit
            bytes_avail--;
            filter->PES_header_remaining -= 5;               // Mark DTS as consumed from header
            filter->DTS |= temp64;                           // DTS now complete
            GST_DEBUG_OBJECT(filter, "Received buffer DTS of time %llu", ((filter->DTS * NS_PER_90KHZ_TICK_X256)>>8));
            filter->state = ESA_STATE_WAITING_HDR_DONE;      // Just consume the remaining PES header
            break;
        case ESA_STATE_WAITING_HDR_DONE: // Consuming optional PES header bytes and stuffing
            /* filter->PES_header_remaining is how many bytes left to stay in this state*/
            bytes_to_skip = min(bytes_avail, (guint)(filter->PES_header_remaining));
            gst_bit_reader_skip(gbr, (bytes_to_skip * 8));
            bytes_avail -= bytes_to_skip;
            filter->PES_header_remaining -= bytes_to_skip;
            if (filter->PES_header_remaining == 0)
            {
                filter->state = ESA_STATE_PRE_ASSEMBLY;
            } /* endif finished with PES header*/
            break;
        case ESA_STATE_PRE_ASSEMBLY: // allocate a buffer for the new ES packet

            buf_size = filter->packet_length_remaining ? filter->packet_length_remaining  : UNBOUNDED_PES_PACKET_SIZE;

            // Get the pad to which the output pad is attached to determine if it supports buffer allocation
            peer_pad = gst_pad_get_peer(filter->srcpad);

            // Get buffer from element attached to source/output pad if it has a buffer allocate function
            if ((NULL != peer_pad) && (NULL != peer_pad->bufferallocfunc))
            {
                // Ask downstream element for buffer
                filter->outbuf = NULL;
                alloc_ret = gst_pad_alloc_buffer(filter->srcpad,
                                                 GST_BUFFER_OFFSET_NONE,
                                                 buf_size,
                                                 filter->srcpad->caps,
                                                 &filter->outbuf);
                if (G_LIKELY(!GST_FLOW_IS_SUCCESS(alloc_ret)))
                {
                    // Make sure the output buffer is NULL so error handling will be performed below
                    filter->outbuf = NULL;
                }
            }
            else
            {
                // Allocate the directly buffer here rather than asking downstream element for one
                filter->outbuf = gst_buffer_try_new_and_alloc(buf_size);
            }

            // Unref the peer pad
            if (G_LIKELY(peer_pad != NULL))
            {
                gst_object_unref(peer_pad);
            }

            if (filter->outbuf == NULL)
            {
                /* signal allocation failure and return to initial state */
                GST_ERROR_OBJECT(filter, "GST destination buffer allocation of size %d failed.",
                                 (filter->packet_length_remaining ? filter->packet_length_remaining  : UNBOUNDED_PES_PACKET_SIZE));
                bytes_avail = 0;
                filter->state = ESA_STATE_WAITING_PUSI;
            }
            else
            {
                filter->state = ESA_STATE_ASSEMBLING;
                /* Perform initial fixup of the gst buffer */
                GST_BUFFER_OFFSET(filter->outbuf) = 0;
                GST_BUFFER_TIMESTAMP(filter->outbuf) = filter->pusi_arrival_time;
            }
            break;

        case ESA_STATE_ASSEMBLING: // Assembling transport packet payloads into an ES packet
            /* Now assembling PES packet to ES buffer */
            /* If PUSI, finish any packet in progress */
            if (payload_unit_start)
            {
                /* detach last buffer for return to caller */
                outbuf = filter->outbuf;
                GST_BUFFER_SIZE(outbuf)       = GST_BUFFER_OFFSET(outbuf);
                GST_BUFFER_OFFSET(outbuf)     = GST_BUFFER_OFFSET_NONE;

                // If in trick mode, set buffer timestamp to incoming chunk header time
                if (filter->playrate != 1.0)
                {
                    GST_BUFFER_TIMESTAMP(outbuf) = buftime;
                }
                outbuf = gst_es_assembler_process_es_packet(filter, outbuf);
                filter->outbuf = NULL;
                filter->state = ESA_STATE_WAITING_PUSI;
            }
            else
            {
                /* copy this payload fragment into the output buffer */
                gst_es_assembler_append_data(filter, gbr, bytes_avail);
                bytes_avail = 0;
            } /* endif PUSI arrived */
            break;
        default:
            /* should never get here - return to starting state */
            bytes_avail = 0;
            filter->state = ESA_STATE_WAITING_PUSI;
            break;
        } /* endswitch filter->state */
    } /* endwhile bytes_avail */

    /* Set the caps on the buffer before sending */
    if (outbuf)
    {                   
        GstCaps *pcaps;
        // set caps on buffer before pushing
        pcaps = gst_caps_copy(GST_PAD_CAPS(filter->srcpad));
        GST_DEBUG_OBJECT(filter,"Setting buffer caps to %" GST_PTR_FORMAT, pcaps);

        gst_buffer_set_caps(outbuf, pcaps);
        gst_caps_unref(pcaps);
    }

    return outbuf;

} /* gst_es_assembler_process_tp_payload */

static GstBuffer * gst_es_assembler_process_es_packet(GstESAssembler *filter, GstBuffer *buf)
{
    GstBuffer *outbuf = NULL;
    guint8 *pdata = GST_BUFFER_DATA(buf);
    guint32 data32;
    guint8 * pseq_start_code = NULL;
    GstEvent *new_segment = NULL;
    GstCaps  *caps = NULL;
    GstClockTime     input_buf_time = GST_CLOCK_TIME_NONE;
    GstClockTime     running_time = GST_CLOCK_TIME_NONE;
    GstClockTime     pts = GST_CLOCK_TIME_NONE; // PTS as a GstClockTime
    GstClockTime     last_pts;               // last PTS as a GstClockTime
    GstClockTimeDiff pts_offset;             // working value when input buffer timestamped
    GstClockTimeDiff pts_change;             // change in PTS since last arrival
    GstClockTime     expected_running_time;  // pipeline should be roughly here
    GstClockTimeDiff running_time_change;    // change in running time since last arrival
    gboolean reset_times = FALSE;

    GST_DEBUG_OBJECT(filter, "Received es buffer of length %u", GST_BUFFER_SIZE(buf));
    /*
     * Work out how we should timestamp this buffer
     * Buffer timestamp at this point is that obtained from the
     * GstBuffer that contained the PUSI flag
     * which may have been GST_CLOCK_TIME_NULL
     * It may be in pipeline time, or may be in PCR time
     * (depending on whether we had the TS pre-formatter up-stream)
     * If in pipeline time, we will just add ~700ms to the pipeline time
     * as the buffer (PTS) time.
     * If in PCR time, we will add the PTS to PCR offset to the pipeline
     * time and treat that as the buffer (PTS) time. Downstream of this element
     * the buffer time is always in pipeline time.
     */

    running_time = gst_es_assembler_get_running_time(filter);
    input_buf_time = GST_BUFFER_TIMESTAMP(buf);

    /* Create the GstBuffer time information */
    if (filter->PTS_DTS_flags)
    {
        /* Convert the incoming PTS into GstClock units */
        pts = (filter->PTS * NS_PER_90KHZ_TICK_X256) >> 8;

        /* Note the first arriving PTS and the running time when it arrived */
        if (filter->baseline_pts == 0)
        {
            filter->baseline_pts = pts;
            filter->baseline_running_time = (GST_CLOCK_TIME_IS_VALID(running_time) ? running_time : 0) + ESA_FIXED_PTS_OFFSET;
            filter->pts_since_baseline = 0;
            reset_times = TRUE;
            GST_INFO_OBJECT(filter, "Resetting times since received baseline absolute PTS of %llu ns", pts);
        }
        else
        {
            /* Separate out normal playing at 1x from trick modes */
            if (filter->playrate == 1.0)
            {
                /* Normal 1x processing */

                /* Need to detect PTS discontinuity and reset baseline_running_time and baseline_pts */
                /* Also detect chronically late PTS and reset baseline_running_time and baseline_pts */
                if (filter->baseline_pts <= pts)
                {
                    last_pts = filter->baseline_pts + filter->pts_since_baseline;
                }
                else
                {
                    GST_DEBUG_OBJECT(filter, "Would have a negative last pts since saw decrementing PTS");
                    last_pts = filter->baseline_pts - filter->pts_since_baseline;
                }

                pts_change = llabs(GST_CLOCK_DIFF(last_pts, pts));
                expected_running_time = filter->last_buffer_time_ns + ESA_PTS_DEFAULT_INCR;
                running_time_change = GST_CLOCK_DIFF(expected_running_time, running_time);
                GST_DEBUG_OBJECT(filter, "Discontinuity Check: oldpts= %llu, newpts= %llu, abs_delta %llu, delta %lli, baseline %llu, offset %llu",
                        last_pts, pts, pts_change, GST_CLOCK_DIFF(last_pts, pts), filter->baseline_pts, filter->pts_since_baseline);
                GST_DEBUG_OBJECT(filter, "Running Time Check: rt= %llu, exp_rt= %llu, abs_delta %llu ns",
                        running_time, expected_running_time, running_time_change);

                if (pts_change > ESA_PTS_MAX_CHANGE)
                {
                    GST_DEBUG_OBJECT(filter, "PTS discontinuity detected - resetting Running PTS offset");
                    GST_BUFFER_FLAG_SET(buf, GST_BUFFER_FLAG_DISCONT);
                }
                if (running_time_change > ESA_RT_MAX_CHANGE)
                {
                    GST_DEBUG_OBJECT(filter, "Running chronically late resetting Running PTS offset");
                }

                if ((pts_change > ESA_PTS_MAX_CHANGE) || (running_time_change > ESA_RT_MAX_CHANGE))
                {
                    /* Reset baseline running time to current running time plus default offset for latency */
                    filter->baseline_pts = pts;
                    filter->baseline_running_time = (GST_CLOCK_TIME_IS_VALID(running_time) ? running_time : 0) + ESA_FIXED_PTS_OFFSET;
                    filter->pts_since_baseline = 0;
                    GST_DEBUG_OBJECT(filter, "Resetting times since received baseline absolute PTS of %llu ns", pts);
                } /* endif PTS discontinuity */

                // Handle decrementing PTSs
                if (filter->baseline_pts <= pts)
                {
                    filter->pts_since_baseline = pts - filter->baseline_pts;
                }
                else
                {
                    filter->pts_since_baseline = filter->baseline_pts - pts;

                    GST_DEBUG_OBJECT(filter, "Running PTS offset for decrementing PTS of %lld", filter->pts_since_baseline);
                }
            }
            else
            {
                /* Trick mode processing */
                gst_es_assembler_trickmode_discontinuity_check(filter, buf, pts, running_time, input_buf_time);
            }
        } /* endif first arriving PTS */

        /* If not trick mode: If input buffer timestamped, calculate correct output time */
        if ((filter->playrate == 1.0) && GST_CLOCK_TIME_IS_VALID(input_buf_time))
        {
            /* PTS should be later than buffer time */
            pts_offset = GST_CLOCK_DIFF(input_buf_time, pts);
            if ((pts_offset > 0) && (pts_offset < ESA_MAX_PTS_PCR_OFFSET))
            {
                /* TODO: Need to fix this to capture the first PCR and reference PTS offset from that */
                /* Input buffer time was in PCR time */
                /* Add offset to pipeline time */
                GST_BUFFER_TIMESTAMP(buf) = running_time + pts_offset;
                GST_DEBUG_OBJECT(filter, "PCR good on buffer, stamping buffer with time %" GST_TIME_FORMAT,GST_TIME_ARGS (GST_BUFFER_TIMESTAMP(buf)));
            }
            else
            {
                GST_BUFFER_TIMESTAMP(buf) = filter->baseline_running_time + filter->pts_since_baseline;
                GST_DEBUG_OBJECT(filter, "PCR bad on buffer, stamping buffer with time %" GST_TIME_FORMAT,GST_TIME_ARGS (GST_BUFFER_TIMESTAMP(buf)));
            } /* endif incoming buffer timestamp in the same vicinity as PTS */
        }
        else
        {
            if (filter->playrate == 1.0)
            {
                GST_BUFFER_TIMESTAMP(buf) = filter->baseline_running_time + filter->pts_since_baseline;
                GST_DEBUG_OBJECT(filter, "Time absent on buffer rate 1x, stamping buffer with time %" GST_TIME_FORMAT,
                        GST_TIME_ARGS (GST_BUFFER_TIMESTAMP(buf)));
            }
            else
            {
                gst_es_assembler_trickmode_timestamp_buf(filter, buf, input_buf_time, reset_times);
            }
        } /* endif input buffer timestamped */
    }
    else
    {
        GST_BUFFER_TIMESTAMP(buf) = GST_CLOCK_TIME_NONE;
        GST_DEBUG_OBJECT(filter, "PTS absent on buffer, stamping buffer with time %" GST_TIME_FORMAT,GST_TIME_ARGS (GST_BUFFER_TIMESTAMP(buf)));
    } /* endif PTS was present on PES packet */

    /* Make note of last ES buffer time dispatched */
    if (filter->playrate == 1.0)
    {
        filter->last_buffer_time_ns = GST_BUFFER_TIMESTAMP(buf);
    }

    /* Special processing for video */
    if ((filter->stream_id & 0xF0) == 0xE0)
    {
        /* process the packet looking for start codes, frame rate etc. */
        pseq_start_code = gst_es_assembler_look_for_start_codes(filter, buf);
        /* Decide what to do with the buffer */
        switch (filter->pstate)
        {
        case ESP_STATE_WAITING_FOR_SEQ_START:
            if (pseq_start_code != NULL)
            {
                pdata= pseq_start_code;
            }
            /* Waiting for sequence start code. If present process, else discard buffer */
            if (BE_32_AT(pdata) == MPEG_SC_SEQUENCE_HEADER)
            {
                caps = gst_caps_new_simple( caps_formats[ES_FORMAT_MPEGVIDEO],
                                            "mpegversion", G_TYPE_INT, 2,
                                            "systemstream", G_TYPE_BOOLEAN, FALSE,
                                            NULL );

                filter->stream_format = ES_FORMAT_MPEGVIDEO;
                outbuf = buf;
                filter->pstate = ESP_STATE_RUNNING;
                /* Do some special things if we are a non-live pipeline */
                if (!GST_CLOCK_TIME_IS_VALID(running_time)) // Implies not playing yet, hence not live pipeline
                {
                    /* Fix up things to re-start buffer offsets from 0 */
                    if (filter->PTS_DTS_flags)
                    {
                       GST_INFO_OBJECT(filter, "Resetting times due to start code");
                        filter->baseline_pts = pts;
                        filter->baseline_running_time = ESA_FIXED_PTS_OFFSET;
                        filter->pts_since_baseline = 0;
                        GST_BUFFER_TIMESTAMP(buf) = ESA_FIXED_PTS_OFFSET;
                    } /* endif this buffer has a PTS */

                }
                GST_INFO_OBJECT(filter, "Sending new segment event due to new sequence start");
                new_segment = gst_event_new_new_segment_full(FALSE, 1.0, 
                                                             (gdouble)(filter->do_timestamp ? filter->playrate : filter->applied_rate),
                                                              GST_FORMAT_TIME, 0,-1, 0);

            }
            else if ((BE_32_AT(pdata) == MPEG_SC_NAL_UNIT) && (*(pdata+4) == MPEG_NAL_UNIT_TYPE_A_U_D))
            {
                caps = gst_caps_new_simple( caps_formats[ES_FORMAT_H264VIDEO],
                                            NULL );

                filter->stream_format = ES_FORMAT_H264VIDEO;
                outbuf = buf;
                filter->pstate = ESP_STATE_RUNNING;
            }
            else
            {
                gst_buffer_unref(buf);
            } /* endif sequence start code */
            break;
        case ESP_STATE_RUNNING:
            outbuf = buf;
            break;
        default:
            gst_buffer_unref(buf);
            break;
        } /* endswitch pstate */

    }
    else
    {
        /* Must be audio stream - simply pass these all through */
        /* TODO: Need to determine the audio frame interval in case no PTS */

        switch (filter->pstate)
        {
        case ESP_STATE_WAITING_FOR_SEQ_START:
            data32 = BE_32_AT(pdata);
            if ((data32 >> 16) == 0x0B77)
            {
                // AC3. Further determine if AC3 or E-AC3
                // Obtain "bsid" 5 bit field 
                // Should be 6 or 8 for AC3, 16 for E-AC3
                unsigned char bsid;
                data32 = BE_32_AT(pdata+4);
                bsid = ((data32 >> 19) & 0x1F);
                if (bsid <= 8)
                {
                    GST_INFO_OBJECT(filter, "Found AC3 content");
                    caps = gst_caps_new_simple( caps_formats[ES_FORMAT_AC3AUDIO],
                                                // "channels", G_TYPE_INT, 2,
                                                // "rate", G_TYPE_INT, 48000,
                                                NULL );

                    filter->stream_format = ES_FORMAT_AC3AUDIO;
                }
                else if ((bsid >= 11) && (bsid <=16))
                {
                    GST_INFO_OBJECT(filter, "Found EAC3 content");
                    caps = gst_caps_new_simple( caps_formats[ES_FORMAT_EAC3AUDIO],
                                                // "channels", G_TYPE_INT, 2,
                                                // "rate", G_TYPE_INT, 48000,
                                                NULL );
                    filter->stream_format = ES_FORMAT_EAC3AUDIO;
                }
            }
            else if ((data32 >> 20) == 0x00000FFF)
            {
                guint32 layer = ((data32 >> 17) & 0x03);
                if (layer == 0)
                {
                    GST_INFO_OBJECT(filter, "Found MPEG 2/4 AAC (adts) content");
                    caps = gst_caps_new_simple( caps_formats[ES_FORMAT_MPEGAUDIO],
                                                "mpegversion", G_TYPE_INT, 4,
                                                NULL );
                    filter->stream_format = ES_FORMAT_MPEGAUDIO; // MPEG 1/2/4 including AAC
                }
                else
                {    
                    GST_INFO_OBJECT(filter, "Found MPEG 1 layer %d content", layer);
                    caps = gst_caps_new_simple( caps_formats[ES_FORMAT_MPEGAUDIO],
                                                "mpegversion", G_TYPE_INT, 1,
                                                "layer", G_TYPE_INT, layer,
                                                NULL );
                    filter->stream_format = ES_FORMAT_MPEGAUDIO; // MPEG 1/2/4 including AAC
                }
            }
            else if (data32 == 0x41444946)
            {
                GST_INFO_OBJECT(filter, "Found MPEG 2/4 AAC (ADIF) content");
                caps = gst_caps_new_simple( caps_formats[ES_FORMAT_MPEGAUDIO],
                                            "mpegversion", G_TYPE_INT, 4,
                                            NULL );
                filter->stream_format = ES_FORMAT_MPEGAUDIO; // MPEG 1/2/4 including AAC
            }
            else
            {
                filter->stream_format = ES_FORMAT_NONE;      //Unknown
            }

            break;
        case ESP_STATE_RUNNING:
            outbuf = buf;
            break;
        default:
            gst_buffer_unref(buf);
            break;
        } /* endswitch on pstate */

    } /* endif video ES */

    /* If caps created (indicates start of stream, set caps, send new_segment event) */
    if (caps)
    {
        GST_INFO_OBJECT(filter,"Setting output pad caps to %" GST_PTR_FORMAT, caps);
        if (!gst_pad_set_caps(filter->srcpad,caps))
        {
            GST_ERROR_OBJECT(filter,"Unable to set pad caps to new format");
        }
        gst_caps_unref(caps);
        caps = NULL;

        /* Create a default new segment if one was not created above */
        if (new_segment == NULL)
        {
            GST_INFO_OBJECT(filter, "Sending new segment event due to caps change");
            new_segment = gst_event_new_new_segment_full(FALSE, 1.0, 
                                                         (gdouble)(filter->playrate),
                                                          GST_FORMAT_TIME, 0,-1, 0);
        }
        (void)gst_pad_push_event(filter->srcpad, new_segment);
        /* don't unref the new_segment - push took ownership */
        outbuf = buf;
        filter->pstate = ESP_STATE_RUNNING;
    }

    return outbuf;

} /* gst_es_assembler_process_es_packet */

/*
    Function: gst_es_assembler_trickmode_discontinuity_check

    Description:
    The pipeline clock will continue to increase monotonically at 1.0x rate
    We need to deal with incoming PTS values which are unmodified from the original
    stream, and adjust them to make them get consumed faster or slower

    The logic is that PTSout = (PTSin - PTSbaseline)/playrate

    For reverse play, PTS may be decreasing, but playrate should be negative, so
    PTSout will also be later than PTSbaseline.

    For playrate of zero, pipeline is paused so we can ignore those.

    If discontinuity is detected, baseline pts and running times are reset, along
    with disconinuity flag set on buffer.  If input buf time is invalid, this
    indicates a new segment or file is starting on server side.  Send out
    new segment event when this condition is detected.

*/
static void gst_es_assembler_trickmode_discontinuity_check(GstESAssembler *filter,
                                                           GstBuffer *buf,
                                                           GstClockTime pts,
                                                           GstClockTime running_time,
                                                           GstClockTime input_buf_time)
{
    GstClockTime     last_pts;               // last PTS as a GstClockTime
    GstClockTimeDiff pts_change;             // change in PTS since last arrival
    GstClockTimeDiff pts_diff_scaled;
    guint64 max_pts_change = 0;
    gboolean running_late = FALSE;

    /* For first sample, leave PTS as-is*/
    if (pts != filter->baseline_pts)
    {
        pts_diff_scaled = filter->baseline_pts - pts;
        /* Extend fractional rates to 2 decimals */
        pts_diff_scaled /= (GstClockTimeDiff)(filter->playrate * 100);
        pts_diff_scaled *= 100;

        /* pts_diff_scaled should be positive, but make sure */
        if (filter->playrate > 0)
        {
            pts = filter->baseline_pts + llabs(pts_diff_scaled);
        }
        else
        {
            pts = filter->baseline_pts - llabs(pts_diff_scaled);
        }

        /* Need to detect PTS discontinuity and reset baseline_running_time and baseline_pts */
        last_pts = filter->baseline_pts + filter->pts_since_baseline;
        pts_change = llabs(GST_CLOCK_DIFF(last_pts, pts));

        // Check if buffer timestamps are really late compared with pipeline clock
        if (GST_CLOCK_TIME_IS_VALID(input_buf_time))
        {
            guint64 duration = input_buf_time - filter->last_media_time_ns;
            if (input_buf_time < filter->last_media_time_ns)
            {
                duration = filter->last_media_time_ns - input_buf_time;
            }

            GstClockTime now = gst_es_assembler_get_pipeline_time(filter);
            GstClockTime base_time = gst_element_get_base_time(GST_ELEMENT(filter));
            GstClockTime stream_time = now - base_time;
            GstClockTime end = filter->last_buffer_time_ns + duration;
            if (end < stream_time)
            {
                GstClockTimeDiff end_delta = stream_time - end;
                if (end_delta > (GST_SECOND * 1L))
                {
                    GST_ERROR_OBJECT(filter,
                            "running late, end time = %" GST_TIME_FORMAT ", is less than stream_time = %" GST_TIME_FORMAT,
                            GST_TIME_ARGS(end), GST_TIME_ARGS(stream_time));

                    running_late = TRUE;
                }
            }
        }
        else
        {
            GST_INFO_OBJECT(filter, "Invalid input buf time= %llu", input_buf_time);
        }

        max_pts_change = ESA_PTS_MAX_CHANGE * llabs(filter->playrate);
        if ((pts_change > max_pts_change) || (running_late))
        {
            GST_DEBUG_OBJECT(filter, "Resetting due to PTS discontinuity detected, - change: %llu ns, max allowed: %llu ns",
                    pts_change, max_pts_change);

            GST_BUFFER_FLAG_SET(buf, GST_BUFFER_FLAG_DISCONT);

            /* Reset baseline running time to current running time plus default offset for latency */
            filter->baseline_pts = pts;
            filter->baseline_running_time = (GST_CLOCK_TIME_IS_VALID(running_time) ? running_time : 0) + ESA_FIXED_PTS_OFFSET;
            filter->pts_since_baseline = 0;

            /* If this is a start of a new segment which is indicated by 0 input time and non-zero last time,
             * send out new segment event */
            if ((!GST_CLOCK_TIME_IS_VALID(input_buf_time)) || (running_late))
            {
                GST_WARNING_OBJECT(filter, "Detected start of new segment, sending out event");
                // Send a flush start & flush stop prior to new segment to clear out previous buffers
                GstEvent *new_segment;
                (void)gst_pad_push_event(filter->srcpad, gst_event_new_flush_start());
                (void)gst_pad_push_event(filter->srcpad, gst_event_new_flush_stop());

                new_segment = gst_event_new_new_segment_full(FALSE, 1.0,
                        (gdouble)filter->playrate,
                        GST_FORMAT_TIME, 0,-1, 0);
                (void)gst_pad_push_event(filter->srcpad, new_segment);
            }
            else
            {
                /* Don't send new segment event when media times don't indicate start of new file */
                GST_DEBUG_OBJECT(filter,
                        "Detected start of new segment but valid media time, but not sending out event, input= %llu, last= %llu",
                        input_buf_time, filter->last_buffer_time_ns);
            }

            // When running late, have valid input buf time so update media time
            if (running_late)
            {
                GST_INFO_OBJECT(filter, "running late so adjusting media time to: %llu", input_buf_time);
                filter->last_media_time_ns = input_buf_time;
                filter->last_buffer_time_ns = filter->baseline_running_time;
             }
        } /* endif PTS discontinuity */
        else
        {
            /* Initialize last buffer time to initial value to be used */
            if (!GST_CLOCK_TIME_IS_VALID(filter->last_buffer_time_ns))
            {
                filter->last_buffer_time_ns = filter->baseline_running_time;
                filter->last_media_time_ns = 0;
                GST_INFO_OBJECT(filter, "Initializing last buf time to baseline rt= %llu", filter->last_buffer_time_ns);
            }
        }
    } /* endif not first sample */
    else
    {
        GST_DEBUG_OBJECT(filter, "Skipping trick mode processing on first sample");
    }
}

/*
    Function: gst_es_assembler_trickmode_timestamp_buf

    Description:
    This function is called when playrate is in trick mode (!= 1.0).
    It relies on the input media time which is set based on chunk header
    values when reading data from socket in MPE layer.  It utilizes the
    gst clock and timestamp of last/previous buffer.  It will calculation
    a duration based on last media time and incoming media time.
    The absolute difference between these two will be used as the duration
    or increment in new timestamp since timestamps are always increasing
    even when rewinding.
*/
static void gst_es_assembler_trickmode_timestamp_buf(GstESAssembler *filter,
                                                     GstBuffer *buf,
                                                     GstClockTime input_media_time,
                                                     gboolean reset_times)
{
    guint64 duration = 0;
    GstClockTime input_buf_time = input_media_time;

    /* Determine if input media time which is incoming buffer time stamp is valid */
    if (GST_CLOCK_TIME_IS_VALID(input_buf_time))
    {
        /* Initialize last media time received if necessary */
        if (filter->last_media_time_ns == 0)
        {
            filter->last_media_time_ns = input_buf_time;
            filter->last_buffer_time_ns = filter->baseline_running_time;
            GST_INFO_OBJECT(filter, "Initializing last media time= %llu", filter->last_media_time_ns);
        }

        /* Calculate difference between new and last media times do determine how long buffer should be displayed */
        if (input_buf_time >= filter->last_media_time_ns)
        {
            duration = input_buf_time - filter->last_media_time_ns;
        }
        else
        {
            duration = filter->last_media_time_ns - input_buf_time;
        }

        /* Update last media time */
        filter->last_media_time_ns = input_buf_time;

        /* Scaled duration based on playrate */
        duration /= (GstClockTimeDiff)(llabs(filter->playrate) * 100);
        duration *= 100;

        /* If last buffer time valid, add duration and use for timestamp */
        if (GST_CLOCK_TIME_IS_VALID(filter->last_buffer_time_ns))
        {
            filter->last_buffer_time_ns += duration;

            GST_BUFFER_TIMESTAMP(buf) = filter->last_buffer_time_ns;
        }
        else /* Last buf time was not valid */
        {
            /* Last buffer is not valid, timestamp with none until valid time is available */
            GST_DEBUG_OBJECT(filter, "Last buf time invalid, time stamping with zero");
            GST_BUFFER_TIMESTAMP(buf) = GST_CLOCK_TIME_NONE;

        } /* end last buf time check */
    } /* end valid media time */
    else /* input time invalid */
    {
        /* Reset last media time to zero so it will be reset when valid time is received */
        GST_INFO_OBJECT(filter, "Input time invalid %llu, resetting last media time to zero",
                input_buf_time);
        filter->last_media_time_ns = 0;

        /* Skip time stamp on buffer until valid value is calculated */
        GST_BUFFER_TIMESTAMP(buf) = GST_CLOCK_TIME_NONE;

    } /* end input time invalid */

    /* If flag is set which indicates pts has been reset, timestamp with none */
    if (reset_times)
    {
        GST_INFO_OBJECT(filter, "Saw reset flag, time stamping with zero");
        GST_BUFFER_TIMESTAMP(buf) = GST_CLOCK_TIME_NONE;
    }

    GST_DEBUG_OBJECT(filter, "Trickmode, stamping buffer with time %" GST_TIME_FORMAT ", input= %llu, last buf= %llu, duration= %llu",
            GST_TIME_ARGS(GST_BUFFER_TIMESTAMP(buf)), input_buf_time, filter->last_buffer_time_ns, duration);
}

/*
    Function: gst_es_assembler_consume_sequence_header

    Description:
    This function is called with pdata pointing at the first byte following the
    sequence_header start code (0x000001B3). It then parses out the sequence_header
    looking for useful things like frame rate, aspect ratio, bitrate etc. and puts
    them into the filter state data.

    On exit, the contents of plen have been reduced by the data consumed, and the
    new pdata is returned.

*/
static guint8 *gst_es_assembler_consume_sequence_header(GstESAssembler *filter, guint8 *pdata, guint32 *plen)
{
    guint32 temp;
    guint8  temp8;

    if (*plen >= 4)
    {
        temp = BE_32_AT(pdata);
        pdata += 4;
        *plen -= 4;

        filter->horizontal_size_value    = (temp >> 20) & 0x0FFF;
        filter->vertical_size_value      = (temp >> 8)  & 0x0FFF;
        filter->aspect_ratio_information = (temp >> 4) & 0x000F;
        filter->frame_rate_code          = temp & 0x000F;
        filter->buffer_time_delta_ns     = frame_delta_ns[filter->frame_rate_code];

        GST_DEBUG_OBJECT(filter,"Sequence header:");
        GST_DEBUG_OBJECT(filter,"Video is %d x %d", filter->horizontal_size_value, filter->vertical_size_value);
        GST_DEBUG_OBJECT(filter,"Aspect ratio %d (%s)", filter->aspect_ratio_information,
                (filter->aspect_ratio_information == 1 ? "Square Pixel" :
                (filter->aspect_ratio_information == 2 ? "4:3" :
                (filter->aspect_ratio_information == 3 ? "16:9" :
                (filter->aspect_ratio_information == 4 ? "2.21:1" : "Reserved/Forbidden")))));
#ifdef DEBUG_STREAM
        {
            GST_DEBUG_OBJECT(filter,"Frame rate code %d %s", filter->frame_rate_code,
                             gst_es_assembler_decode_frame_rate(filter->frame_rate_code));
        }
#endif
    }
    else
    {
        GST_WARNING_OBJECT(filter, "Insufficient data to parse horizontal_size_value/vertical_size_value/aspect_ratio_information/frame_rate_code\n");
        pdata = NULL;
        *plen = 0;
    }

    if (*plen >= 4)
    {
        temp = BE_32_AT(pdata);
        pdata += 4;
        *plen -= 4;
    
        filter->bit_rate_value = (temp >> 14) & 0x0003FFFF;
        if (temp & 0x00000002)
        {
            if (*plen >= 64)
            {
                pdata +=63;
                *plen -= 64;
                temp8 = *(pdata++);
                temp = (guint32) temp8;
            }
            else
            {
                GST_WARNING_OBJECT(filter, "Insufficient data to skip over intra_quantiser_matrix\n");
                pdata = NULL;
                *plen = 0;
            }
        }
        if (temp & 0x1)
        {
            if (*plen >= 64)
            {
                pdata += 64;
                *plen -= 64;
            }
            else
            {
                GST_WARNING_OBJECT(filter, "Insufficient data to skip over non_intra_quantiser_matrix\n");
                pdata = NULL;
                *plen = 0;
            }
        }
    }
    else
    {
        GST_WARNING_OBJECT(filter, "Insufficient data to parse bit_rate_value/marker_bit/vbv_buffer_size_value/constrained_parameters_flag\n");
        pdata = NULL;
        *plen = 0;
    }

    return pdata;

} /* gst_es_assembler_consume_sequence_header */

/*
    Function: gst_es_assembler_consume_picture_header

    Description:
    This function is called with pdata pointing at the first byte following the
    sequence_header start code (0x00000100). It then parses out the picture_header
    looking for useful things like frame rate, aspect ratio, bitrate etc. and puts
    them into the filter state data.

    On exit, the contents of plen have been reduced by the data consumed, and the
    new pdata is returned.

*/
static guint8 *gst_es_assembler_consume_picture_header(GstESAssembler *filter, guint8 *pdata, guint32 *plen)
{
    guint32  temp;
    guint8   picture_type;
    if (*plen >= 4)
    {
        *plen -= 4;
        temp = BE_32_AT(pdata);
        pdata +=4;
        picture_type = (temp >> 19) & 3;
        filter->last_picture_type = picture_type;
#ifdef DEBUG_STREAM
        {
            guint16  temporal_reference = (temp >> 22) & 0x3FF;
            guint16  vbv_delay = temp & 0x0000FFFF;

            GST_DEBUG_OBJECT(filter,"       Picture type:       %s",
                         (picture_type == 1 ? "I" : (picture_type == 2 ? "P" : (picture_type == 3 ? "B" : "(unknown)"))));
            GST_DEBUG_OBJECT(filter,"       Temporal reference: 0x%3.3X", temporal_reference);
            GST_DEBUG_OBJECT(filter,"       VBV delay:          0x%4.4X", vbv_delay);
        }
#endif
    }
    else
    {
        GST_WARNING_OBJECT(filter, "Insufficient data to parse picture type\n");
        pdata = NULL;
        *plen = 0;
    }

    return pdata;

} /* gst_es_assembler_consume_picture_header */

/*
    Function: gst_es_assembler_consume_gop_header

    Description:
    This function is called with pdata pointing at the first byte following the
    sequence_header start code (0x000001b8). It then parses out the picture_header
    looking for useful things like frame rate, aspect ratio, bitrate etc. and puts
    them into the filter state data.

    On exit, the contents of plen have been reduced by the data consumed, and the
    new pdata is returned.

*/
static guint8 *gst_es_assembler_consume_gop_header(GstESAssembler *filter, guint8 *pdata, guint32 *plen)
{
    if (*plen >= 4)
    {
        *plen -= 4;

#ifdef DEBUG_STREAM
        {
            guint32 temp = BE_32_AT(pdata);

            guint32 time_code = (temp >> 7) & 0x01FFFFFF;
            guint8 closed_gop = (temp >> 6) & 1;
            guint16 broken_link = (temp >> 5) & 1;

            GST_DEBUG_OBJECT(filter,"       time_code:           0x%7.7X", time_code);
            GST_DEBUG_OBJECT(filter,"       closed_gop:          %d", closed_gop);
            GST_DEBUG_OBJECT(filter,"       broken_link:         %d", broken_link);
        }
#endif
        pdata +=4;
    }
    else
    {
        GST_WARNING_OBJECT(filter, "Insufficient data to parse GOP header.\n");
        pdata = NULL;
        *plen = 0;
    }

    return pdata;

} /* gst_es_assembler_consume_gop_header */

/*
    Function: gst_es_assembler_consume_sequence_header

    Description:
    This function is called with pdata pointing at the first byte following the
    sequence_header start code (0x000001B3). It then parses out the sequence_header
    looking for useful things like frame rate, aspect ratio, bitrate etc. and puts
    them into the filter state data.

    On exit, the contents of plen have been reduced by the data consumed, and the
    new pdata is returned.

*/
static guint8 *gst_es_assembler_consume_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen)
{
    guint8  temp8;

    if (*plen >= 1)
    {
        temp8 = *pdata;
        switch ((temp8 >> 4) & 0x0F)
        {
        case 1: pdata = gst_es_assembler_consume_sequence_extension(filter, pdata, plen); break;
        case 2: pdata = gst_es_assembler_consume_sequence_display_extension(filter, pdata, plen); break;
        case 3: pdata = gst_es_assembler_consume_quant_matrix_extension(filter, pdata, plen); break;
        case 4: pdata = gst_es_assembler_consume_copyright_extension(filter, pdata, plen); break;
        case 5: pdata = gst_es_assembler_consume_sequence_scalable_extension(filter, pdata, plen); break;
        case 7: pdata = gst_es_assembler_consume_picture_display_extension(filter, pdata, plen); break;
        case 8: pdata = gst_es_assembler_consume_picture_coding_extension(filter, pdata, plen); break;
        case 9: pdata = gst_es_assembler_consume_picture_spatial_scalable_extension(filter, pdata, plen); break;
        case 10: pdata = gst_es_assembler_consume_picture_temporal_scalable_extension(filter, pdata, plen); break;
        case 11: pdata = gst_es_assembler_consume_camera_parameters_extension(filter, pdata, plen); break;
        case 12: pdata = gst_es_assembler_consume_itu_t_extension(filter, pdata, plen); break;
        default: break;    
        }
    }
    return pdata;

} /* gst_es_assembler_consume_extension */

#ifdef DEBUG_STREAM
static guint8 *gst_es_assembler_consume_sequence_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) 
{
    if (*plen >= 6)
    {
        guint32 temp = BE_32_AT(pdata);

        pdata += 4;
        *plen -= 4;
        guint16 temp16 = BE_16_AT(pdata);

        pdata += 2;
        *plen -= 2;

        guint8 profile_and_level = (temp >> 20) & 0x000000FF;
        guint8 progressive_sequence = (temp >> 19) & 1;
        guint8 chroma_format = (temp >> 17) & 3;
        guint8 horizontal_size_ext = (temp >> 15) & 3;
        guint8 vertical_size_ext = (temp >> 13) & 3;
        guint16 bit_rate_extension = (temp >> 1) & 0x0FFF;

        guint8 vbv_buffer_size_extension = temp16 >> 8;
        guint8 low_delay = (temp16 >> 7) & 1;
        guint8 frame_rate_extension_n = (temp16 >> 5) & 3;
        guint8 frame_rate_extension_d = (temp16 & 0x1F);

        GST_DEBUG_OBJECT(filter,"Sequence extension header:");
        GST_DEBUG_OBJECT(filter,"       profile_and_level         0x%2.2X %s", profile_and_level, gst_es_assembler_decode_profile_and_level(profile_and_level));
        GST_DEBUG_OBJECT(filter,"       progressive_sequence      %d", progressive_sequence);
        GST_DEBUG_OBJECT(filter,"       chroma_format             %d", chroma_format);
        GST_DEBUG_OBJECT(filter,"       horizontal_size_ext       %d", horizontal_size_ext);
        GST_DEBUG_OBJECT(filter,"       vertical_size_ext         %d", vertical_size_ext);
        GST_DEBUG_OBJECT(filter,"       bit_rate_extension        0x%2.2X", bit_rate_extension);
        GST_DEBUG_OBJECT(filter,"       vbv_buffer_size_extension 0x%2.2X", vbv_buffer_size_extension);
        GST_DEBUG_OBJECT(filter,"       low_delay                 %d", low_delay);
        GST_DEBUG_OBJECT(filter,"       frame_rate_extension_n    %d", frame_rate_extension_n);
        GST_DEBUG_OBJECT(filter,"       frame_rate_extension_d    0x%2.2X", frame_rate_extension_d);
    }
    else
    {
        GST_WARNING_OBJECT(filter, "Insufficient data to parse bit_rate_value/marker_bit/vbv_buffer_size_value/constrained_parameters_flag\n");
        pdata = NULL;
        *plen = 0;
    }
    return pdata;
}
#else
static guint8 *gst_es_assembler_consume_sequence_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen)
{
    if (*plen >= 6)
    {
        pdata += 6;
        *plen -= 6;
    }
    else
    {
        GST_WARNING_OBJECT(filter, "Insufficient data to parse bit_rate_value/marker_bit/vbv_buffer_size_value/constrained_parameters_flag\n");
        pdata = NULL;
        *plen = 0;
    }
    return pdata; 
}
#endif

static guint8 *gst_es_assembler_consume_sequence_display_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) { GST_DEBUG_OBJECT(filter,"Sequence display extension header"); return pdata; }
static guint8 *gst_es_assembler_consume_quant_matrix_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) {  GST_DEBUG_OBJECT(filter,"Quant matrix extension header");return pdata; }
static guint8 *gst_es_assembler_consume_copyright_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) {  GST_DEBUG_OBJECT(filter,"Copyright extension header");return pdata; }
static guint8 *gst_es_assembler_consume_sequence_scalable_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) { GST_DEBUG_OBJECT(filter,"Sequence scalable extension header"); return pdata; }
static guint8 *gst_es_assembler_consume_picture_display_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) { GST_DEBUG_OBJECT(filter,"Picture display extension header"); return pdata; }

#ifdef DEBUG_STREAM
    static guint8 *gst_es_assembler_consume_picture_coding_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen)
    {
        guint32 temp = 0;
        guint8 temp8 = 0;
        guint8 progressive_frame = 0;
        guint8 composite_display_flag = 0;
        guint8 fcode00 = 0;
        guint8 fcode01 = 0;
        guint8 fcode10 = 0;
        guint8 fcode11 = 0;
        guint8 intra_dc_precision = 0;
        guint8 picture_structure = 0;
        guint8 top_field_first = 0;
        guint8 frame_pred_frame_dct = 0;
        guint8 concealment_motion_vectors = 0;
        guint8 q_scale_type = 0;
        guint8 intra_vlc_format = 0;
        guint8 alternate_scan = 0;
        guint8 repeat_first_field = 0;
        guint8 chroma_420_type = 0;

        if (*plen >= 4)
        {
            temp = BE_32_AT(pdata);
            pdata += 4;
            *plen -= 4;
            fcode00 = (temp >> 24) & 0x0F;
            fcode01 = (temp >> 20) & 0x0F;
            fcode10 = (temp >> 16) & 0x0F;
            fcode11 = (temp >> 12) & 0x0F;
            intra_dc_precision = (temp >> 10) & 3;
            picture_structure = (temp >> 8) & 3;
            top_field_first = (temp >> 7) & 1;
            frame_pred_frame_dct = (temp >> 6) & 1;
            concealment_motion_vectors = (temp >> 5) & 1;
            q_scale_type = (temp >> 4) & 1;
            intra_vlc_format = (temp >> 3) & 1;
            alternate_scan = (temp >> 2) & 1;
            repeat_first_field = (temp >> 1) & 1;
            chroma_420_type = temp & 1;

            GST_DEBUG_OBJECT(filter,"Picture coding extension header:");
            GST_DEBUG_OBJECT(filter,"       fcode00                    0x%1.1X", fcode00);
            GST_DEBUG_OBJECT(filter,"       fcode01                    0x%1.1X", fcode01);
            GST_DEBUG_OBJECT(filter,"       fcode10                    0x%1.1X", fcode10);
            GST_DEBUG_OBJECT(filter,"       fcode11                    0x%1.1X", fcode11);
            GST_DEBUG_OBJECT(filter,"       intra_dc_precision         %d", intra_dc_precision);
            GST_DEBUG_OBJECT(filter,"       picture_structure          %d %s", picture_structure, gst_es_assembler_decode_picture_structure(picture_structure));
            GST_DEBUG_OBJECT(filter,"       top field first            %d", top_field_first);
            GST_DEBUG_OBJECT(filter,"       frame_pred_frame_dct       %d", frame_pred_frame_dct);
            GST_DEBUG_OBJECT(filter,"       concealment_motion_vectors %d", concealment_motion_vectors);
            GST_DEBUG_OBJECT(filter,"       q_scale_type               %d", q_scale_type);
            GST_DEBUG_OBJECT(filter,"       intra_vlc_format           %d", intra_vlc_format);
            GST_DEBUG_OBJECT(filter,"       alternate_scan             %d", alternate_scan);
            GST_DEBUG_OBJECT(filter,"       repeat_first_field         %d", repeat_first_field);
            GST_DEBUG_OBJECT(filter,"       chroma_420_type            %d", chroma_420_type);

            if (*plen >= 1)
            {
                temp8 = *(pdata++);
                (*plen)--;

                progressive_frame = (temp8 >> 7) & 1;
                composite_display_flag = (temp8 >> 6) & 1;

                if (*plen >= 1)
                {
                    GST_DEBUG_OBJECT(filter,"       progressive_frame          %d", progressive_frame);
                    GST_DEBUG_OBJECT(filter,"       composite_display_flag     %d", composite_display_flag);
                }
                else
                {
                    GST_WARNING_OBJECT(filter, "Insufficient data to parse progressive_frame.\n");
                    pdata = NULL;
                    *plen = 0;
                }
            }
            else
            {
                GST_WARNING_OBJECT(filter, "Insufficient data to parse picture_coding_extension\n");
                pdata = NULL;
                *plen = 0;
            }
        }
        return pdata;
    }
#else
    static guint8 *gst_es_assembler_consume_picture_coding_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen)
    {
        if (*plen >= 4)
        {
            pdata += 4;
            *plen -= 4;
            if (*plen >= 1)
            {
                pdata++;
                (*plen)--;

                if (*plen < 1)
                {
                    GST_WARNING_OBJECT(filter, "Insufficient data to parse progressive_frame.\n");
                    pdata = NULL;
                    *plen = 0;
                }
            }
            else
            {
                GST_WARNING_OBJECT(filter, "Insufficient data to parse picture_coding_extension\n");
                pdata = NULL;
                *plen = 0;
            }
        }
        return pdata;
    }
#endif

static guint8 *gst_es_assembler_consume_picture_spatial_scalable_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) { GST_DEBUG_OBJECT(filter,"Picture spatial scalable extension header"); return pdata; }
static guint8 *gst_es_assembler_consume_picture_temporal_scalable_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) { GST_DEBUG_OBJECT(filter,"Picture temporal scalable extension header"); return pdata; }
static guint8 *gst_es_assembler_consume_camera_parameters_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) { GST_DEBUG_OBJECT(filter,"Camera parameters extension header"); return pdata; }
static guint8 *gst_es_assembler_consume_itu_t_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen) { GST_DEBUG_OBJECT(filter,"ITU-T extension header"); return pdata; }

#ifdef DEBUG_STREAM
static char * gst_es_assembler_decode_picture_structure(int picture_structure)
{
    switch (picture_structure)
    {
    case 0: return "(Reserved)"; break;
    case 1: return "(Top Field)"; break;
    case 2: return "(Bottom Field)"; break;
    case 3: return "(Frame Picture)"; break;
    default: return "(Reserved)"; break;
    }
    return "";
}

static char * gst_es_assembler_decode_frame_rate(int frame_rate_code)
{
    switch (frame_rate_code)
    {
    case 0: return "(Forbidden)"; break;
    case 1: return "(23.976 FPS)"; break;
    case 2: return "(24 FPS)"; break;
    case 3: return "(25 FPS)"; break;
    case 4: return "(29.97 FPS)"; break;
    case 5: return "(30 FPS)"; break;
    case 6: return "(50 FPS)"; break;
    case 7: return "(59.94 FPS)"; break;
    case 8: return "(60 FPS)"; break;
    default: return "(Reserved)"; break;
    }
    return "";
}

static char * gst_es_assembler_decode_profile_and_level(int profile_and_level)
{
    switch (profile_and_level)
    {
    case 0x14: return "(HP@HL)"; break;
    case 0x16: return "(HP@High1440)"; break;
    case 0x18: return "(HP@ML)"; break;
    case 0x1A: return "(HP@LL)"; break;
    case 0x24: return "(SpaScalP@HL)"; break;
    case 0x26: return "(SpaScalP@High1440)"; break;
    case 0x28: return "(SpaScalP@ML)"; break;
    case 0x2A: return "(SpaScalP@LL)"; break;
    case 0x34: return "(SNRScalP@HL)"; break;
    case 0x36: return "(SNRScalP@High1440)"; break;
    case 0x38: return "(SNRScalP@ML)"; break;
    case 0x3A: return "(SNRScalP@LL)"; break;
    case 0x44: return "(MP@HL)"; break;
    case 0x46: return "(MP@High1440)"; break;
    case 0x48: return "(MP@ML)"; break;
    case 0x4A: return "(MP@LL)"; break;
    case 0x54: return "(SP@HL)"; break;
    case 0x56: return "(SP@High1440)"; break;
    case 0x58: return "(SP@ML)"; break;
    case 0x5A: return "(SP@LL)"; break;

    case 0x8E: return "(MVP@LL)"; break;
    case 0x8D: return "(MVP@ML)"; break;
    case 0x8B: return "(MVP@High1440)"; break;
    case 0x8A: return "(MVP@HL)"; break;
    case 0x85: return "(422@ML)"; break;
    case 0x82: return "(422@HL)"; break;
    default: return "(Reserved)"; break;
    }
    return "";
}
#endif

#if 0
static guint8 *gst_es_assembler_consume_sequence_extension(GstESAssembler *filter, guint8 *pdata, guint32 *plen)
{
    guint32 temp;
    guint16 temp16;

    temp = BE_32_AT(pdata);
    pdata += 4;
    temp16 = BE_16_AT(pdata);
    pdata += 2;
    *plen -= 6;

    return pdata;

} /* gst_es_assembler_consume_sequence_extension */
#endif

static guint8 * gst_es_assembler_look_for_start_codes(GstESAssembler *filter, GstBuffer *buf)
{
    guint8 *pdata = GST_BUFFER_DATA(buf);
    guint32 buflen = GST_BUFFER_SIZE(buf);
    guint8  state = 0;
    guint8  data;
    guint8 *rc = NULL;

    while (buflen)
    {
        data = *(pdata++);
        buflen--;
        switch (state)
        {
        case 0:     // have nothing
            if (!data)
            {
                state = 1;
            }
            break;
        case 1:     // have first 0
            if (!data)
            {
                state = 2;
            }
            else
            {
                state = 0;
            }
            break;
        case 2:     // have second 0
            if (data == 0x01)
            {
                state = 3;
            }
            else if (data == 0)
            {
                state = 2;
            }
            else
            {
                state = 0;
            }
            break;
        case 3:     // have startcode 0x01
            state = 0;
            if (data == 0)
            {
                pdata = gst_es_assembler_consume_picture_header(filter, pdata, &buflen);
            }
            else if (data < 0xB0)
            {
                //gprintf("Slice Header\n");
            }
            else
            {
                switch (data)
                {
                case 0xb0: GST_DEBUG_OBJECT(filter,"Reserved (0xb0) start code"); break;
                case 0xb1: GST_DEBUG_OBJECT(filter,"Reserved (0xb1) start code"); break;
                case 0xb2: GST_DEBUG_OBJECT(filter,"user_data_start_code"); break;
                case 0xb3:
                    GST_DEBUG_OBJECT(filter,"sequence_header_code");
                    /* Get the sequence header info of interest */
                    if (filter->pstate == ESP_STATE_WAITING_FOR_SEQ_START)
                    {
                        rc = pdata-4;
                    }
                    pdata = gst_es_assembler_consume_sequence_header(filter, pdata, &buflen);
                    break;
                case 0xb4: GST_DEBUG_OBJECT(filter,"sequence_error_code"); break;
                case 0xb5:
                    GST_DEBUG_OBJECT(filter,"extension_start_code");
                    pdata = gst_es_assembler_consume_extension(filter, pdata, &buflen);
                    break;
                case 0xb6: GST_DEBUG_OBJECT(filter,"Reserved (0xb6) start code"); break;
                case 0xb7: GST_DEBUG_OBJECT(filter,"sequence_end_code"); break;
                case 0xb8: 
                    GST_DEBUG_OBJECT(filter,"group_start_code"); 
                    pdata = gst_es_assembler_consume_gop_header(filter, pdata, &buflen);
                    break;
                default:   GST_DEBUG_OBJECT(filter,"System start code 0x%2.2X",data); break;
                } /* endswitch data (start code) */
            }
            break;
        default:
            state = 0;
            break;
        }
    }
    return rc;
}

static GstESAssemberState gst_es_assembler_process_stream_id(GstESAssembler *filter, guint8 stream_id)
{
    GstESAssemberState ret = ESA_STATE_WAITING_PUSI; // Default to "unsupported type and discard"

    GST_DEBUG_OBJECT(filter,"Received stream ID of 0x%2.2X", stream_id);
    /* Only support audio and video PES streams for now */
    /* Note that advanced audio codecs (AC3, AAC etc) use stream id 0xBD (private_stream_1) */
    if (((stream_id & 0xE0) == 0xC0) || ((stream_id & 0xF0) == 0xE0) || stream_id == 0xBD)
    {
        filter->stream_id = stream_id;
        ret = ESA_STATE_WAITING_H4;
    }
    return ret;
} /* gst_es_assembler_process_stream_id */


static void gst_es_assembler_append_data(GstESAssembler *filter, GstBitReader *gbr, guint8 bytes_avail)
{
    guint64 space_left;
    GstBuffer *buf;
    GstBuffer *newbuf = NULL;
    int buf_size = 0;           // size of buffer in bytes to request
    GstPad* peer_pad = NULL;    // peer pad which is queried for buffer allocation
    GstFlowReturn  alloc_ret = GST_FLOW_OK; // return code from buffer allocation

    if (filter->outbuf != NULL)
    {
        buf = filter->outbuf;
        space_left = GST_BUFFER_SIZE(buf) - GST_BUFFER_OFFSET(buf);
        /* Check enough space left in the buffer */
        if (space_left < bytes_avail)
        {
           GST_DEBUG_OBJECT(filter,
                 "Not enough space left in the ES Buffer (current size %d, space left %llu, need %d): - realloc the buffer larger\n",
                 GST_BUFFER_SIZE(buf), space_left, bytes_avail);

           // Allocate a new buffer twice the size of the existing if this is large enough
           if (((guint64)GST_BUFFER_SIZE(buf) * 2) > ((guint64)GST_BUFFER_SIZE(buf) + (guint64)bytes_avail - space_left))
           {
              buf_size = GST_BUFFER_SIZE(buf) * 2;

              // Get the pad to which the output pad is attached to determine if it supports buffer allocation
              peer_pad = gst_pad_get_peer(filter->srcpad);

              // Get buffer from element attached to source/output pad if it has a buffer allocate function
              if ((NULL != peer_pad) && (NULL != peer_pad->bufferallocfunc))
              {
                 // Ask downstream element for buffer
                 alloc_ret = gst_pad_alloc_buffer(filter->srcpad,
                       GST_BUFFER_OFFSET_NONE,
                       buf_size,
                       filter->srcpad->caps,
                       &newbuf);
                 if (G_LIKELY(!GST_FLOW_IS_SUCCESS(alloc_ret)))
                 {
                    GST_ERROR_OBJECT(filter, "Failed to allocate %d bytes of memory for buffer extension",
                          buf_size);

                    // Unref the peer pad
                    if (G_LIKELY(peer_pad != NULL))
                    {
                       gst_object_unref(peer_pad);
                    }
                    return;
                 }

                 // Unref the peer pad
                 if (G_LIKELY(peer_pad != NULL))
                 {
                    gst_object_unref(peer_pad);
                 }
              }
              else
              {
                 // Allocate the directly buffer here rather than asking downstream element for one
                 newbuf = gst_buffer_try_new_and_alloc(buf_size);
              }

              // Copy data that is in the existing buffer to newly allocated larger buffer
              memcpy(GST_BUFFER_DATA(newbuf), GST_BUFFER_DATA(buf), GST_BUFFER_OFFSET(buf));
              GST_BUFFER_OFFSET(newbuf) += GST_BUFFER_OFFSET(buf);

              // Free the old out buffer
              gst_buffer_unref(buf);

              // Set buffer pointer to point to newly allocated buffer
              buf = newbuf;
           }
           else
           {
              GST_WARNING_OBJECT(filter, "Not enough space even when allocating twice the buffer\n");
              return;
           }
        }

        /* Now append the data */
        memcpy(GST_BUFFER_DATA(buf)+GST_BUFFER_OFFSET(buf), gbr->data + gbr->byte, bytes_avail);
        GST_BUFFER_OFFSET(buf) += bytes_avail;

        // If allocated a new buffer which is larger, point to new larger buffer
        if (NULL != newbuf)
        {
           filter->outbuf = buf;
        }
    }
}

/**
 * Get the pipeline clock - may return NULL
 */
static GstClock * gst_es_assembler_get_pipeline_clock(GstObject *object)
{
    // If clock has not yet been initialized, initialize it
    GstObject* obj = object;
    GstObject* parent = object;        // Ensure initialized to non-NULL
    GstClock *pipeline_clock = NULL;

    /* NULL parent is indicator we are done, whether we have a clock or not */
    while (NULL != parent)
    {
        parent = GST_OBJECT_PARENT(obj);
        if (NULL == parent)
        {
            // Found pipeline, get clock
            GstPipeline *pipeline = GST_PIPELINE(obj);
            pipeline_clock = gst_pipeline_get_clock(pipeline);
        }
        else
        {
            // Move one step higher in the hierarchy
            obj = parent;
        } /* endif NULL parent */

    } /* endwhile clock not found*/

    return pipeline_clock;

} /* gst_es_assembler_get_pipeline_clock */
static GstClockTime gst_es_assembler_get_pipeline_time(GstESAssembler *filter)
{
    GstClockTime gstClockTime = GST_CLOCK_TIME_NONE;

    if (!filter->pipeline_clock)
    {
        filter->pipeline_clock = gst_es_assembler_get_pipeline_clock((GstObject *)filter);
    }
    if (filter->pipeline_clock)
    {
        gstClockTime = gst_clock_get_time(filter->pipeline_clock);
    }

    return gstClockTime;

} /* gst_es_assembler_get_pipeline_time */

static GstClockTime gst_es_assembler_get_running_time(GstESAssembler *filter)
{
    GstClockTime pipeline_time = GST_CLOCK_TIME_NONE;
    GstClockTime running_time = GST_CLOCK_TIME_NONE;
    GstClockTime base_time = GST_CLOCK_TIME_NONE;

    base_time     = gst_element_get_base_time((GstElement *)filter);
    pipeline_time = gst_es_assembler_get_pipeline_time(filter);

    if (GST_CLOCK_TIME_IS_VALID(pipeline_time) && GST_CLOCK_TIME_IS_VALID(base_time))
    {
        /* If base time has been set, calc running time, else r_t = 0 */
        if (base_time != 0LL)
        {
            running_time = (GstClockTime)GST_CLOCK_DIFF(base_time, pipeline_time);
        }
        else
        {
            running_time = GST_CLOCK_TIME_NONE;
        }
    }
    GST_DEBUG_OBJECT(filter, "Base time %" GST_TIME_FORMAT, GST_TIME_ARGS (base_time));
    GST_DEBUG_OBJECT(filter, "Pipeline time %" GST_TIME_FORMAT,GST_TIME_ARGS (pipeline_time));
    GST_DEBUG_OBJECT(filter, "Running time %" GST_TIME_FORMAT,GST_TIME_ARGS (running_time));

    return running_time;

} /* gst_es_assembler_get_running_time */

#if 0
/* entry point to initialize the plug-in
 * initialize the plug-in itselfhttp://start.ubuntu.com/8.04/
 * register the element factories and other features  */
static gboolean es_assembler_init (GstPlugin * esassembler)
{
    /* debug category for fltering log messages
     *
     * exchange the string 'Template esassembler' with your description
     */
/*
  GST_DEBUG_CATEGORY_INIT (gst_esassembler_debug, "esassembler",
      0, "Template esassembler");s/platform_full_decode/ri/RI_Platform/gen/Linux/debug/clplugins/gstdemux.o /home/steve/CableLabsRI/branches/platform_full_decode/ri/RI_Platform/gen/Linux/debug/clplugins/gstffmpeg.o -L/home/steve/CableLabsRI/branches/pl
*/
    return gst_element_register (esassembler, "esassembler", GST_RANK_NONE,
                                 GST_TYPE_ES_ASSEMBLER);
}

/* gstreamer looks for this structure to register esassemblers
 *
 * exchange the string 'Template esassembler' with your esassembler description  */
GST_PLUGIN_DEFINE (
                  GST_VERSION_MAJOR,
                  GST_VERSION_MINOR,
                  "esassembler",
                  "Elementary Stream Assembler",
                  es_assembler_init,
                  VERSION,
                  "LGPL",
                  "CableLabs GStreamer Plugins",
                  "Unknown Origin URL"
                  )
#endif
