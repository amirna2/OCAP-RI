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

#include <mpeTest.h>

#include "mpetest_filter.h"
#include "mpetest_sys.h"

#include <stdio.h>
#include <ctype.h>

#include <test_utils.h>

#define VTE_DUMPUTILS_LINESIZE (16)

static char asciiLine[VTE_DUMPUTILS_LINESIZE + 1] =
{ '\0' };

static void addChar(unsigned char c, int position)
{
    position = position % VTE_DUMPUTILS_LINESIZE;
    if (isprint(c))
    {
        asciiLine[position] = c;
    }
    else
    {
        asciiLine[position] = '.';
    }
    asciiLine[position + 1] = '\0';
} /* end addChar(unsigned char,int) */

static void asciiPrint(int pos)
{
    while (pos % VTE_DUMPUTILS_LINESIZE)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "   ");
        pos++;
    }
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "   %s", asciiLine);
    asciiLine[0] = '\0';
} /* end asciiPrint(int) */

static int doDump(unsigned char *buffer, int length, int position)
{
    int i;

    for (i = 0; i < length; i++)
    {
        if ((position % VTE_DUMPUTILS_LINESIZE) == 0)
        {
            if (position)
            {
                asciiPrint(0);
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n");
            }
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%06x :", position);
        }
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%02x ", (unsigned int) buffer[i]);
        addChar(buffer[i], position);
        position++;
    }
    return position;
} /* end doDump(unsigned char*,int,int) */

/**
 * vte_dump
 * @param buffer
 * @param length
 */
void vte_dump(unsigned char *buffer, int length)
{
    int pos = doDump(buffer, length, 0);
    asciiPrint(pos);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n");
} /* end vte_dump(unsigned char*,int) */

#ifndef _WINDOWS
void vte_dumpBuffer(Stream_Buffer *buf, int limit)
{
    int pos = 0;
    if (limit == 0)
        limit = 0x7fffffff;
    while (buf && ((limit - pos) > 0))
    {
        pos = doDump((unsigned char *) (buf->buffer), MIN(buf->size, (limit
                - pos)), pos);
        buf = buf->next;
    }

    asciiPrint(pos);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n");
} /* end vte_dumpBuffer(Stream_Buffer*,int) */
#endif
/**
 * vte_dumpSection
 * @param section
 * @param limit
 */
void vte_dumpSection(mpe_FilterSectionHandle section, int limit)
{
    uint8_t buffer[4096];
    uint32_t bytes;
    int i;

    for (i = 0; i < 4096; i += 4)
    {
        *(uint32_t*) &buffer[i] = 0xdeadbeef;
    }

    if (limit == 0 || limit > 4096)
    {
        limit = 4096;
    }

    if (mpe_filterSectionRead(section, 0, limit, 0, buffer, &bytes)
            == MPE_SUCCESS)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Dumping %d bytes\n", bytes);
        vte_dump(buffer, (int) bytes);
    }
    else
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Error Reading Section!");

} /* end vte_dumpSection(mpe_FilterSectionHandle,int) */
