/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2009 mkorzen <<user@hostname.org>>
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
 * SECTION:element-mpeg_decoder
 *
 * FIXME:Describe mpeg_decoder here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! mpeg_decoder ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <gst/gst.h>

#include "gstmpegdecoder.h"
#include "gstffmpeg.h"

GST_DEBUG_CATEGORY_STATIC ( gst_mpeg_decoder_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_mpeg_decoder_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0, PROP_DIRECT_RENDERING, PROP_LOW_QUALITY_DECODE,
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE("sink",
        GST_PAD_SINK, GST_PAD_ALWAYS, GST_STATIC_CAPS("video/mpeg"));

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC, GST_PAD_ALWAYS, GST_STATIC_CAPS("video/x-raw-yuv, "
            "endianness = (int) [ 0, MAX ], " // required by gstdisplay
                "format = (fourcc) { I420 }, "
                "width = (int) [ 16, 4096 ], "
                "height = (int) [ 16, 4096 ], "
                "framerate = (fraction) [ 0/1, 2147483647/1 ], "
                "pixel-aspect-ratio = (fraction) [ 0/1, 2147483647/1 ] "));

/*lint -e(123)*/GST_BOILERPLATE (GstMpegDecoder, gst_mpeg_decoder, GstElement, GST_TYPE_ELEMENT)

// Forward declarations
static void gst_mpeg_decoder_dispose (GObject * object);
static void gst_mpeg_decoder_finalize(GObject * object);

static void gst_mpeg_decoder_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec);
static void gst_mpeg_decoder_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec);

static gboolean gst_mpeg_decoder_event(GstPad * pad, GstEvent * event);
static GstFlowReturn gst_mpeg_decoder_chain(GstPad * pad, GstBuffer * buf);
static GstFlowReturn gst_mpeg_decoder_pad_buffer_alloc(GstPad * pad,
        guint64 offset, guint size, GstCaps * caps, GstBuffer ** buf);

static GstFlowReturn gst_mpeg_decoder_decode_video(GstMpegDecoder *filter,
        GstBuffer *inbuf, GstBuffer **outbuf);
static GstFlowReturn gst_mpeg_decoder_alloc_buffer_and_set_caps(
        GstMpegDecoder *filter, GstBuffer **buf, AVPicture *picture,
        int *aligned_width, int *aligned_height);
static int get_mpeg_decoder_get_buffer(struct AVCodecContext *c, AVFrame *pic);
static void gst_mpeg_decoder_release_buffer(struct AVCodecContext *c,
        AVFrame *pic);

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

static void gst_mpeg_decoder_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "MpegDecoder",
            "FIXME:Generic", "FIXME:Generic Template Element",
            "mkorzen <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the mpeg_decoder's class */
