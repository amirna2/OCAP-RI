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

#include <ri_display.h>
#include <gstreamer/gstdisplay.h>
#include "ui_opengl_common.h"
#include "ui_window_common.h"
#include <gst/gst.h>
#include <stdlib.h>
#include <string.h>

#include <gst/interfaces/xoverlay.h>
#include "display.h"
#include "platform.h"
#include <ri_log.h>
#include <ri_types.h>
#include "gst_utils.h"
#include "video_device.h"
#include <test_interface.h>

#define RILOG_CATEGORY riDisplayCat
log4c_category_t* riDisplayCat = NULL;

// Global variables use to configure display setup
//
static gboolean g_createTestPipeline;
static gboolean g_testProperties;
static gboolean g_runTests;
static GraphicsInfo* graphicsInfo;
static ri_video_device_t* status_video_device;

/**
 * Display information
 */
struct ri_display_data_s
{
    GstElement* gst_videosrc;
    GstElement* gst_videosink;

    ri_video_device_t* video_device;
};

// Forward declarations
//
ri_video_device_t* get_video_device(ri_display_t* object);
void* get_graphics_buffer(ri_display_t* object, ri_env env);
void draw_graphics_buffer(ri_display_t* object, ri_env env);
void update_configuration(ri_display_t* object, uint32_t graphicsWidth,
        uint32_t graphicsHeight, uint32_t graphicsPARx, uint32_t graphicsPARy,
        uint32_t videoWidth, uint32_t videoHeight, uint32_t videoPARx,
        uint32_t videoPARy, uint32_t backgroundWidth,
        uint32_t backgroundHeight, uint32_t backgroundPARx,
        uint32_t backgroundPARy, ri_env env);

void set_dfc_mode(ri_display_t* object, int32_t dfc);
void set_dfc_default(ri_display_t* object, int32_t dfc);
void set_bg_color(ri_display_t* object, uint32_t color);
void block_presentation(ri_display_t* object, ri_bool block);
void freeze_video(ri_display_t* object);
void resume_video(ri_display_t* object);
void get_incoming_video_aspect_ratio(ri_display_t* object, int32_t* ar);
void get_incoming_video_size(ri_display_t* object, int32_t* width,
        int32_t* height);
void get_video_afd(ri_display_t* object, uint32_t* adf);
void set_bounds(ri_display_t* object, ri_rect* src, ri_rect* dest);
void get_bounds(ri_display_t* object, ri_rect* src, ri_rect* dest);
void check_bounds(ri_display_t* object, ri_rect* desiredSrc,
        ri_rect* desiredDst, ri_rect* actualSrc, ri_rect* actualDst);
void get_scaling(ri_display_t* object, int32_t* positioning, float** horiz,
        float** vert, ri_bool* hRange, ri_bool* vRange, ri_bool* canClip,
        ri_bool* supportsComponent);
int32_t get_threedtv_info(ri_display_t* object, int32_t* formatType, int32_t* payloadType,
            uint32_t* payloadSz, uint8_t* payload, int32_t* scanMode);
void block_display(ri_display_t* object, ri_bool block);

// Local routines
//
static void read_config_values(GstElement* videosink);
static void read3DTVConfig(GstElement* videosink);
static void init_graphics_surface(GstDisplay* display, guint width,
        guint height, guint par_n, guint par_d, ri_env env);
static void set_snapshot_display(GstDisplay *riDisplay);
void take_snapshot(void);

void addTVSafeArea (unsigned char * pData, int height, int width, int bpl, float widthFraction, float heightFraction);
void drawSafeAreaHorizontalLine (unsigned char * pData, int y1, int x1, int x2, int bytesPerLine);
void drawSafeAreaVerticalLine (unsigned char * pData, int x1, int y1, int y2, int bytesPerLine);


// Test routines
//
static void create_test_pipeline(ri_display_t* display, GstElement* videosink);
gboolean test(gpointer display);
void test_draw_something(ri_display_t* riDisplay);
void test_draw_square(ri_display_t* riDisplay);
static inline void test_drawPixel(unsigned char* pGraphics, unsigned char red,
        unsigned char green, unsigned char blue, unsigned char alpha);
void test_set_display_properties(GstElement* videosink);
void test_draw_square_with_border(ri_display_t* riDisplay);
void test_freeze_resume(ri_display_t* riDisplay);
void test_block_unblock(ri_display_t* riDisplay);
void test_video_scaling(ri_display_t* riDisplay);
void test_reset(ri_display_t* riDisplay);
extern void decode_bin_status(ri_video_device_t* video_device);

#define DISPLAY_TESTS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| d | dump display data     \r\n" \
    "|---+-----------------------\r\n" \
    "| s | snapshot display      \r\n" \
    "|---+-----------------------\r\n" \
    "| k | key input             \r\n" \


