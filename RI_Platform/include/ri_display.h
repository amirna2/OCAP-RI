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

#ifndef _RI_DISPLAY_H_
#define _RI_DISPLAY_H_

#include "ri_types.h"
#include "ri_video_device.h"

#define MAX_GRAPHICS_BUFFER_SZ	(1920 * 1080 * 4)

typedef struct ri_display_s ri_display_t;
typedef struct ri_display_data_s ri_display_data_t;

typedef enum
{
    ENV_STACK = 0, ENV_MFG = 1, MAX_ENV = 2,
} ri_env;

struct ri_display_s
{
    ri_video_device_t* (*get_video_device)(ri_display_t* object);

    void* (*get_graphics_buffer)(ri_display_t* object, ri_env env);

    void (*draw_graphics_buffer)(ri_display_t* object, ri_env env);

    void (*update_configuration)(ri_display_t* object, uint32_t graphicsWidth,
            uint32_t graphicsHeight, uint32_t graphicsPARx,
            uint32_t graphicsPARy, uint32_t videoWidth, uint32_t videoHeight,
            uint32_t videoPARx, uint32_t videoPARy, uint32_t backgroundWidth,
            uint32_t backgroundHeight, uint32_t backgroundPARx,
            uint32_t backgrondPARy, ri_env env);

    void (*set_dfc_mode)(ri_display_t* object, int32_t dfc);

    void (*set_dfc_default)(ri_display_t* object, int32_t dfc);

    void (*set_bg_color)(ri_display_t* object, uint32_t color);

    void (*block_presentation)(ri_display_t* object, ri_bool block);

    void (*freeze_video)(ri_display_t* object);

    void (*resume_video)(ri_display_t* object);

    void (*get_incoming_video_aspect_ratio)(ri_display_t* object, int32_t* ar);

    void (*get_incoming_video_size)(ri_display_t* object, int32_t* width,
            int32_t* height);

    void (*get_video_afd)(ri_display_t* object, uint32_t* afd);

    void (*set_bounds)(ri_display_t* object, ri_rect* src, ri_rect* dest);

    void (*get_bounds)(ri_display_t* object, ri_rect* src, ri_rect* dest);

    void (*check_bounds)(ri_display_t* object, ri_rect* desiredSrc,
            ri_rect* desiredDst, ri_rect* actualSrc, ri_rect* actualDst);

    void (*get_scaling)(ri_display_t* object, int32_t* positioning,
            float** horiz, float** vert, ri_bool* hRange, ri_bool* vRange,
            ri_bool* canClip, ri_bool* supportsComponent);

    int32_t (*get_threedtv_info)(ri_display_t* object, int32_t* formatType, int32_t* payloadType,
            uint32_t* payloadSz, uint8_t* payload, int32_t* scanMode);
			
    void (*block_display)(ri_display_t* object, ri_bool block);

    ri_display_data_t* data;
};

#endif /* _RI_DISPLAY_H_ */
