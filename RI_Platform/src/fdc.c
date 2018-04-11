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

#include "fdc.h"

static ri_bool g_removeDuplicateFdcTables = FALSE;

// Variable PID values
static int g_pmtPid = -1;
static int g_dsmccPid = -1;

// Logging category
log4c_category_t* fdc_RILogCategory = NULL;

// Use UAL category for logs in this file
#define RILOG_CATEGORY fdc_RILogCategory

SectionCache g_fdc;

SectionCache* GetFdcSectionCache(void)
{
    return &g_fdc;
}

/**
 * RemoveDuplicateFdcTables: A method that is used to access dup FDC state
 *                     @returns: the boolean result of the dup FDC state
 */
ri_bool RemoveDuplicateFdcTables(void)
{
    return g_removeDuplicateFdcTables;
}

/**
 * getPmtPid:  Parse the given PAT section and return the PID of the first
 *             PMT found.  For OOB carousels there can be only one program.
 *
 * @param: pat - the PAT section byte data
 * @param: sectLen - the length of the PAT section in bytes
 * @returns: the PMT PID or -1 if it could not be determined
 */
static int getPmtPid(unsigned char *pat, int sectLen)
{
    int PAT_HEADER_SIZE = 8; // Program loop starts after this
    int index = PAT_HEADER_SIZE;
    int crcIndex = sectLen - 4; // CRC index
    int pmtPid = -1;
    
    // Make sure this is a PAT
    if (*pat != PAT_TID)
    {
        RILOG_ERROR("%s PAT section (%p) does not have correct table ID (%x)!\n",
                    __FUNCTION__, pat, *pat);
        return -1;
    }
    
    // Loop through programs looking for first PMT PID
    while (index < crcIndex)
    {
        uint16_t program, pid;
        
        if ((index + 4) > crcIndex) // Not enough data
        {
            break;
        }
        
        program = ((pat[index] & 0x00FF) << 8) |
                   (pat[index+1] & 0x00FF);
        index += 2;
        pid = ((pat[index] & 0x001F) << 8) |
               (pat[index+1] & 0x00FF);
        index += 2;

        if (program != 0)
        {
            pmtPid = pid;
            break;
        }
    }

    RILOG_INFO("%s: OOB carousel PMT PID is %d\n", __FUNCTION__, pmtPid);

    return pmtPid;
}

/**
 * getPmtPid:  Parse the given PMT section and return the PID associated with
 *             the DSMCC elementary stream
 *
 * @param: pat - the PMT section byte data
 * @param: sectLen - the length of the PMT section in bytes
 * @returns: the DSMCC PID or -1 if it could not be determined
 */
static gint getCarouselPid(unsigned char *pmt, int sectLen)
{
    int PMT_HEADER_SIZE = 10; // program_info_length starts after this
    int index = PMT_HEADER_SIZE;
    int crcIndex = sectLen - 4; // CRC index
    gint carouselPid = -1;
    
    // Make sure this is a PAT
    if (*pmt != PMT_TID)
    {
        RILOG_ERROR("%s PMT section (%p) does not have correct table ID (%x)!\n",
                    __FUNCTION__, pmt, *pmt);
        return -1;
    }

    // Do we have enough data to read the descriptor length
    if ((index + 2) > crcIndex)
    {
        RILOG_ERROR("%s PAT section (%x) not long enough (%d) to find first program!\n",
                    __FUNCTION__, *pmt, sectLen);
        return -1;
    }

    // Skip over PMT descriptors
    index += (((pmt[index] & 0x0F) << 8) | (pmt[index+1] & 0xFF)) + 2;
    
    // Loop through programs looking for first PMT PID
    while (index < crcIndex)
    {
        uint8_t streamType;
        
        // Ensure that we have enough data to read this ES description
        if (index + 5 >= crcIndex)
        {
            break;
        }

        // If this ES is our carousel, then just return the PID
        streamType = pmt[index++];
        if (streamType == DSMCC_ES_TYPEA || streamType == DSMCC_ES_TYPEB ||
            streamType == DSMCC_ES_TYPEC || streamType == DSMCC_ES_TYPED)
        {
            carouselPid = (gint)(((pmt[index] & 0x1F) << 8) | (pmt[index+1] & 0xFF));
            break;
        }
        
        // Otherwise, skip over this ES
        index += 2;
        index += (((pmt[index] & 0x0F) << 8) | (pmt[index+1] & 0xFF)) + 2;
    }

    RILOG_INFO("%s: OOB carousel DSMCC PID is %d\n", __FUNCTION__, carouselPid);

    return carouselPid;
}

