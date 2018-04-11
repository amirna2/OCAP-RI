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
 * This file provides implementations of the MPE OS Closed Captioning API
 * for the CableLabs Reference Implementation.
 */

/* Header Files */

#include <stdio.h>
#include <mpe_types.h>      /* Resolve basic type references. */
#include <mpe_error.h>
#include <mpeos_dbg.h>
#include <mpeos_caption.h>
#include <platform.h>
#include <jvmmgr.h>
#include <stdlib.h>
#include <memory.h>

/*use to Store the current CC attribute value. */
/*During CC Init it will initialized with default value. */
static mpe_CcAttributes mpos_CCSetAttribute[MPE_CC_TYPE_MAX];
/*Internal interface to set default Analog CC attribute value*/
static mpe_Error mpe_ccSetAttributeAnalog(void);
/*Internal interface to set default Digital CC attribute value*/
static mpe_Error mpe_ccSetAttributeDigital(void);

/**
 *
 Constant Arrays maintained for Attribute Values
 *
 **/

#define MPE_CC_COLOR_SIZE     9
#define MPE_CC_OPACITY_SIZE   4
#define MPE_CC_BORDER_SIZE    6
#define MPE_CC_TEXTSTYLE_SIZE 2
#define MPE_CC_FONTSTYLE_SIZE 9
#define MPE_CC_FONTSIZE_SIZE  3

// Color attribute values
typedef struct ColorCapability
{
    const uint32_t rgb;
    const char* name;
} ColorCapability;
ColorCapability Color[MPE_CC_COLOR_SIZE] = 
{
    { MPE_CC_COLOR(0, 0, 0), "Black" },
    { MPE_CC_COLOR(255, 255, 255), "White" },
    { MPE_CC_COLOR(0, 0, 255), "Blue" },
    { MPE_CC_COLOR(255, 255, 0), "Yellow" },
    { MPE_CC_COLOR(255, 200, 0), "Orange" },
    { MPE_CC_COLOR(255, 0, 0), "Red" },
    { MPE_CC_COLOR(0, 255, 0), "Green" },
    { MPE_CC_COLOR(255, 0, 255), "Magenta" },
    { MPE_CC_COLOR(128, 128, 128), "Gray" }
};
static ColorCapability* getValidColor(uint32_t rgb)
{
    int i;
    for (i = 0; i < MPE_CC_COLOR_SIZE; i++)
    {
        if (Color[i].rgb == rgb)
            return &Color[i];
    }
    return NULL;
}

// Font attribute values
typedef struct FontCapability
{
    const char* name;
} FontCapability;
FontCapability FontStyle[MPE_CC_FONTSTYLE_SIZE] = 
{
    { "Arial 10pt Bold" },
    { "Monospaced Serif" },
    { "Proportional Serif" },
    { "Monospaced SansSerif" },
    { "Proportional SansSerif" },
    { "Casual" },
    { "Cursive" },
    { "Small Capitals" },
    { "Courier 12pt Reg" }
};
static mpe_Bool isValidFontStyle(mpe_CcFontStyle name)
{
    int i;
    for (i = 0; i < MPE_CC_FONTSTYLE_SIZE; i++)
    {
        if (strcmp(FontStyle[i].name,name) == 0)
            return TRUE;
    }
    return FALSE;
}


//Values for Attribute MPE_CC_ATTRIB_FONT_SIZE
const mpe_CcFontSize FontSize[MPE_CC_FONTSIZE_SIZE] =
{
    MPE_CC_FONT_SIZE_SMALL,
    MPE_CC_FONT_SIZE_STANDARD,
    MPE_CC_FONT_SIZE_LARGE
};
static mpe_Bool isValidFontSize(mpe_CcFontSize size)
{
    int i;
    for (i = 0; i < MPE_CC_FONTSIZE_SIZE; i++)
    {
        if (FontSize[i] == size)
            return TRUE;
    }
    return FALSE;
}

// Values for Attribute MPE_CC_ATTRIB_FONT_OPACITY,
// MPE_CC_ATTRIB_BACKGROUND_OPACITY, MPE_CC_ATTRIB_WIN_OPACITY
const mpe_CcOpacity Opacity[MPE_CC_OPACITY_SIZE] =
{
    MPE_CC_OPACITY_TRANSPARENT,
    MPE_CC_OPACITY_TRANSLUCENT,
    MPE_CC_OPACITY_SOLID,
    MPE_CC_OPACITY_FLASHING
};
static mpe_Bool isValidOpacity(mpe_CcOpacity opacity)
{
    int i;
    for (i = 0; i < MPE_CC_OPACITY_SIZE; i++)
    {
        if (Opacity[i] == opacity)
            return TRUE;
    }
    return FALSE;
}

