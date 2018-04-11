/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 * Copyright (C) 2009 Cable Television Laboratories, Inc. 
 *
 * gstbuffer.h: Header for GstBuffer object
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

#ifndef __GST_SECTION_FILTER_ITEM_H__
#define __GST_SECTION_FILTER_ITEM_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define GST_TYPE_SECTION_FILTER_ITEM            (gst_section_filter_item_get_type())
#define GST_IS_SECTION_FILTER_ITEM(obj)         (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_SECTION_FILTER_ITEM))
#define GST_IS_SECTION_FILTER_ITEM_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_SECTION_FILTER_ITEM))
#define GST_SECTION_FILTER_ITEM_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), GST_TYPE_SECTION_FILTER_ITEM, GstSectionFilterItemClass))
#define GST_SECTION_FILTER_ITEM(obj)            (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_SECTION_FILTER_ITEM, GstSectionFilterItem))
#define GST_SECTION_FILTER_ITEM_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_SECTION_FILTER_ITEM, GstSectionFilterItemClass))
#define GST_SECTION_FILTER_ITEM_CAST(obj)       ((GstSectionFilterItem *)(obj))

typedef struct _GstSectionFilterItem GstSectionFilterItem;
typedef struct _GstSectionFilterItemClass GstSectionFilterItemClass;

struct _GstSectionFilterItem
{
    GstMiniObject parent;

    /*< public >*//* with COW */
    guint32 filter_id;
    guint16 PID;

    /*< private >*/
    guint16 pos_length;
    guint8 *pos_mask;
    guint8 *pos_value;

    guint16 neg_length;
    guint8 *neg_mask;
    guint8 *neg_value;

    gpointer _gst_reserved[GST_PADDING];
};

struct _GstSectionFilterItemClass
{
    GstMiniObjectClass parent_class;
};

GstSectionFilterItem * gst_section_filter_item_new(guint32 filter_id,
        guint16 PID, guint16 pos_length, guint8 * pos_mask, guint8 * pos_value,
        guint16 neg_length, guint8 * neg_mask, guint8 * neg_value);
gboolean gst_section_filter_item_matches(GstSectionFilterItem * item,
        GstBuffer * buf);
char * gst_section_filter_item_snprintf(GstSectionFilterItem * item,
        char * buffer, size_t size);
GType gst_section_filter_item_get_type(void);

G_END_DECLS

#endif /* __GST_SECTION_FILTER_ITEM__ */
