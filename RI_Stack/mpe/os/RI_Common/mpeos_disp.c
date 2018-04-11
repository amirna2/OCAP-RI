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

/*
 * @file mpeos_disp.c
 *
 * This modules contains platform dependent implementation of the MPEOS
 * display configuration APIs for the CableLabs Reference Implementation.
 * These APIs allow for discovery and configuration of the display including
 * (potential) support for multiple screens, multiple devices (background, graphics,
 * and video), and multiple device configurations.
 *
 * The number of screens, devices, and configurations supported are
 * discovered at runtime by parsing a static data structure.
 *
 * @note For API documentation please see <mpeos_disp.h>
 */
#include <mpeos_disp.h>
#include <mpeos_snd.h>
#include <mpeos_dbg.h>
#include <mpeos_mem.h>
#include <mpeos_gfx.h>
#include <mpeos_screen.h>
#include <mpeos_util.h>
#include <mpeos_media.h>
#include <mpe_error.h>

#include <mpeos_event.h>

#include <platform.h>
#include <ri_ui_manager.h>
#include "platform_gfxdfb.h"
#include "directfb/rip_display.h"
#include <ri_display.h>
#include <ri_pipeline_manager.h>
#include <ri_video_device.h>

#include <stdlib.h>
#include <string.h>
#include <ri_test_interface.h>

#include <os_disp.h>

#define SHARED_VIDEO_BACKGROUND_DECODER 0   /* decoder is restricted to one use at a time if true */
#define UNUSED(x) (void)x

//Need the event queue and ACT (Asynchronous Completion Token) to post connect/disconnect events.
static mpe_EventQueue gDeviceSettingsDisplayQueueId;
static mpe_Bool gDeviceSettingsDisplayQueueIdRegistered = FALSE;
static void* gDeviceSettingsACT = NULL;
//NOTE: The MPE_DISP_EVENT... codes MUST match definitions in org.cablelabs.impl.manager.host.DeviceSettingsHostImpl
#define MPE_DISP_EVENT_CONNECTED 0x2500
#define MPE_DISP_EVENT_DISCONNECTED 0x2501

/**
 * Describes a native configuration.
 */
typedef struct os_Config
{
    int valid;
    mpe_DispDeviceConfigInfo info;
} os_Config;

typedef struct os_Config* os_CoherentConfig;

/**
 * Describes a bg image.
 */
typedef struct os_BgImage
{
    mpe_GfxDimensions size; /**< size */
    void* handle; /**< unused */
    unsigned int length;
    void* data;
} os_BgImage;

/**
 * Describes a native device.
 */
typedef struct os_Device
{
    int valid;
    mpe_DispDeviceInfo info;

    os_Config *configs;
    os_Config *curr;
    mpe_GfxColor color; /**< Only used for BG device to store color */
    mpe_DispDeviceDest dest;

    ri_video_device_t* riVideoDevice; // only used for video devices

} os_Device;

/**
 * Describes a native screen.
 */
typedef struct os_Screen
{
    int valid;
    os_Device *bg;
    os_Device *gfx;
    os_Device *vid;

    os_CoherentConfig **cc;

    ri_display_t* riDisplay;

} os_Screen;

/**
 * The supported video display modes.
 */
typedef enum
{
    VDM_ASPECTRATIO_UNKNOWN = -1,

    /** Display in standard def, or 4x3 ratio. */
    VDM_ASPECTRATIO_4_3 = 2,

    /** Display in high def, or 16x9 ratio. */
    VDM_ASPECTRATIO_16_9
} VIDEO_DISPLAY_MODE;

/*
 * Map all logging messages as follows.
 * We don't necessarily want them always on.
 */
#define DISP_LOG_DEBUG MPE_LOG_TRACE1

#define INITIAL_RF_CHANNEL 3
/* boolean value that indicates whether RF bypass is enabled or disabled */
static mpe_Bool gRFBypassEnabled = FALSE;
/* channel number that the A/V signal is RF-modulated to. */
static uint32_t gRFChannelNumber = INITIAL_RF_CHANNEL;

// Fake implementation of DFC processing, this really should be set
// on a per decoder / per video device basis
static mpe_DispDfcAction g_applicationDfc = MPE_DFC_PLATFORM;
static mpe_DispDfcAction g_platformDfc = MPE_DFC_PROCESSING_NONE;
static mpe_DispDfcAction g_defaultPlatformDfc = MPE_DFC_PROCESSING_UNKNOWN;
static int32_t g_supportedDfcs[] =
{ MPE_DFC_PROCESSING_NONE, MPE_DFC_PROCESSING_FULL, MPE_DFC_PROCESSING_LB_16_9,
        MPE_DFC_PROCESSING_LB_14_9, MPE_DFC_PROCESSING_CCO,
        MPE_DFC_PROCESSING_PAN_SCAN, MPE_DFC_PROCESSING_LB_2_21_1_ON_4_3,
        MPE_DFC_PROCESSING_LB_2_21_1_ON_16_9, MPE_DFC_PLATFORM,
        MPE_DFC_PROCESSING_16_9_ZOOM, MPE_DFC_PROCESSING_PILLARBOX_4_3,
        MPE_DFC_PROCESSING_WIDE_4_3 };

static mpe_Bool setScreenAspect(int width, int height);

static void updateSDPorts(ri_backpanel_t* bp, mpe_Bool enable);

static void populateVideoOutputConfigInfo();


static mpe_Error gfxGetScreenSurface(mpe_GfxSurface *surface);
static mpe_Error gfxScreenFlush(void);
static os_CoherentConfig * determineInitialCoherentConfigForScreen(
        os_Screen * targetScreen);
void postDisplayConnectDisconnectEvent(ri_bool connected, void* video_output_port_handle);
static mpe_Error refreshDisplay (os_Screen *pScreen, int doDisplayInit);

/* Predeclare data structure instances to allow back-references. */
/* Each list is null-terminated - so size is deviceCount+1 */
/*lint -esym(31, gGfxDev, gVidDev, gBgDev, gScreens)*/
static os_Device gGfxDev[2];
static os_Device gVidDev[2];
static os_Device gBgDev[2];
static os_Screen gScreens[2];

/**
 * Configurations supported by Graphics[0].
 */
static os_Config gGfxCfg0[] =
{
/* 640x480 on a 4:3 screen */
{ 1, /* valid */
{ (mpe_DispDevice) & gGfxDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 640, 480 }, /* resolution */
{ 1, 1 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
FALSE, /* NA */
FALSE, /* NA */
{ 4, 3 } /* screen aspect ratio */
} },
/* 960x540 on a 4:3 screen */
{ 1, /* valid */
{ (mpe_DispDevice) & gGfxDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 960, 540 }, /* resolution */
{ 3, 4 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
FALSE, /* NA */
FALSE, /* NA */
{ 4, 3 } /* screen aspect ratio */
} },
/* 640x480 on a 16:9 screen */
{ 1, /* valid */
{ (mpe_DispDevice) & gGfxDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 640, 480 }, /* resolution */
{ 4, 3 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
FALSE, /* NA */
FALSE, /* NA */
{ 16, 9 } /* screen aspect ratio */
} },
/* 960x540 on a 16:9 screen */
{ 1, /* valid */
{ (mpe_DispDevice) & gGfxDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 960, 540 }, /* resolution */
{ 1, 1 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
FALSE, /* NA */
FALSE, /* NA */
{ 16, 9 } /* screen aspect ratio */
} },
{ 0 } };
/**
 * Graphics devices.
 */
static os_Device gGfxDev[] =
{
{ 1, /* valid */
{ MPE_DISPLAY_GRAPHICS_DEVICE, "Graphics[0]", (mpe_DispScreen) & gScreens[0],
{ 4, 3 } }, gGfxCfg0, &gGfxCfg0[0] },
{ 0 } };

/**
 * Configurations supported by Video[0].
 */
static os_Config gVidCfg0[] =
{
/* 720x480 on a 4:3 screen */
{ 1, /* valid */
{ (mpe_DispDevice) & gVidDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 720, 480 }, /* resolution */
{ 8, 9 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
FALSE, /* NA */
FALSE, /* NA */
{ 4, 3 }, /* screen aspect ratio */
} },
/* 1920x1080 on a 16:9 screen */
{ 1, /* valid */
{ (mpe_DispDevice) & gVidDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 1920, 1080 }, /* resolution */
{ 1, 1 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
FALSE, /* NA */
FALSE, /* NA */
{ 16, 9 }, /* screen aspect ratio */
} },
{ 0 } };

/* Non-contributing video */
#define NONCONTRIB_CFG (gVidCfg0[(sizeof(gVidCfg0)/sizeof(os_Config))-2])

/**
 * Video devices.
 */
static os_Device gVidDev[] =
{
{ 1, /* valid */
{ MPE_DISPLAY_VIDEO_DEVICE, "Video[0]", (mpe_DispScreen) & gScreens[0],
{ 4, 3 } }, gVidCfg0, &gVidCfg0[0], 0, MPE_DISPLAY_DEST_TV, NULL },
{ 0 } };

/**
 * Configurations supported by Background[0]
 */
static os_Config gBgCfg0[] =
{
/* 640x480 on a 4:3 screen with I-frame support */
{ 1, /* valid */
{ (mpe_DispDevice) & gBgDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 640, 480 }, /* resolution */
{ 1, 1 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
TRUE, /* supports MPEG I-frame */
TRUE, /* supports changeable color */
{ 4, 3 }, /* screen aspect ratio */
} },
/* 720x480 on a 4:3 screen with I-frame support */
{ 1, /* valid */
{ (mpe_DispDevice) & gBgDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 720, 480 }, /* resolution */
{ 8, 9 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
TRUE, /* supports MPEG I-frame */
TRUE, /* supports changeable color */
{ 4, 3 }, /* screen aspect ratio */
} },
/* 1920x1080 on a 16:9 screen with I-frame support */
{ 1, /* valid */
{ (mpe_DispDevice) & gBgDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 1920, 1080 }, /* resolution */
{ 1, 1 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
TRUE, /* supports MPEG I-frame */
TRUE, /* supports changeable color */
{ 16, 9 }, /* screen aspect ratio */
} },
/* 1920x1080 on a 16:9 screen without I-frame support */
{ 1, /* valid */
{ (mpe_DispDevice) & gBgDev[0], FALSE, /* flicker filter supported */
FALSE, /* interlaced */
{ 1920, 1080 }, /* resolution */
{ 1, 1 }, /* pixel aspect ratio */
{ 0, 0, 1, 1 }, /* screen area */
FALSE, /* supports MPEG I-frame */
TRUE, /* supports changeable color */
{ 16, 9 }, /* screen aspect ratio */
} },
{ 0 } };

/**
 * Background devices.
 */
static os_Device gBgDev[] =
{
{ 1, /* valid */
{ MPE_DISPLAY_BACKGROUND_DEVICE, "Background[0]", (mpe_DispScreen)
        & gScreens[0],
{ 4, 3 } }, gBgCfg0, &gBgCfg0[0], mpe_gfxRgbToColor(0, 0, 0) },
{ 0 } };

/**
 * Coherent Configurations
 */
static os_CoherentConfig gCoherent1a[] =
{
/* SD 4:3 */
&gGfxCfg0[0], /* 640x480 1:1 graphics */
&gVidCfg0[0], /* 720x480 8:9 video */
&gBgCfg0[0], /* 640x480 1:1 background */
NULL };
static os_CoherentConfig gCoherent1b[] =
{
/* SD 4:3 */
&gGfxCfg0[0], /* 640x480 1:1 graphics */
&gVidCfg0[0], /* 720x480 8:9 video */
&gBgCfg0[1], /* 720x480 8:9 background */
NULL };
static os_CoherentConfig gCoherent2a[] =
{
/* SD 4:3 */
&gGfxCfg0[1], /* 960x540 3:4 graphics */
&gVidCfg0[0], /* 720x480 8:9 video */
&gBgCfg0[0], /* 640x480 1:1 background */
NULL };
static os_CoherentConfig gCoherent2b[] =
{
/* SD 4:3 */
&gGfxCfg0[1], /* 960x540 3:4 graphics */
&gVidCfg0[0], /* 720x480 8:9 video */
&gBgCfg0[1], /* 720x480 8:9 background */
NULL };
static os_CoherentConfig gCoherent3[] =
{
/* HD 16:9 */
&gGfxCfg0[2], /* 640x480 4:3 graphics */
&gVidCfg0[1], /* 1920x1080 1:1 video */
#if SHARED_VIDEO_BACKGROUND_DECODER
        &gBgCfg0[3], /* 1920x1080 1:1 background (without I-frame support) */
#else
        &gBgCfg0[2], /* 1920x1080 1:1 background (with I-frame support) */
#endif
        NULL };
