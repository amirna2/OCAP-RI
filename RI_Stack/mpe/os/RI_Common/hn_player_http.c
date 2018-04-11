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

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <errno.h>
#include <inttypes.h>

#include <mpe_types.h>
#include <mpe_error.h>

#include <mpeos_dbg.h>
#include <mpeos_mem.h>

#include "hn_player_http.h"

#define CR '\r'
#define LF '\n'

#define       HEADER_MIN_SIZE 48 // "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n\0"
#define CHUNK_HEADER_MIN_SIZE  4 // "0\r\n\0"
#define       BUFFER_MIN_SIZE  1

// DLNA 7.2.34 DDC Maximum HTTP Header Size
#define MAX_HTTP_HDR_LEN        (4096 + 1) // terminating '\0'

#ifdef MPE_LINUX
#define min(x,y) ((x)<(y)?(x):(y))
#endif

// Main state machine parsing function
static mpe_Error hnPlayer_httpDecodeInternal(http_decoder_t* http_decoder,
    uint32_t in_buffer_size, uint8_t* in_buffer_data, uint32_t* bytes_read);

// Outgoing buffer copy function
static uint32_t hnPlayer_httpDecodeGetData(http_decoder_t* http_decoder,
    uint32_t* out_buffer_size, uint8_t** out_buffer_data);

// Header decoding functions
static void hnPlayer_httpDecodeHeaderFields(http_decoder_t* http_decoder);
static void hnPlayer_httpDecodeChunkHeaderFields(http_decoder_t* http_decoder);

// Utility functions
static mpe_Error parse_npt_time(char* npt_string, uint64_t* media_time_ms);
static mpe_Error parse_uint32(char* string, uint32_t* result, mpe_Bool isHex);
static mpe_Error parse_uint64(char* string, uint64_t* result, mpe_Bool isHex);
static uint8_t* datnchr(uint8_t* buffer, uint32_t size, uint8_t what);
static void upcase(char *p);

mpe_Error hnPlayer_httpInit(http_decoder_t* http_decoder,
    uint32_t header_max_size, uint32_t chunk_header_max_size,
    uint32_t buffer_max_size, mpe_MemColor heap_type)
{
    mpe_Error ret_val = MPE_EINVAL;

    if (http_decoder == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - http_decoder is NULL\n", __FUNCTION__);
    }
    else if (header_max_size < HEADER_MIN_SIZE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - header_max_size must be at least %u\n", __FUNCTION__, HEADER_MIN_SIZE);
    }
    else if (chunk_header_max_size < CHUNK_HEADER_MIN_SIZE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - chunk_header_max_size must be at least %u\n", __FUNCTION__, CHUNK_HEADER_MIN_SIZE);
    }
    else if (buffer_max_size < BUFFER_MIN_SIZE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - buffer_max_size must be at least %u\n", __FUNCTION__, BUFFER_MIN_SIZE);
    }
    else
    {
        ret_val = MPE_ENOMEM;

        if (mpe_memAllocP(heap_type, header_max_size, (void**) &http_decoder->header_data) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Error allocating memory for header_data\n", __FUNCTION__);
        }
        else if (mpe_memAllocP(heap_type, chunk_header_max_size, (void**) &http_decoder->chunk_header_data) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Error allocating memory for chunk_header_data\n", __FUNCTION__);
        }
        else if (mpe_memAllocP(heap_type, buffer_max_size, (void**) &http_decoder->buffer_data) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Error allocating memory for buffer_data\n", __FUNCTION__);
        }
        else
        {
            http_decoder->header_max_size = header_max_size;
            http_decoder->chunk_header_max_size = chunk_header_max_size;
            http_decoder->buffer_max_size = buffer_max_size;
            hnPlayer_httpReset(http_decoder);
            ret_val = MPE_SUCCESS;
        }
    }

    return ret_val;
}

void hnPlayer_httpReset(http_decoder_t* http_decoder)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - resetting http_decoder fields\n", __FUNCTION__);
    http_decoder->state = HTTP_HEADER_ASSEMBLY;

    http_decoder->header_size = 0;
    http_decoder->header_status_code = 0;
    http_decoder->header_connection_id = 0;
    //http_decoder->header_media_time_ms = 0;

    http_decoder->chunk_header_size = 0;
    http_decoder->chunk_media_time_ms = 0;
    http_decoder->chunk_byte_position = 0;

    http_decoder->read_size = 0;
    http_decoder->data_size = INVALID_CONTENT_LENGTH;

    http_decoder->buffer_size = 0;
    http_decoder->header_available_start_time_ms = INVALID_MEDIA_TIME_MS; 
    http_decoder->header_available_end_time_ms = INVALID_MEDIA_TIME_MS;

    http_decoder->header_time_seek_start_time_ms = INVALID_MEDIA_TIME_MS;
    http_decoder->header_time_seek_end_time_ms = INVALID_MEDIA_TIME_MS;

}

