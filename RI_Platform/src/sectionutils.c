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


#include <ri_config.h>
#include <ri_log.h>

#include "sectionutils.h"


// Logging category
log4c_category_t* ri_RILogCategory = NULL;
#define RILOG_CATEGORY ri_RILogCategory
#define CHECK_LOGGER() {if (NULL == ri_RILogCategory) ri_RILogCategory = log4c_category_get("RI.SectionUtils"); }

ri_bool DuplicateSection(uint8_t id, uint32_t crc, SectionCache *cache)
{
    CHECK_LOGGER();
    int section = 0;

    if(NULL == cache)
    {
        RILOG_ERROR("%s -- section cache is NULL?!\n", __func__);
        return FALSE;
    }

    for (section = 0; (section < NSECTS); section++)
    {
        if (cache->section[section].id == id &&
            cache->section[section].crc == crc)
            return TRUE;
    }

    return FALSE;
}

ri_bool AddSectionsFromFile(char *filePath,
                            int (*parseSections)(uint8_t *bis, int length))
{
    CHECK_LOGGER();
    int bytesRead, numSections = 0;
    unsigned char *buf;
    FILE *fp = NULL;
    ri_bool retVal = FALSE;

    RILOG_DEBUG("%s -- Entry, (%s);\n", __func__, filePath);

    if (filePath != NULL)
    {
        if (NULL != (fp = fopen(filePath, "rb")))
        {
            if (NULL != (buf = g_try_malloc(MAX_XAIT_FILE_SZ)))
            {
                if (0 != (bytesRead = fread(buf, 1, MAX_XAIT_FILE_SZ - 1, fp)))
                {
                    buf[bytesRead] = 0;

                    // Convert stream to section(s)
                    numSections += parseSections(buf, bytesRead);
                    RILOG_DEBUG("%s -- %d sections in %d bytes from %s\n",
                            __func__, numSections, bytesRead, filePath);
                    retVal = TRUE;
                }
                else
                {
                    RILOG_FATAL(-2, "%s -- couldn't read in %s?!\n",
                            __func__, filePath);
                }

                g_free(buf);
            }
            else
            {
                RILOG_FATAL(-2,
                        "%s -- couldn't allocate enough memory for %s?!\n",
                        __func__, filePath);
            }

            fclose(fp);
        }
        else
        {
            RILOG_FATAL(-2, "%s -- file %s couldn't be opened?!\n",
                    __func__, filePath);
        }
    }
    else
    {
        RILOG_FATAL(-1, "%s -- file path is NULL?!\n", __func__);
    }

    RILOG_DEBUG("%s -- Exit, retVal = %d, filePath = %s\n", __func__,
            retVal, filePath);
    return retVal;
}

ri_bool AddSectionToFile(char *filePath, uint8_t *buf, size_t bytes)
{
    CHECK_LOGGER();
    int bytesWritten;
    FILE *fp = NULL;
    ri_bool retVal = FALSE;

    RILOG_DEBUG("%s Entry (%s, %p, %d)\n", __func__, filePath, buf, bytes);

    if (filePath != NULL)
    {
        if (NULL != (fp = fopen(filePath, "wb+")))
        {
            if (0 != (bytesWritten = fwrite(buf, 1, bytes, fp)))
            {
                RILOG_TRACE("%s -- wrote %d to file %s\n", __func__,
                        bytesWritten, filePath);
                retVal = TRUE;
            }

            fclose(fp);
        }
        else
        {
            RILOG_DEBUG("%s -- file %s couldn't be opened?!\n", __func__,
                    filePath);
        }
    }
    else
    {
        RILOG_DEBUG("%s -- file path is NULL?!\n", __func__);
    }

    return retVal;
}

void FreeSection(int section, SectionCache *cache)
{
    CHECK_LOGGER();
    RILOG_DEBUG("%s(%d, %p);\n", __func__, section, cache);

    if(NULL == cache)
    {
        RILOG_ERROR("%s -- section cache is NULL?!\n", __func__);
        return;
    }

    g_mutex_lock(cache->AddRemoveSection);
    g_free(cache->section[section].data);
    cache->section[section].data = NULL;
    cache->section[section].id = 0;
    cache->section[section].crc = 0;
    cache->section[section].len = 0;
    g_mutex_unlock(cache->AddRemoveSection);
}

int GetSection(int sect, uint8_t *buf, int bufsize, SectionCache *cache)
{
    CHECK_LOGGER();
    int retVal = 0;
    RILOG_DEBUG("%s Entry, (%d, %p, %d);\n", __func__, sect, buf, bufsize);

    if (NULL == buf)
    {
        RILOG_ERROR("%s NULL inbound buffer?!\n", __func__);
    }
    else if(NULL == cache)
    {
        RILOG_ERROR("%s -- section cache is NULL?!\n", __func__);
    }
    else
    {
        g_mutex_lock(cache->AddRemoveSection);

        if (NULL == cache->section[sect].data)
        {
            RILOG_ERROR("%s NULL section data?!\n", __func__);
        }

        if ((size_t) bufsize > cache->section[sect].len)
        {
            memcpy(buf, cache->section[sect].data, cache->section[sect].len);
            retVal = cache->section[sect].len;
        }
        else
        {
            RILOG_ERROR("%s section length too long: %d\n", __func__,
                        cache->section[sect].len);
        }

        g_mutex_unlock(cache->AddRemoveSection);
    }

    RILOG_TRACE("%s -- Exit\n", __func__);
    return retVal;
}