/**
 * getSectionPid:  Returns the PID that should be associated with the
 *                 given section
 *
 * @param: section - the section data
 * @param: sectLen - the section length
 * @returns: the PID that should be associated with the given section.
 *           returns -1 if the PID could not be determined
 */
static int getSectionPid(unsigned char *section, int sectLen)
{
    unsigned char tableID = section[0];
    int pid;

    // Check to see if we can use this table to calculate one of our
    // variable PIDs
    if (g_pmtPid == -1 && tableID == PAT_TID)
    {
        g_pmtPid = getPmtPid(section, sectLen);
    }
    else if (g_dsmccPid == -1 && tableID == PMT_TID)
    {
        g_dsmccPid = getCarouselPid(section, sectLen);
    }
    
    // Determine PID value
    switch (tableID)
    {
    case XAIT_TID:    // Standard 0x1FFC tables
    case NIT_TID:
    case NTT_TID:
    case SVCT_TID:
    case STT_TID:
    case MGT_TID:
    case LVCT_TID:
    case RRT_TID:
    case EAS_TID:
        pid = OOB_PID;
        break;

    case DSMCC_TID_MPDATA: // OOB Carousel tables
    case DSMCC_TID_UNMSG:
    case DSMCC_TID_DDMSG:
    case DSMCC_TID_STRMDESC:
    case DSMCC_TID_PRIV:
    case DSMCC_TID_ADDR:
        pid = g_dsmccPid;
        break;
        
    case PAT_TID:          // OOB PAT tables
        pid = 0;
        break;
        
    case PMT_TID:          // OOB PMT tables
        pid = g_pmtPid;
        break;
        
    default:
        pid = -1;
        break;
    }

    return pid;
}

/**
 * getSection:  Get the next section from the provided input stream...
 *              The returned section has a prepended PID as the end consumer
 *              (section_filter) expects this section format
 *             
 * @param: bis - the byte input stream to obtain the section from
 * @param: bisLen - the length of the inbound byte input stream
 * @param: offset - the distance into the inbound byte input stream we are
 *                  currently operating at...
 * @param: sectLen - the length of the section read from the input stream
 *                   plus the prepended PID header
 * @returns: the pointer to the section data that must be freed
 */
