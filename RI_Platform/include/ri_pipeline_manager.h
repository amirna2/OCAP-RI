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

#ifndef _RI_PIPELINE_MANAGER_H_
#define _RI_PIPELINE_MANAGER_H_

#include <ri_pipeline.h>
#include <ri_oob_pipeline.h>
#include <ri_dsg_pipeline.h>
#include <ri_display.h>

typedef struct ri_pipeline_manager_s ri_pipeline_manager_t;
typedef struct ri_pipeline_manager_data_s ri_pipeline_manager_data_t;

/**
 * This structure represents the singleton manager of all media pipelines
 * supported by the current RI platform configuration.
 */
struct ri_pipeline_manager_s
{
    /**
     * Returns the out-of-band (OOB) pipeline for the platform.  Only one OOB
     * pipeline exists for each RI platform instance
     *
     * @param object The "this" pointer
     * @return the OOB pipeline for the platform
     */
    const ri_oob_pipeline_t* (*get_oob_pipeline)(ri_pipeline_manager_t* object);

    /**
     * Returns the DSG pipeline for the platform.  Only one DSG
     * pipeline exists for each RI platform instance
     *
     * @param object The "this" pointer
     * @return the DSG pipeline for the platform
     */
    const ri_dsg_pipeline_t* (*get_dsg_pipeline)(ri_pipeline_manager_t* object);

    /**
     * Returns the list of available live media pipelines available in the
     * current RI platform configuration.
     *
     * @param object The "this" pointer
     * @param num_pipelines The length of the returned pipeline array
     * @return An array of pointers to all the live media pipelines currently
     *         available on the platform
     */
    const ri_pipeline_t** (*get_live_pipelines)(ri_pipeline_manager_t* object,
            uint32_t* num_pipelines);

    /**
     * Returns the list of available DVR record pipelines available in the
     * current RI platform configuration.
     *
     * @param object The "this" pointer
     * @param num_pipelines The length of the returned pipeline array
     * @return An array of pointers to all the DVR record pipelines currently
     *         available on the platform
     */
    //const ri_record_pipeline_t* (*get_record_pipelines)(ri_pipeline_manager_t* object,
    //                                                    uint32_t* num_pipelines);

    /**
     * Returns the list of available DVR playback pipelines available in the
     * current RI platform configuration.
     *
     * @param object The "this" pointer
     * @param num_pipelines The length of the returned pipeline array
     * @return An array of pointers to all the DVR playback pipelines currently
     *         available on the platform
     */
    const ri_pipeline_t** (*get_playback_pipelines)(
            ri_pipeline_manager_t* object, uint32_t* num_pipelines);

    /**
     * Returns the list of available DVR remote playback pipelines available in the
     * current RI platform configuration.
     *
     * @param object The "this" pointer
     * @param num_pipelines The length of the returned pipeline array
     * @return An array of pointers to all the DVR remote playback pipelines currently
     *         available on the platform
     */
    const ri_pipeline_t** (*get_hn_server_pipelines)(
            ri_pipeline_manager_t* object, uint32_t* num_pipelines);

    /**
     * Returns the HN streaming pipeline for the platform.  Only one HN Streaming
     * pipeline exists for each RI platform instance due to one decoder limitation
     *
     * @param object The "this" pointer
     * @return the HN stream pipeline for the platform
     */
    ri_pipeline_t* (*get_hn_player_pipeline)(ri_pipeline_manager_t* object);

    /**
     * Returns the display display for the platform.  Currently only one
     * display display exists for each RI platform instance
     *
     * @param object The "this" pointer
     * @return the display screen for the platform
     */
    ri_display_t* (*get_display)(ri_pipeline_manager_t* object);

    /**
     * Private pipeline manager data
     */
    ri_pipeline_manager_data_t* data;
};

/**
 * Returns the singleton instance of the pipeline manager for the RI platform
 *
 * @return the pipeline manager singleton instance
 */
RI_MODULE_EXPORT ri_pipeline_manager_t* ri_get_pipeline_manager(void);

#endif /* _RI_PIPELINE_MANAGER_H_ */