mpe_Error hnPlayer_httpDecode(http_decoder_t* http_decoder,
    uint32_t in_buffer_size, uint8_t* in_buffer_data,
    uint32_t* bytes_read,
    uint32_t out_buffer_size, uint8_t* out_buffer_data,
    uint32_t* bytes_written)
{
    mpe_Error ret_val = MPE_SUCCESS;

    MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() - Input buffer %u bytes, output buffer %u bytes\n", __FUNCTION__, in_buffer_size, out_buffer_size);

    if (in_buffer_size <= 0 || in_buffer_data == NULL)
    {
        *bytes_read = 0;
        *bytes_written = hnPlayer_httpDecodeGetData(http_decoder, &out_buffer_size, &out_buffer_data);
    }
    else
    {
        uint32_t local_read = 0;
        uint32_t total_avail_read = in_buffer_size;

        *bytes_read = 0;
        *bytes_written = 0;

        do
        {
            ret_val = hnPlayer_httpDecodeInternal(http_decoder, in_buffer_size, in_buffer_data, &local_read);
            MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() - Read %5u of %5u bytes\n", __FUNCTION__, local_read, in_buffer_size);
            *bytes_read += local_read;
            in_buffer_data += local_read;
            in_buffer_size -= local_read;

            if (http_decoder->buffer_size == http_decoder->buffer_max_size && out_buffer_size == 0)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() Buffer overflow. Make sure out_buffer_size >= in_buffer_size\n", __FUNCTION__);
                ret_val = MPE_ENOMEM;
            }

            if (ret_val == MPE_SUCCESS)
            {
                if (http_decoder->buffer_size == http_decoder->buffer_max_size || http_decoder->state == HTTP_COMPLETE)
                {
                    *bytes_written += hnPlayer_httpDecodeGetData(http_decoder, &out_buffer_size, &out_buffer_data);
                }
            }
            else
            {
                break;
            }
        }
        while (*bytes_read < total_avail_read && http_decoder->state != HTTP_COMPLETE);
    }

    MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() - Read total of %u bytes, wrote total of %u bytes\n", __FUNCTION__, *bytes_read, *bytes_written);

    return ret_val;
}

