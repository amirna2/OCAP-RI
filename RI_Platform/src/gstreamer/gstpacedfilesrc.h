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

#ifndef __GST_PACEDFILESRC_H__
#define __GST_PACEDFILESRC_H__

#include <stdio.h>

#include <gst/gst.h>
#include <gst/base/gstpushsrc.h>
/*lint -e(451)*/
#include <gst/base/gstadapter.h>

#include <ri_tuner.h>

#include "gstmpeg.h"

G_BEGIN_DECLS

/* #defines don't like whitespacey bits */
#define GST_TYPE_PACEDFILESRC \
  (gst_paced_file_src_get_type())
#define GST_PACEDFILESRC(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_PACEDFILESRC,GstPacedFileSrc))
#define GST_PACEDFILESRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_PACEDFILESRC,GstPacedFileSrcClass))
#define GST_IS_PACEDFILESRC(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_PACEDFILESRC))
#define GST_IS_PACEDFILESRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_PACEDFILESRC))

typedef struct _GstPacedFileSrc GstPacedFileSrc;
typedef struct _GstPacedFileSrcClass GstPacedFileSrcClass;

#define DEFAULT_FILEPATH    NULL
#define DEFAULT_FILENAME    NULL
#define DEFAULT_BLKSIZE     (TS_PACKET_SIZE * 2)

#define PIDFORMAT_SIZE      10                  // 10 chars per PID "0xNNNN=PN "
#define PIDLIST_SIZE        ((PIDFORMAT_SIZE * MAX_PIDS) + 1)
#define MIN_BLKSIZE         TS_PACKET_SIZE
#define MAX_BLKSIZE         (TS_PACKET_SIZE * 20)
#define BLKSIZE_CLOCKS      ((src->blksize / TS_PACKET_SIZE) * 40)

#define BIT2BYTE(b)         ((b) >> 3)          // ((b) / 8)
#define BIT2MASK(b)         (1 << ((b) & 7))    // (1 << ((b) % 8))
#define TESTPIDBIT(b)       (src->PIDbitfield[BIT2BYTE(b)] & BIT2MASK(b))
#define SETPIDBIT(b)        (src->PIDbitfield[BIT2BYTE(b)] |= BIT2MASK(b))
#define CLRPIDBIT(b)        (src->PIDbitfield[BIT2BYTE(b)] &= ~BIT2MASK(b))


struct _GstPacedFileSrc
{
    GstPushSrc parent;

    /* Protects read/write to:
     * - path
     * - name
     * - blocksize
     */
    GMutex *props_lock;

    /* Properties - set/get via GObject interface */
    gchar *uri;
    gchar *location;
    gchar *pidlist;
    guint blksize;
    guint16 loop;
    gboolean pcr_pacing;
    gboolean tuner_pid_filtering;
    gboolean rewrite_pcr_and_cc;
    gchar pmtpidlist[PIDLIST_SIZE];

    /* private vars */
    FILE* fp;
    guint8* filebuf;
    guint8* filebufp;
    gint filebufsz;
    guint16 pacing_pid;
    guint16 curr_tsid;
    guint64 begPCR;
    guint64 endPCR;
    guint64 curPCR;
    guint64 duration;
    guint64 bytes_sent;
    GstClock* sysclock;
    GstClockTime current_time;
    GstClockTime last_clock_time;
    GstClockTimeDiff clock_diff;
    GstBuffer* null_ts_packet;
    guint8 PIDbitfield[(TS_MAX_PIDS+1)/8];     // 1 bit per PID
};

struct _GstPacedFileSrcClass
{
    GstPushSrcClass parent_class;
};

GType gst_paced_file_src_get_type(void);

G_END_DECLS

#endif /* __GST_PACEDFILESRC_H__ */
