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

#include "ui_opengl_common.h"
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <GL/gl.h>
#include <GL/glext.h>
#include <ri_config.h>
#include <ri_log.h>

// Note:
// Since much of this code is called very frequently, there is very little in
// in the way of trace logging included.  This is to minimize the overhead
// incurred during trace-level logging.
// Logging category
#define RILOG_CATEGORY g_uiCat1
static log4c_category_t* g_uiCat1 = NULL;
static char* LOG_CAT = "RI.UI.OpenGL.common";

// Functions which have Win32 specific implementations
//
void opengl_swap_buffers(UIInfo* uiInfo);
void opengl_report_information(WindowOSInfo* windowOSInfo);
void opengl_set_pixel_format(gulong win, WindowOSInfo* windowOSInfo,
        gboolean hw_acceleration_disabled);

// Forward declarations for functions in this file
//
static void opengl_render_video(UIInfo* uiInfo);
static void opengl_render_graphics(UIInfo* uiInfo);
static int opengl_is_extension_supported(const char *extension);
static void opengl_yuv_to_rgb(guchar* buffer_data, guint buffer_width,
        guint buffer_height, guchar* dest);
static void opengl_update_clear_color(UIInfo* uiInfo);
static void opengl_video_src_pixels(UIInfo* uiInfo, gint* size,
        gint* buffer_width, gint* buffer_height, gint* format, guchar** buffer);
static void opengl_allocate_clip_buffer(UIInfo* uiInfo, guint width,
        guint height);
static void opengl_video_calc_offsets(UIInfo* uiInfo, gint buffer_width,
        gint buffer_height, float* x_offset, float* y_offset);
static void opengl_get_clipped_buffer_sz(UIInfo* uiInfo, guint *pwidth,
        guint *pheight);
static void opengl_video_clip(UIInfo* uiInfo);
static void opengl_dfc_conversion(UIInfo* uiInfo);
static void opengl_dfc_panscan(UIInfo* uiInfo);
static void opengl_dfc_wide(UIInfo* uiInfo);
static void opengl_dfc_zoom(UIInfo* uiInfo);
static void opengl_dfc_cco(UIInfo* uiInfo);
static void opengl_yuv_extract(UIInfo* uiInfo, int x_start, int x_end,
        int y_start, int y_end);
static void opengl_rgb_extract(UIInfo* uiInfo, int x_start, int x_end,
        int y_start, int y_end);

static void opengl_yuv_wide(UIInfo* uiInfo);
static void opengl_rgb_wide(UIInfo* uiInfo);
static void opengl_video_free_freeze_buffer(UIInfo* uiInfo);
static void opengl_video_free_block_buffer(UIInfo* uiInfo);
static gboolean is_video_clipped(UIInfo* uiInfo);
static gboolean is_video_scaled(UIInfo* uiInfo);
static void opengl_display_save(UIInfo* uiInfo);

extern char *dateString(char *date, int dateSize);

// Flag used to indicate if OpenGL BGRA extension is present on machine
//
static int hasBGRA = 0;

static char *snapshotPath = NULL;

// Declares the number of bytes per pixel for RGB color formats
// used by the yuv->rgb conversion routines
//
#define RGB_BYTES_PER_PIXEL 3

#ifdef WIN32

struct Stats
{
    long calls;
    long shortest;
    long longest;
    long frequency;
    SYSTEMTIME last;
    SYSTEMTIME start;
    SYSTEMTIME end;
}stats;

long difftm(SYSTEMTIME *start, SYSTEMTIME *end)
{
    long delta = 0;

    if (end->wMinute < start->wMinute)
    end->wMinute = start->wMinute + (60 - start->wMinute);

    delta += (end->wMinute - start->wMinute) * 60;
    delta += (end->wSecond - start->wSecond);
    return((delta*1000) + (end->wMilliseconds - start->wMilliseconds));
}

#else

struct Stats
{
    long calls;
    long shortest;
    long longest;
    long frequency;
    struct timeval last;
    struct timeval start;
    struct timeval end;
} stats;

long difftm(struct timeval *start, struct timeval *end)
{
    long delta = 0;

    if (end->tv_sec < start->tv_sec)
        end->tv_sec = start->tv_sec + (60 - start->tv_sec);

    delta = (end->tv_sec - start->tv_sec);
    return ((delta * 1000) + ((end->tv_usec - start->tv_usec) / 1000));
}

#endif

void callStats(gboolean start)
{
    if (start)
    {
#ifdef WIN32
        GetLocalTime(&stats.start);
#else
        gettimeofday(&stats.start, NULL);
#endif
    }
    else
    {
        long delta = 0;
#ifdef WIN32
        GetLocalTime(&stats.end);
#else
        gettimeofday(&stats.end, NULL);
#endif
        stats.frequency = (stats.frequency + difftm(&stats.last, &stats.start))
                / 2;
        memcpy(&stats.last, &stats.start, sizeof(stats.last));
        delta = difftm(&stats.start, &stats.end);

        if (delta > 0)
        {
            if ((delta < stats.shortest) || (stats.shortest == 0))
                stats.shortest = delta;

            if ((delta > stats.longest) || (stats.longest == 0))
                stats.longest = delta;
        }

        if ((++stats.calls % 1000) == 0)
        {
            RILOG_DEBUG("%s -- %ld/%ld/%ld/%ld calls/short/long/freq\n",
                    __FUNCTION__, stats.calls, stats.shortest, stats.longest,
                    stats.frequency);
        }
    }
}

/**
 * Make the calls to display the current video and graphic image buffers
 * to the display.
 *
 * @param uiInfo    display information
 */
void opengl_render_display(UIInfo* uiInfo)
{
    callStats(1);

    if (uiInfo->pBackgroundInfo->update_needed)
    {
        opengl_update_clear_color(uiInfo);
    }

    // Reset the back buffer
    glClear( GL_COLOR_BUFFER_BIT);

    // Acquire the flow lock to ensure single threaded access to image buffers
    g_mutex_lock(uiInfo->flow_lock);

    if (!uiInfo->block_display)
    {
        // Draw the video frame
        opengl_render_video(uiInfo);

        // Draws overlay graphics
        opengl_render_graphics(uiInfo);
    }

    if (uiInfo->take_snapshot)
    {
        opengl_display_save(uiInfo);
        uiInfo->take_snapshot = FALSE;
    }

    // Swap the back buffer with the front buffer
    opengl_swap_buffers(uiInfo);

    // Release the flow lock
    g_mutex_unlock(uiInfo->flow_lock);

    callStats(0);
}

/**
 * Make the OpenGL calls to display the current video image buffer
 * to the display.
 *
 * @param uiInfo    display information
 */