static mpe_Error hnPlayer_httpDecodeInternal(http_decoder_t* http_decoder,
    uint32_t in_buffer_size, uint8_t* in_buffer_data, uint32_t* bytes_read)
{
    mpe_Error ret_val = MPE_SUCCESS;

    uint32_t bytes_total = in_buffer_size;
    mpe_Bool buffer_full = FALSE;

    *bytes_read = 0;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - about to read headers", __FUNCTION__);
    while (*bytes_read < bytes_total && buffer_full == FALSE && ret_val == MPE_SUCCESS)
    {
        switch (http_decoder->state)
        {

            /****************************/
            /***                      ***/
            /*** HTTP_HEADER_ASSEMBLY ***/
            /***                      ***/
            /****************************/
            case HTTP_HEADER_ASSEMBLY:
            {
                uint32_t bytes_header_left = http_decoder->header_max_size - http_decoder->header_size - 1; // need space for \0

                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_HEADER_ASSEMBLY:          bytes_header_left = %u\n", bytes_header_left);

                if (bytes_header_left > 0)
                {
                    uint32_t bytes_left = min(bytes_header_left, in_buffer_size);
                    uint32_t bytes_copy = 0;

                    uint8_t* cr_pos = datnchr(in_buffer_data, bytes_left, CR);
                    if (cr_pos == NULL)
                    {
                        bytes_copy = bytes_left;
                    }
                    else
                    {
                        bytes_copy = ((uint32_t) (cr_pos - in_buffer_data)) + 1; // include trailing CR
                        http_decoder->state = HTTP_HEADER_ASSEMBLY_CR;
                    }

                    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_HEADER_ASSEMBLY:          bytes_left = %u, bytes_copy = %u\n", bytes_left, bytes_copy);
                    memcpy(&http_decoder->header_data[http_decoder->header_size], in_buffer_data, bytes_copy);
                    http_decoder->header_size += bytes_copy;
                    in_buffer_size -= bytes_copy;
                    in_buffer_data += bytes_copy;
                    *bytes_read += bytes_copy;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_HEADER_ASSEMBLY: insufficient memory to store HTTP header (header_max_size = %u)\n",
                        __FUNCTION__, http_decoder->header_max_size);
                    http_decoder->header_data[http_decoder->header_size++] = '\0';
                    ret_val = MPE_ENOMEM;
                }
                break;
            }

            /*******************************/
            /***                         ***/
            /*** HTTP_HEADER_ASSEMBLY_CR ***/
            /***                         ***/
            /*******************************/
            case HTTP_HEADER_ASSEMBLY_CR:
            {
                uint32_t bytes_header_left = http_decoder->header_max_size - http_decoder->header_size - 1; // need space for \0

                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_HEADER_ASSEMBLY_CR:       bytes_header_left = %u\n", bytes_header_left);

                if (bytes_header_left > 0) // have at least 1 byte
                {
                    http_decoder->header_data[http_decoder->header_size++] = *in_buffer_data;

                    if (*in_buffer_data == LF)
                    {
                        http_decoder->state = HTTP_HEADER_ASSEMBLY_CRLF;
                    }
                    else if (*in_buffer_data != CR)
                    {
                        http_decoder->state = HTTP_HEADER_ASSEMBLY;
                    }
                    // else stay in HTTP_HEADER_ASSEMBLY_CR

                    in_buffer_size--;
                    in_buffer_data++;
                    (*bytes_read)++;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_HEADER_ASSEMBLY_CR: insufficient memory to store HTTP header (header_max_size = %u)\n",
                        __FUNCTION__, http_decoder->header_max_size);
                    http_decoder->header_data[http_decoder->header_size++] = '\0';
                    ret_val = MPE_ENOMEM;
                }
                break;
            }

            /*********************************/
            /***                           ***/
            /*** HTTP_HEADER_ASSEMBLY_CRLF ***/
            /***                           ***/
            /*********************************/
            case HTTP_HEADER_ASSEMBLY_CRLF:
            {
                uint32_t bytes_header_left = http_decoder->header_max_size - http_decoder->header_size - 1; // need space for \0

                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_HEADER_ASSEMBLY_CRLF:     bytes_header_left = %u\n", bytes_header_left);

                if (bytes_header_left > 0) // have at least 1 byte
                {
                    http_decoder->header_data[http_decoder->header_size++] = *in_buffer_data;

                    if (*in_buffer_data == CR)
                    {
                        http_decoder->state = HTTP_HEADER_ASSEMBLY_CRLFCR;
                    }
                    else
                    {
                        http_decoder->state = HTTP_HEADER_ASSEMBLY;
                    }

                    in_buffer_size--;
                    in_buffer_data++;
                    (*bytes_read)++;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_HEADER_ASSEMBLY_CRLF: insufficient memory to store HTTP header (header_max_size = %u)\n",
                        __FUNCTION__, http_decoder->header_max_size);
                    http_decoder->header_data[http_decoder->header_size++] = '\0';
                    ret_val = MPE_ENOMEM;
                }
                break;
            }

            /***********************************/
            /***                             ***/
            /*** HTTP_HEADER_ASSEMBLY_CRLFCR ***/
            /***                             ***/
            /***********************************/
            case HTTP_HEADER_ASSEMBLY_CRLFCR:
            {
                uint32_t bytes_header_left = http_decoder->header_max_size - http_decoder->header_size - 1; // need space for \0

                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_HEADER_ASSEMBLY_CRLFCR:   bytes_header_left = %u\n", bytes_header_left);

                if (bytes_header_left > 0)
                {
                    http_decoder->header_data[http_decoder->header_size++] = *in_buffer_data;
                    if (*in_buffer_data == LF)
                    {
                        // Don't increment bytes_read, so that the header is parsed in this
                        // iteration. This is in case the in_buffer_data last character is
                        // the LF that we are processing right now.
                        http_decoder->header_data[http_decoder->header_size++] = '\0';
                        http_decoder->state = HTTP_HEADER_COMPLETE;
                    }
                    else
                    {
                        (*bytes_read)++;
                        if (*in_buffer_data != CR)
                        {
                            http_decoder->state = HTTP_HEADER_ASSEMBLY;
                        }
                        else
                        {
                            http_decoder->state = HTTP_HEADER_ASSEMBLY_CR;
                        }
                    }

                    in_buffer_size--;
                    in_buffer_data++;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_HEADER_ASSEMBLY_CRLFCR: insufficient memory to store HTTP header (header_max_size = %u)\n",
                        __FUNCTION__, http_decoder->header_max_size);
                    http_decoder->header_data[http_decoder->header_size++] = '\0';
                    ret_val = MPE_ENOMEM;
                }
                break;
            }

            /****************************/
            /***                      ***/
            /*** HTTP_HEADER_COMPLETE ***/
            /***                      ***/
            /****************************/
            case HTTP_HEADER_COMPLETE:
            {
                char *http_header = (char*) http_decoder->header_data;
                char *upcase_http_header = NULL;

                char *chunked_ptr = NULL;
                char *content_ptr = NULL;
                char *connClose_ptr = NULL;

                if (mpe_memAllocP(MPE_MEM_HN, MAX_HTTP_HDR_LEN, (void**) &upcase_http_header) != MPE_SUCCESS)
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Error allocating memory for upcase_http_header\n", __FUNCTION__);
                    return MPE_ENOMEM;
                }
                strcpy(upcase_http_header, (char*) http_decoder->header_data);
                upcase(upcase_http_header);

                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() HTTP_HEADER_COMPLETE: Parsing assembled header:\n%s\n", __FUNCTION__, http_header);

                (*bytes_read)++;

                chunked_ptr = strstr(http_header, "Transfer-Encoding: chunked");
                content_ptr = strstr(http_header, "Content-Length:");
                connClose_ptr = strstr(upcase_http_header, "CONNECTION: CLOSE");

                if (content_ptr != NULL)
                {
                    ret_val = parse_uint64(content_ptr + 15, &http_decoder->data_size, FALSE);
                    if (ret_val == MPE_SUCCESS)
                    {
                        if (chunked_ptr != NULL)
                        {
                            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "%s() HTTP_HEADER_COMPLETE: 'Content-Length' and 'Transfer-Encoding: chunked' are both present - "
                                "assuming non-chunked encoding mode\n", __FUNCTION__);
                        }
                        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() HTTP_HEADER_COMPLETE: Content-Length is %"PRIu64"\n", __FUNCTION__, http_decoder->data_size);
                        if (http_decoder->data_size > 0)
                        {
                            http_decoder->state = HTTP_STREAM_ASSEMBLY;
                        }
                        else
                        {
                            http_decoder->state = HTTP_COMPLETE;
                        }
                    }
                    else
                    {
                        if (chunked_ptr == NULL)
                        {
                            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "%s() HTTP_HEADER_COMPLETE: Unable to parse 'Content-Length' - "
                                "assuming unbound non-chunked encoding mode\n", __FUNCTION__);
                            http_decoder->state = HTTP_STREAM_ASSEMBLY;
                        }
                        else
                        {
                            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "%s() HTTP_HEADER_COMPLETE: 'Content-Length' and 'Transfer-Encoding: chunked' are both present "
                                "but failed to parse content length - assuming chunked encoding mode\n", __FUNCTION__);
                            http_decoder->state = HTTP_CHUNK_HEADER_ASSEMBLY;
                        }
                        ret_val = MPE_SUCCESS;
                    }
                }
                else if (chunked_ptr == NULL)
                {
                    MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "%s() HTTP_HEADER_COMPLETE: No 'Content-Length' nor 'Transfer-Encoding: chunked' found - "
                        "assuming unbound non-chunked encoding mode\n", __FUNCTION__);
                    if (connClose_ptr != NULL)
                    {
                        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() HTTP_HEADER_COMPLETE: Setting data_size to UNSPECIFIED_CONTENT_LENGTH\n", __FUNCTION__);
                        http_decoder->data_size = UNSPECIFIED_CONTENT_LENGTH;
                    }
                    else
                    {
                    }
                    http_decoder->state = HTTP_STREAM_ASSEMBLY;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "HTTP_HEADER_COMPLETE:          Found 'Transfer-Encoding: chunked'\n");
                    http_decoder->state = HTTP_CHUNK_HEADER_ASSEMBLY;
                }

                if (ret_val == MPE_SUCCESS)
                {
                    hnPlayer_httpDecodeHeaderFields(http_decoder);
                }
                break;
            }

            /****************************/
            /***                      ***/
            /*** HTTP_STREAM_ASSEMBLY ***/
            /***                      ***/
            /****************************/
            case HTTP_STREAM_ASSEMBLY:
            {
                uint32_t bytes_buffer_left = http_decoder->buffer_max_size - http_decoder->buffer_size;

                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_STREAM_ASSEMBLY:          bytes_buffer_left = %u\n", bytes_buffer_left);

                if (bytes_buffer_left > 0)
                {
                    uint32_t bytes_have = 0;
                    if (http_decoder->data_size == UNSPECIFIED_CONTENT_LENGTH)
                    {
                        bytes_have = (uint64_t) bytes_buffer_left;
                    }
                    else
                    {
                        uint64_t total_left = http_decoder->data_size - http_decoder->read_size;
                        bytes_have = (uint32_t) min(total_left, (uint64_t) bytes_buffer_left);
                    }

                    uint32_t bytes_copy = min(bytes_have, in_buffer_size);

                    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_STREAM_ASSEMBLY:          bytes_have = %u, bytes_copy = %u\n", bytes_have, bytes_copy);

                    memcpy(&http_decoder->buffer_data[http_decoder->buffer_size], in_buffer_data, bytes_copy);
                    http_decoder->buffer_size += bytes_copy;
                    http_decoder->read_size += (uint64_t) bytes_copy;
                    in_buffer_size -= bytes_copy;
                    in_buffer_data += bytes_copy;
                    *bytes_read += bytes_copy;

                    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_STREAM_ASSEMBLY:          read_size = %"PRIu64", data_size = %"PRIu64"\n", http_decoder->read_size, http_decoder->data_size);
                    if (http_decoder->data_size != UNSPECIFIED_CONTENT_LENGTH &&
                        http_decoder->read_size == http_decoder->data_size)
                    {
                        http_decoder->state = HTTP_COMPLETE;
                    }

                    if (http_decoder->buffer_size == http_decoder->buffer_max_size)
                    {
                        buffer_full = TRUE;
                    }
                }
                else
                {
                    buffer_full = TRUE;
                }
                break;
            }

            /**********************************/
            /***                            ***/
            /*** HTTP_CHUNK_HEADER_ASSEMBLY ***/
            /***                            ***/
            /**********************************/
            case HTTP_CHUNK_HEADER_ASSEMBLY:
            {
                uint32_t bytes_chunk_header_left = http_decoder->chunk_header_max_size - http_decoder->chunk_header_size - 1; // need space for \0

                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_CHUNK_HEADER_ASSEMBLY:    bytes_chunk_header_left = %u\n", bytes_chunk_header_left);

                if (bytes_chunk_header_left > 0)
                {
                    uint32_t bytes_left = min(bytes_chunk_header_left, in_buffer_size);
                    uint32_t bytes_copy = 0;

                    uint8_t* cr_pos = datnchr(in_buffer_data, bytes_left, CR);
                    if (cr_pos == NULL)
                    {
                        bytes_copy = bytes_left;
                    }
                    else
                    {
                        bytes_copy = ((uint32_t) (cr_pos - in_buffer_data)) + 1; // include trailing CR
                        http_decoder->state = HTTP_CHUNK_HEADER_ASSEMBLY_CR;
                    }

                    memcpy(&http_decoder->chunk_header_data[http_decoder->chunk_header_size], in_buffer_data, bytes_copy);
                    http_decoder->chunk_header_size += bytes_copy;
                    in_buffer_size -= bytes_copy;
                    in_buffer_data += bytes_copy;
                    *bytes_read += bytes_copy;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_CHUNK_HEADER_ASSEMBLY: insufficient memory to store HTTP chunk header (chunk_header_max_size = %u)\n",
                        __FUNCTION__, http_decoder->chunk_header_max_size);
                    ret_val = MPE_ENOMEM;
                }
                break;
            }

            /*************************************/
            /***                               ***/
            /*** HTTP_CHUNK_HEADER_ASSEMBLY_CR ***/
            /***                               ***/
            /*************************************/
            case HTTP_CHUNK_HEADER_ASSEMBLY_CR:
            {
                uint32_t bytes_chunk_header_left = http_decoder->chunk_header_max_size - http_decoder->chunk_header_size - 1; // need space for \0

                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_CHUNK_HEADER_ASSEMBLY_CR: bytes_chunk_header_left = %u\n", bytes_chunk_header_left);

                if (bytes_chunk_header_left > 0)
                {
                    http_decoder->chunk_header_data[http_decoder->chunk_header_size++] = *in_buffer_data;
                    if (*in_buffer_data == LF)
                    {
                        // Don't increment bytes_read, so that the header is parsed in this
                        // iteration. This is in case the in_buffer_data last character is
                        // the LF that we are processing right now.
                        http_decoder->chunk_header_data[http_decoder->chunk_header_size++] = '\0';
                        http_decoder->state = HTTP_CHUNK_HEADER_COMPLETE;
                    }
                    else
                    {
                        (*bytes_read)++;
                        if (*in_buffer_data != CR)
                        {
                            http_decoder->state = HTTP_CHUNK_HEADER_ASSEMBLY;
                        }
                        // else stay in HTTP_CHUNK_HEADER_ASSEMBLY_CR
                    }

                    in_buffer_size--;
                    in_buffer_data++;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_CHUNK_HEADER_ASSEMBLY_CR: insufficient memory to store HTTP chunk header (chunk_header_max_size = %u)\n",
                        __FUNCTION__, http_decoder->chunk_header_max_size);
                    ret_val = MPE_ENOMEM;
                }
                break;
            }

            /**********************************/
            /***                            ***/
            /*** HTTP_CHUNK_HEADER_COMPLETE ***/
            /***                            ***/
            /**********************************/
            case HTTP_CHUNK_HEADER_COMPLETE:
            {
                MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() HTTP_CHUNK_HEADER_COMPLETE: Looking at assembled chunk header:\n%s\n",
                    __FUNCTION__, http_decoder->chunk_header_data);

                (*bytes_read)++;

                ret_val = parse_uint64((char*) http_decoder->chunk_header_data, &http_decoder->data_size, TRUE);
                if (ret_val == MPE_SUCCESS)
                {
                    MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() HTTP_CHUNK_HEADER_COMPLETE: Parsed chunk length %"PRIu64"\n", __FUNCTION__, http_decoder->data_size);
                    if (http_decoder->data_size > 0)
                    {
                        http_decoder->state = HTTP_CHUNK_ASSEMBLY;
                    }
                    else
                    {
                        http_decoder->state = HTTP_CHUNK_COMPLETE;
                    }
                    hnPlayer_httpDecodeChunkHeaderFields(http_decoder);
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_CHUNK_HEADER_COMPLETE: Error parsing chunk length '%s'\n",
                        __FUNCTION__, http_decoder->chunk_header_data);
                    ret_val = MPE_EINVAL;
                }
                break;
            }

            /***************************/
            /***                     ***/
            /*** HTTP_CHUNK_ASSEMBLY ***/
            /***                     ***/
            /***************************/
            case HTTP_CHUNK_ASSEMBLY:
            {
                uint32_t bytes_buffer_left = http_decoder->buffer_max_size - http_decoder->buffer_size;

                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_CHUNK_ASSEMBLY:           bytes_buffer_left = %u\n", bytes_buffer_left);

                if (bytes_buffer_left > 0)
                {
                    uint64_t total_left = http_decoder->data_size - http_decoder->read_size;
                    uint32_t bytes_have = (uint32_t) min(total_left, (uint64_t) bytes_buffer_left);
                    uint32_t bytes_copy = min(bytes_have, in_buffer_size);

                    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_CHUNK_ASSEMBLY:           bytes_have = %u, bytes_copy = %u\n", bytes_have, bytes_copy);

                    memcpy(&http_decoder->buffer_data[http_decoder->buffer_size], in_buffer_data, bytes_copy);
                    http_decoder->buffer_size += bytes_copy;
                    http_decoder->read_size += (uint64_t) bytes_copy;
                    in_buffer_size -= bytes_copy;
                    in_buffer_data += bytes_copy;
                    *bytes_read += bytes_copy;

                    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_CHUNK_ASSEMBLY:           read_size = %"PRIu64", data_size = %"PRIu64"\n", http_decoder->read_size, http_decoder->data_size);
                    if (http_decoder->read_size == http_decoder->data_size)
                    {
                        http_decoder->state = HTTP_CHUNK_COMPLETE;
                    }

                    if (http_decoder->buffer_size == http_decoder->buffer_max_size)
                    {
                        buffer_full = TRUE;
                    }
                }
                else
                {
                    buffer_full = TRUE;
                }
                break;
            }

            /***************************/
            /***                     ***/
            /*** HTTP_CHUNK_COMPLETE ***/
            /***                     ***/
            /***************************/
            case HTTP_CHUNK_COMPLETE:
            {
                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_CHUNK_COMPLETE:           \n");

                if (in_buffer_size > 0) // need to read single byte
                {
                    if (*in_buffer_data == CR)
                    {
                        http_decoder->state = HTTP_CHUNK_COMPLETE_CR;
                    }
                    else
                    {
                        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_CHUNK_COMPLETE: Improperly terminated chunk (missing CRLF)\n", __FUNCTION__);
                        ret_val = MPE_EINVAL;
                    }

                    in_buffer_size--;
                    in_buffer_data++;
                    (*bytes_read)++;
                }
                break;
            }

            /******************************/
            /***                        ***/
            /*** HTTP_CHUNK_COMPLETE_CR ***/
            /***                        ***/
            /******************************/
            case HTTP_CHUNK_COMPLETE_CR:
            {
                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_CHUNK_COMPLETE_CR:        \n");

                if (in_buffer_size > 0) // need to read single byte
                {
                    if (*in_buffer_data == LF)
                    {
                        if (http_decoder->data_size > 0)
                        {
                            http_decoder->chunk_header_size = 0;
                            http_decoder->read_size = 0;
                            http_decoder->state = HTTP_CHUNK_HEADER_ASSEMBLY;
                        }
                        else
                        {
                            http_decoder->state = HTTP_COMPLETE;
                            if (http_decoder->buffer_size == http_decoder->buffer_max_size)
                            {
                                buffer_full = TRUE;
                            }
                        }
                    }
                    else
                    {
                        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() HTTP_CHUNK_COMPLETE: Improperly terminated chunk (missing LF)\n", __FUNCTION__);
                        ret_val = MPE_EINVAL;
                    }

                    in_buffer_size--;
                    in_buffer_data++;
                    (*bytes_read)++;
                }
                break;
            }

            /*********************/
            /***               ***/
            /*** HTTP_COMPLETE ***/
            /***               ***/
            /*********************/
            case HTTP_COMPLETE:
            {
                MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_HN, "HTTP_COMPLETE:                 \n");

                if (http_decoder->buffer_size == http_decoder->buffer_max_size)
                {
                    buffer_full = TRUE;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "%s() HTTP_COMPLETE: Transmission done, unable to accept more data\n", __FUNCTION__);
                    ret_val = MPE_ENODATA;
                }
                break;
            }

            default:
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() Invalid state %u\n", __FUNCTION__, http_decoder->state);
                ret_val = MPE_EINVAL;
                break;
            }
        }
    }

    return ret_val;
}

