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

#ifndef _MPEOS_DISP_H_
#define _MPEOS_DISP_H_

#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_gfx.h>
#include <mpeos_snd.h>

/**
 *  Display related events
 *
 *  Events are sent through the ED mechanism.  It is required that an event counter
 *  be sent as part of the event.  This allows the java code to synchronize updates.
 *  The counter can roll over as long as any two events in sequence have different
 *  numbers.
 *
 *  The Java code expects an ED event in this form:
 *
 *  asyncEvent( int eventCode, int videoPortHandle, int eventCounter )
 *
 */
typedef enum
{
    MPE_DISP_EVENT_CONNECTED = 0x2500, /* display connected to video port */
    MPE_DISP_EVENT_DISCONNECTED, /* display disconnected from video port */
    MPE_DISP_EVENT_RESOLUTION, /* resolution of display changed  -- NOTE: THIS EVENT IS DEPRECATED!! (SEE IT427) */
    MPE_DISP_EVENT_SHUTDOWN,
/* close down this queue */
} mpe_DispEvent;

/***************************************************************
 * DISPLAY Handles
 ***************************************************************/

/**
 * Screen handle.
 * Serves as an abstraction for an actual display screen, which
 * supports 1 or more screen devices (e.g., video, graphics, and/or
 * background).
 */
typedef struct
{
    int unused1;
}*mpe_DispScreen;

/**
 * Screen device handle.
 * Serves as an abstraction for a screen device, belonging to
 * a single screen.
 * May be one of video, graphics, or background type.
 */
typedef struct
{
    int unused1;
}*mpe_DispDevice;

/**
 * Screen device configuration handle.
 * Serves as an abstraction for a discrete configuration for a
 * screen device.
 */
typedef struct
{
    int unused1;
}*mpe_DispDeviceConfig;

/**
 * Screen device coherent configurations handle.
 * Serves as an abstraction for a coherent set of configurations
 * for multiple devices.  A coherent set of configurations
 * is a set of configurations that can be used together.
 */
typedef struct
{
    int unused1;
}*mpe_DispCoherentConfig;

/**
 * Display (Video Output) port handle.
 */
typedef struct
{
    int unused1;
}*mpe_DispOutputPort;

/**
 * Background Image handle.
 */
typedef struct
{
    int unused1;
}*mpe_DispBGImage;

/**
 * handle to video config
 */
typedef struct
{
    int unused1;
}*mpe_DispVideoConfig;

/**
 * Enumeration describing the destination type of an <code>mpe_DispDevice</code>.
 * Used by media manager to distinguish a TV(mainwin) from a VIDEO(PIPwin)
 * destination by device handle.
 */
typedef enum
{
    MPE_DISPLAY_DEST_TV = 0, MPE_DISPLAY_DEST_VIDEO = 1, MPE_DISPLAY_DEST_UNKNOWN = -1
} mpe_DispDeviceDest;


/***************************************************************
 * DISPLAY public data structures
 ***************************************************************/

/**
 * Error codes.
 */
/* TODO: Look at replacing this directly w/ mpe_Error if it is made to support the necessary errors (mainly INVALID). */
typedef enum mpe_DispError
{
    MPE_DISP_ERROR_NO_ERROR = MPE_SUCCESS, MPE_DISP_ERROR_FIRST_ERROR = 0, /**< TODO: Should modify to be base of display errors */

    MPE_DISP_ERROR_IMPACTS_OTHERS, /**< configuration setting implicitly affected other devices */
    MPE_DISP_ERROR_UNKNOWN, /**< a generic error */
    MPE_DISP_ERROR_NO_MEMORY, /**< out of memory */
    MPE_DISP_ERROR_INVALID_PARAM, /**< invalid parameters passed to function */
    MPE_DISP_ERROR_UNIMPLEMENTED, /**< unimplemented support */
    MPE_DISP_ERROR_BAD_IFRAME, /**< i-frame was rejected */
    MPE_DISP_ERROR_BGCOLOR, /**< problems encountered setting bg color */
    MPE_DISP_ERROR_NOT_AVAILABLE,
/**< for this device, this functionality is not available */

} mpe_DispError;

/**
 * Enumeration describing the type of an <code>mpe_DispDevice</code>.
 * The <code>MPE_DISPLAY_ALL</code> is not an actual device type, but
 * is used as input to <code>mpeos_dispGetDevices()</code> and
 * <code>mpeos_dispGetDeviceCount()</code>.
 */
typedef enum
{
    MPE_DISPLAY_ALL_DEVICES = 0,
    MPE_DISPLAY_GRAPHICS_DEVICE,
    MPE_DISPLAY_VIDEO_DEVICE,
    MPE_DISPLAY_BACKGROUND_DEVICE,
} mpe_DispDeviceType;

/**
 * Enumeration describing the type of an <code>mpe_DispOutputPort</code>.
 */
