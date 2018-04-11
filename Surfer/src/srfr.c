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

// Header files

#ifdef _WIN32
#include <windows.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

#include <gst/gst.h>

#include <ri_config.h>
#include <ri_pipeline.h>
#include <ri_pipeline_manager.h>
#include <ri_section_filter.h>
#include <ri_tuner.h>
#include <ri_ui_manager.h>

#include "platform.h"
#include "srfr.h"


#define CANNED_FILES

#define CRC_LEN     4

static ri_tune_params_t tuneData[] =
{
#ifdef CANNED_FILES
    { 447000000L, RI_MODULATION_QAM64, 1 },
    { 489000000L, RI_MODULATION_QAM256, 2 },
    { 599000000L, RI_MODULATION_QAM256, 3 },
    { 603000000L, RI_MODULATION_QAM256, 25991 },
    { 651000000L, RI_MODULATION_QAM256, 4 },
    { 699000000L, RI_MODULATION_QAM256, 25992 },
    { 645000000L, RI_MODULATION_QAM256, 1 },
#elif defined(COMCAST_SUBURBS)
    { 465000000L, RI_MODULATION_QAM256, 1 },
    { 465000000L, RI_MODULATION_QAM256, 2 },
    { 471000000L, RI_MODULATION_QAM256, 3 },
    { 471000000L, RI_MODULATION_QAM256, 4 },
    { 471000000L, RI_MODULATION_QAM256, 5 },
    { 471000000L, RI_MODULATION_QAM256, 6 },
    { 471000000L, RI_MODULATION_QAM256, 7 },
    { 477000000L, RI_MODULATION_QAM256, 1 },
    { 477000000L, RI_MODULATION_QAM256, 2 },
    { 477000000L, RI_MODULATION_QAM256, 4 },
    { 477000000L, RI_MODULATION_QAM256, 5 },
    { 483000000L, RI_MODULATION_QAM256, 1 },
    { 483000000L, RI_MODULATION_QAM256, 2 },
    { 489000000L, RI_MODULATION_QAM256, 1 },
    { 489000000L, RI_MODULATION_QAM256, 2 },
    { 489000000L, RI_MODULATION_QAM256, 3 },
    { 489000000L, RI_MODULATION_QAM256, 4 },
    { 495000000L, RI_MODULATION_QAM256, 1 },
    { 495000000L, RI_MODULATION_QAM256, 2 },
    { 567000000L, RI_MODULATION_QAM64, 9 },
    { 567000000L, RI_MODULATION_QAM64, 66 },
    { 573000000L, RI_MODULATION_QAM256, 2 },
    { 573000000L, RI_MODULATION_QAM256, 3 },
    { 573000000L, RI_MODULATION_QAM256, 4 },
    { 573000000L, RI_MODULATION_QAM256, 6 },
    { 573000000L, RI_MODULATION_QAM256, 9 },
    { 573000000L, RI_MODULATION_QAM256, 13 },
#else
    { 447000000L, RI_MODULATION_QAM64, 1 },
    { 447000000L, RI_MODULATION_QAM64, 2 },
    { 447000000L, RI_MODULATION_QAM64, 3 },
    { 447000000L, RI_MODULATION_QAM64, 4 },
    { 447000000L, RI_MODULATION_QAM64, 5 },
    { 447000000L, RI_MODULATION_QAM64, 6 },
    { 447000000L, RI_MODULATION_QAM64, 7 },
    { 447000000L, RI_MODULATION_QAM64, 8 },
    { 447000000L, RI_MODULATION_QAM64, 9 },
    { 447000000L, RI_MODULATION_QAM64, 10 },
    { 447000000L, RI_MODULATION_QAM64, 14 },
    { 465000000L, RI_MODULATION_QAM64, 2 },
    { 465000000L, RI_MODULATION_QAM64, 3 },
    { 465000000L, RI_MODULATION_QAM64, 4 },
    { 465000000L, RI_MODULATION_QAM64, 5 },
    { 465000000L, RI_MODULATION_QAM64, 6 },
    { 465000000L, RI_MODULATION_QAM64, 11 },
    { 465000000L, RI_MODULATION_QAM64, 12 },
#endif
};