// Values for  Attribute MPE_CC_ATTRIB_BORDER_TYPE
const mpe_CcBorderType BorderType[MPE_CC_BORDER_SIZE] =
{
    MPE_CC_BORDER_TYPE_NONE,
    MPE_CC_BORDER_TYPE_RAISED,
    MPE_CC_BORDER_TYPE_DEPRESSED,
    MPE_CC_BORDER_TYPE_UNIFORM,
    MPE_CC_BORDER_TYPE_SHADOW_LEFT,
    MPE_CC_BORDER_TYPE_SHADOW_RIGHT
};
static mpe_Bool isValidBorderType(mpe_CcBorderType border)
{
    int i;
    for (i = 0; i < MPE_CC_BORDER_SIZE; i++)
    {
        if (BorderType[i] == border)
            return TRUE;
    }
    return FALSE;
}

//Values for MPE_CC_ATTRIB_FONT_ITALIC,    MPE_CC_ATTRIB_FONT_UNDERLINE
const mpe_CcTextStyle TextStyle[MPE_CC_TEXTSTYLE_SIZE] =
{
    MPE_CC_TEXT_STYLE_TRUE,
    MPE_CC_TEXT_STYLE_FALSE
};
static mpe_Bool isValidTextStyle(mpe_CcTextStyle style)
{
    int i;
    for (i = 0; i < MPE_CC_TEXTSTYLE_SIZE; i++)
    {
        if (TextStyle[i] == style)
            return TRUE;
    }
    return FALSE;
}

static struct
{
    mpe_CcState             state;
    mpe_CcAnalogServiceMap  enabledAnalog;
    mpe_CcDigitalServiceMap enabledDigital;
}
caption_emu;

static const uint32_t caption_supported_analog_services[] =
{
    MPE_CC_ANALOG_SERVICE_CC1,
    MPE_CC_ANALOG_SERVICE_CC2,
    MPE_CC_ANALOG_SERVICE_CC3,
    MPE_CC_ANALOG_SERVICE_CC4,
    MPE_CC_ANALOG_SERVICE_T1,
    MPE_CC_ANALOG_SERVICE_T2,
    MPE_CC_ANALOG_SERVICE_T3,
    MPE_CC_ANALOG_SERVICE_T4
};

static const uint32_t caption_supported_digital_services[] =
{
     1,  2,  3,  4,  5,  6,  7,
     8,  9, 10, 11, 12, 13, 14,
    15, 16, 17, 18, 19, 20, 21,
    22, 23, 24, 25, 26, 27, 28,
    29, 30, 31, 32, 33, 34, 35,
    36, 37, 38, 39, 40, 41, 42,
    43, 44, 45, 46, 47, 48, 49,
    50, 51, 52, 53, 54, 55, 56,
    57, 58, 59, 60, 61, 62, 63
};

/**
 * This function initialize the MPE closed captioning manager.
 *
 * @return mpe_Error    MPE_SUCCESS if the call is successful.
 *                      MPE_CC_OSERR if an error occurs from the OS
 *
 * @Note: Powertv default is to use embedded attribute values fro EIA 708 and 608-B
 *        Also by default no CC service is selected
 *
 * @see
 */

mpe_Error mpeos_ccInit(void)
{
    /*Store the return value*/

    mpe_CcError mpe_CcReturnVal = MPE_CC_ERROR_NONE;
    /*Set default attribute value for CC type analog*/
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
            "%s: Setting Default Analog CC Attribute\n", __FUNCTION__);
    if (MPE_CC_ERROR_NONE != mpe_ccSetAttributeAnalog())
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                "%s: Failed to set Default Analog CC Attribute\n", __FUNCTION__);
        mpe_CcReturnVal = MPE_CC_ERROR_OS;
        return mpe_CcReturnVal;
    }
    /*Set default attribute value for CC type digital*/
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
            "%s: Setting Default Digital CC Attribute\n", __FUNCTION__);
    if (MPE_CC_ERROR_NONE != mpe_ccSetAttributeDigital())
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                "%s: Failed to set Default Digital CC Attribute\n",
                __FUNCTION__);
        mpe_CcReturnVal = MPE_CC_ERROR_OS;
        return mpe_CcReturnVal;
    }
    /*return MPE_CC_ERROR_OS;*/
    return mpe_CcReturnVal;
}

