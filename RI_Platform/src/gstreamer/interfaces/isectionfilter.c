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

#include "isectionfilter.h"
#include "../gstmpeg.h"

static void gst_isection_filter_class_init(GstISectionFilterClass * klass);

GType gst_isection_filter_get_type(void)
{
    static GType gst_isection_filter_type = 0;

    if (!gst_isection_filter_type)
    {
        static const GTypeInfo gst_isection_filter_info =
        { sizeof(GstISectionFilterClass),
                (GBaseInitFunc) gst_isection_filter_class_init, NULL, NULL,
                NULL, NULL, 0, 0, NULL, };

        gst_isection_filter_type = g_type_register_static(G_TYPE_INTERFACE,
                "GstISectionFilter", &gst_isection_filter_info, 0);
        g_type_interface_add_prerequisite(gst_isection_filter_type,
                GST_TYPE_IMPLEMENTS_INTERFACE);
    }

    return gst_isection_filter_type;
}

static void gst_isection_filter_class_init(GstISectionFilterClass * klass)
{
    /* default virtual functions */
    klass->create = NULL;
    klass->cancel = NULL;
}

/**
 * gst_isection_filter_create:
 * @filter: A #GstISectionFilter instance
 * @PID: ...
 * 
 * Enable section filtering on given PID.
 */
void gst_isection_filter_create(GstISectionFilter* filter, guint16 PID,
        guint16 pos_length, guint8* pos_mask, guint8* pos_values,
        guint16 neg_length, guint8* neg_mask, guint8* neg_values,
        guint32* filter_id)
{
    GstISectionFilterClass *klass = GST_ISECTION_FILTER_GET_CLASS(filter);

    if (klass->create)
    {
        klass->create(filter, PID, pos_length, pos_mask, pos_values,
                neg_length, neg_mask, neg_values, filter_id);
    }
}

/**
 * gst_isection_filter_cancel:
 * @filter: A #GstISectionFilter instance
 * @PID: ...
 * 
 * Disable section filtering on given PID.
 */
void gst_isection_filter_cancel(GstISectionFilter* filter, guint32 filter_id)
{
    GstISectionFilterClass *klass = GST_ISECTION_FILTER_GET_CLASS(filter);

    if (klass->cancel)
    {
        klass->cancel(filter, filter_id);
    }
}

/**
 * gst_isection_filter_get_pid_for_filter:
 * @filter: A #GstISectionFilter instance
 * @PID: ...
 * 
 * Returns PID associated with given filter_id or INVALID_PID.
 */
guint16 gst_isection_filter_get_pid_for_filter(GstISectionFilter* filter,
        guint32 filter_id)
{
    GstISectionFilterClass *klass = GST_ISECTION_FILTER_GET_CLASS(filter);

    if (klass->get_pid_for_filter)
    {
        return klass->get_pid_for_filter(filter, filter_id);
    }
    else
    {
        return INVALID_PID;
    }
}
