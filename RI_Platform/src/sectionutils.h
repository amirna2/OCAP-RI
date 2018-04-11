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

#ifndef _SECTIONUTILS_H_
#define _SECTIONUTILS_H_

#include <glib.h>
#include <ri_types.h>

#define RCVBUFSIZE       (4*1024)
#define NSECTS           1024
#define MAX_XAIT_FILE_SZ (2*NSECTS*1024)
#define MAXURLLEN        1024
#define ADDRLEN          48           // we only support 48 chars in addresses

#define ConvertToBuffer(buf, offset, object, size) \
{ \
    int i; \
    for(i = 0; i < size; i++) \
    buf[offset+i] = (object >> (8*(size-1-i))); \
}

#define ConvertFromBuffer(buf, offset, object, size) \
{ \
    int i; \
    for(object = i = 0; i < size; i++) \
    { \
        object <<= 8; \
        object |= (buf[offset+i]); \
    } \
}

#define ConvertFromAsciiBuffer(buf, offset, object, size, radix) \
{ \
    int i; \
    for(object = i = 0; i < size; i++) \
    { \
        unsigned char c = buf[offset+i]; \
        if(c >= '0' && c <= '9') \
        { \
            object *= radix; \
            object |= (buf[offset+i] - '0'); \
        } \
        else if(c >= 'A' && c <= 'F') \
        { \
            object *= radix; \
            object |= (buf[offset+i] - 'A' + 10); \
        } \
        if(c >= 'a' && c <= 'f') \
        { \
            object *= radix; \
            object |= (buf[offset+i] - 'a' + 10); \
        } \
        if(c == '\r' || c == '\n' || c == '\0') \
            break; \
    } \
}

/**
 * Stream: This typedef'd struct contains the data required for stream delivery
 */
typedef struct stream
{
    long frequency;
    char modulation[16];
    int program;
    char destinationAddress[ADDRLEN];
    int destinationPort;
    char srcUrl[MAXURLLEN];
} Stream;

typedef struct
{
    uint8_t id;
    uint8_t *data;
    uint32_t crc;
    size_t len;
} Section;

typedef struct
{
    int sections;
    Section section[NSECTS];
    GMutex *AddRemoveSection;
} SectionCache;


ri_bool DuplicateSection(uint8_t id, uint32_t crc, SectionCache *cache);
ri_bool AddSectionsFromFile(char *filePath, int (*)(uint8_t *bis, int length));
ri_bool AddSectionToFile(char *filePath, uint8_t *buf, size_t bytes);
void FreeSection(int section, SectionCache *cache);
ri_bool GetSection(int section, uint8_t *buf, int size, SectionCache *cache);
int GetNumSections(SectionCache *cache);
ri_bool LoadData(char *dir, char *file, int (*)(uint8_t *bis, int length));
unsigned long mpegCrc32(unsigned char *datap, int length);

#endif