static os_CoherentConfig gCoherent4[] =
{
/* HD 16:9 */
&gGfxCfg0[3], /* 960x540 1:1 graphics */
&gVidCfg0[1], /* 1920x1080 1:1 video */
#if SHARED_VIDEO_BACKGROUND_DECODER
        &gBgCfg0[3], /* 1920x1080 1:1 background (without I-frame support) */
#else
        &gBgCfg0[2], /* 1920x1080 1:1 background (with I-frame support) */
#endif
        NULL };
#if SHARED_VIDEO_BACKGROUND_DECODER
static os_CoherentConfig gCoherent6[] =
{
    /* HD 16:9 */
    &gGfxCfg0[2], /* 640x480 4:3 graphics */
    &gVidCfg0[1], /* 1920x1080 1:1 video */
    &gBgCfg0[2], /* 1920x1080 1:1 background (with I-frame support) */
    NULL
};
static os_CoherentConfig gCoherent8[] =
{
    /* HD 16:9 */
    &gGfxCfg0[3], /* 960x540 1:1 graphics */
    &gVidCfg0[1], /* 1920x1080 1:1 video */
    &gBgCfg0[2], /* 1920x1080 1:1 background (with I-frame support) */
    NULL
};
#endif

/**
 * Coherent configurations supported by screen[0].
 */
static os_CoherentConfig *gCoherent[] =
{ gCoherent1a, gCoherent1b, gCoherent2a, gCoherent2b, gCoherent3, gCoherent4,
#if SHARED_VIDEO_BACKGROUND_DECODER
        gCoherent6,
        gCoherent8,
#endif
        NULL };

/**
 * Supported screens.
 */
static os_Screen gScreens[] =
{
{ 1, /* valid */
gBgDev, gGfxDev, gVidDev, gCoherent, NULL },
{ 0 } };



/* DSExt */
static mpe_DispVideoResolutionInfo gVideoResolutionInfoSD_SS2D =
{
{ 720, 480 },
{ 4, 3 }, 60, TRUE, MPE_SSMODE_2D};

static mpe_DispVideoResolutionInfo gVideoResolutionInfoSD_SS3DSS =
{
{ 720, 480 },
{ 4, 3 }, 60, TRUE, MPE_SSMODE_3D_SIDE_BY_SIDE};

static mpe_DispVideoResolutionInfo gVideoResolutionInfoSD_SS3DTB =
{
{ 720, 480 },
{ 4, 3 }, 60, TRUE, MPE_SSMODE_3D_TOP_AND_BOTTOM};


static mpe_DispVideoResolutionInfo gVideoResolutionInfoHD_SS2D =
{
{ 1920, 1080 },
{ 16, 9 }, 24, FALSE, MPE_SSMODE_2D};

static mpe_DispVideoResolutionInfo gVideoResolutionInfoHD_SS3DSS =
{
{ 1920, 1080 },
{ 16, 9 }, 24, FALSE, MPE_SSMODE_3D_SIDE_BY_SIDE};

static mpe_DispVideoResolutionInfo gVideoResolutionInfoHD_SS3DTB =
{
{ 1920, 1080 },
{ 16, 9 }, 24, FALSE, MPE_SSMODE_3D_TOP_AND_BOTTOM};


static mpe_DispFixedVideoOutputConfigInfo gFixedConfigArray[] =
{
{ TRUE, "Fixed_ConfigSD_SS2D", &gVideoResolutionInfoSD_SS2D },
{ TRUE, "Fixed_ConfigSD_SS3DSS", &gVideoResolutionInfoSD_SS3DSS },
{ TRUE, "Fixed_ConfigSD_SS3DTB", &gVideoResolutionInfoSD_SS3DTB },
{ TRUE, "Fixed_ConfigHD_SS2D", &gVideoResolutionInfoHD_SS2D },
{ TRUE, "Fixed_ConfigHD_SS3DSS", &gVideoResolutionInfoHD_SS3DSS },
{ TRUE, "Fixed_ConfigHD_SS3DTB", &gVideoResolutionInfoHD_SS3DTB },

};
#define N_FIXED_CONFIGS (sizeof(gFixedConfigArray) / sizeof(mpe_DispFixedVideoOutputConfigInfo))

//Dynamic Video Configurations

static mpe_DispDynamicVideoOutputMapping gDynamicVideoConfigurationMappings[] =
{
{ &gVideoResolutionInfoSD_SS2D, &gFixedConfigArray[0] },
{ &gVideoResolutionInfoHD_SS2D, &gFixedConfigArray[3] }, };

static mpe_DispDynamicVideoOutputConfigInfo gDynamicConfigArray[] =
{
{ TRUE, "Dynamic", gDynamicVideoConfigurationMappings,
        sizeof(gDynamicVideoConfigurationMappings)
                / sizeof(mpe_DispDynamicVideoOutputMapping) }, };
#define N_DYNAMIC_CONFIGS (sizeof(gDynamicConfigArray) / sizeof(mpe_DispDynamicVideoOutputConfigInfo))

// NOTE: Be sure that the default current config in the struct below has
// stereoscopic mode MPE_SSMODE_2D.
static mpe_DispVideoOutputConfigInfo gVideoOutputConfigInfo =
{ gFixedConfigArray, gDynamicConfigArray, (mpe_DispVideoConfig)
        & gFixedConfigArray[0], };

static int gFixedOutputConfigCount = 0;

/* end DSExt */

//TODO:HOST_FEATURE_STUB - IEEE1394 devices are not emulated, just faked.
//Warning - This is a stub for an opencable host hardware feature that is not implemented in the RI PC platform.
//Ports of the RI to actual host implementations need to replace this stub with functioning code
//Need a fake IEEE1394 device
static mpe_Disp1394DeviceInfo gIEEE1394deviceInfo =
{
{ 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 }, /* eui64[8] */
"CableLabs", /* vendor[128] */
"Fake1394", /* model[128] */
0 /* subunitType; */
};

//previously unimplemented, copied from simulator
/* DSExt */
/* See API doc (mpeos_disp.h) */
// These are vars represent properties of the attached display 
static mpe_DispVideoDisplayAttrInfo *g_pDisplayAttrInfo = NULL;
static mpe_GfxDimensions *g_pDisplaySupportedResolutions = NULL;
static int g_DisplayNumSupportedResolutions = 0;

#ifdef MPEOS_LOG_ENABLED
static void dumpConfig(int level, os_Config* pConfig);
#else
#define dumpConfig(a,b);
#endif // MPEOS_LOG_ENABLED

#define MPEOS_TESTS \
    "\r\n" \
    "|---+---------------------------------\r\n" \
    "| 1 | set current-display props\r\n" \
    "|---+---------------------------------\r\n" \
    "| 2 | send display connected event\r\n" \
    "|---+---------------------------------\r\n" \
    "| 3 | send display disconnected event\r\n" \


// These are vars are temporary and represent properties of the attached display that are used by the testing interface.  
// They are used when display-connected or display-disconnected events are initiated by the test intertace.
// For a display-connected event, the test-interface user first fills out these temp vars, then the temp vars are swapped
// into main vars right before the event is sent.
// For a display-disconnected event, the contents of the main vars will be copied into these temp vars right before the
// event is sent.
static mpe_GfxDimensions *g_pTestInterfaceTempSupportedResolutions = NULL;
static int g_TestInterfaceNumSupportedResolutions = 0;
static mpe_DispVideoDisplayAttrInfo *g_pTestInterfaceTempDisplayAttrInfo = NULL;

