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

#ifndef __UI_INFO_H__
#define __UI_INFO_H__

#include <glib.h>
#include <glib-object.h>

G_BEGIN_DECLS

// Types used by GstDisplay Plugin
//
typedef struct _UIInfo UIInfo;

typedef struct _WindowInfo WindowInfo;

typedef struct _WindowOSInfo WindowOSInfo;

typedef struct _DisplayInfo DisplayInfo;

typedef struct _GraphicsInfo GraphicsInfo;

typedef struct _VideoInfo VideoInfo;

typedef struct _BackgroundInfo BackgroundInfo;

typedef struct _ImageBuffer ImageBuffer;

// Default screen / window dimensions
//
#define DEFAULT_SCREEN_WIDTH  640
#define DEFAULT_SCREEN_HEIGHT 480

/**
 * DisplayInfo:
 *
 * @depth: the color depth of Display @disp
 * @bpp: the number of bits per pixel on Display @disp
 * @endianness: the endianness of image bytes on Display @disp
 * @width: the width in pixels of Display @disp
 * @height: the height in pixels of Display @disp
 * @widthmm: the width in millimeters of Display @disp
 * @heightmm: the height in millimeters of Display @disp
 * @par: the pixel aspect ratio calculated from @width, @widthmm and @height,
 * @heightmm ratio
 * @caps: the #GstCaps that Display @disp can accept
 * @lastcaps: copy of last set of #GstCaps that Display @disp can accept
 *
 * Structure used to store various informations collected/calculated for a
 * Display.
 */
struct _DisplayInfo
{
    // Number of bits available for each pixel to represent colors
    gint depth;

    // Bits per pixel
    gint bpp;

    // Byte order of data
    gint endianness;

    // Entire physical display width & height in pixels
    gint width;
    gint height;

    // Entire physical display width & height in mm
    gint widthmm;
    gint heightmm;

    // Calculated pixel aspect ratio of physical output device / monitor
    // For computer monitors, it will always be 1/1
    GValue *par;
    gint par_n;
    gint par_d;
};

// Graphic screen surface internal representation
//
struct _GraphicsInfo
{
    // Width and height of graphics plane in pixels
    guint width;
    guint height;

    // Pixel Aspect Ratio for the graphics plane
    guint par_n;
    guint par_d;

    // Scale factors for the graphics plane are saved so that they do not
    // need to be calculated each time a video frame is displayed
    gfloat aspect_ratio_width_factor;
    gfloat aspect_ratio_height_factor;

    // Buffers for data, paint buffer is the current version which is
    // painted
    void* pixel_data[2];
    void* paint_pixel_data;

    gint32 bpl;
    guint bpp;
};

// Window general info which is not native to OS
//
struct _WindowInfo
{
    // Operating system specific screen information
    WindowOSInfo* pWindowOSInfo;

    // Indicates if window can not be moved
    gboolean is_fixed;

    // Indicates if window was supplied by application
    // *TODO* - do we need both of these?
    gboolean is_supplied;

    // Indicates if created internally or passed window
    gboolean is_created_internally;

    // Id of the window
    gulong win; // window id, same as hWnd

    // Height and width of window / screen in pixels
    gint32 width;
    gint32 height;
};

// Information related to presenting video
//
struct _VideoInfo
{
    // Size of incoming (unscaled) video in pixels
    guint incoming_width;
    guint incoming_height;

    // Pixel Aspect Ratio of incoming video
    GValue* incoming_par;
    gint incoming_par_n;
    gint incoming_par_d;
    float incoming_sar;

    // Size of desired video output
    guint output_width;
    guint output_height;

    // Pixel Aspect Ratio of video output
    gint output_par_n;
    gint output_par_d;
    float output_sar;

    // Factor used to scale video in display window
    gfloat aspect_ratio_width_factor;
    gfloat aspect_ratio_height_factor;

    // Flag which indicates plugin video frames are not to be shown
    // and display should be black
    gboolean is_blocked;

    // Flag which indicates plugin the last video frame is shown and
    // not to be updated
    gboolean is_frozen;

