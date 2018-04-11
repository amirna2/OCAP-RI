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

#ifndef _HN_DTCPIP_H_
#define _HN_DTCPIP_H_

#include <stdlib.h> // size_t
#include "mpe_error.h"

#define DTCPIP_DECL(ret,func,...) \
    char* func ## _name; \
    ret (*func)(__VA_ARGS__);

#define DTCPIP_INVALID_SESSION_HANDLE (-1)

// Name of the mpeenv.ini variable containing the library location
#define DLL_PATH_ENV "MPEOS.HN.DTCPIP.DLL"

// Name of the mpeenv.ini variable containing the DTCP storage location
#define DTCP_STORAGE_ENV "MPEOS.HN.DTCPIP.STORAGE"


typedef struct
{
    DTCPIP_DECL(int, dtcpip_cmn_init, const char* storage_path)
    DTCPIP_DECL(void, dtcpip_cmn_get_version, char* string, size_t length)

    DTCPIP_DECL(int, dtcpip_src_init, unsigned short dtcp_port)
    DTCPIP_DECL(int, dtcpip_src_open, int* session_handle, int is_audio_only)
    DTCPIP_DECL(int, dtcpip_src_alloc_encrypt, int session_handle,
                     unsigned char cci,
                     char* cleartext_data, size_t cleartext_size,
                     char** encrypted_data, size_t* encrypted_size)
    DTCPIP_DECL(int, dtcpip_src_free, char* encrypted_data)
    DTCPIP_DECL(int, dtcpip_src_close,int session_handle)

    DTCPIP_DECL(int, dtcpip_snk_init, void)
    DTCPIP_DECL(int, dtcpip_snk_open,
                     char* ip_addr, unsigned short ip_port,
                     int *session_handle)
    DTCPIP_DECL(int, dtcpip_snk_alloc_decrypt, int session_handle,
                     char* encrypted_data, size_t encrypted_size,
                     char** cleartext_data, size_t* cleartext_size)
    DTCPIP_DECL(int, dtcpip_snk_free, char* cleartext_data)
    DTCPIP_DECL(int, dtcpip_snk_close, int session_handle)
}
dtcpip_typed_functions_t;

extern dtcpip_typed_functions_t* g_dtcpip_ftable;

mpe_Error dtcpip_init();

#endif  /* _HN_DTCPIP_H_ */