static uint32_t hnPlayer_httpDecodeGetData(http_decoder_t* http_decoder,
    uint32_t* out_buffer_size, uint8_t** out_buffer_data)
{
    uint32_t bytes_copy = min(*out_buffer_size, http_decoder->buffer_size);
    memcpy(*out_buffer_data, http_decoder->buffer_data, bytes_copy);
    MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() - Copied %u bytes\n", __FUNCTION__, bytes_copy);

    if (*out_buffer_size < http_decoder->buffer_max_size)
    {
        uint32_t bytes_move = http_decoder->buffer_size - bytes_copy;
        memmove(&http_decoder->buffer_data[0], &http_decoder->buffer_data[bytes_copy], bytes_move);
        MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() - Moved %u bytes\n", __FUNCTION__, bytes_move);
    }

    *out_buffer_data += bytes_copy;
    *out_buffer_size -= bytes_copy;
    http_decoder->buffer_size -= bytes_copy;

    return bytes_copy;
}

static void hnPlayer_httpDecodeHeaderFields(http_decoder_t* http_decoder)
{
    char* string = NULL;
    char flags[8 + 1];
    char* header_copy = NULL;
    int S0_INCREASING = 1 << 27;
    long value = 0;

    
    if (mpe_memAllocP(MPE_MEM_HN, MAX_HTTP_HDR_LEN, (void**) &header_copy) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Error allocating memory for header_copy\n", __FUNCTION__);
    }
    strcpy(header_copy, (char*) http_decoder->header_data);
    upcase(header_copy);
    string = strstr(header_copy, "HTTP/1.1");
    if (string != NULL)
    {
        if (parse_uint32(string + 8, &http_decoder->header_status_code, FALSE) == MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - Parsed HTTP status code %u\n", __FUNCTION__, http_decoder->header_status_code);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Failed to parse HTTP status code\n", __FUNCTION__);
        }
    }

    string = strstr(header_copy, "SCID.DLNA.ORG:");
    if (string != NULL)
    {
        if (parse_uint32(string + 14, &http_decoder->header_connection_id, FALSE) == MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - Parsed scid.dlna.org %u\n", __FUNCTION__, http_decoder->header_connection_id);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Failed to parse scid.dlna.org\n", __FUNCTION__);
        }
    }

    //  TimeSeekRange-line = "TimeSeekRange.dlna.org" *LWS ":" *LWS range specifier
    //  range specifier = npt range [SP bytes-range]
    //
    //   Examples:
    //   •   TimeSeekRange.dlna.org : npt=335.11-336.08
    //   •   TimeSeekRange.dlna.org : npt=00:05:35.3-00:05: 37.5
    string = strstr(header_copy, "TIMESEEKRANGE.DLNA.ORG:");
    if (string != NULL)
    {
        string = strstr(string, "NPT=");
        if (string != NULL && parse_npt_time(string + 4, &http_decoder->header_time_seek_start_time_ms) == MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - Parsed TimeSeekRange.dlna.org start %"PRIu64"\n",
                    __FUNCTION__, http_decoder->header_time_seek_start_time_ms);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Failed to parse TimeSeekRange.dlna.org start from: %s\n",
                    __FUNCTION__, (string + 4));
        }

        string = strstr(string, "-");
        if (string != NULL && parse_npt_time(string + 1, &http_decoder->header_time_seek_end_time_ms) == MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - Parsed TimeSeekRange.dlna.org end %"PRIu64"\n", __FUNCTION__,
                    http_decoder->header_time_seek_end_time_ms);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Failed to parse TimeSeekRange.dlna.org end from: %s\n",
                    __FUNCTION__, (string + 1));
        }
    }

    //  AvailableSeekRange-line = "AvailableSeekRange.dlna.org" *LWS ":" *LWS range specifier
    //  range specifier = npt range [SP bytes-range]
    //
    //   Examples:
    //      availableSeekRange.dlna.org: 0 bytes=214748364-224077003
    //      availableSeekRange.dlna.org: 0 npt=00:05.30.12-00:10:34
    //      availableSeekRange.dlna.org: 1 npt=00:05.30.12-00:10:34 bytes=214748364-224077003
    string = strstr(header_copy, "AVAILABLESEEKRANGE.DLNA.ORG:");
    if (string != NULL)
    {
        string = strstr(string, "NPT=");
        if (string != NULL && parse_npt_time(string + 4, &http_decoder->header_available_start_time_ms) == MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - Parsed availableSeekRange.dlna.org start %"PRIu64"\n", __FUNCTION__,
                    http_decoder->header_available_start_time_ms);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Failed to parse availableSeekRange.dlna.org start time from string: %s\n",
                    __FUNCTION__, (string + 4));
        }

        string = strstr(string, "-");
        if (string != NULL && parse_npt_time(string + 1, &http_decoder->header_available_end_time_ms) == MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - Parsed availableSeekRange.dlna.org end %"PRIu64"\n", __FUNCTION__,
                    http_decoder->header_available_end_time_ms);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Failed to parse availableSeekRange.dlna.org end time from string: %s\n",
                    __FUNCTION__, (string + 1));
        }
    }

    string = strstr(header_copy, "CONTENTFEATURES.DLNA.ORG:");
    if (string != NULL)
    {
        string += strlen("contentFeatures.dlna.org:");
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - Content features string: %s\n", __FUNCTION__, string);

        // Get flags param portion of protocol info
        string = strstr(string, "DLNA.ORG_FLAGS=");
        if (string != NULL)
        {
            // Get rid of flags prefix and trailing 24 digits which are unused
            string += strlen("DLNA.ORG_FLAGS=");
            strncpy((char*)&flags[0], string, 8);
            flags[8] = '\0';
            value = strtol(flags, (char**)NULL, 16);
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - flags string: %s, value: %ld\n", __FUNCTION__, flags, value);

            // Extract s0 increasing flag from protocol info string
            http_decoder->header_s0_increasing = (value & S0_INCREASING) == S0_INCREASING;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - s0 increasing: %d\n", __FUNCTION__,
                    http_decoder->header_s0_increasing);
        }
    }

    string = strstr(header_copy, "CONNECTION: CLOSE");
    if (string != NULL)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - connection: close found\n", __FUNCTION__);
        http_decoder->close_conn = true;
    }
    else
    {
        http_decoder->close_conn = false;
    }

    if (mpeos_memFreePGen(MPE_MEM_HN, (void*) header_copy) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Error deallocating memory for header_copy\n", __FUNCTION__);
    }

}

