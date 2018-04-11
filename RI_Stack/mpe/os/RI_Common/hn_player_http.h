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

#ifndef _HN_PLAYER_HTTP_H_
#define _HN_PLAYER_HTTP_H_

#include <mpe_types.h>
#include <mpe_error.h>

#include <mpeos_mem.h>

typedef enum
{
    HTTP_HEADER_ASSEMBLY,
    HTTP_HEADER_ASSEMBLY_CR,
    HTTP_HEADER_ASSEMBLY_CRLF,
    HTTP_HEADER_ASSEMBLY_CRLFCR,
    HTTP_HEADER_COMPLETE,
    HTTP_STREAM_ASSEMBLY,
    HTTP_CHUNK_HEADER_ASSEMBLY,
    HTTP_CHUNK_HEADER_ASSEMBLY_CR,
    HTTP_CHUNK_HEADER_COMPLETE,
    HTTP_CHUNK_ASSEMBLY,
    HTTP_CHUNK_COMPLETE,
    HTTP_CHUNK_COMPLETE_CR,
    HTTP_COMPLETE
}
http_decode_state_e;

#define INVALID_CONTENT_LENGTH ((uint64_t) -1)
#define UNSPECIFIED_CONTENT_LENGTH ((uint64_t) -2)

#define INVALID_CONNECTION_ID ((uint32_t) -1)
#define INVALID_MEDIA_TIME_MS ((uint64_t) -1)
#define INVALID_BYTE_POSITION ((uint64_t) -1)

typedef struct
{
    http_decode_state_e state;

    uint32_t header_max_size;
    uint32_t chunk_header_max_size;
    uint32_t buffer_max_size;

    uint32_t header_size;
    uint8_t* header_data;

    uint32_t header_status_code;
    uint32_t header_connection_id;
    uint64_t header_time_seek_start_time_ms;
    uint64_t header_time_seek_end_time_ms;
    uint64_t header_available_start_time_ms;
    uint64_t header_available_end_time_ms;
    mpe_Bool header_s0_increasing;

    uint32_t chunk_header_size;
    uint8_t* chunk_header_data;

    uint64_t chunk_byte_position;
    uint64_t chunk_media_time_ms;

    uint64_t read_size; // total number of bytes read
    uint64_t data_size; // either content length or chunk length

    uint32_t buffer_size;
    uint8_t* buffer_data;

    mpe_Bool close_conn;
}
http_decoder_t;

mpe_Error hnPlayer_httpInit(http_decoder_t* http_decoder,
    uint32_t header_max_size, uint32_t chunk_header_max_size,
    uint32_t buffer_max_size, mpe_MemColor heap_type);

void hnPlayer_httpReset(http_decoder_t* http_decoder);

mpe_Error hnPlayer_httpDecode(http_decoder_t* http_decoder,
    uint32_t in_buffer_size, uint8_t* in_buffer_data,
    uint32_t* bytes_read,
    uint32_t out_buffer_size, uint8_t* out_buffer_data,
    uint32_t* bytes_written);

#endif // _HN_PLAYER_HTTP_H_
