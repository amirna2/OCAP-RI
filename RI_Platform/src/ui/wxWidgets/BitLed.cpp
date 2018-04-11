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
 * BitLed.cpp
 *
 *  Created on: Jun 18, 2009
 *      Author: Mark Millard, enableTv Inc.
 */

// Include system header files.
#ifdef __WXMSW__
#include <gl/gl.h>
#include <gl/glext.h>
#endif /* __WXMSW__ */
#ifdef __WXGTK__
#include <GL/gl.h>
#include <GL/glext.h>
#include <GL/glx.h>
#endif /* __WXX11__ */

// Include RI Emulator header files.
#include "ri_log.h"
#include "BitLed.h"
#include "Base64Codec.h"
#include "ui_frontpanel.h"

// Logging category
#define RILOG_CATEGORY g_uiFrontPanelCat
extern log4c_category_t* g_uiFrontPanelCat;

const int BitLed::LED_HEIGHT = 12;
const int BitLed::LED_WIDTH = 12;
BitLed::ColorIntensityMap BitLed::m_ledImageMap;
Image* BitLed::m_offPicture;
bool BitLed::m_colorIntensityMapLoaded = false;

BitLed::BitLed(ri_led_t *led, ImageMap *imageMap) :
    LedBase(0, 0, LED_WIDTH, LED_HEIGHT), m_imageMap(imageMap), m_led(led)
{
    m_lastColor = RI_FP_NONE;
    m_lastIntensity = MIN_INTENSITY;
    m_lastPicture = NULL;
    m_currentPicture = NULL;

    //int numMetaData = m_imageMap->GetNumMetaData();
    //MetaDataNode *metaData = m_imageMap->GetMetaData();

    // Load the LED images, if needed.
    if (!m_colorIntensityMapLoaded)
    {
        m_offPicture = new Image(GetImageData(RI_FP_BLUE, OFF));
        m_ledImageMap[RI_FP_BLUE][OFF] = m_offPicture;
        m_ledImageMap[RI_FP_BLUE][LOW] = new Image(
                GetImageData(RI_FP_BLUE, LOW));
        m_ledImageMap[RI_FP_BLUE][MEDIUM] = new Image(GetImageData(RI_FP_BLUE,
                MEDIUM));
        m_ledImageMap[RI_FP_BLUE][HIGH] = new Image(GetImageData(RI_FP_BLUE,
                HIGH));
        m_ledImageMap[RI_FP_GREEN][OFF] = m_offPicture;
        m_ledImageMap[RI_FP_GREEN][LOW] = new Image(GetImageData(RI_FP_GREEN,
                LOW));
        m_ledImageMap[RI_FP_GREEN][MEDIUM] = new Image(GetImageData(
                RI_FP_GREEN, MEDIUM));
        m_ledImageMap[RI_FP_GREEN][HIGH] = new Image(GetImageData(RI_FP_GREEN,
                HIGH));
        m_ledImageMap[RI_FP_YELLOW][OFF] = m_offPicture;
        m_ledImageMap[RI_FP_YELLOW][LOW] = new Image(GetImageData(RI_FP_YELLOW,
                LOW));
        m_ledImageMap[RI_FP_YELLOW][MEDIUM] = new Image(GetImageData(
                RI_FP_YELLOW, MEDIUM));
        m_ledImageMap[RI_FP_YELLOW][HIGH] = new Image(GetImageData(
                RI_FP_YELLOW, HIGH));
        m_ledImageMap[RI_FP_ORANGE][OFF] = m_offPicture;
        m_ledImageMap[RI_FP_ORANGE][LOW] = new Image(GetImageData(RI_FP_ORANGE,
                LOW));
        m_ledImageMap[RI_FP_ORANGE][MEDIUM] = new Image(GetImageData(
                RI_FP_ORANGE, MEDIUM));
        m_ledImageMap[RI_FP_ORANGE][HIGH] = new Image(GetImageData(
                RI_FP_ORANGE, HIGH));
        m_ledImageMap[RI_FP_RED][OFF] = m_offPicture;
        m_ledImageMap[RI_FP_RED][LOW] = new Image(GetImageData(RI_FP_RED, LOW));
        m_ledImageMap[RI_FP_RED][MEDIUM] = new Image(GetImageData(RI_FP_RED,
                MEDIUM));
        m_ledImageMap[RI_FP_RED][HIGH] = new Image(
                GetImageData(RI_FP_RED, HIGH));
        m_colorIntensityMapLoaded = TRUE;
    }

    // Set the image location derived from the Image Map.
    GetImageLocation(&m_x, &m_y);

    // Display the led.
    Redisplay();
}

