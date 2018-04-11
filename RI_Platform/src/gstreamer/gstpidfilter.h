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

#ifndef __GST_PIDFILTER_H__
#define __GST_PIDFILTER_H__

#include <gst/gst.h>
#include "RemapIntf.h"

G_BEGIN_DECLS

/* #defines don't like whitespacey bits */
#define GST_TYPE_PIDFILTER \
  (gst_pid_filter_get_type())
#define GST_PIDFILTER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_PIDFILTER,GstPidFilter))
#define GST_PIDFILTER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_PIDFILTER,GstPidFilterClass))
#define GST_IS_PIDFILTER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_PIDFILTER))
#define GST_IS_PIDFILTER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_PIDFILTER))

/* Need only be 188 for now - array to hold leftovers */
#define MAX_LEFTOVERS (188)
/* Max number of packets we can queue up to copy to output */
#define MAX_PACKETS (128)
/* Common PMT PID */
#define PMT_PID (0x0E0)
/* Common video decode PID */
#define VIDEO_DECODE_PID (0x1E0)
/* Common video decode PID */
#define AUDIO_DECODE_PID (0x2E0)
/* Common PCR PID */
#define PCR_PID (0x3E0)

#ifndef min
#define min(a,b) ((a) > (b) ? (b) : (a))
#endif
//#define BE_SHORT_AT(a) (((guint16)((*(guint8 *)(a)) << 8)) + ( (*(guint8 *)((a)+1))))

#define MAX_PKT_INDEX 3
#define CURR_PKT (filter->curr_pkt_index)
#define PREV_PKT (filter->prev_pkt_index)
#define RESIDUAL (filter->residual_pkt_index)

typedef struct _GstPidFilter GstPidFilter;
typedef struct _GstPidFilterClass GstPidFilterClass;

struct _GstPidFilter
{
    /* GST Element Items */
    GstElement element;
    GstPad *sinkpad, *srcpad;
    GstClockTime qos_timestamp;
    GstClockTimeDiff qos_time_diff;
    gdouble qos_proportion;
    gboolean silent;
    GMutex *props_lock;

    /* Alignment Items */
    guint8 residual[MAX_PKT_INDEX][MAX_LEFTOVERS];
    gint leftover_count;

    /* Sync Detector Items */
    gboolean packet_sync_present; /* boolean - are we in sync or not?   */
    gboolean packet_sync_offset; /* packet sync offset   */
    gboolean b47[188]; /* array of trues for 47 seen here    */
    gint num_47s; /* count of possible 47 locations     */
    gint sd_running_offset; /* running offset modulo 188 kickoff  */
    guint bytes_since_start;

    /* Remap Module Items */
    gchar *stringremapinfo; /* String of the Prog. PMT info to map */
    gchar *stringpidlist; /* String of the pidlist to filter/map */
    RemapProg oldprognum;
    RemapProg newprognum;
    RemapPid oldpmtpid;
    RemapPid newpmtpid;
    RemapHandle remap_handle;

    /* Remap Call Items */
    guint curr_pkt_index;
    guint prev_pkt_index;
    guint residual_pkt_index;
    guint pkt_count[MAX_PKT_INDEX];
    RemapPacket *ppkts[MAX_PKT_INDEX][MAX_PACKETS];
    RemapPacket **pppkts[MAX_PKT_INDEX]; /* List of packets to copy */
    GstBuffer *savebuf[MAX_PKT_INDEX];
    GstClockTime savetimestamp[MAX_PKT_INDEX];
    guint8 *saveptr[MAX_PKT_INDEX];

};

struct _GstPidFilterClass
{
    GstElementClass parent_class;
};

GType gst_pid_filter_get_type(void);

G_END_DECLS

#endif /* __GST_PIDFILTER_H__ */
