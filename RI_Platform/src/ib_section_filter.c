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
#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>

#include "gst_utils.h"
#include "section_cache.h"
#include "ib_section_filter.h"
#include "gstreamer/interfaces/isectionassembler.h"
#include "gstreamer/interfaces/isectionfilter.h"
#include "platform.h"

#include <gst/gst.h>
#include <glib/gprintf.h>

#include <stdlib.h>
#include <string.h>

#define RILOG_CATEGORY riIbSectionFilterCat
log4c_category_t* riIbSectionFilterCat = NULL;

#define CHECK_OK_OBJECT(o,x,m) if (!(x)) { GST_ERROR_OBJECT((o), (m)); }

// Maximum logical filters allowed by each instance of this filtering
// implementation
#define MAX_FILTERS 200

// Maximum logical filter bins allowed by each instance of this filtering
// implementation which maps to section assemblers
#define MAX_BINS 32

/**
 * This structure defines an individual section filter and associated
 * sink.  It contains a reference to each of the possible sink types.
 * The actual sink type that will be used will be non-null, other
 * sink type elements will be null.
 */
typedef struct filter_bin_data_s
{
    gboolean is_active;
    int filter_bin_id;
    uint32_t filter_id;

    GstElement* filter_bin;

    GstElement* section_filter;
    GstElement* section_sink;

    GstPad* filter_tee_src_pad;

} filter_bin_data_t;

/**
 * This structure defines an individual filtering bin associated
 * with a single PID filter
 */
typedef struct sf_bin_data_s
{
    gboolean is_active;
    int bin_id;
    uint16_t pid;
    uint16_t invalid_pid;

    // Filter bins keyed by filter id
    // Key = filter_id, Value = filter_bin_data_t
    GHashTable* filter_table;

    GstElement* sf_bin;

    GstElement* pid_filter;
    GstElement* section_assembler;
    GstElement* sf_bin_tee;

    GstPad* tee_src_pad;

} sf_bin_data_t;

/**
 * This structure defines parameters needed when this inband
 * section filter is setting PAT and PMT filters itself.
 */
typedef struct closed_data_s
{
    uint32_t pat_filter_id;
    uint32_t pmt_filter_id;

} closed_data_t;

/**
 * This structure defines the private data available to a
 * section filtering implementation
 */
typedef struct ib_section_filter_data_s
{
    // Indicates if this ib section filter is closed,
    // meaning it filters for PAT & PMT
    gboolean is_closed;

    // Number of logical filters currently set
    int cur_bin_cnt;

    // Number of logical filters currently set
    int cur_filter_cnt;

    // Maximum number of section filtering bins allowed
    uint8_t max_sf_bins;

    // Maximum number of section filter output bins allowed
    uint8_t max_filters;

    // Table of pids, keyed using filter id
    GHashTable* pid_table;

    // Table of section filter bins, keyed using pid
    GHashTable* bin_table;

    // Key = filter_id, Value = filter_bin_data_t
    GHashTable* filter_bin_table;

    // Section data callbacks
    // Key = filter_id, Value = section_data_callback_f
    GHashTable* cb_table;

    // Overall bin for all inband section filtering elements
    GstElement* bin;
    GstElement* queue;
    GstElement* transportsync; // Preprocessor for section leg of decoder
    GstElement* tee;

    GMutex* filter_data_mutex;

    ri_tuner_t* tuner;

    // Data needed when section filter does its own PAT and PMT filtering
    closed_data_t* closed_data;

    // Callback function when PIDs are discovered with closed filter
    ri_hn_decode_callback_f decode_cb;

    // Data to return in decode callback
    void* decode_data;

} ib_section_filter_data_t;

// Local methods
//
static void create_ib_section_filter_bin(ri_section_filter_t* section_filter);

static void create_sf_bin(ri_section_filter_t* section_filter, int bin_id);

static void create_sf_bin_elements(ri_section_filter_t* section_filter,
        sf_bin_data_t* filter_data);

static sf_bin_data_t* assign_available_bin(ri_section_filter_t* section_filter,
        uint16_t pid);

static void set_element_string(GstElement *element, const char * target,
        const char *value);

static void sf_bin_modify_link(ib_section_filter_data_t* data,
        sf_bin_data_t* filter_data, gboolean attach);

static void detach_unused_bin(ib_section_filter_data_t* data,
        sf_bin_data_t* sf_bin_data);

static filter_bin_data_t* create_filter_bin(
        ri_section_filter_t* section_filter, int filter_bin_id);

static void create_filter_bin_elements(ri_section_filter_t* section_filter,
        filter_bin_data_t* output_data);

static filter_bin_data_t* assign_available_filter(
        ri_section_filter_t* section_filter, sf_bin_data_t* sf_bin_data);

static void filter_bin_modify_link(sf_bin_data_t* filter_data,
        filter_bin_data_t* output_data, gboolean attach);

static void update_sf_filter_table(sf_bin_data_t* filter_data,
        filter_bin_data_t* output_data, uint32_t filter_id);

static void setup_for_closed_inband_section_filter(
        ri_section_filter_t* section_filter);

static void pat_data_cb(ri_section_filter_t* filter, uint32_t sectionID,
        uint32_t filterID, uint8_t* sectionData, uint16_t sectionLength);

static void pmt_data_cb(ri_section_filter_t* filter, uint32_t sectionID,
        uint32_t filterID, uint8_t* sectionData, uint16_t sectionLength);

static void extract_pmt_pid(uint8_t* sectionData, uint16_t sectionLength,
        uint16_t* pmt_pid);

static void extract_video_pid(uint8_t* data, uint16_t length,
        uint16_t* video_pid);

static uint8_t* parse_uint16(uint8_t* inBuf, uint16_t* out_int);

/**
 * Create a new logical section filter on an in-band pipeline
 */
