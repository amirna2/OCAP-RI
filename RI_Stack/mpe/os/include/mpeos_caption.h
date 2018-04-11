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

#ifndef _MPEOS_CAPTION_H_
#define _MPEOS_CAPTION_H_

#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_gfx.h> /* for color macros */

/* close captioning specific errors */
typedef enum mpe_CcError
{
    MPE_CC_ERROR_NONE = MPE_SUCCESS, /* no error */
    MPE_CC_ERROR_INVALID_PARAM = MPE_EINVAL, /* invalid parameters passed in MPE functions */
    MPE_CC_ERROR_OS
/* OS failure */
} mpe_CcError;

/* closed captioning states */
typedef enum mpe_CcState
{
    MPE_CC_STATE_OFF, /* turn off CC */
    MPE_CC_STATE_ON, /* turn on  */
    MPE_CC_STATE_ON_MUTE, /* turn on, when audio is muted */
    MPE_CC_STATE_MAX
} mpe_CcState;

/* analog closed captioning services */
typedef enum mpe_CcAnalogServices
{
    MPE_CC_ANALOG_SERVICE_NONE = 0,
    MPE_CC_ANALOG_SERVICE_CC1 = 1000,
    MPE_CC_ANALOG_SERVICE_CC2 = 1001,
    MPE_CC_ANALOG_SERVICE_CC3 = 1002,
    MPE_CC_ANALOG_SERVICE_CC4 = 1003,
    MPE_CC_ANALOG_SERVICE_T1 = 1004,
    MPE_CC_ANALOG_SERVICE_T2 = 1005,
    MPE_CC_ANALOG_SERVICE_T3 = 1006,
    MPE_CC_ANALOG_SERVICE_T4 = 1007
} mpe_CcAnalogServices;

/* digital closed captioning services */
typedef enum mpe_CcDigitalServices
{
    MPE_CC_DIGITAL_SERVICE_NONE = 0
} mpe_CcDigitalServices;

/* type of attributes */
typedef enum mpe_CcAttribType
{
    MPE_CC_ATTRIB_FONT_COLOR = 0x0001,
    MPE_CC_ATTRIB_BACKGROUND_COLOR = 0x0002,
    MPE_CC_ATTRIB_FONT_OPACITY = 0x0004,
    MPE_CC_ATTRIB_BACKGROUND_OPACITY = 0x0008,
    MPE_CC_ATTRIB_FONT_STYLE = 0x0010,
    MPE_CC_ATTRIB_FONT_SIZE = 0x0020,
    MPE_CC_ATTRIB_FONT_ITALIC = 0x0040,
    MPE_CC_ATTRIB_FONT_UNDERLINE = 0x0080,
    MPE_CC_ATTRIB_BORDER_TYPE = 0x0100,
    MPE_CC_ATTRIB_BORDER_COLOR = 0x0200,
    MPE_CC_ATTRIB_WIN_COLOR = 0x0400,
    MPE_CC_ATTRIB_WIN_OPACITY = 0x0800,
    MPE_CC_ATTRIB_MAX
} mpe_CcAttribType;

/* Closed Captioning Opacity */
typedef enum mpe_CcOpacity
{
    MPE_CC_OPACITY_EMBEDDED = -1,
    MPE_CC_OPACITY_TRANSPARENT,
    MPE_CC_OPACITY_TRANSLUCENT,
    MPE_CC_OPACITY_SOLID,
    MPE_CC_OPACITY_FLASHING,
    MPE_CC_OPACITY_MAX
} mpe_CcOpacity;

/* Closed Captioning Font Size */
typedef enum mpe_CcFontSize
{
    MPE_CC_FONT_SIZE_EMBEDDED = -1,
    MPE_CC_FONT_SIZE_SMALL,
    MPE_CC_FONT_SIZE_STANDARD,
    MPE_CC_FONT_SIZE_LARGE,
    MPE_CC_FONT_SIZE_MAX
} mpe_CcFontSize;

/* Closed Captioning Font Style */
#define MPE_CC_MAX_FONT_NAME_LENGTH 128
#define MPE_CC_FONT_STYLE_EMBEDDED "EMBEDDED"
typedef char mpe_CcFontStyle[MPE_CC_MAX_FONT_NAME_LENGTH];

// Maximum number of CC font style capability values
#define MPE_CC_FONT_STYLE_MAX 16

/* Window Border type */
typedef enum mpe_CcBorderType
{
    MPE_CC_BORDER_TYPE_EMBEDDED = -1,
    MPE_CC_BORDER_TYPE_NONE,
    MPE_CC_BORDER_TYPE_RAISED,
    MPE_CC_BORDER_TYPE_DEPRESSED,
    MPE_CC_BORDER_TYPE_UNIFORM,
    MPE_CC_BORDER_TYPE_SHADOW_LEFT,
    MPE_CC_BORDER_TYPE_SHADOW_RIGHT,
    MPE_CC_BORDER_TYPE_MAX
} mpe_CcBorderType;