static void opengl_render_video(UIInfo* uiInfo)
{
    if (NULL == g_uiCat1)
        g_uiCat1 = log4c_category_get(LOG_CAT);

    if (NULL == uiInfo->pDisplayImageBuffer)
    {
        // No video image to render, just return
        return;
    }

    // When scaling has been defined, this takes precedence over DFC conversions
    // (according to Interactive TV Standards book, pg. 281)
    if ((TRUE == is_video_clipped(uiInfo)) || (TRUE == is_video_scaled(uiInfo)))
    {
        if (TRUE == is_video_clipped(uiInfo))
        {
            // Put pixels in the clip buffer to be displayed based on scaling factors
            opengl_video_clip(uiInfo);
        }
    }
    else
    {
        // Make any adjustments for DFC
        opengl_dfc_conversion(uiInfo);
    }

    // Set the default parameters for pixel buffer draw
    gint size = uiInfo->pDisplayImageBuffer->size;
    gint buffer_width = uiInfo->pDisplayImageBuffer->buffer_width;
    gint buffer_height = uiInfo->pDisplayImageBuffer->buffer_height;
    gint format = GL_RGBA;
    guchar* buffer = uiInfo->pDisplayImageBuffer->buffer_data;

    // Create the pixel buffer used as video source rectangle
    opengl_video_src_pixels(uiInfo, &size, &buffer_width, &buffer_height,
            &format, &buffer);

    //RILOG_DEBUG("%s -- buffer width %d, height %d\n", __FUNCTION__, buffer_width, buffer_height);

    // Establish the initial video drawing offsets
    float offset_x = 0;
    float offset_y = 0;
    opengl_video_calc_offsets(uiInfo, buffer_width, buffer_height, &offset_x,
            &offset_y);

    // Position video image on display
    //RILOG_DEBUG("%s -- Calc raster pos using - scale x: %f, window width: %d, scale w: %f, offset x: %f\n",
    //      __FUNCTION__, uiInfo->pVideoInfo->scale_x, uiInfo->pWindowInfo->width,
    //      uiInfo->pVideoInfo->scale_width, offset_x);

    // The offsets have 2 parts:
    //    1) offset_x/offset_y: the centering of the video in the PC TV Screen
    //    2) the difference betwen the x/y locations of the src and dest rectangles -- these are set
    //       by the calling xlet
    glRasterPos2f(
        offset_x + 
        (uiInfo->pVideoInfo->scale_dest_x - uiInfo->pVideoInfo->scale_src_x) * 
        (uiInfo->pVideoInfo->aspect_ratio_width_factor * uiInfo->pDisplayImageBuffer->buffer_width),
        offset_y + 
        (uiInfo->pVideoInfo->scale_dest_y - uiInfo->pVideoInfo->scale_src_y) * 
        (uiInfo->pVideoInfo->aspect_ratio_height_factor * uiInfo->pDisplayImageBuffer->buffer_height));

    // Video is arranged in top-to-bottom orientation, whereas openGL works in bottom-to-top orientation.
    // The negative value for the vertical scaling tells OpenGL to vertically flip the video image.
    //RILOG_DEBUG("%s -- pixel zoom video aspect ratio width %f, height %f\n",
    //      __FUNCTION__, uiInfo->pVideoInfo->aspect_ratio_width_factor,
    //      uiInfo->pVideoInfo->aspect_ratio_height_factor);
    // Scale by pVideoInfo->aspect_ratio_xxxx_factor: this represents the TV Screen to PC Window mapping
    // Also scale by the ratio of the dest to src rectangles: this represents additional scaling imposed by xlet
    glPixelZoom(uiInfo->pVideoInfo->scale_dest_width / uiInfo->pVideoInfo->scale_src_width
            * uiInfo->pVideoInfo->aspect_ratio_width_factor,
            -uiInfo->pVideoInfo->scale_dest_height / uiInfo->pVideoInfo->scale_src_height
                    * uiInfo->pVideoInfo->aspect_ratio_height_factor);

    glDrawPixels(buffer_width, buffer_height, format, GL_UNSIGNED_BYTE, buffer);

    // Free freeze buffer if no longer frozen
    opengl_video_free_freeze_buffer(uiInfo);

    // Free black screen buffer if no longer frozen
    opengl_video_free_block_buffer(uiInfo);
}

/**
 * Sets the supplied pointers to the correct values depending
 * on where the pixels are coming from.  It handles all the
 * various cases including dfc, clipping, freeze, or blocked.
 *
 * @param   uiInfo         display information
 * @param   size           size of the buffer in pixels
 * @param   buffer_width   number of pixels in image width
 * @param   buffer_height  number of pixels in image height
 * @param   format         either YUV or RGB
 * @param   buffer         pointer to buffer to use as source of image
 */
static void opengl_video_src_pixels(UIInfo* uiInfo, gint* size,
        gint* buffer_width, gint* buffer_height, gint* format, guchar** buffer)
{
    // Check for a freeze frame to display
    if ((uiInfo->pVideoInfo->is_frozen) && (NULL
            != uiInfo->pVideoInfo->freeze_buffer))
    {
        // Make sure rendering from freeze frame
        *size = uiInfo->pVideoInfo->freeze_size;
        *buffer_width = uiInfo->pVideoInfo->freeze_width;
        *buffer_height = uiInfo->pVideoInfo->freeze_height;
        *format = uiInfo->pVideoInfo->freeze_format;
        *buffer = uiInfo->pVideoInfo->freeze_buffer;
    }
    // Check for blocked screen to display
    else if ((uiInfo->pVideoInfo->is_blocked) && (NULL
            != uiInfo->pVideoInfo->block_screen_buffer))
    {
        // Make sure rendering from freeze frame
        *size = uiInfo->pVideoInfo->block_screen_size;
        *buffer_width = uiInfo->pVideoInfo->block_screen_width;
        *buffer_height = uiInfo->pVideoInfo->block_screen_height;
        *format = uiInfo->pVideoInfo->block_screen_format;
        *buffer = uiInfo->pVideoInfo->block_screen_buffer;
    }
    // Check for dfc or clipped video to display
    else if (NULL != uiInfo->pVideoInfo->pClipBuffer)
    {
        *buffer_width = uiInfo->pVideoInfo->clip_buffer_width;
        *buffer_height = uiInfo->pVideoInfo->clip_buffer_height;

        if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
        {
            // ...convert it to RGB and...
            opengl_yuv_to_rgb(uiInfo->pVideoInfo->pClipBuffer,
                    uiInfo->pVideoInfo->clip_buffer_width,
                    uiInfo->pVideoInfo->clip_buffer_height,
                    uiInfo->pVideoInfo->pClipConversionBuff);

            *format = GL_RGB;
            *size = uiInfo->pVideoInfo->clip_conversion_buffer_size;
            *buffer = uiInfo->pVideoInfo->pClipConversionBuff;
        }
        else
        {
            *size = uiInfo->pVideoInfo->clip_buffer_size;
            *buffer = uiInfo->pVideoInfo->pClipBuffer;
        }
    }
    // Display incoming frame unaltered...
    else if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
    {

        guint expected_buffer_size = uiInfo->pDisplayImageBuffer->buffer_width
                * uiInfo->pDisplayImageBuffer->buffer_height
                * RGB_BYTES_PER_PIXEL;

        if (uiInfo->pDisplayImageBuffer->buffer_width > 0
                && uiInfo->pDisplayImageBuffer->buffer_height > 0)
        {
            if (uiInfo->pVideoInfo->pConversionBuff != NULL
                    && uiInfo->pVideoInfo->conversion_buffer_size
                            != expected_buffer_size)
            {
                RILOG_DEBUG("Freeing pConversionBuff of size %u\n",
                        uiInfo->pVideoInfo->conversion_buffer_size);
                g_free(uiInfo->pVideoInfo->pConversionBuff);
                uiInfo->pVideoInfo->pConversionBuff = NULL;
            }

            if (uiInfo->pVideoInfo->pConversionBuff == NULL)
            {
                uiInfo->pVideoInfo->pConversionBuff = g_try_malloc0(
                        expected_buffer_size);

                if (NULL == uiInfo->pVideoInfo->pConversionBuff)
                {
                    RILOG_FATAL(-1,
                            "line %d of %s, %s memory allocation failure!\n",
                            __LINE__, __FILE__, __func__);
                }

                uiInfo->pVideoInfo->conversion_buffer_size
                        = expected_buffer_size;
                RILOG_DEBUG("Allocated pConversionBuff of size %u\n",
                        uiInfo->pVideoInfo->conversion_buffer_size);
            }
        }

        // ...convert it to RGB and...
        opengl_yuv_to_rgb(uiInfo->pDisplayImageBuffer->buffer_data,
                uiInfo->pDisplayImageBuffer->buffer_width,
                uiInfo->pDisplayImageBuffer->buffer_height,
                uiInfo->pVideoInfo->pConversionBuff);

        *format = GL_RGB;
        *size = uiInfo->pVideoInfo->conversion_buffer_size;
        *buffer = uiInfo->pVideoInfo->pConversionBuff;
    }

    // Capture last frame if video is frozen and has not yet been captured
    if ((uiInfo->pVideoInfo->is_frozen) && (NULL
            == uiInfo->pVideoInfo->freeze_buffer))
    {
        // If frozen and buffer not yet set, save current buffer as freeze frame
        uiInfo->pVideoInfo->freeze_size = *size;
        uiInfo->pVideoInfo->freeze_width = *buffer_width;
        uiInfo->pVideoInfo->freeze_height = *buffer_height;
        uiInfo->pVideoInfo->freeze_format = *format;
        uiInfo->pVideoInfo->freeze_buffer = g_try_malloc0(*size);

        if (NULL == uiInfo->pVideoInfo->freeze_buffer)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        memcpy(uiInfo->pVideoInfo->freeze_buffer, *buffer, *size);
    }

    // If presentation is blocked, create display black screen to display
    // if not already created
    if ((uiInfo->pVideoInfo->is_blocked) && (NULL
            == uiInfo->pVideoInfo->block_screen_buffer))
    {
        uiInfo->pVideoInfo->block_screen_size = *size;
        uiInfo->pVideoInfo->block_screen_width = *buffer_width;
        uiInfo->pVideoInfo->block_screen_height = *buffer_height;
        uiInfo->pVideoInfo->block_screen_format = *format;
        uiInfo->pVideoInfo->block_screen_buffer = g_try_malloc0(*size);

        if (NULL == uiInfo->pVideoInfo->block_screen_buffer)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        memset(uiInfo->pVideoInfo->block_screen_buffer, 0, *size);
    }
}

