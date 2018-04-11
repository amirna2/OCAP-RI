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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

#define _REMAP_IMPL_C "$Rev: 141 $"

//#define DEBUG_PAT_AND_PMT

#include <glib.h>

#include <stdlib.h>
#include "RemapImpl.h"
#include "ri_log.h"

log4c_category_t* remap_RILogCategory = NULL;
#define RILOG_CATEGORY remap_RILogCategory

static unsigned long crcTable[256];
static RemapBoolean crcInitialized = RemapFalse;

static void InitCrcTable(void)
{
    if (!crcInitialized)
    {
        unsigned long dwPoly = 0x04c11db7;
        unsigned long crc = 0x80000000;
        unsigned long i, j;

        crcTable[0] = 0;

        for (i = 1; i < 256; i <<= 1)
        {
            crc = (crc << 1) ^ ((crc & 0x80000000) ? dwPoly : 0);
            for (j = 0; j < i; j++)
            {
                crcTable[i + j] = crc ^ crcTable[j];
            }
        }
        crcInitialized = RemapTrue;
    }
}

static inline void InitPatCrc(RemapHandle remapHandle)
{
    remapHandle->patCrc = 0xFFFFFFFF;
}

static inline void NextPatCrc(RemapHandle remapHandle, unsigned char aByte)
{
    remapHandle->patCrc = ((remapHandle->patCrc << 8)
            ^ crcTable[((remapHandle->patCrc >> 24) ^ aByte)]);
}

static inline unsigned long DonePatCrc(RemapHandle remapHandle)
{
    return remapHandle->patCrc;
}

static inline void InitPmtCrc(RemapHandle remapHandle)
{
    remapHandle->pmtCrc = 0xFFFFFFFF;
}

static inline void NextPmtCrc(RemapHandle remapHandle, unsigned char aByte)
{
    remapHandle->pmtCrc = ((remapHandle->pmtCrc << 8)
            ^ crcTable[((remapHandle->pmtCrc >> 24) ^ aByte)]);
}

static inline unsigned long DonePmtCrc(RemapHandle remapHandle)
{
    return remapHandle->pmtCrc;
}

const char * DescriptorToString(unsigned char descriptor)
{
    switch (descriptor)
    {
    case 0:
    case 1:
        return "Reserved";
    case 2:
        return "video_stream";
    case 3:
        return "audio_stream";
    case 4:
        return "hierarchy";
    case 5:
        return "registration";
    case 6:
        return "data_stream_alignment";
    case 7:
        return "target_background_grid";
    case 8:
        return "Video_window";
    case 9:
        return "CA";
    case 10:
        return "ISO_639_language";
    case 11:
        return "System_clock";
    case 12:
        return "Multiplex_buffer_utilization";
    case 13:
        return "Copyright";
    case 14:
        return "Maximum_bitrate";
    case 15:
        return "Private_data_indicator";
    case 16:
        return "Smoothing_buffer";
    case 17:
        return "STD";
    case 18:
        return "IBP";
    case 19:
    case 20:
    case 21:
    case 22:
    case 23:
    case 24:
    case 25:
    case 26:
        return "ISO/IEC 13818-6";
    case 27:
        return "MPEG-4_video";
    case 28:
        return "MPEG-4_audio";
    case 29:
        return "IOD";
    case 30:
        return "SL";
    case 31:
        return "FMC";
    case 32:
        return "External_ES_ID";
    case 33:
        return "MuxCode";
    case 34:
        return "FmxBufferSize";
    case 35:
        return "MultiplexBuffer";
    }

    if ((36 <= descriptor) && (descriptor <= 63))
        return "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 Reserved";

    return "User Private";
}

const char * StreamTypeToString(unsigned char streamType)
{
    switch (streamType)
    {
    case 0x00:
        return "ITU-T | ISO/IEC Reserved";
    case 0x01:
        return "ISO/IEC 11172 Video";
    case 0x02:
        return "ITU-T Rec. H.262 | ISO/IEC 13818-2 Video or ISO/IEC 11172-2 constrained parameter video stream";
    case 0x03:
        return "ISO/IEC 11172 Audio";
    case 0x04:
        return "ISO/IEC 13818-3 Audio";
    case 0x05:
        return "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 private_sections";
    case 0x06:
        return "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 PES packets containing private data";
    case 0x07:
        return "ISO/IEC 13522 MHEG";
    case 0x08:
        return "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 Annex A DSM-CC";
    case 0x09:
        return "ITU-T Rec. H.222.1";
    case 0x0A:
        return "ISO/IEC 13818-6 type A";
    case 0x0B:
        return "ISO/IEC 13818-6 type B";
    case 0x0C:
        return "ISO/IEC 13818-6 type C";
    case 0x0D:
        return "ISO/IEC 13818-6 type D";
    case 0x0E:
        return "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 auxiliary";
    case 0x0F:
        return "ISO/IEC 13818-7 Audio with ADTS transport syntax";
    case 0x10:
        return "ISO/IEC 14496-2 Visual";
    case 0x11:
        return "ISO/IEC 14496-3 Audio with the LATM transport syntax as defined in ISO/IEC 14496-3 / AMD 1";
    case 0x12:
        return "ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in PES packets";
    case 0x13:
        return "ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in ISO/IEC14496_sections.";
    case 0x14:
        return "ISO/IEC 13818-6 Synchronized Download Protocol";
    }

    if ((0x15 <= streamType) && (streamType <= 0x7F))
        return "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 Reserved";

    return "User Private";
}

static void RemapPatEntByte(RemapHandle remapHandle, unsigned char aByte)
{
    //   for (i = 0; i < N; i++) {
    //     program_number               16 uimsbf
    //
    //     reserved                      3 bslbf
    //     if (program_number == '0') {
    //       network_PID                13 uimsbf
    //     }
    //     else {
    //       program_map_PID            13 uimsbf
    //     }
    //   }

    switch (remapHandle->remapPatEntState)
    {
    case RemapPatEntState00:
        remapHandle->patTemp = ((unsigned long) aByte) << 8;
        remapHandle->remapPatEntState = RemapPatEntState01;
        break;

    case RemapPatEntState01:
        remapHandle->patTemp |= ((unsigned long) aByte) << 0;
        RILOG_DEBUG("PAT program_number           %ld\n", remapHandle->patTemp);
#ifdef SAVE_PAT_AND_PMT
        if (remapHandle->pSavePat && !remapHandle->donePat)
        remapHandle->pSavePat[remapHandle->nextPat].prog = remapHandle->patTemp;
#endif
        remapHandle->remapPatEntState = RemapPatEntState02;
        break;

    case RemapPatEntState02:
        //      RILOG_DEBUG("PAT reserved                 %d\n", (aByte>>5)&7);
        remapHandle->patTemp = ((unsigned long) (aByte & 0x1F)) << 8;
        remapHandle->remapPatEntState = RemapPatEntState03;
        break;

    case RemapPatEntState03:
        remapHandle->patTemp |= ((unsigned long) aByte) << 0;
        RILOG_DEBUG("PAT PID                      0x%04lX (%ld)\n",
                remapHandle->patTemp, remapHandle->patTemp);
#ifdef SAVE_PAT_AND_PMT
        if (remapHandle->pSavePat && !remapHandle->donePat)
        remapHandle->pSavePat[remapHandle->nextPat++].pid = remapHandle->patTemp;
#endif
        remapHandle->remapPatEntState = RemapPatEntState00;
        break;
    }
}