static unsigned char *
getSection(unsigned char *bis, int bisLen, int offset, int *sectLen)
{
    int sectionLength = 0;
    int remainingLength = 0;
    unsigned char *section = NULL;
    RILOG_DEBUG("%s Entry, (%p, %d, %d);\n", __FUNCTION__, bis, bisLen, offset);

    if (NULL != bis && 0 != bisLen)
    {
        // Extract length in 12 bits, 4 of byte 1 and 8 in byte 2
        sectionLength = ((bis[offset + 1] & 0x000F) << 8) + bis[offset + 2];
        RILOG_DEBUG("%s -- Table ID %X starting packet at %d, length %d\n",
                __FUNCTION__, bis[offset], offset, sectionLength);

        // Verify that binary data remaining in the stream is at least this long
        remainingLength = bisLen - offset;

        // the current section length should include the table header
        // (tid, flags, and length) and the prepended PID
        *sectLen = sectionLength + 5; // include tid, flags, length, and PID
        RILOG_DEBUG(
                "%s Total section length: %d bytes, remaining in file: %d\n",
                __FUNCTION__, *sectLen, remainingLength);

        // don't consider the PID in the remaining length calculation as
        // it isn't in the input byte stream...
        if ((*sectLen - 2) <= remainingLength)
        {
            section = g_try_malloc(*sectLen);

            if (section)
            {
                int pid = getSectionPid(&bis[offset], (*sectLen - 2));
                if (pid == -1)
                {
                    pid = 0xFFFF;
                }

                // prepend the PID for the section_filter consumer...
                section[0] = (pid & 0xFF00) >> 8;
                section[1] = (pid & 0x00FF);

                // don't consider the PID in the mem copy of the section data
                // as it isn't in the input byte stream...
                memcpy(&section[2], &bis[offset], (*sectLen - 2));
            }
            else
            {
                RILOG_ERROR("%s -- allocate of section (%d bytes) failed!\n",
                        __FUNCTION__, *sectLen);
            }
        }
        else
        {
            RILOG_ERROR(
                    "%s Total section length: %d exceeds data available: %d\n",
                    __FUNCTION__, *sectLen, remainingLength);
        }
    }
    else
    {
        RILOG_ERROR("%s -- NULL bis or 0 bisLen?!\n", __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return section;
}

/**
 * getSection:  Read in all the sections from the provided input stream...
 *             
 * @param: bis - the byte input stream to obtain sections from
 * @param: length - the length of the inbound byte input stream
 * @returns: the number of sections read
 */
static int parseSections(unsigned char *bis, int length)
{
    int offset = 0;
    int section = 0;
    int sectionLength = 0;
    unsigned char *buf = NULL;
    unsigned char tid = 0;
    uint32_t crc = 0;

    RILOG_DEBUG("%s Entry, (%p, %d)\n", __func__, bis, length);
    g_mutex_lock(g_fdc.AddRemoveSection);

    for (section = 0; (section + g_fdc.sections < NSECTS)
            && (length - offset > 8);)
    {
        if (NULL != (buf = getSection(bis, length, offset, &sectionLength)))
        {
            // we got back a pointer to a buffer that holds a section with
            // a prepended PID.  The returned sectionLength includes
            // the additional 2 bytes of PID which should not be used in
            // the offset adjustment and the PID was not in the bis...
            offset += (sectionLength - 2);
            tid = buf[2];  // TID is after the PID
            crc = 0;
            crc |= buf[sectionLength - 4] << 24;
            crc |= buf[sectionLength - 3] << 16;
            crc |= buf[sectionLength - 2] << 8;
            crc |= buf[sectionLength - 1];

            if (RemoveDuplicateFdcTables() &&
                DuplicateSection(tid, crc, &g_fdc))
            {
                RILOG_DEBUG("%s -- skip duplicate TID:%X, CRC:%X\n",
                        __FUNCTION__, tid, crc);
            }
            else
            {
                RILOG_DEBUG("%s -- Table ID:%X, CRC:%X, length:%d\n",
                        __FUNCTION__, tid, crc, sectionLength - 2);
                g_fdc.section[section + g_fdc.sections].id = tid;
                g_fdc.section[section + g_fdc.sections].crc = crc;
                g_fdc.section[section + g_fdc.sections].data =
                        (uint8_t *) g_base64_encode(buf, sectionLength);
                g_fdc.section[section + g_fdc.sections].len = strlen((const char *)
                        g_fdc.section[section + g_fdc.sections].data);
                RILOG_DEBUG("%s -- g_base64_encode() = %s\n", __FUNCTION__,
                        g_fdc.section[section + g_fdc.sections].data);
                section++;
            }

            g_free(buf);
        }
        else
        {
            g_fdc.section[section + g_fdc.sections].data = NULL;
            RILOG_FATAL(-2, "%s -- getSection() failure - corrupt file?\n",
                    __FUNCTION__);
        }
    }

    if (section + g_fdc.sections >= NSECTS)
    {
        RILOG_FATAL(-1, "%s -- reached maximum number of sections (%d)\n",
                __FUNCTION__, section);
    }
    else
    {
        g_fdc.sections += section;
    }

    g_mutex_unlock(g_fdc.AddRemoveSection);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return section;
}

ri_bool FdcInit()
{
    ri_bool retVal = TRUE;

    // Create our logging category
    fdc_RILogCategory = log4c_category_get("RI.FDC");
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Initialize our section cache
    g_fdc.AddRemoveSection = g_mutex_new();
    g_fdc.sections = 0;

    g_removeDuplicateFdcTables = ricfg_getBoolValue("RIPlatform",
                                        "RI.Platform.RemoveDuplicateFdcTables");
    RILOG_TRACE("%s -- removeDuplicateFdcTables = %s\n", __FUNCTION__,
               boolStr(g_removeDuplicateFdcTables));

    if (!LoadData("/fdcdata/", "fdc-files.txt", parseSections))
    {
        RILOG_ERROR("%s didn't load FDC data?!\n", __FUNCTION__);
        retVal = FALSE;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

void FdcExit()
{
    int i;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    for (i = 0; i < g_fdc.sections; i++)
    {
        FreeSection(i, &g_fdc);
    }

    g_fdc.sections = 0;

    if (NULL != g_fdc.AddRemoveSection)
    {
        g_mutex_free(g_fdc.AddRemoveSection);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

