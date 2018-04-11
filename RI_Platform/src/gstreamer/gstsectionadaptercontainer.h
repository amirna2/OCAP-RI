/* GStreamer
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

#include <gst/gst.h>
#include "gstsectionadapter.h"

#ifndef __GST_SECTION_ADAPTER_CONTAINER_H__
#define __GST_SECTION_ADAPTER_CONTAINER_H__

G_BEGIN_DECLS

#define GST_TYPE_SECTION_ADAPTER_CONTAINER \
  (gst_section_adapter_container_get_type())
#define GST_SECTION_ADAPTER_CONTAINER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj), GST_TYPE_SECTION_ADAPTER_CONTAINER, GstSectionAdapterContainer))
#define GST_SECTION_ADAPTER_CONTAINER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass), GST_TYPE_SECTION_ADAPTER_CONTAINER, GstSectionAdapterContainerClass))
#define GST_SECTION_ADAPTER_CONTAINER_GET_CLASS(obj) \
  (G_TYPE_INSTANCE_GET_CLASS ((obj), GST_TYPE_SECTION_ADAPTER_CONTAINER, GstSectionAdapterContainerClass))
#define GST_IS_SECTION_ADAPTER_CONTAINER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj), GST_TYPE_SECTION_ADAPTER_CONTAINER))
#define GST_IS_SECTION_ADAPTER_CONTAINER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), GST_TYPE_SECTION_ADAPTER_CONTAINER))

typedef struct _GstSectionAdapterContainer GstSectionAdapterContainer;
typedef struct _GstSectionAdapterContainerClass GstSectionAdapterContainerClass;

/**
 * GstSectionAdapterContainer:
 * @object: the parent object.
 *
 * The opaque #GstSectionAdapterContainer data structure.
 */
struct _GstSectionAdapterContainer
{
    GObject parent;

    /*< public >*/
    GMutex *adapter_lock;

    /*< public > *//* with LOCK */
    GstSectionAdapter *adapter;
    GstBuffer *metadata;

    /*< private >*/
    gpointer _gst_reserved[GST_PADDING - 1];
};

struct _GstSectionAdapterContainerClass
{
    GObjectClass parent_class;

    /*< private >*/
    gpointer _gst_reserved[GST_PADDING];
};

// free with g_object_unref()
GstSectionAdapterContainer* gst_section_adapter_container_new(guint16 PID);
gint gst_section_adapter_container_key_compare_func(gconstpointer a,
        gconstpointer b);
gboolean gst_section_adapter_container_add_to_queue_func(gpointer key,
        gpointer value, gpointer user_data_queue);
GType gst_section_adapter_container_get_type(void);

G_END_DECLS

#endif /* __GST_SECTION_ADAPTER_CONTAINER_H__ */
