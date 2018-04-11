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

/* Header Files */
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_sys.h>
#include <mpe_file.h>
#include <mpe_dbg.h>
#include <mpeos_dll.h>
#include <os_util.h>

#include <windows.h>
#include <stdlib.h>

/* The global target environment (located in mpeos_util.c). */
extern os_EnvConfig g_osEnvironment;

/* Static reference to MPE function table. */
static void **g_mpe_ftable;

/*
 * Export a special function to allow the CVM to acquire the mpe_ftable pointer
 * very early in it's initialization phase for memory allocations.
 */
__declspec (dllexport) void **os_getMpeFTable()
{
    return g_mpe_ftable;
}

/**
 * The <i>mpeos_dlmodInit</i> function will initialize the Module Support API.
 * <p>
 * Initialize the MPEOS DLL support with the MPE global function table (mpe_ftable).
 * This populates the porting layer with the MPE global function table pointer
 * so that it can be passed to library modules during intialization of the
 * modules.
 * </p>
 *
 * @param mpe_ftable Is a pointer to the MPE global function table.
 */
void mpeos_dlmodInit(void **ftable)
{
    g_mpe_ftable = ftable;
}

/**
 * The <i>mpeos_dlmodShutdown</i> function will terminate the Module Support API.
 */
void mpeos_dlmodShutdown()
{
    g_mpe_ftable = NULL;
}

/**
 * The <i>mpeos_dlmodOpen()</i> function shall load/locate/link and initialize the
 * module specified by name.
 * <p>
 * This function can also be used to get a pointer to the
 * calling module (null name).  This initialization interface returns a ID/handle
 * for association of the library module and subsequent symbol lookup.
 * On Windows, this module will be represented by a Windows DLL which will
 * be loaded using the LoadLibray function.
 * </p>
 *
 * @param name Is a pointer to the name of the module to open.
 * @param dlmodId Is a pointer for returning the identifier of the opened module.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_dlmodOpen(const char *name, mpe_Dlmod *dlmodId)
{
    HINSTANCE hInstance = NULL;

    /* Standard MPE filesystem syntax will indicate an absolute native path
     as "/<driveletter>/<restofpath>".  For Windows, we will convert the
     initial "/" and drive letter to "<driveletter>:" */
    if (strlen(name) > 2 && name[0] == '/')
    {
        char newPath[MPE_FS_MAX_PATH];
        strcpy(newPath, name);
        newPath[0] = name[1]; // Copy Drive Letter
        newPath[1] = ':';

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL,
                "mpeos_dlmodOpen: trying to load %s\n", newPath);

        hInstance = LoadLibrary(newPath);
    }
    else
    {
        // Walk through the list of module paths, attempting to open
        // the named DLL in each location.
        int i;
        char *newPath = NULL;
        for (i = 0; i < g_osEnvironment.os_numModules; i++)
        {
            // Build the new path.
            int size = strlen(g_osEnvironment.os_modulePath[i]) + strlen(name)
                    + 2;
            newPath = (char *) malloc(size);
            char *ptr = newPath;
            strncpy(ptr, g_osEnvironment.os_modulePath[i], strlen(
                    g_osEnvironment.os_modulePath[i]));
            ptr += strlen(g_osEnvironment.os_modulePath[i]);
            strncpy(ptr, "/", sizeof(char));
            ptr += sizeof(char);
            strncpy(ptr, name, strlen(name));
            ptr += strlen(name);
            *ptr = '\0';

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL,
                    "mpeos_dlmodOpen: trying to load %s\n", newPath);

            // Attempt to open the new path.
            hInstance = LoadLibrary(newPath);
            if (hInstance)
                break;

            // Clean up this pass.
            free(newPath);
            newPath = NULL;
        }
        if (newPath != NULL)
            free(newPath);
    }

    if (hInstance)
    {
        /*
         * Find and call the MPE library initialization stub (if available)
         * MPE itself does not contain the mpelib_init function
         */
        FARPROC proc = GetProcAddress(hInstance, "mpelib_init");

        *dlmodId = (mpe_Dlmod) hInstance;
        if (proc)
        {
            void (*local_lib_init_ptr)(void**) = (void(*)(void**))proc;
            local_lib_init_ptr(g_mpe_ftable);
        }

        return MPE_SUCCESS;
    }
    else
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL,
                "mpeos_dlmodOpen: failed to load %s, error = %d\n", name,
                GetLastError());

    return MPE_EINVAL;
}

/**
 * The <i>mpeos_dlmodClose()</i> function shall terminate use of the target
 * module.  The target module identifier is the identifier returned from the
 * original library open call.
 *
 * @param dlmodId Is the identifier of the target module.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_dlmodClose(mpe_Dlmod dlmodId)
{
    BOOL b = FreeLibrary((HMODULE) dlmodId);

    if (b)
    {
        return MPE_SUCCESS;
    }

    return MPE_EINVAL;
}

/**
 * The <i>mpeos_dlmodGetSymbol()</i> function shall locate symbol information for
 * target symbol in specified module.  This will be the mechanism for locating a
 * target function within a module.  The target library module is specified by the
 * ID/handle returned from the "open" operation.
 *
 * @param dlmodId Is the identifier of the target module.
 * @param symbol Is a pointer to a name string for the symbol for which to perform
 *          the search/lookup.
 * @param value Is a void pointer for returning the associated value of the target
 *          symbol.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_dlmodGetSymbol(mpe_Dlmod dlmodId, const char *symbol,
        void **value)
{
    FARPROC proc = GetProcAddress((HMODULE) dlmodId, symbol);
    if (proc)
    {
        *value = /*lint -e(611)*/(void*) proc;
        return MPE_SUCCESS;
    }

    return MPE_EINVAL;
}