static int testInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    int retVal;

    RILOG_TRACE("%s -- Entry, received: %s\n", __FUNCTION__, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "d"))
    {
        decode_bin_status(status_video_device);
        retVal = 0;
    }
    else if (strstr(rxBuf, "s"))
    {
        test_SendString(sock, "\r\n\nsnapshot display...\r\n");
        take_snapshot();
        retVal = 0;
    }
    else if (strstr(rxBuf, "k"))
    {
        char buf[64];
        memset(buf, 0, sizeof(buf));

        // Retrieve the key value.
        if (test_GetString(sock, buf, sizeof(buf), "\r\n\nkey: "))
        {
            RILOG_DEBUG("Recieved Key %s\n", buf);
        }
        else
        {
            RILOG_ERROR("%s -- test_GetString failure?!\n", __FUNCTION__);
        }

        // Process the key value.
        int value = atoi(buf);
        ri_process_key(value);

        retVal = 0;
    }
    else if (strstr(rxBuf, "x"))
    {
        retVal = -1;
    }
    else
    {
        strcat(rxBuf, " - unrecognized\r\n\n");
        test_SendString(sock, rxBuf);
        RILOG_WARN("%s %s\n", __FUNCTION__, rxBuf);
        *retCode = MENU_INVALID;
        retVal = 0;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

static MenuItem DisplayMenuItem =
{ TRUE, "d", "Display", DISPLAY_TESTS, testInputHandler };

/**
 * Creates the display plugin.
 *
 * @param videosink  display plugin which serves as the sink element for video.
 * @return  display related components
 */
ri_display_t* create_display()
{
    riDisplayCat = log4c_category_get("RI.Display");

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    ri_display_t* display = g_try_malloc0(sizeof(ri_display_t));

    if (NULL == display)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Associate the methods defined in the plugin
    display->get_video_device = get_video_device;
    display->get_graphics_buffer = get_graphics_buffer;
    display->draw_graphics_buffer = draw_graphics_buffer;
    display->update_configuration = update_configuration;
    display->set_dfc_mode = set_dfc_mode;
    display->set_dfc_default = set_dfc_default;
    display->set_bg_color = set_bg_color;
    display->block_presentation = block_presentation;
    display->freeze_video = freeze_video;
    display->resume_video = resume_video;
    display->get_incoming_video_size = get_incoming_video_size;
    display->get_incoming_video_aspect_ratio = get_incoming_video_aspect_ratio;
    display->get_video_afd = get_video_afd;
    display->set_bounds = set_bounds;
    display->get_bounds = get_bounds;
    display->check_bounds = check_bounds;
    display->get_scaling = get_scaling;
    display->get_threedtv_info = get_threedtv_info;
    display->block_display = block_display;
	
    // Allocate object memory
    display->data = g_try_malloc0(sizeof(ri_display_data_t));

    if (NULL == display->data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Create the video device
    status_video_device = display->data->video_device = create_video_device();

    // Get the display plugin
    GstElement* videosink = get_video_sink_element(display->data->video_device);

    // Get config parameters related to window creation
    read_config_values(videosink);

    read3DTVConfig(videosink);


    // Assign the elements to data structure
    display->data->gst_videosink = videosink;

    // Create the video test pipeline if configured to do so
    if (g_createTestPipeline)
    {
        create_test_pipeline(display, videosink);
    }

    // Set properties for test purposes if configured to do so
    if (g_testProperties)
    {
        test_set_display_properties(videosink);
    }

    // Start a timer to run test to periodically if configured to do so
    if (g_runTests)
    {
        (void) g_timeout_add(5000, test, (gpointer) display);
    }

    test_RegisterMenu(&DisplayMenuItem);
    set_snapshot_display((GstDisplay*) display->data->gst_videosink);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return display;
}

ri_video_device_t* get_video_device(ri_display_t* object)
{
    ri_video_device_t* retVal;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    retVal = object->data->video_device;
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return retVal;
}

/**
 * Reads the display related config parameters from the config file.
 *
 * @param   display element
 */
static void read_config_values(GstElement* videosink)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    char* cfgVal;

    if (FALSE == ricfg_getBoolValue("RIPlatform",
                                    "RI.Platform.display.window_is_supplied"))
    {
        g_object_set(G_OBJECT(videosink), "supplied-window", FALSE, NULL);
    }

    if (TRUE == ricfg_getBoolValue("RIPlatform",
                                   "RI.Platform.display.window_is_fixed"))
    {
        g_object_set(G_OBJECT(videosink), "fixed-window", TRUE, NULL);
    }

    if ((cfgVal = ricfg_getValue("RIPlatform",
            "RI.Platform.display.window_width")) != NULL)
    {
        int width = atoi(cfgVal);
        if (0 != width)
        {
            g_object_set(G_OBJECT(videosink), "window-width", width, NULL);
        }
    }
    if ((cfgVal = ricfg_getValue("RIPlatform",
            "RI.Platform.display.window_height")) != NULL)
    {
        int height = atoi(cfgVal);
        if (0 != height)
        {
            g_object_set(G_OBJECT(videosink), "window-height", height, NULL);
        }
    }
    g_createTestPipeline = FALSE;

    if (TRUE == ricfg_getBoolValue("RIPlatform",
                                   "RI.Platform.display.create_test_pipeline"))
    {
        g_createTestPipeline = TRUE;
    }
    g_testProperties = FALSE;
    if (TRUE == ricfg_getBoolValue("RIPlatform",
                                   "RI.Platform.display.test_properties"))
    {
        g_testProperties = TRUE;
    }
    g_runTests = FALSE;
    if (TRUE == ricfg_getBoolValue("RIPlatform",
                                   "RI.Platform.display.run_tests"))
    {
        g_runTests = TRUE;
    }
    if (TRUE == ricfg_getBoolValue("RIPlatform",
                                   "RI.Platform.display.force_rgb_video"))
    {
        g_object_set(G_OBJECT(videosink), "force-rgb-video", TRUE, NULL);
    }
    if (TRUE == ricfg_getBoolValue("RIPlatform",
                               "RI.Platform.display.hw_acceleration_disabled"))
    {
        g_object_set(G_OBJECT(videosink), "hw-acceleration-disabled", TRUE, NULL);
    }
    else
    {
        g_object_set(G_OBJECT(videosink), "hw-acceleration-disabled", FALSE, NULL);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void read3DTVConfig(GstElement* videosink)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    GstDisplay *display;
    char* cfgVal;

    g_return_if_fail(GST_IS_DISPLAY(videosink));

    display = GST_DISPLAY(videosink);

    
    if ((cfgVal = ricfg_getValue("RIPlatform",
            "RI.Platform.video.scanmode")) != NULL)
    {
        int scanmode = atoi(cfgVal);
        if (0 != scanmode)
        {
            display->pUIInfo->pVideoInfo->scan_mode = scanmode;
        }
    }

    if ((cfgVal = ricfg_getValue("RIPlatform",
            "RI.Platform.3dtv.format")) != NULL)
    {
        int format = atoi(cfgVal);
        if (0 != format)
        {
            display->pUIInfo->pVideoInfo->threedtv_format_type = format;
        }
    }

    if ((cfgVal = ricfg_getValue("RIPlatform",
            "RI.Platform.3dtv.payloadtype")) != NULL)
    {
        int payloadtype = atoi(cfgVal);
        if (0 != payloadtype)
        {
            display->pUIInfo->pVideoInfo->threedtv_payload_type = payloadtype;
        }
    }

    if ((cfgVal = ricfg_getValue("RIPlatform",
            "RI.Platform.3dtv.payload")) != NULL)
    {
        int length = strlen(cfgVal);
        char * endPtr = cfgVal + length;
        char *tempPtr = cfgVal;
        int index = 0;

        // first parse the string to get the payload array length
        while (tempPtr < endPtr)
        {
            (void) strtol (tempPtr, &tempPtr, 16);
            index++;
        }

        // allocate and populate payload array
        display->pUIInfo->pVideoInfo->threedtv_payload = (unsigned char *)malloc (index);
        display->pUIInfo->pVideoInfo->threedtv_payload_sz = index;

        index = 0;
        tempPtr = cfgVal;
        while (tempPtr < endPtr)
        {
            int32_t tempInt = strtol (tempPtr, &tempPtr, 16);

            display->pUIInfo->pVideoInfo->threedtv_payload[index] = (unsigned char) tempInt;

            index++;
        }
    }    
}

/**
 * Create a pipeline which has a test video source and the display element
 * as a video sink used only for testing.
 *
 * @param   display     display element used as video sink
 * @param   videosink   element representation of this display element
 */
static void create_test_pipeline(ri_display_t* display, GstElement* videosink)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // For test pipeline, retrieve test video source
    //   GstElement *videosrc = gst_element_factory_make("videotestsrc", "test-source");
    GstElement *videosrc = gst_load_element("testvideosrc", "test-source");

    // Temporary video src, set properties here for testing only
    // Set the video test pattern here:
    // 0 - color bars
    // 1 - snow
    // 2 - black
    // 3 - white
    // 5 - green
    // 6 - blue
    // 11 - circular
    g_object_set(G_OBJECT(videosrc), "pattern", 0, NULL);

    display->data->gst_videosrc = videosrc;

    // Create a test pipeline
    GstElement* pipeline = gst_pipeline_new("video pipeline");
    gst_bin_add_many(GST_BIN(pipeline), videosrc, videosink, NULL);
    (void) gst_element_link_many(videosrc, videosink, NULL);

    // Start up the test video pipeline
    (void) gst_element_set_state(pipeline, GST_STATE_PLAYING);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Initiates update of the video, graphics and background in the display plugin.
 *
 * @param object  display object which has reference to display plugin
 * @param videoWidth    requested video width in pixels
 * @param videoHeight   requested video height in pixels
 */
void update_configuration(ri_display_t* object, uint32_t graphicsWidth,
        uint32_t graphicsHeight, uint32_t graphicsPARx, uint32_t graphicsPARy,
        uint32_t videoWidth, uint32_t videoHeight, uint32_t videoPARx,
        uint32_t videoPARy, uint32_t backgroundWidth,
        uint32_t backgroundHeight, uint32_t backgroundPARx,
        uint32_t backgroundPARy, ri_env env)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        // Initialize the surface given the desired graphics parameters
        init_graphics_surface(display, graphicsWidth, graphicsHeight,
                graphicsPARx, graphicsPARy, env);

        gst_display_update_configuration(display, videoWidth, videoHeight,
                videoPARx, videoPARy);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Retrieves pointer to graphics buffer
 *
 * @param object  display object which has reference to display plugin
 * @return pointer to memory allocated for graphics pixel data
 */
void* get_graphics_buffer(ri_display_t* object, ri_env env)
{
    RILOG_TRACE("%s -- Entry, env: %d --\n", __FUNCTION__, env);

    void* buffer = NULL;
    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if ((NULL != display) && (NULL != display->pUIInfo->pGraphicsInfo))
    {
        buffer = display->pUIInfo->pGraphicsInfo->pixel_data[env];
        // buffer = display->pUIInfo->pGraphicsInfo->pixel_data;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return buffer;
}

/**
 * Initiates an render operation of the graphics buffer.
 *
 * @param object display object which has reference to display plugin
 */
void draw_graphics_buffer(ri_display_t* object, ri_env env)
{
    ri_bool enableSafeArea = FALSE;
    char *pTemp;


    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if ((NULL != display) && (NULL != display->pUIInfo->pGraphicsInfo))
    {
        // Get the lock
        g_mutex_lock(display->pUIInfo->flow_lock);

        // add safe area if specified in cfg file	
        enableSafeArea = ricfg_getBoolValue("RIPlatform",
                                        "RI.Platform.display.enable_safe_area");
        if (enableSafeArea)
        {
            float widthFraction = 0.0;
            float heightFraction = 0.0;

            if ((pTemp = ricfg_getValue("RIPlatform", "RI.Platform.display.safe_area_width_fraction")))
            {
                sscanf (pTemp, "%f", &widthFraction);
            }
            if ((pTemp = ricfg_getValue("RIPlatform", "RI.Platform.display.safe_area_height_fraction")))
            {
                sscanf (pTemp, "%f", &heightFraction);
            }

            addTVSafeArea (display->pUIInfo->pGraphicsInfo->pixel_data[env], display->pUIInfo->pGraphicsInfo->height,
                display->pUIInfo->pGraphicsInfo->width, display->pUIInfo->pGraphicsInfo->bpl, widthFraction, heightFraction);
        }

        // Copy app's pixel data into display buffer based on environment mode
        memcpy(display->pUIInfo->pGraphicsInfo->paint_pixel_data,
                display->pUIInfo->pGraphicsInfo->pixel_data[env],
                display->pUIInfo->pGraphicsInfo->height
                        * display->pUIInfo->pGraphicsInfo->bpl);

        g_mutex_unlock(display->pUIInfo->flow_lock);

        window_request_repaint(display->pUIInfo);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void addTVSafeArea (unsigned char * pData, int height, int width, int bpl, float widthFraction, float heightFraction)
{
    int leftX = width * widthFraction;
    int rightX = width - leftX;

    int upperY = height * heightFraction;
    int lowerY = height - upperY;

    if (heightFraction != 0.0)
    {
        drawSafeAreaHorizontalLine (pData, upperY, leftX, rightX, bpl);
        drawSafeAreaHorizontalLine (pData, lowerY, leftX, rightX, bpl);
    }

    if (widthFraction != 0.0)
    {
        drawSafeAreaVerticalLine (pData, leftX, upperY, lowerY, bpl);
        drawSafeAreaVerticalLine (pData, rightX, upperY, lowerY, bpl);
    }
}

void drawSafeAreaHorizontalLine (unsigned char * pData, int y, int x1, int x2, int bytesPerLine)
{
    int bytesPerPixel = 4;
    unsigned char pLinePixel[] = {0, 0, 255, 255};
    int nNumPixels = x2 - x1 + 1;
    int i=0;

    for (i=0; i<nNumPixels; i++)
    {
        memcpy (pData + (y * bytesPerLine + (x1+i) * bytesPerPixel), pLinePixel, bytesPerPixel);
    }
}


void drawSafeAreaVerticalLine (unsigned char * pData, int x, int y1, int y2, int bytesPerLine)
{
    int bytesPerPixel = 4;
    unsigned char pLinePixel[] = {0, 0, 255, 255};
    int nNumPixels = y2 - y1 + 1;
    int i=0;

    for (i=0; i<nNumPixels; i++)
    {
        memcpy (pData + ((y1 + i) * bytesPerLine + x * bytesPerPixel), pLinePixel, bytesPerPixel);
    }
}


/**
 * Get the display context.
 *
 * @param display object which has reference to display plugin
 *
 * @return a pointer to UIInfo is returned
 */
void* display_get_context(ri_display_t* object)
{
    void* retVal = NULL; // assume failure

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if ((NULL != object) && (NULL != object->data) && (NULL
            != object->data->gst_videosink))
    {
        GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
        if (NULL != display)
        {
            //         return display->pUIInfo;
            retVal = display->pUIInfo;
        }
        else
        {
            RILOG_WARN("%s -- returned display was NULL\n", __FUNCTION__);
        }
    }
    else
    {
        RILOG_WARN("%s -- unable to get context\n", __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    //   return NULL;
    return retVal;
}

/**
 * Initiates an render operation of the graphics buffer.
 *
 * @param object display object which has reference to display plugin
 * @param window_id the window identifier
 */
void display_set_window_id(ri_display_t* object, uint32_t window_id)
{
    RILOG_TRACE("%s -- Entry, window id = %d\n", __FUNCTION__, window_id);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        // Give the display plugin the window ID to use
        gst_display_set_window_id((GstXOverlay*) display, window_id);

        // Re-calculate the aspect ratio factors based on the new window size,
        // using existing video parameters
        gst_display_update_configuration(display,
                display->pUIInfo->pVideoInfo->output_width,
                display->pUIInfo->pVideoInfo->output_height,
                display->pUIInfo->pVideoInfo->output_par_n,
                display->pUIInfo->pVideoInfo->output_par_d);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets the display's decoder format conversion mode to the supplied value.
 *
 * @param object display object which has reference to display plugin
 */
void set_dfc_mode(ri_display_t* object, int32_t dfc)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        g_object_set(G_OBJECT(display), "dfc", dfc, NULL);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets the display's decoder format conversion default to the supplied value.
 *
 * @param object display object which has reference to display plugin
 */
void set_dfc_default(ri_display_t* object, int32_t dfc)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        g_object_set(G_OBJECT(display), "dfc_default", dfc, NULL);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets the display's background color to the supplied value
 *
 * @param object display object which has reference to display plugin
 */
void set_bg_color(ri_display_t* object, uint32_t color)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        // Get current color and see if it has changed
        uint32_t curColor;
        g_object_get(G_OBJECT(display), "background-screen-color", &curColor,
                NULL);
        if (curColor != color)
        {
            g_object_set(G_OBJECT(display), "background-screen-color", color,
                    NULL);

            window_request_repaint(display->pUIInfo);
        }
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets the display to black and shows no video if supplied boolean is TRUE.
 * Resumes video presentation if currently blocked and supplied boolean
 * value is FALSE.
 *
 * @param object display object which has reference to display plugin
 * @param block   disable video presentation if TRUE, re-enables video
 *                presentation if FALSE
 */
void block_presentation(ri_display_t* object, ri_bool block)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        gst_display_video_block(display, block);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Disables update of video frames and leaves current image displayed
 * if video frames are currently updating.
 *
 * @param object display object which has reference to display plugin
 */
void freeze_video(ri_display_t* object)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        gst_display_video_freeze(display);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Re-enables update of frozen video.
 *
 * @param object display object which has reference to display plugin
 */
void resume_video(ri_display_t* object)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        gst_display_video_resume(display);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Retrieves the width and height in pixels of incoming video prior to
 * any scaling or positioning.
 *
 * @param   object   display object which has reference to display plugin
 * @param   width    pointer to incoming video width value
 * @param   height   pointer to incoming video height value
 */
void get_incoming_video_size(ri_display_t* object, int32_t* width,
        int32_t* height)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        gst_display_wait_for_tune_completion();

        g_object_get(G_OBJECT(display), "incoming-video-width", width, NULL);
        g_object_get(G_OBJECT(display), "incoming-video-height", height, NULL);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Retrieves the incoming video aspect ratio which is calculated from
 * incoming video size and pixel aspect ratio.
 *
 * @param object display object which has reference to display plugin
 * @param ar   pointer to calculated aspect ratio
 */
void get_incoming_video_aspect_ratio(ri_display_t* object, int32_t* ar)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    int32_t width = 0, height = 0, par_x = 0, par_y = 0;

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        g_object_get(G_OBJECT(display), "incoming-video-width", &width, NULL);
        g_object_get(G_OBJECT(display), "incoming-video-height", &height, NULL);
        g_object_get(G_OBJECT(display), "incoming-video-par-x", &par_x, NULL);
        g_object_get(G_OBJECT(display), "incoming-video-par-y", &par_y, NULL);
    }

    // Calculate the aspect ratio, making sure no divison by zero
    gfloat calcAR = 0.0;
    if ((0 != par_y) && (0 != height))
    {
        calcAR = (float) (width * par_x) / (float) (height * par_y);
    }

    // Check for match enumeration to return, need to be insync with values
    // defined in mpeos_media.h
    //
    // MPE_ASPECT_RATIO_UNKNOWN = -1,
    // MPE_ASPECT_RATIO_4_3 = 2,
    // MPE_ASPECT_RATIO_16_9, = 3
    // MPE_ASPECT_RATIO_2_21_1 = 4

    // Initialize to unknown
    *ar = -1;
    float ar4_3 = 4.0f / 3.0f;
    float ar16_9 = 16.0f / 9.0f;
    float ar221_1 = 221.0f / 100.0f;
    if (ar4_3 == calcAR)
    {
        // aspect ration is 4:3
        *ar = 2;
    }
    else if (ar16_9 == calcAR)
    {
        *ar = 3;
    }
    else if (ar221_1 == calcAR)
    {
        *ar = 4;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Retrieves the current Active Format Description (AFD) code which is currently
 * present in the MPEG stream
 *
 * @param object display object which has reference to display plugin
 * @param ar   pointer to current AFD code
 */
void get_video_afd(ri_display_t* object, uint32_t* afd)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        g_object_get(G_OBJECT(display), "incoming-video-afd", afd, NULL);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets the video scaling to the supplied values where the source
 * and destination rectangles are x, y, width and height float values
 * in a normalized display coordinates where (0,0) is upper left and
 * (1,1) represents lower right.  The destination rectangle describes
 * the area on the display where video is to be rendered.  The source
 * rectangle is the area of the framebuffer that should be rendered to the
 * display.  Imagine cutting a piece out of the image and then
 * shrinking/stretching it to an arbitrary window.
 *
 * @param object  display object which has reference to display plugin
 * @param src     area of the framebuffer that should be rendered to display
 * @param dest    area on display where video is to be rendered
 */
void set_bounds(ri_display_t* object, ri_rect* src, ri_rect* dest)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        // Get the lock
        g_mutex_lock(display->pUIInfo->flow_lock);

        g_object_set(G_OBJECT(display), "scaled-video-src-x", src->x, NULL);
        g_object_set(G_OBJECT(display), "scaled-video-src-y", src->y, NULL);
        g_object_set(G_OBJECT(display), "scaled-video-src-width", src->width,
                NULL);
        g_object_set(G_OBJECT(display), "scaled-video-src-height", src->height,
                NULL);

        g_object_set(G_OBJECT(display), "scaled-video-dest-x", dest->x, NULL);
        g_object_set(G_OBJECT(display), "scaled-video-dest-y", dest->y, NULL);
        g_object_set(G_OBJECT(display), "scaled-video-dest-width", dest->width,
                NULL);
        g_object_set(G_OBJECT(display), "scaled-video-dest-height",
                dest->height, NULL);

        // Release the lock
        g_mutex_unlock(display->pUIInfo->flow_lock);

        // Update the video aspect ratio due to new scaling parameters
        gst_display_video_aspect_ratio_adjust(display);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Gets the video scaling values where the source
 * and destination rectangles are x, y, width and height float values
 * in a normalized display coordinates where (0,0) is upper left and
 * (1,1) represents lower right.  The destination rectangle describes
 * the area on the display where video is to be rendered.  The source
 * rectangle is the area of the framebuffer that should be rendered to the
 * display.  Imagine cutting a piece out of the image and then
 * shrinking/stretching it to an arbitrary window.
 *
 * @param object  display object which has reference to display plugin
 * @param src     area of the framebuffer that should be rendered to display
 * @param dest    area on display where video is to be rendered
 */
void get_bounds(ri_display_t* object, ri_rect* src, ri_rect* dest)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        g_object_get(G_OBJECT(display), "scaled-video-src-x", &src->x, NULL);
        g_object_get(G_OBJECT(display), "scaled-video-src-y", &src->y, NULL);
        g_object_get(G_OBJECT(display), "scaled-video-src-width", &src->width,
                NULL);
        g_object_get(G_OBJECT(display), "scaled-video-src-height",
                &src->height, NULL);

        g_object_get(G_OBJECT(display), "scaled-video-dest-x", &dest->x, NULL);
        g_object_get(G_OBJECT(display), "scaled-video-dest-y", &dest->y, NULL);
        g_object_get(G_OBJECT(display), "scaled-video-dest-width",
                &dest->width, NULL);
        g_object_get(G_OBJECT(display), "scaled-video-dest-height",
                &dest->height, NULL);
    }

    RILOG_INFO("%s - returning src - x %f, y %f, width %f, height %f\n",
            __FUNCTION__, src->x, src->y, src->width, src->height);

    RILOG_INFO("%s - returning dest - x %f, y %f, width %f, height %f\n",
            __FUNCTION__, dest->x, dest->y, dest->width, dest->height);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Validates that the desired source and destination video playback bounds can be
 * supported.  If the specified bounds are directly supportable, then the function
 * will return without error and the input desired bounds specifications will match
 * the actual bounds output parameters.  If the bounds are not supportable, an error
 * will be returned and the closest approximation bounds will be returned via the
 * output parameters.
 *
 * @param videoDevice is target decoder video device.
 * @param desiredSrc is a pointer to the desired source dimension.
 * @param desiredDst is a pointer to the desired destination dimension.
 * @param actualSrc is a pointer for returning the actual source dimension.
 * @param actualDst is a pointer for returning the actual destination dimension.
 *
 * @return MPE_SUCCESS if the desired bounds dimensions are supportable, or a usable
 *         alternative dimension has been returned.
 */
void check_bounds(ri_display_t* object, ri_rect* desiredSrc,
        ri_rect* desiredDest, ri_rect* actualSrc, ri_rect* actualDest)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    //The only scaling limitation is that src or dst height or width must be greater than .005
    //if desired src or dst height or width are less than or equal to .005, return the current gstreamer src/dst
    float minWidth = 0.005, minHeight = 0.005;
    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display && (desiredSrc->width < minWidth || desiredSrc->height
            < minHeight || desiredDest->width < minWidth || desiredDest->height
            < minHeight))
    {
        g_object_get(G_OBJECT(display), "scaled-video-src-x", &actualSrc->x,
                NULL);
        g_object_get(G_OBJECT(display), "scaled-video-src-y", &actualSrc->y,
                NULL);
        g_object_get(G_OBJECT(display), "scaled-video-src-width",
                &actualSrc->width, NULL);
        g_object_get(G_OBJECT(display), "scaled-video-src-height",
                &actualSrc->height, NULL);

        g_object_get(G_OBJECT(display), "scaled-video-dest-x", &actualDest->x,
                NULL);
        g_object_get(G_OBJECT(display), "scaled-video-dest-y", &actualDest->y,
                NULL);
        g_object_get(G_OBJECT(display), "scaled-video-dest-width",
                &actualDest->width, NULL);
        g_object_get(G_OBJECT(display), "scaled-video-dest-height",
                &actualDest->height, NULL);
    }
    else
    {
        actualSrc->x = desiredSrc->x;
        actualSrc->y = desiredSrc->y;
        actualSrc->width = desiredSrc->width;
        actualSrc->height = desiredSrc->height;

        actualDest->x = desiredDest->x;
        actualDest->y = desiredDest->y;
        actualDest->width = desiredDest->width;
        actualDest->height = desiredDest->height;
    }

    RILOG_INFO("%s - returning src - x %f, y %f, width %f, height %f\n",
            __FUNCTION__, actualSrc->x, actualSrc->y, actualSrc->width,
            actualSrc->height);
    RILOG_INFO("%s - returning dest - x %f, y %f, width %f, height %f\n",
            __FUNCTION__, actualDest->x, actualDest->y, actualDest->width,
            actualDest->height);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Acquire the scaling capabilities of the specified decoder.
 *
 * @param positioning is a pointer for returning the positioning capabilities
 * @param horiz is a pointer for returning a pointer to an array of floats
 *        representing either the arbitrary range or discrete horizontal
 *        scaling factors supported (array is terminated with a value of (-1.0))
 * @param vert is a pointer for returning a pointer to an array of floats
 *        representing either the arbitrary range or discrete vertical
 *        scaling factors supported (array is terminated with a value of (-1.0))
 * @param hRange is a pointer for returning a boolean value indicating
 *        whether the horizontal values represent an arbitrary range.
 * @param vRange is a pointer for returning a boolean value indicating
 *        whether the vertical values represent an arbitrary range.
 * @param canClip is a pointer for returning a boolean value indicating
 *        whether the specified decoder can support clipping.
 * @param supportsComponent is a pointer for returning a boolean value indicating
 *        whether the specified decoder supports component based scaling.
 *
 * @return MPE_EINVAL if an invalid display device is specified, or
 *         MPE_ERROR_MEDIA_OS if the target decoder does not support
 *         scaling at all.
 */
void get_scaling(ri_display_t* object, int32_t* positioning, float** horiz,
        float** vert, ri_bool* hRange, ri_bool* vRange, ri_bool* canClip,
        ri_bool* supportsComponent)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        // Positioning capability is 0 = MPE_POS_CAP_FULL
        // which indicates the video can be positioned anywhere on
        // the display, even if a part of the video is off display.
        *positioning = 0;

        // List of horizontal scaling range which is 0.0 to 1.0
        **horiz++ = 0.0;
        **horiz = 1.0;
        *hRange = TRUE;

        // List of vertical scaling range which is 0.0 to 1.0
        **vert++ = 0.0;
        **vert = 1.0;
        *vRange = TRUE;

        // Set flag indicating that clipping is supported
        *canClip = TRUE;

        // Set flag indicating that component based video is supported
        // Might not be supported at stack level, but is support at platform level
        *supportsComponent = TRUE;
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

int32_t get_threedtv_info(ri_display_t* object, int32_t* formatType, int32_t* payloadType,
            uint32_t* payloadSz, uint8_t* payload, int32_t* scanMode)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) object->data->gst_videosink;
    if (NULL != display)
    {
        *scanMode = display->pUIInfo->pVideoInfo->scan_mode;

        *formatType = display->pUIInfo->pVideoInfo->threedtv_format_type;
        *payloadType = display->pUIInfo->pVideoInfo->threedtv_payload_type;

        if (*payloadSz < display->pUIInfo->pVideoInfo->threedtv_payload_sz)
        {
            *payloadSz = display->pUIInfo->pVideoInfo->threedtv_payload_sz;
            RILOG_TRACE("%s -- Exit Error\n", __FUNCTION__);
            return -1;
        }
            
        *payloadSz = display->pUIInfo->pVideoInfo->threedtv_payload_sz;
        memcpy (payload, display->pUIInfo->pVideoInfo->threedtv_payload, *payloadSz);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return 0;
}



/**
 * Allocates memory and initializes the data structure
 * which is used to represent the graphics surface.
 *
 * @param display plugin which will use the allocated graphics surface
 */
static void init_graphics_surface(GstDisplay* display, guint width,
        guint height, guint par_n, guint par_d, ri_env env)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Do these operations here so that the memory is allocated here and
    // not in the display plugin
    g_mutex_lock(display->pUIInfo->flow_lock);

    // Allocate memory for surface info
    if (graphicsInfo == NULL)
    {
        graphicsInfo = g_try_new0(GraphicsInfo, 1);
    }
    display->pUIInfo->pGraphicsInfo = graphicsInfo;

    if (NULL != display->pUIInfo->pGraphicsInfo)
    {
        display->pUIInfo->pGraphicsInfo->width = width;
        display->pUIInfo->pGraphicsInfo->height = height;

        display->pUIInfo->pGraphicsInfo->par_n = par_n;
        display->pUIInfo->pGraphicsInfo->par_d = par_d;

        // *TODO* - Do we know if this is really 32???
        display->pUIInfo->pGraphicsInfo->bpp = 32;

        display->pUIInfo->pGraphicsInfo->bpl
                = (display->pUIInfo->pGraphicsInfo->bpp >> 3)
                        * display->pUIInfo->pGraphicsInfo->width; // bits/pixel divided by 8 bits/byte * width

        if (display->pUIInfo->pGraphicsInfo->pixel_data[env] == NULL)
        {
            // set buffer to highest resolution, even if we are using lower resolution
            // leave buffer allocated, even when resolution changes
            display->pUIInfo->pGraphicsInfo->pixel_data[env] = g_try_malloc0(
                    MAX_GRAPHICS_BUFFER_SZ);

            if (NULL == display->pUIInfo->pGraphicsInfo->pixel_data[env])
            {
                RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                            __LINE__, __FILE__, __func__);
            }

            RILOG_DEBUG(
                    "%s: allocating graphics buffer: %u\n",
                    __FUNCTION__,
                    (unsigned int) display->pUIInfo->pGraphicsInfo->pixel_data[env]);
        }

        display->pUIInfo->pGraphicsInfo->paint_pixel_data = g_try_malloc0(
                display->pUIInfo->pGraphicsInfo->height
                        * display->pUIInfo->pGraphicsInfo->bpl);

        if (NULL == display->pUIInfo->pGraphicsInfo->paint_pixel_data)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        // Don't need to re-calculate aspect ratio here since it will be called in update_configuration
    }
    else
    {
        RILOG_ERROR("line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    g_mutex_unlock(display->pUIInfo->flow_lock);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Routine which sets properties in display plugin to various values.
 */
void test_set_display_properties(GstElement* videosink)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // display SIZE TESTS
    // ------------------------------------------------------------
    // Mismatch SAR
    //g_object_set(G_OBJECT(videosink), "window-width", 800, NULL);
    //g_object_set(G_OBJECT(videosink), "window-height", 800, NULL);

    // SAR 16:9
    //g_object_set(G_OBJECT(videosink), "window-width", 800, NULL);
    //g_object_set(G_OBJECT(videosink), "window-height", 450, NULL);

    // Output video 4:3
    //g_object_set(G_OBJECT(videosink), "output-video-width", 720, NULL);
    //g_object_set(G_OBJECT(videosink), "output-video-height", 480, NULL);
    //g_object_set(G_OBJECT(videosink), "output-video-par-x", 8, NULL);
    //g_object_set(G_OBJECT(videosink), "output-video-par-y", 9, NULL);

    // Output video 16:9
    //g_object_set(G_OBJECT(videosink), "output-video-width", 1920, NULL);
    //g_object_set(G_OBJECT(videosink), "output-video-height", 1080, NULL);
    //g_object_set(G_OBJECT(videosink), "output-video-par-x", 1, NULL);
    //g_object_set(G_OBJECT(videosink), "output-video-par-y", 1, NULL);

    // Scale video test
    //g_object_set(G_OBJECT(videosink), "scaled-video-x", 0.00, NULL);
    //g_object_set(G_OBJECT(videosink), "scaled-video-y", 0.30, NULL);
    //g_object_set(G_OBJECT(videosink), "scaled-video-width", .60, NULL);
    //g_object_set(G_OBJECT(videosink), "scaled-video-height", .60, NULL);

    //g_object_set(G_OBJECT(videosink), "background-display-color", 0x000F00000, NULL);

    //g_object_set(G_OBJECT(videosink), "hw-acceleration-disabled", TRUE, NULL);

    // Borderless Window test
    //g_object_set(G_OBJECT(videosink), "fixed-window", TRUE, NULL);

    // Allow display to create and set window id
    g_object_set(G_OBJECT(videosink), "supplied-window", FALSE, NULL);

    // Misc properties - not really used currently
    //g_object_set(G_OBJECT(videosink), "force-aspect-ratio", FALSE, NULL);

    // Set the DFC for video
    //  -1 = unknown           - *
    //   0 = none              - *
    //   1 = full              - *
    //   2 = letter box 16:9   - 16:9->4:3
    //   3 = letter box 14:9   - 14:9->4:3
    //   4 = center cut out    - 16:9->4:3
    //   5 = pan scan          - 16:9->4:3
    //   6 = letter box 2.21:1 on 16:9  - 2.21:1->16:9
    //   7 = letter box 2.21:1 on 4:3   - 2.21:1->4:3
    //   8 = platform          - *
    //   9 = zoom              - 4:3->16:9
    // 100 = pillar            - 4:3->16:9
    // 101 = wide              - 4:3->16:9
    g_object_set(G_OBJECT(videosink), "dfc", 0, NULL);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

static GstDisplay* snapshot_display = NULL;

static void set_snapshot_display(GstDisplay *riDisplay)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    snapshot_display = riDisplay;
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void take_snapshot(void)
{
    RILOG_TRACE("%s -- Entry;\n", __func__);

    if ((snapshot_display) && (snapshot_display->pUIInfo))
    {
        snapshot_display->pUIInfo->take_snapshot = TRUE;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets the display as blocked and therefore doesn't update any more.
 * Mimics disabled and unconnected displays.
 *
 * Using snapshot_display as easy handle.
 */
void block_display(ri_display_t* object, ri_bool block)
{
    RILOG_TRACE("%s -- Entry;\n", __func__);

    if ((snapshot_display) && (snapshot_display->pUIInfo))
    {
        snapshot_display->pUIInfo->block_display = block;
        gst_display_video_block(snapshot_display, block);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Routine which is periodically called to draw something to display
 */
gboolean test(gpointer object)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    //test_draw_something((ri_display_t*)object);

    //test_draw_square((ri_display_t*)object);

    //test_draw_square_with_border((ri_display_t*)object);

    //test_supplying_window((ri_display_t*)object);

    //test_freeze_resume((ri_display_t*)object);

    //test_block_unblock((ri_display_t*)object);

    //int32_t width, height;
    //get_incoming_video_size((ri_display_t*)object, &width, &height);

    //int32_t ar;
    //get_incoming_video_aspect_ratio((ri_display_t*)object, &ar);

    //set_bg_color((ri_display_t*)object, 0x000F00000);

    //test_video_scaling((ri_display_t*)object);

    test_reset((ri_display_t*) object);

    //draw_graphics_buffer((ri_display_t*)object);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return TRUE;
}

/**
 * Test routine which draws a square to the display
 */
void test_draw_square(ri_display_t* riDisplay)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) riDisplay->data->gst_videosink;

    int i_pitch = display->pUIInfo->pGraphicsInfo->bpl;
    int x;
    int y;
    unsigned char* pucData = display->pUIInfo->pGraphicsInfo->paint_pixel_data;
    int bytes_per_pixel = display->pUIInfo->pGraphicsInfo->bpp >> 3; // bits/pixel divided by 8 bits/byte

    // Makes square when PAR is 1/1
    int rect1Width = 200;
    int rect1Height = 200;

    if ((3 == display->pUIInfo->pGraphicsInfo->par_n) && (4
            == display->pUIInfo->pGraphicsInfo->par_d))
    {
        // Makes a square when PAR is 3/4
        rect1Width = 200;
        rect1Height = 150;
    }
    if ((4 == display->pUIInfo->pGraphicsInfo->par_n) && (3
            == display->pUIInfo->pGraphicsInfo->par_d))
    {
        // Makes a square when PAR is 4/3
        rect1Width = 150;
        rect1Height = 200;
    }

    // Calculate the center of the graphics plane
    int centerX = display->pUIInfo->pGraphicsInfo->width / 2;
    int centerY = display->pUIInfo->pGraphicsInfo->height / 2;

    int rect1Xpos = centerX - (rect1Width / 2);
    int rect1Ypos = centerY - (rect1Height / 2);

    for (x = rect1Xpos; x < rect1Width + rect1Xpos; x++)
    {
        for (y = rect1Ypos; y < rect1Height + rect1Ypos; y++)
        {
            test_drawPixel(&pucData[(x * bytes_per_pixel) + (y * i_pitch)],
                    255, 0, 0, 255);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Test routine which draws a square in the center of the display
 * along with a 3 pixel border along the outside.
 */
void test_draw_square_with_border(ri_display_t* riDisplay)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) riDisplay->data->gst_videosink;

    int i_pitch = display->pUIInfo->pGraphicsInfo->bpl;
    int x;
    int y;
    unsigned char* pucData = display->pUIInfo->pGraphicsInfo->paint_pixel_data;
    int bytes_per_pixel = display->pUIInfo->pGraphicsInfo->bpp >> 3; // bits/pixel divided by 8 bits/byte

    // Makes square when PAR is 1/1
    int rect1Width = 200;
    int rect1Height = 200;

    if ((3 == display->pUIInfo->pGraphicsInfo->par_n) && (4
            == display->pUIInfo->pGraphicsInfo->par_d))
    {
        // Makes a square when PAR is 3/4
        rect1Width = 200;
        rect1Height = 150;
    }
    if ((4 == display->pUIInfo->pGraphicsInfo->par_n) && (3
            == display->pUIInfo->pGraphicsInfo->par_d))
    {
        // Makes a square when PAR is 4/3
        rect1Width = 150;
        rect1Height = 200;
    }

    // Calculate the center of the graphics plane
    int centerX = display->pUIInfo->pGraphicsInfo->width / 2;
    int centerY = display->pUIInfo->pGraphicsInfo->height / 2;

    int rect1Xpos = centerX - (rect1Width / 2);
    int rect1Ypos = centerY - (rect1Height / 2);

    for (x = rect1Xpos; x < rect1Width + rect1Xpos; x++)
    {
        for (y = rect1Ypos; y < rect1Height + rect1Ypos; y++)
        {
            test_drawPixel(&pucData[(x * bytes_per_pixel) + (y * i_pitch)],
                    255, 0, 0, 255);
        }
    }
    // draw top and bottom lines
    for (x = 0; x < (int) display->pUIInfo->pGraphicsInfo->width; x++)
    {
        // Top line
        test_drawPixel(&pucData[(x * bytes_per_pixel) + (0 * i_pitch)], 255, 0,
                0, 255);
        test_drawPixel(&pucData[(x * bytes_per_pixel) + (1 * i_pitch)], 255, 0,
                0, 255);
        test_drawPixel(&pucData[(x * bytes_per_pixel) + (2 * i_pitch)], 255, 0,
                0, 255);

        // Bottom line
        test_drawPixel(&pucData[(x * bytes_per_pixel)
                + ((display->pUIInfo->pGraphicsInfo->height - 3) * i_pitch)],
                255, 0, 0, 255);
        test_drawPixel(&pucData[(x * bytes_per_pixel)
                + ((display->pUIInfo->pGraphicsInfo->height - 2) * i_pitch)],
                255, 0, 0, 255);
        test_drawPixel(&pucData[(x * bytes_per_pixel)
                + ((display->pUIInfo->pGraphicsInfo->height - 1) * i_pitch)],
                255, 0, 0, 255);
    }

    // draw left and right lines
    for (y = 0; y < (int) display->pUIInfo->pGraphicsInfo->height; y++)
    {
        // Left side line
        test_drawPixel(&pucData[(0 * bytes_per_pixel) + (y * i_pitch)], 255, 0,
                0, 255);
        test_drawPixel(&pucData[(1 * bytes_per_pixel) + (y * i_pitch)], 255, 0,
                0, 255);
        test_drawPixel(&pucData[(2 * bytes_per_pixel) + (y * i_pitch)], 255, 0,
                0, 255);

        // Right side line
        test_drawPixel(&pucData[((display->pUIInfo->pGraphicsInfo->width - 3)
                * bytes_per_pixel) + (y * i_pitch)], 255, 0, 0, 255);
        test_drawPixel(&pucData[((display->pUIInfo->pGraphicsInfo->width - 2)
                * bytes_per_pixel) + (y * i_pitch)], 255, 0, 0, 255);
        test_drawPixel(&pucData[((display->pUIInfo->pGraphicsInfo->width - 1)
                * bytes_per_pixel) + (y * i_pitch)], 255, 0, 0, 255);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Original test method which draws three rectangles of varying opacity
 * to display
 */
void test_draw_something(ri_display_t* riDisplay)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) riDisplay->data->gst_videosink;

    int i_pitch = display->pUIInfo->pGraphicsInfo->bpl;
    int x;
    int y;
    unsigned char* pucData = display->pUIInfo->pGraphicsInfo->pixel_data[0]; // 0 for RI Stack , 1 for Mfg Stack
    int bytes_per_pixel = display->pUIInfo->pGraphicsInfo->bpp >> 3; // bits/pixel divided by 8 bits/byte

    //display->pGraphicsInfo = g_value_get_pointer(value);

    // *****
    // draw red rectangle, full opacity
    int rect1Xpos = 0;
    int rect1Ypos = 0;
    int rect1Width = 100;
    int rect1Height = 200;
    for (x = rect1Xpos; x < rect1Width + rect1Xpos; x++)
    {
        for (y = rect1Ypos; y < rect1Height + rect1Ypos; y++)
        {
            test_drawPixel(&pucData[(x * bytes_per_pixel) + (y * i_pitch)],
                    255, 0, 0, 255);
        }
    }
    // draw green rectangle, half opacity
    int rect2Xpos = 50;
    int rect2Ypos = 50;
    int rect2Width = 100;
    int rect2Height = 200;
    for (x = rect2Xpos; x < rect2Width + rect2Xpos; x++)
    {
        for (y = rect2Ypos; y < rect2Height + rect2Ypos; y++)
        {
            test_drawPixel(&pucData[(x * bytes_per_pixel) + (y * i_pitch)], 0,
                    255, 0, 128);
        }
    }

    // draw blue rectangle, quarter opacity
    int rect3Xpos = 100;
    int rect3Ypos = 100;
    int rect3Width = 100;
    int rect3Height = 200;
    for (x = rect3Xpos; x < rect3Width + rect3Xpos; x++)
    {
        for (y = rect3Ypos; y < rect3Height + rect3Ypos; y++)
        {
            test_drawPixel(&pucData[(x * bytes_per_pixel) + (y * i_pitch)], 0,
                    0, 255, 64);
        }
    }

    // draw top and bottom lines
    for (x = 0; x < (int) display->pUIInfo->pGraphicsInfo->width; x++)
    {
        // Top line
        test_drawPixel(&pucData[(x * bytes_per_pixel) + (0 * i_pitch)], 0, 255,
                255, 255);
        test_drawPixel(&pucData[(x * bytes_per_pixel) + (1 * i_pitch)], 0, 255,
                255, 255);

        // Bottom line
        // *TODO* - figure out how to adjust this, works find with no title, hoarked with title
        test_drawPixel(&pucData[(x * bytes_per_pixel)
                + ((display->pUIInfo->pGraphicsInfo->height - 31) * i_pitch)],
                0, 255, 255, 255);
        test_drawPixel(&pucData[(x * bytes_per_pixel)
                + ((display->pUIInfo->pGraphicsInfo->height - 2) * i_pitch)],
                0, 255, 255, 255);
        test_drawPixel(&pucData[(x * bytes_per_pixel)
                + ((display->pUIInfo->pGraphicsInfo->height - 1) * i_pitch)],
                0, 255, 255, 255);
    }

    // draw left and right lines
    for (y = 0; y < (int) display->pUIInfo->pGraphicsInfo->height; y++)
    {
        // Left side line
        test_drawPixel(&pucData[(0 * bytes_per_pixel) + (y * i_pitch)], 0, 255,
                255, 255);
        test_drawPixel(&pucData[(1 * bytes_per_pixel) + (y * i_pitch)], 0, 255,
                255, 255);

        // Right side line
        // *TODO* - not seeing the last line
        test_drawPixel(&pucData[((display->pUIInfo->pGraphicsInfo->width - 21)
                * bytes_per_pixel) + (y * i_pitch)], 0, 255, 255, 255);
        test_drawPixel(&pucData[((display->pUIInfo->pGraphicsInfo->width - 2)
                * bytes_per_pixel) + (y * i_pitch)], 0, 255, 255, 255);
        test_drawPixel(&pucData[((display->pUIInfo->pGraphicsInfo->width - 1)
                * bytes_per_pixel) + (y * i_pitch)], 0, 255, 255, 255);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Test routine which sets a pixel values in buffer to supplied values
 */
static inline void test_drawPixel(unsigned char* pGraphics, unsigned char red,
        unsigned char green, unsigned char blue, unsigned char alpha)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    *pGraphics++ = red;
    *pGraphics++ = green;
    *pGraphics++ = blue;
    *pGraphics = alpha;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Test which calls method to freeze or resume video
 */
void test_freeze_resume(ri_display_t* riDisplay)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) riDisplay->data->gst_videosink;

    if (FALSE == display->pUIInfo->pVideoInfo->is_frozen)
    {
        freeze_video(riDisplay);
    }
    else
    {
        resume_video(riDisplay);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Test which calls method to block and unblock video presentation.
 */
void test_block_unblock(ri_display_t* riDisplay)
{
    GstDisplay* display = (GstDisplay*) riDisplay->data->gst_videosink;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    RILOG_DEBUG("%s -- Current blocked state: %d\n", __FUNCTION__,
            display->pUIInfo->pVideoInfo->is_blocked);

    if (FALSE == display->pUIInfo->pVideoInfo->is_blocked)
    {
        block_presentation(riDisplay, TRUE);
    }
    else
    {
        block_presentation(riDisplay, FALSE);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Test which calls methods to clip and scale video.
 */
void test_video_scaling(ri_display_t* riDisplay)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    ri_rect* src = g_try_new0(ri_rect, 1);
    ri_rect* dest = g_try_new0(ri_rect, 1);

    if (NULL == src || NULL == dest)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Scale to qtr display in upper right with no clipping
    /*
     src->x = 0.0;
     src->y = 0.0;
     src->width = 1.0;
     src->height = 1.0;

     dest->x = 0.5;
     dest->y = 0.5;
     dest->width = 0.5;
     dest->height = 0.5;
     */

    // Clip quarter display with no scaling
    /*
     src->x = 0.5;
     src->y = 0.5;
     src->width = 0.5;
     src->height = 0.5;

     dest->x = 0.0;
     dest->y = 0.0;
     dest->width = 1.0;
     dest->height = 1.0;
     */

    // Clip quarter display and scale to quarter display
    src->x = 0.0;
    src->y = 0.0;
    src->width = 0.5;
    src->height = 0.5;

    dest->x = 0.5;
    dest->y = 0.0;
    dest->width = 0.5;
    dest->height = 0.5;

    set_bounds(riDisplay, src, dest);

    g_free(src);
    g_free(dest);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Test which calls method to block and unblock video presentation.
 */
void test_reset(ri_display_t* riDisplay)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstDisplay* display = (GstDisplay*) riDisplay->data->gst_videosink;
    gst_display_reset(display);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

