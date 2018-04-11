/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
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

#ifndef __GST_ES_ASSEMBLER_H__
#define __GST_ES_ASSEMBLER_H__

#include <gst/gst.h>

G_BEGIN_DECLS

//#define DEBUG_STREAM

/* #defines don't like whitespacey bits */
#define GST_TYPE_ES_ASSEMBLER \
  (gst_es_assembler_get_type())
#define GST_ES_ASSEMBLER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_ES_ASSEMBLER,GstESAssembler))
#define GST_ES_ASSEMBLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_ES_ASSEMBLER,GstESAssemblerClass))
#define GST_IS_ES_ASSEMBLER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_ES_ASSEMBLER))
#define GST_IS_ES_ASSEMBLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_ES_ASSEMBLER))

typedef struct _GstESAssembler GstESAssembler;
typedef struct _GstESAssemblerClass GstESAssemblerClass;

/* These are the states for the es packet assembler that combines transport
   packets into assembled ES buffers. There is a further state machine that
   processes the ES buffers */
typedef enum _ES_ASSEMBLER_STATE {
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
    ESA_STATE_WAITING_H9,       // Consuming PTS byte 1
    ESA_STATE_WAITING_H10,      // Consuming PTS byte 2
    ESA_STATE_WAITING_H11,      // Consuming PTS byte 3
    ESA_STATE_WAITING_H12,      // Consuming PTS byte 4
    ESA_STATE_WAITING_H13,      // Consuming PTS byte 5
    ESA_STATE_WAITING_H14,      // Consuming DTS byte 1
    ESA_STATE_WAITING_H15,      // Consuming DTS byte 2
    ESA_STATE_WAITING_H16,      // Consuming DTS byte 3
    ESA_STATE_WAITING_H17,      // Consuming DTS byte 4
    ESA_STATE_WAITING_H18,      // Consuming DTS byte 5
    ESA_STATE_WAITING_HDR_DONE, // waiting until the remaining header consumed
    ESA_STATE_PRE_ASSEMBLY,     // Pseudo-state to allocate output buffer
    ESA_STATE_ASSEMBLING,   
    ESA_STATE_MAX
} GstESAssemberState;

typedef enum _ES_PROCESSOR_STATE {
    ESP_STATE_WAITING_FOR_SEQ_START,
    ESP_STATE_RUNNING
} GstESProcessorState;

typedef enum _ES_STREAM_FORMAT {
    ES_FORMAT_NONE,
    ES_FORMAT_MPEGVIDEO,
    ES_FORMAT_H264VIDEO,
    ES_FORMAT_MPEGAUDIO,
    ES_FORMAT_AC3AUDIO,
    ES_FORMAT_EAC3AUDIO
} GstEsStreamFormat;

#define NS_PER_90KHZ_TICK_X256  2844444LL
#define ESA_MAX_PTS_PCR_OFFSET  1000000000LL   // Max PTS delay from PCR considered valid (ns)
#define ESA_FIXED_PTS_OFFSET    300000000LL    // Fixed PTS delay for invalid input PTS offset
//#define ESA_FIXED_PTS_OFFSET    0LL           // Fixed PTS delay for invalid input PTS offset
#define ESA_PTS_MAX_CHANGE      1000000000LL   // Max magnitude of PTS change that signals discontinuity
#define ESA_RT_MAX_CHANGE       1000000000LL   // Max magnitude of running time mismatch that signals discontinuity
#define ESA_PTS_DEFAULT_INCR    33366666LL     // Default frame time in ns

#define MPEG_SC_PICTURE         0x00000100
#define MPEG_SC_USER_DATA       0x000001B2
#define MPEG_SC_SEQUENCE_HEADER 0x000001B3
#define MPEG_SC_SEQUENCE_ERROR  0x000001B4
#define MPEG_SC_EXTENSION       0x000001B5
#define MPEG_SC_SEQUENCE_END    0x000001B7
#define MPEG_SC_GROUP           0x000001B8
#define MPEG_SC_NAL_UNIT        0x00000001
#define MPEG_NAL_UNIT_TYPE_A_U_D 0x09

#define MAX_ESA_PLAYRATE        1000.0
#define MIN_ESA_PLAYRATE        (0.0 - MAX_ESA_PLAYRATE)
#define DEFAULT_ESA_PLAYRATE    1.0

typedef enum _ES_START_CODE {
    ES_SC_PICTURE = 0,
    ES_SC_USER_DATA = 0xB2,
    ES_SC_SEQUENCE_HEADER = 0xB3,
    ES_SC_SEQUENCE_ERROR = 0xB4,
    ES_SC_EXTENSION = 0xB5,
    ES_SC_SEQUENCE_END = 0xB7,
    ES_SC_GROUP = 0xB8
} ES_START_CODE;

struct _GstESAssembler
{
  GstElement           element;

  /* Protects read/write to:
   * - playrate
   * - playrate_updated
   */
  GMutex              *props_lock;
  gfloat               playrate;
  gboolean             playrate_updated;


  GstPad              *sinkpad;
  GstPad              *srcpad;
  GstESAssemberState   state;
  GstESProcessorState  pstate;
  gboolean             silent;
  gboolean             is_pcr_pid;
  gboolean             ignore_cc_error;
  gboolean             do_timestamp;
  gdouble              applied_rate;
  guint8               last_continuity_counter;
  guint16              currentPID;
  guint8               stream_id;
  guint16              packet_length;
  gint32               packet_length_remaining;
  guint8               data_alignment_indicator;
  guint8               PTS_DTS_flags;
  guint8               ESCR_flag;
  guint8               ES_rate_flag;
  guint8               DSM_trick_mode_flag;
  guint8               additional_copy_info_flag;
  guint8               PES_CRC_flag;
  guint8               PES_extension_flag;
  guint8               PES_header_data_length;
  guint8               PES_header_remaining;
  guint16              horizontal_size_value;
  guint16              vertical_size_value;
  guint8               aspect_ratio_information;
  guint8               frame_rate_code;
  guint16              bit_rate_value;
  guint64              PTS;                    // PTS in 90KHz ticks
  guint64              DTS;                    // DTS in 90KHz ticks
  guint64              baseline_pts;           // value of first arriving PTS in ns
  gint64               pts_since_baseline;     // offset from baseline_pts of last PTS that arrived, in ns
  guint64              baseline_running_time;  // value of running time at first PTS in ns
  guint64              last_buffer_time_ns;
  guint64              buffer_time_delta_ns;
  guint64              last_media_time_ns;
  guint8               last_picture_type;
  GstEsStreamFormat    stream_format;
  GstClock            *pipeline_clock;
  GstClockTime         pusi_arrival_time;      /* Timestamp of GstBuffer containing PUSI */

  GstBuffer           *outbuf;
};

struct _GstESAssemblerClass 
{
  GstElementClass parent_class;
};

GType gst_es_assembler_get_type (void);

G_END_DECLS

#endif /* __GST_ES_ASSEMBLER_H__ */
