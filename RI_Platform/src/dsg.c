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

#include "dsg.h"

static ri_bool fakeDSG = FALSE;

// Logging category
log4c_category_t* dsg_RILogCategory = NULL;
#define RILOG_CATEGORY dsg_RILogCategory

SectionCache dsg;
static int sect = 0;


/**
 * UseFakeDSG: A method that is used to access the UAL DSG state
 *       @returns: the boolean result of the DSG state
 */
ri_bool UseFakeDSG(void)
{
    return fakeDSG;
}

static int parseSections(unsigned char *data, int dataLength)
{
    int section = 0;
    RILOG_DEBUG("%s Entry, (%p, %d)\n", __func__, data, dataLength);
    g_mutex_lock(dsg.AddRemoveSection);

    if (1+dsg.sections >= NSECTS)
    {
        RILOG_FATAL(-1, "%s -- reached maximum number of sections (%d)\n",
                __FUNCTION__, section);
    }
    else
    {
        uint16_t pid = ((data[0] << 8) | data[1]);
        uint16_t len = (((data[3] & 0x0F) << 8) | data[4]);
        uint32_t crc = ((data[5+len] << 24) | (data[6+len] << 16) |
                        (data[7+len] << 8)  |  data[8+len]);
        RILOG_DEBUG("%s -- PID:%X, CRC:%X, length:%d\n",
                __FUNCTION__, pid, crc, len);
        dsg.section[dsg.sections].id = pid;
        dsg.section[dsg.sections].crc = crc;
        dsg.section[dsg.sections].len = dataLength;
        dsg.section[dsg.sections].data = g_try_malloc0(dataLength);

        if (NULL == dsg.section[dsg.sections].data)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        memcpy(dsg.section[dsg.sections].data, data, dataLength);
        dsg.sections++;
        section = 1;
    }

    g_mutex_unlock(dsg.AddRemoveSection);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return section;
}

ri_bool DsgInit()
{
    ri_bool retVal = TRUE;

    // Create our logging category
    dsg_RILogCategory = log4c_category_get("RI.DSG");
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    dsg.AddRemoveSection = g_mutex_new();
    dsg.sections = 0;
    fakeDSG = FALSE; // default value

    fakeDSG = ricfg_getBoolValue("RIPlatform",
                                 "RI.Platform.UalUseSimulatedDsg");
    RILOG_INFO("%s -- fakeDSG = %s\n", __FUNCTION__, boolStr(fakeDSG));

    if (UseFakeDSG())
    {
        if (!LoadData("/dsgdata/", "dsg-files.txt", parseSections))
        {
            RILOG_ERROR("%s didn't load DSG data?!\n", __FUNCTION__);
            retVal = FALSE;
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

void DsgExit()
{
    int i;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    for (i = 0; i < dsg.sections; i++)
    {
        FreeSection(i, &dsg);
    }

    dsg.sections = 0;

    if (NULL != dsg.AddRemoveSection)
    {
        g_mutex_free(dsg.AddRemoveSection);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

int CfdRead(uint8_t *buf, size_t len)
{
    int numRead = 0;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (UseFakeDSG())
    {
        if (sect == 0)
        {
            // re-read the simulated DSG section data...
            DsgExit();
            (void) DsgInit();
        }

        if (sect < GetNumSections(&dsg) &&
                   GetSection(sect, buf, len, &dsg))
        {
            numRead =  dsg.section[sect++].len;
        }
    }
    else
    {
        //numRead =  cfd_Read(buf, len);
    }

    RILOG_TRACE("%s -- Exit numRead: %d\n", __FUNCTION__, numRead);
    return numRead;
}

int CfdDataAvailable(void)
{
    int retVal = 0;

    if (UseFakeDSG())
    {
        if (sect < GetNumSections(&dsg))
        {
            retVal = 1;
        }
        else
        {
            retVal = sect = 0;
        }
    }
    else
    {
        //retVal =  cfd_DataAvailable();
    }

    RILOG_TRACE("%s -- Exit retVal: %d\n", __FUNCTION__, retVal);
    return retVal;
}

