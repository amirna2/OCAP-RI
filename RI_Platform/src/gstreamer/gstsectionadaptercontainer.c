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

/**
 * FIXME:Describe sectionadaptercontainer here.
 */

#include "gstsectionadaptercontainer.h"
#include "gstmpeg.h"

GST_DEBUG_CATEGORY_STATIC ( gst_section_adapter_container_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_section_adapter_container_debug

#define _do_init(thing) \
  GST_DEBUG_CATEGORY_INIT (gst_section_adapter_container_debug, "sectionadaptercontainer", 0, "Single PID MPEG-2 private section adapter container")

/*lint -e(123)*/GST_BOILERPLATE_FULL (GstSectionAdapterContainer, gst_section_adapter_container, GObject, G_TYPE_OBJECT, _do_init)

// Forward declarations
static void gst_section_adapter_container_finalize (GObject * object);

/**
 * gst_section_adapter_container_new:
 *
 * Creates a new #GstSectionAdapterContainer. Free with g_object_unref().
 *
 * Returns: a new #GstSectionAdapterContainer
 */
GstSectionAdapterContainer *
gst_section_adapter_container_new(guint16 PID)
{
    GstSectionAdapterContainer *container = NULL;

    g_return_val_if_fail(PID <= TS_MAX_PIDS, NULL);

    container = g_object_new(GST_TYPE_SECTION_ADAPTER_CONTAINER, NULL);

    container->metadata = gst_buffer_new_and_alloc(sizeof(PID));

    GST_WRITE_UINT16_BE(&GST_BUFFER_DATA(container->metadata)[0], PID);

    return container;
}

gint gst_section_adapter_container_key_compare_func(gconstpointer a,
        gconstpointer b)
{
    return GPOINTER_TO_INT(a) - GPOINTER_TO_INT(b);
}

gboolean gst_section_adapter_container_add_to_queue_func(gpointer key,
        gpointer value, gpointer user_data_queue)
{
    g_queue_push_tail((GQueue*) user_data_queue, value);
    return FALSE;
}

//
//
//
// INTERNAL IMPLEMENTATION
//
//
//

/********************************************/
/**********                        **********/
/********** GObject IMPLEMENTATION **********/
/**********                        **********/
/********************************************/

static void gst_section_adapter_container_base_init(gpointer g_class)
{
    /* nop */
}

static void gst_section_adapter_container_class_init(
        GstSectionAdapterContainerClass * klass)
{
    GObjectClass *object = G_OBJECT_CLASS(klass);

    object->finalize = gst_section_adapter_container_finalize;
}

static void gst_section_adapter_container_init(
        GstSectionAdapterContainer * container,
        GstSectionAdapterContainerClass * g_class)
{
    container->adapter_lock = g_mutex_new();
    container->adapter = gst_section_adapter_new();
}

static void gst_section_adapter_container_finalize(GObject * object)
{
    GstSectionAdapterContainer *container = GST_SECTION_ADAPTER_CONTAINER(
            object);

    g_mutex_free(container->adapter_lock);
    g_object_unref(container->adapter);
    gst_buffer_unref(container->metadata);

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, finalize, (object));
}
