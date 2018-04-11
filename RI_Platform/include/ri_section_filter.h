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

#ifndef _RI_SECTION_FILTER_H_
#define _RI_SECTION_FILTER_H_

#include <ri_types.h>

typedef struct ri_section_filter_s ri_section_filter_t;

/**
 * The standard section data callback registered with each logical
 * section filter
 */
typedef void (*section_data_callback_f)(ri_section_filter_t* object,
        uint32_t section_id, uint32_t filter_id, uint8_t* section_data,
        uint16_t section_length);

/**
 * This structure provides section filtering functionality for a single media
 * pipeline.  Separate instances will exist for each in-band, out-of-band and,
 * DVR pipeline.
 */
struct ri_section_filter_s
{
    /**
     * Create a logical section filter with the given filtering parameters.
     * Filtering parameters consist of positive and negative filter masks and
     * values along with a relative priority for the logical filter.  Sections
     * will be matched when the section header, bitwise-ANDed with the positive
     * mask array, IS EQUAL to the values specified in the positive values
     * array AND when the section header, bitwise-ANDed with the negative
     * mask array, IS NOT EQUAL to the values specified in the negative values
     * array.
     *
     * As part of creating a logical section filter, you also specify a
     * callback function on which you will receive section data.  For each
     * section matched by the filter, the callback will be notified with
     * the section filtering instance that produced the section, the logical
     * filter ID that matched the section, the section data, and a unique
     * identifier that represents your reference to the underlying resources
     * associated with the section data.  YOU MUST eventuanlly pass this ID to
     * release_section_data() to ensure that section memory will be properly
     * recycled.
     *
     * @param object The "this" pointer
     * @param filter_id The location where the implementation will store the
     *        unique identifier for the newly-created logical section filter
     * @param pos_mask The positive mask byte array
     * @param pos_value The positive data value byte array
     * @param pos_length The length, in bytes, of the positive mask and values
     *        arrays
     * @param neg_mask The negative mask byte array
     * @param neg_value The negative data value byte array
     * @param neg_length The length, in bytes, of the negative mask and values
     *        arrays
     * @param section_data_cb The section data callback function that will be
     *        notified when this filter matches a section
     * @return An error code detailing the success or failure of the request.
     */
    ri_error (*create_filter)(ri_section_filter_t* object, uint32_t* filter_id,
            uint16_t pid, uint8_t* pos_mask, uint8_t* pos_value,
            uint16_t pos_length, uint8_t* neg_mask, uint8_t* neg_value,
            uint16_t neg_length, section_data_callback_f section_data_cb);

    /**
     * Cancels a previously created logical section filter.  No additional
     * sections will be evented by this filter's criteria.  Any outstanding
     * sections associated with this filter as delivered to the section_data_cb
     * function must still be explicitly freed by calling release_section_data()
     *
     * @param object The "this" pointer
     * @param filter_id The unique identifier for the logical filter that
     *        is to be canceled
     * @return An error code detailing the success or failure of the request.
     */
    ri_error (*cancel_filter)(ri_section_filter_t* object, uint32_t filter_id);

    /**
     * sets the application ID for DSG section flows
     *
     * @param object The "this" pointer
     * @param appId The application ID for this DSG section flow
     * @return An error code detailing the success or failure of the request.
     */
    ri_error (*set_appID)(ri_section_filter_t* object, uint32_t appId);

    /**
     * gets the application ID for DSG section flows
     *
     * @param object The "this" pointer
     * @param appId The returned application ID for this DSG section flow
     * @return An error code detailing the success or failure of the request.
     */
    ri_error (*get_appID)(ri_section_filter_t* object, uint32_t *appId);

    /**
     * Returns the maximum number of logical filters allowed by this section
     * filtering implementation.
     *
     * @param object The "this" pointer
     */
    uint16_t (*num_allowed_filters)(ri_section_filter_t* object);

    // Section filter private data
    void* data;
};

#endif /* _RI_SECTION_FILTER_H_ */