static void RemapPatBytes(RemapHandle remapHandle, const unsigned char pdeLen,
        unsigned char bytes[])
{
    // Table 2-25 – Program association section
    //
    // program_association_section() {
    //
    //   table_id                        8 uimsbf
    //
    //   section_syntax_indicator        1 bslbf
    //   '0'                             1 bslbf
    //   reserved                        2 bslbf
    //   section_length                 12 uimsbf
    //
    //   transport_stream_id            16 uimsbf
    //
    //   reserved                        2 bslbf
    //   version_number                  5 uimsbf
    //   current_next_indicator          1 bslbf
    //
    //   section_number                  8 uimsbf
    //
    //   last_section_number             8 uimsbf
    //
    //   for (i = 0; i < N; i++) {
    //
    //     program_number               16 uimsbf
    //
    //     reserved                      3 bslbf
    //     if (program_number == '0') {
    //       network_PID                13 uimsbf
    //     }
    //     else {
    //       program_map_PID            13 uimsbf
    //     }
    //   }
    //
    //   CRC_32                         32 rpchof
    // }
    //
    // Where N = [section_length - 9] / 4 (entries)

    unsigned char i;

    for (i = 0; i < pdeLen; i++)
    {
        register unsigned char aByte = bytes[i];

        switch (remapHandle->remapPatState)
        {
        case RemapPatStateIn:
            break; // Waiting for PUSI

        case RemapPatState00: // table_id
            NextPatCrc(remapHandle, aByte);
            RILOG_DEBUG("\n");
            RILOG_DEBUG("PAT table_id                 0x%02X\n", aByte);
            // Make sure we have the correct table ID
            remapHandle->remapPatState = (aByte == 0x00 ? RemapPatState01
                    : RemapPatStateIn);
            break;

        case RemapPatState01: // MSB of section_length, CHANGE IT TO 0
            NextPatCrc(remapHandle, (remapHandle->programNumber ? (bytes[i]
                    &= 0xF0) : aByte));
            RILOG_DEBUG("PAT section_syntax_indicator %d\n", (aByte >> 7) & 1);
            //          RILOG_DEBUG("PAT '0'                      %d\n", (aByte>>6)&1);
            //          RILOG_DEBUG("PAT reserved                 %d\n", (aByte>>5)&3);
            remapHandle->patTemp = ((unsigned long) (aByte & 0xF)) << 8;
            remapHandle->remapPatState = RemapPatState02;
            break;

        case RemapPatState02: // LSB of section_length, CHANGE IT TO 13 (or 9)
            remapHandle->patTemp |= aByte;
            remapHandle->patMax0 = remapHandle->patTemp - 9;
            NextPatCrc(remapHandle, (remapHandle->programNumber ? (bytes[i]
                    = (remapHandle->patMax0 ? 13 : 9)) : aByte));
            if (remapHandle->programNumber)
            {
                RILOG_DEBUG("PAT section_length           %ld -> %d\n",
                        remapHandle->patTemp, remapHandle->patMax0 ? 13 : 9);
            }
            else
            {
                RILOG_DEBUG("PAT section_length           %ld\n",
                        remapHandle->patTemp);
            }
#ifdef SAVE_PAT_AND_PMT
            if (remapHandle->pSavePat == NULL)
            {
                remapHandle->pSavePat = g_try_malloc(remapHandle->patMax0);
                remapHandle->nextPat = 0;
                remapHandle->donePat = RemapFalse;
            }
#endif
            remapHandle->remapPatState = RemapPatState03;
            break;

        case RemapPatState03: // MSB of transport_stream_id
            NextPatCrc(remapHandle, aByte);
            remapHandle->patTemp = ((unsigned long) aByte) << 8;
            remapHandle->remapPatState = RemapPatState04;
            break;

        case RemapPatState04: // LSB of transport_stream_id
            NextPatCrc(remapHandle, aByte);
            remapHandle->patTemp |= aByte;
            RILOG_DEBUG("PAT transport_stream_id      0x%04lX (%ld)\n",
                    remapHandle->patTemp, remapHandle->patTemp);
            remapHandle->remapPatState = RemapPatState05;
            break;

        case RemapPatState05: // version_number and current_next_indicator
            NextPatCrc(remapHandle, aByte);
            //          RILOG_DEBUG("PAT reserved                 %d\n", (aByte>>6)&0x03);
            RILOG_DEBUG("PAT version_number           %d\n", (aByte >> 1)
                    & 0x1F);
            RILOG_DEBUG("PAT current_next_indicator   %d\n", (aByte >> 0)
                    & 0x01);
            remapHandle->remapPatState = RemapPatState06;
            break;

        case RemapPatState06: // section_number
            NextPatCrc(remapHandle, aByte);
            RILOG_DEBUG("PAT section_number           %d\n", aByte);
            remapHandle->remapPatState = RemapPatState07;
            break;

        case RemapPatState07: // last_section_number
            NextPatCrc(remapHandle, aByte);
            RILOG_DEBUG("PAT last_section_number      %d\n", aByte);
            // Setup for the RemapPatEntByte() function in state RemapPatState08
            remapHandle->remapPatEntState = RemapPatEntState00;
            remapHandle->patCnt0 = 0; // remapHandle->patMax0 already set up
            remapHandle->remapPatState = remapHandle->patMax0 ? RemapPatState08
                    : // There is one or more entries
                    RemapPatState21; // NO entries (probably should never happen)
            break;

            //----------------------------------------------------------------------
            // We end up here if there is one or more entries.
            //
        case RemapPatState08: // MSB of the program_number, OVERWRITE IT
            NextPatCrc(remapHandle, (remapHandle->programNumber ? bytes[i]
                    = (remapHandle->programNumber >> 8) : aByte));
            remapHandle->patTemp = ((unsigned long) aByte) << 8;
            remapHandle->remapPatState = RemapPatState09;
            break;

        case RemapPatState09: // LSB of the program_number, OVERWRITE IT
            NextPatCrc(remapHandle, (remapHandle->programNumber ? bytes[i]
                    = remapHandle->programNumber : aByte));
            remapHandle->patTemp |= ((unsigned long) aByte) << 0;
            if (remapHandle->programNumber)
            {
                RILOG_DEBUG("PAT program_number           %ld -> %d\n",
                        remapHandle->patTemp, remapHandle->programNumber);
            }
            else
            {
                RILOG_DEBUG("PAT program_number           %ld\n",
                        remapHandle->patTemp);
            }
#ifdef SAVE_PAT_AND_PMT
            if (remapHandle->pSavePat && !remapHandle->donePat)
            remapHandle->pSavePat[remapHandle->nextPat].prog = remapHandle->patTemp;
#endif
            remapHandle->remapPatState = RemapPatState10;
            break;

        case RemapPatState10: // MSB of the PMT PID, OVERWRITE IT
            NextPatCrc(remapHandle, (remapHandle->programNumber ? bytes[i]
                    = ((bytes[i] & 0xE0) | (remapHandle->newPmtPid >> 8))
                    : aByte));
            //          RILOG_DEBUG("PAT reserved                 %d\n", (aByte>>5)&7);
            remapHandle->patTemp = ((unsigned long) (aByte & 0x1F)) << 8;
            remapHandle->remapPatState = RemapPatState11;
            break;

        case RemapPatState11: // LSB of the PMT PID, OVERWRITE IT
            NextPatCrc(remapHandle, (remapHandle->programNumber ? bytes[i]
                    = remapHandle->newPmtPid : aByte));
            remapHandle->patTemp |= ((unsigned long) aByte) << 0;
            if (remapHandle->programNumber)
            {
                RILOG_DEBUG(
                        "PAT PID                      0x%04lX -> 0x%04X (%ld -> %d)\n",
                        remapHandle->patTemp, remapHandle->newPmtPid,
                        remapHandle->patTemp, remapHandle->newPmtPid);
            }
            else
            {
                RILOG_DEBUG("PAT PID                      0x%04lX (%ld)\n",
                        remapHandle->patTemp, remapHandle->patTemp);
            }
#ifdef SAVE_PAT_AND_PMT
            if (remapHandle->pSavePat && !remapHandle->donePat)
            remapHandle->pSavePat[remapHandle->nextPat++].pid = remapHandle->patTemp;
#endif
            remapHandle->patCnt0 += 4;
            remapHandle->remapPatState = remapHandle->patCnt0
                    >= remapHandle->patMax0 ? RemapPatState21 : // Only one entry, overwrite the CRC bytes
                    RemapPatState12; // There is more than one entry, CRC then pad
            break;

            //----------------------------------------------------------------------
            // We end up here if there was more than one entry.  In this case the
            // new CRC bytes will overwrite the second entry and the rest of the
            // old PAT is padded out with 0xFF characters.
            //
        case RemapPatState12:
            if (remapHandle->programNumber)
                bytes[i] = DonePatCrc(remapHandle) >> 24;
            else
            {
                NextPatCrc(remapHandle, aByte);
                remapHandle->patTemp = ((unsigned long) aByte) << 8;
            }
            remapHandle->remapPatState = RemapPatState13;
            break;

        case RemapPatState13:
            if (remapHandle->programNumber)
                bytes[i] = DonePatCrc(remapHandle) >> 16;
            else
            {
                NextPatCrc(remapHandle, aByte);
                remapHandle->patTemp |= ((unsigned long) aByte) << 0;
                RILOG_DEBUG("PAT program_number           %ld\n",
                        remapHandle->patTemp);
            }
#ifdef SAVE_PAT_AND_PMT
            if (remapHandle->pSavePat && !remapHandle->donePat)
            remapHandle->pSavePat[remapHandle->nextPat].prog = remapHandle->patTemp;
#endif
            remapHandle->remapPatState = RemapPatState14;
            break;

        case RemapPatState14:
            if (remapHandle->programNumber)
                bytes[i] = DonePatCrc(remapHandle) >> 8;
            else
            {
                NextPatCrc(remapHandle, aByte);
                //              RILOG_DEBUG("PAT reserved                 %d\n", (aByte>>5)&7);
                remapHandle->patTemp = ((unsigned long) (aByte & 0x1F)) << 8;
            }
            remapHandle->remapPatState = RemapPatState15;
            break;

        case RemapPatState15:
            if (remapHandle->programNumber)
                bytes[i] = DonePatCrc(remapHandle) >> 0;
            else
            {
                NextPatCrc(remapHandle, aByte);
                remapHandle->patTemp |= ((unsigned long) aByte) << 0;
                RILOG_DEBUG("PAT PID                      0x%04lX (%ld)\n",
                        remapHandle->patTemp, remapHandle->patTemp);
            }
#ifdef SAVE_PAT_AND_PMT
            if (remapHandle->pSavePat && !remapHandle->donePat)
            remapHandle->pSavePat[remapHandle->nextPat++].pid = remapHandle->patTemp;
#endif
            remapHandle->patCnt0 += 4;
            remapHandle->remapPatState = remapHandle->patCnt0
                    >= remapHandle->patMax0 ? RemapPatState17 : // There was only two entries
                    RemapPatState16; // There are more than two entries
            break;

            //----------------------------------------------------------------------
            // We end up here if there was more than two entries.  In this case the
            // new CRC bytes will overwrite the second entry and the rest of the
            // entries are overwritten with 0xFF characters.
            //
        case RemapPatState16:
            if (remapHandle->programNumber)
                bytes[i] = 0xFF;
            else
            {
                NextPatCrc(remapHandle, aByte);
                RemapPatEntByte(remapHandle, aByte);
            }
            if (++remapHandle->patCnt0 >= remapHandle->patMax0)
            {
                remapHandle->remapPatState = RemapPatState17;
            }
            break;

            //----------------------------------------------------------------------
            // We end up here if there was two or more entries.  All the entries
            // have been taken care of so the only remaining task is to overwrite
            // the old CRC bytes with 0xFF characters.
            //
        case RemapPatState17:
            if (remapHandle->programNumber)
                bytes[i] = 0xFF;
            remapHandle->patTemp = ((unsigned long) aByte) << 24;
            remapHandle->remapPatState = RemapPatState18;
            break;

        case RemapPatState18:
            if (remapHandle->programNumber)
                bytes[i] = 0xFF;
            remapHandle->patTemp |= ((unsigned long) aByte) << 16;
            remapHandle->remapPatState = RemapPatState19;
            break;

        case RemapPatState19:
            if (remapHandle->programNumber)
                bytes[i] = 0xFF;
            remapHandle->patTemp |= ((unsigned long) aByte) << 8;
            remapHandle->remapPatState = RemapPatState20;
            break;

        case RemapPatState20:
            if (remapHandle->programNumber)
                bytes[i] = 0xFF;
            remapHandle->patTemp |= ((unsigned long) aByte) << 0;
            RILOG_DEBUG("PAT CRC_32                   0x%08lX -> 0x%08lX\n",
                    remapHandle->patTemp, DonePatCrc(remapHandle));
#ifdef SAVE_PAT_AND_PMT
            if (remapHandle->pSavePat && !remapHandle->donePat)
            remapHandle->donePat = RemapTrue;
#endif
            remapHandle->remapPatState = RemapPatStateIn;
            break;

            //----------------------------------------------------------------------
            // We end up here if there was one or zero entries.  In this case
            // we simply overwrite the old CRC with the new CRC:
            //
        case RemapPatState21:
            if (remapHandle->programNumber)
                bytes[i] = DonePatCrc(remapHandle) >> 24;
            remapHandle->patTemp = ((unsigned long) aByte) << 24;
            remapHandle->remapPatState = RemapPatState22;
            break;

        case RemapPatState22:
            if (remapHandle->programNumber)
                bytes[i] = DonePatCrc(remapHandle) >> 16;
            remapHandle->patTemp |= ((unsigned long) aByte) << 16;
            remapHandle->remapPatState = RemapPatState23;
            break;

        case RemapPatState23:
            if (remapHandle->programNumber)
                bytes[i] = DonePatCrc(remapHandle) >> 8;
            remapHandle->patTemp |= ((unsigned long) aByte) << 8;
            remapHandle->remapPatState = RemapPatState24;
            break;

        case RemapPatState24:
            if (remapHandle->programNumber)
                bytes[i] = DonePatCrc(remapHandle) >> 0;
            remapHandle->patTemp |= ((unsigned long) aByte) << 0;
            RILOG_DEBUG("PAT CRC_32                   0x%08lX -> 0x%08lX\n",
                    remapHandle->patTemp, DonePatCrc(remapHandle));
#ifdef SAVE_PAT_AND_PMT
            if (remapHandle->pSavePat && !remapHandle->donePat)
            remapHandle->donePat = RemapTrue;
#endif
            remapHandle->remapPatState = RemapPatStateIn;
            break;
        }
    }
}