/**
 * Clip the incoming video image in the manner defined in the source
 * scaling parameters.  It's like taking a scissors and cutting out
 * a rectangle.
 *
 * @param   uiInfo   display information
 */
static void opengl_video_clip(UIInfo* uiInfo)
{
    if (NULL == g_uiCat1)
        g_uiCat1 = log4c_category_get(LOG_CAT);

    // Determine the desired height and width
    guint width = (guint)(uiInfo->pVideoInfo->incoming_width
            * uiInfo->pVideoInfo->scale_src_width);
    guint height = (guint)(uiInfo->pVideoInfo->incoming_height
            * uiInfo->pVideoInfo->scale_src_height);

    opengl_allocate_clip_buffer(uiInfo, width, height);

    // Transfer the pixels in image buffer into scale conversion buffer
    // using the scale conversion factors
    int x_start = (int) (uiInfo->pDisplayImageBuffer->buffer_width
            * uiInfo->pVideoInfo->scale_src_x);

    int y_start = (int) (uiInfo->pDisplayImageBuffer->buffer_height
            * uiInfo->pVideoInfo->scale_src_y);

    if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
    {
        opengl_yuv_extract(uiInfo, x_start, x_start
                + uiInfo->pVideoInfo->clip_buffer_width, // x end
                y_start, y_start + uiInfo->pVideoInfo->clip_buffer_height); // y end
    }
    else
    {
        opengl_rgb_extract(uiInfo, x_start, x_start
                + uiInfo->pVideoInfo->clip_buffer_width, // x end
                y_start, y_start + uiInfo->pVideoInfo->clip_buffer_height); // y end
    }
}

/**
 * Determine the offsets used by opengl to position output.
 *
 * @param   uiInfo         display information
 * @param   buffer_width   size in pixel of the width of the image to display
 * @param   buffer_height  size in pixel of the height of the image to display
 * @param   x_offset       x position where image display should start
 * @param   y_offset       y position where image display should start
 */
static void opengl_video_calc_offsets(UIInfo* uiInfo, gint buffer_width,
        gint buffer_height, float* x_offset, float* y_offset)
{
    if (is_video_clipped(uiInfo))
    {
        // Use the incoming buffer width & height to calculate offset,
        // rather than clipped buffer size
        buffer_width = uiInfo->pDisplayImageBuffer->buffer_width;
        buffer_height = uiInfo->pDisplayImageBuffer->buffer_height;
    }

    if (uiInfo->pWindowInfo->width
            > (uiInfo->pVideoInfo->aspect_ratio_width_factor * buffer_width))
    {
        *x_offset
                = (uiInfo->pWindowInfo->width
                        - (uiInfo->pVideoInfo->aspect_ratio_width_factor
                                * buffer_width)) / 2;
    }

    if (uiInfo->pWindowInfo->height
            > (uiInfo->pVideoInfo->aspect_ratio_height_factor * buffer_height))
    {
        *y_offset = (uiInfo->pWindowInfo->height
                - (uiInfo->pVideoInfo->aspect_ratio_height_factor
                        * buffer_height)) / 2;
    }
}

/**
 * Make the OpenGL calls to display the current graphic buffer.
 *
 * @param uiInfo    display information
 */
static void opengl_render_graphics(UIInfo* uiInfo)
{
    if (NULL == g_uiCat1)
        g_uiCat1 = log4c_category_get(LOG_CAT);

    float offset_x = 0;
    float offset_y = 0;

    if ((NULL != uiInfo->pGraphicsInfo) && (NULL
            != uiInfo->pGraphicsInfo->paint_pixel_data))
    {
        // Establish the offsets so graphics plane will be centered within window
        if (uiInfo->pWindowInfo->width
                > (uiInfo->pGraphicsInfo->aspect_ratio_width_factor
                        * uiInfo->pGraphicsInfo->width))
        {
            offset_x = (uiInfo->pWindowInfo->width
                    - (uiInfo->pGraphicsInfo->aspect_ratio_width_factor
                            * uiInfo->pGraphicsInfo->width)) / 2;
        }

        if (uiInfo->pWindowInfo->height
                > (uiInfo->pGraphicsInfo->aspect_ratio_height_factor
                        * uiInfo->pGraphicsInfo->height))
        {
            offset_y = (uiInfo->pWindowInfo->height
                    - (uiInfo->pGraphicsInfo->aspect_ratio_height_factor
                            * uiInfo->pGraphicsInfo->height)) / 2;
        }

        // Adjust the raster position so the graphics plane is centered on screen
        glRasterPos2f(offset_x, offset_y);

        glPixelZoom(uiInfo->pGraphicsInfo->aspect_ratio_width_factor,
                -uiInfo->pGraphicsInfo->aspect_ratio_height_factor);

#ifdef GL_EXT_bgra
        if (hasBGRA)
        {
            //#ifdef __WXMSW__  /* not working in Linux at the moment */
            glDrawPixels(uiInfo->pGraphicsInfo->width,
                    uiInfo->pGraphicsInfo->height,
                    GL_BGRA_EXT,
                    GL_UNSIGNED_BYTE,
                    uiInfo->pGraphicsInfo->paint_pixel_data);
            //#endif
        }
        else
#endif
        {
            // If is BGRA extension is not represent, the color will not show up correctly
            // i.e. blues will look like reds
            RILOG_WARN(
                    "%s -- Needs EXT_bgra extension to handle BGRA to RBGA Conversion!\n",
                    __FUNCTION__);

            glDrawPixels(uiInfo->pGraphicsInfo->width,
                    uiInfo->pGraphicsInfo->height, GL_RGBA, GL_UNSIGNED_BYTE,
                    uiInfo->pGraphicsInfo->paint_pixel_data);
        }
    }
}

/**
 * Initializes a pre-existing system window as a target for OpenGL drawing.
 *
 * It is assumed that the window has already been created and associated with
 * an OpenGL context.  This function prepares the window for use by setting
 * the viewport, projection, and matrix mode to be used for OpenGL operations.
 *
 * @param uiInfo    display information
 */