/**
 * This function sets the user specified attributes for displaying the closed captioning text.
 *
 * These attributes are set for Digital or Analog CC depending of ccType parameter
 *
 * @param attrib    Pointer to a mpe_CcAttributes data structure.
 *                  This data structure contains user preferences for displaying the CC text.
 *                  If an attribute is set to USE_EMBEDDED, the value embedded in the CC data
 *                  stream will be used rather than the user specific.
 *                  If attrib is NULL all the attributes are reset to embedded values.
 * @param type      Represents which attributes to set. Multiple attributes can be set at once
 * @param ccType    Type of closed captioning the attributes are set for.
 *
 * @return mpe_Error    MPE_SUCCESS if the call is successful.
 *                      MPE_CC_OSERR if an error occurs from the OS
 *
 * @see #mpeos_ccGetAttributes
 */
mpe_Error mpeos_ccSetAttributes(mpe_CcAttributes *attrib, uint16_t type,
        mpe_CcType ccType)
{
    mpe_CcError mpe_CcReturnVal = MPE_CC_ERROR_NONE;

    /*Check the input arguments*/
    if ((NULL == attrib) || ((ccType != MPE_CC_TYPE_ANALOG) && (ccType
            != MPE_CC_TYPE_DIGITAL)))
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Invalid Input argument\n",
                __FUNCTION__);
        mpe_CcReturnVal = MPE_CC_ERROR_INVALID_PARAM;
        return mpe_CcReturnVal;
    }
    /*Set the attribute value*/
    ColorCapability* color;
    switch (type)
    {
    case MPE_CC_ATTRIB_FONT_OPACITY: /*Setting front opacity*/
        mpos_CCSetAttribute[ccType].charFgOpacity =
            isValidOpacity(attrib->charFgOpacity) ? attrib->charFgOpacity : MPE_CC_OPACITY_SOLID;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Front Opacity -- %d\n",
                  __FUNCTION__, mpos_CCSetAttribute[ccType].charFgOpacity);
        break;

    case MPE_CC_ATTRIB_BACKGROUND_OPACITY: /*Setting background opacity*/
        mpos_CCSetAttribute[ccType].charBgOpacity =
            isValidOpacity(attrib->charBgOpacity) ? attrib->charBgOpacity : MPE_CC_OPACITY_SOLID;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Background Opacity -- %d\n",
                  __FUNCTION__, mpos_CCSetAttribute[ccType].charBgOpacity);
        break;

    case MPE_CC_ATTRIB_FONT_STYLE: /*Setting front style*/
        if (isValidFontStyle(attrib->fontStyle))
        {
            strncpy(mpos_CCSetAttribute[ccType].fontStyle,
                    attrib->fontStyle, MPE_CC_MAX_FONT_NAME_LENGTH);
        }
        else
        {
            strncpy(mpos_CCSetAttribute[ccType].fontStyle,
                    FontStyle[0].name, MPE_CC_MAX_FONT_NAME_LENGTH);
        }
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Front Style -- %s\n",
                  __FUNCTION__, mpos_CCSetAttribute[ccType].fontStyle);
        break;

    case MPE_CC_ATTRIB_FONT_SIZE: /*Setting front size*/
        mpos_CCSetAttribute[ccType].fontSize =
            isValidFontSize(attrib->fontSize) ? attrib->fontSize : MPE_CC_FONT_SIZE_SMALL;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Front Size -- %d\n",
                  __FUNCTION__, mpos_CCSetAttribute[ccType].fontSize);
        break;

    case MPE_CC_ATTRIB_FONT_ITALIC: /*Setting front italic*/
        mpos_CCSetAttribute[ccType].fontItalic =
            isValidTextStyle(attrib->fontItalic) ? attrib->fontItalic : MPE_CC_TEXT_STYLE_TRUE;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Front Italic -- %d\n",
                  __FUNCTION__, mpos_CCSetAttribute[ccType].fontItalic);
        break;

    case MPE_CC_ATTRIB_FONT_UNDERLINE: /*Setting front underline*/
        mpos_CCSetAttribute[ccType].fontUnderline =
            isValidTextStyle(attrib->fontUnderline) ? attrib->fontUnderline : MPE_CC_TEXT_STYLE_TRUE;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Front Underline -- %d\n",
                  __FUNCTION__, mpos_CCSetAttribute[ccType].fontUnderline);
        break;

    case MPE_CC_ATTRIB_BORDER_TYPE: /*Setting border type*/
        mpos_CCSetAttribute[ccType].borderType =
            isValidBorderType(attrib->borderType) ? attrib->borderType : MPE_CC_BORDER_TYPE_UNIFORM;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Border Type\n",
                __FUNCTION__);
        break;

    case MPE_CC_ATTRIB_BORDER_COLOR: /*Setting border color*/
        if ((color = getValidColor(attrib->borderColor.rgb)) == NULL)
        {
            color = &Color[0];
        }
        mpos_CCSetAttribute[ccType].borderColor.rgb = color->rgb;
        strncpy(mpos_CCSetAttribute[ccType].borderColor.name,
                color->name, MPE_MAX_CC_COLOR_NAME_LENGTH);
        
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Border Color -- %s\n",
                  __FUNCTION__, color->name);
        break;

    case MPE_CC_ATTRIB_WIN_COLOR: /*Setting window color*/
        if ((color = getValidColor(attrib->winColor.rgb)) == NULL)
        {
            color = &Color[0];
        }
        mpos_CCSetAttribute[ccType].winColor.rgb = color->rgb;
        strncpy(mpos_CCSetAttribute[ccType].winColor.name,
                color->name, MPE_MAX_CC_COLOR_NAME_LENGTH);
        
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Window color -- %s\n",
                  __FUNCTION__, color->name);
        break;

    case MPE_CC_ATTRIB_FONT_COLOR: /*Setting foreground color*/
        if ((color = getValidColor(attrib->charFgColor.rgb)) == NULL)
        {
            color = &Color[0];
        }
        mpos_CCSetAttribute[ccType].charFgColor.rgb = color->rgb;
        strncpy(mpos_CCSetAttribute[ccType].charFgColor.name,
                color->name, MPE_MAX_CC_COLOR_NAME_LENGTH);
        
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Foreground color -- %s\n",
                  __FUNCTION__, color->name);
        break;

    case MPE_CC_ATTRIB_BACKGROUND_COLOR: /*Setting background color*/
        if ((color = getValidColor(attrib->charBgColor.rgb)) == NULL)
        {
            color = &Color[0];
        }
        mpos_CCSetAttribute[ccType].charBgColor.rgb = color->rgb;
        strncpy(mpos_CCSetAttribute[ccType].charBgColor.name,
                color->name, MPE_MAX_CC_COLOR_NAME_LENGTH);
        
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Background color -- %s\n",
                  __FUNCTION__, color->name);
        break;

    case MPE_CC_ATTRIB_WIN_OPACITY: /*Setting window opecity*/
        mpos_CCSetAttribute[ccType].winOpacity =
            isValidOpacity(attrib->winOpacity) ? attrib->winOpacity : MPE_CC_OPACITY_SOLID;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Setting Window Opacity -- %d\n",
                  __FUNCTION__, mpos_CCSetAttribute[ccType].winOpacity);
        break;

    default:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Invalid Input Type\n",
                __FUNCTION__);
        mpe_CcReturnVal = MPE_CC_ERROR_INVALID_PARAM;
        break;

    }
    return mpe_CcReturnVal;
}

