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

#include "gstffmpeg.h"

static const gchar *
gst_ffmpeg_get_video_media_type(enum PixelFormat pixel_format)
{
    gchar* media_type = NULL;
    switch (pixel_format)
    {
    case PIX_FMT_YUV420P:
    {
        media_type = "video/x-raw-yuv";
        break;
    }
    default:
    {
        GST_ERROR("FFMPEG pixel format %s is not supported - aborting!",
                avcodec_get_pix_fmt_name(pixel_format));
        g_assert( FALSE);
    }
    }
    return media_type;
}

static const guint32 gst_ffmpeg_get_video_format(enum PixelFormat pixel_format)
{
    guint32 format = 0;
    switch (pixel_format)
    {
    case PIX_FMT_YUV420P:
    {
        format = GST_MAKE_FOURCC('I', '4', '2', '0');
        break;
    }
    default:
    {
        GST_ERROR("FFMPEG pixel format %s is not supported - aborting!",
                avcodec_get_pix_fmt_name(pixel_format));
        g_assert( FALSE);
    }
    }
    return format;
}

// frame_rate = 1 / time_base
// 1 / (a / b) = b / a
static const AVRational gst_ffmpeg_get_frame_rate(AVRational time_base)
{
    AVRational frame_rate =
    { 0, 0 };
    (void) av_reduce(&frame_rate.num, &frame_rate.den, (int64_t) time_base.den,
            (int64_t) time_base.num, INT_MAX);
    return frame_rate;
}

GstCaps *
gst_ffmpeg_create_fixed_video_caps(enum PixelFormat pixel_format, int width,
        int height, AVRational time_base, AVRational pixel_aspect_ratio)
{
    AVRational frame_rate = gst_ffmpeg_get_frame_rate(time_base);
    return gst_caps_new_simple(gst_ffmpeg_get_video_media_type(pixel_format),
            "endianness", G_TYPE_INT, G_BYTE_ORDER, "format", GST_TYPE_FOURCC,
            gst_ffmpeg_get_video_format(pixel_format), "width", G_TYPE_INT,
            width, "height", G_TYPE_INT, height, "framerate",
            GST_TYPE_FRACTION, frame_rate.num, frame_rate.den,
            "pixel-aspect-ratio", GST_TYPE_FRACTION, pixel_aspect_ratio.num,
            pixel_aspect_ratio.den, NULL);
}