static RemapPid RemapPidInBuffer(RemapHandle remapHandle,
        unsigned char * const pPidLsb)
{
    unsigned char pidLsb = *pPidLsb;

    // We now have the four following values to work with:
    //
    //      remapHandle->pPidMsb  address of the MSB of the PID field or NULL
    //      remapHandle->pidMsb   value of the MSB of the PID field
    //      pPidLsb               address of the LSB of the PID field, never NULL
    //      pidLsb                value of the LSB of the PID field
    //
    // Combine the remapHandle->pidMsb value with the pidLsb value to get the oldPid value

    RemapPid oldPid = (((RemapPid)(remapHandle->pidMsb & 0x1F)) << 8) | pidLsb;

    if (remapHandle->programNumber) // We are rewritting the PATs and PMTs
    {
        RemapPid newPid = (*remapHandle->pRemapPids).newPid[oldPid];

        if ((newPid != REMAP_UNDEFINED_PID) && (newPid != oldPid))
        {
            // The PID is being remapped, first calculate the MSB and LSB of the new PID

            remapHandle->pidMsb = (remapHandle->pidMsb & 0xE0) | (newPid >> 8);
            pidLsb = newPid;

            if (remapHandle->pPidMsb) // Can we modify the actual buffer?
            {
                *remapHandle->pPidMsb = remapHandle->pidMsb;
                *pPidLsb = pidLsb;

            } // else make this PMT have a CRC error so it will be thrown out!

            NextPmtCrc(remapHandle, remapHandle->pidMsb);
            NextPmtCrc(remapHandle, pidLsb);
            return newPid;

        } // else the PID is not being remapped, use the old PID value

    } // else we are not rewritting the PATs and PMTs

    // Calculate the CRC based on the old PID values

    NextPmtCrc(remapHandle, remapHandle->pidMsb);
    NextPmtCrc(remapHandle, pidLsb);

    // Return the old PID value

    return oldPid;
}