/**
 * This function returns the current user specified attributes for displaying
 * the closed captioning text depending on the type of CC.
 *
 *
 * @param attrib    Pointer to a mpe_CcAttributes data structure.
 * @param ccType    Type of closed captioning the attributes are set for.
 *
 * @return mpe_Error    MPE_SUCCESS if the call is successful.
 *                      MPE_CC_ERROR_INVALID_PARAM if parameter is invalid
 *
 *
 * @see #mpeos_ccSetAttributes
 */
mpe_Error mpeos_ccGetAttributes(mpe_CcAttributes *attrib, mpe_CcType ccType)
{
    /*MPE_UNUSED_PARAM(attrib);
     MPE_UNUSED_PARAM(ccType);
     return MPE_CC_ERROR_OS;*/
    /*Store the return value*/
    mpe_CcError mpe_CcReturnVal = MPE_CC_ERROR_NONE;

    /*Check the input arguments*/
    if ((NULL == attrib) || ((ccType != MPE_CC_TYPE_ANALOG) && (ccType
            != MPE_CC_TYPE_DIGITAL)))
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Invalid Input argument\n",
                __FUNCTION__);
        mpe_CcReturnVal = MPE_CC_ERROR_INVALID_PARAM;
        return mpe_CcReturnVal;
    }
    /*Check the internal attribute structure*/
    if (NULL == &mpos_CCSetAttribute[ccType])
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                "%s: Invalid Internal attribute structure\n", __FUNCTION__);
        mpe_CcReturnVal = MPE_CC_ERROR_OS;
        return mpe_CcReturnVal;
    }
    else
    {
        /*Store the menber of the internal attribute structure  out put parameter.*/
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_CC,
                "%s: Successfully storing the attribute structure pointer to the output \n",
                __FUNCTION__);
        memcpy(attrib, &mpos_CCSetAttribute[ccType],
                sizeof(mpos_CCSetAttribute[ccType]));
    }
    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_CC,
            "%s: Successfully storing the attribute structure pointer to the output\n",
              __FUNCTION__);
    return mpe_CcReturnVal;
}

