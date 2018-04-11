/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 * Copyright (C) 2009 Cable Television Laboratories, Inc.
 *
 * gstbuffer.c: Buffer operations
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
 * FIXME:Describe gstesbuffer here.
 */

#include "gstesbuffer.h"

GST_DEBUG_CATEGORY_STATIC ( gst_es_buffer_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_es_buffer_debug

#define _do_init(thing) \
  GST_DEBUG_CATEGORY_INIT (gst_es_buffer_debug, "esbuffer", 0, "Elementary Stream Buffer")

/*lint -e(123)*/GST_BOILERPLATE_FULL (GstESBuffer, gst_es_buffer, GstBuffer, GST_TYPE_BUFFER, _do_init)

// Forward declarations
static GstESBuffer * gst_es_buffer_copy (GstESBuffer * esbuf);
static void gst_es_buffer_finalize(GstESBuffer * esbuf);

/**
 * gst_es_buffer_new:
 *
 * Creates a new #GstESBuffer. Free with gst_mini_object_unref().
 *
 * Returns: a new #GstESBuffer
 */
GstESBuffer *
gst_es_buffer_new(void)
{
    return GST_ES_BUFFER_CAST(gst_mini_object_new(GST_TYPE_ES_BUFFER));
}

//
//
//
// INTERNAL IMPLEMENTATION
//
//
//

/**************************************************/
/**********                              **********/
/********** GstMiniObject IMPLEMENTATION **********/
/**********                              **********/
/**************************************************/

static void gst_es_buffer_base_init(gpointer g_class)
{
    /* nop */
}

static void gst_es_buffer_class_init(GstESBufferClass * klass)
{
    GstMiniObjectClass *mini_object = GST_MINI_OBJECT_CLASS(klass);

    mini_object->copy = (GstMiniObjectCopyFunction) gst_es_buffer_copy;
    mini_object->finalize
            = (GstMiniObjectFinalizeFunction) gst_es_buffer_finalize;
}

static void gst_es_buffer_init(GstESBuffer * esbuf, GstESBufferClass * g_class)
{
    esbuf->dts = GST_CLOCK_TIME_NONE;
}

static void gst_es_buffer_finalize(GstESBuffer * esbuf)
{
    g_return_if_fail(esbuf != NULL);

    GST_MINI_OBJECT_CLASS(parent_class)->finalize(GST_MINI_OBJECT(esbuf));
}

// Deep copy, including all of GstBuffer data
static GstESBuffer *
gst_es_buffer_copy(GstESBuffer * esbuf)
{
    GstESBuffer *copy = NULL;

    g_return_val_if_fail(esbuf != NULL, NULL);

    copy = gst_es_buffer_new();

    // GstBuffer copy
    GST_BUFFER_DATA(copy) = g_memdup(GST_BUFFER_DATA(esbuf), GST_BUFFER_SIZE(
            esbuf));
    /* make sure it gets freed (even if the parent is subclassed, we return a
     normal buffer) */
    GST_BUFFER_MALLOCDATA(copy) = GST_BUFFER_DATA(copy);
    GST_BUFFER_SIZE(copy) = GST_BUFFER_SIZE(esbuf);
    gst_buffer_copy_metadata(GST_BUFFER_CAST(copy), GST_BUFFER_CAST(esbuf),
            GST_BUFFER_COPY_ALL);

    // GstESBuffer copy
    copy->dts = esbuf->dts;

    return copy;
}