static void RemapDescriptorByte(RemapHandle remapHandle, unsigned char * pByte)
{
    // descriptor() {
    //   descriptor_tag    8 uimsbf
    //   descriptor_length 8 uimsbf
    //   data
    // }

    register unsigned char aByte = *pByte;
    RemapPid remapPid;

    switch (remapHandle->remapPmtDesState)
    {
    case RemapPmtDesState00:
        NextPmtCrc(remapHandle, aByte);
        RILOG_DEBUG("PMT descriptor:              %3d = [%s] ", aByte,
                DescriptorToString(aByte));
        // Handle the CA descriptor differently since it contains a PID
        remapHandle->remapPmtDesState = aByte == 0x09 ? RemapPmtDesState03
                : RemapPmtDesState01;
        break;

    case RemapPmtDesState01:
        NextPmtCrc(remapHandle, aByte);
        RILOG_DEBUG("(%d) ", aByte);
        // Setup for the loop in state RemapPmtDesState02
        remapHandle->pmtCnt0 = 0;
        remapHandle->pmtMax0 = aByte;
        remapHandle->remapPmtDesState = RemapPmtDesState02;
        break;

    case RemapPmtDesState02:
        NextPmtCrc(remapHandle, aByte);
        RILOG_DEBUG("0x%02X ", aByte);
        if (++remapHandle->pmtCnt0 >= remapHandle->pmtMax0)
        {
            RILOG_DEBUG("\n");
            remapHandle->remapPmtDesState = RemapPmtDesState00;
        }
        break;

    case RemapPmtDesState03:

        // CA_descriptor() {
        //   descriptor_tag              8 uimsbf [already got this aByte]
        //   descriptor_length           8 uimsbf
        //   CA_system_ID               16 uimsbf
        //   reserved                    3 bslbf
        //   CA_PID                     13 uimsbf
        //   for (i = 0; i < N; i++) {
        //     private_data_byte         8 uimsbf
        //   }
        // }

        NextPmtCrc(remapHandle, aByte);
        RILOG_DEBUG("(%d) ", aByte);
        // Setup for the loop in state RemapPmtDesState02
        remapHandle->pmtCnt0 = 0;
        remapHandle->pmtMax0 = aByte;
        remapHandle->remapPmtDesState = RemapPmtDesState04;
        break;

    case RemapPmtDesState04:
        NextPmtCrc(remapHandle, aByte);
        remapHandle->pmtTemp = ((unsigned long) aByte) << 8;
        remapHandle->pmtCnt0++;
        remapHandle->remapPmtDesState = RemapPmtDesState05;
        break;

    case RemapPmtDesState05:
        NextPmtCrc(remapHandle, aByte);
        remapHandle->pmtTemp |= aByte;
        RILOG_DEBUG("CA_system_ID=0x%04lX ", remapHandle->pmtTemp);
        remapHandle->pmtCnt0++;
        remapHandle->remapPmtDesState = RemapPmtDesState06;
        break;

    case RemapPmtDesState06:
        //      NextPmtCrc(remapHandle, aByte); THIS IS DONE IN RemapPidInBuffer()
        RILOG_DEBUG("reserved=%d ", (aByte >> 5) & 7);
        remapHandle->pmtTemp = ((unsigned long) (aByte & 0x1F)) << 8;
        remapHandle->pmtCnt0++;
        remapHandle->pPidMsb = pByte;
        remapHandle->pidMsb = aByte;
        remapHandle->msbPtrGotSet = RemapTrue;
        remapHandle->remapPmtDesState = RemapPmtDesState07;
        break;

    case RemapPmtDesState07:
        //      NextPmtCrc(remapHandle, aByte); THIS IS DONE IN RemapPidInBuffer()
        remapPid = RemapPidInBuffer(remapHandle, pByte);
        remapHandle->pmtTemp |= aByte;
        if (remapHandle->programNumber)
        {
            RILOG_DEBUG("CA_PID=0x%04lX->0x%04X (%ld->%d) ",
                    remapHandle->pmtTemp, remapPid, remapHandle->pmtTemp,
                    remapPid);
        }
        else
        {
            RILOG_DEBUG("CA_PID=0x%04lX (%ld) ", remapHandle->pmtTemp,
                    remapHandle->pmtTemp);
        }
        remapHandle->pmtCnt0++;
        remapHandle->remapPmtDesState = RemapPmtDesState02;
        break;
    }
}

