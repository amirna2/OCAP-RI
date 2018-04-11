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

#ifndef _PIPELINE_HN_SERVER_H_
#define _PIPELINE_HN_SERVER_H_

#include <ri_pipeline.h>

typedef struct pipeline_hn_server_data_s
{
    GstAppSinkCallbacks appsink_callbacks;  // Callbacks for appsink in hn server pipeline
    guint bufCnt;                           // buffer counter which is incremented via callback method
    gboolean isEOS;                         // flag indicating when EOS has been reached
    uint64_t bytePos;                       // current byte position in stream
    float rate;                             // streaming rate
    gboolean eventReceived;                 // flag indicating event was received
    guint appsink_event_probe;              // method to call when event is received
    GMutex* buf_mutex;                      // Mutex to control access to buffer count & eos flag

    ri_hn_srvr_type orig_pipe_type;         // the original pipe type (before CT)
    char* orig_file_path;                   // the original path (before CT)
    char* orig_file_name;                   // the original file (before CT)
    char ct_path[FILENAME_MAX];             // the path to use for CT
    char ct_file[FILENAME_MAX];             // the file to use for CT
    ri_transformation_t* ct;                // the transformation information

} pipeline_hn_server_data_t;

gboolean pipeline_hn_server_create(ri_pipeline_t* pPipeline);
void pipeline_hn_server_flow_start(ri_pipeline_t* pPipeline, int tuner,
                                   ri_pid_info_t* pids, int pid_count);
void pipeline_hn_server_flow_stop(ri_pipeline_t* pPipeline, int tuner);

ri_error pipeline_transform_live_stream(ri_pipeline_t* object,
                                        int tuner,
                                        ri_transformation_t* ct);
ri_error pipeline_transform_file_stream(ri_pipeline_t* object,
                                        char* file_path,
                                        char* file_name,
                                        ri_transformation_t* ct);
ri_error pipeline_transform_status(ri_pipeline_t* object,
                                   char* status,
                                   int buffer_size);
#endif /* _PIPELINE_HN_SERVER_H_ */
