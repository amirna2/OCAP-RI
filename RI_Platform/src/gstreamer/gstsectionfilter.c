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

/**
 * SECTION:element-sectionfilter
 *
 * FIXME:Describe sectionfilter here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! sectionfilter ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <string.h> // g_memmove
#include <gst/gst.h>

#include "gstsectionfilter.h"
#include "interfaces/isectionfilter.h"
#include "gstsectionfilteritem.h"
#include "gstmpeg.h"
#include "gstmatchedsection.h"

GST_DEBUG_CATEGORY_STATIC ( gst_section_filter_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_section_filter_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0, PROP_TEST_INTERFACE
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate sink_factory =
        GST_STATIC_PAD_TEMPLATE("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
                GST_STATIC_CAPS("application/x-sections"));

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC, GST_PAD_ALWAYS, GST_STATIC_CAPS(
                "application/x-matched-sections"));

/*lint -esym(578,GstSectionFilter)*/
GST_BOILERPLATE_WITH_INTERFACE (GstSectionFilter, gst_section_filter, GstElement,
        GST_TYPE_ELEMENT, GstISectionFilter, GST_TYPE_ISECTION_FILTER, gst_isection_filter)

// Forward declarations
static void gst_section_filter_dispose (GObject * object);
static void gst_section_filter_finalize(GObject * object);

static void gst_section_filter_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec);
static void gst_section_filter_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec);

static GstFlowReturn gst_section_filter_chain(GstPad * pad, GstBuffer * buf);

static gpointer gst_section_filter_test_thread(
        gpointer user_data_section_filter);

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

static void gst_section_filter_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "SectionFilter",
            "FIXME:Generic", "FIXME:Generic Template Element",
            "U-PRESTOMarcin <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the sectionfilter's class */
static void gst_section_filter_class_init(GstSectionFilterClass * klass)
{
    GObjectClass *gobject_class;

    GST_DEBUG_CATEGORY_INIT(gst_section_filter_debug, "sectionfilter", 0,
            "MPEG-2 Private Section Filter");

    gobject_class = (GObjectClass *) klass;

    gobject_class->dispose = gst_section_filter_dispose;
    gobject_class->finalize = gst_section_filter_finalize;

    gobject_class->set_property = gst_section_filter_set_property;
    gobject_class->get_property = gst_section_filter_get_property;

    g_object_class_install_property(gobject_class, PROP_TEST_INTERFACE,
            g_param_spec_boolean("test-interface",
                    "Test interface ISectionFilter",
                    "Run ISectionFilter test thread", DEFAULT_TEST_INTERFACE,
                    G_PARAM_READWRITE | G_PARAM_CONSTRUCT));
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_section_filter_init(GstSectionFilter * filter,
        GstSectionFilterClass * gclass)
{
    // sink (input) pad
    filter->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    gst_pad_set_chain_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_section_filter_chain));
    gst_pad_use_fixed_caps(filter->sinkpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->sinkpad);

    // source (output) pad
    filter->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    gst_pad_use_fixed_caps(filter->sinkpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->srcpad);

    // section filter specific initialization
    filter->filter_lock = g_mutex_new();
    filter->filter_item_list = NULL;
}

static void gst_section_filter_dispose(GObject * object)
{
    GstSectionFilter *filter = GST_SECTION_FILTER(object);

    filter->test_interface = FALSE;

    g_mutex_lock(filter->filter_lock);
    g_slist_foreach(filter->filter_item_list, (GFunc) gst_mini_object_unref,
            NULL);
    g_mutex_unlock(filter->filter_lock);

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, dispose, (object));
}

static void gst_section_filter_finalize(GObject * object)
{
    GstSectionFilter *filter = GST_SECTION_FILTER(object);

    g_mutex_free(filter->filter_lock);
    g_slist_free(filter->filter_item_list);

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, finalize, (object));
}

