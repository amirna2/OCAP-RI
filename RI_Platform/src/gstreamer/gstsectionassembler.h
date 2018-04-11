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

#ifndef __GST_SECTION_ASSEMBLER_H__
#define __GST_SECTION_ASSEMBLER_H__

#include <gst/gst.h>
#include <gst/base/gstadapter.h>

#include "gstmpeg.h"

G_BEGIN_DECLS

/* #defines don't like whitespacey bits */
#define GST_TYPE_SECTION_ASSEMBLER \
  (gst_section_assembler_get_type())
#define GST_SECTION_ASSEMBLER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_SECTION_ASSEMBLER,GstSectionAssembler))
#define GST_SECTION_ASSEMBLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_SECTION_ASSEMBLER,GstSectionAssemblerClass))
#define GST_IS_SECTION_ASSEMBLER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_SECTION_ASSEMBLER))
#define GST_IS_SECTION_ASSEMBLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_SECTION_ASSEMBLER))

typedef struct _GstSectionAssembler GstSectionAssembler;
typedef struct _GstSectionAssemblerClass GstSectionAssemblerClass;

#define DEFAULT_DROP_TEI_PACKETS       FALSE
#define DEFAULT_DROP_TSC_PACKETS       FALSE
#define DEFAULT_MAX_SECTION_ASSEMBLERS 64
#define DEFAULT_ASSEMBLE_ON_ALL_PIDS   FALSE
#define DEFAULT_CHECK_CRC              TRUE
#define DEFAULT_TEST_INTERFACE         FALSE

struct _GstSectionAssembler
{
    GstElement element;

    GstPad *sinkpad, *srcpad;

    gboolean drop_tei_packets;
    gboolean drop_tsc_packets;
    guint16 max_section_assemblers;
    gboolean assemble_on_all_PIDs;
    gboolean check_crc;
    gboolean test_interface;

    // SectionAssembler specific variables
    GstAdapter* adapter;

    /*< public >*/
    GMutex *adapter_containers_lock;

    /*< public > *//* with LOCK */
    GTree *adapter_containers_tree;
};

struct _GstSectionAssemblerClass
{
    GstElementClass parent_class;
};

GType gst_section_assembler_get_type(void);

G_END_DECLS

#endif /* __GST_SECTION_ASSEMBLER_H__ */