int GetNumSections(SectionCache *cache)
{
    CHECK_LOGGER();
    int sects = 0;

    if(NULL == cache)
    {
        RILOG_ERROR("%s -- section cache is NULL?!\n", __func__);
    }
    else
    {
        g_mutex_lock(cache->AddRemoveSection);
        sects = cache->sections;
        g_mutex_unlock(cache->AddRemoveSection);
        RILOG_DEBUG("%s -- sections = %d\n", __func__, sects);
    }
    return sects;
}

ri_bool LoadData(char *dataDir, char *fileName,
                 int (*parseSections)(uint8_t *bis, int length))
{
    ri_bool retVal = FALSE;
    char *path, file[FILENAME_MAX], fileUrl[FILENAME_MAX];
    FILE *fp = NULL;

    CHECK_LOGGER();
    RILOG_DEBUG("%s -- Entry\n", __FUNCTION__);

    if (NULL != (path = ricfg_getValue("RIPlatform",
            "RI.Headend.resources.directory")))
    {
        if (strlen(path) + strlen(fileName) + strlen(dataDir) < FILENAME_MAX)
        {
            RILOG_DEBUG("%s -- path = %s\n", __FUNCTION__, path);
            sprintf(file, "%s/%s", path, fileName);

           if (NULL != (fp = fopen(file, "r")))
           {
               while (NULL != fgets(file, sizeof(file), fp))
               {
                   // skip over empty lines
                   if (('\n' != file[0]) && ('\r' != file[0]))
                   {
                       sprintf(fileUrl, "%s%s%s", path, dataDir, file);
                       int urlLen = strlen(fileUrl);

                       // Remove newline char
                       if (fileUrl[urlLen - 1] == '\n' ||
                           fileUrl[urlLen - 1] == '\r')
                       {
                           fileUrl[urlLen - 1] = '\0';
                       }
#ifndef WIN32
                       // Remove newline char
                       if (fileUrl[urlLen - 2] == '\n' ||
                           fileUrl[urlLen - 2] == '\r')
                       {
                           fileUrl[urlLen - 2] = '\0';
                       }
#endif
                       if (!AddSectionsFromFile(fileUrl, parseSections))
                       {
                           RILOG_ERROR("%s couldn't add OOB data from %s\n",
                                   __FUNCTION__, fileUrl);
                       }
                   }
               }

               fclose(fp);
               retVal = TRUE;
           }
           else
           {
               RILOG_ERROR("%s could not open %s/%s?!\n",
                       __FUNCTION__, path, fileName);
           }
        }
        else
        {
            RILOG_ERROR("%s path is too long (%s)\n", __FUNCTION__, path);
        }
    }
    else
    {
        RILOG_ERROR("%s could not read resource path from config?!\n",
                __FUNCTION__);
    }

    RILOG_DEBUG("%s = %s -- Exit\n", boolStr(retVal), __FUNCTION__);
    return retVal;
}

static unsigned long mpegCrcTable[256];
static unsigned long mpegCrcTablePoly = 0x04C11DB7L;

//
// generates the MPEG (big endian) CRC32 table using the 
// polynonial: x32+x26+x23+x22+x16+x12+x11+x10+x8+x7+x5+x4+x2+x+1
//
static void generateMpegCrcTable(void)
{
    int i = 0, j = 0;
    unsigned long crc = 0;

    RILOG_INFO("%s using polynomial: 0x%08lX\n", __func__, mpegCrcTablePoly);

    while (i < 256)
    {
        crc = i << 24;

        for (j = 0; j < 8; j++)
        {
            if (crc & 0x80000000L)
            {
                crc = (crc << 1) ^ mpegCrcTablePoly;
            }
            else
            {
                crc <<= 1;
            }
        }

        mpegCrcTable[i++] = crc;
    }
}

//
// calculates the MPEG (big endian) CRC32 for the given buffer
// using the table generated above
//
unsigned long mpegCrc32(unsigned char *datap, int length)
{
    unsigned long sum = 0xFFFFFFFFL;

    if ((mpegCrcTable[0] != 0) || (mpegCrcTable[1] != mpegCrcTablePoly))
    {
        generateMpegCrcTable();
    }

    for ( ; length > 0; length--, datap++)
    {
        sum = mpegCrcTable[((sum >> 24) ^ *datap) & 0xFF] ^ (sum << 8);
    }

    RILOG_DEBUG("%s returning: 0x%08lX\n", __func__, sum);
    return sum;
}