static void gst_mpeg_decoder_class_init(GstMpegDecoderClass * klass)
{
    GObjectClass *gobject_class = NULL;

    GST_DEBUG_CATEGORY_INIT(gst_mpeg_decoder_debug, "mpegdecoder", 0,
            "Template mpegdecoder");

    gobject_class = (GObjectClass *) klass;

    gobject_class->dispose = gst_mpeg_decoder_dispose;
    gobject_class->finalize = gst_mpeg_decoder_finalize;

    gobject_class->set_property = gst_mpeg_decoder_set_property;
    gobject_class->get_property = gst_mpeg_decoder_get_property;

    g_object_class_install_property(gobject_class, PROP_DIRECT_RENDERING,
            g_param_spec_boolean("direct-rendering", "Direct Rendering",
                    "Allocate picture buffers from upstream ?", TRUE,
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_LOW_QUALITY_DECODE,
            g_param_spec_boolean("low-quality-decode", "Low Quality Decode",
                    "Lower decode quality to minimize CPU usage ?", FALSE,
                    G_PARAM_READWRITE));

    GST_INFO("Using FFMPEG's libavcodec version %u.%u.%u",
            LIBAVCODEC_VERSION_MAJOR, LIBAVCODEC_VERSION_MINOR,
            LIBAVCODEC_VERSION_MICRO);

    klass->codec = avcodec_find_decoder(CODEC_ID_MPEG2VIDEO);
    if (klass->codec == NULL)
    {
        GST_ERROR("Unable to find FFMPEG's MPEG 1/2 video decoder!");
        g_assert( FALSE);
    }
    else
    {
        GST_DEBUG("Found FFMPEG's MPEG 1/2 video decoder");
    }

    GST_DEBUG("FFMPEG MPEG 1/2 decoder caps");
    GST_DEBUG("----------------------------");
    GST_DEBUG("Field support:             %c", (klass->codec->capabilities
            & CODEC_CAP_DRAW_HORIZ_BAND) ? 'Y' : 'N');
    GST_DEBUG("Direct rendering:          %c", (klass->codec->capabilities
            & CODEC_CAP_DR1) ? 'Y' : 'N');
    GST_DEBUG("Supports parse only:       %c", (klass->codec->capabilities
            & CODEC_CAP_PARSE_ONLY) ? 'Y' : 'N');
    GST_DEBUG("Truncated:                 %c", (klass->codec->capabilities
            & CODEC_CAP_TRUNCATED) ? 'Y' : 'N');
    GST_DEBUG("Supports HW accel (XvMC):  %c", (klass->codec->capabilities
            & CODEC_CAP_HWACCEL) ? 'Y' : 'N');
    GST_DEBUG("Has non-zero decode delay: %c", (klass->codec->capabilities
            & CODEC_CAP_DELAY) ? 'Y' : 'N');
    GST_DEBUG("Supports small last frame: %c", (klass->codec->capabilities
            & CODEC_CAP_SMALL_LAST_FRAME) ? 'Y' : 'N');
    GST_DEBUG("Supports HW accel (VDPAU): %c", (klass->codec->capabilities
            & CODEC_CAP_HWACCEL_VDPAU) ? 'Y' : 'N');
    GST_DEBUG("----------------------------");
    GST_DEBUG("Supported frame rates:   %s",
            (klass->codec->supported_framerates == NULL) ? "N/A" : "");
    if (klass->codec->supported_framerates)
    {
        int index = 0;
        while (klass->codec->supported_framerates[index].num != 0
                && klass->codec->supported_framerates[index].den != 0)
        {
            GST_DEBUG("  %d/%d", klass->codec->supported_framerates[index].num,
                    klass->codec->supported_framerates[index].den);
            index++;
        }
    }
    GST_DEBUG("Supported pixel formats: %s",
            (klass->codec->pix_fmts == NULL) ? "N/A" : "");
    if (klass->codec->pix_fmts)
    {
        int index = 0;
        while (klass->codec->pix_fmts[index] != -1)
        {
            GST_DEBUG("  %s", avcodec_get_pix_fmt_name(
                    klass->codec->pix_fmts[index]));
            index++;
        }
    }
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_mpeg_decoder_init(GstMpegDecoder * filter,
        GstMpegDecoderClass * gclass)
{
    // sink (input) pad
    filter->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    gst_pad_set_event_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_mpeg_decoder_event));
    gst_pad_set_chain_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_mpeg_decoder_chain));
    gst_pad_use_fixed_caps(filter->sinkpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->sinkpad);
    gst_pad_set_bufferalloc_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_mpeg_decoder_pad_buffer_alloc));

    // source (output) pad
    filter->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    // TODO gst_pad_set_getcaps_function (filter->srcpad, GST_DEBUG_FUNCPTR(gst_pad_proxy_getcaps));
    gst_pad_set_event_function(filter->srcpad, GST_DEBUG_FUNCPTR(
            gst_mpeg_decoder_event));
    gst_pad_use_fixed_caps(filter->srcpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->srcpad);

    // MPEG decoder specific initialization
    filter->direct_rendering = DEFAULT_DIRECT_RENDERING;
    filter->low_quality_decode = DEFAULT_LOW_QUALITY_DECODE;

    // FFMPEG specific initialization

    // Turn off av logging unless the mpeg decoder level is at debug level or greater
    if (gst_debug_category_get_threshold(gst_mpeg_decoder_debug)
            <= GST_LEVEL_INFO)
    {
        av_log_set_level( AV_LOG_QUIET);
    }
    else
    {
        GST_DEBUG_OBJECT(filter, "Leaving FFMPEG av logging at default level");
    }

    filter->context = avcodec_alloc_context();
    if (avcodec_open(filter->context, gclass->codec) < 0)
    {
        GST_ERROR_OBJECT(filter,
                "Unable to initialize FFMPEG's MPEG 1/2 video decoder context!");
        g_assert( FALSE);
    }
    else
    {
        GST_DEBUG_OBJECT(filter,
                "Initialized FFMPEG's MPEG 1/2 video decoder context");
    }
    filter->context->opaque = (void *) filter;
    if (gclass->codec->capabilities & CODEC_CAP_DR1)
    {
        if (filter->direct_rendering == TRUE)
        {
            filter->context->get_buffer = get_mpeg_decoder_get_buffer;
            filter->context->release_buffer = gst_mpeg_decoder_release_buffer;
            GST_INFO_OBJECT(filter, "Direct rendering enabled");
        }
        else
        {
            GST_INFO_OBJECT(filter, "Direct rendering disabled");
        }
    }
    else
    {
        GST_ERROR_OBJECT(filter,
                "Direct rendering disabled - no codec support!?");
    }

    filter->picture = avcodec_alloc_frame();
}