typedef enum _TunerState
{
    TUNER_IDLE,
    TUNER_BUSY,
    TUNED_SYNC,
    TUNED_NOSYNC
} TunerState;

typedef struct _TunerData
{
    int index;
    int chan;
    TunerState state;
    ri_tune_params_t params;
    ri_tuner_t *tuner;
    ri_pipeline_t *pipe;
    uint32_t PAT_filter_id;
    uint32_t PMT_filter_id;
} TunerData;

typedef enum _DecoderState
{
    DEC_IDLE,
    DEC_BUSY,
    DEC_PLAYING,
    DEC_PAUSED
} DecoderState;

typedef struct _DecoderData
{
    DecoderState state;
    TunerData *pTD;
    ri_display_t *pDisp;
    ri_video_device_t *pVD;
    ri_pid_info_t *pids;
    int numPids;
} DecoderData;

static struct srfr_private_data
{
    gboolean power;
    uint32_t nTuners;
    uint32_t curTuner;
    ri_ui_manager_t *ri_ui_manager;
    TunerData td[MAX_TUNERS];
    DecoderData fsDec;
    DecoderData pipDec;
} srfr;

void hex_dump(void *data, int size)
{
    unsigned char *p = data;
    unsigned char c;
    int n;
    char bytestr[4];
    char addrstr[10];
    char hexstr[16 * 3 + 5];
    char charstr[16 * 1 + 5];

    if (NULL == p)
    {
        return;
    }

    memset(bytestr, 0, 4);
    memset(addrstr, 0, 10);
    memset(hexstr, 0, 16 * 3 + 5);
    memset(charstr, 0, 16 * 1 + 5);

    for (n = 1; n <= size; n++)
    {
        if (n % 16 == 1)
        {
            /* store address for this line */
            (void) snprintf(addrstr, sizeof(addrstr), "%.4x", ((unsigned int) p
                    - (unsigned int) data));
        }

        c = *p;

        if (! (c >= ' ' && c <= '~'))
        {
            c = '.';
        }

        /* store hex str (for left side) */
        (void) snprintf(bytestr, sizeof(bytestr), "%02X ", *p);
        strncat(hexstr, bytestr, sizeof(hexstr) - strlen(hexstr) - 1);

        /* store char str (for right side) */
        (void) snprintf(bytestr, sizeof(bytestr), "%c", c);
        strncat(charstr, bytestr, sizeof(charstr) - strlen(charstr) - 1);

        if (n % 16 == 0)
        {
            /* line completed */
            printf("[%4.4s]   %-50.50s  %s\n", addrstr, hexstr, charstr);
            hexstr[0] = 0;
            charstr[0] = 0;
        }
        else if (n % 8 == 0)
        {
            /* half line: add whitespaces */
            strncat(hexstr, "  ", sizeof(hexstr) - strlen(hexstr) - 1);
            strncat(charstr, " ", sizeof(charstr) - strlen(charstr) - 1);
        }

        p++; /* next byte */
    }

    if (strlen(hexstr) > 0)
    {
        /* print rest of buffer if not empty */
        printf("[%4.4s]   %-50.50s  %s\n", addrstr, hexstr, charstr);
    }

    fflush(stdout);
}

char *srfr_log_formatter(char *buf, char *priority, char *category, char *msg)
{
#ifndef _WIN32
    struct timeval timestamp;
    struct tm tm;

    gettimeofday(&timestamp, NULL);
    localtime_r(&timestamp.tv_sec, &tm);
    if (0 > snprintf(buf, 1024,
            "%04d%02d%02d %02d:%02d:%02d.%03ld %-8s %s- %s", tm.tm_year + 1900,
            tm.tm_mon + 1, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec,
            timestamp.tv_usec / 1000, priority, category, msg))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }
