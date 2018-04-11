// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

#include <ri_section_filter.h>
#include <ri_section_cache.h>
#include <ri_tuner.h>
#include <ri_log.h>

#include "section_cache.h"
#include "section_filter.h"
#include "gstreamer/interfaces/isectionassembler.h"
#include "gstreamer/interfaces/isectionfilter.h"

#include <gst/gst.h>

#include <stdlib.h>
#include <string.h>

#define RILOG_CATEGORY riSectionFilterCat
log4c_category_t* riSectionFilterCat = NULL;

// Maximum logical filters allowed by each instance of this filtering
// implementation
#define MAX_FILTERS 200

/**
 * This structure defines parameters needed when this inband
 * section filter is setting PAT and PMT filters itself.
 */
typedef struct PAT_PMT_data_s
{
    uint32_t PAT_filter_id;
    uint32_t PMT_filter_id;

} PAT_PMT_data_t;

/**
 * This structure defines the private data available to a
 * section filtering implementation
 */
typedef struct ri_section_filter_data_s
{
    // Section assembler element
    GstElement* section_assembler;

    // Section filter element
    GstElement* section_filter;

    // Section sink element
    GstElement* section_sink;

    // Section data callbacks
    // Key = filter_id, Value = section_data_callback_f
    GHashTable* cb_table;

    // Number of logical filters currently set
    int num_filters;

    GMutex* filter_data_mutex;

    ri_tuner_t* tuner;

    // Data needed when section filter does its own PAT and PMT filtering
    PAT_PMT_data_t* PAT_PMT_data;

    // Callback function when PIDs are discovered with closed filter
    ri_hn_decode_callback_f decode_cb;

    // Data to return in decode callback
    void* decode_data;

} section_filter_data_t;

/**
 * Create a new logical section filter on an in-band pipeline
 */