static int testInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    char buf[1024];

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    if (strstr(rxBuf, "1"))
    {
        char *manufacturerName = NULL;
        uint8_t manufactureWeek;
        uint8_t manufactureYear;
        uint16_t productCode;
        uint32_t serialNumber;
        int32_t aspectRatioWidth;
        int32_t aspectRatioHeight;
        int i;

        int numBytes = ri_test_GetString(sock, buf, sizeof(buf), "\r\nmanufacturerName: ");
        manufacturerName = (char *)malloc(numBytes + 1);
        strcpy(manufacturerName, buf);

        manufactureWeek = ri_test_GetNumber(sock, buf, sizeof(buf), "\r\nmanufactureWeek: ", 0);
        manufactureYear = ri_test_GetNumber(sock, buf, sizeof(buf), "\r\nmanufactureYear: ", 0);
        productCode = ri_test_GetNumber(sock, buf, sizeof(buf), "\r\nproductCode: ", 0);
        serialNumber = ri_test_GetNumber(sock, buf, sizeof(buf), "\r\nserialNumber: ", 0);
        aspectRatioWidth = ri_test_GetNumber(sock, buf, sizeof(buf), "\r\naspectRatioWidth: ", 0);
        aspectRatioHeight = ri_test_GetNumber(sock, buf, sizeof(buf), "\r\naspectRatioHeight: ", 0);

        if (g_pTestInterfaceTempDisplayAttrInfo != NULL)
        {
            if (g_pTestInterfaceTempDisplayAttrInfo->manufacturerName != NULL)
            {
                free ((char *)g_pTestInterfaceTempDisplayAttrInfo->manufacturerName);
            }

            free (g_pTestInterfaceTempDisplayAttrInfo);
        }

        g_pTestInterfaceTempDisplayAttrInfo = (mpe_DispVideoDisplayAttrInfo *) malloc (sizeof(mpe_DispVideoDisplayAttrInfo));

        g_pTestInterfaceTempDisplayAttrInfo->manufactureWeek = manufactureWeek;
        g_pTestInterfaceTempDisplayAttrInfo->manufactureYear = manufactureYear;
        g_pTestInterfaceTempDisplayAttrInfo->productCode = productCode;
        g_pTestInterfaceTempDisplayAttrInfo->serialNumber = serialNumber;
        g_pTestInterfaceTempDisplayAttrInfo->manufacturerName = manufacturerName;
        g_pTestInterfaceTempDisplayAttrInfo->aspectRatio.width = aspectRatioWidth;
        g_pTestInterfaceTempDisplayAttrInfo->aspectRatio.height = aspectRatioHeight;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): manufactureWeek = %d\n", g_pTestInterfaceTempDisplayAttrInfo->manufactureWeek);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): manufactureYear = %d\n", g_pTestInterfaceTempDisplayAttrInfo->manufactureYear);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): productCode = %d\n", g_pTestInterfaceTempDisplayAttrInfo->productCode);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): serialNumber = %d\n", g_pTestInterfaceTempDisplayAttrInfo->serialNumber);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): manufacturerName = %s\n", g_pTestInterfaceTempDisplayAttrInfo->manufacturerName);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): aspectRatio.width = %d\n", g_pTestInterfaceTempDisplayAttrInfo->aspectRatio.width);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): aspectRatio.height = %d\n", g_pTestInterfaceTempDisplayAttrInfo->aspectRatio.height);


        g_TestInterfaceNumSupportedResolutions = ri_test_GetNumber(sock, buf, sizeof(buf), "\r\nNumber of supported resolutions: ", 0);
        if (g_pTestInterfaceTempSupportedResolutions != NULL)
        {
            free (g_pTestInterfaceTempSupportedResolutions);
        }

        if (g_TestInterfaceNumSupportedResolutions == 0)
        {
            g_pTestInterfaceTempSupportedResolutions = NULL;
        }
        else
        {
            g_pTestInterfaceTempSupportedResolutions = (mpe_GfxDimensions *) malloc (g_TestInterfaceNumSupportedResolutions * sizeof(mpe_GfxDimensions));
        }


        for (i=0; i<g_TestInterfaceNumSupportedResolutions; i++)
        {
            char temp[100];
            sprintf(temp, "\r\nWidth of supported resolution %d: ", i);
            g_pTestInterfaceTempSupportedResolutions[i].width = ri_test_GetNumber(sock, buf, sizeof(buf), temp, 0);
            sprintf(temp, "\r\nHeight of supported resolution %d: ", i);
            g_pTestInterfaceTempSupportedResolutions[i].height = ri_test_GetNumber(sock, buf, sizeof(buf), temp, 0);
        }

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): NumSupportedResolutions = %d\n", g_TestInterfaceNumSupportedResolutions);
        for (i=0; i<g_TestInterfaceNumSupportedResolutions; i++)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(1): supportedResolution %d: (%d, %d)", i, g_pTestInterfaceTempSupportedResolutions[i].width,
                g_pTestInterfaceTempSupportedResolutions[i].height);
        }
    }
    else if (strstr(rxBuf, "2"))  // display connected
    {
        ri_test_GetString(sock, buf, sizeof(buf), "\r\nvideoOutputPortName: ");

        ri_backpanel_t* bp = ri_get_backpanel();
        void *video_output_port_handle = bp->getVideoOutputPortHandle(buf);

        if (video_output_port_handle != NULL)
        {
            mpe_Bool connectedFlag = true;

            if (g_pDisplayAttrInfo != NULL)
            {
                if (g_pDisplayAttrInfo->manufacturerName != NULL)
                {
                    free ((char *)g_pDisplayAttrInfo->manufacturerName);
                }

                free (g_pDisplayAttrInfo);
            }

            g_pDisplayAttrInfo = g_pTestInterfaceTempDisplayAttrInfo;
            g_pTestInterfaceTempDisplayAttrInfo = NULL;

            
            if (g_pDisplaySupportedResolutions != NULL)
            {
                free (g_pDisplaySupportedResolutions);
            }

            g_pDisplaySupportedResolutions = g_pTestInterfaceTempSupportedResolutions;
            g_DisplayNumSupportedResolutions = g_TestInterfaceNumSupportedResolutions;
            g_pTestInterfaceTempSupportedResolutions = NULL;
            g_TestInterfaceNumSupportedResolutions = 0;

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(2): video_output_port_handle = %x\n", (unsigned int)video_output_port_handle);
            
            // update gVideoOutputConfigInfo and change connected flag 
            bp->setVideoOutputPortValue(video_output_port_handle, VIDEO_OUTPUT_CONNECTED, &connectedFlag);
            populateVideoOutputConfigInfo();


            postDisplayConnectDisconnectEvent(TRUE, video_output_port_handle);
        }
        else
        {
            ri_test_SendString(sock, "\r\n\nINVALID VIDEO OUTPUT PORT NAME!\r\n");
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(2): video_output_port_handle NOT FOUND\n");
            *retCode = MENU_FAILURE;
        }

    }
    else if (strstr(rxBuf, "3")) // display disconnected
    {
        ri_test_GetString(sock, buf, sizeof(buf), "\r\nvideoOutputPortName: ");

        ri_backpanel_t* bp = ri_get_backpanel();
        void *video_output_port_handle = bp->getVideoOutputPortHandle(buf);

        if (video_output_port_handle != NULL)
        {
            mpe_Bool connectedFlag = false;

            if (g_pTestInterfaceTempDisplayAttrInfo != NULL)
            {
                if (g_pTestInterfaceTempDisplayAttrInfo->manufacturerName != NULL)
                {
                    free ((char *)g_pTestInterfaceTempDisplayAttrInfo->manufacturerName);
                }

                free (g_pTestInterfaceTempDisplayAttrInfo);
            }

            g_pTestInterfaceTempDisplayAttrInfo = g_pDisplayAttrInfo;
            g_pDisplayAttrInfo = NULL;


            if (g_pTestInterfaceTempSupportedResolutions != NULL)
            {
                free (g_pTestInterfaceTempSupportedResolutions);
            }

            g_pTestInterfaceTempSupportedResolutions = g_pDisplaySupportedResolutions;
            g_TestInterfaceNumSupportedResolutions = g_DisplayNumSupportedResolutions;
            g_pDisplaySupportedResolutions = NULL;
            g_DisplayNumSupportedResolutions = 0;

            // update  gVideoOutputConfigInfo and change connected flag
            bp->setVideoOutputPortValue(video_output_port_handle, VIDEO_OUTPUT_CONNECTED, &connectedFlag);
            populateVideoOutputConfigInfo();


            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(3): video_output_port_handle = %x\n", (unsigned int)video_output_port_handle);

            postDisplayConnectDisconnectEvent(FALSE, video_output_port_handle);
        }
        else
        {
            ri_test_SendString(sock, "\r\n\nINVALID VIDEO OUTPUT PORT NAME!\r\n");
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "TestInterface(3): video_output_port_handle NOT FOUND\n");
            *retCode = MENU_FAILURE;
        }
    }
    else
    {
        strcat(rxBuf, " - unrecognized\r\n\n");
        ri_test_SendString(sock, rxBuf);
        *retCode = MENU_INVALID;
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP, "%s - Exit 0\n", __FUNCTION__);
    return 0;
}

static MenuItem MpeosMenuItem =
{ false, "d", "MPEOS Display", MPEOS_TESTS, testInputHandler };

/**
 * Get the video destination device.
 *
 * Used internally for MPEOS layer calls.
 *
 * @param disp  device for which destination is desired.
 * @return MPE_DISPLAY_DEST_TV if dest is main window.
 * @return MPE_DISPLAY_DEST_VIDEO if dest is PIP window.
 */
mpe_DispDeviceDest dispGetDestDevice(mpe_DispDevice disp)
{
    os_Device* pDevice = NULL;

    if (disp != NULL)
    {
        pDevice = (os_Device*) disp;
        return pDevice->dest;
    }

    return -1;
}

/**
 * Determines if the two supplied display devices are equal by
 * comparing the id strings, if id strings are equal, they are
 * assumed to be equal.  If strings are not equal, they are assumed
 * to be not equal.
 *
 * @param   disp1    first display device
 * @param   disp2    second display device to see if it is equal to disp1
 *
 * @return  true if two display devices are equal, false otherwise
 */
mpe_Bool dispDeviceEquals(mpe_DispDevice disp1, mpe_DispDevice disp2)
{
    mpe_Bool equals = FALSE;

    os_Device* pDevice1 = (os_Device*) disp1;
    os_Device* pDevice2 = (os_Device*) disp2;
    mpe_DispDeviceInfo info1 = pDevice1->info;
    mpe_DispDeviceInfo info2 = pDevice2->info;

    // These devices are equal if the id strings are the same
    if (strcmp(info1.idString, info2.idString) == 0)
    {
        equals = TRUE;
    }

    return equals;
}

/**
 * Get the video destination device.
 *
 * Used internally for MPEOS layer calls.
 *
 * @param disp  device for which destination is desired.
 * @return MPE_DISPLAY_DEST_TV if dest is main window.
 * @return MPE_DISPLAY_DEST_VIDEO if dest is PIP window.
 */
ri_video_device_t* dispGetVideoDevice(mpe_DispDevice disp)
{
    os_Device* pDevice = NULL;

    if (disp != NULL)
    {
        pDevice = (os_Device*) disp;
        return pDevice->riVideoDevice;
    }

    return NULL;
}

/**
 * Initializes the display and graphics subsystem.
 *
 */
mpe_Error mpeos_dispInit()
{
    os_Screen * targetScreen = &gScreens[0];
    os_CoherentConfig * pInitialCC = determineInitialCoherentConfigForScreen(
            &gScreens[0]);
    os_Config **ppCurConfig = pInitialCC;
    os_Device * pCurDevice;
    int i;
    mpe_Error result = MPE_SUCCESS;

    ri_test_RegisterMenu(&MpeosMenuItem);

    // Set the current config on all the devices accd to the
    //  selected coherent configuration
    for (i = 0; ppCurConfig[i] != NULL; i++)
    {
        pCurDevice = (os_Device *) (ppCurConfig[i]->info.device);

        switch (pCurDevice->info.type)
        {
        case MPE_DISPLAY_GRAPHICS_DEVICE:
            targetScreen->gfx->curr = ppCurConfig[i];
            break;
        case MPE_DISPLAY_VIDEO_DEVICE:
            targetScreen->vid->curr = ppCurConfig[i];
            break;
        case MPE_DISPLAY_BACKGROUND_DEVICE:
            targetScreen->bg->curr = ppCurConfig[i];
            break;
        default:
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                    "mpeos_dispInit: Invalid current device type (%d)\n",
                    pCurDevice->info.type);
            break;
        }
    } // END for loop

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP, "DISP: Initial device config:"
        "(gfx %dx%d %d:%d,vid %dx%d %d:%d,bg %dx%d %d:%d)\n",
            targetScreen->gfx->curr->info.resolution.width,
            targetScreen->gfx->curr->info.resolution.height,
            targetScreen->gfx->curr->info.screenAspectRatio.width,
            targetScreen->gfx->curr->info.screenAspectRatio.height,
            targetScreen->vid->curr->info.resolution.width,
            targetScreen->vid->curr->info.resolution.height,
            targetScreen->vid->curr->info.screenAspectRatio.width,
            targetScreen->vid->curr->info.screenAspectRatio.height,
            targetScreen->bg->curr->info.resolution.width,
            targetScreen->bg->curr->info.resolution.height,
            targetScreen->bg->curr->info.screenAspectRatio.width,
            targetScreen->bg->curr->info.screenAspectRatio.height);

    // Make calls to rip_display to setup display to match selected config
    (void) rip_InitDisplay(targetScreen->gfx->curr->info.resolution.width,
            targetScreen->gfx->curr->info.resolution.height,
            targetScreen->gfx->curr->info.pixelAspectRatio.width,
            targetScreen->gfx->curr->info.pixelAspectRatio.height,
            targetScreen->vid->curr->info.resolution.width,
            targetScreen->vid->curr->info.resolution.height,
            targetScreen->vid->curr->info.pixelAspectRatio.width,
            targetScreen->vid->curr->info.pixelAspectRatio.height,
            targetScreen->bg->curr->info.resolution.width,
            targetScreen->bg->curr->info.resolution.height,
            targetScreen->bg->curr->info.pixelAspectRatio.width,
            targetScreen->bg->curr->info.pixelAspectRatio.height);

    result = dfb_gfxCreatePrimarySurface(
            targetScreen->gfx->curr->info.resolution);

    (void) mpeos_dispSetCoherentConfig_Helper((mpe_DispScreen) targetScreen,
            (mpe_DispCoherentConfig) pInitialCC, 0);

    // Map the RI components to MPEOS level
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        gScreens[0].riDisplay = pMgr->get_display(pMgr);
        if (NULL != gScreens[0].riDisplay)
        {
            gScreens[0].vid->riVideoDevice
                    = gScreens[0].riDisplay->get_video_device(
                            gScreens[0].riDisplay);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                    "mpeos_dispInit: unable to get RI display\n");
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                "mpeos_dispInit: unable to get RI pipeline manager\n");
    }

    g_pDisplayAttrInfo = (mpe_DispVideoDisplayAttrInfo *) malloc (sizeof (mpe_DispVideoDisplayAttrInfo));
    g_pDisplayAttrInfo->aspectRatio.width = 16;
    g_pDisplayAttrInfo->aspectRatio.height = 9;
    g_pDisplayAttrInfo->manufacturerName = (char *) malloc (100);
    strcpy((char *)g_pDisplayAttrInfo->manufacturerName, "Cablelabs");
    g_pDisplayAttrInfo->manufactureWeek = 10;
    g_pDisplayAttrInfo->manufactureYear = 8;
    g_pDisplayAttrInfo->productCode = 12;
    g_pDisplayAttrInfo->serialNumber = 1;

    g_pDisplaySupportedResolutions = NULL;
    g_DisplayNumSupportedResolutions = 0;


    // for testing different display resolutions on startup
/*    g_DisplayNumSupportedResolutions = 1;
    g_pDisplaySupportedResolutions = (mpe_GfxDimensions *) malloc (g_DisplayNumSupportedResolutions * sizeof(mpe_GfxDimensions));

    for (i=0; i<g_DisplayNumSupportedResolutions; i++)
    {
        g_pDisplaySupportedResolutions[i].width = 1920;
        g_pDisplaySupportedResolutions[i].height = 1080;
    }
    

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "dispInit: NumSupportedResolutions = %d\n", g_DisplayNumSupportedResolutions);
    for (i=0; i<g_DisplayNumSupportedResolutions; i++)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "dispInit: supportedResolution %d: (%d, %d)", i, g_pDisplaySupportedResolutions[i].width,
            g_pDisplaySupportedResolutions[i].height);
    }
    */

    populateVideoOutputConfigInfo();

    return result;
} // END mpeos_dispInit()