typedef enum
{
    MPE_DISPLAY_RF_PORT = 0,
    MPE_DISPLAY_BASEBAND_PORT,
    MPE_DISPLAY_SVIDEO_PORT,
    MPE_DISPLAY_1394_PORT,
    MPE_DISPLAY_DVI_PORT,
    MPE_DISPLAY_COMPONENT_PORT,
    MPE_DISPLAY_HDMI_PORT,
    MPE_DISPLAY_INTERNAL_PORT
} mpe_DispOutputPortType;

/**
 * Enumeration describing the options that can be get/set with
 * <code>dispSetVideoOutputPortOption</code> and <code>dispGetVideoOutputPortOption</code>.
 */
typedef enum
{
    MPE_DISP_1394_SELECT_JACK,
    MPE_DISP_1394_DEVICE_LIST,
    MPE_DISP_1394_MODEL_NAME,
    MPE_DISP_1394_VENDOR_NAME,
    MPE_DISP_1394_SUBUNIT_TYPE,
    MPE_DISP_1394_SELECT_SINK
} mpe_DispOutputPortOptionName;

/**
 * Enumeration describing the values that can be used in the <code>value</code>
 * field of a <code>mpe_DispOutputPortOption</code>.
 */
typedef enum
{
    MPE_DISP_1394_SELECT_JACK_TYPE_ANALOG,
    MPE_DISP_1394_SELECT_JACK_TYPE_DIGITAL
} mpe_DispOutputPortOptionValue;

/**
 * Structure used to define the device information for a single/specific IEEE-1394 device.
 */
typedef struct
{
    char eui64[8];
    char vendor[128];
    char model[128];
    int subunitType;
} mpe_Disp1394DeviceInfo;

/**
 * Structure used to define the IEEE-1394 device information array.
 */
typedef struct
{
    int infoCount;
    mpe_Disp1394DeviceInfo infoArray[1];
} mpe_Disp1394Devices;

/**
 * Structure used when calling <code>dispSetVideoOutputPortOption</code>
 * and <code>dispGetVideoOutputPortOption</code>.  <code>option</code> is the
 * name of the value to get or set.  <code>value</code> contains the value to
 * set or the retrieved value.
 */
typedef struct
{
    mpe_DispOutputPortOptionName option;
    void *value;
} mpe_DispOutputPortOption;

/**
 * Enumeration constants that represent the various types of decoder-format-conversions
 */
typedef enum
{
    MPE_DFC_PROCESSING_NONE = 0,
    MPE_DFC_PROCESSING_FULL,
    MPE_DFC_PROCESSING_LB_16_9,
    MPE_DFC_PROCESSING_LB_14_9,
    MPE_DFC_PROCESSING_CCO,
    MPE_DFC_PROCESSING_PAN_SCAN,
    MPE_DFC_PROCESSING_LB_2_21_1_ON_4_3,
    MPE_DFC_PROCESSING_LB_2_21_1_ON_16_9,
    MPE_DFC_PLATFORM,
    MPE_DFC_PROCESSING_16_9_ZOOM,
    MPE_DFC_PROCESSING_PILLARBOX_4_3 = 100,
    MPE_DFC_PROCESSING_WIDE_4_3 = 101,
    MPE_DFC_PROCESSING_UNKNOWN = -1
} mpe_DispDfcAction;

typedef enum _mpe_DispStereoscopicMode
{
    MPE_SSMODE_UNKNOWN = 0,
    MPE_SSMODE_2D = 1,
    MPE_SSMODE_3D_SIDE_BY_SIDE = 3,
    MPE_SSMODE_3D_TOP_AND_BOTTOM = 4
} mpe_DispStereoscopicMode;

/**
 * Represents an area in normalized screen coordinates.
 */
typedef struct
{
    float x;
    float y;
    float width;
    float height;
} mpe_DispScreenArea;

/**
 * Structure which provides fields for describing a screen.
 *
 * @see #mpeos_dispGetScreenInfo
 */
typedef struct
{
    /**
     * If zero, then there is no video <i>not-contributing</i> configuration.
     * If non-zero, then the video <i>not-contributing</i> configuration.
     * This configuration should be referenced by video devices when a
     * non-contributing configuration is necessary.
     * <p>
     * While this information is acquired for a given screen, it should
     * not be different for any given screen.  This is because
     * it is expected that the need for a <i>not-contributing</i> configuration
     * is global to all video devices.
     */
    mpe_DispDeviceConfig notContributing;
} mpe_DispScreenInfo;

/**
 * Structure which provides fields for describing a device.
 *
 * @see #mpeos_dispGetDeviceInfo
 */
typedef struct
{
    /** The type of this device. */
    mpe_DispDeviceType type;

    /** Identification string associated with this device. */
    const char* idString;

    /** Parent screen for this device. */
    mpe_DispScreen screen;

    /** The screen aspect ratio for this device.  Cached representation of current config. */
    mpe_GfxDimensions screenAspectRatio;

} mpe_DispDeviceInfo;

/**
 * Structure which provides fields for describing a device configuration.
 * <p>
 * If the associated configuration is the <i>not-contributing</i>
 * configuration then all values are expected to be zero.
 *
 * @see #mpeos_dispGetConfigInfo
 */