/**
 * This function gets the count of analog and digital services combined that support closed captioning.
 *
 * @param count  A pointer used to return the number of closed caption service codes supported by the platform
 *
 * @return mpe_Error	MPE_SUCCESS if the call is successful.
 *						MPE_CC_ERROR_INVALID_PARAM  if the input paramaters are invalid
 *                                                  (ie: NULL)
 *
 */
mpe_Error mpeos_ccGetSupportedServiceNumbersCount(uint32_t *count)
{
    mpe_CcError retval = MPE_CC_ERROR_NONE;

    if (count == NULL)
    {
        retval = MPE_CC_ERROR_INVALID_PARAM;
    }
    else
    {
        uint32_t analog_size  = sizeof(caption_supported_analog_services);
        uint32_t digital_size = sizeof(caption_supported_digital_services);
        uint32_t total_size   = analog_size + digital_size;
        *count = total_size / sizeof(uint32_t);
    }

    return retval;
}

/**
 * This function gets the analog and digital services that support closed captioning.
 *
 * @param serviceArray  The address of a pointer to an array that will be filled in
 *                      with the supported services.
 *
 * @param count On input, this parameter specifies the capacity of servicesArray.
 *                      On output, the parameter indicates the actual number of supported service codes stored in servicesArray
 *
 * @return mpe_Error    MPE_SUCCESS if the call is successful.
 *                      MPE_CC_ERROR_INVALID_PARAM  if the input paramaters are invalid
 *                                                  (ie: NULL)
 *
 */
mpe_Error mpeos_ccGetSupportedServiceNumbers(uint32_t *servicesArray,
        uint32_t *count)
{
    mpe_CcError retval = MPE_CC_ERROR_NONE;

    if (servicesArray == NULL || count == NULL)
    {
        retval = MPE_CC_ERROR_INVALID_PARAM;
    }
    else
    {
        uint32_t analog_size  = sizeof(caption_supported_analog_services);
        uint32_t digital_size = sizeof(caption_supported_digital_services);

        if (((analog_size + digital_size) / sizeof(uint32_t)) != *count)
        {
            retval = MPE_CC_ERROR_INVALID_PARAM;
        }
        else
        {
            uint32_t analog_offset = digital_size / sizeof(uint32_t);
            memcpy(servicesArray, &caption_supported_digital_services[0], digital_size);
            memcpy(servicesArray + analog_offset, &caption_supported_analog_services[0], analog_size);
        }
    }

    return retval;
}

/**
 * This function sets the digital closed captioning services.
 *
 * @param service   An 8 bit value representing the service to be set (1..63).
 *
 * @return mpe_Error    MPE_SUCCESS if the call is successful.
 *                      MPE_CC_OSERR if an error occurs from the OS
 *
 * @see #mpeos_ccGetDigitalServices
 */
mpe_Error mpeos_ccSetDigitalServices(uint32_t service)
{
    int i = 0;

    mpe_CcError retval = MPE_CC_ERROR_INVALID_PARAM;

    if (service == MPE_CC_DIGITAL_SERVICE_NONE)
    {
        caption_emu.enabledDigital = (uint64_t) MPE_CC_DIGITAL_SERVICE_NONE;
        retval = MPE_CC_ERROR_NONE;
    }
    else
    {
        int size = sizeof(caption_supported_digital_services) / sizeof(uint32_t);
        for (i = 0; i < size; i++)
        {
            if (caption_supported_digital_services[i] == service)
            {
                caption_emu.enabledDigital |= (((uint64_t) 1) << service);
                retval = MPE_CC_ERROR_NONE;
            }
        }
    }

    return retval;
}

/**
 * This function returns the digital closed captioning service.
 *
 * @param *service  Pointer to mpe_CcDigitalServiceMap that contains the current service.
 *
 * @return mpe_Error    MPE_SUCCESS if the call is successful.
 *                      MPE_CC_ERROR_INVALID_PARAM if parameter is NULL
 *
 * @see #mpeos_ccSetDigitalServices
 */
