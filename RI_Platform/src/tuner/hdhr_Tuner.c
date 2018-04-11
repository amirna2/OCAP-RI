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

#include <platform.h>
#include <ri_log.h>

#include <hdhomerun.h>

#include "hdhr_Tuner.h"

// Logging category
log4c_category_t* hdhr_RILogCategory = NULL;

// Use HDHR category for logs in this file
#define RILOG_CATEGORY hdhr_RILogCategory

#define CHECK_INITIALIZATION(index) (tuner[index].mInitialized)

// contain all the module data hdhr_Tuner requires:
static struct Tuner
{
    // streamer Launch reply buffer
    char rxBuf[RCVBUFSIZE];
    // set when streamer is active
    ri_bool mStreaming;
    // set when initialized
    ri_bool mInitialized;
    // streamer control ID
    long mId;
    // streamer tuner number
    int mTnum;
    // HDHR control socket
    struct hdhomerun_control_sock_t *ctlSock;
    char channelMap[10];
} tuner[MAX_TUNERS];

/**
 * Create the HDHR subprocess for this command
 * @param index - tuner index
 * @param command - HDHR command line to launch
 * @param id - HDHR identifier
 */
static char *launchHdhrCommand(int index, char *command)
{
    RILOG_DEBUG("%s Entry, (%d, \"%s\");\n", __FUNCTION__, index, command);
    char *rxBuf = tuner[index].rxBuf;
    char *p, *getSet, *cmd, *arg;
    char *err = NULL, *val = NULL;
    char buf[512];
    int rc;
    struct hdhomerun_control_sock_t *ctlSock = tuner[index].ctlSock;

    if (NULL == command)
    {
        return "ERROR - command == NULL!?";
    }
    else if (NULL == ctlSock)
    {
        return "ERROR - hdhomerun_control_ctlSock not initialized!?";
    }

    // split the command string for hdhomerun_control_[get/set]
    strncpy(buf, command, sizeof buf);
    getSet = strtok(buf, " ");
    cmd = strtok(NULL, " ");
    arg = strtok(NULL, " ");

    // Replace /tuner%d with tuner index
    if (NULL != (p = strstr(cmd, "tunerN")))
    {
        p += strlen("tuner");
        *p = '0' + tuner[index].mTnum;
    }

    if (strstr(getSet, "get"))
    {
        RILOG_INFO("%s get cmd: %s, ctlSock: %p, mId: %08lX\n",
               __FUNCTION__, cmd, ctlSock, tuner[index].mId);

        if (1 != (rc = hdhomerun_control_get(ctlSock, cmd, &val, &err)))
        {
             RILOG_ERROR("%s %d = hdhomerun_control_get(%s) ERROR\n== %s\n",
                   __FUNCTION__, rc, cmd, err);
            if (NULL != err)
            {
                return err;
            }
            else
            {
                return "hdhomerun_control_get() ERROR";
            }
        }
    }
    else
    {
        RILOG_INFO("%s set cmd: %s, arg: %s, ctlSock: %p, mId: %08lX\n",
               __FUNCTION__, cmd, arg, ctlSock, tuner[index].mId);

        if (1 != (rc = hdhomerun_control_set(ctlSock, cmd, arg, &val, &err)))
        {
             RILOG_ERROR("%s %d = hdhomerun_control_set(%s) ERROR\n== %s\n",
                   __FUNCTION__, rc, cmd, err);
            if (NULL != err)
            {
                return err;
            }
            else
            {
                return "hdhomerun_control_set() ERROR";
            }
        }
    }

    if (NULL != val)
    {
        strncpy(tuner[index].rxBuf, val, sizeof(tuner[index].rxBuf));
    }
    else
    {
        sprintf(tuner[index].rxBuf, "NULL");
    }

    RILOG_INFO("%s hdhomerun_control() returned: %s\n", __FUNCTION__, rxBuf);
    return rxBuf;
}

