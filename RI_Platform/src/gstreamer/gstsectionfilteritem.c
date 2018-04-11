/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 * Copyright (C) 2009 Cable Television Laboratories, Inc.
 *
 * gstbuffer.c: Buffer operations
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
 * FIXME:Describe sectionfilteritem here.
 */

#include <stdio.h>
#include "gstsectionfilteritem.h"

GST_DEBUG_CATEGORY_STATIC ( gst_section_filter_item_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_section_filter_item_debug

#define _do_init(thing) \
  GST_DEBUG_CATEGORY_INIT (gst_section_filter_item_debug, "sectionfilteritem", 0, "Single MPEG-2 Section Filter Item")

/*lint -e(123)*/GST_BOILERPLATE_FULL (GstSectionFilterItem, gst_section_filter_item, GstMiniObject, GST_TYPE_MINI_OBJECT, _do_init)

// Forward declarations
static GstSectionFilterItem * gst_section_filter_item_copy (GstSectionFilterItem * item);
static void gst_section_filter_item_finalize(GstSectionFilterItem * item);

static gboolean gst_section_filter_item_buffers_match(guint16 length,
        guint8 *mask, guint8 *buf1, guint8 *buf2);

/**
 * gst_section_filter_item_new:
 *
 * Creates a new #GstSectionFilterItem. Free with gst_mini_object_unref().
 *
 * Returns: a new #GstSectionFilterItem
 */
GstSectionFilterItem *
gst_section_filter_item_new(guint32 filter_id, guint16 PID, guint16 pos_length,
        guint8 * pos_mask, guint8 * pos_value, guint16 neg_length,
        guint8 * neg_mask, guint8 * neg_value)
{
    GstSectionFilterItem *item = GST_SECTION_FILTER_ITEM_CAST(
            gst_mini_object_new(GST_TYPE_SECTION_FILTER_ITEM));

    item->filter_id = filter_id;
    item->PID = PID;
    item->pos_length = pos_length;
    item->pos_mask = g_memdup(pos_mask, pos_length);
    item->pos_value = g_memdup(pos_value, pos_length);
    item->neg_length = neg_length;
    item->neg_mask = g_memdup(neg_mask, neg_length);
    item->neg_value = g_memdup(neg_value, neg_length);

    return item;
}

/**
 * gst_section_filter_item_matches:
 *
 * Returns: TRUE if #GstBuffer matches #GstSectionFilterItem, FALSE otherwise
 */
gboolean gst_section_filter_item_matches(GstSectionFilterItem * item,
        GstBuffer * buf)
{
    gboolean retval;

    if (item->PID != GST_READ_UINT16_BE(GST_BUFFER_DATA(buf)))
    {
        retval = FALSE;
    }
    else
    {
        guint PID_size = sizeof(guint16);
        GstBuffer *section = gst_buffer_create_sub(buf, PID_size,
                GST_BUFFER_SIZE(buf) - PID_size);

        if (item->pos_length > GST_BUFFER_SIZE(section) || item->neg_length
                > GST_BUFFER_SIZE(section))
        {
            retval = FALSE;
        }
        else if (gst_section_filter_item_buffers_match(item->pos_length,
                item->pos_mask, item->pos_value, GST_BUFFER_DATA(section))
                == TRUE && (item->neg_length == 0
                || gst_section_filter_item_buffers_match(item->neg_length,
                        item->neg_mask, item->neg_value, GST_BUFFER_DATA(
                                section)) == FALSE))
        {
            retval = TRUE;
        }
        else
        {
            retval = FALSE;
        }

        gst_buffer_unref(section);
    }

    return retval;
}

#define DUMP_MAX_TERMS 32
#define DUMP_STRIDE    2
#define DUMP_MAX_SIZE  (DUMP_STRIDE * DUMP_MAX_TERMS + 1)

char *
gst_section_filter_item_snprintf(GstSectionFilterItem * item, char * buffer,
        size_t size)
{
    int i = 0;
    int pos_length = MIN(item->pos_length, DUMP_MAX_TERMS);
    int neg_length = MIN(item->neg_length, DUMP_MAX_TERMS);

    char pos_mask[DUMP_MAX_SIZE];
    char pos_value[DUMP_MAX_SIZE];
    char neg_mask[DUMP_MAX_SIZE];
    char neg_value[DUMP_MAX_SIZE];

    for (i = 0; i < pos_length; i++)
    {
        snprintf(&pos_mask[i * DUMP_STRIDE], DUMP_STRIDE + 1, "%02X",
                item->pos_mask[i]);
        snprintf(&pos_value[i * DUMP_STRIDE], DUMP_STRIDE + 1, "%02X",
                item->pos_value[i]);
    }
    pos_mask[i * DUMP_STRIDE] = pos_value[i * DUMP_STRIDE] = '\0';

    for (i = 0; i < neg_length; i++)
    {
        snprintf(&neg_mask[i * DUMP_STRIDE], DUMP_STRIDE + 1, "%02X",
                item->neg_mask[i]);
        snprintf(&neg_value[i * DUMP_STRIDE], DUMP_STRIDE + 1, "%02X",
                item->neg_value[i]);
    }
    neg_mask[i * DUMP_STRIDE] = neg_value[i * DUMP_STRIDE] = '\0';

    snprintf(
            buffer,
            size,
            "filter_id %8u: PID %4u, pos_length %2u, pos_mask %s, pos_value %s, neg_length %2u, neg_mask %s, neg_value %s",
            item->filter_id, item->PID, item->pos_length, pos_mask, pos_value,
            item->neg_length, neg_mask, neg_value);

    return buffer;
}

//
//
//
// INTERNAL IMPLEMENTATION
//
//
//

/**************************************************/
/**********                              **********/
/********** GstMiniObject IMPLEMENTATION **********/
/**********                              **********/
/**************************************************/

static void gst_section_filter_item_base_init(gpointer g_class)
{
    /* nop */
}

static void gst_section_filter_item_class_init(
        GstSectionFilterItemClass * klass)
{
    GstMiniObjectClass *mini_object = GST_MINI_OBJECT_CLASS(klass);

    mini_object->copy
            = (GstMiniObjectCopyFunction) gst_section_filter_item_copy;
    mini_object->finalize
            = (GstMiniObjectFinalizeFunction) gst_section_filter_item_finalize;
}

static void gst_section_filter_item_init(GstSectionFilterItem * item,
        GstSectionFilterItemClass * g_class)
{
    item->filter_id = 0;

    item->pos_length = 0;
    item->pos_mask = NULL;
    item->pos_value = NULL;

    item->neg_length = 0;
    item->neg_mask = NULL;
    item->neg_value = NULL;
}

static void gst_section_filter_item_finalize(GstSectionFilterItem * item)
{
    g_return_if_fail(item != NULL);

    if (item->pos_length > 0)
    {
        g_free(item->pos_mask);
        g_free(item->pos_value);
    }

    if (item->neg_length > 0)
    {
        g_free(item->neg_mask);
        g_free(item->neg_value);
    }

    GST_MINI_OBJECT_CLASS(parent_class)->finalize(GST_MINI_OBJECT(item));
}

static GstSectionFilterItem *
gst_section_filter_item_copy(GstSectionFilterItem * item)
{
    g_return_val_if_fail(item != NULL, NULL);

    return gst_section_filter_item_new(item->filter_id, item->PID,
            item->pos_length, item->pos_mask, item->pos_value,
            item->neg_length, item->neg_mask, item->neg_value);
}

/*********************************************************/
/**********                                     **********/
/********** GstSectionFilterItem IMPLEMENTATION **********/
/**********                                     **********/
/*********************************************************/

static gboolean gst_section_filter_item_buffers_match(guint16 length,
        guint8 *mask, guint8 *buf1, guint8 *buf2)
{
    guint i;
    for (i = 0; i < length; i++)
    {
        if ((buf1[i] & mask[i]) != (buf2[i] & mask[i]))
        {
            return FALSE;
        }
    }
    return TRUE;
}
