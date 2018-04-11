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

#ifndef __GST_SPTSFILESRC_H__
#define __GST_SPTSFILESRC_H__

#include <stdio.h>

#include <gst/gst.h>
#include <gst/base/gstpushsrc.h>
/*lint -e(451)*/
#include <gst/base/gstadapter.h>

#include "gstmpeg.h"

G_BEGIN_DECLS

/* #defines don't like whitespacey bits */
#define GST_TYPE_SPTSFILESRC \
  (gst_spts_file_src_get_type())
#define GST_SPTSFILESRC(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_SPTSFILESRC,GstSptsFileSrc))
#define GST_SPTSFILESRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_SPTSFILESRC,GstSptsFileSrcClass))
#define GST_IS_SPTSFILESRC(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_SPTSFILESRC))
#define GST_IS_SPTSFILESRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_SPTSFILESRC))

typedef struct _GstSptsFileSrc GstSptsFileSrc;
typedef struct _GstSptsFileSrcClass GstSptsFileSrcClass;

#define DEFAULT_FILEPATH    NULL
#define DEFAULT_FILENAME    NULL
#define DEFAULT_BLCKSIZE    (TS_PACKET_SIZE * 1)
#define DEFAULT_PROGRAM     1

#define MIN_BLKSIZE         TS_PACKET_SIZE
#define MAX_BLKSIZE         (TS_PACKET_SIZE * 20)

#define PAT_PID             0
#define PMT_TID             2
#define NULL_PID            0x1FFF
#define MAX_AUDIO_PIDS      16
#define TS_HDR_SZ           4
#define PAT_HDR_SZ          3

struct _GstSptsFileSrc
{
    GstPushSrc parent;

    /* Protects read/write to:
     * - path
     * - name
     * - blocksize
     * - program
     * - loop
     */
    GMutex *props_lock;

    /* Properties - set/get via GObject interface */
    gchar *uri;
    gchar *location;
    guint blksize;
    guint16 program;
    guint16 loop;

    /* private vars */
    FILE* fp;
    guint8 filebuf[MAX_BLKSIZE];
    guint8* filebufp;
    gint filebufsz;
    guint16 pmt_pid;
    guint16 video_pid;
    guint16 audio_pids[MAX_AUDIO_PIDS];
    guint8 audio_channel_count;
    guint16 pacing_pid;
    guint32 overflow_count;
    GstClock* sysclock;
    GstClockTime current_time;
    GstBuffer* null_ts_packet;
    guint16 hSize;
    guint16 vSize;
    guint8 aspectRatioIndex;
    guint8 frameRateIndex;
    guint64 begPCR;
    guint64 endPCR;
    guint64 curPCR;
    guint64 duration;
};

struct _GstSptsFileSrcClass
{
    GstPushSrcClass parent_class;
};

GType gst_spts_file_src_get_type(void);

G_END_DECLS

#endif /* __GST_SPTSFILESRC_H__ */