void opengl_init_environment(UIInfo* uiInfo, gulong win)
{
    if (NULL == g_uiCat1)
        g_uiCat1 = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- ENTRY win %ld, width %d, height %d\n", __FUNCTION__,
            win, uiInfo->pWindowInfo->width, uiInfo->pWindowInfo->height);

    opengl_set_pixel_format(win, uiInfo->pWindowInfo->pWindowOSInfo,
            uiInfo->hw_acceleration_disabled);

    RILOG_INFO("%s --  uiInfo->hw_acceleration_disabled:%d\n", __FUNCTION__,
            uiInfo->hw_acceleration_disabled);
    hasBGRA = opengl_is_extension_supported("GL_EXT_bgra");

    // Set up the data to be used by OpenGL
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

    // Get the config value to see if pixel byte swapping has been requested
    // Default is false, set to true to compensate for issues with some hw graphics drivers
    if (ricfg_getBoolValue("RIPlatform",
                           "RI.Platform.opengl.swap_bytes_in_pixel_store"))
    {
        glPixelStorei(GL_UNPACK_SWAP_BYTES, 1);
    }

    // Initialize the transparency blending
    glEnable( GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // Sets up the OpenGL viewport
    glViewport(0, // x
            0, // y
            uiInfo->pWindowInfo->width, // width
            uiInfo->pWindowInfo->height); // height

    // Specifies the current matrix is the projection matrix
    glMatrixMode( GL_PROJECTION);

    // Replaces current matrix with the project matrix
    glLoadIdentity();

    // Sets up the clipping region
    glOrtho(0.0, // left
            uiInfo->pWindowInfo->width, // right
            uiInfo->pWindowInfo->height, // bottom
            0.0, // top
            -1.0, // near
            1.0); // far

    // Set current matrix back to Modelview
    glMatrixMode( GL_MODELVIEW);
    glLoadIdentity();

    // Disable 3D support
    glDisable( GL_DEPTH_TEST);
    glDepthMask( GL_FALSE);

    // Set the clear color
    // the background color is specified as an RGBA8888 value, so we need to
    //  extract the individual color components to establish the background color
    float red = ((uiInfo->pBackgroundInfo->background_color & 0x00FF000000)
            >> 24) / (float) 0xFF;
    float green = ((uiInfo->pBackgroundInfo->background_color & 0x0000FF0000)
            >> 16) / (float) 0xFF;
    float blue = ((uiInfo->pBackgroundInfo->background_color & 0x000000FF00)
            >> 8) / (float) 0xFF;
    float alpha = (uiInfo->pBackgroundInfo->background_color & 0x00000000FF)
            / (float) 0xFF;

    glClearColor(red, green, blue, alpha);

    // Report the OpenGL settings
    if (NULL != uiInfo->pWindowInfo->pWindowOSInfo)
    {
        opengl_report_information(uiInfo->pWindowInfo->pWindowOSInfo);
    }

    uiInfo->take_snapshot = FALSE;
    snapshotPath = ricfg_getValue("RIPlatform", "RI.Platform.SnapshotDir");

    if (NULL == snapshotPath)
    {
        snapshotPath = "";
        RILOG_WARN("%s -- RI Platform snapshot path not specified!\n",
                __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Updates the OpenGL clear color to the value that is currently set in the
 * display instance.
 *
 * @param   uiInfo   display information
 */
static void opengl_update_clear_color(UIInfo* uiInfo)
{
    if (NULL == g_uiCat1)
        g_uiCat1 = log4c_category_get(LOG_CAT);
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // the background color is specified as an RGBA8888 value, so we need to
    //  extract the individual color components to establish the background color
    float red = ((uiInfo->pBackgroundInfo->background_color & 0x00FF000000)
            >> 24) / (float) 0xFF;
    float green = ((uiInfo->pBackgroundInfo->background_color & 0x0000FF0000)
            >> 16) / (float) 0xFF;
    float blue = ((uiInfo->pBackgroundInfo->background_color & 0x000000FF00)
            >> 8) / (float) 0xFF;
    float alpha = (uiInfo->pBackgroundInfo->background_color & 0x00000000FF)
            / (float) 0xFF;

    glClearColor(red, green, blue, alpha);

    // Clear the flag since it has been updated
    uiInfo->pBackgroundInfo->update_needed = FALSE;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Utility function to convert the image from YUV to RGB format
 *
 * @param buffer_data   contains the original YUV image to be converted.
 * @param buffer_width  size in pixels of the width of the image
 * @param buffer_height size in pixels of the height of the image
 * @param dest          points to pre-allocated memory of the right size
 *                      i.e. (height * width * 3(RGB))
 */
static void opengl_yuv_to_rgb(guchar* buffer_data, guint buffer_width,
        guint buffer_height, guchar* dest)
{
    // pitch of a single row in the RGB buffer
    int rgbPitch = RGB_BYTES_PER_PIXEL * buffer_width;

    // current x, y offsets
    register int x, y;

    // x, y indexes into U & V buffers
    int x_prime, y_prime;

    // temporary variables for Y, U, and V pixel portions
    int Y, U, V;

    // base offsets to U and V image buffers
    int uBufferBase, vBufferBase;

    // index offset to current pixel in U and V image buffers
    int uvPixelIndex;

    uBufferBase = buffer_width * buffer_height;
    vBufferBase = (int) ((buffer_width * buffer_height) * 1.25);

    // pitch of a single row in the U and V buffers
    int uvPitch = buffer_width / 2;

    guchar* pRGB;

    // YUV I4:2:0 -> RGB conversion algorithm
    // Note that this algorithm is commonly available on the internet
    // Strategy:
    // Working through each pixel of the image, collect the Y, U, and V
    // elements.  Once all of the elements are extracted from the source image,
    // perform the YUV->RGB conversion into a pre-allocated buffer.
    //
    // Things to note:
    // For each pixel, there is an 8-bit Y value.  These values are packed so that
    // they are contiguous in memory.
    // Following the Y values in the source buffer are the U values.
    // There is one U value for every 4 I values arranged in a 2x2 square.
    // Following the U values in the source buffer are the V values.
    // There is one V value for every 4 I values arranged in a 2x2 square.
    //
    // In the following, the x and y values iterate through each pixel offset in the image.
    // These correspond 1:1 in the buffer of I values.
    // The x_prime and y_prime values are calculated to iterate through the corresponding pixel
    // offsets in the U and V buffers.  Since each U and V entry represents 4 pixels in the
    // actual image, these are each divided by two so that they line up correctly.

    for (x = 0; x < (int) buffer_width; x++)
    {
        // divide the x offset by two for use in the U and V buffers
        x_prime = x >> 1;

        for (y = 0; y < (int) buffer_height; y++)
        {
            // divide the y offset by two for use in the U and V buffers
            y_prime = y >> 1;

            uvPixelIndex = (y_prime * uvPitch) + x_prime;

            // get Y value (intensity component) from video frame
            Y = buffer_data[(y * buffer_width) + x];

            // get U value (blue component) from video frame
            U = buffer_data[uBufferBase + uvPixelIndex];

            // get V value (red component) from video frame
            V = buffer_data[vBufferBase + uvPixelIndex];

            // calculate pixel offset into destination RGB array
            pRGB = &dest[(x * RGB_BYTES_PER_PIXEL) + (y * rgbPitch)];

            //            //
            //            // option 1: floating point option
            //            //
            //            // write Red value
            //            *pRGB++ = CLAMP((1.164 * (Y - 16) + 1.596 * (V - 128)), 0, 255);
            //
            //            // write Green value
            //            *pRGB++ = CLAMP((1.164 * (Y - 16) - 0.813 * (V - 128) - 0.391 * (U - 128)) , 0, 255);
            //
            //            // write Blue value
            //            *pRGB = CLAMP((1.164 * (Y - 16) + 2.018 * (U - 128)), 0, 255);

            //
            // option 2: integer option
            //
            register int temp = 298 * (Y - 16);

            // write Red value
            *pRGB++ = CLAMP((temp + 409 * (V - 128) + 128) / 256, 0, 255);

            // write Green value
            *pRGB++ = CLAMP((temp - 100 * (U - 128) - 208 * (V - 128) + 128)
                    / 256, 0, 255);

            // write Blue value
            *pRGB = CLAMP((temp + 516 * (U - 128) + 128) / 256, 0, 255);
        }
    }
}

/**
 * Determines if the supplied OpenGL is supported on this platform
 *
 * @param   extension  name of extension to check if supported
 */
static int opengl_is_extension_supported(const char *extension)
{
    const GLubyte *extensions = NULL;
    const GLubyte *start;
    GLubyte *where, *terminator;

    RILOG_TRACE("%s -- Entry, %s\n", __FUNCTION__, extension);

    // Extension names should not have spaces.
    where = (GLubyte *) strchr(extension, ' ');
    if (where || *extension == '\0')
    {
        return 0;
    }
    extensions = glGetString(GL_EXTENSIONS);

    // It takes a bit of care to be fool-proof about parsing the
    // OpenGL extensions string. Don't be fooled by sub-strings, etc.
    if (extensions != NULL)
    {
        start = extensions;
        for (;;)
        {
            where = (GLubyte *) strstr((const char *) start, extension);
            if (!where)
            {
                break;
            }
            terminator = where + strlen(extension);
            if (where == start || *(where - 1) == ' ')
            {
                if (*terminator == ' ' || *terminator == '\0')
                {
                    RILOG_TRACE("%s -- Exit, returning 1\n", __FUNCTION__);
                    return 1;
                }
            }
            start = terminator;
        }
    }

    RILOG_TRACE("%s -- Exit, returning 0\n", __FUNCTION__);
    return 0;
}

/**
 * Determines what the current Decoder Format Conversion is
 * and calls the necessary methods to perform actions necessary
 * to setup the buffer using the source image.
 *
 * @param   uiInfo   display information
 */
static void opengl_dfc_conversion(UIInfo* uiInfo)
{
    if (NULL == g_uiCat1)
        g_uiCat1 = log4c_category_get(LOG_CAT);

    // Adjust for DFC where input buffer is modified
    switch (uiInfo->pVideoInfo->dfc)
    {
    // Use Center Cut Off function for conversion
    case (DFC_PROCESSING_CCO):
        if (is_16_9_to_4_3(uiInfo))
        {
            opengl_dfc_cco(uiInfo);
        }
        break;

        // Use Pan Scan function for conversion
    case (DFC_PROCESSING_PAN_SCAN):
        if (is_16_9_to_4_3(uiInfo))
        {
            opengl_dfc_panscan(uiInfo);
        }
        break;

        // Use Zoom function for conversion
    case (DFC_PROCESSING_16_9_ZOOM):
        if (is_4_3_to_16_9(uiInfo))
        {
            opengl_dfc_zoom(uiInfo);
        }
        break;

        // Use Wide function for conversion
    case (DFC_PROCESSING_WIDE_4_3):
        if (is_4_3_to_16_9(uiInfo))
        {
            opengl_dfc_wide(uiInfo);
        }
        break;

        // These format conversions are done through adjusted scale factors
    case (DFC_PROCESSING_FULL):
    case (DFC_PROCESSING_LB_16_9):
    case (DFC_PROCESSING_LB_14_9):
    case (DFC_PROCESSING_LB_2_21_1_ON_4_3):
    case (DFC_PROCESSING_LB_2_21_1_ON_16_9):
    case (DFC_PROCESSING_PILLARBOX_4_3):
        // DFC adjustment was already made to scale factor
        break;

    case (DFC_PROCESSING_NONE):
    case (DFC_PLATFORM):
    case (DFC_PROCESSING_UNKNOWN):
        // DFC set to none, unknown or platform");
        break;

    default:
        RILOG_ERROR("%s -- Unrecognized DFC value: %d\n", __FUNCTION__,
                uiInfo->pVideoInfo->dfc);
    }
}

/**
 * Utility method which determines if the incoming video
 * aspect ratio is 14:9 and the desired video output aspect
 * ratio is 4:3
 *
 * @param   display information
 * @return  true if incoming is 16:9 and output is 4:3,
 *          false otherwise
 */
gboolean is_14_9_to_4_3(UIInfo* uiInfo)
{
    gboolean tis_14_9_to_4_3 = FALSE;
    if (((float) 14 / (float) 9 == uiInfo->pVideoInfo->incoming_sar)
            && ((float) 4 / (float) 3 == uiInfo->pVideoInfo->output_sar))
    {
        tis_14_9_to_4_3 = TRUE;
    }
    return tis_14_9_to_4_3;
}

/**
 * Utility method which determines if the incoming video
 * aspect ratio is 2.21:1 and the desired video output aspect
 * ratio is 16:9
 *
 * @param   display information
 * @return  true if incoming is 2.21:1 and output is 16:9,
 *          false otherwise
 */
gboolean is_221_100_to_16_9(UIInfo* uiInfo)
{
    gboolean tis_221_100_to_16_9 = FALSE;
    if (((float) 221 / (float) 100 == uiInfo->pVideoInfo->incoming_sar)
            && ((float) 16 / (float) 9 == uiInfo->pVideoInfo->output_sar))
    {
        tis_221_100_to_16_9 = TRUE;
    }
    return tis_221_100_to_16_9;
}

/**
 * Utility method which determines if the incoming video
 * aspect ratio is 2.21:1 and the desired video output aspect
 * ratio is 4:3
 *
 * @param   display information
 * @return  true if incoming is 2.21:1 and output is 4:3,
 *          false otherwise
 */
gboolean is_221_100_to_4_3(UIInfo* uiInfo)
{
    gboolean tis_221_100_to_4_3 = FALSE;
    if (((float) 221 / (float) 100 == uiInfo->pVideoInfo->incoming_sar)
            && ((float) 4 / (float) 3 == uiInfo->pVideoInfo->output_sar))
    {
        tis_221_100_to_4_3 = TRUE;
    }
    return tis_221_100_to_4_3;
}

/**
 * Utility method which determines if the incoming video
 * aspect ratio is 4:3 and the desired video output aspect
 * ratio is 16:9
 *
 * @param   display information
 * @return  true if incoming is 4:3 and output is 16:9,
 *          false otherwise
 */
gboolean is_4_3_to_16_9(UIInfo* uiInfo)
{
    gboolean tis_4_3_to_16_9 = FALSE;
    if (((float) 4 / (float) 3 == uiInfo->pVideoInfo->incoming_sar)
            && ((float) 16 / (float) 9 == uiInfo->pVideoInfo->output_sar))
    {
        tis_4_3_to_16_9 = TRUE;
    }
    return tis_4_3_to_16_9;
}

/**
 * Utility method which determines if the incoming video
 * aspect ratio is 16:9 and the desired video output aspect
 * ratio is 4:3
 *
 * @param   display information
 * @return  true if incoming is 16:9 and output is 4:3,
 *          false otherwise
 */
gboolean is_16_9_to_4_3(UIInfo* uiInfo)
{
    gboolean tis_16_9_to_4_3 = FALSE;
    if (((float) 16 / (float) 9 == uiInfo->pVideoInfo->incoming_sar)
            && ((float) 4 / (float) 3 == uiInfo->pVideoInfo->output_sar))
    {
        tis_16_9_to_4_3 = TRUE;
    }
    return tis_16_9_to_4_3;
}

/**
 * Determines if the source scaling factors indicate that the
 * incoming video frame is to be clipped (like taking a scissors
 * and cutting out a rectangle).
 *
 * @param   uiInfo   display info
 * @return  true if source video is to be clipped, false otherwise
 */
static gboolean is_video_clipped(UIInfo* uiInfo)
{
    gboolean is_clipped = FALSE;
    if ((0.0 != uiInfo->pVideoInfo->scale_src_x) || (0.0
            != uiInfo->pVideoInfo->scale_src_y) || (1.0
            != uiInfo->pVideoInfo->scale_src_width) || (1.0
            != uiInfo->pVideoInfo->scale_src_height))
    {
        is_clipped = TRUE;
    }

    return is_clipped;
}

/**
 * Determine if the destination scaling factors indicate that
 * the output video frame is to be scaled.
 *
 * @param   uiInfo   display information
 * @return  true if output video is to be scaled, false otherwise
 */
static gboolean is_video_scaled(UIInfo* uiInfo)
{
    gboolean is_scaled = FALSE;
    if ((0.0 != uiInfo->pVideoInfo->scale_dest_x) || (0.0
            != uiInfo->pVideoInfo->scale_dest_y) || (1.0
            != uiInfo->pVideoInfo->scale_dest_width) || (1.0
            != uiInfo->pVideoInfo->scale_dest_height))
    {
        is_scaled = TRUE;
    }

    return is_scaled;
}

/**
 * Convert source data into rectangle which has desired SAR by
 * stretching the width while maintaining height using a function
 * which normalizes horizontal output coordinates.
 *
 * @param uiInfo  display information
 */
static void opengl_dfc_wide(UIInfo* uiInfo)
{
    // Determine the size of the buffer.
    guint height = 0;
    guint width = 0;
    opengl_get_clipped_buffer_sz(uiInfo, &width, &height);

    opengl_allocate_clip_buffer(uiInfo, width, height);

    // Transfer the pixels in image buffer into dfc conversion buffer
    // using the dfc conversion formula for wide
    if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
    {
        opengl_yuv_wide(uiInfo);
    }
    else
    {
        opengl_rgb_wide(uiInfo);
    }
}

/**
 * Determine a rectangle in source which is centered and matches the desired output SAR,
 * by adjusting the height of the source rectangle.
 *
 * @param   uiInfo   display information
 */
static void opengl_dfc_zoom(UIInfo* uiInfo)
{
    // Determine size of buffer
    guint width = 0;
    guint height = 0;
    opengl_get_clipped_buffer_sz(uiInfo, &width, &height);

    opengl_allocate_clip_buffer(uiInfo, width, height);

    // Transfer the pixels in image buffer into dfc conversion buffer
    // using the dfc conversion formula for wide
    int y_start = (uiInfo->pDisplayImageBuffer->buffer_height
            - uiInfo->pVideoInfo->clip_buffer_height) / 2;

    if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
    {
        opengl_yuv_extract(uiInfo, 0, // x start
                uiInfo->pDisplayImageBuffer->buffer_width, // x end
                y_start, // y start
                y_start + uiInfo->pVideoInfo->clip_buffer_height); // y end
    }
    else
    {
        opengl_rgb_extract(uiInfo, 0, // x start
                uiInfo->pDisplayImageBuffer->buffer_width, // x end
                y_start, // y start
                y_start + uiInfo->pVideoInfo->clip_buffer_height); // y end
    }
}

static void opengl_get_clipped_buffer_sz(UIInfo* uiInfo, guint *pwidth,
        guint *pheight)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    float window_sar = (float) (uiInfo->pDisplayInfo->par_n
            * uiInfo->pWindowInfo->width)
            / (float) (uiInfo->pDisplayInfo->par_d
                    * uiInfo->pWindowInfo->height);

    float adjusted_window_width = uiInfo->pWindowInfo->width;
    float adjusted_window_height = uiInfo->pWindowInfo->height;
    if (window_sar > uiInfo->pVideoInfo->output_sar)
    {
        adjusted_window_width = (uiInfo->pVideoInfo->output_sar / window_sar)
                * uiInfo->pWindowInfo->width;
    }
    else if (window_sar < uiInfo->pVideoInfo->output_sar)
    {
        adjusted_window_height = (window_sar / uiInfo->pVideoInfo->output_sar)
                * uiInfo->pWindowInfo->height;
    }

    *pwidth = (guint)(adjusted_window_width
            / (uiInfo->pVideoInfo->aspect_ratio_width_factor));
    if (*pwidth > uiInfo->pVideoInfo->incoming_width)
    {
        *pwidth = uiInfo->pVideoInfo->incoming_width;
    }

    *pheight = (guint)(adjusted_window_height
            / (uiInfo->pVideoInfo->aspect_ratio_height_factor));
    if (*pheight > uiInfo->pVideoInfo->incoming_height)
    {
        *pheight = uiInfo->pVideoInfo->incoming_height;
    }

    // If height is not even value, round up to ensure complete row
    if (*pheight % 2)
    {
        (*pheight)++;
    }

    RILOG_DEBUG("%s -- Exit, width = %d, height = %d\n", __FUNCTION__, *pwidth,
            *pheight);
}

/**
 * Determine a rectangle in source which is a centered cut out and matches
 * the desired output SAR, by adjusting the width of the source rectangle.
 *
 * @param   uiInfo   display information
 */
static void opengl_dfc_cco(UIInfo* uiInfo)
{
    // Determine the size of the buffer.
    guint height = 0;
    guint width = 0;
    opengl_get_clipped_buffer_sz(uiInfo, &width, &height);

    opengl_allocate_clip_buffer(uiInfo, width, height);

    // Transfer the pixels in image buffer into dfc conversion buffer
    // using the dfc conversion formula for wide
    int x_start = (uiInfo->pDisplayImageBuffer->buffer_width
            - uiInfo->pVideoInfo->clip_buffer_width) / 2;

    if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
    {
        opengl_yuv_extract(uiInfo, x_start, // x start
                x_start + uiInfo->pVideoInfo->clip_buffer_width - 1, // x end
                0, // y start
                uiInfo->pDisplayImageBuffer->buffer_height); // y end
    }
    else
    {
        opengl_rgb_extract(uiInfo, x_start, // x start
                x_start + uiInfo->pVideoInfo->clip_buffer_width - 1, // x end
                0, // y start
                uiInfo->pDisplayImageBuffer->buffer_height); // y end
    }
}

/**
 * Creates a buffer using the source image frame which is a rectangle
 * cut from source where the coordinates of the rectangle are determined
 * by the pan scan vectors which are supplied as meta data on the video frame.
 *
 * @param   uiInfo
 *
 * NOTE: This method needs to actually retrieve and use the pan scan vectors
 * when they are available in the meta data.
 */
static void opengl_dfc_panscan(UIInfo* uiInfo)
{
    // Determine the size of the buffer.
    guint height = 0;
    guint width = 0;
    opengl_get_clipped_buffer_sz(uiInfo, &width, &height);

    opengl_allocate_clip_buffer(uiInfo, width, height);

    // *TODO* - add logic to extract pan scan vectors from meta data
    // and use to calculate x/y start & end
    // For now, it is set to do same as center cut out dfc
    int x_start = (uiInfo->pDisplayImageBuffer->buffer_width
            - uiInfo->pVideoInfo->clip_buffer_width) / 2;

    // Transfer the pixels in image buffer into dfc conversion buffer
    // using the dfc conversion formula for wide
    if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
    {
        opengl_yuv_extract(uiInfo, x_start, // x start
                x_start + uiInfo->pVideoInfo->clip_buffer_width - 1, // x end
                0, // y start
                uiInfo->pDisplayImageBuffer->buffer_height); // y end
    }
    else
    {
        opengl_rgb_extract(uiInfo, x_start, // x start
                x_start + uiInfo->pVideoInfo->clip_buffer_width - 1, // x end
                0, // y start
                uiInfo->pDisplayImageBuffer->buffer_height); // y end
    }
}

/**
 * Utility function to extract a rectangular portion of the YUV image
 * specified by the supplied x & y coordinates and store it in the
 * clip buffer.
 *
 * @param   uiInfo   buffer information
 * @param   x_start  starting x index in image buffer
 * @param   x_end    ending x index in image buffer
 * @param   y_start  starting y index in image buffer
 * @param   y_end    ending y idex in image buffer
 */
static void opengl_yuv_extract(UIInfo* uiInfo, int x_start, int x_end,
        int y_start, int y_end)
{
    // Source data is always incoming image buffer
    guchar* src_data = uiInfo->pDisplayImageBuffer->buffer_data;
    guint src_width = uiInfo->pDisplayImageBuffer->buffer_width;
    guint src_height = uiInfo->pDisplayImageBuffer->buffer_height;

    // Destination data is always stored in clip buffer
    guchar* dest_data = uiInfo->pVideoInfo->pClipBuffer;
    guint dest_width = uiInfo->pVideoInfo->clip_buffer_width;
    guint dest_height = uiInfo->pVideoInfo->clip_buffer_height;

    // current x, y offsets
    register int x, y;

    // x, y indexes into U & V buffers
    int x_prime, x_prime_dest, y_prime, y_prime_dest;

    // base offsets to U and V image buffers
    int uBufferBase, vBufferBase;
    int uDestBufferBase, vDestBufferBase;

    // Index offset to current pixel in U and V image buffers
    int uvPixelIndex;
    int uvDestPixelIndex;

    uBufferBase = src_width * src_height;
    vBufferBase = (int) (src_width * src_height * 1.25);

    uDestBufferBase = dest_width * dest_height;
    vDestBufferBase = (int) (dest_width * dest_height * 1.25);

    // pitch of a single row in the U and V buffers
    int uvPitch = src_width / 2;
    int uvDestPitch = dest_width / 2;

    // Work through each pixel of the image, convert the Y, U, and V
    // elements.
    //
    // Things to note:
    // For each pixel, there is an 8-bit Y value.  These values are packed so that
    // they are contiguous in memory.
    // Following the Y values in the source buffer are the U values.
    // There is one U value for every 4 I values arranged in a 2x2 square.
    // Following the U values in the source buffer are the V values.
    // There is one V value for every 4 I values arranged in a 2x2 square.
    //
    // In the following, the x and y values iterate through each pixel offset in the image.
    // These correspond 1:1 in the buffer of I values.
    // The x_prime and y_prime values are calculated to iterate through the corresponding pixel
    // offsets in the U and V buffers.  Since each U and V entry represents 4 pixels in the
    // actual image, these are each divided by two so that they line up correctly.

    // Determine the starting x coordinate
    int x_dest = 0;

    // Determine the starting y coordinate
    int y_dest = 0;

    // Calculate the starting y index and max y index
    for (x = x_start; x < x_end; x++)
    {
        // divide the x offset by two for use in the U and V buffers
        x_prime = x >> 1;
        x_prime_dest = x_dest >> 1;
        y_dest = 0;

        for (y = y_start; y < y_end; y++)
        {
            // divide the y offset by two for use in the U and V buffers
            y_prime = y >> 1;
            y_prime_dest = y_dest >> 1;

            uvPixelIndex = (y_prime * uvPitch) + x_prime;
            uvDestPixelIndex = (y_prime_dest * uvDestPitch) + x_prime_dest;

            // get Y value (intensity component) from video frame
            dest_data[(y_dest * dest_width) + x_dest]
                    = src_data[(y * src_width) + x];

            // get U value (blue component) from video frame
            dest_data[uDestBufferBase + uvDestPixelIndex]
                    = src_data[uBufferBase + uvPixelIndex];

            // get V value (red component) from video frame
            dest_data[vDestBufferBase + uvDestPixelIndex]
                    = src_data[vBufferBase + uvPixelIndex];

            // Increment the destination index
            y_dest++;
        }
        x_dest++;
    }
}

/**
 * Utility function to extract a rectangular portion of the RGB image
 * specified by the supplied x & y coordinates and store it in the
 * clip buffer.
 *
 * @param   uiInfo   buffer information
 * @param   x_start  starting x index in image buffer
 * @param   x_end    ending x index in image buffer
 * @param   y_start  starting y index in image buffer
 * @param   y_end    ending y idex in image buffer
 */
static void opengl_rgb_extract(UIInfo* uiInfo, int x_start, int x_end,
        int y_start, int y_end)
{
    if (NULL == g_uiCat1)
        g_uiCat1 = log4c_category_get(LOG_CAT);

    // Source data is always incoming image buffer
    guchar* src_data = uiInfo->pDisplayImageBuffer->buffer_data;
    guint src_width = uiInfo->pDisplayImageBuffer->buffer_width;
    guint src_height = uiInfo->pDisplayImageBuffer->buffer_height;

    // Destination data is always stored in clip buffer
    guchar* dest_data = uiInfo->pVideoInfo->pClipBuffer;
    guint dest_width = uiInfo->pVideoInfo->clip_buffer_width;
    guint dest_height = uiInfo->pVideoInfo->clip_buffer_height;

    register int y;
    int y_dest = 0;
    int dest_offset = 0;
    int src_offset = 0;

    // Copy each horizontal line into destination buffer
    for (y = y_start; y < y_end; y++)
    {
        // Current row in dest buffer, always starts at x=0
        dest_offset = y_dest * dest_width * 4;

        // Current row in src buffer, adjusted for starting x
        src_offset = (y * src_width * 4) + (x_start * 4);

        if (dest_offset >= (int) (dest_width * dest_height * 4))
        {
            RILOG_ERROR("%s -- dest buffer offset out of range: %d, max: %d\n",
                    __FUNCTION__, dest_offset, (dest_width * dest_height * 4));
        }
        else if (src_offset >= (int) (src_width * src_height * 4))
        {
            RILOG_ERROR("%s -- src buffer offset out of range: %d, max: %d\n",
                    __FUNCTION__, src_offset, (src_width * src_height * 4));
        }
        else
        {
            memcpy((dest_data + dest_offset), (src_data + src_offset),
                    dest_width * 4);
        }
        y_dest++;
    }
}

/**
 * Utility function to perform wide conversion on YUV image.
 * Can not use standard yuv extract method because function is
 * applied to generate additional pixel data to fill screen.
 *
 * @param   uiInfo   display information
 */
static void opengl_yuv_wide(UIInfo* uiInfo)
{
    // Source data is always incoming image buffer
    guchar* src_data = uiInfo->pDisplayImageBuffer->buffer_data;
    guint src_width = uiInfo->pDisplayImageBuffer->buffer_width;
    guint src_height = uiInfo->pDisplayImageBuffer->buffer_height;

    // Destination data is always stored in clip buffer
    guchar* dest_data = uiInfo->pVideoInfo->pClipBuffer;
    guint dest_width = uiInfo->pVideoInfo->clip_buffer_width;
    guint dest_height = uiInfo->pVideoInfo->clip_buffer_height;

    // current x, y offsets
    register int x_dest, prev_x_dest;
    register unsigned int x, y;

    // x, y indexes into U & V buffers
    int x_prime, y_prime, x_prime_dest;
    int x_prime_prev_dest;

    // base offsets to U and V image buffers
    int uBufferBase, vBufferBase;
    int uDestBufferBase, vDestBufferBase;

    // index offset to current pixel in U and V image buffers
    int uvPixelIndex;
    int uvDestPixelIndex;

    uBufferBase = src_width * src_height;
    vBufferBase = (int) ((src_width * src_height) * 1.25);

    uDestBufferBase = dest_width * dest_height;
    vDestBufferBase = (int) ((dest_width * dest_height) * 1.25);

    // pitch of a single row in the U and V buffers
    int uvPitch = src_width / 2;
    int uvDestPitch = dest_width / 2;

    // Work through each pixel of the image, convert the Y, U, and V
    // elements.
    //
    // Things to note:
    // For each pixel, there is an 8-bit Y value.  These values are packed so that
    // they are contiguous in memory.
    // Following the Y values in the source buffer are the U values.
    // There is one U value for every 4 I values arranged in a 2x2 square.
    // Following the U values in the source buffer are the V values.
    // There is one V value for every 4 I values arranged in a 2x2 square.
    //
    // In the following, the x and y values iterate through each pixel offset in the image.
    // These correspond 1:1 in the buffer of I values.
    // The x_prime and y_prime values are calculated to iterate through the corresponding pixel
    // offsets in the U and V buffers.  Since each U and V entry represents 4 pixels in the
    // actual image, these are each divided by two so that they line up correctly.
    x_dest = 0;
    for (x = 0; x < src_width; x++)
    {
        // Calculate the x for destination for given source index
        prev_x_dest = x_dest;
        x_dest = (int) (((asin((2 * ((float) x / (float) src_width)) - 1.0)
                / 3.0) + 0.5) * dest_width);

        // If the calculated index is less than 0, set it to zero
        if (x_dest < 0)
        {
            x_dest = 0;
        }

        // Set skipped destination indices to previous values
        if (prev_x_dest + 1 < x_dest)
        {
            int i = 0;
            x_prime_prev_dest = prev_x_dest >> 1;
            while (prev_x_dest + i < x_dest)
            {
                for (y = 0; y < src_height; y++)
                {
                    // divide the y offset by two for use in the U and V buffers
                    y_prime = y >> 1;

                    uvDestPixelIndex = (y_prime * uvDestPitch)
                            + x_prime_prev_dest;

                    dest_data[(y * dest_width) + prev_x_dest + i]
                            = dest_data[(y * dest_width) + prev_x_dest];
                    dest_data[uDestBufferBase + uvDestPixelIndex + i]
                            = dest_data[uDestBufferBase + uvDestPixelIndex];
                    dest_data[vDestBufferBase + uvDestPixelIndex + i]
                            = dest_data[vDestBufferBase + uvDestPixelIndex];
                }
                i++;
            }
        }

        // divide the x offset by two for use in the U and V buffers
        x_prime = x >> 1;
        x_prime_dest = x_dest >> 1;

        for (y = 0; y < src_height; y++)
        {
            // divide the y offset by two for use in the U and V buffers
            y_prime = y >> 1;

            uvPixelIndex = (y_prime * uvPitch) + x_prime;
            uvDestPixelIndex = (y_prime * uvDestPitch) + x_prime_dest;

            // get Y value (intensity component) from video frame
            dest_data[(y * dest_width) + x_dest]
                    = src_data[(y * src_width) + x];

            // get U value (blue component) from video frame
            dest_data[uDestBufferBase + uvDestPixelIndex]
                    = src_data[uBufferBase + uvPixelIndex];

            // get V value (red component) from video frame
            dest_data[vDestBufferBase + uvDestPixelIndex]
                    = src_data[vBufferBase + uvPixelIndex];
        }
    }
}

/**
 * Utility function to perform wide conversion on YUV image.
 * Can not use standard yuv extract method because function is
 * applied to generate additional pixel data to fill screen.
 *
 * @param   uiInfo   display information
 */
static void opengl_rgb_wide(UIInfo* uiInfo)
{
    // Source data is always incoming image buffer
    guchar* src_data = uiInfo->pDisplayImageBuffer->buffer_data;
    guint src_width = uiInfo->pDisplayImageBuffer->buffer_width;
    guint src_height = uiInfo->pDisplayImageBuffer->buffer_height;

    // Destination data is always stored in clip buffer
    guchar* dest_data = uiInfo->pVideoInfo->pClipBuffer;
    guint dest_width = uiInfo->pVideoInfo->clip_buffer_width;
    guint dest_height = uiInfo->pVideoInfo->clip_buffer_height;

    // current x, y offsets
    register int x_dest, prev_x_dest;
    register unsigned int x, y;

    x_dest = 0;
    for (x = 0; x < src_width; x++)
    {
        // Calculate the x for destination for given source index
        prev_x_dest = x_dest;
        x_dest = (int) (((asin((2 * ((float) x / (float) src_width)) - 1.0)
                / 3.0) + 0.5) * dest_width);

        // If the calculated index is less than 0, set it to zero
        if (x_dest < 0)
        {
            x_dest = 0;
        }

        // Set skipped destination indices to previous values
        if (prev_x_dest + 1 < x_dest)
        {
            int i = 0;
            while (prev_x_dest + i < x_dest)
            {
                for (y = 0; y < src_height; y++)
                {
                    // Copy four values at previous destination to skipped destination
                    memcpy(dest_data + (y * dest_width * 4)
                            + ((prev_x_dest + i) * 4), dest_data + (y
                            * dest_width * 4) + (prev_x_dest * 4), 4);
                }
                i++;
            }
        }

        // Set the row value for this index
        for (y = 0; y < dest_height; y++)
        {
            // Copy the four values to destination
            memcpy(dest_data + (y * dest_width * 4) + (x_dest * 4), src_data
                    + (y * src_width * 4) + (x * 4), 4);
        }
    }
}

/**
 * Allocates the scale buffer which is used to transform the source rectangle
 * into the desired source rectangle to use for output.  A buffer is also
 * allocated to perform YUV conversion as necessary.
 *
 * @param   uiInfo   display information
 * @param   width    width of the image which needs buffers allocated
 * @param   height   height of the image which needs buffers allocated
 */
static void opengl_allocate_clip_buffer(UIInfo* uiInfo, guint width,
        guint height)
{
    if (NULL == g_uiCat1)
        g_uiCat1 = log4c_category_get(LOG_CAT);

    // Free old buffer if it exists and is wrong size
    if ((NULL != uiInfo->pVideoInfo->pClipBuffer) && ((width
            != uiInfo->pVideoInfo->clip_buffer_width) || (height
            != uiInfo->pVideoInfo->clip_buffer_height)))
    {
        RILOG_DEBUG(
                "%s -- freeing clip buffer, old wd %d, old ht %d, new wd %d, new ht %d\n",
                __FUNCTION__, uiInfo->pVideoInfo->clip_buffer_width,
                uiInfo->pVideoInfo->clip_buffer_height, width, height);

        g_free(uiInfo->pVideoInfo->pClipBuffer);
        uiInfo->pVideoInfo->pClipBuffer = NULL;

        // Free the conversion buffer also
        if (NULL != uiInfo->pVideoInfo->pClipConversionBuff)
        {
            g_free(uiInfo->pVideoInfo->pClipConversionBuff);
            uiInfo->pVideoInfo->pClipConversionBuff = NULL;
        }
    }

    // Allocate new buffer if one doesn't exist
    if (NULL == uiInfo->pVideoInfo->pClipBuffer)
    {
        uiInfo->pVideoInfo->clip_buffer_width = width;
        uiInfo->pVideoInfo->clip_buffer_height = height;

        RILOG_DEBUG("%s -- allocating new buffer, width %d, height %d\n",
                __FUNCTION__, uiInfo->pVideoInfo->clip_buffer_width,
                uiInfo->pVideoInfo->clip_buffer_height);

        // there should be 4 bytes/pixel for each of the RGBA values
        float multiplier = 4;
        if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
        {
            // there should 1 byte/pixel for the Y values,
            //    plus .25 bytes/pixel for each of the U and V values
            multiplier = 1.5;
        }

        uiInfo->pVideoInfo->clip_buffer_size
                = (int) (uiInfo->pVideoInfo->clip_buffer_width
                        * uiInfo->pVideoInfo->clip_buffer_height
                        * multiplier) + 2;  // add two to account for partially empty U and V values at the end of the U and V arrays
        uiInfo->pVideoInfo->pClipBuffer = g_try_malloc0(
                uiInfo->pVideoInfo->clip_buffer_size);
        if (NULL == uiInfo->pVideoInfo->pClipBuffer)
        {
            RILOG_ERROR("%s -- Failed allocating memory for clip buffer\n",
                    __FUNCTION__);
            return;
        }

        // Only allocate a conversion buffer if YUV
        if (TRUE == uiInfo->pDisplayImageBuffer->isI420YUV)
        {
            uiInfo->pVideoInfo->clip_conversion_buffer_size
                    = uiInfo->pVideoInfo->clip_buffer_width
                            * uiInfo->pVideoInfo->clip_buffer_height
                            * RGB_BYTES_PER_PIXEL;
            uiInfo->pVideoInfo->pClipConversionBuff = g_try_malloc0(
                    uiInfo->pVideoInfo->clip_conversion_buffer_size);
            if (NULL == uiInfo->pVideoInfo->pClipConversionBuff)
            {
                RILOG_ERROR(
                        "%s -- Failed allocating memory for clip conversion buffer\n",
                        __FUNCTION__);
                return;
            }
        }
    }
}

/**
 * Free the memory allocated for freeze buffer when video is no longer
 * frozen.
 *
 * @param   uiInfo   buffer info
 */
static void opengl_video_free_freeze_buffer(UIInfo* uiInfo)
{
    if ((!uiInfo->pVideoInfo->is_frozen) && (NULL
            != uiInfo->pVideoInfo->freeze_buffer))
    {
        // Not frozen, release freeze buffer memory
        g_free(uiInfo->pVideoInfo->freeze_buffer);
        uiInfo->pVideoInfo->freeze_buffer = NULL;
    }
}

/**
 * Free the memory allocated for block buffer when video is no longer
 * blocked.
 *
 * @param   uiInfo   buffer info
 */
static void opengl_video_free_block_buffer(UIInfo* uiInfo)
{
    if ((!uiInfo->pVideoInfo->is_blocked) && (NULL
            != uiInfo->pVideoInfo->block_screen_buffer))
    {
        // Not blocked, release buffer memory
        g_free(uiInfo->pVideoInfo->block_screen_buffer);
        uiInfo->pVideoInfo->block_screen_buffer = NULL;
    }
}

static char *make_snapshot_filename(char *fileName, int fileNameSize)
{
    char date[128];
    (void) snprintf(fileName, fileNameSize, "%ssnapshot-%sbmp", snapshotPath,
            dateString(date, 127));
    return fileName;
}

static void opengl_display_save(UIInfo* uiInfo)
{
    RILOG_TRACE("%s -- Entry, uiInfo = (%p)\n", __FUNCTION__, uiInfo);

    if ((uiInfo) && (uiInfo->pGraphicsInfo))
    {
        short unusedShort = 0;
        short colorPlanes = 1;
        short bits = 24;
        int colorMode = 3;
        int colors = 0;
        int w = uiInfo->pGraphicsInfo->width;
        int h = uiInfo->pGraphicsInfo->height;
        int size = w * h * colorMode;
        int header = 54;
        int offset = 40;
        int compression = 0;
        int totalSize = size + header;
        int pixelResolution = 2835;
        char file[512];
        FILE *pFile = fopen(make_snapshot_filename(file, sizeof(file)), "wb");

        if (pFile)
        {
            unsigned char *image =
                    (unsigned char*) g_try_malloc0(w * h * colorMode);

            if (image)
            {
                glReadPixels(0, 0, w, h, GL_BGR, GL_UNSIGNED_BYTE, image);

                // Write BMP header.
                fwrite("BM", sizeof(unsigned char), 2, pFile);
                fwrite(&totalSize, sizeof(int), 1, pFile);
                fwrite(&unusedShort, sizeof(short), 1, pFile);
                fwrite(&unusedShort, sizeof(short), 1, pFile);
                fwrite(&header, sizeof(int), 1, pFile);
                fwrite(&offset, sizeof(int), 1, pFile);
                fwrite(&w, sizeof(int), 1, pFile);
                fwrite(&h, sizeof(int), 1, pFile);
                fwrite(&colorPlanes, sizeof(short), 1, pFile);
                fwrite(&bits, sizeof(short), 1, pFile);
                fwrite(&compression, sizeof(int), 1, pFile);
                fwrite(&size, sizeof(int), 1, pFile);
                fwrite(&pixelResolution, sizeof(int), 1, pFile);
                fwrite(&pixelResolution, sizeof(int), 1, pFile);
                fwrite(&colors, sizeof(int), 1, pFile);
                fwrite(&colors, sizeof(int), 1, pFile);

                // Write image.
                fwrite(image, sizeof(unsigned char), (w * h * colorMode), pFile);

                g_free(image);
                RILOG_DEBUG("%s -- Wrote (%d x %d) %s\n", __FUNCTION__, w, h,
                        file);
            }

            fclose(pFile);
        }
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}