static void gst_mpeg_decoder_dispose(GObject * object)
{
    GstMpegDecoder *filter = GST_MPEG_DECODER(object);

    // From http://library.gnome.org/devel/gobject/2.16/gobject-memory.html
    //
    // When dispose ends, the object should not hold any reference to any other
    // member object. The object is also expected to be able to answer client
    // method invocations (with possibly an error code but no memory violation)
    // until finalize is executed. dispose can be executed more than once.
    // dispose should chain up to its parent implementation just before returning
    // to the caller.
    (void) filter;

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, dispose, (object));
}

static void gst_mpeg_decoder_finalize(GObject * object)
{
    GstMpegDecoder *filter = GST_MPEG_DECODER(object);

    av_free(filter->context);
    av_free(filter->picture);

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, finalize, (object));
}

static void gst_mpeg_decoder_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec)
{
    GstMpegDecoder *filter = GST_MPEG_DECODER(object);

    switch (prop_id)
    {
    case PROP_DIRECT_RENDERING:
        filter->direct_rendering = g_value_get_boolean(value);
        break;
    case PROP_LOW_QUALITY_DECODE:
        filter->low_quality_decode = g_value_get_boolean(value);
        if (filter->low_quality_decode)
        {
            GST_WARNING_OBJECT(filter, "Property set to low quality decode");
            // Set the decoder to discard all bidirectional frames
            filter->context->skip_frame = AVDISCARD_BIDIR;
            filter->context->skip_idct = AVDISCARD_BIDIR;
            filter->context->skip_loop_filter = AVDISCARD_BIDIR;
        }
        else
        {
            GST_WARNING_OBJECT(filter, "Property set to normal quality decode");
            filter->context->skip_frame = AVDISCARD_DEFAULT;
            filter->context->skip_idct = AVDISCARD_DEFAULT;
            filter->context->skip_loop_filter = AVDISCARD_DEFAULT;
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_mpeg_decoder_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec)
{
    GstMpegDecoder *filter = GST_MPEG_DECODER(object);

    switch (prop_id)
    {
    case PROP_DIRECT_RENDERING:
        g_value_set_boolean(value, filter->direct_rendering);
        break;
    case PROP_LOW_QUALITY_DECODE:
        g_value_set_boolean(value, filter->low_quality_decode);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/***********************************************/
/**********                           **********/
/********** GstElement IMPLEMENTATION **********/
/**********                           **********/
/***********************************************/

static gboolean gst_mpeg_decoder_event(GstPad * pad, GstEvent * event)
{
    gboolean ret = FALSE;
    GstMpegDecoder *filter = GST_MPEG_DECODER(GST_OBJECT_PARENT(pad));

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_NEWSEGMENT:
        GST_INFO_OBJECT(filter, "Received NEWSEGMENT event.");
        ret = gst_pad_push_event(filter->srcpad, event);
        break;
    case GST_EVENT_EOS:
        GST_INFO_OBJECT(filter, "Received EOS event.");
        avcodec_flush_buffers(filter->context);
        ret = gst_pad_push_event(filter->srcpad, event);
        break;
    case GST_EVENT_FLUSH_START:
        GST_INFO_OBJECT(filter, "Received FLUSH_START event.");
        ret = gst_pad_push_event(filter->srcpad, event);
        break;
    case GST_EVENT_FLUSH_STOP:
        GST_INFO_OBJECT(filter, "Received FLUSH_STOP event.");
        ret = gst_pad_push_event(filter->srcpad, event);
        break;
    default:
        GST_DEBUG_OBJECT(filter, "Received other (%s) event.",
                GST_EVENT_TYPE_NAME(event));
        if (pad == filter->sinkpad)
        {
            ret = gst_pad_push_event(filter->srcpad, event);
        }
        else if (pad == filter->srcpad)
        {
            ret = gst_pad_push_event(filter->sinkpad, event);
        }
        else
        {
            GST_ERROR_OBJECT(filter, "Received event from unknown pad!?");
        }
        break;
    }
    return ret;
}

/*
 * chain function
 * this function does the actual processing
 *
 *       IN:
 * ---------------
 * An MPEG elementary stream, chunked at the PES packet boundries -
 * i.e. one PES packet = one GST buffer. Buffer timestamp carries
 * the PTS as extracted/interpolated from PES headers.
 *
 *       OUT:
 * ----------------
 * Decoded pictures in YUV format. One picture per GST buffer.
 * Buffer timestamp carries the picture presentation time.
 *
 */
static GstFlowReturn gst_mpeg_decoder_chain(GstPad * pad, GstBuffer * buf)
{
    GstMpegDecoder *filter = GST_MPEG_DECODER(GST_OBJECT_PARENT(pad));
    GstFlowReturn ret = GST_FLOW_OK;
    GstBuffer *outbuf = NULL;

    if (G_UNLIKELY(GST_BUFFER_IS_DISCONT(buf) == TRUE))
    {
        GST_DEBUG_OBJECT(filter,
                "DISCONT flag set on buffer %p - flushing buffers.", buf);
        ret = gst_mpeg_decoder_decode_video(filter, NULL, &outbuf);
        if (G_LIKELY(GST_FLOW_IS_SUCCESS(ret) == TRUE) && G_LIKELY(outbuf
                != NULL))
        {
            ret = gst_pad_push(filter->srcpad, outbuf);
            outbuf = NULL;
        }
    }

    if (G_LIKELY(GST_FLOW_IS_SUCCESS(ret)) == TRUE)
    {
        ret = gst_mpeg_decoder_decode_video(filter, buf, &outbuf);
        if (G_LIKELY(GST_FLOW_IS_SUCCESS(ret) == TRUE) && G_LIKELY(outbuf
                != NULL))
        {
            ret = gst_pad_push(filter->srcpad, outbuf);
        }
    }

    gst_buffer_unref(buf);

    return ret;
}

/***************************************************/
/**********                               **********/
/********** GstMpegDecoder IMPLEMENTATION **********/
/**********                               **********/
/***************************************************/

/*
 static void
 gst_mpeg_decoder_dump_context(GstMpegDecoder *filter)
 {
 GST_DEBUG_OBJECT(filter, "AVCodecContext %p", filter->context);
 GST_DEBUG_OBJECT(filter, "bit_rate, time_base:           %d,%d/%d", filter->context->bit_rate, filter->context->time_base.num, filter->context->time_base.den);
 GST_DEBUG_OBJECT(filter, "width x height / pixel format: %dx%d/%s", filter->context->width, filter->context->height, avcodec_get_pix_fmt_name(filter->context->pix_fmt));
 GST_DEBUG_OBJECT(filter, "frame rate emulation:          %d", filter->context->rate_emu);
 GST_DEBUG_OBJECT(filter, "frame #, read picture #:       %d,%d", filter->context->frame_number, filter->context->real_pict_num);
 GST_DEBUG_OBJECT(filter, "frame delay, has B frames:     %d", filter->context->has_b_frames);
 GST_DEBUG_OBJECT(filter, "sample_aspect_ratio:           %d/%d", filter->context->sample_aspect_ratio.num, filter->context->sample_aspect_ratio.den);
 GST_DEBUG_OBJECT(filter, "coded frame:                   %p", filter->context->coded_frame);
 GST_DEBUG_OBJECT(filter, "AFD:                           %d", filter->context->dtg_active_format);
 GST_DEBUG_OBJECT(filter, "flags2:                        %d", filter->context->flags2);
 GST_DEBUG_OBJECT(filter, "thread count:                  %d", filter->context->thread_count);
 GST_DEBUG_OBJECT(filter, "coded_width, coded_height:     %dx%d", filter->context->coded_width, filter->context->coded_height);
 GST_DEBUG_OBJECT(filter, "# of reference frames:         %d", filter->context->refs);
 GST_DEBUG_OBJECT(filter, "reordered_opaque:              %lld", filter->context->reordered_opaque);

 }
 */

static GstFlowReturn gst_mpeg_decoder_decode_video(GstMpegDecoder *filter,
        GstBuffer *inbuf, GstBuffer **outbuf)
{
    GstFlowReturn ret = GST_FLOW_OK;
    unsigned int used_bytes = 0;
    int got_picture = 0;

    if (inbuf == NULL)
    {
        used_bytes = avcodec_decode_video(filter->context, filter->picture,
                &got_picture, NULL, 0);
        avcodec_flush_buffers(filter->context);
    }
    else
    {
        filter->context->reordered_opaque = GST_BUFFER_TIMESTAMP(inbuf);
        used_bytes = avcodec_decode_video(filter->context, filter->picture,
                &got_picture, GST_BUFFER_DATA(inbuf), GST_BUFFER_SIZE(inbuf));
        if (used_bytes < GST_BUFFER_SIZE(inbuf))
        {
            GST_WARNING_OBJECT(filter,
                    "Decoding used only %d bytes (out of %d total injected)",
                    used_bytes, GST_BUFFER_SIZE(inbuf));
        }
    }

    if (got_picture)
    {
        GST_LOG_OBJECT(
                filter,
                "Injected PES packet of size %5u, used %5d bytes, got back %c-frame",
                (inbuf == NULL) ? (0) : (GST_BUFFER_SIZE(inbuf)), used_bytes,
                av_get_pict_type_char(filter->picture->pict_type));
        if (filter->direct_rendering == TRUE)
        {
            *outbuf = filter->picture->opaque;
            (void) gst_buffer_ref(*outbuf);
        }
        else
        {
            int width = 0;
            int height = 0;
            AVPicture picture =
            {
            { NULL, NULL, NULL, NULL },
            { 0, 0, 0, 0 } };

            ret = gst_mpeg_decoder_alloc_buffer_and_set_caps(filter, outbuf,
                    &picture, &width, &height);
            av_picture_copy(&picture, (AVPicture *) filter->picture,
                    filter->context->pix_fmt, width, height);
            GST_BUFFER_TIMESTAMP(*outbuf) = filter->picture->reordered_opaque;
        }
    }

    return ret;
}

/**
 * Allocate a GstBuffer for FFMPEG to render/copy decoded frames into it.
 * This function is used in a dual fashion:
 * - when direct_rendering is ENABLED, it creates a buffer for FFMPEG to
 *   directly decode into it. It is called from the FFMPEG callback function
 *   gst_mpeg_decoder_get_buffer().
 * - when direct_rendering is DISABLED, it creates a buffer. That buffer is
 *   then used to copy an already decoded picture into it (from an internal
 *   FFMPEG buffer).
 */
static GstFlowReturn gst_mpeg_decoder_alloc_buffer_and_set_caps(
        GstMpegDecoder *filter, GstBuffer **buf, AVPicture *pic,
        int *aligned_width, int *aligned_height)
{
    GstFlowReturn ret = GST_FLOW_OK;
    GstCaps *cur_caps = NULL;
    GstCaps *buf_caps = NULL;

    struct AVCodecContext *c = filter->context;
    int width = c->width;
    int height = c->height;

    // Some sanity checks
    if (c->width != c->coded_width || c->height != c->coded_height)
    {
        GST_WARNING_OBJECT(
                filter,
                "Requested picture dimensions %dx%d != encoded picture dimensions %dx%d",
                c->width, c->height, c->coded_width, c->coded_height);
    }

    // required by FFMPEG
    avcodec_align_dimensions(c, &width, &height);
    if (width != c->width || height != c->height)
    {
        GST_DEBUG_OBJECT(filter,
                "Aligned requested buffer dimensions %dx%d -> %dx%d", c->width,
                c->height, width, height);
    }
    if (aligned_width != NULL)
    {
        *aligned_width = width;
    }
    if (aligned_height != NULL)
    {
        *aligned_height = height;
    }

    cur_caps = gst_pad_get_negotiated_caps(filter->srcpad);
    buf_caps = gst_ffmpeg_create_fixed_video_caps(c->pix_fmt, width, height,
            c->time_base, c->sample_aspect_ratio);

    // Downstream caps (re)negotiation, if necessary
    if (G_UNLIKELY(cur_caps == NULL || gst_caps_is_equal_fixed(cur_caps,
            buf_caps) == FALSE))
    {
        gchar* cur_caps_str = gst_caps_to_string(cur_caps);
        gchar* buf_caps_str = gst_caps_to_string(buf_caps);
        GST_DEBUG_OBJECT(filter,
                "Forcing caps re-negotiation from \"%s\" to \"%s\"",
                cur_caps_str, buf_caps_str);
        if (G_UNLIKELY(gst_pad_set_caps(filter->srcpad, buf_caps) == FALSE))
        {
            GST_ERROR_OBJECT(filter, "Could not negotiate new format (%s)!?",
                    buf_caps_str);
            ret = GST_FLOW_ERROR;
        }
        g_free(cur_caps_str);
        g_free(buf_caps_str);
    }

    // Allocate buffer, if caps (re)negotiation succeeded
    if (G_LIKELY(GST_FLOW_IS_SUCCESS(ret)))
    {
        int size = avpicture_get_size(c->pix_fmt, width, height);
        if (filter->direct_rendering == TRUE)
        {
            ret = gst_pad_alloc_buffer_and_set_caps(filter->srcpad,
                    GST_BUFFER_OFFSET_NONE, size, buf_caps, buf);
        }
        else
        {
            *buf = gst_buffer_try_new_and_alloc(size);
            if (G_LIKELY(*buf != NULL))
            {
                gst_buffer_set_caps(*buf, buf_caps);
            }
            else
            {
                ret = GST_FLOW_ERROR;
            }
        }
    }

    if (G_LIKELY(GST_FLOW_IS_SUCCESS(ret)))
    {
        memset(GST_BUFFER_DATA(*buf), 0, GST_BUFFER_SIZE(*buf));
        if (pic != NULL)
        {
            (void) avpicture_fill(pic, GST_BUFFER_DATA(*buf), c->pix_fmt,
                    width, height);
        }
    }

    if (G_LIKELY(cur_caps != NULL))
    {
        gst_caps_unref(cur_caps);
    }
    gst_caps_unref(buf_caps);

    return ret;
}

static int get_mpeg_decoder_get_buffer(struct AVCodecContext *c, AVFrame *pic)
{
    int ret = 0;
    GstMpegDecoder *filter = GST_MPEG_DECODER(c->opaque);
    GstBuffer *buf = NULL;
    GstFlowReturn alloc_ret = GST_FLOW_OK;

    alloc_ret = gst_mpeg_decoder_alloc_buffer_and_set_caps(filter, &buf,
            (AVPicture *) pic, NULL, NULL);
    if (G_UNLIKELY(GST_FLOW_IS_SUCCESS(alloc_ret) == FALSE))
    {
        GST_ERROR_OBJECT(filter,
                "Buffer allocation from peer's pad failed with (%s)!?",
                gst_flow_get_name(alloc_ret));
        ret = -1;
    }
    else
    {
        pic->age = 256 * 256 * 256 * 64; // copied from avcodec/utils.c
        pic->type = FF_BUFFER_TYPE_USER;
        pic->opaque = buf;
        GST_BUFFER_TIMESTAMP(buf) = c->reordered_opaque;
        GST_LOG_OBJECT(filter, "Allocated buffer %p, size %u", pic->opaque,
                GST_BUFFER_SIZE(pic->opaque));
    }

    return ret;
}

static void gst_mpeg_decoder_release_buffer(struct AVCodecContext *c,
        AVFrame *pic)
{
    int i = 0;
    // Make sure we are freeing buffers allocated by GStreamer
    g_assert(pic->type == FF_BUFFER_TYPE_USER);
    g_assert(pic->opaque != NULL);
    for (i = 0; i < 4; i++)
    {
        pic->data[i] = NULL;
        pic->linesize[i] = 0;
    }
    gst_buffer_unref(pic->opaque);
    pic->opaque = NULL;
}

/**
 * This function allocates a buffer requested via the sink/input pad.
 * It return a buffer that's starting addr is byte alligned and the returned
 * buffer size is incremented by FF_INPUT_BUFFER_PADDING_SIZE constant.
 *
 * The buffer will become the input buffer for the ffmpeg decode function so it
 * needs to comply with the recommendation made in the avcodec.h method description
 * of avcodec_decode_video().  These recommendations are as follows:
 *
 * @warning The input buffer must be FF_INPUT_BUFFER_PADDING_SIZE larger than
 * the actual read bytes because some optimized bitstream readers read 32 or 64
 * bits at once and could read over the end.
 *
 * @warning The end of the input buffer buf should be set to 0 to ensure that
 * no overreading happens for damaged MPEG streams.
 */
static GstFlowReturn gst_mpeg_decoder_pad_buffer_alloc(GstPad * pad,
        guint64 offset, guint size, GstCaps * caps, GstBuffer ** buf)
{
    GstMpegDecoder *filter = GST_MPEG_DECODER(GST_OBJECT_PARENT(pad));
    GstFlowReturn ret = GST_FLOW_OK;
    GstCaps *cur_caps = NULL;
    int buf_size = 0;

    // Get the current caps on the input/sink pad
    cur_caps = gst_pad_get_negotiated_caps(filter->sinkpad);

    // Upstream caps (re)negotiation, if necessary
    if (G_UNLIKELY(cur_caps == NULL || gst_caps_is_equal_fixed(cur_caps, caps)
            == FALSE))
    {
        gchar* cur_caps_str = gst_caps_to_string(cur_caps);
        gchar* caps_str = gst_caps_to_string(caps);
        GST_DEBUG_OBJECT(filter,
                "Forcing caps re-negotiation from \"%s\" to \"%s\"",
                cur_caps_str, caps_str);
        if (G_UNLIKELY(gst_pad_set_caps(filter->sinkpad, caps) == FALSE))
        {
            GST_ERROR_OBJECT(filter, "Could not negotiate new format (%s)!?",
                    caps_str);
            ret = -1;
        }
        g_free(cur_caps_str);
        g_free(caps_str);
    }

    // Allocate buffer, if caps (re)negotiation succeeded
    if (G_LIKELY(ret == 0))
    {
        // Determine buffer size using requested size, adding recommended buffer padding
        buf_size = size + FF_INPUT_BUFFER_PADDING_SIZE;

        // Call utility function to allocate buffer
        *buf = gst_buffer_try_new_and_alloc(buf_size);
        if (NULL == *buf)
        {
            GST_ERROR_OBJECT(filter, "Unable to allocate buffer of size %d",
                    buf_size);
            ret = -1;
        }
        else
        {
            // Memset the buffer with 0 to make sure no over-reading occurs
            memset(GST_BUFFER_DATA(*buf), 0, GST_BUFFER_SIZE(*buf));

            gst_buffer_set_caps(*buf, cur_caps);
        }
    }

    // Free reference to local caps
    if (G_LIKELY(cur_caps != NULL))
    {
        gst_caps_unref(cur_caps);
    }

    return ret;
}
