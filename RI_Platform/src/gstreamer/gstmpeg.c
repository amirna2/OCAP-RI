/* GStreamer
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * Copied from assembler.c and fixed.
 */

#include "gstmpeg.h"

#define CRC_QUOTIENT 0x04C11DB7

static guint32 crctab[256];
static guint g_crcInited = 0;

void crc32_init(void)
{
    guint i, j;
    guint32 crc;

    for (i = 0; i < 256; i++)
    {
        crc = i << 24;
        for (j = 0; j < 8; j++)
        {
            if (crc & 0x80000000)
                crc = (crc << 1) ^ CRC_QUOTIENT;
            else
                crc = crc << 1;
        }
        crctab[i] = crc;
    }
}

/*
 * Calculates that CRC code
 */
guint32 crc32_calc(guint8 *data, guint len)
{
    guint32 result = 0xFFFFFFFF;
    guint i;

    if (!g_crcInited)
    {
        crc32_init();
        g_crcInited = 1;
    }

    /*    
     if (len < 4) abort();

     result = *data++ << 24;
     result |= *data++ << 16;
     result |= *data++ << 8;
     result |= *data++;
     result = ~ result;
     len -=4;
     */
    for (i = 0; i < len; i++)
    {
        // copied from mpegtsparse.c:194
        result = (result << 8) ^ crctab[((result >> 24) ^ *data++) & 0xff];
        //result = (result << 8 | *data++) ^ crctab[result >> 24];
    }

    return result;
}
