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

#ifdef CVM_DYNAMIC_LINKING

#include "javavm/include/porting/linker.h"
#include "javavm/include/porting/ansi/errno.h"

#include "javavm/include/utils.h"

#include <mpe_os.h>
#include <mpe_error.h>
#include <mpe_dll.h>
#ifdef CVM_DEBUG
#include <mpe_dbg.h>
#endif

static void* cvmLibHandle = NULL;

/*
 * create a string for the dynamic lib open call by adding the
 * appropriate pre and extensions to a filename and the path
 */
void
CVMdynlinkbuildLibName(char *holder, int holderlen, const char *pname,
        char *fname)
{
    const int pnamelen = pname ? strlen(pname) : 0;
    int extraLength = 0;

    /* Calculate length of extra name pieces */
    extraLength += strlen(JNI_LIB_PREFIX);
    extraLength += strlen(JNI_LIB_SUFFIX);

    /* Quietly truncate on buffer overflow.  Should be an error. */
    /* We use the same library naming convention as JNI */
    if (pnamelen + strlen(fname) + extraLength > holderlen)
    {
        *holder = '\0';
        return;
    }

    if (pnamelen == 0)
    sprintf(holder, "%s%s%s", JNI_LIB_PREFIX, fname, JNI_LIB_SUFFIX);
    else
    sprintf(holder, "%s/%s%s%s", pname, JNI_LIB_PREFIX, fname, JNI_LIB_SUFFIX);
}

/**
 * <i>CVMdynlinkOpen()</i> 
 *
 * Dynamically links in a shared object. This takes a platform-dependent, 
 * absolute pathname to the shared object. Calling code is responsible for 
 * constructing this name.  Returns an abstract "handle" to the shared object 
 * which must be used for later symbol queries, or NULL if an error occurred.
 *
 * @param absolutePathName is a pointer to the target library name
 *
 * @return a handle to the shared object or NULL on error.
 */
void *
CVMdynlinkOpen(const void *absolutePathName)
{
    mpe_Error ec;
    mpe_Dlmod handle;
    const char* path = (const char*)absolutePathName;

    /* Return the CVM library handle */
    if (path == NULL)
    {
        if (cvmLibHandle == NULL)
        {
            path = mpe_envGet("VMDLLPATH");
            if (path != NULL)
            {
                if ((ec = mpe_dlmodOpen(path, &handle)) == MPE_SUCCESS)
                {
                    cvmLibHandle = (void*)handle;
                }
            }
        }
        return cvmLibHandle;
    }

    /* Attempt to open target library. */
    /* maybe check to see if the library is statically linked first*/
    /* otherwise do the dlmodOpen */
    if ((ec = mpe_dlmodOpen(path, &handle)) == MPE_SUCCESS)
    {
        return (void*)handle;
    }

    CVMsetErrno(ec);
    return NULL;
}

/**
 * <i>CVMdynlinkSym()</i> 
 *
 * Finds a function or data pointer in an already opened shared object. Takes 
 * the dynamic shared object handle and a platform dependent symbol as an 
 * argument (typically, but not always, a char*). This function is also 
 * responsible for adding any necessary "decorations" to the symbol name before
 * doing the lookup.
 *
 * @param dsoHandle is the handle to the shared library to search.
 * @param name is the symbol to look up.
 *
 * @return the value associated with the symbol or NULL on error.
 */
void *
CVMdynlinkSym(void *dsoHandle, const void *name)
{
    mpe_Error ec;
    void *value = NULL;

    /* Perform symbol lookup. */
    if ((ec = mpe_dlmodGetSymbol((mpe_Dlmod)dsoHandle, (const char*)name,
                            &value)) != MPE_SUCCESS)
    {
        CVMsetErrno(ec);
        return NULL;
    }

    /* Return symbol value. */
    return value;
}

/**
 * <i>CVMdynlinkClose()</i> 
 *
 * Closes a dynamic shared object. This should probably have the semantics that
 * its symbols are unloaded. This function takes the dynamic shared object 
 * handle from CVMdynlinkOpen.
 *
 * @param dsoHandle is the handle to the shared library.
 *
 */
void
CVMdynlinkClose(void *dsoHandle)
{
    mpe_dlmodClose((mpe_Dlmod)dsoHandle);
}

/* check if library exists */
CVMBool CVMdynlinkExists(const char *name)
{
    void* handle;

    if ((handle = CVMdynlinkOpen(name)) != NULL)
    CVMdynlinkClose(handle);

    return (handle != NULL);
}

#endif /* #defined CVM_DYNAMIC_LINKING */