typedef struct
{
    /** The device to which this configuration belongs. */
    mpe_DispDevice device;

    /** If <code>TRUE</code> then flicker filter is supported. */
    mpe_Bool flickerFilter;

    /** If <code>TRUE</code> then configuration is interlaced;
     ** if <code>FALSE</code> then configuration is progressive. */
    mpe_Bool interlaced;

    /**
     * The pixel resolution of this configuration.
     * Will be <code>{0,0}</code> if this configuration is the
     * video <i>not-contributing</i> configuration.
     */
    mpe_GfxDimensions resolution;

    /** The pixel aspect ratio of this configuration. */
    mpe_GfxDimensions pixelAspectRatio;

    /** The normalized screen area covered by this configuration. */
    mpe_DispScreenArea area;

    /**
     * For background device configurations only.
     * If <code>true</code> then MPEG I-Frame stills are supported by
     * this configuration.
     */
    mpe_Bool mpegStills;

    /**
     * For background device configurations only.
     * If <code>true</code> then a changeable single color is supported
     * by this configuration.
     */
    mpe_Bool changeableColor;

    mpe_GfxDimensions screenAspectRatio; /* Used instead of one on device. */

} mpe_DispDeviceConfigInfo;

typedef struct
{
    mpe_GfxDimensions pixelResolution;
    mpe_GfxDimensions aspectRatio;
    int frameRate;
    /** If <code>TRUE</code> then configuration is interlaced;
     ** if <code>FALSE</code> then configuration is progressive. */
    mpe_Bool interlaced;
    mpe_DispStereoscopicMode stereoscopicMode;

} mpe_DispVideoResolutionInfo;

typedef struct
{
    mpe_Bool enabled; /* support configurations changing with display connected. */
    const char* idString; /* Identification string associated with this video output config. */
    mpe_DispVideoResolutionInfo* resolution;
} mpe_DispFixedVideoOutputConfigInfo;

typedef struct
{
    mpe_DispVideoResolutionInfo* inputResolution;
    mpe_DispFixedVideoOutputConfigInfo* outputResolution;
} mpe_DispDynamicVideoOutputMapping;

typedef struct
{
    mpe_Bool enabled; /* support configurations changing with display connected. */
    const char* idString; /* Identification string associated with this video output config. */
    mpe_DispDynamicVideoOutputMapping* mappings; /* an array of supported mappings */
    int mappingsCount; /* number of mappings in mappings array */
} mpe_DispDynamicVideoOutputConfigInfo;

/**
 * Structure which provides fields for describing a video output port configuration.
 * <p>
 *
 * DSExt structure
 */
typedef struct
{
    /*
     * intersection of display capabilities and video port capabilities
     * Very dynamic since it can change whenever a new display
     * is connected. fixed configs can be disabled if a display can not handle them.
     * 0...*
     */

    mpe_DispFixedVideoOutputConfigInfo* fixedConfigs; /* array of fixed configs */
    mpe_DispDynamicVideoOutputConfigInfo* dynamicConfigs; /* array of dynamic configs */

    /* what is being used now */
    mpe_DispVideoConfig curConfig;

} mpe_DispVideoOutputConfigInfo;

/**
 * Structure which provides fields for describing a display's attributes.
 * <p>
 *
 * DSExt structure
 */
typedef struct
{
    mpe_GfxDimensions aspectRatio;
    const char* manufacturerName;
    uint8_t manufactureWeek;
    uint8_t manufactureYear;
    uint16_t productCode;
    uint32_t serialNumber;
} mpe_DispVideoDisplayAttrInfo;

/**
 * Structure which provides fields for describing an video output port.
 *
 * @see #mpeos_dispGetOutputPortInfo
 */
typedef struct
{
    /**
     * Unique identification for this port
     */
    const char* idString;

    /**
     * Specifies the <i>type</i> of the port.
     */
    mpe_DispOutputPortType type;

    /**
     * Specifies whether the output port is enabled or
     * not.  If <code>TRUE</code> then the port was
     * enabled at the time <code>mpeos_dispGetOutputPortInfo()</code>
     * was called.
     */
    mpe_Bool enabled;

    /**
     * Specifies whether DTCP is supported on the port or not.
     * If <code>TRUE</code> then the port supports DTCP.
     */
    mpe_Bool dtcpSupported;

    /**
     * Specifies whether HDCP is supported on the port or not.
     * If <code>TRUE</code> then the port supports HDCP.
     */
    mpe_Bool hdcpSupported;

    /**
     * Specifies the restricted vertical resolution of HD output
     * for the given port.
     * If there is no restriction, then the maximum vertical
     * resolution is specified.
     */
    int32_t restrictedResolution;

    /**
     * handle into the audio port for this video output port
     */
    mpe_SndAudioPort audioPort;

    /**
     * TRUE if this port is connected to a display.
     * FALSE if not connected, or if port does not support connection detection.
     */
    mpe_Bool connected;

    /**
     * Configuration information about his port.
     * An array.
     */
    mpe_DispVideoOutputConfigInfo* fixedConfigInfo;

} mpe_DispOutputPortInfo;

