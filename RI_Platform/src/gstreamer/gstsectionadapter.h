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
#include <gst/base/gstadapter.h>

#ifndef __GST_SECTION_ADAPTER_H__
#define __GST_SECTION_ADAPTER_H__

G_BEGIN_DECLS

#define GST_TYPE_SECTION_ADAPTER \
  (gst_section_adapter_get_type())
#define GST_SECTION_ADAPTER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj), GST_TYPE_SECTION_ADAPTER, GstSectionAdapter))
#define GST_SECTION_ADAPTER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass), GST_TYPE_SECTION_ADAPTER, GstSectionAdapterClass))
#define GST_SECTION_ADAPTER_GET_CLASS(obj) \
  (G_TYPE_INSTANCE_GET_CLASS ((obj), GST_TYPE_SECTION_ADAPTER, GstSectionAdapterClass))
#define GST_IS_SECTION_ADAPTER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj), GST_TYPE_SECTION_ADAPTER))
#define GST_IS_SECTION_ADAPTER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), GST_TYPE_SECTION_ADAPTER))

typedef struct _GstSectionAdapter GstSectionAdapter;
typedef struct _GstSectionAdapterClass GstSectionAdapterClass;

typedef enum
{
    STATE_LOOKING_FOR_PUSI,
    STATE_ASSEMBLING_HEADER,
    STATE_ASSEMBLING_DATA,
    STATE_SECTION_COMPLETE
} section_adapter_state_t;

/**
 * GstSectionAdapter:
 * @object: the parent object.
 *
 * The opaque #GstSectionAdapter data structure.
 */
struct _GstSectionAdapter
{
    GObject parent;

    /*< private >*/

    /* Per-adapter data - persistent across section and transport packet data changes */
    GQueue *assembled_section_queue;

    /* Per-transport packet data */
    guint8 last_continuity_counter;
    guint16 still_discontinuous;

    /* Per-section data */
    section_adapter_state_t section_adapter_state;
    GstAdapter *section_adapter;
    guint16 section_length;

    /* Misc */
    gpointer _gst_reserved[GST_PADDING - 1];
};

struct _GstSectionAdapterClass
{
    GObjectClass parent_class;

    /*< private >*/
    gpointer _gst_reserved[GST_PADDING];
};

// free with g_object_unref()
GstSectionAdapter* gst_section_adapter_new(void);
void gst_section_adapter_clear(GstSectionAdapter *section_adapter);
void gst_section_adapter_push_packet(GstSectionAdapter *section_adapter,
        GstBuffer *buf);
GstBuffer* gst_section_adapter_take_section(GstSectionAdapter *section_adapter);
guint
        gst_section_adapter_available_sections(
                GstSectionAdapter *section_adapter);
GType gst_section_adapter_get_type(void);

G_END_DECLS

#endif /* __GST_SECTION_ADAPTER_H__ */