#else
    SYSTEMTIME stime;

    GetLocalTime(&stime);
    if(0 > snprintf(buf, 1024, "%04d%02d%02d %02d:%02d:%02d.%03d %-8s %s- %s",
                    stime.wYear, stime.wMonth , stime.wDay,
                    stime.wHour, stime.wMinute, stime.wSecond,
                    stime.wMilliseconds,
                    priority, category, msg))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }
#endif
    return buf;
}

static uint8_t* parse_uint16(uint8_t* inBuf, uint16_t* out_int)
{
    uint16_t val = 0;

    val |= (inBuf[0] << 8) & 0xFF00;
    val |= inBuf[1] & 0xFF;
    inBuf += 2;

    *out_int = val;
    return inBuf;
}

uint32_t setPosFilter(uint16_t pid, uint8_t mask, uint8_t val, uint8_t len,
    void (*cb)(ri_section_filter_t *, uint32_t, uint32_t, uint8_t *, uint16_t))
{
    uint8_t neg_mask = 0;
    uint8_t neg_val = 0;
    uint8_t neg_len = 0;
    uint32_t filter_id = 0;
    TunerData *pTD = &srfr.td[srfr.curTuner];
    ri_section_filter_t *filter = pTD->pipe->get_section_filter(pTD->pipe);

    RILOG_TRACE("%s -- enter (%x, %d, %d, %d, %p)\n",
                 __func__, pid, mask, val, len, cb);

    if (pid <= 0x1FFF)
    {
        filter->create_filter(filter, &filter_id, pid, &mask, &val, len,
                              &neg_mask, &neg_val, neg_len, cb);
    }

    RILOG_TRACE("%s -- exit (%d)\n", __func__, filter_id);
    return filter_id;
}

static uint16_t getPmtPid(uint8_t *section, uint16_t len, uint16_t prog)
{
    int offset = 0;
    uint16_t progNum = 0xFFFF;
    uint16_t pid = 0x1FFF;

    RILOG_TRACE("%s -- enter (%p, %d, %d)\n", __func__, section, len, prog);
    hex_dump(section, len);
    section += 8;    // Jump to the program numbers...
    offset += 8;

    do
    {
        section = parse_uint16(section, &progNum);
        offset += 2;
        section = parse_uint16(section, &pid);
        offset += 2;
        pid &= 0x1FFF;
        RILOG_DEBUG("%s -- progNum:%d has pid:%x\n", __func__, progNum, pid);

        if ((0 != progNum) && (prog == progNum))
        {
            RILOG_TRACE("%s -- exit, found pid:%x\n", __func__, pid);
            return pid;
        }

    } while (offset - CRC_LEN < len);

    RILOG_WARN("%s -- exit, pid not found!?\n", __func__);
    return 0xFFFF;
}

static uint16_t getVideoPid(uint8_t *section, uint16_t len)
{
    int offset = 0;
    uint16_t progInfoLen = 0;
    uint16_t pid = 0x1FFF;
    uint8_t type = 0;

    RILOG_TRACE("%s -- enter (%p, %d)\n", __func__, section, len);
    hex_dump(section, len);
    section += 10;    // Jump to the program info length...
    offset += 10;
    section = parse_uint16(section, &progInfoLen);
    offset += 2;
    progInfoLen &= 0x0FFF;
    RILOG_DEBUG("%s -- progInfoLen:%d\n", __func__, progInfoLen);
    section += progInfoLen;    // Jump over the program info length...
    offset += progInfoLen;

    do
    {
        type = *section++;
        offset++;
        section = parse_uint16(section, &pid);
        offset += 2;
        pid &= 0x1FFF;
        RILOG_DEBUG("%s -- type:%d at pid:%x\n", __func__, type, pid);

        if ((RI_SI_ELEM_MPEG_2_VIDEO == type) ||
            (RI_SI_ELEM_VIDEO_DCII == type))
        {
            RILOG_TRACE("%s -- exit, found pid:%x\n", __func__, pid);
            return pid;
        }

    } while (offset - CRC_LEN < len);

    RILOG_WARN("%s -- exit, pid not found!?\n", __func__);
    return 0xFFFF;
}

