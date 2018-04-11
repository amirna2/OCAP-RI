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

#include "mpeos_dbg.h"
#include "mpeos_media.h"
#include "mpeos_event.h"

#include <stdlib.h>
#include <string.h>

#include <ri_test_interface.h>

int test3DTVInputHandler(int sock, char* rxBuf, mpe_EventQueue queue, void* act,
        mpe_Media3DPayloadType* payloadType, mpe_DispStereoscopicMode* stereoscopicMode,
        uint8_t** payload, uint32_t* payloadSz, mpe_MediaScanMode* scanMode)
{
    int i = 0;
    char *p = NULL;
    char buf[1024];

    //assuming transition from some 3d format (supported or unsupported)
    if (strstr(rxBuf, "1"))
    {
        *stereoscopicMode = MPE_SSMODE_2D;
        *payloadType = 0;

        //enqueue format change event and 2D success event
#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_FROM_3D_TO_2D, act, 0);
            mpeos_eventQueueSend(queue, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_2D_SUCCESS, act, 0);
        }
    }
    else if (strstr(rxBuf, "2"))
    {
        mpe_Media3DPayloadType currentType = *payloadType;
        *stereoscopicMode = MPE_3D_MPEG2_USER_DATA_TYPE;
        *payloadType = MPE_SSMODE_3D_SIDE_BY_SIDE;

#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            //type set to null if 2D
            if ((mpe_Media3DPayloadType)NULL == currentType)
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_FROM_2D_TO_3D, act, 0);
            }
            else
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_OF_3D_FORMAT, act, 0);
            }
            mpeos_eventQueueSend(queue, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_3D_SUCCESS, act, 0);
        }
    }
    else if (strstr(rxBuf, "3"))
    {
        mpe_Media3DPayloadType currentType = *payloadType;
        *stereoscopicMode = MPE_3D_MPEG2_USER_DATA_TYPE;
        *payloadType = MPE_SSMODE_3D_TOP_AND_BOTTOM;

#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            //type set to null if 2D
            if (0 == currentType)
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_FROM_2D_TO_3D, act, 0);
            }
            else
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_OF_3D_FORMAT, act, 0);
            }
            mpeos_eventQueueSend(queue, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_3D_SUCCESS, act, 0);
        }
    }
    else if (strstr(rxBuf, "4"))
    {
        mpe_Media3DPayloadType currentType = *payloadType;
        *stereoscopicMode = MPE_3D_AVC_SEI_PAYLOAD_TYPE;
        *payloadType = MPE_SSMODE_3D_SIDE_BY_SIDE;

#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            //type set to null if 2D
            if ((mpe_Media3DPayloadType)NULL == currentType)
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_FROM_2D_TO_3D, act, 0);
            }
            else
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_OF_3D_FORMAT, act, 0);
            }
            mpeos_eventQueueSend(queue, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_3D_SUCCESS, act, 0);
        }
    }
    else if (strstr(rxBuf, "5"))
    {
        mpe_Media3DPayloadType currentType = *payloadType;
        *stereoscopicMode = MPE_3D_AVC_SEI_PAYLOAD_TYPE;
        *payloadType = MPE_SSMODE_3D_TOP_AND_BOTTOM;

#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            //type set to null if 2D
            if (0 == currentType)
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_FROM_2D_TO_3D, act, 0);
            }
            else
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_OF_3D_FORMAT, act, 0);
            }
            mpeos_eventQueueSend(queue, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_3D_SUCCESS, act, 0);
        }
    }
    else if (strstr(rxBuf, "6"))
    {
        mpe_Media3DPayloadType currentType = *payloadType;
        *stereoscopicMode = MPE_3D_MPEG2_USER_DATA_TYPE;
        *payloadType = MPE_SSMODE_3D_SIDE_BY_SIDE;

#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            //type set to null if 2D
            if (0 == currentType)
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_FROM_2D_TO_3D, act, 0);
            }
            else
            {
                //assuming last format wasn't mpeg2 user data/side by side..
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_OF_3D_FORMAT, act, 0);
            }
            mpeos_eventQueueSend(queue, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_3D_FORMAT_UNCONFIRMED, act, 0);
        }
    }
    else if (strstr(rxBuf, "7"))
    {
        mpe_Media3DPayloadType currentType = *payloadType;
        *stereoscopicMode = MPE_3D_MPEG2_USER_DATA_TYPE;
        *payloadType = MPE_SSMODE_3D_SIDE_BY_SIDE;

#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            //type set to null if 2D
            if (0 == currentType)
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_FROM_2D_TO_3D, act, 0);
            }
            else
            {
                //assuming last format wasn't mpeg2 user data/side by side..
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_OF_3D_FORMAT, act, 0);
            }
            mpeos_eventQueueSend(queue, MPE_CONTENT_NOT_PRESENTING, (void*)MPE_NOT_PRESENTING_3D_DISPLAY_DEVICE_NOT_CAPABLE, act, 0);
        }
    }
    else if (strstr(rxBuf, "8"))
    {
        mpe_Media3DPayloadType currentType = *payloadType;
        *stereoscopicMode = MPE_3D_MPEG2_USER_DATA_TYPE;
        *payloadType = MPE_SSMODE_3D_SIDE_BY_SIDE;

#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            //type set to null if 2D
            if (0 == currentType)
            {
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_FROM_2D_TO_3D, act, 0);
            }
            else
            {
                //assuming last format wasn't mpeg2 user data/side by side..
                mpeos_eventQueueSend(queue, MPE_3D_FORMAT_CHANGED, (void*)S3D_TRANSITION_OF_3D_FORMAT, act, 0);
            }
            mpeos_eventQueueSend(queue, MPE_CONTENT_NOT_PRESENTING, (void*)MPE_NOT_PRESENTING_3D_NO_CONNECTED_DISPLAY_DEVICE, act, 0);
        }
    }
    else if (strstr(rxBuf, "n"))
    {
#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            mpeos_eventQueueSend(queue, MPE_CONTENT_NOT_PRESENTING, (void*)MPE_NOT_PRESENTING_NO_DATA, act, 0);
        }
    }
    else if (strstr(rxBuf, "d"))
    {
        mpe_Media3DPayloadType currentType = *payloadType;

#ifdef __linux__
        if (queue != 0 && act != NULL)
#else
        if (queue != NULL && act != NULL)
#endif
        {
            //type set to null if 2D
            if ((mpe_Media3DPayloadType)NULL == currentType)
            {
                mpeos_eventQueueSend(queue, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_2D_SUCCESS, act, 0);
            }
            else
            {
                mpeos_eventQueueSend(queue, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_3D_SUCCESS, act, 0);
            }
        }
    }
    else if (strstr(rxBuf, "p"))
    {
        int i=0;
        char temp[100];
        ri_test_SendString(sock, "\r\n\nset 3D Payload Size\r\n");
        *payloadSz = ri_test_GetNumber(sock, buf, sizeof(buf),
                              "\r\npayload value index: ", 0);
        if (*payload != NULL) free (*payload);

        *payload = (uint8_t*) malloc (*payloadSz);

        for (i=0; i<*payloadSz; i++)
        {
            sprintf (temp, "\r\npayload[%d]: ", i);
            *payload[i] = ri_test_GetNumber(sock, buf, sizeof(buf), temp, *payload[i]);
        }
    }
    else if (strstr(rxBuf, "s"))
    {
        ri_test_SendString(sock, "\r\n\nShow 3D settings...\r\n");
        sprintf(buf, "\r\ndata:%d, format:%d\r\n", *payloadType, *stereoscopicMode);
        ri_test_SendString(sock, buf);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - %s);\n", __FUNCTION__, buf);
        p = buf;
        p += sprintf(p, "\r\npayload[%d] = {", *payloadSz);

        for (i = 0; i < *payloadSz; i++)
        {
            p += sprintf(p, " %d,", (*payload)[i]);
        }

        sprintf(p-1, " };\r\n");
        ri_test_SendString(sock, buf);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - %s);\n", __FUNCTION__, buf);
    }
    else if (strstr(rxBuf, "m"))
    {
        *scanMode = ri_test_GetNumber(sock, buf, sizeof(buf),
            "\r\nSet Scan Mode (0 = UNKNOWN, 1=INTERLACED, 2=PROGRESSIVE): ", 0);
    }
    else if (strstr(rxBuf, "g"))
    {
        sprintf (buf, "ScanMode = %d", *scanMode); 
        ri_test_SendString(sock, buf);
    }
    else
    {
        strcat(rxBuf, " - unrecognized\r\n\n");
        ri_test_SendString(sock, rxBuf);
    }

    return 0;
}