static void RemapEntryByte(RemapHandle remapHandle, unsigned char * pByte)
{
    //   for (i = 0; i < N1; i++) {
    //
    //     stream_type                   8 uimsbf
    //
    //     reserved                      3 bslbf
    //     elementary_PID               13 uimsbf
    //
    //     reserved                      4 bslbf
    //     ES_info_length               12 uimsbf
    //
    //     for (i = 0; i < N2; i++) {
    //       descriptor()
    //     }
    //   }

    register unsigned char aByte = *pByte;
    RemapPid remapPid;

    switch (remapHandle->remapPmtEntState)
    {
    case RemapPmtEntState00:
        // Remap stream type 0x80 (Digicipher II) to stream type 0x02 (MPEG-2 Video)
        if (aByte == 0x80)
        {
            aByte = 0x02;
            *pByte = aByte;
        }
        NextPmtCrc(remapHandle, aByte);
        RILOG_DEBUG("PMT stream_type              0x%02X = [%s]\n", aByte,
                StreamTypeToString(aByte));
#ifdef SAVE_PAT_AND_PMT
        remapHandle->streamType = aByte;
#endif
        remapHandle->remapPmtEntState = RemapPmtEntState01;
        break;

    case RemapPmtEntState01:
        //      NextPmtCrc(remapHandle, aByte); THIS IS DONE IN RemapPidInBuffer()
        //      RILOG_DEBUG("PMT reserved                 %d\n", (aByte>>5)&7);
        remapHandle->pmtTemp = ((unsigned long) (aByte & 0x1F)) << 8;
        remapHandle->pPidMsb = pByte;
        remapHandle->pidMsb = aByte;
        remapHandle->msbPtrGotSet = RemapTrue;
        remapHandle->remapPmtEntState = RemapPmtEntState02;
        break;

    case RemapPmtEntState02:
        //      NextPmtCrc(remapHandle, aByte); THIS IS DONE IN RemapPidInBuffer()
        remapPid = RemapPidInBuffer(remapHandle, pByte);
        remapHandle->pmtTemp |= aByte;
        if (remapHandle->programNumber)
        {
            RILOG_DEBUG(
                    "PMT elementary_PID           0x%04lX -> 0x%04X (%ld -> %d)\n",
                    remapHandle->pmtTemp, remapPid, remapHandle->pmtTemp,
                    remapPid);
        }
        else
        {
            RILOG_DEBUG("PMT elementary_PID           0x%04lX (%ld)\n",
                    remapHandle->pmtTemp, remapHandle->pmtTemp);
        }
#ifdef SAVE_PAT_AND_PMT
        switch (remapHandle->streamType)
        {
            case 0x01: // ISO/IEC 11172 Video";
            case 0x02: // ITU-T Rec. H.262 | ISO/IEC 13818-2 Video or ISO/IEC 11172-2 constrained parameter video stream";
            remapHandle->videoPid = remapHandle->pmtTemp;
        }
#endif
        remapHandle->remapPmtEntState = RemapPmtEntState03;
        break;

    case RemapPmtEntState03:
        NextPmtCrc(remapHandle, aByte);
        //      RILOG_DEBUG("PMT reserved                 %d\n", (aByte>>4)&0xF);
        remapHandle->pmtTemp = ((unsigned long) (aByte & 0xF)) << 8;
        remapHandle->remapPmtEntState = RemapPmtEntState04;
        break;

    case RemapPmtEntState04:
        NextPmtCrc(remapHandle, aByte);
        remapHandle->pmtTemp |= aByte;
        RILOG_DEBUG("PMT ES_info_length           %ld\n", remapHandle->pmtTemp);
        // Setup for RemapDescriptorByte() function in state RemapPmtEntState05
        remapHandle->remapPmtDesState = RemapPmtDesState00;
        remapHandle->pmtCnt1 = 0;
        remapHandle->pmtMax1 = remapHandle->pmtTemp;
        remapHandle->remapPmtEntState
                = remapHandle->pmtMax1 ? RemapPmtEntState05
                        : RemapPmtEntState00;
        break;

    case RemapPmtEntState05:
        RemapDescriptorByte(remapHandle, pByte);
        if (++remapHandle->pmtCnt1 >= remapHandle->pmtMax1)
        {
            remapHandle->remapPmtEntState = RemapPmtEntState00;
        }
        break;
    }
}

