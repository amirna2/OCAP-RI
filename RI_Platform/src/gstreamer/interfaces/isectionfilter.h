/* GStreamer Color Balance
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2009 Cable Television Laboratories, Inc. 
 *
 * color-balance.h: image color balance interface design
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

#ifndef __GST_ISECTION_FILTER_H__
#define __GST_ISECTION_FILTER_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define GST_TYPE_ISECTION_FILTER \
  (gst_isection_filter_get_type ())
#define GST_ISECTION_FILTER(obj) \
  (GST_IMPLEMENTS_INTERFACE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_ISECTION_FILTER, \
                                                 GstISectionFilter))
#define GST_ISECTION_FILTER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_ISECTION_FILTER, \
                            GstISectionFilterClass))
#define GST_IS_ISECTION_FILTER(obj) \
  (GST_IMPLEMENTS_INTERFACE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_ISECTION_FILTER))
#define GST_IS_ISECTION_FILTER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_ISECTION_FILTER))
#define GST_ISECTION_FILTER_GET_CLASS(inst) \
  (G_TYPE_INSTANCE_GET_INTERFACE ((inst), GST_TYPE_ISECTION_FILTER, GstISectionFilterClass))

typedef struct _GstISectionFilter GstISectionFilter;
typedef struct _GstISectionFilterClass GstISectionFilterClass;

struct _GstISectionFilterClass
{
    GTypeInterface parent;

    /* virtual functions */
    void (*create)(GstISectionFilter* filter, guint16 PID, guint16 pos_length,
            guint8* pos_mask, guint8* pos_values, guint16 neg_length,
            guint8* neg_mask, guint8* neg_values, guint32* filter_id);
    void (*cancel)(GstISectionFilter* filter, guint32 filter_id);
    guint16 (*get_pid_for_filter)(GstISectionFilter* filter, guint32 filter_id);

    gpointer _gst_reserved[GST_PADDING];
};

GType gst_isection_filter_get_type(void);

/* virtual class function wrappers */
void gst_isection_filter_create(GstISectionFilter* filter, guint16 PID,
        guint16 pos_length, guint8* pos_mask, guint8* pos_values,
        guint16 neg_length, guint8* neg_mask, guint8* neg_values,
        guint32* filter_id);
void gst_isection_filter_cancel(GstISectionFilter* filter, guint32 filter_id);
guint16 gst_isection_filter_get_pid_for_filter(GstISectionFilter* filter,
        guint32 filter_id);
G_END_DECLS

#endif /* __GST_ISECTION_FILTER_H__ */