    // Video frame used when display is frozen
    guint freeze_size;
    guint freeze_width;
    guint freeze_height;
    gint freeze_format;
    guchar* freeze_buffer;

    // Video frame used when presentation is blocked
    guint block_screen_size;
    guint block_screen_width;
    guint block_screen_height;
    gint block_screen_format;
    guchar* block_screen_buffer;

    // Video scaling parameters
    gfloat scale_src_x;
    gfloat scale_src_y;
    gfloat scale_src_width;
    gfloat scale_src_height;

    gfloat scale_dest_x;
    gfloat scale_dest_y;
    gfloat scale_dest_width;
    gfloat scale_dest_height;

    // Buffer used to extract part of source video frame
    // used for clipping and dfc conversions
    guint clip_buffer_width;
    guint clip_buffer_height;
    gint clip_buffer_format;
    guint clip_buffer_size;
    guchar* pClipBuffer;
    guint clip_conversion_buffer_size;
    guchar* pClipConversionBuff;

    // Selected Decoder Conversion Format for video
    gboolean is_dfc_default;
    gint dfc;
    gint dfc_default;

    // Incoming video Active Format Descriptor
    gint afd;

    // For converting video yuv images to rgb
    guint conversion_buffer_size;
    guchar* pConversionBuff;

    guint threedtv_format_type;
    guint threedtv_payload_type;
    guint threedtv_payload_sz;
    guchar* threedtv_payload;

    gint scan_mode;
};

// Background device general info
//
struct _BackgroundInfo
{
    gboolean update_needed;
    gulong background_color;

    // Pixel aspect ratio of background
    guint par_n;
    guint par_d;

    gfloat aspect_ratio_width_factor;
    gfloat aspect_ratio_height_factor;

    // Width & height of background in pixels
    gint width;
    gint height;
};

struct _ImageBuffer
{
    // Pointer to the data for this image
    guchar* data;

    // Pointer to the data in GstBuffer for this image
    guint8* buffer_data;

    // True => image is YUV, false => image is RGB
    gboolean isI420YUV;

    // Width and height of the buffer
    gint buffer_width;
    gint buffer_height;

    // Size of data for this image
    size_t size;
};

struct _UIInfo
{
    // Information about the display
    DisplayInfo* pDisplayInfo;

    // General window information
    WindowInfo *pWindowInfo;

    // Graphics plane on the display
    GraphicsInfo *pGraphicsInfo;

    // General video information
    VideoInfo *pVideoInfo;

    // General background device information
    BackgroundInfo *pBackgroundInfo;

    ImageBuffer *pFillImageBuffer;
    ImageBuffer *pDisplayImageBuffer;

    // Ensures the window will not be destroyed while opengl methods are
    // accessing window and screen info
    GMutex *window_lock;

    // Provides single threaded access to image & conversion buffer to
    // prevent erroneous access and null pointer exceptions
    GMutex *flow_lock;

    gboolean hw_acceleration_disabled;

    gboolean take_snapshot;

    gboolean block_display;

    // Thread which handles events
    GThread *event_thread;
};

/**
 * Enumeration constants that represent the various types of decoder-format-conversions.
 * These values must be kept in sync with same structure defined in mpeos_disp.h on the ri stack side.
 */
typedef enum
{
    DFC_PROCESSING_NONE = 0,
    DFC_PROCESSING_FULL,
    DFC_PROCESSING_LB_16_9,
    DFC_PROCESSING_LB_14_9,
    DFC_PROCESSING_CCO,
    DFC_PROCESSING_PAN_SCAN,
    DFC_PROCESSING_LB_2_21_1_ON_4_3,
    DFC_PROCESSING_LB_2_21_1_ON_16_9,
    DFC_PLATFORM,
    DFC_PROCESSING_16_9_ZOOM,
    DFC_PROCESSING_PILLARBOX_4_3 = 100,
    DFC_PROCESSING_WIDE_4_3 = 101,
    DFC_PROCESSING_UNKNOWN = -1
} dfc_value;

G_END_DECLS

#endif /* __UI_INFO_H__ */
