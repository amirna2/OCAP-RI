/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 * Copyright (C) 2009 Cable Television Laboratories, Inc. 
 *
 * gstbuffer.h: Header for GstBuffer object
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

#ifndef __GST_ES_BUFFER_H__
#define __GST_ES_BUFFER_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define GST_TYPE_ES_BUFFER            (gst_es_buffer_get_type())
#define GST_IS_ES_BUFFER(obj)         (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_ES_BUFFER))
#define GST_IS_ES_BUFFER_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_ES_BUFFER))
#define GST_ES_BUFFER_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), GST_TYPE_ES_BUFFER, GstESBufferClass))
#define GST_ES_BUFFER(obj)            (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_ES_BUFFER, GstESBuffer))
#define GST_ES_BUFFER_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_ES_BUFFER, GstESBufferClass))
#define GST_ES_BUFFER_CAST(obj)       ((GstESBuffer *)(obj))

typedef struct _GstESBuffer GstESBuffer;
typedef struct _GstESBufferClass GstESBufferClass;

struct _GstESBuffer
{
    GstBuffer parent;

    /*< public >*//* with COW */

    /* MPEG-2 PES PTS is set in GST_BUFFER_TIMESTAMP */
    GstClockTime dts;

    /*< private >*/
    gpointer _gst_reserved[GST_PADDING];
};

struct _GstESBufferClass
{
    GstBufferClass parent_class;
};

// free with gst_mini_object_unref
GstESBuffer * gst_es_buffer_new(void);
GType gst_es_buffer_get_type(void);

G_END_DECLS

#endif /* __GST_ES_BUFFER__ */