mpe_Error mpeos_ccGetDigitalServices(mpe_CcDigitalServiceMap *service)
{
    *service = caption_emu.enabledDigital;
    return MPE_CC_ERROR_NONE;
}

/**
 * This function sets the analog closed captioning service.
 * For example : if service = 0x80  CC1 service is set
 *
 * @param service       An 8 bit value representing the service to be set.
 *
 * @return mpe_Error    MPE_SUCCESS if the call is successful.
 *                      MPE_CC_OSERR if an error occurs from the OS
 *
 * @see #mpeos_ccGetAnalogServices
 */
mpe_Error mpeos_ccSetAnalogServices(uint32_t service)
{
    int i = 0;

    mpe_CcError retval = MPE_CC_ERROR_INVALID_PARAM;

    if (service == MPE_CC_ANALOG_SERVICE_NONE)
    {
        caption_emu.enabledAnalog = MPE_CC_ANALOG_SERVICE_NONE;
        retval = MPE_CC_ERROR_NONE;
    }
    else
    {
        int size = sizeof(caption_supported_analog_services) / sizeof(uint32_t);
        for (i = 0; i < size; i++)
        {
            if (caption_supported_analog_services[i] == service)
            {
                if (service == MPE_CC_ANALOG_SERVICE_CC1)
                {
                    caption_emu.enabledAnalog |= 0x80;
                    retval = MPE_CC_ERROR_NONE;
                }
                else if (service == MPE_CC_ANALOG_SERVICE_CC2)
                {
                    caption_emu.enabledAnalog |= 0x40;
                    retval = MPE_CC_ERROR_NONE;
                }
                else if (service == MPE_CC_ANALOG_SERVICE_CC3)
                {
                    caption_emu.enabledAnalog |= 0x20;
                    retval = MPE_CC_ERROR_NONE;
                }
                else if (service == MPE_CC_ANALOG_SERVICE_CC4)
                {
                    caption_emu.enabledAnalog |= 0x10;
                    retval = MPE_CC_ERROR_NONE;
                }
                else if (service == MPE_CC_ANALOG_SERVICE_T1)
                {
                    caption_emu.enabledAnalog |= 0x08;
                    retval = MPE_CC_ERROR_NONE;
                }
                else if (service == MPE_CC_ANALOG_SERVICE_T2)
                {
                    caption_emu.enabledAnalog |= 0x04;
                    retval = MPE_CC_ERROR_NONE;
                }
                else if (service == MPE_CC_ANALOG_SERVICE_T3)
                {
                    caption_emu.enabledAnalog |= 0x02;
                    retval = MPE_CC_ERROR_NONE;
                }
                else if (service == MPE_CC_ANALOG_SERVICE_T4)
                {
                    caption_emu.enabledAnalog |= 0x01;
                    retval = MPE_CC_ERROR_NONE;
                }
            }
        }
    }

    return retval;
}

/**
 * This function returns the current analog closed captioning services.
 *
 * @param *service  Pointer to mpe_CcAnalogServiceMap that contains the current services.
 *
 * @return mpe_Error    MPE_SUCCESS if the call is successful.
 *                      MPE_CC_ERROR_INVALID_PARAM if parameter is NULL
 *
 * @see #mpeos_ccSetAnalogServices
 */
mpe_Error mpeos_ccGetAnalogServices(mpe_CcAnalogServiceMap *service)
{
    *service = caption_emu.enabledAnalog;
    return MPE_CC_ERROR_NONE;
}

/**
 * This function sets the closed captioning state.
 *
 * @param state the new state of closed captioning:
 * MPE_CC_STATE_ON, MPE_CC_STATE_OFF, MPE_CC_STATE_ON_MUTE
 *
 * @see
 */
mpe_Error mpeos_ccSetClosedCaptioning(mpe_CcState state)
{
    mpe_CcError retval = MPE_CC_ERROR_OS;

    if (state == MPE_CC_STATE_OFF ||
        state == MPE_CC_STATE_ON ||
        state == MPE_CC_STATE_ON_MUTE)
    {
        caption_emu.state = state;
        retval = MPE_CC_ERROR_NONE;
    }
    else
    {
        retval = MPE_CC_ERROR_INVALID_PARAM;
    }

    return retval;
}

/**
 * This function return the capability for a given attribute
 * It hides or show the closed captioning when the cc state is MPE_CC_STATE_ON_MUTE
 *
 * @param
 *
 * @see
 */