void decodeStop(DecoderData *pDec)
{
    RILOG_TRACE("%s -- enter (%p)\n", __func__, pDec);

    switch (pDec->state)
    {
        case DEC_IDLE:
            break;
        case DEC_BUSY:
            pDec->state = DEC_IDLE;
            break;
        case DEC_PLAYING:
        case DEC_PAUSED:
            pDec->pTD->pipe->decode_stop(pDec->pTD->pipe);
            pDec->state = DEC_IDLE;
            break;
    }

    RILOG_TRACE("%s -- exit\n", __func__);
}

void decode(DecoderData *pDec, TunerData *pTD, ri_pid_info_t *pids, int numPids)
{
    int i = 0;

    RILOG_TRACE("%s -- enter (%p, %p, %p, %d)\n",
                __func__, pDec, pTD, pids, numPids);

    switch (pDec->state)
    {
        case DEC_IDLE:
            break;
        case DEC_BUSY:
        case DEC_PLAYING:
        case DEC_PAUSED:
            decodeStop(pDec);
            break;
    }

    pDec->pTD = pTD;
    pDec->state = DEC_BUSY;
    pDec->numPids = numPids;
 
    if (NULL == (pDec->pids = g_malloc0(sizeof(ri_pid_info_t) * pDec->numPids)))
    {
        RILOG_ERROR("%s: could not allocate memory for pids!?\n", __func__);
    }
    else
    {
        for (i = 0; i < pDec->numPids; i++)
        {
            pDec->pids[i].mediaType = pids[i].mediaType;
            pDec->pids[i].srcPid = pids[i].srcPid;
            pDec->pids[i].srcFormat = pids[i].srcFormat;
            RILOG_DEBUG("%s -- pid: %x, format: %d, type: %d\n", __func__,
                        pids[i].srcPid, pids[i].srcFormat, pids[i].mediaType);
        }

        pTD->pipe->decode(pTD->pipe, pDec->pVD, pDec->pids, pDec->numPids);
        pDec->state = DEC_PLAYING;
        g_free(pDec->pids);
    }

    RILOG_TRACE("%s -- exit\n", __func__);
}

void pmtDiscovered(ri_section_filter_t *filter, uint32_t sectionId,
                   uint32_t filterId, uint8_t *section, uint16_t len)
{
    int i;
    TunerData *pTD = NULL;
    ri_pid_info_t ri_pid;

    RILOG_TRACE("%s -- filter:%p, sectId:%d filtId:%d, sect:%p, len:%d\n",
                __func__, filter, sectionId, filterId, section, len);

    for (i = 0; i < srfr.nTuners; i++)
    {
        RILOG_TRACE("%s -- filterId:%d == srfr.td[%d].PMT_filter_id:%d?\n",
                    __func__, filterId, i, srfr.td[i].PMT_filter_id);

        if (filterId == srfr.td[i].PMT_filter_id)
        {
            pTD = &srfr.td[i];
            break;
        }
    }

    if (NULL == pTD)
    {
        RILOG_ERROR("%s -- invalid filter ID %d\n", __func__, filterId);
        filter->cancel_filter(filter, filterId);
        return;
    }

    filter->cancel_filter(filter, pTD->PMT_filter_id);
    pTD->PMT_filter_id = 0;
    ri_pid.srcPid = getVideoPid(section, len);
    ri_pid.srcFormat = RI_SI_ELEM_MPEG_2_VIDEO;
    ri_pid.mediaType = RI_MEDIA_TYPE_VIDEO;
    decode(&srfr.fsDec, pTD, &ri_pid, 1);

    RILOG_TRACE("%s -- exit\n", __func__);
}