/**
 * determine the initial coherent configuration to be used.
 *
 * <p>
 * Currently, a couple env variables factor into this function:
 * <ul>
 * <li>DISP.DEFAULT.CONFIG - which designates the index of
 *     the initial coherent config</li>
 * <li>DISP.DEFAULT.GFXCONFIG - which selects an initial
 *     coherent config based on graphics properties</li>
 * </ul>
 */
static os_CoherentConfig * determineInitialCoherentConfigForScreen(
        os_Screen * targetScreen)
{
    const char* env;
    long int coherentConfigIndex = -1;
    char * configSettingString = "DISP.DEFAULT.CONFIG";
    unsigned long int formatSettingIndex = 0;
    char * formatSettingString = "DISP.DEFAULT.GFXCONFIG";
    os_CoherentConfig ** ppCurCC;
    os_Config **ppCurConfig;
    os_Device * pCurDevice;
    int i;
    int numCCs = sizeof(gCoherent) / sizeof(os_CoherentConfig*);
    struct gfxformat
    {
        int width;
        int height;
        int xaspect;
        int yaspect;
    } gfxformat[] =
    {
    { 640, 480, 4, 3 },
    { 960, 540, 4, 3 },
    { 640, 480, 16, 9 },
    { 960, 540, 16, 9 } };

    // NOTE: configSettingString explicitly selects the coherent config
    //       and overrides the formatSettingString - which is fuzzier
    //       and can match more than one config

    if ((env = mpeos_envGet(configSettingString)) != NULL)
    {
        coherentConfigIndex = atol(env);

        if ((coherentConfigIndex >= numCCs)
                || (gScreens[0].cc[coherentConfigIndex] == NULL))
        {
            MPEOS_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_DISP,
                    "DISP: Coherent config %ld specified via %s is not a valid mode.\n",
                    coherentConfigIndex, configSettingString);

            coherentConfigIndex = -1;
        }
        else
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP,
                    "DISP: Selecting coherent config %ld via %s\n",
                    coherentConfigIndex, configSettingString);
        }
    } // END if (configSettingString set in env)

    //
    // If explicit CC wasn't set, try the gfx mode variable
    //
    if (coherentConfigIndex < 0)
    {
        coherentConfigIndex = 0;

        if ((env = mpeos_envGet(formatSettingString)) != NULL)
        {
            formatSettingIndex = atol(env);
            // Valid range of the index is 1..n
            if ((formatSettingIndex <= 0) || (formatSettingIndex
                    > (sizeof(gfxformat) / sizeof(struct gfxformat))))
            {
                MPEOS_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_DISP,
                        "DISP: Graphics format %s=%lu is not a valid format index.  Selecting default coherent config 0.\n",
                        formatSettingString, (formatSettingIndex + 1));
                // We'll stick with coherentConfigIndex = 0
            }
            else
            {
                formatSettingIndex--; // Adjust for 0-based index
                // Try to find a coherent config that satisfies the graphics format

                ppCurCC = targetScreen->cc;
                i = 0;

                while (*ppCurCC != NULL) // Walk the coherent configs
                {
                    ppCurConfig = *ppCurCC;

                    while ((*ppCurConfig != NULL) && (coherentConfigIndex == 0))
                    {
                        pCurDevice
                                = (os_Device *) ((*ppCurConfig)->info.device);
                        if ((pCurDevice->info.type
                                == MPE_DISPLAY_GRAPHICS_DEVICE)
                                && ((*ppCurConfig)->info.resolution.width
                                        == gfxformat[formatSettingIndex].width)
                                && ((*ppCurConfig)->info.resolution.height
                                        == gfxformat[formatSettingIndex].height)
                                && ((*ppCurConfig)->info.screenAspectRatio.width
                                        == gfxformat[formatSettingIndex].xaspect)
                                && ((*ppCurConfig)->info.screenAspectRatio.height
                                        == gfxformat[formatSettingIndex].yaspect))
                        {
                            coherentConfigIndex = i;
                            break;
                        }
                        ppCurConfig++; // Next config in the coherent config
                    } // END while (config loop)
                    ppCurCC++;
                    i++;
                } // END while (coherent config loop)

                MPEOS_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_DISP,
                        "DISP: Selecting coherent config %ld via %s=%lu (%dx%d %d:%d)\n",
                        coherentConfigIndex, formatSettingString,
                        (formatSettingIndex + 1),
                        gfxformat[formatSettingIndex].width,
                        gfxformat[formatSettingIndex].height,
                        gfxformat[formatSettingIndex].xaspect,
                        gfxformat[formatSettingIndex].yaspect);
            } // END else/if (formatStringIndex not valid)
        }
        else
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP,
                    "DISP: %s not specified. Selecting coherent config %ld.\n",
                    formatSettingString, coherentConfigIndex);
        }

    } // END if (coherentConfigIndex < 0)
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP,
                "DISP: Selecting default coherent config %ld.\n",
                coherentConfigIndex);
    }

    return targetScreen->cc[coherentConfigIndex];
} // END determineInitialCoherentConfig()

/**
 * Tests for a valid screen.
 * @return <i>screen</i> cast as an <code>os_Screen*</code>
 * if valid, <code>NULL</code> otherwise.
 */
static os_Screen* isValidScreen(mpe_DispScreen screen)
{
    os_Screen *pScreen = (os_Screen*) screen;

    int i;
    for (i = 0; gScreens[i].valid; ++i)
    {
        if (pScreen == &gScreens[i])
            return &gScreens[i];
    }
    return NULL;
}

/**
 * Counts the devices pointed to by <i>devs</i>.
 */
static int countDevices(os_Device *devs)
{
    int n;
    for (n = 0; devs->valid; ++n, ++devs)
        ;
    return n;
}

/**
 * Copies the devices pointed to by <i>src</i> to <i>dest</i>.
 * @return location past the last written location in <i>dest</i>;
 * <i>dest</i> if no devices were written
 */
static os_Device** copyDevices(os_Device *src, os_Device **dest)
{
    while (src->valid)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "DEVICE: %p\n", src);
        *dest++ = src++;
    }
    return dest;
}

/**
 * Tests for a valid config.
 * @return <i>config</i> cast as an <code>os_Config*</code>
 * if valid, <code>NULL</code> otherwise.
 */
static os_Config* isValidConfig(mpe_DispDeviceConfig config, os_Config *configs)
{
    os_Config *pConfig = (os_Config*) config;

    for (; configs->valid; ++configs)
    {
        if (pConfig == configs)
            return configs;
    }
    return NULL;
}

/**
 * Tests for a valid config.
 * @return <i>config</i> cast as an <code>os_Config*</code>
 * if valid, <code>NULL</code> otherwise.
 */
static os_Config* isValidConfig2(mpe_DispDeviceConfig config,
		os_CoherentConfig *configs)
{
    os_Config *pConfig = (os_Config*)config;

    for (; *configs; ++configs)
    {
        if (pConfig == *configs)
        {
            return pConfig;
        }
    }

    return NULL;
}

/**
 * Tests for a valid coherent config.
 * @return <i>config</i> cast as an <code>os_CoherentConfig*</code>
 * if valid, <code>NULL</code> otherwise.
 */
static os_CoherentConfig* isValidCoherentConfig(mpe_DispCoherentConfig config,
        os_CoherentConfig **cc)
{
    os_CoherentConfig *pConfig = (os_CoherentConfig*) config;

    if (cc != NULL)
    {
        for (; *cc != NULL; ++cc)
        {
            if (pConfig == *cc)
                return pConfig;
        }
    }
    return NULL;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetScreenCount(uint32_t *nScreens)
{
    int n;
    for (n = 0; gScreens[n].valid; ++n)
        ;
    *nScreens = n;
    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetScreens(mpe_DispScreen* screens)
{
    int i;
    for (i = 0; gScreens[i].valid; ++i)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "SCREEN[%d] = %d\n", i,
                (int) &gScreens[i]);
        *screens++ = (void*) &gScreens[i];
    }
    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetScreenInfo(mpe_DispScreen screen,
        mpe_DispScreenInfo* info)
{
    UNUSED(info);

    if (!isValidScreen(screen))
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
#if SHARED_VIDEO_BACKGROUND_DECODER
    info->notContributing = (mpe_DispDeviceConfig)&NONCONTRIB_CFG;
#else
    info->notContributing = NULL;
#endif

    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetDeviceCount(mpe_DispScreen screen,
        mpe_DispDeviceType type, uint32_t *nDevices)
{
    os_Screen *pScreen = (os_Screen*) screen;

    if (!isValidScreen(screen) || NULL == nDevices)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    switch (type)
    {
    case MPE_DISPLAY_ALL_DEVICES:
        *nDevices = countDevices(pScreen->bg) + countDevices(pScreen->gfx)
                + countDevices(pScreen->vid);
        break;
    case MPE_DISPLAY_GRAPHICS_DEVICE:
        *nDevices = countDevices(pScreen->gfx);
        break;
    case MPE_DISPLAY_VIDEO_DEVICE:
        *nDevices = countDevices(pScreen->vid);
        break;
    case MPE_DISPLAY_BACKGROUND_DEVICE:
        *nDevices = countDevices(pScreen->bg);
        break;
    default:
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetDevices(mpe_DispScreen screen, mpe_DispDeviceType type,
        mpe_DispDevice* devices)
{
    os_Screen *pScreen = (os_Screen*) screen;

    if (!isValidScreen(screen) || NULL == devices)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    switch (type)
    {
    case MPE_DISPLAY_ALL_DEVICES:
        (void) copyDevices(pScreen->bg, copyDevices(pScreen->gfx, copyDevices(
                pScreen->vid, (os_Device**) devices)));
        break;
    case MPE_DISPLAY_GRAPHICS_DEVICE:
        (void) copyDevices(pScreen->gfx, (os_Device**) devices);
        break;
    case MPE_DISPLAY_VIDEO_DEVICE:
        (void) copyDevices(pScreen->vid, (os_Device**) devices);
        break;
    case MPE_DISPLAY_BACKGROUND_DEVICE:
        (void) copyDevices(pScreen->bg, (os_Device**) devices);
        break;
    default:
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetDeviceInfo(mpe_DispDevice device,
        mpe_DispDeviceInfo* info)
{
    if (NULL == device || NULL == info)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Device* pDevice = (os_Device*) device;

        *info = pDevice->info;

        info->screenAspectRatio = pDevice->curr->info.screenAspectRatio;

        return MPE_SUCCESS;
    }
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetDeviceDest(mpe_DispDevice device, mpe_DispDeviceDest* dest)
{
    if (NULL == device || NULL == dest)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        *dest = dispGetDestDevice(device);
        return MPE_SUCCESS;
    }
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetConfigCount(mpe_DispDevice device, uint32_t* nConfigs)
{
    if (NULL == device || NULL == nConfigs)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Device* pDevice = (os_Device*) device;
        int n;

        for (n = 0; pDevice->configs[n].valid; ++n)
            ;
        *nConfigs = n;

        return MPE_SUCCESS;
    }
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetConfigs(mpe_DispDevice device,
        mpe_DispDeviceConfig* configs)
{
    if (NULL == device || NULL == configs)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Device* pDevice = (os_Device*) device;
        int i;

        for (i = 0; pDevice->configs[i].valid; ++i)
        {
            *configs++ = (void*) &pDevice->configs[i];
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "CONFIG[%d][%d] = %d\n",
                    (int) pDevice, i, (int) &pDevice->configs[i]);
        }

        return MPE_SUCCESS;
    }
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetCurrConfig(mpe_DispDevice device,
        mpe_DispDeviceConfig* config)
{
    if (NULL == device || NULL == config)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Device* pDevice = (os_Device*) device;

        *config = (mpe_DispDeviceConfig) pDevice->curr;

        return MPE_SUCCESS;
    }
}

/**
 * Sets true if choosing the given device for the given
 * configuration will require a change to any other device
 * configurations on the same screen.
 */
static mpe_Error wouldImpact(os_Device *dev, os_Config *cfg, mpe_Bool *impact)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP,
                "wouldImpact called: dev = %d, cfg = %d, dev->curr = %d", (unsigned int)dev, 
                (unsigned int)cfg, (unsigned int)dev->curr);

    *impact = TRUE;
    os_Screen *pScreen = (os_Screen*) dev->info.screen;
    os_CoherentConfig **pCC = (os_CoherentConfig**) pScreen->cc;

    os_Device *bg = pScreen->bg;
    os_Device *gfx = pScreen->gfx;
    os_Device *vid = pScreen->vid;

    os_Config *bgConf;
    os_Config *gfxConf;
    os_Config *vidConf;
    int i = 0;
    if (dev == bg)
    {
        bgConf = cfg;
        gfxConf = gfx->curr;
        vidConf = vid->curr;
    }
    else if (dev == gfx)
    {
        bgConf = bg->curr;
        gfxConf = cfg;
        vidConf = vid->curr;
    }
    else if (dev == vid)
    {
        bgConf = bg->curr;
        gfxConf = gfx->curr;
        vidConf = cfg;
    }
    else
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    for (i = 0; pCC[i] != NULL; i++)
    {
        if (isValidConfig2((mpe_DispDeviceConfig) bgConf, pCC[i])
            && isValidConfig2((mpe_DispDeviceConfig) gfxConf, pCC[i])
            && isValidConfig2((mpe_DispDeviceConfig) vidConf, pCC[i]))
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                    "Coherent config Found!!!..\n");
            *impact = FALSE;
            break;
        }
        else
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                    "Coherent config not Found!!!..\n");
        }
    }
    return MPE_SUCCESS;
}