/***************************************************************
 * DISPLAY API functions.
 ***************************************************************/

/**
 * Initialize the display subsystem.
 */
mpe_Error mpeos_dispInit(void);

/**
 * Retrieves the number of supported display screens.
 *
 * @param nScreens Pointer to a <code>uint32_t</code> where, upon successful
 *                 return, the number of supported display screens will
 *                 be written.
 *
 * @return mpe_Error If the screen count cannot be determined.
 *
 * @see mpeos_dispGetScreens
 */
mpe_Error mpeos_dispGetScreenCount(uint32_t *nScreens);

/**
 * Retrieves the set of display screens.
 *
 * @param screens Address of an array to be populated with <code>mpe_DispScreen</code>
 *                handles.
 *                It is assumed that the array is at least big enough to hold
 *                references to all display screens -- that is at least
 *                <code>sizeof(mpe_DispScreen)*nScreens</code> bytes where
 *                <code>nScreens</code> can be retrieved using <code>mpeos_dispGetScreenCount</code>.
 *
 * @return mpe_Error
 *
 * @see mpeos_dispGetScreenCount
 */
mpe_Error mpeos_dispGetScreens(mpe_DispScreen* screens);

/**
 * Retrieves information about the given screen.
 *
 * @param screen the screen to query
 * @param info pointer to a buffer to populate with information about
 *             the given screen
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetScreenInfo(mpe_DispScreen screen,
        mpe_DispScreenInfo* info);

/**
 * Retrieves the number of supported display devices for the given
 * display screen.
 *
 * @param screen the screen to query
 * @param type the type of device for which a count is desired;
 *             if <code>MPE_DISPLAY_ALL_DEVICES</code> then the total
 *             count of all device types for the screen is returned.
 * @param nDevices pointer to <code>uint32_t</code> where nDevices should be returned
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetDeviceCount(mpe_DispScreen screen,
        mpe_DispDeviceType type, uint32_t *nDevices);

/**
 * Retrieves the display devices of the requested type for the given
 * display screen.
 *
 * @param screen the screen to query
 * @param type the type of device(s) to be retrieved;
 *             if <code>MPE_DISPLAY_ALL_DEVICES</code> then all
 *             devices are returned.
 * @param devices pointer to array where devices should be written.
 *             It is assumed that the array is at least big enough to hold
 *             references to all display screens -- that is at least
 *             <code>sizeof(mpe_DispDevice)*nDevices</code> bytes where
 *             <code>nDevices</code> can be retrieved using <code>mpeos_dispGetDeviceCount</code>.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetDevices(mpe_DispScreen screen, mpe_DispDeviceType type,
        mpe_DispDevice* devices);

/**
 * Retrieves information about the given device.
 *
 * @param device the device to query
 * @param info pointer to a structure to fill in with information about
 *             the given device
 */
mpe_Error mpeos_dispGetDeviceInfo(mpe_DispDevice device,
        mpe_DispDeviceInfo* info);

/**
 * Retrieves destination information about the given device.
 *
 * @param device    the device to query
 * @param dest      TV or Video (PIP).  This is equivalent to the background and foreground video device.
 */
mpe_Error mpeos_dispGetDeviceDest(mpe_DispDevice device, mpe_DispDeviceDest* dest);

/**
 * Retrieves the number of supported display device configurations
 * for the given device.
 *
 * @param device the device to query
 * @param nConfigs Pointer to a <code>uint32_t</code> where, upon successful
 *                 return, the number of supported configurations will
 *                 be written.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetConfigCount(mpe_DispDevice device, uint32_t* nConfigs);

/**
 * Retrieves the set of supported display device configurations
 * for the given device.
 *
 * @param device the device to query
 * @param configs Address of an array to be populated with <code>mpe_DispDeviceConfig</code>
 *                handles.
 *                It is assumed that the array is at least big enough to hold
 *                references to all configurations -- that is at least
 *                <code>sizeof(mpe_DispDeviceConfig)*nConfigs</code> bytes where
 *                <code>nConfigs</code> can be retrieved using <code>mpeos_dispGetConfigCount</code>.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetConfigs(mpe_DispDevice device,
        mpe_DispDeviceConfig* configs);

/**
 * Retrieves the currently set configuration for the given device.
 *
 * @param device the device to query
 * @param config Address where, upon successful completion, a reference to
 *               the current configuration will be written.
 */
mpe_Error mpeos_dispGetCurrConfig(mpe_DispDevice device,
        mpe_DispDeviceConfig* config);

/**
 * Sets the current configuration for the given device.
 * If the current configuration is the same as the new configuration,
 * then this operation should have no effect.
 * Upon successful return, the change to the device configuration should be
 * considered complete.
 *
 * <p>
 * If setting this configuration would require changes to other device
 * configurations, then the operation will fail.
 * The error code <code>MPE_DISP_ERROR_IMPACTS_OTHERS</code> will be returned
 * when attempting to set a configuration which would require changes to
 * other device configurations.
 * If this is returned, then it is necessary to find a suitable coherent
 * configuration and use that to change configurations.
 *
 * @param device the device on which to set the current configuration
 * @param config the new current configuration for the device
 *
 * @return mpe_Error
 *
 * @see #mpeos_dispSetCoherentConfig
 */