static void hnPlayer_httpDecodeChunkHeaderFields(http_decoder_t* http_decoder)
{
    char* string = NULL;

    string = strstr((char*) http_decoder->chunk_header_data, "bytes=");
    if (string != NULL)
    {
        if (parse_uint64(string + 7, &http_decoder->chunk_byte_position, FALSE) == MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() - Parsed bytes %"PRIu64"\n", __FUNCTION__, http_decoder->chunk_byte_position);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Failed to parse bytes in string: %s, starting at: %s\n",
                    __FUNCTION__, string, (string + 7));
        }
    }

    // RI-only, not present in the spec...
    string = strstr((char*) http_decoder->chunk_header_data, "npt=");
    if (string != NULL)
    {
        if (parse_npt_time(string + 5, &http_decoder->chunk_media_time_ms) == MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_HN, "%s() - Parsed npt %"PRIu64"\n", __FUNCTION__, http_decoder->chunk_media_time_ms);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Failed to parse npt in string: %s, starting at: %s\n",
                    __FUNCTION__, string, (string + 5));
        }
    }
}

//  range specifier = npt range [SP bytes-range]
//  npt range   =   "npt" "=" npt time "-" [ npt time ] [instance-duration]
//  instance-duration = "/" (npt-time | "*")
//  bytes range = "bytes" "=" first byte pos "-" last byte pos instance-length
//  first byte pos = 1*DIGIT
//  last byte pos = 1*DIGIT
//  instance-length = "/" (1*DIGIT | "*")
//
//  npt time  = npt sec | npt hhmmss
//  npt sec   = 1*DIGIT [ "." 1*3DIGIT ]
//  npthhmmss = npthh":"nptmm":"nptss["."1*3DIGIT]
//  npthh     = 1*DIGIT     ; any positive number
//  nptmm     = 1*2DIGIT    ; 0-59
//  nptss     = 1*2DIGIT    ; 0-59
static mpe_Error parse_npt_time(char* string, uint64_t* media_time_ms)
{
    mpe_Error ret_val = MPE_SUCCESS;

    uint32_t hours = 0;
    uint32_t  mins = 0;
    float     secs = 0.;

    if (sscanf(string, "%u:%u:%f", &hours, &mins, &secs) == 3)
    {
        // Long form
        *media_time_ms = (hours * 60 * 60 * 1000) + (mins * 60 * 1000) + (secs * 1000);
    }
    else if (sscanf(string, "%f", &secs) == 1)
    {
        // Short form
        *media_time_ms = secs * 1000;
    }
    else
    {
        ret_val = MPE_EINVAL;
    }

    return ret_val;
}