BitLed::~BitLed()
{
    m_currentPicture = NULL;
    m_lastPicture = NULL;
    m_imageMap = NULL;
    m_led = NULL;
}

BitLed::INTENSITY BitLed::MapIntensity(unsigned int brightness)
{
    INTENSITY ret = OFF;

    // Map intensities based on the available number.
    switch (m_led->m_numBrightnesses)
    {
    case 2:
    {
        // On and off.  Do Off and Medium.
        switch (brightness)
        {
        case 1:
            // Already set to off by default.
            break;

        case 2:
            ret = MEDIUM;
            break;

        default:
            // Bad value - turn it off.
            break;
        }
    }
        break;

    case 3:
    {
        // High, low, and off.  Do High, Medium and Off.
        switch (brightness)
        {
        case 1:
            // Already set to off by default.
            break;

        case 2:
            ret = MEDIUM;
            break;

        case 3:
            ret = HIGH;
            break;

        default:
            // Bad value - turn it off.
            break;
        }
    }
        break;

    case 4:
    {
        // Direct mapping.
        switch (brightness)
        {
        case 1:
            // Already set to off by default.
            break;

        case 2:
            ret = LOW;
            break;

        case 3:
            ret = MEDIUM;
            break;

        case 4:
            ret = HIGH;
            break;

        default:
            // Bad value - turn it off.
            break;
        }
    }
        break;

    default:
        // Bad brightnesses value.  Turn off the LED.
        break;
    }

    // Give'em the computed intensity.
    return ret;
}

Image* BitLed::GetImage(RI_FP_INDICATOR_COLOR color,
        BitLed::INTENSITY intensity)
{
    // Do a pseudo cache of the last image grabbed so
    // getting the same image over and over is not too
    // costly.
    if ((color == m_lastColor) && (intensity == m_lastIntensity))
    {
        // Just return the last used picture.
        return m_lastPicture;
    }

    // Get the Intensity map for the color.
    ColorIntensityMap::iterator colorIter;
    colorIter = m_ledImageMap.find(color);
    IntensityMap::iterator intensityIter;
    intensityIter = (*colorIter).second.find(intensity);

    return (*intensityIter).second;
}

void BitLed::Redisplay()
{
    // Check if rendering has been initialized.
    if (!m_canRender)
        return;

    // Figure out the intensity.
    INTENSITY intensity;
    if (m_led->m_on)
        intensity = MapIntensity(m_led->m_brightnessLevel);
    else
        intensity = OFF;

    // Get the image for the intensity and color.
    m_currentPicture = GetImage(m_led->m_color, intensity);
    unsigned char *image = NULL;
    if (m_currentPicture != NULL)
        image = m_currentPicture->GetPixmap();

    // Set raster position and zoom factor (in case it was previously modified).
    glPixelZoom(1.0f, 1.0f);
    glRasterPos2i(m_x, m_y);

    // Redisplay the image.
    // Draw the image.
    if (image != NULL)
        glDrawPixels(m_width, m_height, GL_RGBA, GL_UNSIGNED_BYTE, image);
    GLenum error = glGetError();
    if (error != GL_NO_ERROR)
    {
        //const GLubyte *errString = gluErrorString(error);
        //RILOG_ERROR("OpenGL Error: %s\n", errString);
        RILOG_ERROR("OpenGL Error: %d\n", error);
    }

    glFlush();
}

void BitLed::ResetLED()
{
    // TODO: reset ri_led_t data? Will need to request the RI Platform to do this since
    // it owns the ri_led_t state.

    // Redisplay the image.
    Redisplay();
}