static void gst_section_filter_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec)
{
    GstSectionFilter *filter = GST_SECTION_FILTER(object);

    switch (prop_id)
    {
    case PROP_TEST_INTERFACE:
        filter->test_interface = g_value_get_boolean(value);
        if (filter->test_interface == TRUE)
        {
            (void) g_thread_create(gst_section_filter_test_thread, filter,
                    TRUE, NULL);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_section_filter_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec)
{
    GstSectionFilter *filter = GST_SECTION_FILTER(object);

    switch (prop_id)
    {
    case PROP_TEST_INTERFACE:
        g_value_set_boolean(value, filter->test_interface);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/***********************************************/
/**********                           **********/
/********** GstElement IMPLEMENTATION **********/
/**********                           **********/
/***********************************************/

/*
 * chain function
 * this function does the actual processing
 *
 *       IN:
 * ---------------
 * PID                                                16 uimsbf
 * private_section()
 * {
 *   table_id                                          8 uimsbf
 *   section_syntax_indicator                          1 bslbf
 *   private_indicator                                 1 bslbf
 *   reserved                                          2 bslbf
 *   private_section_length                           12 uimsbf
 *   if (section_syntax_indicator = = '0')
 *   {
 *     for (i = 0; i < N; i++)
 *     {
 *       private_data_byte                             8 bslbf
 *     }
 *   }
 *   else
 *   {
 *     table_id_extension                             16 uimsbf
 *     reserved                                        2 bslbf
 *     version_number                                  5 uimsbf
 *     current_next_indicator                          1 bslbf
 *     section_number                                  8 uimsbf
 *     last_section_number                             8 uimsbf
 *     for (i = 0; i < private_section_length-9; i++)
 *     {
 *       private_data_byte                             8 bslbf
 *     }
 *     CRC_32                                         32 rpchof
 *   }
 * }
 *
 *       OUT:
 * ----------------
 * PID                                                16 uimsbf
 * private_section_offset                             16 uimsbf
 * private_section_length                             16 uimsbf
 * filter_ids_offset                                  16 uimsbf
 * filter_ids_length                                  16 uimsbf
 * private_section()
 * {
 *   table_id                                          8 uimsbf
 *   section_syntax_indicator                          1 bslbf
 *   private_indicator                                 1 bslbf
 *   reserved                                          2 bslbf
 *   private_section_length                           12 uimsbf
 *   if (section_syntax_indicator = = '0')
 *   {
 *     for (i = 0; i < N; i++)
 *     {
 *       private_data_byte                             8 bslbf
 *     }
 *   }
 *   else
 *   {
 *     table_id_extension                             16 uimsbf
 *     reserved                                        2 bslbf
 *     version_number                                  5 uimsbf
 *     current_next_indicator                          1 bslbf
 *     section_number                                  8 uimsbf
 *     last_section_number                             8 uimsbf
 *     for (i = 0; i < private_section_length-9; i++)
 *     {
 *       private_data_byte                             8 bslbf
 *     }
 *     CRC_32                                         32 rpchof
 *   }
 * }
 * for (i = 0; i < N; i++)
 * {
 *   filter_id                                        32 uimsbf
 * }
 *
 */
static GstFlowReturn gst_section_filter_chain(GstPad * pad, GstBuffer * buf)
{
    GstSectionFilter *filter = GST_SECTION_FILTER(GST_OBJECT_PARENT(pad));
    GstFlowReturn ret = GST_FLOW_OK;
    GSList *list_iterator = NULL;
    GQueue matched_filter_id_queue = G_QUEUE_INIT;
    GstBuffer *matched_filter_data = NULL;

    GST_DEBUG_OBJECT(filter, "Received section %p", buf);

    // apply the filters
    g_mutex_lock(filter->filter_lock);
    list_iterator = filter->filter_item_list;
    while (list_iterator != NULL)
    {
        GstSectionFilterItem *item = GST_SECTION_FILTER_ITEM(
                list_iterator->data);
        GST_DEBUG_OBJECT(filter, "Applying filter %u", item->filter_id);
        if (gst_section_filter_item_matches(item, buf) == TRUE)
        {
            g_queue_push_tail(&matched_filter_id_queue, GUINT_TO_POINTER(
                    item->filter_id));
            GST_DEBUG_OBJECT(filter, "Filter %u matched section %p",
                    item->filter_id, buf);
        }
        list_iterator = g_slist_next(list_iterator);
    }

    // send the section, if it matched at least one filter
    if (g_queue_is_empty(&matched_filter_id_queue) == FALSE)
    {
        guint matched_filter_ids = g_queue_get_length(&matched_filter_id_queue);

        guint private_section_offset = OFFSET_FILTER_IDS_LENGTH
                + SIZEOF_FILTER_IDS_LENGTH;
        guint private_section_length = GST_BUFFER_SIZE(buf) - SIZEOF_PID;
        guint filter_ids_offset = private_section_offset
                + private_section_length;
        guint filter_ids_length = matched_filter_ids * SIZEOF_FILTER_ID;

        matched_filter_data = gst_buffer_new_and_alloc(SIZEOF_PID
                + SIZEOF_PRIVATE_SECTION_OFFSET + SIZEOF_PRIVATE_SECTION_LENGTH
                + SIZEOF_FILTER_IDS_OFFSET + SIZEOF_FILTER_IDS_LENGTH
                + private_section_length + filter_ids_length);

        g_memmove(&GST_BUFFER_DATA(matched_filter_data)[OFFSET_PID],
                &GST_BUFFER_DATA(buf)[OFFSET_PID], SIZEOF_PID);

        GST_WRITE_UINT16_BE(
                &GST_BUFFER_DATA(matched_filter_data)[OFFSET_PRIVATE_SECTION_OFFSET],
                (guint16) private_section_offset);
        GST_WRITE_UINT16_BE(
                &GST_BUFFER_DATA(matched_filter_data)[OFFSET_PRIVATE_SECTION_LENGTH],
                (guint16) private_section_length);
        GST_WRITE_UINT16_BE(
                &GST_BUFFER_DATA(matched_filter_data)[OFFSET_FILTER_IDS_OFFSET],
                (guint16) filter_ids_offset);
        GST_WRITE_UINT16_BE(
                &GST_BUFFER_DATA(matched_filter_data)[OFFSET_FILTER_IDS_LENGTH],
                (guint16) matched_filter_ids);

        g_memmove(
                &GST_BUFFER_DATA(matched_filter_data)[private_section_offset],
                &GST_BUFFER_DATA(buf)[SIZEOF_PID], private_section_length);

        while (g_queue_is_empty(&matched_filter_id_queue) == FALSE)
        {
            guint filter_id = GPOINTER_TO_UINT(g_queue_pop_head(
                    &matched_filter_id_queue));
            GST_WRITE_UINT32_BE(
                    &GST_BUFFER_DATA(matched_filter_data)[filter_ids_offset],
                    filter_id);
            filter_ids_offset += SIZEOF_FILTER_ID;
        }
    }
    else
    {
        GST_DEBUG_OBJECT(filter, "Section %p did not match any filters", buf);
    }
    gst_buffer_unref(buf);
    g_mutex_unlock(filter->filter_lock);

    if (matched_filter_data != NULL)
    {
        GstCaps *pcaps = gst_caps_copy(gst_pad_get_pad_template_caps(
                filter->srcpad));
        gst_buffer_set_caps(matched_filter_data, pcaps);
        gst_caps_unref(pcaps);
        GST_DEBUG_OBJECT(filter, "Pushing matched section %p",
                matched_filter_data);
        ret = gst_pad_push(filter->srcpad, matched_filter_data);
    }

    return ret;
}

/*****************************************************/
/**********                                 **********/
/********** GstSectionFilter IMPLEMENTATION **********/
/**********                                 **********/
/*****************************************************/

static guint32 filter_id_counter = 0;
static GStaticMutex counter_mutex = G_STATIC_MUTEX_INIT;

static guint32 gst_section_filter_new_id(GstSectionFilter *filter)
{
    guint32 new_filter_id = 0;

    g_static_mutex_lock(&counter_mutex);
    filter_id_counter++;
    if (filter_id_counter == 0)
    {
        filter_id_counter++;
    }
    new_filter_id = filter_id_counter;
    g_static_mutex_unlock(&counter_mutex);

    return new_filter_id;
}

static gpointer gst_section_filter_test_thread(
        gpointer user_data_section_filter)
{
    GstSectionFilter *filter = NULL;
    GstISectionFilter *ifilter = NULL;

    guint16 PID = 0x0100;

    guint16 pos_length = 1;
    guint8 pos_mask_array[] =
    { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF };
    guint8 *pos_mask = &pos_mask_array[0];
    guint8 pos_values_array[] =
    { 0x02, 0xB0, 0xB3, 0x65, 0x87 };
    guint8 *pos_values = &pos_values_array[0];

    guint16 neg_length = 0;
    guint8 neg_mask_array[] =
    { 0x00, 0xFF };
    guint8 *neg_mask = &neg_mask_array[0];
    guint8 neg_values_array[] =
    { 0xFF, 0xB1 };
    guint8 *neg_values = &neg_values_array[0];

    guint32 filter_id = 0;

    g_return_val_if_fail(GST_IS_SECTION_FILTER(user_data_section_filter), NULL);
    filter = GST_SECTION_FILTER(user_data_section_filter);
    ifilter = GST_ISECTION_FILTER(filter);

    GST_INFO_OBJECT(filter, "gst_section_filter_test_thread started");

    gst_isection_filter_create(ifilter, PID, pos_length, pos_mask, pos_values,
            neg_length, neg_mask, neg_values, &filter_id);

    GST_INFO_OBJECT(filter, "gst_section_filter_test_thread terminated");

    return NULL;
}

/****************************************************************/
/**********                                            **********/
/********** GstISectionFilter INTERFACE IMPLEMENTATION **********/
/**********                                            **********/
/****************************************************************/

static void gst_isection_filter_create_impl(GstISectionFilter* ifilter,
        guint16 PID, guint16 pos_length, guint8* pos_mask, guint8* pos_values,
        guint16 neg_length, guint8* neg_mask, guint8* neg_values,
        guint32* filter_id)
{
    GstSectionFilter *filter = GST_SECTION_FILTER(ifilter);
    GstSectionFilterItem *item = NULL;
    char printf_buffer[1024];

    *filter_id = gst_section_filter_new_id(filter);

    g_mutex_lock(filter->filter_lock);
    item = gst_section_filter_item_new(*filter_id, PID, pos_length, pos_mask,
            pos_values, neg_length, neg_mask, neg_values);
    filter->filter_item_list = g_slist_prepend(filter->filter_item_list, item);
    g_mutex_unlock(filter->filter_lock);

    GST_DEBUG_OBJECT(filter, "Added   %s", gst_section_filter_item_snprintf(
            item, printf_buffer, 1024));
}

static void gst_isection_filter_cancel_impl(GstISectionFilter* ifilter,
        guint32 filter_id)
{
    GstSectionFilter *filter = GST_SECTION_FILTER(ifilter);
    GSList *list_iterator = NULL;
    GstSectionFilterItem *item = NULL;
    char printf_buffer[1024];

    g_mutex_lock(filter->filter_lock);
    list_iterator = filter->filter_item_list;
    while (list_iterator != NULL)
    {
        GstSectionFilterItem *current_item = GST_SECTION_FILTER_ITEM(
                list_iterator->data);
        if (current_item->filter_id == filter_id)
        {
            item = current_item;
            filter->filter_item_list = g_slist_remove(filter->filter_item_list,
                    item);
            break;
        }
        list_iterator = g_slist_next(list_iterator);
    }
    g_mutex_unlock(filter->filter_lock);

    if (item == NULL)
    {
        GST_WARNING_OBJECT(filter,
                "Unable to cancel filter_id %8u: filter does not exist",
                filter_id);
    }
    else
    {
        GST_DEBUG_OBJECT(filter, "Removed %s",
                gst_section_filter_item_snprintf(item, printf_buffer, 1024));
        gst_mini_object_unref(GST_MINI_OBJECT_CAST(item));
    }
}

static guint16 gst_isection_filter_get_pid_for_filter_impl(
        GstISectionFilter* ifilter, guint32 filter_id)
{
    GstSectionFilter *filter = GST_SECTION_FILTER(ifilter);
    GSList *list_iterator = NULL;
    guint16 PID = INVALID_PID;

    g_mutex_lock(filter->filter_lock);
    list_iterator = filter->filter_item_list;
    while (list_iterator != NULL)
    {
        GstSectionFilterItem *current_item = GST_SECTION_FILTER_ITEM(
                list_iterator->data);
        if (current_item->filter_id == filter_id)
        {
            PID = current_item->PID;
            break;
        }
        list_iterator = g_slist_next(list_iterator);
    }
    g_mutex_unlock(filter->filter_lock);

    return PID;
}

static void gst_isection_filter_interface_init(GstISectionFilterClass * iface)
{
    iface->create = gst_isection_filter_create_impl;
    iface->cancel = gst_isection_filter_cancel_impl;
    iface->get_pid_for_filter = gst_isection_filter_get_pid_for_filter_impl;
}

static gboolean gst_isection_filter_supported(GstSectionFilter *object,
        GType iface_type)
{
    g_assert(iface_type == GST_TYPE_ISECTION_FILTER);
    return TRUE;
}