static void RemapPmtBytes(RemapHandle remapHandle, const unsigned char pdeLen,
        unsigned char bytes[])
{
    // Table 2-28 – Transport Stream program map section
    //
    // TS_program_map_section() {
    //
    //   table_id                        8 uimsbf
    //
    //   section_syntax_indicator        1 bslbf
    //   '0'                             1 bslbf
    //   reserved                        2 bslbf
    //   section_length                 12 uimsbf
    //
    //   program_number                 16 uimsbf
    //
    //   reserved                        2 bslbf
    //   version_number                  5 uimsbf
    //   current_next_indicator          1 bslbf
    //
    //   section_number                  8 uimsbf
    //
    //   last_section_number             8 uimsbf
    //
    //   reserved                        3 bslbf
    //   PCR_PID                        13 uimsbf
    //
    //   reserved                        4 bslbf
    //   program_info_length            12 uimsbf
    //
    //   for (i = 0; i < N0; i++) {
    //     descriptor()
    //   }
    //
    //   for (i = 0; i < N1; i++) {
    //
    //     stream_type                   8 uimsbf
    //
    //     reserved                      3 bslbf
    //     elementary_PID               13 uimsbf
    //
    //     reserved                      4 bslbf
    //     ES_info_length               12 uimsbf
    //
    //     for (i = 0; i < N2; i++) {
    //       descriptor()
    //     }
    //   }
    //
    //   CRC_32                         32 rpchof
    // }
    //
    // Where N0 = program_info_length (bytes)
    //
    // Where N1 = section_length - program_info_length - 13 (bytes)
    //
    // Where N2 = ES_info_length (bytes)

    unsigned char i;

    for (i = 0; i < pdeLen; i++)
    {
        register unsigned char aByte = bytes[i];
        RemapPid remapPid;

        switch (remapHandle->remapPmtState)
        {
        case RemapPmtStateIn:
            break; // Waiting for PUSI

        case RemapPmtState00:
            NextPmtCrc(remapHandle, aByte);
            RILOG_DEBUG("\n");
            RILOG_DEBUG("PMT table_id                 0x%02X\n", aByte);
            // Make sure we have the correct table ID
            remapHandle->remapPmtState = aByte == 0x02 ? RemapPmtState01
                    : RemapPmtStateIn;
            break;

        case RemapPmtState01:
            NextPmtCrc(remapHandle, aByte);
            RILOG_DEBUG("PMT section_syntax_indicator %d\n", (aByte >> 7) & 1);
            //          RILOG_DEBUG("PMT '0'                      %d\n", (aByte>>6)&1);
            //          RILOG_DEBUG("PMT reserved                 %d\n", (aByte>>5)&3);
            remapHandle->pmtTemp = ((unsigned long) (aByte & 0xF)) << 8;
            remapHandle->remapPmtState = RemapPmtState02;
            break;

        case RemapPmtState02:
            NextPmtCrc(remapHandle, aByte);
            remapHandle->pmtTemp |= aByte;
            RILOG_DEBUG("PMT section_length           %ld\n",
                    remapHandle->pmtTemp);
            remapHandle->pmtMax2 = remapHandle->pmtTemp - 13;
            remapHandle->remapPmtState = RemapPmtState03;
            break;

        case RemapPmtState03:
            NextPmtCrc(remapHandle, aByte);
            remapHandle->pmtTemp = ((unsigned long) aByte) << 8;
            remapHandle->remapPmtState = RemapPmtState04;
            break;

        case RemapPmtState04:
            NextPmtCrc(remapHandle, aByte);
            remapHandle->pmtTemp |= aByte;
            RILOG_DEBUG("PMT transport_stream_id      0x%04lX (%ld)\n",
                    remapHandle->pmtTemp, remapHandle->pmtTemp);
            remapHandle->remapPmtState = RemapPmtState05;
            break;

        case RemapPmtState05:
            NextPmtCrc(remapHandle, aByte);
            //          RILOG_DEBUG("PMT reserved                 %d\n", (aByte>>6)&0x03);
            RILOG_DEBUG("PMT version_number           %d\n", (aByte >> 1)
                    & 0x1F);
            RILOG_DEBUG("PMT current_next_indicator   %d\n", (aByte >> 0)
                    & 0x01);
            remapHandle->remapPmtState = RemapPmtState06;
            break;

        case RemapPmtState06:
            NextPmtCrc(remapHandle, aByte);
            RILOG_DEBUG("PMT section_number           %d\n", aByte);
            remapHandle->remapPmtState = RemapPmtState07;
            break;

        case RemapPmtState07:
            NextPmtCrc(remapHandle, aByte);
            RILOG_DEBUG("PMT last_section_number      %d\n", aByte);
            remapHandle->remapPmtState = RemapPmtState08;
            break;

        case RemapPmtState08:
            //          NextPmtCrc(remapHandle, aByte); THIS IS DONE IN RemapPidInBuffer()
            //          RILOG_DEBUG("PMT reserved                 %d\n", (aByte>>5)&7);
            remapHandle->pmtTemp = ((unsigned long) (aByte & 0x1F)) << 8;
            remapHandle->pPidMsb = &bytes[i];
            remapHandle->pidMsb = aByte;
            remapHandle->msbPtrGotSet = RemapTrue;
            remapHandle->remapPmtState = RemapPmtState09;
            break;

        case RemapPmtState09:
            //          NextPmtCrc(remapHandle, aByte); THIS IS DONE IN RemapPidInBuffer()
            remapPid = RemapPidInBuffer(remapHandle, &bytes[i]);
            remapHandle->pmtTemp |= aByte;
            if (remapHandle->programNumber)
            {
                RILOG_DEBUG(
                        "PMT PCR_PID                  0x%04lX -> 0x%04X (%ld -> %d)\n",
                        remapHandle->pmtTemp, remapPid, remapHandle->pmtTemp,
                        remapPid);
            }
            else
            {
                RILOG_DEBUG("PMT PCR_PID                  0x%04lX (%ld)\n",
                        remapHandle->pmtTemp, remapHandle->pmtTemp);
            }
            remapHandle->remapPmtState = RemapPmtState10;
            break;

        case RemapPmtState10:
            NextPmtCrc(remapHandle, aByte);
            //          RILOG_DEBUG("PMT reserved                 %d\n", (aByte>>4)&0x0F);
            remapHandle->pmtTemp = ((unsigned long) (aByte & 0x0F)) << 8;
            remapHandle->remapPmtState = RemapPmtState11;
            break;

        case RemapPmtState11:
            NextPmtCrc(remapHandle, aByte);
            remapHandle->pmtTemp |= aByte;
            RILOG_DEBUG("PMT program_info_length      %ld\n",
                    remapHandle->pmtTemp);
            // Setup for RemapDescriptorByte() function in state RemapPmtState12
            remapHandle->remapPmtDesState = RemapPmtDesState00;
            remapHandle->pmtCnt1 = 0;
            remapHandle->pmtMax1 = remapHandle->pmtTemp;
            // Setup for the RemapEntryByte() function in state RemapPmtState13
            remapHandle->remapPmtEntState = RemapPmtEntState00;
            remapHandle->pmtCnt2 = 0;
            remapHandle->pmtMax2 -= remapHandle->pmtTemp;
            // Select the next state from
            // [RemapPmtState12, RemapPmtState13 or RemapPmtState14]
            remapHandle->remapPmtState
                    = (remapHandle->pmtMax1 ? RemapPmtState12
                            : (remapHandle->pmtMax2 ? RemapPmtState13
                                    : RemapPmtState14));
            break;

        case RemapPmtState12:
            RemapDescriptorByte(remapHandle, &bytes[i]);
            if (++remapHandle->pmtCnt1 >= remapHandle->pmtMax1)
            {
                // Select next state from [RemapPmtState13 or RemapPmtState14]
                remapHandle->remapPmtState
                        = (remapHandle->pmtMax2 ? RemapPmtState13
                                : RemapPmtState14);
            }
            break;

        case RemapPmtState13:
            RemapEntryByte(remapHandle, &bytes[i]);
            if (++remapHandle->pmtCnt2 >= remapHandle->pmtMax2)
            {
                remapHandle->remapPmtState = RemapPmtState14;
            }
            break;

        case RemapPmtState14:
            bytes[i] = DonePmtCrc(remapHandle) >> 24;
            remapHandle->pmtTemp = ((unsigned long) aByte) << 24;
            remapHandle->remapPmtState = RemapPmtState15;
            break;

        case RemapPmtState15:
            bytes[i] = DonePmtCrc(remapHandle) >> 16;
            remapHandle->pmtTemp |= ((unsigned long) aByte) << 16;
            remapHandle->remapPmtState = RemapPmtState16;
            break;

        case RemapPmtState16:
            bytes[i] = DonePmtCrc(remapHandle) >> 8;
            remapHandle->pmtTemp |= ((unsigned long) aByte) << 8;
            remapHandle->remapPmtState = RemapPmtState17;
            break;

        case RemapPmtState17:
            bytes[i] = DonePmtCrc(remapHandle) >> 0;
            remapHandle->pmtTemp |= ((unsigned long) aByte) << 0;
            RILOG_DEBUG("PMT CRC_32                   0x%08lX -> 0x%08lX\n",
                    remapHandle->pmtTemp, DonePmtCrc(remapHandle));
            remapHandle->remapPmtState = RemapPmtStateIn;
            break;
        }
    }
}

#if 0

static inline void ConvertToNullPacket
(
        unsigned char bytes[REMAP_TRANSPORT_PACKET_SIZE]
)
{
    // Create a null packet.
    //
    // Directly from the specification:
    //
    // For null packets the payload_unit_start_indicator
    //    shall be set to '0'
    // PID value 0x1FFF is reserved for null packets
    // In the case of a null packet the value of the
    //    scrambling_control field shall be set to '00'
    //    adaptation_field_control shall be set to '01'
    //    continuity_counter is undefined
    // In the case of null packets with PID value 0x1FFF,
    //    data_bytes may be assigned any value
    //    (so leave the data alone)
    //
    // So a null packet header is always 0x47 0x1F 0xFF 0x1X

    bytes[1] = ((0<<7) | // 1 bit  Transport Error Indicator
            (0<<6) | // 1 bit  Payload Unit Start Indicator
            (0<<5) | // 1 bit  Transport Priority
            (0x1F)); // 5 bits MSB of the PID
    bytes[2] = ((0xFF)); // 8 bits LSB of the PID
    bytes[3] = ((0<<6) | // 2 bits Scrambling control
            (1<<4) | // 2 bits Adaptation Field Control
            (0x0F)); // 4 bits Continuity counter
}

#endif

