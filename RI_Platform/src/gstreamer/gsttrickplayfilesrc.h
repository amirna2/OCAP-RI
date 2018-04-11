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

#ifndef __GST_TRICKPLAYFILESRC_H__
#define __GST_TRICKPLAYFILESRC_H__

#include <gst/gst.h>
#include <gst/base/gstpushsrc.h>
/*lint -e(451)*/
#include <gst/base/gstadapter.h>

#include <../ifs/IfsIntf.h>

#include "gstmpeg.h"

G_BEGIN_DECLS

/* #defines don't like whitespacey bits */
#define GST_TYPE_TRICKPLAYFILESRC \
  (gst_trick_play_file_src_get_type())
#define GST_TRICKPLAYFILESRC(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_TRICKPLAYFILESRC,GstTrickPlayFileSrc))
#define GST_TRICKPLAYFILESRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_TRICKPLAYFILESRC,GstTrickPlayFileSrcClass))
#define GST_IS_TRICKPLAYFILESRC(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_TRICKPLAYFILESRC))
#define GST_IS_TRICKPLAYFILESRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_TRICKPLAYFILESRC))

typedef struct _GstTrickPlayFileSrc GstTrickPlayFileSrc;
typedef struct _GstTrickPlayFileSrcClass GstTrickPlayFileSrcClass;

#define DEFAULT_FILEPATH       NULL
#define DEFAULT_FILENAME       NULL
#define DEFAULT_FRAMERATE      30
#define DEFAULT_PLAYRATE       1.0
#define DEFAULT_POSITION_TIME  0
#define DEFAULT_POSITION_BYTES 0
#define DEFAULT_BLOCKSIZE      (TS_PACKET_SIZE * 24)
#define DEFAULT_EOS_SLEEP_NSECS 1000000L

#define MAX_FRAMERATE          30
#define MIN_FRAMERATE          1

#define MAX_PLAYRATE           64.0
#define MIN_PLAYRATE           (0.0 - MAX_PLAYRATE)

#define PLAYRATE_PRECISION     100

#define INVALID_POSITION_TIME  G_MAXUINT64
#define INVALID_POSITION_BYTES G_MAXUINT64

#define MIN_BLOCKSIZE          TS_PACKET_SIZE
#define MAX_BLOCKSIZE          (TS_PACKET_SIZE * 64)

struct _GstTrickPlayFileSrc
{
    GstPushSrc parent;

    /* Protects read/write to:
     * - path
     * - name
     * - framerate
     * - playrate
     * - position_time
     * - position_bytes
     * - blocksize
     */
    GMutex *props_lock;

    /* Properties - set/get via GObject interface */
    gchar *filepath;
    gchar *filename;

    gboolean filechanged; // not a property but allows to distinguish
    // when file or name are changed
    guint framerate;
    gfloat playrate;
    gboolean playrate_changed; // not a property but used to signal rate change
    GstClockTime position_time; // set only, get via current_time
    guint64 position_bytes; // set only, get via current_bytes
    guint blocksize;
    guint64 eos_sleep_nsecs;
    gboolean test_thread;
    gboolean timestamp_with_position;

    /*< private GStreamer >*/
    GstClockTime current_time;
    guint64 current_bytes;
    GstAdapter *blocksize_adapter;
    GstClockTime outgoing_timestamp;
    GstClockTime outgoing_duration;
    gboolean hadEOS;
    GstBuffer *null_ts_packet;

    /*< private IFS >*/
    IfsHandle ifs_handle;
    IfsClock ifs_offset;
    IfsClock end_offset;
};

struct _GstTrickPlayFileSrcClass
{
    GstPushSrcClass parent_class;
};

GType gst_trick_play_file_src_get_type(void);

void gst_trick_play_file_src_reset(GstTrickPlayFileSrc *src);

G_END_DECLS

#endif /* __GST_TRICKPLAYFILESRC_H__ */