/**
 * Allows setting of screen aspect ratio.
 * Only two aspect ratios are supported:
 * <ul>
 *   <li> 4:3
 *   <li> 16:9
 * </ul>
 *
 * @param width
 * @param height
 * @return success status (?)
 */
static mpe_Bool setScreenAspect(int width, int height)
{

    // TODO: Update this function
    uint32_t mode;

    // 640x480
    if (width == 4 && height == 3)
    {
        mode = VDM_ASPECTRATIO_4_3;
    }

    // 1920x1080 and 960x540
    else if (width == 16 && height == 9)
    {
        mode = VDM_ASPECTRATIO_16_9;
    }

    // 720x480  - treat this as if it were 640x480 so it fills the simulator window
    else if (width == 3 && height == 2)
    {
        mode = VDM_ASPECTRATIO_4_3;
    }

    // other aspect ratios not handled at this point
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DISP,
                "MPEDISP: setScreenAspect failed - unsupported aspect ratio %d:%d\n",
                width, height);
        return FALSE;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
            "MPEDISP: setScreenAspect(%d:%d) mode=%d\n", width, height, mode);

    return TRUE;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispSetCurrConfig(mpe_DispDevice device,
        mpe_DispDeviceConfig config)
{
    os_Device* pDevice = (os_Device*) device;
    mpe_Bool wouldImpactFlag;

    if (NULL == device || !isValidConfig(config, pDevice->configs))
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else if (MPE_SUCCESS != wouldImpact(pDevice, (os_Config*) config, &wouldImpactFlag))
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else if (wouldImpactFlag)
    {
        return (mpe_Error) MPE_DISP_ERROR_IMPACTS_OTHERS;
    }
    else
    {
        os_Config* cfg = (os_Config*) config;

        pDevice->curr = cfg;

        // since there is just one screen in the RI implementation, refresh that screen's display
        return refreshDisplay (&(gScreens[0]), 1 /* doDisplayInit */);
    }
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispWouldImpact(mpe_DispDevice device,
        mpe_DispDeviceConfig config, mpe_DispDevice device2,
        mpe_DispDeviceConfig config2, mpe_Bool *impact)
{
    os_Device* pDevice = (os_Device*) device;
    os_Device* pDevice2 = (os_Device*) device2;

    /* If these configs aren't in the same coherent config... */
    if (NULL == impact || NULL == device || !isValidConfig(config,
            pDevice->configs) || NULL == device2 || !isValidConfig(config2,
            pDevice2->configs))
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        if (pDevice->info.screen != pDevice2->info.screen)
        {
            *impact = FALSE;
        }
        else
        {
            int i;
            /*
             * Verify whether both the configurations are coherent configuration.
             * if yes then return impact as FALSE else the impact should be TRUE
             */
            os_CoherentConfig **pCC = (os_CoherentConfig**)((os_Screen*)pDevice->info.screen)->cc;
            for (i = 0; pCC[i] != NULL; i++)
            {
                if (isValidConfig2(config2, pCC[i]) && isValidConfig2(config, pCC[i]))
                {
                    *impact = FALSE;
                    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,"Coherent config Found!!!..\n");
                    return MPE_SUCCESS;
                }
            }
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,"No Coherent config Found\n");
            *impact = TRUE;
        }
        return MPE_SUCCESS;
    }
}

#ifdef MPEOS_LOG_ENABLED
static void dumpConfig(int level, os_Config* pConfig)
{
    MPEOS_LOG(level, MPE_MOD_DISP, "CONFIG %d\n", pConfig);
    MPEOS_LOG(level, MPE_MOD_DISP, " interlaced = %d\n", pConfig->info.interlaced);
    MPEOS_LOG(level, MPE_MOD_DISP, " flicker = %d\n", pConfig->info.flickerFilter);
    MPEOS_LOG(level, MPE_MOD_DISP, " stills = %d\n", pConfig->info.mpegStills);
    MPEOS_LOG(level, MPE_MOD_DISP, " colors = %d\n", pConfig->info.changeableColor);
    MPEOS_LOG(level, MPE_MOD_DISP, " pixelAspect = %d:%d\n",
            pConfig->info.pixelAspectRatio.width,
            pConfig->info.pixelAspectRatio.height);
    MPEOS_LOG(level, MPE_MOD_DISP, " resolution = %dx%d\n",
            pConfig->info.resolution.width,
            pConfig->info.resolution.height);
    MPEOS_LOG(level, MPE_MOD_DISP, " area = %f,%f,%f,%f\n",
            pConfig->info.area.x,
            pConfig->info.area.y,
            pConfig->info.area.width,
            pConfig->info.area.height);
}
#endif

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetConfigInfo(mpe_DispDeviceConfig config,
        mpe_DispDeviceConfigInfo* info)
{
    os_Config* pConfig = (os_Config*) config;

    if (NULL == config || NULL == info)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    dumpConfig(MPE_LOG_DEBUG, pConfig);

    *info = pConfig->info;

    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetCoherentConfigCount(mpe_DispScreen screen,
        uint32_t* nSets)
{
    if (!isValidScreen(screen) || NULL == nSets)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Screen* pScreen = (os_Screen*) screen;
        os_CoherentConfig **cc = pScreen->cc;

        int n = 0;
        if (cc != NULL)
        {
            for (n = 0; *cc != NULL; ++cc, ++n)
                ;
        }
        *nSets = n;
        return MPE_SUCCESS;
    }
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetCoherentConfigs(mpe_DispScreen screen,
        mpe_DispCoherentConfig* set)
{
    if (!isValidScreen(screen) || NULL == set)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Screen* pScreen = (os_Screen*) screen;
        os_CoherentConfig **cc = pScreen->cc;
        os_CoherentConfig **dest = (os_CoherentConfig**) set;

        if (cc != NULL)
        {
            for (; *cc != NULL;)
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "COHERENT = %d\n",
                        (int) *cc);
                *dest++ = *cc++;
            }
        }

        return MPE_SUCCESS;
    }
}

#ifndef MAX
#define MAX(x,y) (((x) > (y)) ? (x) : (y))
#endif
#ifndef MIN
#define MIN(x,y) (((x) < (y)) ? (x) : (y))
#endif

/**
 * Returns the greatest common divisor for <i>m</i> and <i>n</i>
 * @param m
 * @param n
 * @return the largest number such that <code>m % gcd(m,n)</code>
 * and <code>n % gcd(m, n)</code> is zero
 */
static int gcd(int m, int n)
{
    int a = MAX(n, m);
    int b = MIN(n, m);
    int r = 1;
    while (r > 0)
    {
        r = a % b;
        a = b;
        b = r;
    }
    return a;
}

/**
 * Returns the aspect ratio for the given dimensions.
 *
 * This is calculated by first finding the greatest common divisor
 * for <code>dim->width</code> and <code>dim->height</code> and then
 * dividing each by that number.
 *
 * @param dim dimensions
 * @param aspect pointer to location where aspect ratio is to be written
 * @return <i>aspect</i>
 */
static void getAspectRatio(mpe_GfxDimensions *dim, mpe_GfxDimensions *aspect)
{
    if (dim->width == 0 || dim->height == 0)
    {
        aspect->width = 0;
        aspect->height = 0;
    }
    else
    {
        int myGcd = gcd(dim->width, dim->height);

        aspect->width = dim->width / myGcd;
        aspect->height = dim->height / myGcd;
    }
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispSetCoherentConfig(mpe_DispScreen screen,
        mpe_DispCoherentConfig set)
{
    return mpeos_dispSetCoherentConfig_Helper(screen, set, 1);
}

mpe_Error mpeos_dispSetCoherentConfig_Helper(mpe_DispScreen screen,
        mpe_DispCoherentConfig set, int doDisplayInit)
{
    os_Screen *pScreen = (os_Screen*) screen;

    if (!isValidScreen(screen) || !isValidCoherentConfig(set, pScreen->cc))
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Config** ppConfigs = (os_CoherentConfig*) set;

        for (; *ppConfigs != NULL; ++ppConfigs)
        {
            os_Config *cfg = *ppConfigs;
            ((os_Device*) cfg->info.device)->curr = cfg;
        }

        return refreshDisplay (pScreen, doDisplayInit);
    }
}

mpe_Error refreshDisplay (os_Screen *pScreen, int doDisplayInit)
{
    mpe_GfxDimensions videoAspectRatio = { 0, 0 };
    mpe_GfxDimensions backgroundAspectRatio = { 0, 0 };
    int aspectHeight = 0;
    int aspectWidth = 0;
    mpe_Bool rc;
    mpe_Error error;


    /* Use the video config to dictate the aspect ratio.  If no video
     config then use the background config */
    getAspectRatio(&pScreen->vid->curr->info.resolution, &videoAspectRatio);
    getAspectRatio(&pScreen->bg->curr->info.resolution, &backgroundAspectRatio);


    /* assume we will use video aspect ratio */
    aspectWidth = videoAspectRatio.width;
    aspectHeight = videoAspectRatio.height;


    /* if no video aspect ratio, then use background */
    if (aspectWidth == 0 && aspectHeight == 0)
    {
        // if not background aspect ratio, then we fail
        if (backgroundAspectRatio.width == 0
                && backgroundAspectRatio.height == 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "No BACKGROUND or VIDEO device in coherent configuration!!!!!\n");
            return MPE_EINVAL;
        }

        aspectWidth = backgroundAspectRatio.width;
        aspectHeight = backgroundAspectRatio.height;
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DISP,
                "MPEDISP: SetCoherentConfig used background aspect ratio %d:%d\n",
                aspectWidth, aspectHeight);
    }

    if (doDisplayInit != 0)
    {

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "Calling rip_InitDisplay...\n");

        // Make calls to rip_display to setup display to match selected config
        (void) rip_InitDisplay(pScreen->gfx->curr->info.resolution.width,
                pScreen->gfx->curr->info.resolution.height,
                pScreen->gfx->curr->info.pixelAspectRatio.width,
                pScreen->gfx->curr->info.pixelAspectRatio.height,
                pScreen->vid->curr->info.resolution.width,
                pScreen->vid->curr->info.resolution.height,
                pScreen->vid->curr->info.pixelAspectRatio.width,
                pScreen->vid->curr->info.pixelAspectRatio.height,
                pScreen->bg->curr->info.resolution.width,
                pScreen->bg->curr->info.resolution.height,
                pScreen->bg->curr->info.pixelAspectRatio.width,
                pScreen->bg->curr->info.pixelAspectRatio.height);
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
            "MPEDISP: SetCoherentConfig: Resizing screen to %d x %d \n",
            pScreen->gfx->curr->info.resolution.width,
            pScreen->gfx->curr->info.resolution.height);

    // Resize the graphics as directed by the new configuration
    error = dfb_gfxResizePrimarySurface(
                    pScreen->gfx->curr->info.resolution);

    if (error != MPE_GFX_ERROR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "<<<<DISP>>>> mpeos_dispSetCoherentConfig - Error setting new screen config\n");
        return error;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
            "MPEDISP: SetCoherentConfig: Setting aspect ratio to %d x %d \n",
            aspectWidth, aspectHeight);

    // Adjust screen aspect ratio as necessary
    rc = setScreenAspect(aspectWidth, aspectHeight);

    if (rc == FALSE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "MPEDISP setCoherentConfig failed to set screen aspect ratio\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetConfigSetCount(mpe_DispCoherentConfig set,
        uint32_t* nConfigs)
{
    if (NULL == set || NULL == nConfigs)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Config** ppConfigs = (os_CoherentConfig*) set;
        int n;

        for (n = 0; ppConfigs[n] != NULL; ++n)
            ;
        *nConfigs = n;

        return MPE_SUCCESS;
    }
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetConfigSet(mpe_DispCoherentConfig set,
        mpe_DispDeviceConfig* configs)
{
    if (NULL == set || NULL == configs)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }
    else
    {
        os_Config** ppConfigs = (os_CoherentConfig*) set;
        int i;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "COHERENT %d:\n",
                (int) ppConfigs);
        for (i = 0; ppConfigs[i] != NULL; ++i)
        {
            dumpConfig(MPE_LOG_DEBUG, ppConfigs[i]);
            configs[i] = (void*) ppConfigs[i];
        }

        return MPE_SUCCESS;
    }
}