static RemapBoolean RemapOnePacket // RemapTrue=keep packet, RemapFalse=drop it
(RemapHandle remapHandle, unsigned char bytes[REMAP_TRANSPORT_PACKET_SIZE])
{
    // Hopefully built for speed...
    //
    // Transport header bits not used in this algorithm:
    //
    // const unsigned char tpBit  = (bytes[1] >> 5) & 0x01; Transport Priority
    // const unsigned char teiBit = (bytes[1] >> 7) & 0x01; Transport Error Indicator
    // const unsigned char ccBits = (bytes[3] >> 0) & 0x0F; Continuity counter
    // const unsigned char scBits = (bytes[3] >> 6) & 0x03; Scrambling control

    const RemapPid oldPid = (((RemapPid)(bytes[1] & 0x1F)) << 8) | bytes[2];
    const RemapPid newPid = (*remapHandle->pRemapPids).newPid[oldPid];

    RemapBoolean keepPacket;

    if (bytes[0] != 0x47)
        return RemapFalse; // Sync error detected, drop packet

    if (newPid == REMAP_UNDEFINED_PID)
    {
        keepPacket = RemapFalse; // PID not in list, drop packet
    }
    else
    {
        keepPacket = RemapTrue;

        if (newPid != oldPid)
        {
            bytes[1] = (bytes[1] & 0xE0) | (newPid >> 8);
            bytes[2] = newPid;
        }
    }

    if (oldPid == 0) // Remap the PAT
    {
        const unsigned char pdeBit = (bytes[3] >> 4) & 0x01;

        if (pdeBit)
        {
            const unsigned char pusiBit = (bytes[1] >> 6) & 0x01;
            const unsigned char afeBit = (bytes[3] >> 5) & 0x01;
            const unsigned char afLen = afeBit ? bytes[4] + 1 : 0;
            unsigned char point = 0;

            if (pusiBit)
            {
                remapHandle->remapPatState = RemapPatState00;
                point = bytes[4 + afLen] + 1;
                InitPatCrc(remapHandle);
            }

            RemapPatBytes(remapHandle, REMAP_TRANSPORT_PACKET_SIZE - (4 + afLen
                    + point), &bytes[4 + afLen + point]);
        }
    }
    else if (oldPid == remapHandle->oldPmtPid)
    {
        const unsigned char pdeBit = (bytes[3] >> 4) & 0x01;

        if (pdeBit)
        {
            const unsigned char pusiBit = (bytes[1] >> 6) & 0x01;
            const unsigned char afeBit = (bytes[3] >> 5) & 0x01;
            const unsigned char afLen = afeBit ? bytes[4] + 1 : 0;
            unsigned char point = 0;

            if (pusiBit)
            {
                remapHandle->remapPmtState = RemapPmtState00;
                point = bytes[4 + afLen] + 1;
                InitPmtCrc(remapHandle);
            }

            RemapPmtBytes(remapHandle, REMAP_TRANSPORT_PACKET_SIZE - (4 + afLen
                    + point), &bytes[4 + afLen + point]);
        }
    }

    return keepPacket; // No sync errors found
}

//  Opens a remap object in the default mode:
//
//      No PID passed through (all PIDs turned off)
//      No PMT rewriting (all PMTs unchanged)
//      No PAT rewriting (all PATs unchanged)
//
//  Note: the RemapHandle must be freed by calling the RemapClose function

RemapReturnCode RemapOpen(RemapHandle * const pRemapHandle // Output
)
{
    RemapReturnCode remapReturnCode;

    InitCrcTable();

    if (pRemapHandle == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: pRemapHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }

    *pRemapHandle = g_try_malloc(sizeof(RemapHandleImpl));
    if (*pRemapHandle == NULL)
        return RemapReturnCodeMemAllocationError;

    // PID filtering and remapping parameters
    (*pRemapHandle)->pRemapPids = g_try_malloc(sizeof(RemapPids));
    if ((*pRemapHandle)->pRemapPids == NULL)
    {
        g_free(*pRemapHandle);
        *pRemapHandle = NULL;
        return RemapReturnCodeMemAllocationError;
    }

    // PMT rewriting parameters
    (*pRemapHandle)->oldPmtPid = 0;

    // PAT rewriting parameters
    (*pRemapHandle)->programNumber = 0;
    (*pRemapHandle)->newPmtPid = 0;

    // PMT processing parameters
    (*pRemapHandle)->remapPmtState = RemapPmtState00;
    (*pRemapHandle)->remapPmtDesState = RemapPmtDesState00;
    (*pRemapHandle)->remapPmtEntState = RemapPmtEntState00;

    // PAT processing parameters
    (*pRemapHandle)->remapPatState = RemapPatState00;
    (*pRemapHandle)->remapPatEntState = RemapPatEntState00;

    // Handles and pointers to the previous data
    (*pRemapHandle)->prevNumPackets = 0;
    (*pRemapHandle)->pPrevPointers = NULL;
    (*pRemapHandle)->pPidMsb = NULL;

#ifdef SAVE_PAT_AND_PMT
    (*pRemapHandle)->pSavePat = NULL;
    (*pRemapHandle)->videoPid = REMAP_UNDEFINED_PID;
#endif

    // Turn off all PIDs
    remapReturnCode = RemapAndFilterPids(*pRemapHandle, 0, NULL);
    if (remapReturnCode != RemapReturnCodeNoErrorReported)
    {
        g_free((*pRemapHandle)->pRemapPids);
        g_free(*pRemapHandle);
        *pRemapHandle = NULL;
        return remapReturnCode;
    }

    return RemapReturnCodeNoErrorReported;
}

//  Change the PID remapping and filtering settings
//
//  If numPairs is zero:
//
//      ALL PIDs filtered out (no PIDs pass through)
//      pRemapPair MAY be NULL
//
//  If numPairs is not zero and pRemapPair is NULL
//
//      No PID filtering (all PIDs pass through)
//      No PID remapping (all PIDs unchanged)
//
//  If numPairs is not zero and pRemapPair is not NULL
//
//      Only the specified PIDs pass through
//      Optionally remap the PIDs
//
// This same information in a table:
//
//     numPairs  pRemapPair  PIDs passed through
//     --------  ----------  -------------------
//            0        NULL  NONE
//            0    not NULL  NONE
//        not 0        NULL  ALL
//        not 0    not NULL  The selected PIDs
//
//  Note: pRemapPair can be static, stack or heap memory.  Ownership of the
//  memory is returned to and remains with the caller.
//
//  Note: if any of the PIDs are remapped (by setting the newPid to a different
//  value than the oldPid) then you will probably also want to do PMT rewriting.
//  See the RemapPmts() function below.
//
//  Note: the PID pair list SHALL NOT contain a remapping of the PAT PID (an
//  entry with the oldPid set to 0x0000 and the newPid set to a different value)
//  but it MUST contain a filter setting of oldPid=0x0000/newPid=0x0000 if you
//  wish to pass the PATs on to the filtered stream.

RemapReturnCode RemapAndFilterPids(RemapHandle remapHandle, // Input
        const NumPairs numPairs, // Input, the number of PID remap pairs
        RemapPair * const pRemapPair // Input, the PIDs to pass and remap
)
{
    NumPairs i;
    RemapPid * newPids;

    if (remapHandle == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: remapHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }

    newPids = remapHandle->pRemapPids->newPid;

    if (numPairs) // Passing everything or selected PIDS
    {
        if (pRemapPair) // Pass (and remap) only the selected PIDs
        {
            // First, turn all PIDs off
            for (i = 0; i < sizeof(RemapPids) / sizeof(RemapPid); i++)
                newPids[i] = REMAP_UNDEFINED_PID;

            // Now turn on only the PIDs they want
            for (i = 0; i < numPairs; i++)
            {
                RemapPid oldPid = pRemapPair[i].oldPid;
                RemapPid newPid = pRemapPair[i].newPid;

                // Make sure they passed us legal values
                if (oldPid & ~REMAP_NULL_PID_VALUE)
                {
                    RILOG_ERROR(
                            "RemapReturnCodeBadInputParameter: oldPid is invalid in line %d of %s\n",
                            __LINE__, __FILE__);
                    return RemapReturnCodeBadInputParameter;
                }
                if (newPid & ~REMAP_NULL_PID_VALUE)
                {
                    RILOG_ERROR(
                            "RemapReturnCodeBadInputParameter: newPid is invalid in line %d of %s\n",
                            __LINE__, __FILE__);
                    return RemapReturnCodeBadInputParameter;
                }

                // Make sure they are not trying to remap the PAT PID
                if (oldPid)
                    newPids[oldPid] = newPid;
                else if (newPid)
                {
                    RILOG_ERROR(
                            "RemapReturnCodeBadInputParameter: (oldPid == 0) && newPid in line %d of %s\n",
                            __LINE__, __FILE__);
                    return RemapReturnCodeBadInputParameter;
                }
                else
                    newPids[0] = 0;
            }
        }
        else // Pass all PIDs with no remapping
        {
            for (i = 0; i < sizeof(RemapPids) / sizeof(RemapPid); i++)
                newPids[i] = i;
        }
    }
    else // Turn off all PIDs
    {
        for (i = 0; i < sizeof(RemapPids) / sizeof(RemapPid); i++)
            newPids[i] = REMAP_UNDEFINED_PID;
    }

    return RemapReturnCodeNoErrorReported;
}

