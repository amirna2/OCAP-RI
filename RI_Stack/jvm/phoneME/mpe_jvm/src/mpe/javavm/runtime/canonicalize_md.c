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

/*
 * Pathname canonicalization for MPE file systems
 */

#include <mpe_os.h>
#include <mpe_file.h>
#include <string.h>
#include "javavm/include/porting/path.h"
#include "javavm/include/porting/ansi/errno.h"

#define MPE_MEM_DEFAULT MPE_MEM_JVM

/* Note: The comments in this file use the terminology
 defined in the java.io.File class */

/**
 * <i>collapsible</i>
 *
 * Check the given name sequence to see if it can be further collapsed.
 *
 * @return zero if not, otherwise return the number of names in the sequence.
 */

static int collapsible(char *names)
{
    char *p = names;
    int dots = 0, n = 0;

    while (*p)
    {
        if ((p[0] == '.') && ((p[1] == '\0') || (p[1] == '/') || ((p[1] == '.')
                && ((p[2] == '\0') || (p[2] == '/')))))
        {
            dots = 1;
        }
        n++;
        while (*p)
        {
            if (*p == '/')
            {
                p++;
                break;
            }
            p++;
        }
    }
    return (dots ? n : 0);
}

/**
 * <i>splitNames</i>
 *
 * Split the names in the given name sequence, replacing slashes with 
 * nulls and filling in the given index array
 *
 */
static void splitNames(char *names, char **ix)
{
    char *p = names;
    int i = 0;

    while (*p)
    {
        ix[i++] = p++;
        while (*p)
        {
            if (*p == '/')
            {
                *p++ = '\0';
                break;
            }
            p++;
        }
    }
}

/**
 * <i>collapse</i>
 *
 * Collapse "." and ".." names in the given path wherever possible.
 * A "." name may always be eliminated; a ".." name may be eliminated if it
 * follows a name that is neither "." nor "..".  This is a syntactic operation
 * that performs no filesystem queries, so it should only be used to cleanup
 * after invoking the realpath() procedure.
 *
 * @return  0 on success and -1 on failure.
 *//* Join the names in the given name sequence, ignoring names whose index
 entries have been cleared and replacing nulls with slashes as needed */

static void joinNames(char *names, int nc, char **ix)
{
    int i;
    char *p;

    for (i = 0, p = names; i < nc; i++)
    {
        if (!ix[i])
            continue;
        if (i > 0)
        {
            p[-1] = '/';
        }
        if (p == ix[i])
        {
            p += strlen(p) + 1;
        }
        else
        {
            char *q = ix[i];
            while ((*p++ = *q++) != 0)
                ;
        }
    }
    *p = '\0';
}

/**
 * <i>collapse</i>
 *
 * Collapse "." and ".." names in the given path wherever possible.
 * A "." name may always be eliminated; a ".." name may be eliminated if it
 * follows a name that is neither "." nor "..".  This is a syntactic operation
 * that performs no filesystem queries, so it should only be used to cleanup
 * after invoking the realpath() procedure.
 *
 * @return  0 on success and -1 on failure.
 */
static int collapse(char *path)
{
    char *names = (path[0] == '/') ? path + 1 : path; /* Preserve first '/' */
    int nc;
    char **ix;
    int i, j;
    mpe_Error result;

    CVMconsolePrintf("Here1\n");

    nc = collapsible(names);
    if (nc < 2)
        return 0; /* Nothing to do */

    CVMconsolePrintf("Here2\n");
    result = mpe_memAlloc(nc * sizeof(char *), (void **) ix);
    if (result != MPE_SUCCESS)
    {
        CVMsetErrno( MPE_ENOMEM);
        return -1;
    }

    CVMconsolePrintf("Here3\n");
    splitNames(names, ix);

    CVMconsolePrintf("Here4\n");
    for (i = 0; i < nc; i++)
    {
        int dots = 0;

        CVMconsolePrintf("Here5\n");
        /* Find next occurrence of "." or ".." */
        do
        {
            char *p = ix[i];
            if (p[0] == '.')
            {
                if (p[1] == '\0')
                {
                    dots = 1;
                    break;
                }
                if ((p[1] == '.') && (p[2] == '\0'))
                {
                    dots = 2;
                    break;
                }
            }
            i++;
        } while (i < nc);
        if (i >= nc)
            break;

        CVMconsolePrintf("Here6\n");
        /* At this point i is the index of either a "." or a "..", so take the
         appropriate action and then continue the outer loop */
        if (dots == 1)
        {
            /* Remove this instance of "." */
            ix[i] = 0;
        }
        else
        {
            /* If there is a preceding name, remove both that name and this
             instance of ".."; otherwise, leave the ".." as is */
            for (j = i - 1; j >= 0; j--)
            {
                if (ix[j])
                    break;
            }
            if (j < 0)
                continue;
            ix[j] = 0;
            ix[i] = 0;
        }
        /* i will be incremented at the top of the loop */
    }
    CVMconsolePrintf("Here7\n");

    joinNames(names, nc, ix);
    CVMconsolePrintf("Here8\n");
    mpe_memFree(ix);
    return 0;
}

/**
 * <i>CVMcanonicalize</i>
 *
 * Convert a pathname to canonical form.  The input path is assumed to contain
 * no duplicate slashes. 
 *
 * @return  0 on success and -1 on failure.
 */
#include <unistd.h>
int CVMcanonicalize(const char *original, char *resolved, int len)
{
    /* all we can really do is check to see if the file really exists
     * and collapse any extra "." and ".." since the mpe file system
     * does not have symbolic links.
     */
    mpe_Dir handle;
    mpe_File file;

    if (len < CVM_PATH_MAXLEN || resolved == NULL)
    {
        CVMsetErrno( MPE_EINVAL);
        return -1;
    }

    if (strlen(original) > CVM_PATH_MAXLEN)
    {
        CVMsetErrno( ENAMETOOLONG);
        return -1;
    }

    strncpy(resolved, original, len);
    //return collapse(resolved);
    return 0;
}