mpe_Error mpeos_dispSetBGColor(mpe_DispDevice device, mpe_GfxColor color)
{
    os_Device* pDevice = (os_Device*) device;

    if (NULL == device)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    pDevice->color = color;

    // *TODO* - need to add support for finding specified background device
    // For now, just use the RI display
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    ri_display_t* display = NULL;
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->set_bg_color(display, (uint32_t) color);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "MPEDISP setBGColor - unable to retrieve display device\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "MPEDISP setBGColor - unable to retrieve pipeline mgr\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * Implemented via WinTvExt.
 */
mpe_Error mpeos_dispGetBGColor(mpe_DispDevice device, mpe_GfxColor* color)
{
    os_Device* pDevice = (os_Device*) device;

    if (NULL == device)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    *color = pDevice->color;

    return MPE_SUCCESS;
}

/**
 * Implemented via WinTvExt.
 */
mpe_Error mpeos_dispDisplayBGImage(mpe_DispDevice device,
        mpe_DispBGImage handle, mpe_GfxRectangle *area)
{
    //os_BgImage* image = (os_BgImage*)handle;
    mpe_Bool success = FALSE;
    UNUSED(device);

    if (NULL == handle)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    // XXX - This use to be implemented via WinTvExt, need something
    // equivalent in RI Simulator.
    //success = Display_DisplayBgImage(image->data, image->length, (LPRECT)area);
    if (!success)
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "DISP: could not display bg image!\n");
    return success ? MPE_SUCCESS : MPE_DISP_ERROR_UNKNOWN;
}

#if 1
/**
 * Validates the given I-Frame and determines the display width/height.
 *
 * @param buffer
 * @param length
 * @param widthp
 * @param heightp
 *
 * @return MPE_SUCCESS or MPE_DISP_ERROR_BAD_IFRAME
 */
static mpe_Error validateMpegIFrame(uint8_t* buffer, size_t length,
        uint32_t *widthp, uint32_t *heightp)
{
    uint32_t width, height;
    /* Simply validate some amount of the sequence header for now. */

    /* sequence_header is at least 98 bytes, so let's worry about that */
    if (length < 98)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "DISP: not long enough for sequence_header\n");
        return (mpe_Error) MPE_DISP_ERROR_BAD_IFRAME;
    }

    if (buffer[0] != 0 || buffer[1] != 0 || buffer[2] != 0x01 || buffer[3]
            != 0xB3)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "DISP: invalid sequenc_header_code %02x%02x%02x%02x\n",
                buffer[0], buffer[1], buffer[2], buffer[3]);
        return (mpe_Error) MPE_DISP_ERROR_BAD_IFRAME;
    }

    /* !!!!TODO!!!! Support sequence_extension_header for full 14-bits !!! */
    width = (buffer[4] << 4) | ((buffer[5] >> 4) & 0xF);
    height = ((buffer[5] & 0xF) << 8) | buffer[6];

    if (!width || !height || width > 1920 || height > 1080)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "DISP: image size out of range %dx%d\n", width, height);
        return (mpe_Error) MPE_DISP_ERROR_BAD_IFRAME;
    }

    *widthp = width;
    *heightp = height;

    return MPE_SUCCESS;
}
#endif

/**
 * Implemented via WinTvExt.
 */
mpe_Error mpeos_dispBGImageNew(uint8_t* data, size_t length,
        mpe_DispBGImage* handle)
{
    uint32_t width, height;
    os_BgImage *image;
    uint8_t* imagedata;
    mpe_Error err;

    if (NULL == data || 0 >= length || NULL == handle)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP, "DISP: invalid params!\n");
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    /* Validate MPEG-2 I-Frame */
    if (MPE_SUCCESS
            != (err = validateMpegIFrame(data, length, &width, &height)))
    {
        return (mpe_Error) err;
    }

    if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_GFX, sizeof(os_BgImage),
            (void**) &image)))
        return err;
    if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_GFX, length,
            (void**) &imagedata)))
    {
        mpeos_memFreeP(MPE_MEM_GFX, (void*) image);
        return err;
    }

    memcpy(imagedata, data, length);

    image->data = imagedata;
    image->length = length;

    /* Fill in the width/height. */
    image->size.width = width;
    image->size.height = height;

    *handle = (void*) image;

    return MPE_SUCCESS;
}

/**
 * Implemented via WinTvExt.
 */
mpe_Error mpeos_dispBGImageDelete(mpe_DispBGImage handle)
{
    os_BgImage* image = (os_BgImage*) handle;

    if (image)
    {
        if (image->data)
        {
            mpeos_memFreeP(MPE_MEM_GFX, (void*) image->data);
        }
        mpeos_memFreeP(MPE_MEM_GFX, (void*) image);
    }
    else
    {
        return MPE_DISP_ERROR_INVALID_PARAM;
    }

    return MPE_SUCCESS;
}

/**
 * Unimplemented or "fake" implemented.
 */