//  Change the PMT rewriting setting
//
//  If pmtPid is zero:
//
//      No PMT rewriting (all PMTs unchanged)
//
//  If pmtPid is not zero:
//
//      PMTs found on the specified PID are rewritten
//
//  Note:  The PMTs are rewritten using the PID pairs specified in the
//  RemapAndFilterPids() function
//
//  Note:  If there are no PID pairs specified in the RemapAndFilterPids()
//  function then PMT rewriting has no effect
//
//  Note:  If the oldPid equal the newPid in all of the PID pairs specified in
//  the RemapAndFilterPids() function then PMT rewriting has no effect

RemapReturnCode RemapPmts(RemapHandle remapHandle, // Input
        const RemapPid pmtPid // Input, the PMT PID
)
{
    if (remapHandle == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: remapHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }
    remapHandle->oldPmtPid = pmtPid;

    return RemapReturnCodeNoErrorReported;
}

//  Change the PAT rewriting setting
//
//  If programNumber is zero:
//
//      No PAT rewriting (all PATs unchanged)
//
//  If programNumber is not zero:
//
//      PATs found on the specified PID are rewritten
//
//  Note:  The PATs are rewritten using the specified program number and
//  pmtPid values so if the PMT PID is being remapped you need to specify the
//  new PMT PID here.
//
//  Note:  If PAT rewriting is enabled using this function then all incoming
//  MPTS PATs are converted to SPTS PATs.

RemapReturnCode RemapPats(RemapHandle remapHandle, // Input
        RemapProg programNumber, // Input
        const RemapPid pmtPid // Input, the new PMT PID
)
{
    if (remapHandle == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: remapHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }

    remapHandle->programNumber = programNumber;
    remapHandle->newPmtPid = pmtPid;

    return RemapReturnCodeNoErrorReported;
}

//  Transfer the next buffer of transport packets and associated array of buffer
//  pointers into the Remap module for processing and receive back the processed
//  previous buffer of transport packets and associated array of buffer pointers
//
//  Note that the first time this function is called:
//
//      *pPrevNumPackets will be set to zero
//      *ppPrevPointers will be set to NULL
//
//  Note that in order to retrieve the last buffer call this function as follows:
//
//  remapReturnCode = RemapAndFilter
//  (
//      remapHandle,     // Input
//      NULL,            // Input one residual packet or NULL
//      0,               // Input next number of packets
//      NULL,            // Input next buffer of data
//      NULL,            // Input next array of buffer pointers
//      &prevNumPackets, // Output previous number of packets
//      &pPrevPointers   // Output previous array of buffer pointers
//  );

RemapReturnCode RemapAndFilter(RemapHandle remapHandle, // Input

        RemapPacket * const pResidualPacket, // Input one residual packet or NULL

        const NumPackets nextNumPackets, // Input next number of packets
        RemapPacket * const pNextPackets, // Input next buffer of data
        RemapPacket ** const pNextPointers, // Input next array of buffer pointers

        NumPackets * const pPrevNumPackets, // Output previous number of packets
        RemapPacket *** const ppPrevPointers // Output previous array of buffer pointers
)
{
    NumPackets actualNumPackets = 0;

    if (remapHandle == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: remapHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }
    if (pPrevNumPackets == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: pPrevNumPackets == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }
    if (ppPrevPointers == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: ppPrevPointers == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }

    // There are four cases:
    //
    //      case  nextNumPackets  pResidualPacket  Action
    //      ----  --------------  ---------------  ------
    //        0)               0             NULL  Return last data
    //        1)           not 0             NULL  Process packets (no residual)
    //        2)               0         not NULL  Process residual (no packets)
    //        3)           not 0         not NULL  Process both

    remapHandle->msbPtrGotSet = RemapFalse;

    if (pResidualPacket) // case 2) or case 3)
    {
        if (pNextPointers == NULL)
        {
            RILOG_ERROR(
                    "RemapReturnCodeBadInputParameter: pNextPointers == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            return RemapReturnCodeBadInputParameter;
        }

        if (RemapOnePacket(remapHandle, pResidualPacket->bytes))
            pNextPointers[actualNumPackets++] = pResidualPacket;
    }

    if (nextNumPackets) // case 1) or case 3)
    {
        NumPackets i;

        if (pNextPackets == NULL)
        {
            RILOG_ERROR(
                    "RemapReturnCodeBadInputParameter: pNextPackets == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            return RemapReturnCodeBadInputParameter;
        }
        if (pNextPointers == NULL)
        {
            RILOG_ERROR(
                    "RemapReturnCodeBadInputParameter: pNextPointers == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            return RemapReturnCodeBadInputParameter;
        }

        for (i = 0; i < nextNumPackets; i++)
        {
            RemapPacket * pRemapPacket = pNextPackets + i;
            if (RemapOnePacket(remapHandle, pRemapPacket->bytes))
                pNextPointers[actualNumPackets++] = pRemapPacket;
        }
    }

    *pPrevNumPackets = remapHandle->prevNumPackets;
    *ppPrevPointers = remapHandle->pPrevPointers;

    remapHandle->prevNumPackets = actualNumPackets;
    remapHandle->pPrevPointers = pNextPointers;

    if (remapHandle->msbPtrGotSet == RemapFalse)
        remapHandle->pPidMsb = NULL;

    return RemapReturnCodeNoErrorReported;
}

//  Close a Remap object and return any remaining packet and pointer memory to
//  the caller so it can be freed if necessary.

RemapReturnCode RemapClose(RemapHandle remapHandle, // Input

        NumPackets * const pPrevNumPackets, // Output previous number of packets (if any)
        RemapPacket *** const ppPrevPointers // Output previous array of buffer pointers (if any)
)
{
    if (remapHandle == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: remapHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }
    if (pPrevNumPackets == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: pPrevNumPackets == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }
    if (ppPrevPointers == NULL)
    {
        RILOG_ERROR(
                "RemapReturnCodeBadInputParameter: ppPrevPointers == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return RemapReturnCodeBadInputParameter;
    }

    *pPrevNumPackets = remapHandle->prevNumPackets;
    *ppPrevPointers = remapHandle->pPrevPointers;

    remapHandle->prevNumPackets = 0;
    remapHandle->pPrevPointers = NULL;

#ifdef SAVE_PAT_AND_PMT
    if (remapHandle->pSavePat) g_free(remapHandle->pSavePat);
#endif
    g_free(remapHandle->pRemapPids);
    g_free(remapHandle);

    return RemapReturnCodeNoErrorReported;
}