mpe_Error mpeos_dispSetCurrConfig(mpe_DispDevice device,
        mpe_DispDeviceConfig config);

/**
 * Returns whether setting of this configuration would require
 * configuration modifications to other devices.
 * Essentially returns <code>TRUE</code> if the configurations for
 * the two devices are coherent.
 *
 * @param device the device to query
 * @param config the configuration to test
 * @param device2 the other device to query
 * @param config2 the other configuration to test
 * @param impact pointer to <code>mpe_Bool </code> where results will
 *               be written;
 *               <code>true</code> if configs aren't coherent;
 *               <code>false</code> otherwise
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispWouldImpact(mpe_DispDevice device,
        mpe_DispDeviceConfig config, mpe_DispDevice device2,
        mpe_DispDeviceConfig config2, mpe_Bool *impact);

/**
 * Retrieves information about the given device configuration.
 *
 * @param config the configuration to query
 * @param info pointer to a buffer to populate with information about
 *             the given configuration
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetConfigInfo(mpe_DispDeviceConfig config,
        mpe_DispDeviceConfigInfo* info);

/**
 * Retrieves the number of defined coherent configurations for the
 * given screen.
 *
 * @param screen the screen to query
 * @param nSets Pointer to a <code>uint32_t</code> where, upon successful
 *              return, the number of supported coherent configuration sets will
 *              be written.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetCoherentConfigCount(mpe_DispScreen screen,
        uint32_t* nSets);

/**
 * Retrieves the set of coherent configurations supported by the
 * given screen.
 *
 * @param screen the screen to query
 * @param configs Address of an array to be populated with <code>mpe_DispCoherentConfig</code>
 *                handles.
 *                It is assumed that the array is at least big enough to hold
 *                references to all coherent configuration sets -- that is at least
 *                <code>sizeof(mpe_DispCoherentConfig)*nSets</code> bytes where
 *                <code>nSets</code> can be retrieved using <code>mpeos_dispGetCoherentConfigCount</code>.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetCoherentConfigs(mpe_DispScreen screen,
        mpe_DispCoherentConfig* set);

/**
 * Sets a coherent set of configurations on the given screen.
 * All devices of the screen represented by configurations within
 * the coherent configuration set will have their current configuration
 * set to the representative configuration.
 *
 * @param screen the screen containing the devices to be modified
 * @param set the coherent configuration set
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispSetCoherentConfig(mpe_DispScreen screen,
        mpe_DispCoherentConfig set);
mpe_Error mpeos_dispSetCoherentConfig_Helper(mpe_DispScreen screen,
        mpe_DispCoherentConfig set, int doDisplayInit);

/**
 * Retrieves the number of distinct device configurations represented by
 * the given coherent configuration set.
 *
 * @param set the coherent configuration set to query
 * @param nConfigs Pointer to a <code>uint32_t</code> where, upon successful
 *                 return, the number of represented configurations will
 *                 be written.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetConfigSetCount(mpe_DispCoherentConfig set,
        uint32_t* nConfigs);

/**
 * Retrieves the set of configurations represented by the given
 * coherent configuration set.
 *
 * @param set the coherent configuration set to query
 * @param configs Address of an array to be populated with <code>mpe_DispCoherentConfig</code>
 *                handles.
 *                It is assumed that the array is at least big enough to hold
 *                references to all configurations -- that is at least
 *                <code>sizeof(mpe_DispDeviceConfig)*nConfigs</code> bytes where
 *                <code>nConfigs</code> can be retrieved using <code>mpeos_dispGetConfigSetCount</code>.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetConfigSet(mpe_DispCoherentConfig set,
        mpe_DispDeviceConfig* configs);

/**
 * Sets the background color on the given background device.
 * The alpha channel in the given color is ignored and treated as
 * <code>255</code> (i.e., fully opaque).
 *
 * If the device is currently displaying a still image, then calling this
 * function will clear the still image.
 *
 * @param device the background device
 * @param color the background color
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispSetBGColor(mpe_DispDevice device, mpe_GfxColor color);

/**
 * Retrieves the current background color associated with this
 * background device.
 * The value returned is not guaranteed to be the color set on
 * the last call to <code>mpeos_dispSetBGColor</code>; as it
 * may reflect a reduced-color palette used by the implementation.
 *
 * If the background device is currently displaying a still image,
 * then the color returned has an undefined value.  I.e., it need
 * not match the last call to <code>mpeos_dispSetBGColor</code>.
 *
 * @param device the background device
 * @param color pointer to location where the color should be stored
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetBGColor(mpe_DispDevice device, mpe_GfxColor* color);

/**
 * Create a new <code>mpe_DispBGImage</code> background image.
 * If background image support is provided by a software decoder,
 * then this will decode the MPEG-2 I-Frame and fail given any
 * errors (this is specified so that it is known that the image
 * is decoded only once in software).
 *
 * @param buffer the byte array the contains the MPEG-2 I-Frame
 * @param length the number of bytes in the byte array
 * @param image pointer to location where new <code>mpe_DispBGImage</code>
 *        should be written upon success
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispBGImageNew(uint8_t* buffer, size_t length,
        mpe_DispBGImage* image);

/**
 * Disposes of the specificed <code>mpe_DispBGImage</code>, freeing up any
 * previously allocated resources.
 *
 * @param image the image previously created with <code>mpeos_dispBGImageNew()</code>
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispBGImageDelete(mpe_DispBGImage image);

/**
 * Requests the size of the given <code>mpe_DispBGImage</code>, previously
 * created with <code>mpeos_dispBGImageNew()</code>.
 *
 * @param image the image to query
 * @param size pointer to <code>mpe_GfxDimensions</code> structure where size
 *        is to be written upon success
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispBGImageGetSize(mpe_DispBGImage image,
        mpe_GfxDimensions* size);

/**
 * Instructs the given background device to display the MPEG I-frame specified
 * by the given <code>mpe_DispBGImage</code>.
 * The contents of the MPEG I-Frame buffer should be considered to be
 * copied out of the passed-in object, allowing the caller to free
 * the object upon return.
 * <p>
 * If non-NULL, the given <code>mpe_GfxRectangle</code> should
 * indicate the screen area within which the image be displayed.
 * It is up to the implementation whether to crop, tile, or scale
 * the image.  The positioning of the image is also implementation-
 * dependent.
 * The current background color should be used wherever the image is
 * not displayed.
 *
 * @param device the background device
 * @param image handle representing the MPEG I-Frame
 * @param area if non-NULL, then this specifies the area within which
 * the image is displayed
 */