mpe_Error mpeos_ccGetCapability(mpe_CcAttribType attrib, mpe_CcType type,
        void* value[], uint32_t *size)
{
    // int i =0;

    mpe_Error Error = MPE_CC_ERROR_NONE;

    // If the attribute is not within the range of CC Attributes Error is returned
    if (((type != MPE_CC_TYPE_ANALOG) && (type != MPE_CC_TYPE_DIGITAL))
            || (NULL == value) || (NULL == size))
    {
        Error = MPE_CC_ERROR_INVALID_PARAM;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                "%s %d %p %u: Invalid Input argument\n", __FUNCTION__, type,
                value, *size);
    }
    else
    {
        int i;

        // Check for the Attribute specified and initialize the value parameter with the Set of
        // values for the specifiedAttribute
        switch (attrib)
        {
        case MPE_CC_ATTRIB_FONT_COLOR:
        case MPE_CC_ATTRIB_BACKGROUND_COLOR:
        case MPE_CC_ATTRIB_BORDER_COLOR:
        case MPE_CC_ATTRIB_WIN_COLOR:

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                      "%s: Returning the Color capability values\n", __FUNCTION__);
            
            // Copy our colors into the given capability array
            for (i = 0; i < MPE_CC_COLOR_SIZE; i++)
            {
                ((mpe_CcColor*)value[i])->rgb = Color[i].rgb;
                strncpy(((mpe_CcColor*)value[i])->name, Color[i].name,
                        MPE_MAX_CC_COLOR_NAME_LENGTH);
            }
            
            *size = MPE_CC_COLOR_SIZE;
            
            break;

        case MPE_CC_ATTRIB_FONT_OPACITY:
        case MPE_CC_ATTRIB_BACKGROUND_OPACITY:
        case MPE_CC_ATTRIB_WIN_OPACITY:

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                      "%s: Returning the Opacity capability values\n", __FUNCTION__);
            
            // Copy our opacity values into the given capability array
            for (i = 0; i < MPE_CC_OPACITY_SIZE; i++)
            {
                *((uint32_t*)value[i]) = Opacity[i];
            }
            
            *size = MPE_CC_OPACITY_SIZE;
            
            break;

        case MPE_CC_ATTRIB_FONT_STYLE:

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                      "%s: Returning the Font Style capbility values\n", __FUNCTION__);
            
            // Copy our font style values into the given capability array
            for (i = 0; i < MPE_CC_FONTSTYLE_SIZE; i++)
            {
                strncpy(*((mpe_CcFontStyle*)value[i]), FontStyle[i].name,
                        MPE_CC_MAX_FONT_NAME_LENGTH);
            }

            *size = MPE_CC_FONTSTYLE_SIZE;
            
            break;

        case MPE_CC_ATTRIB_FONT_SIZE:

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                    "%s: Returning the Font Size capability values\n", __FUNCTION__);

            // Copy our font style values into the given capability array
            for (i = 0; i < MPE_CC_FONTSIZE_SIZE; i++)
            {
                *((uint32_t*)value[i]) = FontSize[i];
            }

            *size = MPE_CC_FONTSIZE_SIZE;//Setting the size to number of Fonts

            break;

        case MPE_CC_ATTRIB_FONT_ITALIC:
        case MPE_CC_ATTRIB_FONT_UNDERLINE:

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                    "%s: Returning the Text Style capability values\n", __FUNCTION__);

            // Copy our text style values into the given capability array
            for (i = 0; i < MPE_CC_TEXTSTYLE_SIZE; i++)
            {
                *((uint32_t*)value[i]) = TextStyle[i];
            }

            *size = MPE_CC_TEXTSTYLE_SIZE; //Setting No of Text Styles

            break;

        case MPE_CC_ATTRIB_BORDER_TYPE:

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                    "%s: Setting the Border Type values\n", __FUNCTION__);

            // Copy our text style values into the given capability array
            for (i = 0; i < MPE_CC_BORDER_SIZE; i++)
            {
                *((uint32_t*)value[i]) = BorderType[i];
            }

            *size = MPE_CC_BORDER_SIZE;//Setting No of Border Types

            break;

        default:
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "%s: Invalid Parameter\n",
                    __FUNCTION__);

            Error = MPE_CC_ERROR_OS;
            break;
        }

    }

    return Error;
}
/**
 * This function returns the current closed captioning state.
 *
 *
 * @param *state pointer to the current closed captioning state value:
 * MPE_CC_STATE_ON, MPE_CC_STATE_OFF, MPE_CC_STATE_ON_MUTE
 *
 * @see
 */