mpe_Error mpeos_dispBGImageGetSize(mpe_DispBGImage handle,
        mpe_GfxDimensions* size)
{
    os_BgImage* image = (os_BgImage*) handle;

    if (image)
    {
        *size = image->size;
    }
    else
    {
        return MPE_DISP_ERROR_INVALID_PARAM;
    }

    //TODO: create GetSize API in WinTvExt

    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetOutputPortCount(uint32_t *nPorts)
{
    char** videoOutputPortNames = NULL;
    *nPorts = ri_get_backpanel()->getVideoOutputPortNameList(
            &videoOutputPortNames);
    ri_get_backpanel()->freeVideoOutputPortNameList(videoOutputPortNames);
    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetOutputPorts(mpe_DispOutputPort *ports)
{
    if (ports == NULL)
    {
        return MPE_DISP_ERROR_INVALID_PARAM;
    }

    ri_backpanel_t* bp = ri_get_backpanel();
    char** videoOutputPortNames = NULL;
    int nPorts = bp->getVideoOutputPortNameList(&videoOutputPortNames);

    int i;
    for (i = 0; i < nPorts; i++)
    {
        ports[i] = bp->getVideoOutputPortHandle(videoOutputPortNames[i]);
    }
    bp->freeVideoOutputPortNameList(videoOutputPortNames);

    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispEnableOutputPort(mpe_DispOutputPort port, mpe_Bool enable)
{
    mpe_Error returnThis = MPE_SUCCESS;
    ri_backpanel_t* bp = ri_get_backpanel();
    mpe_DispOutputPortInfo p;
    returnThis = mpeos_dispGetOutputPortInfo(port, &p);

    if (returnThis == MPE_SUCCESS)
    {
        const char * updateAllSDPorts = mpeos_envGet("DISP.UPDATE.ALL.SDPORTS");
        switch (p.type)
        {
        case MPE_DISPLAY_RF_PORT:
        case MPE_DISPLAY_BASEBAND_PORT:
        case MPE_DISPLAY_SVIDEO_PORT:
            if( (updateAllSDPorts != NULL) && (stricmp(updateAllSDPorts, "TRUE") == 0) )
            {
                /*
                 * Control Standard Definition outputs as a group.
                 * This is NOT specified, but is normal behavior based on typical hardware.
                 * SD outputs generally are all derived from the same source,
                 * thus enabled/disabled together. Control HD outputs exclusively.
                 */
                updateSDPorts(bp, enable);
                returnThis = MPE_SUCCESS;
                break;
            }
            /*
             * Updating the SD port exclusively (below) if DISP.UPDATE.ALL.SDPORTS
             * of mpeenv.ini is set to false
             */
        case MPE_DISPLAY_1394_PORT:
        case MPE_DISPLAY_DVI_PORT:
        case MPE_DISPLAY_COMPONENT_PORT:
        case MPE_DISPLAY_HDMI_PORT:
        case MPE_DISPLAY_INTERNAL_PORT:
            // Control this port
            bp->setVideoOutputPortValue(port, VIDEO_OUTPUT_ENABLED, &enable);
            returnThis = MPE_SUCCESS;
            break;

        default:
            returnThis = MPE_DISP_ERROR_INVALID_PARAM;
            break;
        }
    }
    return returnThis;
}

static void updateSDPorts(ri_backpanel_t* bp, mpe_Bool enable)
{
    int i;
    int aPortType;
    char** videoOutputPortNames = NULL;
    int nPorts = bp->getVideoOutputPortNameList(&videoOutputPortNames);
    for (i = 0; i < nPorts; i++)
    {
        void* aPortHndl = bp->getVideoOutputPortHandle(videoOutputPortNames[i]);
        bp->getVideoOutputPortValue(aPortHndl, VIDEO_OUTPUT_PORT_TYPE, &aPortType);
        switch(aPortType)
        {
        case MPE_DISPLAY_RF_PORT:
        case MPE_DISPLAY_BASEBAND_PORT:
        case MPE_DISPLAY_SVIDEO_PORT:
            bp->setVideoOutputPortValue(aPortHndl, VIDEO_OUTPUT_ENABLED, &enable);
            break;
        }
    }
    bp->freeVideoOutputPortNameList(videoOutputPortNames);
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetOutputPortInfo(mpe_DispOutputPort port,
        mpe_DispOutputPortInfo *info)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_SOUND, "mpeos_dispGetOutputPortInfo\n");
    mpe_Error returnThis = MPE_EINVAL;

    if (port != NULL && info != NULL)
    {
        ri_backpanel_t* bp = ri_get_backpanel();
        bp->getVideoOutputPortValue(port, VIDEO_OUTPUT_PORT_ID,
                &(info->idString));
        bp->getVideoOutputPortValue(port, VIDEO_OUTPUT_PORT_TYPE, &(info->type));
        bp->getVideoOutputPortValue(port, VIDEO_OUTPUT_ENABLED,
                &(info->enabled));
        bp->getVideoOutputPortValue(port, VIDEO_OUTPUT_DTCP_SUPPORTED,
                &(info->dtcpSupported));
        bp->getVideoOutputPortValue(port, VIDEO_OUTPUT_HDCP_SUPPORTED,
                &(info->hdcpSupported));
        bp->getVideoOutputPortValue(port, VIDEO_OUTPUT_RESOLUTION_RESTRICTION,
                &(info->restrictedResolution));
        bp->getVideoOutputPortValue(port, VIDEO_OUTPUT_AUDIO_PORT,
                &(info->audioPort));
        bp->getVideoOutputPortValue(port, VIDEO_OUTPUT_CONNECTED,
                &(info->connected));


        info->fixedConfigInfo = &gVideoOutputConfigInfo;
        returnThis = MPE_SUCCESS;
    }
    return returnThis;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispSetRFBypassState(mpe_Bool enable)
{
    gRFBypassEnabled = enable;
    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetRFBypassState(mpe_Bool *state)
{
    if (NULL == state)
    {
        return MPE_DISP_ERROR_INVALID_PARAM;
    }
    *state = gRFBypassEnabled;
    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispSetRFChannel(uint32_t channel)
{
    /* for now accept all channels */
    gRFChannelNumber = channel;
    return MPE_SUCCESS;
}

/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetRFChannel(uint32_t *channel)
{
    if (NULL == channel)
    {
        return MPE_DISP_ERROR_INVALID_PARAM;
    }
    *channel = gRFChannelNumber;
    return MPE_SUCCESS;
}

/**
 * Get the current Decoder Format Conversion (DFC) mode for the given decoder
 *
 * @param decoder is the decoder
 * @param applicationDfc is a pointer filled in by this method with the DFC mode
 *        currently set by the application.   This can be any of the DFC values
 *        defined in org.dvb.media.VideoFormatControl or
 *        org.ocap.media.VideoFormatControl including DFC_PLATFORM.  Note that
 *        if applicationDfc = DFC_PLATFORM, this indicates that the DFC mode is
 *        controlled by the platform and the exact DFC in use will be returned
 *        in the platformDfc parameter.
 * @param platformDfc is a pointer filled in by this method with the DFC mode
 *        that is applied by the platform when applicationDfc = DFC_PLATFORM.
 *        In the case where applciationDfc != DFC_PLATFORM, this parameter must
 *        still contain the DFC mode that "would be" applied by the platform
 *        based on the current video settings (AR, DAR, AFD).
 *
 * @return error level
 */
mpe_Error mpeos_dispGetDFC(mpe_DispDevice decoder,
        mpe_DispDfcAction *applicationDfc, mpe_DispDfcAction *platformDfc)
{
    // TODO: Make sure to allow DFC processing only on background video device
    //       Foreground video (component/PIP) devices should not support DFCs
    MPE_UNUSED_PARAM(decoder);

    *applicationDfc = g_applicationDfc;
    *platformDfc = g_platformDfc;

    return MPE_SUCCESS;
}

/**
 * Check whether a DFC is valid for the platform
 *
 * @param decoder is the decoder
 * @param action is a DFC
 *
 * @return error if DFC is not valid for this platform
 */
mpe_Error mpeos_dispCheckDFC(mpe_DispDevice decoder, mpe_DispDfcAction action)
{
    // TODO: Make sure to allow DFC processing only on background video device
    //       Foreground video (component/PIP) devices should not support DFCs
    mpe_DispDeviceDest dest = dispGetDestDevice(decoder);
    if (dest == MPE_DISPLAY_DEST_VIDEO || dest == MPE_DISPLAY_DEST_UNKNOWN)
    {
        return MPE_EINVAL;
    }

    // The simulator only supports platform mode and DFCs that copy the entire
    // input frame to the output frame
    if (action != MPE_DFC_PROCESSING_NONE && action != MPE_DFC_PROCESSING_FULL
            && action != MPE_DFC_PROCESSING_LB_16_9 && action
            != MPE_DFC_PROCESSING_LB_14_9 && action != MPE_DFC_PROCESSING_CCO
            && action != MPE_DFC_PROCESSING_PAN_SCAN && action
            != MPE_DFC_PROCESSING_LB_2_21_1_ON_4_3 && action
            != MPE_DFC_PROCESSING_LB_2_21_1_ON_16_9 && action
            != MPE_DFC_PLATFORM && action != MPE_DFC_PROCESSING_16_9_ZOOM
            && action != MPE_DFC_PROCESSING_PILLARBOX_4_3 && action
            != MPE_DFC_PROCESSING_WIDE_4_3)
        return MPE_EINVAL;

    return MPE_SUCCESS;
}

mpe_Error mpeos_dispGetSupportedDFCCount(mpe_DispDevice decoder,
        uint32_t* count)
{
    mpe_DispDeviceDest dest = dispGetDestDevice(decoder);
    if (dest == MPE_DISPLAY_DEST_UNKNOWN || dest == MPE_DISPLAY_DEST_VIDEO)
    {
        *count = 0;
    }
    else
    {
        *count = sizeof(g_supportedDfcs) / sizeof(int32_t);
    }
    return MPE_SUCCESS;
}

mpe_Error mpeos_dispGetSupportedDFCs(mpe_DispDevice decoder,
        mpe_DispDfcAction** dfcs)
{
    mpe_DispDeviceDest dest = dispGetDestDevice(decoder);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
            "mpeos_dispGetSupportedDFCs -- dest = %d\n", dest);

    if (MPE_DISPLAY_DEST_TV == dest)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "mpeos_dispGetSupportedDFCs -- MPE_DISPLAY_DEST_TV\n");
        *dfcs = g_supportedDfcs;
    }
    else if (MPE_DISPLAY_DEST_VIDEO == dest)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "mpeos_dispGetSupportedDFCs -- MPE_DISPLAY_DEST_VIDEO\n");
        *dfcs = NULL;
    }
    else if (MPE_DISPLAY_DEST_UNKNOWN == dest)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "mpeos_dispGetSupportedDFCs -- MPE_DISPLAY_DEST_UNKNOWN\n");
        *dfcs = NULL;
        return MPE_EINVAL;
    }
    else
    {
        *dfcs = NULL;
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

mpe_Error mpeos_dispSetDefaultPlatformDFC(mpe_DispDevice decoder,
        mpe_DispDfcAction action)
{
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = NULL;

    // TODO: Make sure to allow DFC processing only on background video device
    //       Foreground video (component/PIP) devices should not support DFCs
    MPE_UNUSED_PARAM(decoder);

    if (action != MPE_DFC_PROCESSING_NONE && action != MPE_DFC_PROCESSING_FULL
            && action != MPE_DFC_PROCESSING_LB_16_9 && action
            != MPE_DFC_PROCESSING_LB_14_9 && action != MPE_DFC_PROCESSING_CCO
            && action != MPE_DFC_PROCESSING_PAN_SCAN && action
            != MPE_DFC_PROCESSING_LB_2_21_1_ON_4_3 && action
            != MPE_DFC_PROCESSING_LB_2_21_1_ON_16_9 && action
            != MPE_DFC_PLATFORM && action != MPE_DFC_PROCESSING_16_9_ZOOM
            && action != MPE_DFC_PROCESSING_PILLARBOX_4_3 && action
            != MPE_DFC_PROCESSING_WIDE_4_3)
    {
        return MPE_EINVAL;
    }

    g_defaultPlatformDfc = action;

    // Map the RI components to MPEOS level
    // *TODO* - move this call to video_device
    pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->set_dfc_default(display, (uint32_t) action);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "DISP: could not retrieve display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "DISP: could not retrieve pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * Set the current DFC for the given decoder
 *
 * @param decoder is the decoder
 * @param ar is a DFC
 *
 * @return error level
 */
mpe_Error mpeos_dispSetDFC(mpe_DispDevice decoder, mpe_DispDfcAction action)
{
    ri_pipeline_manager_t* pMgr = NULL;
    ri_display_t* display = NULL;

    // TODO: Make sure to allow DFC processing only on background video device
    //       Foreground video (component/PIP) devices should not support DFCs
    if (action != MPE_DFC_PROCESSING_NONE && action != MPE_DFC_PROCESSING_FULL
            && action != MPE_DFC_PROCESSING_LB_16_9 && action
            != MPE_DFC_PROCESSING_LB_14_9 && action != MPE_DFC_PROCESSING_CCO
            && action != MPE_DFC_PROCESSING_PAN_SCAN && action
            != MPE_DFC_PROCESSING_LB_2_21_1_ON_4_3 && action
            != MPE_DFC_PROCESSING_LB_2_21_1_ON_16_9 && action
            != MPE_DFC_PLATFORM && action != MPE_DFC_PROCESSING_16_9_ZOOM
            && action != MPE_DFC_PROCESSING_PILLARBOX_4_3 && action
            != MPE_DFC_PROCESSING_WIDE_4_3)
        return MPE_EINVAL;

    // store the Dfc mode
    g_applicationDfc = action;

    // platformDfc must reflect the true Dfc in use, DFC_PLATFORM is simply
    // used to place DFC processing under control of the platform
    if (g_applicationDfc != MPE_DFC_PLATFORM)
    {
        g_platformDfc = g_applicationDfc;
    }
    else if (g_defaultPlatformDfc != MPE_DFC_PROCESSING_UNKNOWN)
    {
        g_platformDfc = g_defaultPlatformDfc;
    }

    // Set the desired DFC mode in platform
    // Map the RI components to MPEOS level
    // *TODO* - move this call to video_device
    pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->set_dfc_mode(display, (int32_t) action);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "DISP: could not retrieve display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "DISP: could not retrieve pipeline manager\n");
        return MPE_EINVAL;
    }

    // Set the event to registered listeners on this device which
    // is stored in
    if (os_mediaNotifyDecodeListener(decoder, MPE_DFC_CHANGED, (void*) action)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "DISP: problems notifying decode listener DFC changed\n");
        return MPE_DISP_ERROR_UNKNOWN;
    }
    return MPE_SUCCESS;
}

/**
 * Sets an option on a video output port.
 *
 * @param port video output port to set an option on
 * @param opt option name/value structure containing option name and value
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_INVALID_PARAM if opt is <code>NULL</code> or contains an invalid value
 */
mpe_Error mpeos_dispSetVideoOutputPortOption(mpe_DispOutputPort port,
        mpe_DispOutputPortOption *opt)
{
    MPE_UNUSED_PARAM(port); /* TODO: should this parameter be used? */

    if (NULL == opt)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "mpeos_dispSetVideoOutputPortOption - invalid parameter\n");
        return MPE_DISP_ERROR_INVALID_PARAM;
    }

    if (opt->option == MPE_DISP_1394_SELECT_SINK)
    {
        mpe_Disp1394DeviceInfo* deviceInfo =
                (mpe_Disp1394DeviceInfo*) opt->value;
        int i = 0;
        for (i = 0; i < 8; i++)
        {
            if (deviceInfo->eui64[i] != gIEEE1394deviceInfo.eui64[i])
            {
                return MPE_DISP_ERROR_INVALID_PARAM;
            }
        }
        return MPE_SUCCESS;
    }

    return MPE_DISP_ERROR_UNIMPLEMENTED;
}

/**
 * Gets the value of an option on a video output port.
 *
 * @param port video output port to get an option on
 * @return MPE_SUCCESS if the option value was retrieved
 * @return MPE_DISP_ERROR_INVALID_PARAM if opt is <code>NULL</code>
 * @return MPE_ENOMEM if memory cannot be allocated
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if retrieval of particular option is not implemented
 */
mpe_Error mpeos_dispGetVideoOutputPortOption(mpe_DispOutputPort port,
        mpe_DispOutputPortOption *opt)
{
    MPE_UNUSED_PARAM(port); /* TODO: should this parameter be used? */

    if (NULL == opt)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "mpeos_dispGetVideoOutputPortOption - invalid parameter\n");
        return MPE_DISP_ERROR_INVALID_PARAM;
    }

    if (opt->option == MPE_DISP_1394_DEVICE_LIST)
    {

        mpe_Disp1394Devices* devices;
        if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_FP,
                sizeof(mpe_Disp1394Devices), (void**) (&devices)))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "mpeos_dispGetVideoOutputPortOption - unable to allocate memory\n");
            return MPE_ENOMEM;
        }

        devices->infoCount = 1;
        devices->infoArray[0] = gIEEE1394deviceInfo;
        opt->value = (void*) devices;
        return MPE_SUCCESS;
    }

    return MPE_DISP_ERROR_UNIMPLEMENTED;
}

mpe_Error mpeos_dispGetIEEE1394NodeList(int *nodeSize, int nodelist[],
        uint8_t euid[])
{
    mpe_Error ret = MPE_DISP_ERROR_UNIMPLEMENTED;

    // TODO: implement IEEE-1394 simulation
    MPE_UNUSED_PARAM(nodeSize); /* TODO: should this parameter be used? */
    MPE_UNUSED_PARAM(nodelist); /* TODO: should this parameter be used? */
    MPE_UNUSED_PARAM(euid); /* TODO: should this parameter be used? */

    return ret;
}

mpe_Error mpeos_dispGetIEEE1394ModelName(int deviceID, char *name)
{
    mpe_Error ret = MPE_DISP_ERROR_UNIMPLEMENTED;

    // TODO: implement IEEE-1394 simulation
    MPE_UNUSED_PARAM(deviceID); /* TODO: should this parameter be used? */
    MPE_UNUSED_PARAM(name); /* TODO: should this parameter be used? */

    return ret;
}

mpe_Error mpeos_dispGetIEEE1394VendorName(int deviceID, char *name)
{
    mpe_Error ret = MPE_DISP_ERROR_UNIMPLEMENTED;

    // TODO: implement IEEE-1394 simulation
    MPE_UNUSED_PARAM(deviceID); /* TODO: should this parameter be used? */
    MPE_UNUSED_PARAM(name); /* TODO: should this parameter be used? */

    return ret;
}