static mpe_Error parse_uint32(char* string, uint32_t* result, mpe_Bool isHex)
{
    mpe_Error ret_val = MPE_SUCCESS;
    char*     convert = NULL;

    int base = (isHex == TRUE)?(16):(10);

    *result = strtoul(string, &convert, base);
    if (errno == ERANGE || string == convert)
    {
        ret_val = MPE_EINVAL;
    }

    return ret_val;
}

static mpe_Error parse_uint64(char* string, uint64_t* result, mpe_Bool isHex)
{
    mpe_Error ret_val = MPE_SUCCESS;
    char*     convert = NULL;

    int base = (isHex == TRUE)?(16):(10);

    *result = strtoull(string, &convert, base);
    if (errno == ERANGE || string == convert)
    {
        ret_val = MPE_EINVAL;
    }

    return ret_val;
}

static uint8_t* datnchr(uint8_t* buffer, uint32_t size, uint8_t what)
{
    int i = 0;

    for (i = 0; i < size; i++)
    {
        if (buffer[i] == what)
        {
            return &buffer[i];
        }
    }

    return NULL;
}

static void upcase(char *p)
{
    while(*p != '\0')
    {
        if(*p >= 97 && *p <= 122)
        {
            *p -= 32;
        }
        ++p;
    }
}
