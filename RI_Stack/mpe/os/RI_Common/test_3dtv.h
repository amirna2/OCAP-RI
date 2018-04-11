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

#include "mpeos_event.h"

#define MPEOS_3DTV_TESTS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| 1 | set 2D\r\n" \
    "|---+-----------------------\r\n" \
    "| 2 | set 3D MPEG2 User Data/side by side (supported)\r\n" \
    "|---+-----------------------\r\n" \
    "| 3 | set 3D MPEG2 User Data/top and bottom (supported)\r\n" \
    "|---+-----------------------\r\n" \
    "| 4 | set 3D AVC/side by side (supported)\r\n" \
    "|---+-----------------------\r\n" \
    "| 5 | set 3D AVC/top and bottom (supported)\r\n" \
    "|---+-----------------------\r\n" \
    "| 6 | set 3D MPEG2 User Data/top and bottom (simulating 3D format unsupported) (uses option 2 format change)\r\n" \
    "|---+-----------------------\r\n" \
    "| 7 | set 3D MPEG2 User Data/top and bottom (simulating device not capable)\r\n" \
    "|---+-----------------------\r\n" \
    "| 8 | set 3D MPEG2 User Data/top and bottom (simulating no connected device)\r\n" \
    "|---+-----------------------\r\n" \
    "| n | Simulate no data\r\n" \
    "|---+-----------------------\r\n" \
    "| d | Simulate data (recovery from no data)\r\n" \
    "|---+-----------------------\r\n" \
    "| p | set 3D Payload        \r\n" \
    "|---+-----------------------\r\n" \
    "| s | Show 3D settings      \r\n" \
    "|---+-----------------------\r\n" \
    "| m | Set Scan Mode         \r\n" \
    "|---+-----------------------\r\n" \
    "| g | Get Scan Mode         \r\n" \

int test3DTVInputHandler(int sock, char* rxBuf, mpe_EventQueue queue, void* act,
        mpe_Media3DPayloadType* payloadType, mpe_DispStereoscopicMode* stereoscopicMode,
        uint8_t** payload, uint32_t* payloadSz, mpe_MediaScanMode* scanMode);
