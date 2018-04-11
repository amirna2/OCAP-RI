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

#include <ri_section_cache.h>
#include <ri_section_filter.h>
#include <ri_log.h>

#include "section_cache.h"

#include <glib.h>
#include <stdlib.h>

#define RILOG_CATEGORY riSectionCacheCat
log4c_category_t* riSectionCacheCat = NULL;

// Singleton instance
static ri_section_cache_t* section_cache_instance = NULL;

// Data private to each section_cache instance
struct ri_section_cache_data_s
{
    // Maps section data buffers to unique section IDs
    // Key: section ID (integet)
    // Value: section data (GstBuffer*)
    GHashTable* section_data;

    // Mutex to protect access to section cache data structures
    GMutex* section_cache_mutex;

    // This counter generates unique section IDs for each section managed
    // by the cache
    uint32_t section_id_counter;
};

/**
 * Releases the reference to section data tied to the given section ID
 */
void release_section_data(ri_section_cache_t * object, uint32_t section_id)
{
    GstBuffer * buffer;

    RILOG_TRACE("%s -- Entry: section_id = %d", __FUNCTION__, section_id);

    g_mutex_lock(object->data->section_cache_mutex);

    // Find this section in our table
    buffer = g_hash_table_lookup(object->data->section_data, GUINT_TO_POINTER(
            section_id));

    if (buffer == NULL)
    {
        g_mutex_unlock(object->data->section_cache_mutex);
        RILOG_ERROR("%s -- Illegal section ID %d\n", __FUNCTION__, section_id);
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    }
    else
    {
        // Decrement the buffer's reference count
        (void) g_hash_table_remove(object->data->section_data,
                GUINT_TO_POINTER(section_id));
        gst_buffer_unref(buffer);

        g_mutex_unlock(object->data->section_cache_mutex);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    }
}

/**
 * Add a matched section to the cache
 */
ri_error add_section(ri_section_cache_t* object, GstBuffer* section_buffer,
        uint32_t* section_id)
{
    RILOG_TRACE("%s -- section_buffer = 0x%p", __FUNCTION__, section_buffer);

    if (section_id == NULL || section_buffer == NULL)
    {
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return RI_ERROR_ILLEGAL_ARG;
    }

    g_mutex_lock(object->data->section_cache_mutex);

    // Our new section ID
    *section_id = object->data->section_id_counter++;

    // add the section data
    g_hash_table_insert(object->data->section_data, GUINT_TO_POINTER(
            *section_id), section_buffer);

    g_mutex_unlock(object->data->section_cache_mutex);

    RILOG_DEBUG("%s -- Added section_id = %d", __FUNCTION__, *section_id);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

// Create the singleton section cache instance (if necessary)
ri_section_cache_t* ri_get_section_cache(void)
{
    if (section_cache_instance == NULL)
    {
        riSectionCacheCat = log4c_category_get("RI.SectionCache");

        RILOG_DEBUG("%s -- Creating singleton section cache instance\n",
                __FUNCTION__);

        section_cache_instance = g_try_malloc(sizeof(ri_section_cache_t));

        if (NULL == section_cache_instance)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        // Assign implementation function pointers
        section_cache_instance->release_section_data = release_section_data;

        // Allocate private data and assign private function impl
        section_cache_instance->data
                = g_try_malloc(sizeof(ri_section_cache_data_t));

        if (NULL == section_cache_instance->data)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        // Create our cache data structures
        section_cache_instance->data->section_cache_mutex = g_mutex_new();
        section_cache_instance->data->section_data = g_hash_table_new(NULL,
                NULL);
        section_cache_instance->data->section_id_counter = 1;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return section_cache_instance;
}