static ri_error create_filter(ri_section_filter_t* object, uint32_t* filter_id,
        uint16_t pid, uint8_t* pos_mask, uint8_t* pos_value,
        uint16_t pos_length, uint8_t* neg_mask, uint8_t* neg_value,
        uint16_t neg_length, section_data_callback_f section_data_cb)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_TRACE("%s -- Entry, pid = %d\n", __FUNCTION__, pid);

    ib_section_filter_data_t* data;

    // Validate arguments
    if (object == NULL || filter_id == NULL || section_data_cb == NULL
            || (pos_length != 0 && (pos_mask == NULL || pos_value == NULL))
            || (neg_length != 0 && (neg_mask == NULL || neg_value == NULL)))
    {
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_ILLEGAL_ARG;
    }

    data = object->data;

    // Determine if there is already a section filter associated with this pid
    g_mutex_lock(data->filter_data_mutex);
    sf_bin_data_t* sf_bin_data = g_hash_table_lookup(data->bin_table,
            GUINT_TO_POINTER((guint) pid));
    g_mutex_unlock(data->filter_data_mutex);

    // If no filtering bin associated with this PID, assign one
    if (NULL == sf_bin_data)
    {
        // No bin associated with PID, check to see if at max
        if (data->cur_bin_cnt >= data->max_sf_bins)
        {
            RILOG_WARN("%s -- MAX Assemblers Reached!", __FUNCTION__);
            RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
            return RI_ERROR_FILTER_NOT_AVAILABLE;
        }

        // Get next available bin
        sf_bin_data = assign_available_bin(object, pid);

        RILOG_DEBUG("%s -- assigned bin = %d for pid %d\n", __FUNCTION__,
                sf_bin_data->bin_id, sf_bin_data->pid);
    }

    // Make sure there is a filter available
    if (data->cur_filter_cnt >= data->max_filters)
    {
        RILOG_WARN("%s -- MAX_FILTERS Reached!", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_FILTER_NOT_AVAILABLE;
    }

    // Get next available filter bin
    filter_bin_data_t* filter_bin_data = assign_available_filter(object,
            sf_bin_data);
    if (NULL == filter_bin_data)
    {
        RILOG_ERROR("%s -- unable to get filter data for bin id = %d\n",
                __FUNCTION__, sf_bin_data->bin_id);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_FILTER_NOT_AVAILABLE;
    }

    // Create the logical filter in our GStreamer element
    gst_isection_assembler_enable(GST_ISECTION_ASSEMBLER(
            sf_bin_data->section_assembler), sf_bin_data->pid, TRUE);

    gst_isection_filter_create(GST_ISECTION_FILTER(
            filter_bin_data->section_filter), sf_bin_data->pid, pos_length,
            pos_mask, pos_value, neg_length, neg_mask, neg_value, filter_id);

    RILOG_DEBUG("%s -- inserting new filter_id = %d for pid %d\n",
            __FUNCTION__, *filter_id, sf_bin_data->pid);

    g_mutex_lock(data->filter_data_mutex);

    // Register the callback function in our table
    g_hash_table_insert(data->cb_table, GUINT_TO_POINTER(*filter_id),
            section_data_cb);

    // Add the pid and filter id to pid table
    g_hash_table_insert(data->pid_table, GUINT_TO_POINTER(*filter_id),
            (uint16_t*) &sf_bin_data->pid);

    // Update filter table for sf bin
    update_sf_filter_table(sf_bin_data, filter_bin_data, *filter_id);

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
    ib_section_filter_data_t* data;
    guint16 pid = 0xFFFF;

    RILOG_TRACE("%s -- Entry, filter_id = %d\n", __FUNCTION__, filter_id);

    // Validate arguments
    if (object == NULL)
    {
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_ILLEGAL_ARG;
    }

    data = object->data;

    // Find which section filter bin is associated with this filter
    // Need to find the PID to get the bin so look up the filter id in PID table
    g_mutex_lock(data->filter_data_mutex);
    uint16_t* filter_pid = g_hash_table_lookup(data->pid_table,
            GUINT_TO_POINTER(filter_id));
    g_mutex_unlock(data->filter_data_mutex);

    if (NULL == filter_pid)
    {
        RILOG_ERROR(
                "%s -- unable to find pid associated with filter %d in pid table\n",
                __FUNCTION__, filter_id);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_ILLEGAL_ARG;
    }

    // Find the sf bin associated with this PID
    g_mutex_lock(data->filter_data_mutex);
    sf_bin_data_t* sf_bin_data = g_hash_table_lookup(data->bin_table,
            GUINT_TO_POINTER((guint) * filter_pid));
    g_mutex_unlock(data->filter_data_mutex);

    if (NULL == sf_bin_data)
    {
        RILOG_ERROR(
                "%s -- unable to find bin associated with pid %d in bin table\n",
                __FUNCTION__, (uint16_t) * filter_pid);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_ILLEGAL_ARG;
    }

    // Find the filter associated with this filter id
    g_mutex_lock(data->filter_data_mutex);

    filter_bin_data_t* filter_bin_data = g_hash_table_lookup(
            sf_bin_data->filter_table, GUINT_TO_POINTER((guint) filter_id));
    g_mutex_unlock(data->filter_data_mutex);

    if (NULL == filter_bin_data)
    {
        RILOG_ERROR(
                "%s -- unable to find filter associated with id %d in bin table %d\n",
                __FUNCTION__, (uint16_t) filter_id, sf_bin_data->bin_id);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_ILLEGAL_ARG;
    }

    // Cancel the logical section filter in our GStreamer element
    pid = gst_isection_filter_get_pid_for_filter(GST_ISECTION_FILTER(
            filter_bin_data->section_filter), filter_id);

    if (NULL != data->tuner)
    {
        (void) data->tuner->remove_TS_pid(data->tuner, pid);
    }

    gst_isection_assembler_enable(GST_ISECTION_ASSEMBLER(
            sf_bin_data->section_assembler), pid, FALSE);

    gst_isection_filter_cancel(GST_ISECTION_FILTER(
            filter_bin_data->section_filter), filter_id);

    g_mutex_lock(data->filter_data_mutex);

    // Remove the callback function from our hash table
    if (g_hash_table_remove(data->cb_table, GUINT_TO_POINTER(filter_id)))
    {
        RILOG_DEBUG("%s -- removing filter %d associated with pid %d\n",
                __FUNCTION__, filter_id, pid);
    }

    // Unlink this filter from sf bin
    filter_bin_modify_link(sf_bin_data, filter_bin_data, FALSE);

    // Set this filter as inactive
    filter_bin_data->is_active = FALSE;

    // Remove this filter from its table of filters
    if (g_hash_table_remove(sf_bin_data->filter_table, GUINT_TO_POINTER(
            filter_id)))
    {
        RILOG_DEBUG("%s -- removing filter %d from bin %d\n", __FUNCTION__,
                filter_id, sf_bin_data->bin_id);

        // Decrement the number of running filters -- if we actually canceled one
        data->cur_filter_cnt--;
        RILOG_DEBUG("%s -- cur_filter_cnt = %d\n", __FUNCTION__,
                data->cur_filter_cnt);
    }

    // Set the section filter bin to inactive if filter table is empty
    if (g_hash_table_size(sf_bin_data->filter_table) <= 0)
    {
        // Set the inactive flag
        sf_bin_data->is_active = FALSE;
        data->cur_bin_cnt--;
        RILOG_DEBUG("%s -- cur_bin_cnt = %d\n", __FUNCTION__, data->cur_bin_cnt);

        // Remove the bin from our pid hash table
        (void) g_hash_table_remove(data->pid_table, GUINT_TO_POINTER(filter_id));

        // Remove this filter from bin table
        (void) g_hash_table_remove(data->bin_table, GUINT_TO_POINTER((guint)
                * filter_pid));

        // Re-insert with invalid PID value
        g_hash_table_insert(data->bin_table, GUINT_TO_POINTER(
                (guint) sf_bin_data->invalid_pid), sf_bin_data);

        // Detach the bin from the pipeline if there is at least one other
        // bin still connected
        detach_unused_bin(data, sf_bin_data);
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
 * Returns the bin associated with this section filter so it can be added to a
 * pipeline.
 *
 * @param   ib_section_filter    retrieve bin from this section filter
 *
 * @return  GstElement which is the bin which contain all elements of this section filter
 */
GstElement* get_ib_section_filter_bin(ri_section_filter_t* ib_section_filter)
{
    return ((ib_section_filter_data_t*) ib_section_filter->data)->bin;
}

/**
 * This callback is registered to our section sink element and will
 * be notified each time a logical section filter matches a section
 */
static void section_available_cb(GstElement* element, guint filter_id,
        GstBuffer* section_data, ri_section_filter_t* filter)
{
    gpointer hashtable_value;
    ib_section_filter_data_t* data = filter->data;

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
        callback(filter, (uint32_t) section_id, (uint32_t) filter_id,
                GST_BUFFER_DATA(section_data), GST_BUFFER_SIZE(section_data));
    }
    else
    {
        RILOG_WARN(
                "%s -- (filter_id = %d)-- no callback registered! releasing data!\n",
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
ri_section_filter_t* create_ib_section_filter(gboolean is_closed,
        ri_tuner_t* tuner)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Determine the max number of section filter bins which is equal to the max
    // number of section assemblers per inband section filter
    char* maxStr = ricfg_getValue("RIPlatform",
            "RI.Platform.maxSectionAssemblers");
    int maxSfBins = 0;
    if (maxStr == NULL || (maxSfBins = atoi(maxStr)) == 0)
    {
        RILOG_ERROR(
                "%s -- Invalid number of section assemblers specified! %s\n",
                __FUNCTION__, maxStr);
        maxSfBins = MAX_BINS;
    }
    RILOG_INFO("%s -- Num max section assemblers/bins is %d\n", __FUNCTION__,
            maxSfBins);

    // Determine the max number of output bins which is equal to the max
    // number of section filters per inband section filter
    maxStr = ricfg_getValue("RIPlatform", "RI.Platform.maxSectionFilters");
    int maxOutputBins = 0;
    if (maxStr == NULL || (maxOutputBins = atoi(maxStr)) == 0)
    {
        RILOG_ERROR("%s -- Invalid number of section filters specified! %s\n",
                __FUNCTION__, maxStr);
        maxOutputBins = MAX_FILTERS;
    }
    RILOG_INFO("%s -- Num max section filters/output bins is %d\n",
            __FUNCTION__, maxOutputBins);

    ri_section_filter_t* section_filter;

    // Allocate structure data
    section_filter = g_try_malloc0(sizeof(ri_section_filter_t));
    if (NULL != section_filter)
    {
        memset(section_filter, 0, sizeof(ri_section_filter_t));
    }
    else
    {
        RILOG_ERROR("%s -- Unable to allocate memory for section filter\n",
                __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return NULL;
    }

    // Assign implementation function pointers
    section_filter->create_filter = create_filter;
    section_filter->cancel_filter = cancel_filter;
    section_filter->num_allowed_filters = num_allowed_filters;

    // Allocate private data
    section_filter->data = g_try_malloc0(sizeof(ib_section_filter_data_t));
    if (NULL != section_filter->data)
    {
        memset(section_filter->data, 0, sizeof(ib_section_filter_data_t));
    }
    else
    {
        RILOG_ERROR(
                "%s -- Unable to allocate memory for section filter data\n",
                __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return NULL;
    }
    ib_section_filter_data_t* data = section_filter->data;

    // store the tuner reference for TS pid filtering on live pipelines
    data->tuner = tuner;

    // Create the section data callback hash table
    data->bin_table = g_hash_table_new(NULL, NULL);

    // Create table to store list of PIDs in use by filter ids
    data->pid_table = g_hash_table_new(NULL, NULL);

    // Create table to store references to the output bins, keyed by bin id
    data->filter_bin_table = g_hash_table_new(NULL, NULL);

    // Set the number of bins
    data->cur_filter_cnt = 0;
    data->cur_bin_cnt = 0;
    data->max_sf_bins = maxSfBins;
    data->max_filters = maxOutputBins;
    data->is_closed = is_closed;

    // Create the section data callback hash table
    data->cb_table = g_hash_table_new(NULL, NULL);
    data->filter_data_mutex = g_mutex_new();

    // Create overall bin
    create_ib_section_filter_bin(section_filter);

    // Create max number of output bins
    int i = 0;
    for (i = 0; i < data->max_filters; i++)
    {
        (void) create_filter_bin(section_filter, (i + 1));
    }

    // Create max number of bins
    for (i = 0; i < data->max_sf_bins; i++)
    {
        create_sf_bin(section_filter, (i + 1));
    }

    // Connect one of the section bins so there is sink present so data can flow
    GHashTableIter iter;
    gpointer key;
    gpointer value;
    g_hash_table_iter_init(&iter, data->bin_table);
    while (g_hash_table_iter_next(&iter, &key, &value))
    {
        sf_bin_data_t* sf_bin_data = (sf_bin_data_t*) value;

        // Assign an available filter
        filter_bin_data_t* filter_data = assign_available_filter(
                section_filter, sf_bin_data);

        // Use 0 instead of filter id
        update_sf_filter_table(sf_bin_data, filter_data, 0);

        RILOG_DEBUG("%s -- calling attach for bin %d\n", __FUNCTION__,
                sf_bin_data->bin_id);
        sf_bin_modify_link(data, (sf_bin_data_t*) value, TRUE);
        break;
    }

    // Perform additional setup for closed inband section filter
    if (data->is_closed)
    {
        setup_for_closed_inband_section_filter(section_filter);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return section_filter;
}

/**
 * Set the supplied callback function as function to call when PIDs are discovered.
 *
 * @param   section_filter associated filter
 * @param   decode_cb      function to call when PIDs are discovered
 */
void set_ib_section_filter_decode_callback(ri_section_filter_t* section_filter,
        ri_hn_decode_callback_f decode_cb, void* decode_data)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_DEBUG("%s -- Entry\n", __FUNCTION__);

    ib_section_filter_data_t* data = section_filter->data;

    data->decode_cb = decode_cb;
    data->decode_data = decode_data;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Creates the section sink and filtering elements for the supplied pipeline.
 *
 * @param   data    store a reference to the created section sink within this data
 */
static void create_ib_section_filter_bin(ri_section_filter_t* section_filter)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_DEBUG("%s -- Entry\n", __FUNCTION__);

    ib_section_filter_data_t* data = section_filter->data;

    data->queue = gst_load_element("queue", "ibsf_q_main");
    data->transportsync = gst_load_element("transportsync", "tsynchronizer");
    data->tee = gst_load_element("tee", "ibsftee");

    // Section filtering bin part of pipeline
    data->bin = gst_bin_new("bin");
    gst_bin_add_many(GST_BIN(data->bin), data->queue, data->transportsync,
            data->tee, NULL);

    // Ghost the input pad to the overall bin
    GstPad* bin_sink_pad = gst_element_get_static_pad(data->queue, "sink");
    CHECK_OK_OBJECT(data->bin,
            gst_element_add_pad(data->bin, gst_ghost_pad_new("sink", bin_sink_pad)),
            "Adding ghost pad to overall bin failed");

    // Link the bin elements
    CHECK_OK_OBJECT(data->bin,
            gst_element_link_many(data->queue,
                    data->transportsync,
                    data->tee,
                    NULL),
            "Linking overall elements failed");

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Allocates and initializes data structure for section filter bin.
 * Also initiates creation and connection of gstreamer elements.
 *
 * @param   section_filter overall section filtering object
 * @param   pid which this section bin is associated with
 */
static void create_sf_bin(ri_section_filter_t* section_filter, int bin_id)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    ib_section_filter_data_t* data = section_filter->data;

    // Create data structure to store info related to this bin
    sf_bin_data_t* sf_bin_data = g_try_malloc0(sizeof(sf_bin_data_t));
    if (NULL != sf_bin_data)
    {
        memset(sf_bin_data, 0, sizeof(sf_bin_data_t));
    }
    else
    {
        RILOG_ERROR("%s() -- Unable to allocate memory for section filter\n",
                __FUNCTION__);
        return;
    }

    sf_bin_data->is_active = FALSE;
    sf_bin_data->bin_id = bin_id;

    // Create table to store references to the filter bins, keyed by filter id
    sf_bin_data->filter_table = g_hash_table_new(NULL, NULL);

    // Create the gstreamer elments and bin
    create_sf_bin_elements(section_filter, sf_bin_data);

    // Insert this section bin filter into bin table storing using key PID
    g_mutex_lock(data->filter_data_mutex);

    // Add the bin using PID as key to bin table using invalid PID as initial value
    sf_bin_data->invalid_pid = -1 - bin_id;
    g_hash_table_insert(data->bin_table, GUINT_TO_POINTER(
            (guint) sf_bin_data->invalid_pid), sf_bin_data);

    g_mutex_unlock(data->filter_data_mutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Creates the section sink and filtering elements for a section bin in overall
 * inband section filter.
 *
 * @param   section_filter reference to overall section filter
 * @param   filter_data    data specific to section filter bin to create
 */
static void create_sf_bin_elements(ri_section_filter_t* section_filter,
        sf_bin_data_t* filter_data)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Get the data associated with section filter
    ib_section_filter_data_t* data = section_filter->data;

    // Create the gst elements for this section bin
    filter_data->pid_filter = gst_load_element("pidfilter", "pidfilter");
    filter_data->section_assembler = gst_load_element("sectionassembler",
            "inbandsectionassembler");
    filter_data->sf_bin_tee = gst_load_element("tee", "sftee");

    // Create this section bin
    char bin_name[64];
    sprintf(bin_name, "section_filter_bin_%d", filter_data->bin_id);
    filter_data->sf_bin = gst_bin_new(bin_name);
    gst_bin_add_many(GST_BIN(filter_data->sf_bin), filter_data->pid_filter,
            filter_data->section_assembler, filter_data->sf_bin_tee, NULL);

    // Link the bin elements
    CHECK_OK_OBJECT(filter_data->sf_bin,
            gst_element_link_many(
                    filter_data->pid_filter,
                    filter_data->section_assembler,
                    filter_data->sf_bin_tee,
                    NULL),
            "Linking section bin elements failed");

    // Ghost the input pad of the section filter bin
    GstPad* bin_sink_pad = gst_element_get_static_pad(filter_data->pid_filter,
            "sink");
    CHECK_OK_OBJECT(filter_data->sf_bin,
            gst_element_add_pad(filter_data->sf_bin, gst_ghost_pad_new("sink", bin_sink_pad)),
            "Adding ghost pad to bin failed");

    // Add this new bin to the overall bin
    gst_bin_add_many(GST_BIN(data->bin), filter_data->sf_bin, NULL);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Connects or disconnects the supplied section filter bin to the tee
 * in the overall bin.
 *
 * @param   data        overall section filter info
 * @param   filter_data data for specific section filter bin to connect
 * @param   attach      if true, attach the supplied bin, otherwise disconnect
 */
static void sf_bin_modify_link(ib_section_filter_data_t* data,
        sf_bin_data_t* filter_data, gboolean attach)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_DEBUG("%s -- Entry, bin = %d, attach = %d\n", __FUNCTION__,
            filter_data->bin_id, attach);

    // Get the section filter bin sink pad
    GstPad* sf_bin_sink_pad = gst_element_get_static_pad(filter_data->sf_bin,
            "sink");
    if (NULL == sf_bin_sink_pad)
    {
        RILOG_ERROR("%s -- unable to get section filter bin sink pad\n",
                __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    // Get current state of link
    gboolean isLinked = gst_pad_is_linked(sf_bin_sink_pad);
    if (isLinked && attach)
    {
        RILOG_DEBUG("%s -- tee and sf bin already linked\n", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }
    if (!isLinked && !attach)
    {
        RILOG_DEBUG("%s -- tee and sf bin already unlinked\n", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    if (attach)
    {
        // Request a src pad from the tee
        filter_data->tee_src_pad = gst_element_get_request_pad(data->tee,
                "src%d");

        // Link input bin src pad to decode bin sink
        if (GST_PAD_LINK_OK != gst_pad_link(filter_data->tee_src_pad,
                sf_bin_sink_pad))
        {
            RILOG_ERROR("%s -- problems linking tee and sf bin\n", __FUNCTION__);
        }
        else
        {
            RILOG_DEBUG("%s -- successfully linked tee and sf bin\n",
                    __FUNCTION__);
        }
    }
    else // detach
    {
        // Unlink input bin src pad to decode bin sink
        if (TRUE != gst_pad_unlink(filter_data->tee_src_pad, sf_bin_sink_pad))
        {
            RILOG_ERROR("%s -- problems unlinking tee and sf bin\n",
                    __FUNCTION__);
        }
        else
        {
            RILOG_DEBUG("%s -- successfully unlinked tee and sf bin\n",
                    __FUNCTION__);
        }

        // Free our references
        gst_element_release_request_pad(data->tee, filter_data->tee_src_pad);
        gst_object_unref(GST_OBJECT(filter_data->tee_src_pad));
        filter_data->tee_src_pad = NULL;
    }

    // Free our references
    gst_object_unref(GST_OBJECT(sf_bin_sink_pad));

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Look through table of section filter bins and find the first one
 * that is not active, associated it with the supplied PID,
 * mark it as active and return it
 *
 * @param   section_filter    overall section filtering object
 *
 * @return  available section bin, or NULL if none available
 */
static sf_bin_data_t* assign_available_bin(ri_section_filter_t* section_filter,
        uint16_t pid)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    // Get the data associated with section filter
    ib_section_filter_data_t* data = section_filter->data;

    // Look for the first bin which is not active
    GHashTableIter iter;
    gpointer key;
    gpointer value;
    sf_bin_data_t* sf_bin_data = NULL;
    sf_bin_data_t* available_bin_data = NULL;
    int i = 0;
    gchar pidStr[8];

    g_mutex_lock(data->filter_data_mutex);

    g_hash_table_iter_init(&iter, data->bin_table);
    while (g_hash_table_iter_next(&iter, &key, &value))
    {
        i++;
        sf_bin_data = value;
        RILOG_TRACE(
                "%s -- looking at bin %d, current key/pid %u, active? %d\n",
                __FUNCTION__, i, GPOINTER_TO_UINT(key), sf_bin_data->is_active);

        if (FALSE == sf_bin_data->is_active)
        {
            // Remove this value from the table
            g_hash_table_iter_remove(&iter);

            // Set flag as active and re-insert using supplied PID as key
            available_bin_data = sf_bin_data;
            available_bin_data->is_active = TRUE;
            data->cur_bin_cnt++;
            RILOG_DEBUG("%s -- cur_bin_cnt = %d\n", __FUNCTION__,
                    data->cur_bin_cnt);
            available_bin_data->pid = pid;

            // Connect the bin to the pipeline
            RILOG_DEBUG("%s -- calling attach for bin %d\n", __FUNCTION__,
                    available_bin_data->bin_id);
            sf_bin_modify_link(data, available_bin_data, TRUE);

            // Set the PID filter to the specified PID value
            (void) g_sprintf(pidStr, "0x%4.4X", (pid & 0x1FFF));
            set_element_string(available_bin_data->pid_filter, "pidlist",
                    pidStr);

            g_hash_table_insert(data->bin_table, GUINT_TO_POINTER(
                    (guint) available_bin_data->pid), available_bin_data);

            RILOG_DEBUG("%s -- associated bin %d with pid %d\n", __FUNCTION__,
                    i, available_bin_data->pid);
            break;
        }
    }
    g_mutex_unlock(data->filter_data_mutex);

    if (NULL == available_bin_data)
    {
        RILOG_ERROR("%s -- unable to find bin to associate with pid %d\n",
                __FUNCTION__, pid);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return available_bin_data;
}

/**
 * Detach the supplied bin from pipeline if there is still at least
 * one other bin still attached.  One bin needs to be attached to ensure
 * that data will continue to flow through the tee.
 *
 * @param   sf_bin_data bin which has just been marked as unused
 */
static void detach_unused_bin(ib_section_filter_data_t* data,
        sf_bin_data_t* sf_bin_data)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    // Determine if there is still a bin that is marked as active
    gboolean active = FALSE;

    GHashTableIter iter;
    gpointer key;
    gpointer value;
    g_hash_table_iter_init(&iter, data->bin_table);
    while (g_hash_table_iter_next(&iter, &key, &value))
    {
        sf_bin_data_t* hashed_sf_bin_data = (sf_bin_data_t*) value;
        if (hashed_sf_bin_data->is_active)
        {
            active = TRUE;
            break;
        }
    }

    // If at least one bin is still linked, unlink this bin
    if (active)
    {
        sf_bin_modify_link(data, sf_bin_data, FALSE);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Allocates and initializes data structure for section filter output bin.
 * Also initiates creation and connection of gstreamer elements.
 *
 * @param   section_filter overall section filtering object
 * @param   id which this section filter output bin is associated with
 */
static filter_bin_data_t* create_filter_bin(
        ri_section_filter_t* section_filter, int filter_bin_id)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Get the data associated with section filter
    ib_section_filter_data_t* data = section_filter->data;

    // Allocate memory for the output bin data
    filter_bin_data_t* filter_bin_data = g_try_malloc0(
            sizeof(filter_bin_data_t));
    if (NULL != filter_bin_data)
    {
        memset(filter_bin_data, 0, sizeof(filter_bin_data_t));
    }
    else
    {
        RILOG_ERROR(
                "%s -- Unable to allocate memory for section filter output bin\n",
                __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return NULL;
    }

    filter_bin_data->is_active = FALSE;
    filter_bin_data->filter_bin_id = filter_bin_id;
    filter_bin_data->filter_id = 0;

    create_filter_bin_elements(section_filter, filter_bin_data);

    // Add the bin using id as key to bin table
    g_hash_table_insert(data->filter_bin_table, GUINT_TO_POINTER((guint)(
            filter_bin_id)), filter_bin_data);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return filter_bin_data;
}

/**
 * Creates the section sink and filtering elements for a section bin in overall
 * inband section filter.
 *
 * @param   section_filter reference to overall section filter
 * @param   filter_data    data specific to section filter bin to create
 */
static void create_filter_bin_elements(ri_section_filter_t* section_filter,
        filter_bin_data_t* output_data)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Get the data associated with section filter
    //ib_section_filter_data_t* data = section_filter->data;

    output_data->section_filter = gst_load_element("sectionfilter",
            "inbandsectionfilter");
    output_data->section_sink = gst_load_element("sectionsink", "sectionsink");

    // Set the section sink to async which is needed for live sources
    g_object_set(G_OBJECT(output_data->section_sink), "async", FALSE, NULL);

    // Register our section sink signal callback.  Pass the RI filter
    // implementation as user data to be returned on every callback
    (void) g_signal_connect(output_data->section_sink, "section_available",
            G_CALLBACK(section_available_cb), section_filter);

    // Create the bin for this filter bin
    char bin_name[64];
    sprintf(bin_name, "filter_bin_%d", output_data->filter_bin_id);
    output_data->filter_bin = gst_bin_new(bin_name);
    gst_bin_add_many(GST_BIN(output_data->filter_bin),
            output_data->section_filter, output_data->section_sink, NULL);

    // Link the output bin elements
    CHECK_OK_OBJECT(output_data->filter_bin,
            gst_element_link_many(
                    output_data->section_filter,
                    output_data->section_sink,
                    NULL),
            "Linking output bin elements failed");

    // Ghost the input pad of the output bin
    GstPad* filter_bin_sink_pad = gst_element_get_static_pad(
            output_data->section_filter, "sink");

    CHECK_OK_OBJECT(output_data->filter_bin,
            gst_element_add_pad(output_data->filter_bin,
                    gst_ghost_pad_new("sink", filter_bin_sink_pad)),
            "Adding ghost pad to output bin failed");

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Look through table of section filter bins and find the first one
 * that is not active, associated it with the supplied PID,
 * mark it as active and return it
 *
 * @param   section_filter    overall section filtering object
 *
 * @return  available section bin, or NULL if none available
 */
static filter_bin_data_t* assign_available_filter(
        ri_section_filter_t* section_filter, sf_bin_data_t* sf_bin_data)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    // Get the data associated with section filter
    ib_section_filter_data_t* data = section_filter->data;

    // Look for the first filter which is not active
    GHashTableIter iter;
    gpointer key;
    gpointer value;
    filter_bin_data_t* filter_data = NULL;
    filter_bin_data_t* available_data = NULL;

    g_mutex_lock(data->filter_data_mutex);

    g_hash_table_iter_init(&iter, data->filter_bin_table);
    while (g_hash_table_iter_next(&iter, &key, &value))
    {
        filter_data = value;
        RILOG_TRACE("%s -- looking at filter %d, active? %d\n", __FUNCTION__,
                filter_data->filter_bin_id, sf_bin_data->is_active);

        if (FALSE == filter_data->is_active)
        {
            // Set flag as active
            available_data = filter_data;
            available_data->is_active = TRUE;
            data->cur_filter_cnt++;
            RILOG_DEBUG("%s -- cur_filter_cnt = %d\n", __FUNCTION__,
                    data->cur_filter_cnt);

            // Connect the filter to the sf bin
            RILOG_DEBUG("%s -- calling attach for bin %d\n", __FUNCTION__,
                    sf_bin_data->bin_id);
            filter_bin_modify_link(sf_bin_data, available_data, TRUE);

            break;
        }
    }
    g_mutex_unlock(data->filter_data_mutex);

    if (NULL == available_data)
    {
        RILOG_ERROR("%s -- unable to find filter to associate with bin %d\n",
                __FUNCTION__, sf_bin_data->bin_id);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return available_data;
}

/**
 * Updates the sf bin data with supplied filter info since the
 * filter id is only available after the filter is created.
 *
 * @param   sf_bin_data    sf bin which has new filter
 * @param   filter_data    new filter data
 * @param   filter_id      id assigned to new filter
 */
static void update_sf_filter_table(sf_bin_data_t* sf_bin_data,
        filter_bin_data_t* filter_data, uint32_t filter_id)
{
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    // Set the filter id to the supplied value
    filter_data->filter_id = filter_id;

    // Insert this filter into sf bin filter table
    g_hash_table_insert(sf_bin_data->filter_table, GUINT_TO_POINTER(
            (guint) filter_data->filter_id), filter_data);

    RILOG_DEBUG("%s -- associated filter bin %d with sf bin %d\n",
            __FUNCTION__, filter_data->filter_bin_id, sf_bin_data->bin_id);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Connects or disconnects the supplied section filter bin to the tee
 * in the overall bin.
 *
 * @param   data        overall section filter info
 * @param   filter_data data for specific section filter bin to connect
 * @param   attach      if true, attach the supplied bin, otherwise disconnect
 */

static void filter_bin_modify_link(sf_bin_data_t* sf_bin_data,
        filter_bin_data_t* filter_data, gboolean attach)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_DEBUG("%s -- Entry, sf_bin = %d, filter_bin = %d, attach = %d\n",
            __FUNCTION__, sf_bin_data->bin_id, filter_data->filter_bin_id,
            attach);

    // Get the filter bin sink pad
    GstPad* filter_bin_sink_pad = gst_element_get_static_pad(
            filter_data->filter_bin, "sink");
    if (NULL == filter_bin_sink_pad)
    {
        RILOG_ERROR("%s -- unable to get filter bin sink pad\n", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    // Get current state of link
    gboolean isLinked = gst_pad_is_linked(filter_bin_sink_pad);
    if (isLinked && attach)
    {
        RILOG_INFO("%s -- sf tee and filter bin already linked\n", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }
    if (!isLinked && !attach)
    {
        RILOG_INFO("%s -- sf tee and filter bin already unlinked\n",
                __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    // Determine if data is flowing through this bin
    GstFormat format = GST_FORMAT_DEFAULT;
    gboolean isFlowing = gst_element_query_position(sf_bin_data->sf_bin_tee,
            &format, NULL);
    RILOG_DEBUG("%s -- is data flowing? %d\n", __FUNCTION__, isFlowing);

    // Get the current state of the bin
    GstState curState;
    GstState pendingState;
    GstStateChangeReturn rc = 0;
    rc = gst_element_get_state(sf_bin_data->sf_bin_tee,
            &curState, &pendingState, GST_CLOCK_TIME_NONE);
    RILOG_DEBUG("%s -- cur state %s, pending state %s, rc %s\n", __FUNCTION__,
            gst_element_state_get_name(curState), gst_element_state_get_name(
                    pendingState), gst_element_state_change_return_get_name(rc));

    if (attach)
    {
        // Add this filter bin to the section filter bin
        gst_bin_add_many(GST_BIN(sf_bin_data->sf_bin), filter_data->filter_bin,
                NULL);

        // Request a src pad from the tee
        filter_data->filter_tee_src_pad = gst_element_get_request_pad(
                sf_bin_data->sf_bin_tee, "src%d");
        if (NULL == filter_data->filter_tee_src_pad)
        {
            RILOG_ERROR("%s -- unable to get sf bin tee src pad\n",
                    __FUNCTION__);
            RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
            return;
        }

        // If current state is playing and link is already attached, block the input src
        if ((GST_STATE_PLAYING == curState) && (isFlowing))
        {
            RILOG_DEBUG("%s -- blocking input\n", __FUNCTION__);

            // Need this call when modifying elements within same pipeline
            // but causes lock when trying to switch pipelines, blocks forever
            // Block the input src pad
            CHECK_OK_OBJECT(sf_bin_data->sf_bin_tee,
                    gst_pad_set_blocked(filter_data->filter_tee_src_pad, TRUE),
                    "Unable to block sf bin src pad");
        }
        else
        {
            RILOG_DEBUG("%s -- not blocking input\n", __FUNCTION__);
        }

        // Link sf bin src pad to filter bin sink
        if (GST_PAD_LINK_OK != gst_pad_link(filter_data->filter_tee_src_pad,
                filter_bin_sink_pad))
        {
            RILOG_ERROR("%s -- problems linking sf tee and filter bin\n",
                    __FUNCTION__);
        }
        else
        {
            RILOG_DEBUG("%s -- successfully linked sf tee and filter bin\n",
                    __FUNCTION__);
        }

        (void) gst_element_set_state(filter_data->filter_bin, GST_STATE_PLAYING);

        // Unblock input src pad if unblocked
        if (gst_pad_is_blocked(filter_data->filter_tee_src_pad))
        {
            CHECK_OK_OBJECT(sf_bin_data->sf_bin_tee,
                    gst_pad_set_blocked(filter_data->filter_tee_src_pad, FALSE),
                    "Unable to unblock output bin src pad");
        }
    }
    else // detach
    {
        // Unlink sf bin src pad to filter bin sink
        if (TRUE != gst_pad_unlink(filter_data->filter_tee_src_pad,
                filter_bin_sink_pad))
        {
            RILOG_ERROR("%s -- problems unlinking sf tee and filter bin\n",
                    __FUNCTION__);
        }
        else
        {
            RILOG_DEBUG("%s -- successfully unlinked sf tee and filter bin\n",
                    __FUNCTION__);
        }

        // Remove the filter bin from sf bin
        gst_bin_remove_many(GST_BIN(sf_bin_data->sf_bin),
                filter_data->filter_bin, NULL);

        // Free our references
        gst_element_release_request_pad(sf_bin_data->sf_bin_tee,
                filter_data->filter_tee_src_pad);
        gst_object_unref(GST_OBJECT(filter_data->filter_tee_src_pad));
        filter_data->filter_tee_src_pad = NULL;
    }

    // Free our references
    gst_object_unref(GST_OBJECT(filter_bin_sink_pad));

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets a GStreamer element property
 *
 * @param element set property of this gstreamer element
 * @param target  name of property to set
 * @param value   desired value of the property
 */
static void set_element_string(GstElement *element, const char * target,
        const char *value)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GValue prop =
    { 0, }; // Used to set the target property
    (void) g_value_init(&prop, G_TYPE_STRING);
    g_value_set_string(&prop, value);
    g_object_set_property(G_OBJECT(element), target, &prop);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
} /* set_element_string */

/**
 * Performs the necessary initialization so this inband section filter will
 * perform SI acquistion by creating PAT and PMT sinks and acquire the video pid.
 *
 * @param   section_filter initialize this sf as closed
 */
static void setup_for_closed_inband_section_filter(
        ri_section_filter_t* section_filter)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    ib_section_filter_data_t* data = section_filter->data;

    // Create data structure to store info related to this bin
    data->closed_data = g_try_malloc0(sizeof(closed_data_t));
    if (NULL != data->closed_data)
    {
        memset(data->closed_data, 0, sizeof(closed_data_t));
    }
    else
    {
        RILOG_ERROR(
                "%s() -- Unable to allocate memory for closed data of section filter\n",
                __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    // Create a filter on PID 0 to obtain PAT
    uint16_t pid = 0;
    uint8_t pos_mask = 255;
    uint8_t pos_value = 0;
    uint16_t pos_length = 1;
    uint8_t neg_mask = 0;
    uint8_t neg_value = 0;
    uint16_t neg_length = 0;

    // Create a filter which will allow us to get the PAT
    if (RI_ERROR_NONE != create_filter(section_filter,
            &data->closed_data->pat_filter_id, pid, &pos_mask, &pos_value,
            pos_length, &neg_mask, &neg_value, neg_length, pat_data_cb))
    {
        RILOG_ERROR("%s -- problems setting up filter for PAT\n", __FUNCTION__);
    }
    else
    {
        RILOG_INFO("%s -- setup filter %d on pid %d for PAT\n", __FUNCTION__,
                data->closed_data->pat_filter_id, pid);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Callback function returning matched section on filter on PID 0
 * looking for PAT.
 *
 * @param   section_filter this inband section filter
 * @param   sectionID      id of matched section
 * @param   filterID       id of the filter used for matching
 * @param   data           actual data of matched section
 * @param   length         length of data
 */
static void pat_data_cb(ri_section_filter_t* section_filter,
        uint32_t sectionID, uint32_t filterID, uint8_t* section_data,
        uint16_t length)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    ib_section_filter_data_t* data = section_filter->data;

    // Cancel this PAT filter
    if (RI_ERROR_NONE != cancel_filter(section_filter,
            data->closed_data->pat_filter_id))
    {
        RILOG_ERROR("%s -- problems canceling filter for PAT\n", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }
    else
    {
        RILOG_INFO("%s -- canceled filter %d on pid 0 for PAT\n", __FUNCTION__,
                data->closed_data->pat_filter_id);
    }

    // Get the PMT PID out of the section data
    uint16_t pmt_pid = 0;
    extract_pmt_pid(section_data, length, &pmt_pid);
    if (0 == pmt_pid)
    {
        RILOG_ERROR("%s -- unable to extract PMT PID\n", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }
    else
    {
        RILOG_INFO("%s -- about to setup filter on pid %d for PMT\n",
                __FUNCTION__, pmt_pid);
    }

    // Setup the PMT filter
    uint8_t pos_mask = 255;
    uint8_t pos_value = 2;
    uint16_t pos_length = 1;
    uint8_t neg_mask = 0;
    uint8_t neg_value = 0;
    uint16_t neg_length = 0;

    if (RI_ERROR_NONE != create_filter(section_filter,
            &data->closed_data->pmt_filter_id, pmt_pid, &pos_mask, &pos_value,
            pos_length, &neg_mask, &neg_value, neg_length, pmt_data_cb))
    {
        RILOG_ERROR("%s -- problems setting up filter for PMT\n", __FUNCTION__);
    }
    else
    {
        RILOG_INFO("%s -- setup filter %d on pid %d for PMT\n", __FUNCTION__,
                data->closed_data->pmt_filter_id, pmt_pid);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Method which is called when PMT data sections are available
 * based on the filter setup for PMT.
 *
 * @param   section_filter this inband section filter
 * @param   sectionID      id of matched section
 * @param   filterID       id of the filter used for matching
 * @param   section_data   actual data of matched section
 * @param   length         length of data
 */
static void pmt_data_cb(ri_section_filter_t* section_filter,
        uint32_t sectionID, uint32_t filterID, uint8_t* section_data,
        uint16_t length)
{
    uint32_t numPids;
    ri_pid_info_t ri_pid;

    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    ib_section_filter_data_t* data = section_filter->data;

    // Cancel this PMT filter
    if (RI_ERROR_NONE != cancel_filter(section_filter,
            data->closed_data->pmt_filter_id))
    {
        RILOG_ERROR("%s -- problems canceling filter for PMT\n", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }
    else
    {
        RILOG_INFO("%s -- canceled filter %d for PMT\n", __FUNCTION__,
                data->closed_data->pmt_filter_id);
    }

    // Find the first video pid in this section data
    uint16_t video_pid = 0;
    extract_video_pid(section_data, length, &video_pid);
    if (0 == video_pid)
    {
        RILOG_ERROR("%s -- unable to extract Video PID\n", __FUNCTION__);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }
    else
    {
        RILOG_INFO("%s -- extract video pid %d from PMT\n", __FUNCTION__,
                video_pid);
    }

    // Notify via decode callback PIDs which were discovered
    RILOG_INFO("%s -- calling decode cb with video pid %d\n", __FUNCTION__,
            video_pid);

    numPids = 1;
    ri_pid.srcPid = video_pid;
    ri_pid.srcFormat = RI_SI_ELEM_MPEG_2_VIDEO;
    data->decode_cb((ri_pid_info_t*) &ri_pid, numPids, data->decode_data);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Retrieves the PMT PID from the supplied section data, looking for the
 * first non-zero program number.
 *
 * @param   data     byte data contained within the section
 * @param   length   length of the byte data
 * @param   pmt_pid  pointer to the extracted PMT PID value
 */
static void extract_pmt_pid(uint8_t* data, uint16_t length, uint16_t* pmt_pid)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    // Skip to the program numbers
    int curLoc = 8;
    data += 8;

    // Look for the first program number which is not zero, assume it is the PMT
    uint16_t program_number = 0;
    uint16_t pid = 0;

    gboolean done = FALSE;
    do
    {
        data = parse_uint16(data, &program_number);
        curLoc += 2;
        data = parse_uint16(data, &pid);
        curLoc += 2;
        pid &= 0x1fff;

        // Look for the first program number which is not 0
        if (0 != program_number)
        {
            *pmt_pid = pid;
            done = TRUE;
            RILOG_INFO("%s -- found program number %d with pid %d\n",
                    __FUNCTION__, program_number, pid);
        }
        else
        {
            RILOG_INFO("%s -- found program number 0\n", __FUNCTION__);
        }

        // Check if at end of section data
        if (curLoc >= length)
        {
            RILOG_INFO("%s -- reached the end of the data\n", __FUNCTION__);
            done = TRUE;
        }
    } while (!done);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Retrieves the first video PID from the supplied PMT section data.
 *
 * @param   data     byte data contained within the section
 * @param   length   length of the byte data
 * @param   pmt_pid  pointer to the extracted PID value
 */
static void extract_video_pid(uint8_t* data, uint16_t length,
        uint16_t* video_pid)
{
    if (NULL == riIbSectionFilterCat)
    {
        riIbSectionFilterCat = log4c_category_get("RI.SectionFilter.IB");
    }

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    // Skip up to the program info length
    int curLoc = 10;
    data += 10;

    // Get the program info length
    uint16_t program_info_length = 0;
    data = parse_uint16(data, &program_info_length);
    program_info_length &= 0x0fff;
    curLoc += 2;

    // Use program length to skip over the program info descriptors
    RILOG_INFO("%s -- program_info_length %d\n", __FUNCTION__,
            program_info_length);
    data += program_info_length;
    curLoc += program_info_length;

    // Look for the first stream type which is MPEG2 video
    uint8_t stream_type = 0;
    uint16_t pid = 0;

    gboolean done = FALSE;
    do
    {
        stream_type = *data++;
        curLoc++;
        data = parse_uint16(data, &pid);
        curLoc += 2;
        pid &= 0x1fff;

        // Look for the first stream which is type 2 - MPEG2 Video
        if (2 == stream_type)
        {
            *video_pid = pid;
            done = TRUE;
            RILOG_INFO("%s -- found stream type %d with pid %d\n",
                    __FUNCTION__, stream_type, pid);
        }
        else
        {
            RILOG_INFO("%s -- found stream type %d\n", __FUNCTION__,
                    stream_type);
        }

        // Check if at end of section data
        if (curLoc >= length)
        {
            RILOG_INFO("%s -- reached the end of the data\n", __FUNCTION__);
            done = TRUE;
        }
    } while (!done);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Utility function to parse unsigned 16 bit integer.
 *
 * @param   inBuf    current position in buffer to read value from
 * @param   out_int  unsigned integer value which was read
 *
 * @return  adjusted position within the buffer after read
 */
static uint8_t* parse_uint16(uint8_t* inBuf, uint16_t* out_int)
{
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    uint16_t val = 0;

    val |= (inBuf[0] << 8) & 0xFF00;
    val |= inBuf[1] & 0xFF;
    inBuf += 2;

    *out_int = val;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return inBuf;
}
