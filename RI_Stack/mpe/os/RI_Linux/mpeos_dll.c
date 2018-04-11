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

#include <dlfcn.h>    /* dlopen(3), dlerror(3), dlsym(3), dlclose(3) */

#include <mpe_error.h>
#include <mpeos_dll.h>
#include <mpeos_dbg.h>

#define MPE_MEM_DEFAULT MPE_MEM_GENERAL

static void **g_mpe_ftable; /* MPE Global Function Table */

/***
 *** File-Private Function Prototypes
 ***/
//static int hash_string(const char*);
//static const char * get_lib_name(const char *);

/**
 * <i>mpeos_dlmodInit</i>
 *
 * Initialize the MPEOS DLL support with the MPE global function table
 * (mpe_ftable).  This populates the porting layer with the MPE global
 * function table pointer so it can be passed to library modules during
 * intialization of the modules.
 *
 * @param mpe_ftable pointer to MPE global function table.
 */
void mpeos_dlmodInit(void **mpe_ftable)
{
    g_mpe_ftable = mpe_ftable;
}

/**
 * The <i>mpeos_dlmodOpen()</i> function shall load/locate/link and
 * initialize the module specified by name.  The module may be loaded
 * from a file system or located in RAM or ROM.  This function can also
 * be used to get a pointer to the calling module (null name).  This
 * initialization interface returns a ID/handle for association of the
 * library module and subsequent symbol lookup.
 *
 * TODO - identify the Linux module-loading mechanism
 *
 * In the Linux implementation, the module is loaded using TBD.
 * Then the shared library symbol table is acquired via a MDT entry in
 * the module.  This symbol table is a hash table that is subsequently
 * used for symbol lookups.
 *
 * @param name is a pointer to the name of the module to open.
 * @param dlmodId is a pointer for returning the identifier of the
 *     opened module.
 * @return The MPE error code if the create fails, otherwise
 *     <i>MPE_SUCCESS<i/> is returned.
 */
mpe_Error mpeos_dlmodOpen(const char *name, mpe_Dlmod *dlmodId)
{
    void (*mpelib_init)(void**); /* Library init routine. */
    void *linuxMod;

    if (NULL == name)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DLL,
                "mpeos_dlmodOpen() name is NULL!\n");
        return MPE_EINVAL;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL, "mpeos_dlmodOpen() name: \"%s\"\n",
            name);

    /*
     * open the dynamic library in Linux.
     * for now, let's open the DLL in LAZY mode, meaning
     * resolve symbols as the DLL is executed.
     */
    if ((linuxMod = dlopen(name, RTLD_LAZY)) == NULL)
    {
        // This log mesage is DEBUG level because some clients use this failure
        // as a test for library existence
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL,
                "mpeos_dlmodOpen(): dlopen('%s') failed: %s\n", name, dlerror());
        return MPE_EINVAL;
    }

    *dlmodId = linuxMod;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL,
            "mpeos_dlmodOpen(): SUCCESS! Handle = %p\n", *dlmodId);

    /*
     * The library's MPE initialization entry point is optional.
     * If it exists, it must be of the form mpelib_init_[name],
     * where "name" corresponds to the name of the library.
     */

    if (MPE_SUCCESS == mpeos_dlmodGetSymbol(*dlmodId, "mpelib_init",
            (void**) &mpelib_init))
    {
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DLL,
                "mpeos_dlmodOpen(): invoking library MPE initialization function: 'mpelib_init' @ 0x%p\n",
                mpelib_init);
        (*mpelib_init)(g_mpe_ftable);
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DLL,
                "mpeos_dlmodOpen(): NOT invoking library MPE initialization function: 'mpelib_init' (symbol not found)\n");
    }
    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_dlmodGetSymbol()</i> function shall locate symbol
 * information for target symbol in specified module.  This will be the
 * mechanism for locating a target function within a module.  The target
 * library module is specified by the ID/handle returned from the "open"
 * operation.
 *
 * @param dlmodId is the identifier of the target module.
 * @param symbol is a pointer to a name string for the symbol for which
 *     to perform the search/lookup.
 * @param value is a void pointer for returning the associated value of
 * the target symbol.
 * @return The MPE error code if the create fails, otherwise
 * <i>MPE_SUCCESS<i/> is returned.
 */
mpe_Error mpeos_dlmodGetSymbol(mpe_Dlmod dlmodId, const char *symbol,
        void **value)
{
    char *checkRet = (char *) 0;

    /*
     * Per Linux Manpage
     * 1. Clear any extant errors
     * 2. Search for the symbol (NULL is legitimate return value)
     * 3. Check for resulting error
     */
    (void) dlerror();
    *value = dlsym(dlmodId, symbol);
    checkRet = dlerror();
    if (NULL != checkRet)
    {
        /*
         * Changed from _ERROR to _DEBUG to eliminate "noise".
         * The caller should log a complaint, if appropriate.
         */
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DLL,
                "mpeos_dlmodGetSymbol(): failed to find symbol \"%s\" [dll_handle %p].  Reason: %s\n",
                symbol, dlmodId, checkRet);
        return MPE_EINVAL;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL,
            "mpeos_dlmodGetSymbol(): found symbol \"%s\"\n", symbol);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_dlmodClose()</i>
 *
 * Unlink and deinitialize a dynamic link library.
 *
 * @param dlmodId is the DLL handle to close.
 *
 * @return The MPE error code if DLL close fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned.
 */
mpe_Error mpeos_dlmodClose(mpe_Dlmod dlmodId)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL, "mpeos_dlmodClose(): %p\n", dlmodId);

    /* close the DLL in Linux. */
    dlclose(dlmodId);

    return MPE_SUCCESS;
}