mpe_Error mpeos_dispDisplayBGImage(mpe_DispDevice device,
        mpe_DispBGImage image, mpe_GfxRectangle *area);

/**
 * Retrieves the number of supported output ports (irrespective
 * of any screen association).
 *
 * @param nPorts pointer to <code>uint32_t</code> where count should
 * be returned
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetOutputPortCount(uint32_t *nPorts);

/**
 * Retrieves the supported video output ports.
 *
 * @param ports pointer to array where handles should be written.
 * It is assumed that the array is at least big enough to hold
 * references to all output ports -- that is at least
 * <code>sizeof(mpe_DispOutputPort)*nPorts</code> bytes where
 * <code>nPorts</code> can be retrieved using <code>mpeos_dispGetOutputPortCount</code>.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetOutputPorts(mpe_DispOutputPort *ports);

/**
 * Used to enable or disable the given output port, based on
 * the value of the <i>enable</i> parameter.
 *
 * @param port the port to control
 * @param enable if <code>TRUE</code> then the port is to be
 * enabled; if <code>FALSE</code> then the port is to be
 * disabled.
 *
 * @param mpe_Error
 */
mpe_Error mpeos_dispEnableOutputPort(mpe_DispOutputPort port, mpe_Bool enable);

/**
 * Used to request information about the given display port.
 *
 * @param port the port to query
 * @param info pointer to a structure where information about
 * the port is to be written
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetOutputPortInfo(mpe_DispOutputPort port,
        mpe_DispOutputPortInfo *info);

/**
 * Retrieve a handle to the screen surface for the given graphics device.
 * This surface should continue to be valid throughout all changes to
 * the graphics device configuration (allowing the surface handle to be constant
 * over the life of the current runtime).
 * Note that changes to the graphics device configuration (in particular,
 * changes regarding pixel resolution) may result in <i>behind-the-scenes</i>
 * changes to the returned surface.  Any such changes should be visible following
 * a configuration change via the {@link mpeos_gfxSurfaceGetInfo} API.
 *
 * @param device the graphics device to retrieve a surface for
 * @param surface the main surface data to return.
 *
 * @return mpe_Error
 */
mpe_Error mpeos_dispGetGfxSurface(mpe_DispDevice device,
        mpe_GfxSurface *surface);

/**
 * Flush the contents of the main surface for the given graphics
 * device (if the surface is buffered).
 * If the surface is not-buffered (or flushes are implicit with each
 * drawing operation), then this routine does nothing.
 *
 * @return MPE_GFX_ERROR_NOERR if successful;
 *         MPE_GFX_ERROR_UNIMPLEMENTED if this routine does nothing;
 *         MPE_GFX_ERROR_OSERR if an error occured in the underlying graphics toolkit
 */
mpe_Error mpeos_dispFlushGfxSurface(mpe_DispDevice device);

/**
 * Enables or disables RF bypass
 *
 * @param enable if <code>TRUE</code> RF bypass is enabled, if
 * <code>FALSE</code> RF bypass is disabled.
 *
 * @return MPE_SUCCESS if RF bypass was successfully disabled or enabled
 */
mpe_Error mpeos_dispSetRFBypassState(mpe_Bool enable);

