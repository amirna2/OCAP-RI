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

#include <string.h>

#include <mpeos_file.h>
#include <mpe_dbg.h>
#include <mpeos_util.h>
#include <mpeos_dbg.h>

#include <unistd.h>

extern mpeos_filesys_ftable_t port_fileHDFTable;

/* port-specific default-system-directory lookup */
mpe_FileError mpeos_filesysGetDefaultDir(char *buf, uint32_t bufSz)
{
    static const char* defaultSysDir = NULL;

    /* insure that we have a valid caller's buffer                            **
     ** otherwise, they are just using this call to get the size of the string */
    if (buf == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    if (defaultSysDir == NULL)
    {
        const char* sysDirEnv;
        if ((sysDirEnv = mpeos_envGet(MPE_FS_ENV_DEFAULT_SYS_DIR)) == NULL) // try to get it from FS.DEFSYSDIR in mpeenv.ini
        {
            sysDirEnv = MPE_FS_DEFAULT_SYS_DIR; // else use hard coded value from mpeos_file.h
        }

        /* check for special case when default-system-directory is "." */
        if (strcmp(sysDirEnv, ".") == 0)
        {
            char cwd[MPE_FS_MAX_PATH];

            /* get absolute path for our current-working-directory */
            (void) getcwd(cwd, MPE_FS_MAX_PATH);
            defaultSysDir = strdup(cwd);
        }
        else
        {
            defaultSysDir = strdup(sysDirEnv);
        }
    }
    if (strlen(defaultSysDir) >= bufSz)
        return MPE_FS_ERROR_OUT_OF_MEM;

    strcpy(buf, defaultSysDir);
    return MPE_FS_ERROR_SUCCESS;
}

/* initialize all of the file system drivers for this port */
void mpeos_filesysInit(void(*mpe_filesysRegisterDriver)(
        const mpeos_filesys_ftable_t *, const char *))
{
    /*
     * Default Filesystem:  hard-disk
     */
    (*mpe_filesysRegisterDriver)(&port_fileHDFTable, "");
}