mpe_Error mpeos_ccGetClosedCaptioning(mpe_CcState *state)
{
    *state = MPE_CC_STATE_OFF;
    return MPE_SUCCESS;
}

/**
 * This function default alalog CC attribute.
 @param    None
 */
static mpe_Error mpe_ccSetAttributeAnalog(void)
{
    mpe_CcError mpe_CcReturnVal = MPE_CC_ERROR_NONE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
            "%s: Storing default Analog CC attribute\n", __FUNCTION__);

    /*Set the foreground color (WHITE)*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].charFgColor.rgb = Color[1].rgb;
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].charFgColor.name,
            Color[1].name, MPE_MAX_CC_COLOR_NAME_LENGTH);
    
    /*Setting background color (BLACK)*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].charBgColor.rgb = Color[0].rgb;
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].charBgColor.name,
            Color[0].name, MPE_MAX_CC_COLOR_NAME_LENGTH);

    /*Setting front opacity*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].charFgOpacity = MPE_CC_OPACITY_SOLID;

    /*Setting background opacity*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].charBgOpacity = MPE_CC_OPACITY_SOLID;

    /*Setting front style*/
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].fontStyle,
            FontStyle[0].name, MPE_CC_MAX_FONT_NAME_LENGTH);

    /*Setting front size*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].fontSize = MPE_CC_FONT_SIZE_SMALL;

    /*Setting front italic*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].fontItalic = MPE_CC_TEXT_STYLE_TRUE;

    /*Setting front underline*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].fontUnderline = MPE_CC_TEXT_STYLE_TRUE;

    /*Setting border type*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].borderType = MPE_CC_BORDER_TYPE_UNIFORM;

    /*Setting border color (YELLOW)*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].borderColor.rgb = Color[3].rgb;
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].borderColor.name,
            Color[3].name, MPE_MAX_CC_COLOR_NAME_LENGTH);

    /*Setting window color (BLACK)*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].winColor.rgb = Color[0].rgb;
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].winColor.name,
            Color[0].name, MPE_MAX_CC_COLOR_NAME_LENGTH);

    /*Setting window opecity*/
    mpos_CCSetAttribute[MPE_CC_TYPE_ANALOG].winOpacity = MPE_CC_OPACITY_SOLID;

    return mpe_CcReturnVal;
}

/**
 * This function default digital CC attribute.
 @param    None
 */
static mpe_Error mpe_ccSetAttributeDigital(void)
{
    mpe_CcError mpe_CcReturnVal = MPE_CC_ERROR_NONE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
            "%s: Storing default Digital CC attribute\n", __FUNCTION__);

    /*Set the foreground color (WHITE)*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].charFgColor.rgb = Color[1].rgb;
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].charFgColor.name,
            Color[1].name, MPE_MAX_CC_COLOR_NAME_LENGTH);
    
    /*Setting background color (BLACK)*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].charBgColor.rgb = Color[0].rgb;
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].charBgColor.name,
            Color[0].name, MPE_MAX_CC_COLOR_NAME_LENGTH);

    /*Setting front opacity*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].charFgOpacity = MPE_CC_OPACITY_SOLID;

    /*Setting background opacity*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].charBgOpacity = MPE_CC_OPACITY_SOLID;

    /*Setting front style*/
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].fontStyle,
            FontStyle[0].name, MPE_CC_MAX_FONT_NAME_LENGTH);

    /*Setting front size*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].fontSize = MPE_CC_FONT_SIZE_SMALL;

    /*Setting front italic*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].fontItalic = MPE_CC_TEXT_STYLE_TRUE;

    /*Setting front underline*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].fontUnderline = MPE_CC_TEXT_STYLE_TRUE;

    /*Setting border type*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].borderType = MPE_CC_BORDER_TYPE_UNIFORM;

    /*Setting border color (YELLOW)*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].borderColor.rgb = Color[3].rgb;
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].borderColor.name,
            Color[3].name, MPE_MAX_CC_COLOR_NAME_LENGTH);

    /*Setting window color (BLACK)*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].winColor.rgb = Color[0].rgb;
    strncpy(mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].winColor.name,
            Color[0].name, MPE_MAX_CC_COLOR_NAME_LENGTH);

    /*Setting window opecity*/
    mpos_CCSetAttribute[MPE_CC_TYPE_DIGITAL].winOpacity = MPE_CC_OPACITY_SOLID;

    return mpe_CcReturnVal;
}