/**
 * Retrieves the current state of RF bypass.
 *
 * @param state returned value is <code>TRUE</code> if RF bypass is enabled
 * and <code>FALSE</code> if RF bypass is disabled.
 *
 * @return MPE_SUCCESS if RF bypass state is retrieved successfully
 * @return MPE_EINVAL if state is invalid
 */
mpe_Error mpeos_dispGetRFBypassState(mpe_Bool *state);

/**
 * Used set the channel number for RF output.
 *
 * @param channel the channel number for RF output
 *
 * @return MPE_SUCCESS if the RF channel was successfully set
 * @return MPE_EINVAL if the RF channel is not supported
 */
mpe_Error mpeos_dispSetRFChannel(uint32_t channel);

/**
 * Retrieves the channel number the RF output is on.
 *
 * @param channel
 * @return MPE_SUCCESS if the channel number was retrieved
 * @return MPE_EINVAL if channel is invalid
 */
mpe_Error mpeos_dispGetRFChannel(uint32_t *channel);

/**
 * Get the current Decoder Format Conversion (DFC) mode for the given decoder
 *
 * @param decoder is the decoder
 * @param applicationDfc is a pointer filled in by this method with the DFC mode
 *        currently set by the application.   This can be any of the DFC values
 *        defined in org.dvb.media.VideoFormatControl or
 *        org.ocap.media.VideoFormatControl including DFC_PLATFORM.  Note that
 * 		  if applicationDfc = DFC_PLATFORM, this indicates that the DFC mode is
 * 		  controlled by the platform and the exact DFC in use will be returned
 * 		  in the platformDfc parameter.
 * @param platformDfc is a pointer filled in by this method with the DFC mode
 * 		  that is applied by the platform when applicationDfc = DFC_PLATFORM.
 *        In the case where applciationDfc != DFC_PLATFORM, this parameter must
 * 		  still contain the DFC mode that "would be" applied by the platform
 *        based on the current video settings (AR, DAR, AFD).
 *
 * @return error level
 */
mpe_Error mpeos_dispGetDFC(mpe_DispDevice decoder,
        mpe_DispDfcAction *applicationDfc, mpe_DispDfcAction *platformDfc);

/* DSExt */
/**
 * Get the number of supported Decoder Format Conversions (DFC) for the given decoder
 *
 * @param decoder is the decoder
 * @param count filed in with the number of DFCs.  Can be zero.
 *
 * @return error level
 */
mpe_Error mpeos_dispGetSupportedDFCCount(mpe_DispDevice decoder,
        uint32_t* count);

/* DSExt */
/**
 * Get the supported Decoder Format Conversions (DFC) for the given decoder
 *
 * @param decoder is the decoder
 * @param dfcs is a array of DFCs supported by this decorder.  Array is always terminated by
 *        MPE_DFC_PROCESSING_UNKNOWN.
 *
 * @return error level
 */
mpe_Error mpeos_dispGetSupportedDFCs(mpe_DispDevice decoder,
        mpe_DispDfcAction** dfcs);

/**
 * Check whether a given Decoder Format Conversion (DFC) mode is valid for
 * the given decoder
 *
 * @param decoder is the decoder
 * @param action is a DFC
 *
 * @return error if DFC is not valid for this decoder
 */
mpe_Error mpeos_dispCheckDFC(mpe_DispDevice decoder, mpe_DispDfcAction action);

/**
 * Set the active Decoder Format Conversion (DFC) mode for the given decoder
 *
 * @param decoder is the decoder
 * @param action is a DFC
 *
 * @return error level
 */
mpe_Error mpeos_dispSetDFC(mpe_DispDevice decoder, mpe_DispDfcAction action);

/* DSExt */
/**
 * Set the default Decoder Format Conversion (DFC) mode for the given decoder.
 *
 * When the default is not MPE_DFC_PROCESSING_UNKNOWN the default DFC is used as the platform DFC
 * whenever the application DFC is set to MPE_DFC_PLATFORM
 *
 * @param decoder is the decoder
 * @param action is a DFC
 *
 * @return error level
 */
mpe_Error mpeos_dispSetDefaultPlatformDFC(mpe_DispDevice decoder,
        mpe_DispDfcAction action);

/**
 * Sets an option on a video output port.
 *
 * @param port video output port to set an option on.  If <code>NULL</code>,
 * the set operation is done for all output ports of the same type.
 * @param opt option name/value structure containing option name and value
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_INVALID_PARAM if opt is <code>NULL</code>
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported.
 */
mpe_Error mpeos_dispSetVideoOutputPortOption(mpe_DispOutputPort port,
        mpe_DispOutputPortOption *opt);

/**
 * Retrieves the value of an option on a video output port.
 *
 * @param port video output port to get an option on.  If <code>NULL</code>,
 * the get operation is performed for all output ports of the same type.
 * @return MPE_SUCCESS if the option value was retrieved
 * @return MPE_DISP_ERROR_INVALID_PARAM if opt is <code>NULL</code>
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported
 */
mpe_Error mpeos_dispGetVideoOutputPortOption(mpe_DispOutputPort port,
        mpe_DispOutputPortOption *opt);

/* DSExt */

