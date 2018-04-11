/* GStreamer Color Balance
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2009 Cable Television Laboratories, Inc. 
 *
 * colorbalance.c: image color balance interface design
 *                 virtual class function wrappers
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

#include "isectionassembler.h"

static void
        gst_isection_assembler_class_init(GstISectionAssemblerClass * klass);

GType gst_isection_assembler_get_type(void)
{
    static GType gst_isection_assembler_type = 0;

    if (!gst_isection_assembler_type)
    {
        static const GTypeInfo gst_isection_assembler_info =
        { sizeof(GstISectionAssemblerClass),
                (GBaseInitFunc) gst_isection_assembler_class_init, NULL, NULL,
                NULL, NULL, 0, 0, NULL, };

        gst_isection_assembler_type = g_type_register_static(G_TYPE_INTERFACE,
                "GstISectionAssembler", &gst_isection_assembler_info, 0);
        g_type_interface_add_prerequisite(gst_isection_assembler_type,
                GST_TYPE_IMPLEMENTS_INTERFACE);
    }

    return gst_isection_assembler_type;
}

static void gst_isection_assembler_class_init(GstISectionAssemblerClass * klass)
{
    /* default virtual functions */
    klass->enable = NULL;
}

/**
 * gst_isection_assembler_enable:
 * @assembler: A #GstISectionAssembler instance
 * @PID: ...
 * @enable: TRUE to enable, FALSE to disable
 * 
 * Enable/disable section assembly on given PID.
 */
void gst_isection_assembler_enable(GstISectionAssembler * assembler,
        guint16 PID, gboolean enable)
{
    GstISectionAssemblerClass *klass = GST_ISECTION_ASSEMBLER_GET_CLASS(
            assembler);

    if (klass->enable)
    {
        klass->enable(assembler, PID, enable);
    }
}
