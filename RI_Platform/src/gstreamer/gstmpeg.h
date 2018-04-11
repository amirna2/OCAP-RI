/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2008 U-PRESTOMarcin <<user@hostname.org>>
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

#ifndef __GST_MPEG_H__
#define __GST_MPEG_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define TS_PACKET_SIZE                           188
#define TS_PACKET_MAX_OFFSET                     (TS_PACKET_SIZE - 1)
#define TS_SYNC_BYTE                             0x47
#define TS_MAX_PIDS                              0x1FFF

// Whether adaptation field or payload, the actual
// packet data starts right after the continuity counter
#define TS_PACKET_HEADER_LENGTH                  4
#define TS_PACKET_DATA_START_OFFSET              TS_PACKET_HEADER_LENGTH
#define TS_PACKET_ADAPTATION_FIELD_LENGTH_LENGTH 1
#define TS_PACKET_POINTER_FIELD_LENGTH           1

#define INVALID_PAYLOAD_OFFSET                   0xFF
#define INVALID_TABLE_ID                         0xFF
#define INVALID_CONTINUITY_COUNTER               0xFF
#define INVALID_SECTION_LENGTH                   0xFFFF
#define INVALID_PID                              0x2000

#define ADAPTATION_FIELD_RESERVED                0
#define ADAPTATION_FIELD_PAYLOAD_ONLY            1
#define ADAPTATION_FIELD_ONLY                    2
#define ADAPTATION_FIELD_AND_PAYLOAD             3

#define SECTION_HEADER_LENGTH                    3
#define SECTION_HEADER_LENGTH_OFFSET             1
#define SECTION_DATA_MAX_LENGTH                  (4096 - SECTION_HEADER_LENGTH)

void crc32_init(void);
guint32 crc32_calc(guint8 *data, guint len);

G_END_DECLS

#endif /* __GST_TRANSPORTSYNC_H__ */