/* Closed Captioning type */
typedef enum mpe_CcType
{
    MPE_CC_TYPE_ANALOG,
    MPE_CC_TYPE_DIGITAL,
    MPE_CC_TYPE_MAX
} mpe_CcType;

/* Closed Captioning Color definition */
#define MPE_CC_EMBEDDED_COLOR (0xff000000)
#define MPE_CC_COLOR(r,g,b)  ( (((r) & 0xFF) << 16) | (((g) & 0xFF) << 8) | ((b) & 0xFF) )

#define MPE_MAX_CC_COLOR_NAME_LENGTH 32
typedef struct mpe_CcColor
{
    uint32_t rgb; 
    char name[MPE_MAX_CC_COLOR_NAME_LENGTH];
    
} mpe_CcColor;

// Maximum number of CC color capability values
#define MPE_CC_COLOR_MAX 32

typedef enum mpe_CcTextStyle
{
    MPE_CC_TEXT_STYLE_EMBEDDED_TEXT = -1,
    MPE_CC_TEXT_STYLE_FALSE,
    MPE_CC_TEXT_STYLE_TRUE,
    MPE_CC_TEXT_STYLE_MAX
} mpe_CcTextStyle;

/* Closed Captioning Attributes */
typedef struct mpe_CcAttributes
{
    mpe_CcColor charBgColor; /* character background color */
    mpe_CcColor charFgColor; /* character foreground color */
    mpe_CcColor winColor; /* window color */
    mpe_CcOpacity charBgOpacity; /* background opacity */
    mpe_CcOpacity charFgOpacity; /* foreground opacity */
    mpe_CcOpacity winOpacity; /* window opacity */
    mpe_CcFontSize fontSize; /* font size */
    mpe_CcFontStyle fontStyle; /* font style */
    mpe_CcTextStyle fontItalic; /* italicized font */
    mpe_CcTextStyle fontUnderline; /* underlined font */
    mpe_CcBorderType borderType; /* window border type */
    mpe_CcColor borderColor; /* window border color */
} mpe_CcAttributes;

/* Analog Service map */
typedef uint8_t mpe_CcAnalogServiceMap;

/* Digital Service map */
typedef uint64_t mpe_CcDigitalServiceMap;

/**
 * This function sets the user specified attributes for displaying the closed captioning text.
 *
 * These attributes are set for Digital or Analog CC depending of ccType parameter
 *
 * @param attrib	Pointer to a mpe_CcAttributes data structure.
 *                  This data structure contains user preferences for displaying the CC text.
 *                  If an attribute is set to USE_EMBEDDED, the value embedded in the CC data
 *                  stream will be used rather than the user specific.
 *                  If attrib is NULL all the attributes are reset to embedded values.
 * @param type		Represents which attributes to set. Multiple attributes can be set at once
 * @param ccType    Type of closed captioning the attributes are set for.
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_OS if an error occurs from the OS
 *
 * @see #mpeos_ccGetAttributes
 */
mpe_Error mpeos_ccSetAttributes(mpe_CcAttributes *attrib, uint16_t type,
        mpe_CcType ccType);

/**
 * This function returns the current user specified attributes for displaying
 * the closed captioning text depending on the type of CC.
 *
 *
 * @param attrib	Pointer to a mpe_CcAttributes data structure.
 * @param ccType    Type of closed captioning the attributes are set for.
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_INVALID_PARAM if parameter is invalid
 *
 *
 * @see #mpeos_ccSetAttributes
 */
mpe_Error mpeos_ccGetAttributes(mpe_CcAttributes *attrib, mpe_CcType ccType);

/**
 * This function sets the analog closed captioning service.
 * For example : if service = 0x80	CC1 service is set
 *
 * @param service	An 8 bits value representing the service to be set.
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_OS if an error occurs from the OS
 *
 * @see #mpeos_ccGetAnalogServices
 */
mpe_Error mpeos_ccSetAnalogServices(uint32_t service);

/**
 * This function gets the count of analog and digital services combined that support closed captioning.
 *
 * @param count         A pointer used to return the number of closed caption service codes supported by the platform.
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_INVALID_PARAM  if the input paramaters are invalid
 *                                                  (ie: NULL)
 *
 */
mpe_Error mpeos_ccGetSupportedServiceNumbersCount(uint32_t* count);