void hdhr_TunerInit(int index, long id)
{
    char *tunerNum, *path, hdhrPath[512], cfgVar[80], *channelMap;

    // Create our logging category
    hdhr_RILogCategory = log4c_category_get("RI.Tuner.HDHR");

    RILOG_DEBUG("%s -- (%d, %lx);\n", __FUNCTION__, index, id);
    tuner[index].mInitialized = FALSE;
    tuner[index].mStreaming = FALSE;
    tuner[index].ctlSock = NULL;
    tuner[index].channelMap[0] = 0;

    // Get HDHR directory...
    if ((path = ricfg_getValue("RIPlatform", "RI.Headend.resources.directory")))
    {
        if (0 > snprintf(hdhrPath, sizeof(hdhrPath), "%s", path))
        {
            RILOG_ERROR("%s -- snprintf failure?!\n", __FUNCTION__);
        }
        strcat(hdhrPath, HdhrSourceCmd);
    }
    else
    {
        RILOG_FATAL(-9, "%s -- HDHR directory not specified!\n", __FUNCTION__);
    }

    // Get HDHR tuner index...
    sprintf(cfgVar, "RI.Headend.tuner.%d.%s", index, "tunerNum");
    if (NULL == (tunerNum = ricfg_getValue("RIPlatform", cfgVar)))
    {
        tuner[index].mTnum = 0;
        RILOG_WARN("%s -- %s not specified!\n", __FUNCTION__, cfgVar);
    }
    else
    {
        tuner[index].mTnum = atoi(tunerNum);
        RILOG_DEBUG("%s -- tunerNum = %d\n", __FUNCTION__, tuner[index].mTnum);
    }

    // Get the signal format/channel map
    sprintf(cfgVar, "RI.Headend.tuner.%d.%s", index, "ChannelMap");
    if (NULL == (channelMap = ricfg_getValue("RIPlatform", cfgVar)))
    {
        strcpy(tuner[index].channelMap, "us-cable");
        RILOG_DEBUG("%s -- %s not specified (defaulting to 'us-cable')\n", __FUNCTION__, cfgVar);
    }
    else
    {
        strncpy(tuner[index].channelMap, channelMap, sizeof(tuner[index].channelMap)-1);
        RILOG_DEBUG("%s -- channelMap = %s\n", __FUNCTION__, tuner[index].channelMap);
    }

    tuner[index].mId = id;
    tuner[index].ctlSock = hdhomerun_control_create(id, 0, NULL);
    tuner[index].mInitialized = TRUE;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_bool hdhr_TunerTune(int index, Stream *stream)
{
    ri_bool retVal = FALSE;
    char command[512];
    char *result;
    int i;

    RILOG_DEBUG("%s -- Entry,(%d, %p);\n", __FUNCTION__, index, stream);

    if (!CHECK_INITIALIZATION(index))
    {
        RILOG_ERROR("%s -- Exit: tuner not initialized!?\n", __FUNCTION__);
        return retVal;
    }

    if (NULL == stream)
    {
        RILOG_ERROR("%s -- Exit: NULL Stream?!\n", __FUNCTION__);
        return retVal;
    }

    // Clean-up the previous stream
    if (tuner[index].mStreaming)
    {
        (void) launchHdhrCommand(index, cmdSetNoTarget);
    }

    sprintf(command, cmdSetChannelMap, tuner[index].channelMap);
    (void) launchHdhrCommand(index, command);

    sprintf(command, cmdSetChannel, stream->srcUrl);
    (void) launchHdhrCommand(index, command);

    for (i = 0; i < HDHR_CONNECT_ATTEMPTS; i++)
    {
        g_usleep(50000); // SiliconDust requires a sleep before requesting status

        result = launchHdhrCommand(index, cmdGetTuneSts);

        if (NULL != result && (strstr(result, "lock=qam") || (strstr(result, "lock=8vsb"))))
        {
            break;
        }

        g_usleep(HDHR_TUNE_DELAY / HDHR_CONNECT_ATTEMPTS);
    }

    if (i < HDHR_CONNECT_ATTEMPTS)
    {
        retVal = tuner[index].mStreaming = TRUE;

        // Set-up the play
        sprintf(command, cmdSetTarget, stream->destinationAddress,
                stream->destinationPort);
        (void) launchHdhrCommand(index, command);
    }
    else
    {
        tuner[index].mStreaming = FALSE;
        RILOG_ERROR("%s -- tuning error for Tuner%d\n", __FUNCTION__,
                tuner[index].mTnum);
    }

    RILOG_DEBUG("%s -- Exit, Returning: %s\n", __FUNCTION__, boolStr(retVal));
    return retVal;
}

char *hdhr_TunerStatus(int index)
{
    RILOG_DEBUG("%s -- Entry, (%d);\n", __FUNCTION__, index);

    if (!CHECK_INITIALIZATION(index))
    {
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return "ERROR - tuner not initialized!?";
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return launchHdhrCommand(index, cmdGetTuneSts);
}

/**
 * Orders a stream to stop.  Not a normal tuner operation,
 * as it typically is tuned somewhere.
 */
void hdhr_TunerStop(int index)
{
    RILOG_DEBUG("%s -- (%d);\n", __FUNCTION__, index);

    if (tuner[index].mStreaming)
    {
        (void) launchHdhrCommand(index, cmdSetNoTarget);
        tuner[index].mStreaming = FALSE;
    }

    RILOG_INFO("%s -- nothing to do, tuner not streaming...\n", __FUNCTION__);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Shut-down HDHR instance
 */
void hdhr_TunerExit(int index)
{
    RILOG_DEBUG("%s -- Entry(%d);\n", __FUNCTION__, index);

    if (!CHECK_INITIALIZATION(index))
    {
        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    if (tuner[index].mStreaming)
    {
        hdhr_TunerStop(index);
    }

    hdhomerun_control_destroy(tuner[index].ctlSock);
    tuner[index].ctlSock = NULL;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_bool hdhr_TunerIsStreaming(int index)
{
    return tuner[index].mStreaming;
}

/**
 * update transport stream PID list
 *
 * @param object The tuner "this" pointer
 * @return An error code detailing the success or failure of the request.
 */
ri_error hdhr_TunerUpdatePidList(int index, guint16 pids[MAX_PIDS])
{
    RILOG_DEBUG("%s -- did not update PID list for HDHR tuner\n", __func__);
    return RI_ERROR_NONE;
}