void patDiscovered(ri_section_filter_t *filter, uint32_t sectionId,
                   uint32_t filterId, uint8_t *section, uint16_t len)
{
    int i;
    uint16_t pid = 0;
    TunerData *pTD = NULL;

    RILOG_TRACE("%s -- filter:%p, sectId:%d filtId:%d, sect:%p, len:%d\n",
                __func__, filter, sectionId, filterId, section, len);

    for (i = 0; i < srfr.nTuners; i++)
    {
        RILOG_TRACE("%s -- filterId:%d == srfr.td[%d].PAT_filter_id:%d?\n",
                    __func__, filterId, i, srfr.td[i].PAT_filter_id);

        if (filterId == srfr.td[i].PAT_filter_id)
        {
            pTD = &srfr.td[i];
            break;
        }
    }

    if (NULL == pTD)
    {
        RILOG_ERROR("%s -- invalid filter ID %d\n", __func__, filterId);
        filter->cancel_filter(filter, filterId);
        return;
    }

    filter->cancel_filter(filter, pTD->PAT_filter_id);
    pTD->PAT_filter_id = 0;
    pid = getPmtPid(section, len, pTD->params.program_num);
    pTD->PMT_filter_id = setPosFilter(pid, 255, 2, 1, pmtDiscovered);

    RILOG_TRACE("%s -- exit\n", __func__);
}

void tunerEvent(ri_tuner_event event, void *data)
{
    int idx = (int)data;
    uint16_t pid = 0;
    TunerData *pTD = &srfr.td[idx];

    RILOG_TRACE("%s -- event: %d, data: %d \n", __func__, event, idx);

    switch (event)
    {
        case RI_TUNER_EVENT_FAIL:
            switch (pTD->state)
            {
                case TUNER_BUSY:
                    RILOG_INFO("%s -- tuner %d FAIL\n", __func__, idx);
                    pTD->state = TUNER_IDLE;
                    break;
                default:
                    RILOG_WARN("%s -- tuner %d unexpected event FAIL in %d\n",
                                __func__, idx, pTD->state);
                    break;
            }
            break;
        case RI_TUNER_EVENT_SYNC:
            switch (pTD->state)
            {
                case TUNER_BUSY:
                    RILOG_INFO("%s -- tuner %d BUSY->SYNC\n", __func__, idx);
                    pTD->state = TUNED_SYNC;
                    pTD->PAT_filter_id = setPosFilter(pid, 255, 0, 1,
                                                      patDiscovered);
                    break;
                case TUNED_NOSYNC:
                    RILOG_INFO("%s -- tuner %d NOSYNC->SYNC\n", __func__, idx);
                    pTD->state = TUNED_SYNC;
                    break;
                default:
                    RILOG_WARN("%s -- tuner %d unexpected event SYNC in %d\n",
                                __func__, idx, pTD->state);
                    break;
            }
            break;
        case RI_TUNER_EVENT_NOSYNC:
            switch (pTD->state)
            {
                case TUNER_BUSY:
                    RILOG_INFO("%s -- tuner %d BUSY->NOSYNC\n", __func__, idx);
                    pTD->state = TUNED_NOSYNC;
                    break;
                case TUNED_NOSYNC:
                    RILOG_INFO("%s -- tuner %d SYNC->NOSYNC\n", __func__, idx);
                    pTD->state = TUNED_NOSYNC;
                    break;
                default:
                    RILOG_WARN("%s -- tuner %d unexpected event NOSYNC in %d\n",
                                __func__, idx, pTD->state);
                    break;
            }
            break;
        default:
            RILOG_ERROR("%s -- tuner %d invalid event %d\n",
                        __func__, idx, event);
            break;
    }
}

