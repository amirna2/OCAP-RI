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

#include <mpe_file.h>
#include <mpetest_file.h>
#include <test_media.h>
#include <mpe_media.h>

/**
 * decodeError
 * @returns String version of the error.
 */

static char msgbuf[40];

char* decodeError(mpe_Error ec)
{
    switch (ec)
    {
    case MPE_SUCCESS:
        return ("MPE_SUCCESS");
        break;
    case MPE_EINVAL:
        return ("MPE_EINVAL");
        break;
    case MPE_ENOMEM:
        return ("MPE_ENOMEM");
        break;
    case MPE_EBUSY:
        return ("MPE_EBUSY");
        break;
    case MPE_EMUTEX:
        return ("MPE_EMUTEX");
        break;
    case MPE_ECOND:
        return ("MPE_ECOND");
        break;
    case MPE_EEVENT:
        return ("MPE_EEVENT");
        break;
    case MPE_ENODATA:
        return ("MPE_ENODATA");
        break;
    case MPE_ETIMEOUT:
        return ("MPE_ETIMEOUT");
        break;
    case MPE_ETHREADDEATH:
        return ("MPE_ETHREADDEATH");
        break;
    case MPE_FS_ERROR_FAILURE:
        return "FS_Failure";
        break;
    case MPE_FS_ERROR_ALREADY_EXISTS:
        return "FS_AlreadyExists";
        break;
    case MPE_FS_ERROR_NOT_FOUND:
        return "FS_NotFound";
        break;
    case MPE_FS_ERROR_EOF:
        return "FS_EOF";
        break;
    case MPE_FS_ERROR_DEVICE_FAILURE:
        return "FS_DeviceFailure";
        break;
    case MPE_FS_ERROR_INVALID_STATE:
        return "FS_InvalidState";
        break;
    case MPE_FS_ERROR_READ_ONLY:
        return "FS_ReadOnly";
        break;
    case MPE_FS_ERROR_NO_MOUNT:
        return "FS_NoMount";
        break;
    case MPE_FS_ERROR_UNSUPPORT:
        return "FS_Unsupported";
        break;
    case MPE_FS_ERROR_NOTHING_TO_ABORT:
        return "FS_NothingToAbort";
        break;
    case MPE_FS_ERROR_UNKNOWN_URL:
        return "FS_UnknownUrl";
        break;
    case MPE_FS_ERROR_INVALID_DATA:
        return "FS_InvalidData";
        break;
    case MPE_FS_ERROR_DISCONNECTED:
        return "FS_Disconnected";
        break;
#ifndef _WINDOWS
    case MPE_ERROR_MEDIA_BAD_TUNING_REQUEST:
        return ("MPE_ERROR_MEDIA_BAD_TUNING_REQUEST");
        break;
    case MPE_ERROR_MEDIA_INVALID_ID:
        return ("MPE_ERROR_MEDIA_INVALID_ID");
        break;
    case MPE_ERROR_MEDIA_INVALID_CHANNEL:
        return ("MPE_ERROR_MEDIA_INVALID_CHANNEL");
        break;
    case MPE_ERROR_MEDIA_INVALID_SOURCEID:
        return ("MPE_ERROR_MEDIA_INVALID_SOURCEID");
        break;
    case MPE_ERROR_MEDIA_INVALID_PLAYER:
        return ("MPE_ERROR_MEDIA_INVALID_PLAYER");
        break;
    case MPE_ERROR_MEDIA_NO_LONGER_AUTHORIZED:
        return ("MPE_ERROR_MEDIA_NO_LONGER_AUTHORIZED");
        break;
    case MPE_ERROR_MEDIA_AUTHORIZATION_FAILED:
        return ("MPE_ERROR_MEDIA_AUTHORIZATION_FAILED");
        break;
    case MPE_ERROR_MEDIA_API_NOT_IMPLEMENTED:
        return ("MPE_ERROR_MEDIA_API_NOT_IMPLEMENTED");
        break;
    case MPE_ERROR_MEDIA_API_NOT_SUPPORTED:
        return ("MPE_ERROR_MEDIA_API_NOT_SUPPORTED");
        break;
    case MPE_ERROR_MEDIA_OS:
        return ("MPE_ERROR_MEDIA_OS");
        break;
    case MPE_ERROR_MEDIA_STREAM_OPEN:
        return ("MPE_ERROR_MEDIA_STREAM_OPEN");
        break;
    case MPE_ERROR_MEDIA_STREAM_READ:
        return ("MPE_ERROR_MEDIA_STREAM_READ");
        break;
    case MPE_ERROR_MEDIA_BUFFER_OVERRUN:
        return ("MPE_ERROR_MEDIA_BUFFER_OVERRUN");
        break;
    case MPE_ERROR_MEDIA_NO_IDS_AVAILABLE:
        return ("MPE_ERROR_MEDIA_NO_IDS_AVAILABLE");
        break;
    case MPE_ERROR_MEDIA_PRIORITY_LEVEL:
        return ("MPE_ERROR_MEDIA_PRIORITY_LEVEL");
        break;
    case MPE_ERROR_MEDIA_RESOURCE_NOT_ACTIVE:
        return ("MPE_ERROR_MEDIA_RESOURCE_NOT_ACTIVE");
        break;
    case MPE_ERROR_MEDIA_NOT_OWNER:
        return ("MPE_ERROR_MEDIA_NOT_OWNER");
        break;
    case MPE_ERROR_MEDIA_BAD_LINK:
        return ("MPE_ERROR_MEDIA_BAD_LINK");
        break;
    case MPE_ERROR_MEDIA_BLACKED_OUT:
        return ("MPE_ERROR_MEDIA_BLACKED_OUT");
        break;
    case MPE_ERROR_MEDIA_ECM_STREAM:
        return ("MPE_ERROR_MEDIA_ECM_STREAM");
        break;
#endif
    case MPE_SOCKET_EHOSTNOTFOUND:
        return ("MPE_SOCKET_EHOSTNOTFOUND");
        break;

    default:
        sprintf(msgbuf, "Unknown error : %d", (int) ec);
        return (msgbuf);
    }
}
