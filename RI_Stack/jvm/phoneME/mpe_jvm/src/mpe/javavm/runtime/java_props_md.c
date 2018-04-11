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

#include <mpe_os.h>
#include <mpe_file.h>
#include "jvm.h"
#include "jni_util.h"

#include "locale_str.h"
#include "javavm/include/porting/java_props.h"
#include "generated/javavm/include/build_defs.h"
#include "javavm/include/porting/endianness.h"

#define MPE_MEM_DEFAULT MPE_MEM_JVM

CVMBool CVMgetJavaProperties(java_props_t *sprops)
{
    unsigned int endianTest = 0xff000000;
    char buf[MPE_FS_MAX_PATH];
    mpe_FileError ec;
    mpe_FileInfo info;

    if (sprops->user_dir)
    {
        return CVM_FALSE;
    }

    /* tmp dir */
    sprops->tmp_dir = NULL;

    /* Printing properties */
    sprops->printerJob = NULL;

    /* Java 2D properties */
    sprops->graphics_env = NULL;
    sprops->awt_toolkit = NULL;

    sprops->font_dir = NULL;
    sprops->cpu_isalist = NULL;

    /* endianness of platform */
    if (((char*) (&endianTest))[0] != 0)
        sprops->cpu_endian = "big";
    else
        sprops->cpu_endian = "little";

    /* os properties */
    sprops->os_name = NULL;
    sprops->os_version = NULL;

    sprops->language = "eng";
    sprops->encoding = "ISO8859_1";

#if (CVM_ENDIANNESS == CVM_LITTLE_ENDIAN)
    sprops->unicode_encoding = "UnicodeLittle";
#else
    sprops->unicode_encoding = "UnicodeBig";
#endif

    /* Use the default sys dir to set initial values for
     user.home and user.dir */
    info.buf = buf;
    info.size = MPE_FS_MAX_PATH;
    ec = mpe_fileGetStat(NULL, MPE_FS_STAT_DEFAULTSYSDIR, &info);

    /* user properties */
    sprops->user_name = NULL;
    if (ec == MPE_SUCCESS)
    {
        sprops->user_home = strdup(info.buf);
        sprops->user_dir = strdup(info.buf);
    }
    else
    {
        sprops->user_home = NULL;
        sprops->user_dir = NULL;
    }

    sprops->file_separator = MPE_FS_SEPARATION_STRING;
    sprops->path_separator = PATH_SEPARATOR_CHAR; /* Defined in port-specific defs.mk */
    sprops->line_separator = "\n";

    /* User TIMEZONE */
    sprops->timezone = "";

    return CVM_TRUE;
}

/*
 * Free up memory allocated by CVMgetJavaProperties().
 */
void CVMreleaseJavaProperties(java_props_t *sprops)
{
}