void tune(int tuneDataIndex, int tunerIndex)
{
    ri_error retVal = RI_ERROR_NONE;
    TunerData *pTD = &srfr.td[tunerIndex];

    RILOG_TRACE("%s -- enter (%d, %d)\n", __func__, tuneDataIndex, tunerIndex);

    pTD->params.frequency = tuneData[tuneDataIndex].frequency;
    pTD->params.mode = tuneData[tuneDataIndex].mode;
    pTD->params.program_num = tuneData[tuneDataIndex].program_num;

    if (TUNER_IDLE == pTD->state)
    {
        pTD->state = TUNER_BUSY;
    }
    else
    {
        RILOG_INFO("%s - dump previous tune request (in %d)?\n",
                   __func__, pTD->state);
        pTD->state = TUNER_BUSY;
    }

    retVal = pTD->tuner->request_tune(pTD->tuner, pTD->params);

    if (RI_ERROR_NONE != retVal)
    {
        RILOG_ERROR("%s - %d = request_tune(F:%u, M:%d, P:%d)\n", __func__,
                    retVal, pTD->params.frequency, pTD->params.mode,
                    pTD->params.program_num);
    }
    else
    {
        RILOG_INFO("%s - successful tune(F:%u, M:%d, P:%d)\n", __func__,
                   pTD->params.frequency, pTD->params.mode,
                   pTD->params.program_num);
    }

    RILOG_TRACE("%s -- exit\n", __func__);
}

void srfr_key_event_cb(ri_event_type type, ri_event_code key)
{
    TunerData *pTD = &srfr.td[srfr.curTuner];

    RILOG_TRACE("%s -- enter (type: %d, key: %d)\n", __func__, type, key);

    switch (key)
    {
        case RI_VK_BACK_SPACE:
            if (RI_EVENT_TYPE_PRESSED == type)
            {
                tune(pTD->chan, srfr.curTuner);
            }
            break;
        case RI_VK_CHANNEL_UP:
            if (RI_EVENT_TYPE_PRESSED == type)
            {
                if (pTD->chan+1 >= (sizeof(tuneData)/sizeof(ri_tune_params_t)))
                    pTD->chan = 0;
                else
                    pTD->chan++;

                tune(pTD->chan, srfr.curTuner);
            }
            break;
        case RI_VK_CHANNEL_DOWN:
            if (RI_EVENT_TYPE_PRESSED == type)
            {
                if (pTD->chan-1 < 0)
                    pTD->chan = (sizeof(tuneData)/sizeof(ri_tune_params_t)) - 1;
                else
                    pTD->chan--;

                tune(pTD->chan, srfr.curTuner);
            }
            break;
        case RI_VK_POWER:
            if (RI_EVENT_TYPE_PRESSED == type)
            {
                if (FALSE == srfr.power)
                {
                    srfr.power = TRUE;
                    tune(pTD->chan, srfr.curTuner);
                }
                else
                {
                    srfr.power = FALSE;
                    decodeStop(&srfr.fsDec);
                }
            }
            break;
        case RI_VK_LIVE:
            if (RI_EVENT_TYPE_PRESSED == type)
            {
                if (srfr.nTuners == 1)
                {
                    RILOG_INFO("%s: srfr.nTuners == 1 can't swap!\n", __func__);
                    break;
                }

                decodeStop(&srfr.fsDec);
                srfr.curTuner = (0 == srfr.curTuner)?  1 : 0;
                RILOG_INFO("%s: srfr.curTuner = %d\n", __func__, srfr.curTuner);
                pTD = &srfr.td[srfr.curTuner];
                pTD->PAT_filter_id = setPosFilter(0, 255, 0, 1, patDiscovered);
            }
            break;
        default:
            break;
    }

    RILOG_TRACE("%s -- exit\n", __func__);
}

