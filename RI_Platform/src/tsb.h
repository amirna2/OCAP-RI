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

#ifndef _TSB_H_
#define _TSB_H_

#include <glib.h>
#include <ri_pipeline.h>

#include <../ifs/IfsIntf.h>

typedef struct ri_tsb_data_s
{
    GStaticRecMutex tsb_mutex; // Recursive mutex to protect pipeline

    GThread * tsb_thread; // Thread to handle TSB processing
    GAsyncQueue * tsb_queue; // Queue to send commands to TSB
    gulong tsb_wake_interval; // Timeout for tsb_thread queue pop in uS

    ri_dvr_callback_f callback; // A callback function that will receive all active TSB events
    void* cb_data; // User data that will be passed to every active TSB callback invocation

    ri_dvr_callback_f convert_callback; // callback function to receive conversion-related events
    void* convert_cb_data; // user data passed to every conversion callback invocation

    IfsInfo *pIfsInfo; // TSB IfsInfo structure obtained from mpe
    IfsHandle ifsHandleTsb; // Indexing File System TSB session handle

    uint64_t original_tsb_start_time_ns;// original world time of the start of the TSB
    uint64_t  tsb_maximum_duration_ns;  // TSB maximum duration in nS
    ri_tsb_status_t tsb_status; // Used for TSB status reporting

    IfsHandle ifsConvHandle; // Indexing File System conversion handle
    uint64_t requested_convert_start_time_ns; // Requested start time compared to 0 at TSB record start
    uint64_t expected_convert_end_time_ns;// TSB time in nS when the convert should end
    uint32_t requested_convert_duration_s; // Requested length of the conversion in seconds (0 = unlimited)
    ri_tsb_status_t conversion_status; // Used for status reporting - maintains state of conversion
    // of playback
    float rate; // Playback rate (signed) 1.0 is normal 1x

    // Conversion items
    gboolean conversion_in_progress; // Indicates that a conversion is in progress.
    // Written by tsb thread only
} ri_tsb_data_t;

// Used to declare a table of TSBs known to the system, as well as any
//    transitory association with a pipeline
typedef struct _tsb_item
{
    ri_tsb_data_t* pTsb;
    ri_pipeline_t* pPipeline;
} tsb_item_t;

// declare state machine function pointer types
typedef void* (*doTsbState)(tsb_item_t* pTsbItem);
typedef void* (*doConversionState)(tsb_item_t* pTsbItem);

ri_error tsb_start(ri_pipeline_t* object, ri_tsbHandle tsb,
        ri_pid_info_t* pids, uint32_t pid_count, ri_dvr_callback_f callback,
        void* cb_data);

ri_error tsb_stop(ri_pipeline_t* pPipeline);

ri_pipeline_t* getPipelineFromTsbFileName(char* filename);

#endif /* _TSB_H_ */