mpe_Error mpeos_dispGetIEEE1394SubunitType(int deviceID, short *subtype)
{
    mpe_Error ret = MPE_DISP_ERROR_UNIMPLEMENTED;

    // TODO: implement IEEE-1394 simulation
    MPE_UNUSED_PARAM(deviceID); /* TODO: should this parameter be used? */
    MPE_UNUSED_PARAM(subtype); /* TODO: should this parameter be used? */

    return ret;
}

/**
 * The current implementation only supports one graphics device, which
 * is reflected in the implementation of this function.
 */
mpe_Error mpeos_dispGetGfxSurface(mpe_DispDevice device,
        mpe_GfxSurface *surface)
{
    os_Device * gfx = gScreens[0].gfx;

    if (device != (mpe_DispDevice) gfx)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                  "mpeos_dispGetGfxSurface(): %p != %p\n", (os_Device*)device, gfx);
        return MPE_DISP_ERROR_INVALID_PARAM;
    }

    return gfxGetScreenSurface(surface);
}

/**
 * The current implementation only supports one graphics device, which
 * is reflected in the implementation of this function.
 */
mpe_Error mpeos_dispFlushGfxSurface(mpe_DispDevice device)
{
    os_Device * gfx = gScreens[0].gfx;

    if (device != (mpe_DispDevice) gfx)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                  "mpeos_dispFlushGfxSurface(): %p != %p\n", (os_Device*)device, gfx);
        return MPE_DISP_ERROR_INVALID_PARAM;
    }

    return gfxScreenFlush();
}

static mpe_Error gfxGetScreenSurface(mpe_GfxSurface *surface)
{

    mpe_Error ret = MPE_GFX_ERROR_NOERR;

    if (surface)
    {
        *surface = (mpe_GfxSurface) _screen.surf;
    }
    else
    {
        ret = MPE_GFX_ERROR_INVALID;
    }
    return ret;
}

static mpe_Error gfxScreenFlush(void)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;

    GFX_LOCK();

    if (_screen.surf->os_data.os_s->Flip(_screen.surf->os_data.os_s, NULL,
            (DFBSurfaceFlipFlags) 0) != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "gfxScreenFlush() - DirectFB::Flip() failed\n");
        err = MPE_GFX_ERROR_OSERR;
    }

    GFX_UNLOCK();

    return err;
}

/* DSExt */
/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispSetMainVideoOutputPort(mpe_DispScreen screen,
        mpe_DispOutputPort port)
{
    return MPE_DISP_ERROR_UNIMPLEMENTED;
}


/* DSExt */
/* See API doc (mpeos_disp.h) */

mpe_Error mpeos_dispGetDisplayAttributes(mpe_DispOutputPort port,
        mpe_DispVideoDisplayAttrInfo* info)
{
    mpe_Error err = (mpe_Error) MPE_SUCCESS;
    MPE_UNUSED_PARAM(port);

    if (g_pDisplayAttrInfo == NULL)
    {
        err = (mpe_Error) MPE_DISP_ERROR_NOT_AVAILABLE;
    }
    else
    {
        *info = *g_pDisplayAttrInfo;
    }

    return err;
}
/* DSExt */
/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispIsDisplayConnected(mpe_DispOutputPort port,
        mpe_Bool* connected)
{
    mpe_DispOutputPortInfo info;

	if (port == NULL || mpeos_dispGetOutputPortInfo(port, &info) != MPE_SUCCESS)
	{
		return MPE_DISP_ERROR_INVALID_PARAM;
	}

    *connected = info.connected;

    return (mpe_Error) MPE_SUCCESS;
}

/* DSExt */
/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispIsContentProtected(mpe_DispOutputPort port,
        mpe_Bool* encrypted)
{
    *encrypted = FALSE;
    //   return MPE_DISP_ERROR_UNIMPLEMENTED;
    return (mpe_Error) MPE_SUCCESS;
}

/* DSExt */
/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetSupportedFixedVideoOutputConfigurationCount(
        mpe_DispOutputPort port, uint32_t* count)
{
    *count = gFixedOutputConfigCount;
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "mpeos_dispGetSupportedFixedVideoOutputConfigurationCount (1) returning %d\n", *count);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "mpeos_dispGetSupportedFixedVideoOutputConfigurationCount (1) g_DisplayNumSupportedResolutions = %d\n", 
        g_DisplayNumSupportedResolutions);

    return (mpe_Error) MPE_SUCCESS;
}


/* DSExt */
/* See API doc (mpeos_disp.h) */
void populateVideoOutputConfigInfo()
{
    int i,ii;
    mpe_Bool isSDSupported = FALSE;

    mpe_DispFixedVideoOutputConfigInfo* pConfig = NULL;

    if (g_DisplayNumSupportedResolutions == 0 || g_pDisplaySupportedResolutions == NULL)
    {
        gFixedOutputConfigCount = N_FIXED_CONFIGS;
        gVideoOutputConfigInfo.fixedConfigs = gFixedConfigArray;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "populateVideoOutputConfigInfo (1) gFixedOutputConfigCount = %d\n", gFixedOutputConfigCount);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "populateVideoOutputConfigInfo (1) gVideoOutputConfigInfo.fixedConfigs = %x\n", gVideoOutputConfigInfo.fixedConfigs);
        return;
    }

    // DSJ
	for ( i =0; i < N_FIXED_CONFIGS ; i ++)
	{
		pConfig = gFixedConfigArray + i;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "config %d resolution: (%d, %d)\n", i, 
            pConfig->resolution->aspectRatio.width, pConfig->resolution->aspectRatio.height);

        for (ii=0; ii<g_DisplayNumSupportedResolutions; ii++)
        {
            if (((float)g_pDisplaySupportedResolutions[ii].width / (float)g_pDisplaySupportedResolutions[ii].height) ==
                ((float)pConfig->resolution->aspectRatio.width / (float)pConfig->resolution->aspectRatio.height))
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "supported resolution %d: (%d, %d): MATCH\n", ii, g_pDisplaySupportedResolutions[ii].width,
                    g_pDisplaySupportedResolutions[ii].height);

                if (i==0) isSDSupported = TRUE;

                gFixedOutputConfigCount++;
            }
            else
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "supported resolution %d: (%d, %d): NO MATCH\n", ii, g_pDisplaySupportedResolutions[ii].width,
                    g_pDisplaySupportedResolutions[ii].height);
            }
        }
	}

    if (isSDSupported)
    {
        gVideoOutputConfigInfo.fixedConfigs = gFixedConfigArray;    
    }
    else
    {
        gVideoOutputConfigInfo.fixedConfigs = &(gFixedConfigArray[1]);    
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "populateVideoOutputConfigInfo gFixedOutputConfigCount = %d\n", gFixedOutputConfigCount);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "populateVideoOutputConfigInfo gVideoOutputConfigInfo.fixedConfigs = %x\n", gVideoOutputConfigInfo.fixedConfigs);

    return;
}


// DSJ reacting to the change, the pointer passed now points to allocated memory
/* DSExt */
/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetSupportedFixedVideoOutputConfigurations(mpe_DispOutputPort port,	mpe_DispFixedVideoOutputConfigInfo** ptrptrToArray)
{
	int i;
	mpe_DispOutputPortInfo p;

	if (mpeos_dispGetOutputPortInfo(port, &p) != MPE_SUCCESS)
	{
		return MPE_DISP_ERROR_INVALID_PARAM;
	}

	for ( i =0; i < N_FIXED_CONFIGS ; i ++)
	{
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP, "No display info for supported resolutions, so omitting intersection\n");
        ptrptrToArray[i] = (p.fixedConfigInfo->fixedConfigs) +i ;
	}

	return MPE_SUCCESS;
}

/* DSExt */
/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispGetCurVideoOutputConfiguration(mpe_DispOutputPort port,
        mpe_DispVideoConfig* handle)
{
    mpe_DispOutputPortInfo p;
    if (mpeos_dispGetOutputPortInfo(port, &p) != MPE_SUCCESS)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    *handle = p.fixedConfigInfo->curConfig;

    return (mpe_Error) MPE_SUCCESS;
}

/* DSExt */
/* See API doc (mpeos_disp.h) */
mpe_Error mpeos_dispSetCurVideoOutputConfiguration(mpe_DispOutputPort port,
        mpe_DispVideoConfig handle)
{
    mpe_DispOutputPortInfo p;
    if (mpeos_dispGetOutputPortInfo(port, &p) != MPE_SUCCESS)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    p.fixedConfigInfo->curConfig = handle;

    return MPE_SUCCESS;
}

/**
 * Platform calls this callback function to post display connect/disconnect events to the ED queue.
 **/
void postDisplayConnectDisconnectEvent(ri_bool connected, void* video_output_port_handle)
{
	if(gDeviceSettingsDisplayQueueIdRegistered == TRUE)
	{
		int eventCode = connected ? MPE_DISP_EVENT_CONNECTED : MPE_DISP_EVENT_DISCONNECTED;
		mpeos_eventQueueSend(gDeviceSettingsDisplayQueueId, eventCode, video_output_port_handle, gDeviceSettingsACT, 0);
	}
}
/**
 * <i>mpeos_dispRegister()</i>
 *
 * Register to receive asynchronous display events.
 * NOTE: Only 1 async event listener is supported.  So subsequent calls
 *       to mpeos_dispRegister() will override the previous call (ie, the
 *       previously registered listener will then never be called again).
 *
 * @param queueId the ID of the queue to be used for notification events
 * @param handle the Event Dispatcher handle (as the asynchronous completion token)
 *
 * @return MPE_SUCCESS if successful; other error code if not
 */
mpe_Error mpeos_dispRegister(mpe_EventQueue queueId, void *handle)
{
    gDeviceSettingsDisplayQueueId = queueId;
    gDeviceSettingsACT = handle;
    gDeviceSettingsDisplayQueueIdRegistered = TRUE;
    //register the callback with the "backpanel"
   	ri_get_backpanel()->setVideoOutputPortConnectDisconnectCallback(postDisplayConnectDisconnectEvent);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_dispUnregister()</i>
 *
 * Unregister from receiving asynchronous Display events.
 *
 * @param queueId the ID of the queue that was used for notification events
 * @param handle     the Event Dispatcher handle that was used for notification events
 *
 * @return MPE_SUCCESS if successful; other error code if not
 */
mpe_Error mpeos_dispUnregister(mpe_EventQueue queueId, void *handle)
{
    gDeviceSettingsDisplayQueueIdRegistered = FALSE;
    gDeviceSettingsACT = NULL;
   	ri_get_backpanel()->setVideoOutputPortConnectDisconnectCallback(NULL);
    return MPE_SUCCESS;
}

/**
 * see mpeos_disp.h
 *
 *
 */

//mpe_Error mpeos_dispIsDynamicConfigurationSupported(mpe_DispOutputPort port, mpe_Bool* supported)
//{
//  mpe_DispOutputPortInfo *p = findOutputPort(port);
//  if (NULL == p)
//  {
//      return (mpe_Error)MPE_DISP_ERROR_INVALID_PARAM;
//  }
//
//  if(p->fixedConfigInfo->dynamicConfigs != 0)
//  {
//      *supported = TRUE;
//  }
//  else
//  {
//      *supported = FALSE;
//  }
//
//  return (mpe_Error)MPE_SUCCESS;
//}

mpe_Error mpeos_dispGetSupportedDynamicVideoOutputConfigurationCount(
        mpe_DispOutputPort port, uint32_t* count)
{
    *count = N_DYNAMIC_CONFIGS;
    return (mpe_Error) MPE_SUCCESS;
}

mpe_Error mpeos_dispGetSupportedDynamicVideoOutputConfigurations(
        mpe_DispOutputPort port,
        mpe_DispDynamicVideoOutputConfigInfo** ptrptrToArray)
{
	int i;
    mpe_DispOutputPortInfo p;
    if (mpeos_dispGetOutputPortInfo(port, &p) != MPE_SUCCESS)
    {
        return (mpe_Error) MPE_DISP_ERROR_INVALID_PARAM;
    }

    // DSJ
	for ( i =0; i < N_DYNAMIC_CONFIGS ; i ++)
	{
		ptrptrToArray[i] = (p.fixedConfigInfo->dynamicConfigs) +i ;
	}



    return (mpe_Error) MPE_SUCCESS;
}

