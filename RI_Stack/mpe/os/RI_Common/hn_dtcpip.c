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

#include <mpeos_dbg.h>
#include <mpeos_dll.h>

#include <stdio.h>
#include <string.h> // memcpy, strrchr, strlen

#if defined( WIN32 )
    #define snprintf _snprintf
#endif

#include "hn_dtcpip.h"
#include "mpeos_hn.h"

struct dtcpip_untyped_function_entry_s
{
    char *name;
    void (*func)();
};

typedef struct dtcpip_untyped_function_entry_s dtcpip_untyped_function_entry_t;

#define DTCPIP_STUB(ret,func,...) \
    static ret func ## _stub(__VA_ARGS__)

DTCPIP_STUB(int, dtcpip_cmn_init, const char* storage_path);
DTCPIP_STUB(void, dtcpip_cmn_get_version, char* string, size_t length);

DTCPIP_STUB(int, dtcpip_src_init, unsigned short dtcp_port);
DTCPIP_STUB(int, dtcpip_src_open, int* session_handle, int is_audio_only);
DTCPIP_STUB(int, dtcpip_src_alloc_encrypt, int session_handle,
                 unsigned char cci,
                 char* cleartext_data, size_t cleartext_size,
                 char** encrypted_data, size_t* encrypted_size);
DTCPIP_STUB(int, dtcpip_src_free, char* encrypted_data);
DTCPIP_STUB(int, dtcpip_src_close,int session_handle);

DTCPIP_STUB(int, dtcpip_snk_init, void);
DTCPIP_STUB(int, dtcpip_snk_open,
                 char* ip_addr, unsigned short ip_port,
                 int *session_handle);
DTCPIP_STUB(int, dtcpip_snk_alloc_decrypt, int session_handle,
                 char* encrypted_data, size_t encrypted_size,
                 char** cleartext_data, size_t* cleartext_size);
DTCPIP_STUB(int, dtcpip_snk_free, char* cleartext_data);
DTCPIP_STUB(int, dtcpip_snk_close, int session_handle);



#define DTCPIP_INIT(func) \
    { #func, (void(*)()) func ## _stub }
 
static dtcpip_untyped_function_entry_t untyped_functions[] =
{
    DTCPIP_INIT(dtcpip_cmn_init),
    DTCPIP_INIT(dtcpip_cmn_get_version),

    DTCPIP_INIT(dtcpip_src_init),
    DTCPIP_INIT(dtcpip_src_open),
    DTCPIP_INIT(dtcpip_src_alloc_encrypt),
    DTCPIP_INIT(dtcpip_src_free),
    DTCPIP_INIT(dtcpip_src_close),

    DTCPIP_INIT(dtcpip_snk_init),
    DTCPIP_INIT(dtcpip_snk_open),
    DTCPIP_INIT(dtcpip_snk_alloc_decrypt),
    DTCPIP_INIT(dtcpip_snk_free),
    DTCPIP_INIT(dtcpip_snk_close)
};

dtcpip_typed_functions_t* g_dtcpip_ftable = NULL;

static mpe_Dlmod dtcp_dll = (mpe_Dlmod) 0;

DTCPIP_STUB(int, dtcpip_cmn_init, const char* storage_path)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    // need to return success so that hn init suceeds
    return MPE_SUCCESS;
}

DTCPIP_STUB(void, dtcpip_cmn_get_version, char* string, size_t length)
{
    snprintf(string, length, "DTCP NOT IMPLEMENTED");
}

DTCPIP_STUB(int, dtcpip_src_init, unsigned short dtcp_port)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    // need to return success so that hn init suceeds
    return MPE_SUCCESS;
}

DTCPIP_STUB(int, dtcpip_src_open, int* session_handle, int is_audio_only)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    return -1;
}

DTCPIP_STUB(int, dtcpip_src_alloc_encrypt, int session_handle,
                 unsigned char cci,
                 char* cleartext_data, size_t cleartext_size,
                 char** encrypted_data, size_t* encrypted_size)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    return -1;
}

DTCPIP_STUB(int, dtcpip_src_free, char* encrypted_data)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    return -1;
}

DTCPIP_STUB(int, dtcpip_src_close,int session_handle)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    return -1;
}

DTCPIP_STUB(int, dtcpip_snk_init, void)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    // need to return success so that hn init suceeds
    return MPE_SUCCESS;
}

DTCPIP_STUB(int, dtcpip_snk_open,
                 char* ip_addr, unsigned short ip_port,
                 int *session_handle)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    return -1;
}

DTCPIP_STUB(int, dtcpip_snk_alloc_decrypt, int session_handle,
                 char* encrypted_data, size_t encrypted_size,
                 char** cleartext_data, size_t* cleartext_size)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    return -1;
}

DTCPIP_STUB(int, dtcpip_snk_free, char* cleartext_data)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);
    
    return -1;
}

DTCPIP_STUB(int, dtcpip_snk_close, int session_handle)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s NOT IMPLEMENTED\n", __FUNCTION__);

    return -1;
}


/**
 * Initializes the DTCP/IP library. If the library cannot be
 * located or does not have all the required functions, DTCP
 * encryption will be disabled and local stub functions will
 * be used instead.
 */