gpointer surfer(gpointer srfr_data)
{
    RILOG_TRACE("%s -- enter\n", __func__);

    if (NULL != (srfr.ri_ui_manager = ri_get_ui_manager()))
    {
        RILOG_INFO("%s -- registering for events...\n", __func__);
        srfr.ri_ui_manager->register_key_event_cb(
                            srfr.ri_ui_manager, srfr_key_event_cb);
    }

    RILOG_TRACE("%s -- exit\n", __func__);
    return NULL;
}

void srfr_init(int argc, char *argv[])
{
    int i;
    char *ipAddr;
    ri_pipeline_manager_t* pipelineMgr = NULL;
    const ri_pipeline_t** pipelines = NULL;

    RILOG_TRACE("%s -- enter\n", __func__);

    // Retrieve our configuration file name
    if (argc < 2)
    {
        RILOG_ERROR("%s: config file not passed in...\n", __func__);
        exit(1);
    }

    // Load and parse our config file
    if (ricfg_parseConfigFile("Surfer", argv[1]) != RICONFIG_SUCCESS)
    {
        RILOG_ERROR("%s: Couldn't parse platform config file (%s)\n",
               __func__, argv[1]);
        exit(2);
    }

    // Get the RI Platform IP address...
    if (NULL == (ipAddr = ricfg_getValue("RIPlatform",
                                         "RI.Platform.IpAddr")))
    {
        ipAddr = "127.0.0.1";
        RILOG_WARN("%s RI Platform IP address not found!\n", __func__);
    }

    // Get the RI Platform pipeline manager and pipeline array
    if (NULL != (pipelineMgr = ri_get_pipeline_manager()))
    {
        pipelines = pipelineMgr->get_live_pipelines(pipelineMgr, &srfr.nTuners);

        if (NULL == pipelines)
        {
            RILOG_ERROR("%s: Platform returned NULL pipelines!?\n", __func__);
            exit(3);
        }
    }
    else
    {
        RILOG_ERROR("%s: Platform returned NULL pipelineMgr!?\n", __func__);
        exit(4);
    }

    RILOG_INFO("%s: srfr.nTuners = %d\n", __func__, srfr.nTuners);

    // initialize the tuner data for each tuner found...
    for (i = 0; i < srfr.nTuners; i++)
    {
        ri_tuner_t *pT = pipelines[i]->get_tuner((ri_pipeline_t*)pipelines[i]);

        if (NULL == pT)
        {
            RILOG_ERROR("%s: Platform returned NULL tuner [%d]\n", __func__, i);
            exit(5);
        }

        srfr.td[i].index = i;
        srfr.td[i].chan = 0;
        srfr.td[i].state = TUNER_IDLE;
        srfr.td[i].params.frequency = 0;
        srfr.td[i].params.mode = 0;
        srfr.td[i].params.program_num = 0;
        srfr.td[i].tuner = pT;
        srfr.td[i].tuner->register_tuner_event_cb(pT, tunerEvent, (void*) i);
        srfr.td[i].pipe = (ri_pipeline_t *)pipelines[i];
    }

    // initialize the decoder data for the full screen and pip decoders...
    srfr.fsDec.state = DEC_IDLE;
    srfr.fsDec.pTD = NULL;
    srfr.fsDec.pDisp = pipelineMgr->get_display(pipelineMgr);
    srfr.fsDec.pVD = srfr.fsDec.pDisp->get_video_device(srfr.fsDec.pDisp);
    srfr.fsDec.pids = NULL;
    srfr.pipDec.state = DEC_IDLE;
    srfr.pipDec.pTD = NULL;
    srfr.pipDec.pVD = NULL;
    srfr.pipDec.pids = NULL;

    srfr.power = FALSE;
    srfr.curTuner = 0;
    srfr.ri_ui_manager = NULL;

    RILOG_INFO("%s -- Spawning surfer thread\n", __func__);
    g_thread_create(surfer, NULL, TRUE, NULL);

    RILOG_TRACE("%s -- exit\n", __func__);
}

void srfr_term(void)
{
    RILOG_TRACE("%s -- called\n", __func__);
}