/**
 * This function gets the analog and digital services that support closed captioning.
 *
 * @param servicesArray	The address of an already-allocated array that will be filled in
 *                      with the supported services.  
 *
 * @param count On input, this parameter specifies the capacity of servicesArray.
 *                      On output, the parameter indicates the actual number of supported service codes stored in servicesArray
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_INVALID_PARAM  if the input paramaters are invalid (ie: NULL) or if servicesArray is not large
 *                      enough to store the list of supported service codes
 *
 */
mpe_Error mpeos_ccGetSupportedServiceNumbers(uint32_t *servicesArray,
        uint32_t *count);

/**
 * This function sets the digital closed captioning services.
 *
 * @param service	An 8 bit values representing the service to be set (1..63). 
 *
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_OS if an error occurs from the OS
 *
 * @see #mpeos_ccGetDigitalServices
 */
mpe_Error mpeos_ccSetDigitalServices(uint32_t service);

/**
 * This function returns the current analog closed captioning services.
 *
 * @param *service	Pointer to mpe_CcAnalogServiceMap that contains the current services.
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_INVALID_PARAM if parameter is NULL
 *
 * @see #mpeos_ccSetAnalogServices
 */
mpe_Error mpeos_ccGetAnalogServices(mpe_CcAnalogServiceMap *service);

/**
 * This function returns the digital closed captioning service.
 *
 * @param *service	Pointer to mpe_CcDigitalServiceMap that contains the current service.
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_INVALID_PARAM if parameter is NULL
 *
 * @see #mpeos_ccSetDigitalService
 */
mpe_Error mpeos_ccGetDigitalServices(mpe_CcDigitalServiceMap *service);

/**
 * This function sets the closed captioning state.
 *
 *
 * @param state the new state of closed captioning:
 * CC_TURNED_ON, CC_TURNED_OFF, CC_TURNED_ON_MUTE
 *
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_OS if an error occurs from the OS
 *
 * @see
 */
mpe_Error mpeos_ccSetClosedCaptioning(mpe_CcState state);

/**
 * This function retrieves Closed Captioning capabilities as an array
 * of integers or colors
 *
 * MPE_CC_ATTRIB_FONT_COLOR, MPE_CC_ATTRIB_BACKGROUND_COLOR,
 * MPE_CC_BORDER_COLOR, MPE_CC_WIN_COLOR:
 *    Attribute value array is an array of mpe_CcColor* of length
 *    MPE_CC_COLOR_MAX
 *
 * MPE_CC_ATTRIB_FONT_OPACITY, MPE_CC_ATTRIB_BACKGROUND_OPACITY,
 * MPE_CC_ATTRIB_WIN_OPACITY,
 *    Attribute value array is an array of uint32_t* of length
 *    MPE_CC_OPACITY_MAX.  Implementations must only provide values
 *    from mpe_CcOpacity 
 *
 * MPE_CC_ATTRIB_FONT_STYLE:
 *    Attribute value array is an array of mpe_ccFontStyle* of length
 *    MPE_CC_FONT_STYLE_MAX.  Implementations must only provide string
 *    values that represent valid closed captioning fonts
 *
 * MPE_CC_ATTRIB_FONT_SIZE:
 *    Attribute value array is an array of uint32_t* of length
 *    MPE_CC_FONT_SIZE_MAX.  Implementations must only provide values
 *    from mpe_CcFontSize 
 *
 * MPE_CC_ATTRIB_FONT_ITALIC, MPE_CC_ATTRIB_FONT_UNDERLINE,
 *    Attribute value array is an array of uint32_t* of length 2.
 *    Implementations may provide up to two values from mpe_CcTextStyle
 *
 * MPE_CC_ATTRIB_BORDER_TYPE,
 *    Attribute value array is an array of uint32_t* of length
 *    MPE_CC_BORDER_TYPE_MAX.  Implementations must only provide values
 *    from mpe_CcBorderType 
 *
 * @param attrib Attribute we want the capabilities for.
 * @param type   Closed captioning type
 * @param value	 An array of pointers to attribute value structures
 *               based on attribute type as described above.
 * @param size	 Upon return, this value must point to the actual number
 *               of capability values returned
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_OS if an error occurs from the OS
 *
 * @see
 */
mpe_Error mpeos_ccGetCapability(mpe_CcAttribType attrib, mpe_CcType type,
                                void* value[], uint32_t *numCapabilities);

/**
 * This function returns current Closed Captioning state
 *
 *
 * @param state  pointer to the current closed captioning state
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_OS if an error occurs from the OS
 *
 * @see mpe_ccSetClosedCaptioning
 */
mpe_Error mpeos_ccGetClosedCaptioning(mpe_CcState *state);

#endif 