mpe_Error dtcpip_init()
{
    mpe_Error   ret_code = MPE_EINVAL;
    const char *dll_path = NULL;

    g_dtcpip_ftable = (dtcpip_typed_functions_t*) &untyped_functions;

    dll_path = mpeos_envGet(DLL_PATH_ENV);
    if (dll_path == NULL)
    {
#ifdef DTCPIP_BUILD_DLL
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - %s not defined - using compiled value of \"%s\".\n",
             __FUNCTION__, DLL_PATH_ENV, DTCPIP_BUILD_DLL);
        dll_path = DTCPIP_BUILD_DLL;
    }
#else
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "%s - %s not defined.\n", __FUNCTION__, DLL_PATH_ENV);
        return MPE_SUCCESS;
    }
    else
    {
#endif
        ret_code = mpeos_dlmodOpen(dll_path, &dtcp_dll);
        if (ret_code == MPE_SUCCESS)
        {
            int          i = 0;
            int num_funcs = -1;

            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - successfully loaded DTCP/IP library from \"%s\".\n",
                __FUNCTION__, dll_path);

            // First check that all functions defined in the structure can
            // be located in the supplied DLL.
            num_funcs = sizeof(untyped_functions) / sizeof(dtcpip_untyped_function_entry_t);
            for (i = 0; i < num_funcs; i++)
            {
                void (*func)() = NULL;
                ret_code = mpeos_dlmodGetSymbol(dtcp_dll, untyped_functions[i].name, (void **) &func);
                if (ret_code == MPE_SUCCESS)
                {
                    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - successfully located function \"%s\".\n",
                        __FUNCTION__, untyped_functions[i].name);
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - unable to locate function \"%s\".\n",
                        __FUNCTION__, untyped_functions[i].name);
                    break;
                }
            }

            // If all symbols can be located, go ahead and re-assign all of the function pointers.
            if (ret_code == MPE_SUCCESS)
            {
                for (i = 0; i < num_funcs; i++)
                {
                    ret_code = mpeos_dlmodGetSymbol(dtcp_dll, untyped_functions[i].name, (void **) &untyped_functions[i].func);
                    if (ret_code != MPE_SUCCESS)
                    {
                        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - unable to get symbol \"%s\".\n",
                                __FUNCTION__, untyped_functions[i].name);
                        return MPE_HN_ERR_OS_FAILURE;
                    }
                }
            }
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - unable to load DTCP/IP library from \"%s\": MPEOS DLL error %d.\n",
                __FUNCTION__, dll_path, ret_code);
            return MPE_HN_ERR_OS_FAILURE;
        }
    }

    if (ret_code != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - DTCP/IP disabled.\n");
        return MPE_HN_ERR_OS_FAILURE;
    }

    char* dtcp_storage = NULL;
    int    storage_len = 0;

    const char* dtcp_storage_env = mpeos_envGet(DTCP_STORAGE_ENV);
    if (dtcp_storage_env == NULL)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - %s not defined.\n",
                __FUNCTION__, DTCP_STORAGE_ENV);
        const char *last_slash = strrchr(dll_path, '/');
        if (last_slash != NULL)
        {
            storage_len = (int) (last_slash - dll_path);
            ret_code = mpeos_memAllocP(MPE_MEM_HN, storage_len + 1, (void **) &dtcp_storage);
            if (ret_code != MPE_SUCCESS)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - failed to allocate memory to copy \"%s\".\n",
                        __FUNCTION__, DLL_PATH_ENV);
                return MPE_HN_ERR_OS_FAILURE;
            }
            else
            {
                memcpy(dtcp_storage, dll_path, storage_len);
                dtcp_storage[storage_len] = '\0';
            }
        }
        else
        {
            storage_len = 1;
            ret_code = mpeos_memAllocP(MPE_MEM_HN, 2, (void **) &dtcp_storage);
            if (ret_code != MPE_SUCCESS)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - failed to allocate memory to copy and terminate \"%s\".\n",
                        __FUNCTION__, DLL_PATH_ENV);
                return MPE_HN_ERR_OS_FAILURE;
            }
            else
            {
                dtcp_storage[0] = '.';
                dtcp_storage[1] = '\0';
            }
        }
    }
    else
    {
        storage_len = strlen(dtcp_storage_env);
        ret_code = mpeos_memAllocP(MPE_MEM_HN, storage_len + 1, (void **) &dtcp_storage);
        if (ret_code != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - failed to allocate memory to copy env \"%s\".\n",
                    __FUNCTION__, DTCP_STORAGE_ENV);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            memcpy(dtcp_storage, dtcp_storage_env, storage_len);
            dtcp_storage[storage_len] = '\0';
        }
    }

    int result = 0;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - using \"%s\" for DTCP/IP library storage.\n",
            __FUNCTION__, dtcp_storage);
#if defined MPE_WINDOWS
    if (dtcp_storage[0] == '/' &&
            ((storage_len == 2) ||
                    (storage_len > 2 && dtcp_storage[2] == '/')))
    {
        dtcp_storage[0] = dtcp_storage[1];
        dtcp_storage[1] = ':';
    }
#endif
    result = g_dtcpip_ftable->dtcpip_cmn_init(dtcp_storage);
    if (result != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - dtcpip_cmn_init() failed with %d.\n",
                __FUNCTION__, result);
        return MPE_HN_ERR_OS_FAILURE;
    }

    mpeos_memFreeP(MPE_MEM_HN, (void *) dtcp_storage);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - DTCP/IP enabled.\n");

    return MPE_SUCCESS;
}