unsigned char *BitLed::GetImageData(RI_FP_INDICATOR_COLOR color,
        INTENSITY intensity)
{
    // Find the matching meta data information, as retrieved from the image map.
    MetaDataNode *node = m_imageMap->GetMetaDataNode(m_led->m_name);
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No image data for LED %s.\n", m_led->m_name);
        return NULL;
    }

    // Find the matching meta data information for the color value.
    char *colorStr = (char *) "unknown";
    switch (color)
    {
    case (RI_FP_RED):
        colorStr = (char *) "red";
        break;
    case (RI_FP_GREEN):
        colorStr = (char *) "green";
        break;
    case (RI_FP_BLUE):
        colorStr = (char *) "blue";
        break;
    case (RI_FP_YELLOW):
        colorStr = (char *) "yellow";
        break;
    case (RI_FP_ORANGE):
        colorStr = (char *) "orange";
        break;
    default:
        RILOG_ERROR("Invalid color (%s) for LED %s.\n", colorStr, m_led->m_name);
        return NULL;
    }
    node = m_imageMap->FindMetaData(node, colorStr);
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No image data for LED %s with color %s.\n", m_led->m_name,
                colorStr);
        return NULL;
    }

    // Find the matching meta data information for the color intensity value.
    char *intensityStr = (char *) "unknown";
    switch (intensity)
    {
    case (OFF):
        intensityStr = (char *) "off";
        break;
    case (LOW):
        intensityStr = (char *) "low";
        break;
    case (MEDIUM):
        intensityStr = (char *) "medium";
        break;
    case (HIGH):
        intensityStr = (char *) "high";
        break;
    default:
        RILOG_ERROR("Invalid color intensity (%s:%s) for LED %s.\n", colorStr,
                intensityStr, m_led->m_name);
        return NULL;
    }
    std::string b64Data = m_imageMap->GetMetaDataValue(node, intensityStr);
    if (b64Data.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No image data for LED %s with color intensity (%s:%s).\n",
                m_led->m_name, colorStr, intensityStr);
        return NULL;
    }

    // Convert Base64 representation in associated Image Map meta data to pixel data.
    std::string imageData = Base64Codec::Decode(b64Data.data(), b64Data.size());
    if (imageData.size() == 0)
        return NULL;
    else
    {
        const void *data = imageData.data();
        int size = imageData.size();
        unsigned char *retValue = new unsigned char[size];
        memcpy(retValue, data, size);
        return retValue;
    }
}

void BitLed::GetImageLocation(unsigned int *x, unsigned int *y)
{
    // Find the matching meta data information, as retrieved from the image map.
    MetaDataNode *node = m_imageMap->GetMetaDataNode(m_led->m_name);
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No image location data for LED %s.\n", m_led->m_name);
        return;
    }

    node = m_imageMap->FindMetaData(node, "location");
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No image location data for LED %s.\n", m_led->m_name);
        return;
    }

    std::string xLocation = m_imageMap->GetMetaDataValue(node, (char *) "x");
    if (xLocation.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No x location for LED %s.\n", m_led->m_name);
        return;
    }
    else
        *x = atoi(xLocation.c_str());

    std::string yLocation = m_imageMap->GetMetaDataValue(node, (char *) "y");
    if (yLocation.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No y location for LED %s.\n", m_led->m_name);
        return;
    }
    else
        *y = atoi(yLocation.c_str());
}

void BitLed::GetImageSize(unsigned int *width, unsigned int *height)
{
    // Find the matching meta data information, as retrieved from the image map.
    MetaDataNode *node = m_imageMap->GetMetaDataNode(m_led->m_name);
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No image size data for LED %s.\n", m_led->m_name);
    }

    node = m_imageMap->FindMetaData(node, "size");
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No image size data for LED %s.\n", m_led->m_name);
    }

    std::string w = m_imageMap->GetMetaDataValue(node, (char *) "width");
    if (w.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No width for LED %s.\n", m_led->m_name);
    }
    else
        *width = atoi(w.c_str());

    std::string h = m_imageMap->GetMetaDataValue(node, (char *) "height");
    if (h.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No height for LED %s.\n", m_led->m_name);
    }
    else
        *height = atoi(h.c_str());
}