static ri_error create_filter(ri_section_filter_t* object, uint32_t* filter_id,
        uint16_t pid, uint8_t* pos_mask, uint8_t* pos_value,
        uint16_t pos_length, uint8_t* neg_mask, uint8_t* neg_value,
        uint16_t neg_length, section_data_callback_f section_data_cb)
{
    section_filter_data_t* data;

    RILOG_DEBUG("%s -- Entry, filter = 0x%p, pid = %d\n", __FUNCTION__, object,
            pid);

    // Validate arguments
    if (object == NULL || filter_id == NULL || section_data_cb == NULL
            || (pos_length != 0 && (pos_mask == NULL || pos_value == NULL))
            || (neg_length != 0 && (neg_mask == NULL || neg_value == NULL)))
    {
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_ILLEGAL_ARG;
    }

    data = object->data;

    if (data->num_filters == MAX_FILTERS)
    {
        RILOG_DEBUG("%s -- MAX_FILTERS Reached!\n", __FUNCTION__);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_FILTER_NOT_AVAILABLE;
    }

    g_mutex_lock(data->filter_data_mutex);

    // Create the logical filter in our GStreamer element
    gst_isection_assembler_enable(GST_ISECTION_ASSEMBLER(
            data->section_assembler), pid, TRUE);
    gst_isection_filter_create(GST_ISECTION_FILTER(data->section_filter), pid,
            pos_length, pos_mask, pos_value, neg_length, neg_mask, neg_value,
            filter_id);

    RILOG_DEBUG("%s -- filter_id = %d\n", __FUNCTION__, *filter_id);

    // Increment the number of running filters
    data->num_filters++;

    // Register the callback function in our table
    g_hash_table_insert(data->cb_table, GUINT_TO_POINTER(*filter_id),
            section_data_cb);
    g_mutex_unlock(data->filter_data_mutex);

    if (NULL != data->tuner)
    {
        (void) data->tuner->add_TS_pid(data->tuner, pid);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * -- ri_section_filter function implementation --
 * Cancel the given logical filter
 */
static ri_error cancel_filter(ri_section_filter_t* object, uint32_t filter_id)
{
    section_filter_data_t* data;
    guint16 pid = 0xFFFF;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Validate arguments
    if (object == NULL)
    {
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_ILLEGAL_ARG;
    }

    RILOG_DEBUG("%s -- filter = 0x%p, filter_id = %d\n", __FUNCTION__, object,
            filter_id);

    data = object->data;

    // Cancel the logical section filter in our GStreamer element
    pid = gst_isection_filter_get_pid_for_filter(GST_ISECTION_FILTER(
            data->section_filter), filter_id);
    if (NULL != data->tuner)
    {
        (void) data->tuner->remove_TS_pid(data->tuner, pid);
    }
    gst_isection_assembler_enable(GST_ISECTION_ASSEMBLER(
            data->section_assembler), pid, FALSE);
    gst_isection_filter_cancel(GST_ISECTION_FILTER(data->section_filter),
            filter_id);

    g_mutex_lock(data->filter_data_mutex);

    // Remove the callback function from our hash table
    if (g_hash_table_remove(data->cb_table, GUINT_TO_POINTER(filter_id)))
    {
        // Decrement the number of running filters -- if we actually canceled one
        data->num_filters--;
    }

    g_mutex_unlock(data->filter_data_mutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * Returns the maximum number of logical filters allowed by this
 * filtering implementation
 */
static uint16_t num_allowed_filters(ri_section_filter_t* object)
{
    return MAX_FILTERS;
}

/**
 * This callback is registered to our section sink element and will
 * be notified each time a logical section filter matches a section
 */
static void section_available_cb(GstElement* element, guint filter_id,
        GstBuffer* section_data, ri_section_filter_t* filter)
{
    gpointer hashtable_value;
    section_filter_data_t* data = filter->data;

    RILOG_TRACE("%s -- Entry, filter_id = %d, section_data = 0x%p\n",
            __FUNCTION__, filter_id, section_data);

    g_mutex_lock(data->filter_data_mutex);
    // Look for the callback associated with this filter_id
    hashtable_value = g_hash_table_lookup(data->cb_table, GUINT_TO_POINTER(
            filter_id));
    g_mutex_unlock(data->filter_data_mutex);

    // If we have a callback function, call it.  Otherwise, ignore it --
    // the filter must have been canceled.
    if (hashtable_value != NULL)
    {
        uint32_t section_id;
        section_data_callback_f callback = hashtable_value;

        // Add this section to the section cache
        (void) add_section(ri_get_section_cache(), section_data, &section_id);

        // Notify the callback
        RILOG_DEBUG(
                "%s -- Notify via callback, filter_id = %d, section_data = 0x%p\n",
                __FUNCTION__, filter_id, section_data);
        callback(filter, (uint32_t) section_id, (uint32_t) filter_id,
                GST_BUFFER_DATA(section_data), GST_BUFFER_SIZE(section_data));
    }
    else
    {
        RILOG_WARN(
                "%s -- (filter_id = %d) no callback registered! releasing data!\n",
                __FUNCTION__, filter_id);

        // There is no callback registered to this filter ID, so just
        // go ahead and release the section data
        gst_buffer_unref(section_data);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Create a new in-band section filtering implementation given the
 * the underlying GStreamer elements
 */
ri_section_filter_t* create_section_filter(GstElement* gst_section_assembler,
        GstElement* gst_section_filter, GstElement* gst_section_sink,
        ri_tuner_t* tuner)
{
    ri_section_filter_t* section_filter;
    section_filter_data_t* data;

    riSectionFilterCat = log4c_category_get("RI.SectionFilter");

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Allocate structure data
    section_filter = g_try_malloc0(sizeof(ri_section_filter_t));

    if (NULL == section_filter)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Assign implementation function pointers
    section_filter->create_filter = create_filter;
    section_filter->cancel_filter = cancel_filter;
    section_filter->num_allowed_filters = num_allowed_filters;

    // Allocate private data
    data = g_try_malloc0(sizeof(section_filter_data_t));

    if (NULL == data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Assign elements into private data structure
    data->section_assembler = gst_section_assembler;
    data->section_filter = gst_section_filter;
    data->section_sink = gst_section_sink;

    // Create the section data callback hash table
    data->cb_table = g_hash_table_new(NULL, NULL);
    data->filter_data_mutex = g_mutex_new();

    data->num_filters = 0;
    data->tuner = tuner;
    section_filter->data = data;

    // Register our section sink signal callback.  Pass the RI filter
    // implementation as user data to be returned on every callback
    (void) g_signal_connect(gst_section_sink, "section_available", G_CALLBACK(
            section_available_cb), section_filter);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return section_filter;
}