/**
 * Sets the main video output port for an HScreen.
 *
 * DSExt API
 * Not implemented.
 *
 * @param screen the HScreen to associate with <code>port</code>
 * @param port   the VideoOutputPort that is the main video port for the HScreen.
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 */
mpe_Error mpeos_dispSetMainVideoOutputPort(mpe_DispScreen screen,
        mpe_DispOutputPort port);

/**
 * Gets the attributes of the display attached to a video port.
 *
 * DSExt API
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param info if available, the attributes of the display attached to the port, otherwise NULL.
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 * @return MPE_DISP_ERROR_NOT_AVAILABLE if the information is not available for this video port.
 */
mpe_Error mpeos_dispGetDisplayAttributes(mpe_DispOutputPort port,
        mpe_DispVideoDisplayAttrInfo* info);
/**
 * Determines if a display device is attached to a video port.
 *
 * DSExt API
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param connected if available, TRUE or FALSE, otherwise NULL.
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 * @return MPE_DISP_ERROR_NOT_AVAILABLE if the information is not available for this video port.
 */
mpe_Error mpeos_dispIsDisplayConnected(mpe_DispOutputPort port,
        mpe_Bool* connected);

/**
 * Determines if the video content being displayed on a port is encrypted or otherwise protected.
 *
 * DSExt API
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param encrypted if available, TRUE or FALSE, otherwise NULL.
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 * @return MPE_DISP_ERROR_NOT_AVAILABLE if the information is not available for this video port.
 */
mpe_Error mpeos_dispIsContentProtected(mpe_DispOutputPort port,
        mpe_Bool* encrypted);

/**
 *	DSEXT API
 *	not implemented
 *
 *
 *
 */
//mpe_Error mpeos_dispIsDynamicConfigurationSupported(mpe_DispOutputPort port, mpe_Bool* supported);


/**
 * Gets the number in the array of fixed supported configurations.
 *
 * DSExt API
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param count  size of the array
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 */
mpe_Error mpeos_dispGetSupportedFixedVideoOutputConfigurationCount(
        mpe_DispOutputPort port, uint32_t* count);

/**
 * Gets an array of fixed supported configurations.
 *
 * DSExt API
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param ptrArray if supported, an array of supported configurations, otherwise NULL.
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 */
mpe_Error mpeos_dispGetSupportedFixedVideoOutputConfigurations(
        mpe_DispOutputPort port, mpe_DispFixedVideoOutputConfigInfo** ptrArray);

/**
 * Gets the number in the array of dynamic supported configurations.
 *
 * DSExt API
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param count  size of the array
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 */
mpe_Error mpeos_dispGetSupportedDynamicVideoOutputConfigurationCount(
        mpe_DispOutputPort port, uint32_t* count);

/**
 * Gets an array of dynamic supported configurations.
 *
 * DSExt A
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param ptrArray if supported, an array of supported configurations, otherwise NULL.
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 */
mpe_Error mpeos_dispGetSupportedDynamicVideoOutputConfigurations(
        mpe_DispOutputPort port,
        mpe_DispDynamicVideoOutputConfigInfo** ptrArray);

/**
 * Get the current supported configuration.
 *
 * DSExt API
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param currConfig pointer to the field to be set with the current Config.  Current config can be either fixed or dynamic
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 */
mpe_Error mpeos_dispGetCurVideoOutputConfiguration(mpe_DispOutputPort port,
        mpe_DispVideoConfig* currConfig);

/**
 * Sets the current supported configuration.
 *
 * DSExt API
 * Not implemented.
 *
 * @param port the VideoOutputPort
 * @param info the new output config, otherwise NULL.
 *
 * @return MPE_SUCCESS if the option was set successfully
 * @return MPE_DISP_ERROR_UNIMPLEMENTED if the option is not supported by the STB.
 */
mpe_Error mpeos_dispSetCurVideoOutputConfiguration(mpe_DispOutputPort port,
        mpe_DispVideoConfig currConfig);

/**
 * <i>mpeos_dispRegister()</i>
 *
 * DSExt API
 * Not implemented.
 *
 * Register to receive asynchronous display events.
 * NOTE: Only 1 async event listener is supported.  So subsequent calls
 *       to mpeos_cdlRegister() will override the previous call (ie, the
 *       previously registered listener will then never be called again).
 *
 * @param queueId the ID of the queue to be used for notification events
 * @param handle the Event Dispatcher handle (as the asynchronous completion token)
 *
 * @return MPE_SUCCESS if successful; other error code if not
 */
mpe_Error mpeos_dispRegister(mpe_EventQueue queueId, void *handle);

/**
 * <i>mpeos_dispUnregister()</i>
 *
 * DSExt API
 * Not implemented.
 *
 * Unregister from receiving asynchronous display events.
 *
 * @param queueId the ID of the queue that was used for notification events
 * @param handle the Event Dispatcher handle that was used for notification events
 *
 * @return MPE_SUCCESS if successful; other error code if not
 */
mpe_Error mpeos_dispUnregister(mpe_EventQueue queueId, void *handle);

    
#endif /* _MPEOS_DISP_H_ */
