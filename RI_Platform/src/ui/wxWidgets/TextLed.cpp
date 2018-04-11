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
 * TextLed.cpp
 *
 *  Created on: Jun 18, 2009
 *      Author: Mark Millard, enableTv Inc.
 */

// Include system header files.
#include <string.h>
#include <glib.h>
#ifdef WIN32
#include <windows.h>
#endif
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
#include "TextLed.h"
#include "RIFrontPanel.h"

// Logging category
#define RILOG_CATEGORY g_uiFrontPanelCat
extern log4c_category_t* g_uiFrontPanelCat;

// Forward function declaration.
static gboolean clock_timer_proc(gpointer data);

/*lint -sem(TextLed::ResetLED,initializer)*/
TextLed::TextLed(ri_textled_t *led, ImageMap *imageMap) :
    LedBase(0, 0, 0, 0), m_imageMap(imageMap), m_led(led)
{
    m_nSegWidth = 0;
    m_nSegLength = 0;
    m_nMargin = 0;
    m_pData = NULL;
    m_clockTimerId = -1;

    // Get the display area.
    GetDisplayLocation(&m_x, &m_y);
    GetDisplaySize(&m_width, &m_height);

    ResetLED();
}

TextLed::~TextLed()
{
    if (m_pData != NULL)
    {
        delete[] m_pData;
        m_pData = NULL;
    }
}

void *TextLed::g_background = NULL;

void TextLed::Redisplay()
{
    static bool firstTime = true;

    // Check if rendering has been initialized.
    if (!m_canRender)
        return;

    // Clear display.
    if (firstTime)
    {
        // Remember the background image.
        g_background = new unsigned char[m_width * m_height * 4
                * sizeof(GLbyte)];
        glReadPixels((GLint) m_x, (GLint) m_y, (GLsizei) m_width,
                (GLsizei) m_height, GL_RGBA, GL_UNSIGNED_BYTE, g_background);
        firstTime = false;
    }
    else
    {
        // Restore background.
        glPixelZoom(1.0f, 1.0f);
        glRasterPos2i(m_x, m_y);
        glDrawPixels(m_width, m_height, GL_RGBA, GL_UNSIGNED_BYTE, g_background);
    }

    if ((m_nDataSize - m_nStringPos) < (int) FP_TEXTLED_NUM_DISPLAY_LEDS)
    {
        for (int pos = 0; pos < (m_nDataSize - m_nStringPos); pos++)
            DrawLedDisplay(m_pData[m_nStringPos + pos], pos);
    }
    else
    {
        for (int pos = 0; pos < (int) FP_TEXTLED_NUM_DISPLAY_LEDS; pos++)
            DrawLedDisplay(m_pData[m_nStringPos + pos], pos);
    }
}

void TextLed::ResetLED()
{
    // TODO: reset ri_textled_t data? Will need to request the RI Platform to do this since
    // it owns the ri_textled_t state.

    /*lint -e(545) */
    memset(&m_szDisplayText, 0, sizeof(m_szDisplayText));

    if (m_pData)
    {
        delete[] m_pData;
    }
    m_pData = new int[FP_TEXTLED_NUM_DISPLAY_LEDS];
    for (unsigned int i = 0; i < FP_TEXTLED_NUM_DISPLAY_LEDS; ++i)
    {
        m_pData[i] = FP_TEXTLED_SEGMENT_ALL;
    }
    m_nDataSize = FP_TEXTLED_NUM_DISPLAY_LEDS;
    m_nStringPos = 0;

    m_fp = NULL;
}

void TextLed::DrawLedDisplay(unsigned int seg, int col)
{
    int num_led = col;
    unsigned int yOffset;

    // Convert the character number to an offset in pixels.
    col = m_x + ((m_width / 4) * col);

    // Compensate for the coordinate flip.
    yOffset = m_y;

    // TODO: extract these from the image map meta data.
    // Set the display parameters.
    glLineWidth(3.0f);
    glColor3f(1.0f, 1.0f, 1.0f); // white

    // Begin drawing.
    glBegin( GL_LINES);

    // Render the first segment - top horizontal line.
    if ((FP_TEXTLED_SEG1 & seg) || (FP_TEXTLED_SEGMENT_ALL == seg))
    {
        glVertex3f(col + (m_nMargin * 2), yOffset + (m_nSegLength * 2)
                + (m_nMargin * 4), 0.0f);
        glVertex3f(col + m_nSegLength, yOffset + (m_nSegLength * 2)
                + (m_nMargin * 4), 0.0f);
    }

    // Render the second segment - top right vertical line.
    if ((FP_TEXTLED_SEG2 & seg) || (FP_TEXTLED_SEGMENT_ALL == seg))
    {
        glVertex3f(col + m_nSegLength + m_nMargin, yOffset + m_nSegLength
                + (m_nMargin * 4), 0.0f);
        glVertex3f(col + m_nSegLength + m_nMargin, yOffset + (m_nSegLength * 2)
                + (m_nMargin * 3), 0.0f);
    }

    // Render the third segment - bottom right vertical line.
    if ((FP_TEXTLED_SEG3 & seg) || (FP_TEXTLED_SEGMENT_ALL == seg))
    {
        glVertex3f(col + m_nSegLength + m_nMargin, yOffset + (m_nMargin * 2),
                0.0f);
        glVertex3f(col + m_nSegLength + m_nMargin, yOffset + m_nSegLength
                + (m_nMargin * 2), 0.0f);
    }

    // Render the fourth segment.
    if ((FP_TEXTLED_SEG4 & seg) || (FP_TEXTLED_SEGMENT_ALL == seg))
    {
        glVertex3f(col + (m_nMargin * 2), m_nMargin + yOffset, 0.0f);
        glVertex3f(col + m_nSegLength, m_nMargin + yOffset, 0.0f);
    }

    // Render the fifth segment - bottom left vertical line.
    if ((FP_TEXTLED_SEG5 & seg) || (FP_TEXTLED_SEGMENT_ALL == seg))
    {
        glVertex3f(col + m_nMargin, yOffset + (m_nMargin * 2), 0.0f);
        glVertex3f(col + m_nMargin, yOffset + m_nSegLength + (m_nMargin * 2),
                0.0f);
    }

    // Render the sixth segment - top left vertical line.
    if ((FP_TEXTLED_SEG6 & seg) || (FP_TEXTLED_SEGMENT_ALL == seg))
    {
        glVertex3f(col + m_nMargin, yOffset + m_nSegLength + (m_nMargin * 4),
                0.0f);
        glVertex3f(col + m_nMargin, yOffset + (m_nSegLength * 2) + (m_nMargin
                * 3), 0.0f);
    }

    // Render the seventh segment - middle horizontal line.
    if ((FP_TEXTLED_SEG7 & seg) || (FP_TEXTLED_SEGMENT_ALL == seg))
    {
        glVertex3f(col + (m_nMargin * 2), yOffset + m_nSegLength + (m_nMargin
                * 3), 0.0f);
        glVertex3f(col + m_nMargin + m_nSegLength - m_nMargin, yOffset
                + m_nSegLength + (m_nMargin * 3), 0.0f);
    }

    // Render the eight segment.
    if ((FP_TEXTLED_SEG8 & seg) || (FP_TEXTLED_SEGMENT_ALL == seg))
    {
        if (RI_FP_MODE_STRING != m_led->m_textMode)
        {
            // Displaying a clock - handle that mode.
            if ((0 == num_led) || (1 == num_led))
            {
                glVertex3f(col + (int) (m_width / 4) + m_nMargin - 4, yOffset
                        + 2 + m_nMargin, 0.0f);
                glVertex3f(col + (int) (m_width / 4) + m_nMargin - 4, yOffset
                        + 4 + m_nMargin, 0.0f);
            }
            if ((2 == num_led) || (3 == num_led))
            {
                glVertex3f(col + m_nMargin - 4, yOffset + m_nSegLength + 9
                        + (m_nMargin * 4), 0.0f);
                glVertex3f(col + m_nMargin - 4, yOffset + m_nSegLength + 7
                        + (m_nMargin * 4), 0.0f);
            }
        }
        else
        {
            // Just display the dot.
            glVertex3f(col + m_nSegLength + (m_nMargin * 3), yOffset + 2
                    + m_nMargin, 0.0f);
            glVertex3f(col + m_nSegLength + m_nMargin * 3, yOffset + 4
                    + m_nMargin, 0.0f);
        }
    }

    // Done drawing.
    glEnd();
}

void TextLed::DisplayLEDString(char *text)
{
    //RILOG_INFO("Updating Text Display string: %s.\n", text);
    int len = strlen(text);

    // Reset the data buffer.
    // OCORI-2217: the old data should be deleted even if the length 
    // of new text is an empty string which would be the case if 
    // eraseDisplay has been requested
    //if (len) 
    //{
        if (m_pData)
        {
            delete[] m_pData;
        }
        m_pData = new int[len];
        m_nDataSize = len;
    //}

    m_nStringPos = 0;
    // Store the data.
    for (int i = 0; i < len; i++)
    {
        m_pData[i] = FP_TEXTLED_CHARSET[text[i] - FP_TEXTLED_CHAR_START];
    }

    // Prepare for a display redraw.
    Redisplay();
}

#ifndef WIN32
static void _GetTimeFormat(char *format, char newTime[])
{
    struct timeval tv;
    time_t curtime;
    struct tm now;

    gettimeofday(&tv, NULL);
    curtime = tv.tv_sec;
    localtime_r(&curtime, &now);

    char buffer[5];
    strftime(buffer, 5, format, &now);
    newTime[0] = buffer[0];
    newTime[1] = buffer[1];
    newTime[2] = buffer[2];
    newTime[3] = buffer[3];
}
#endif /* ! WIN32 */

gboolean TextLed::DisplayLEDClock(int mode)
{
    //RILOG_INFO("Updating Text Display clock: %s.\n");
    char szNewTime[4];
    if (mode == RI_FP_MODE_24H_CLOCK)
    {
#ifdef WIN32
        (void) GetTimeFormat(LOCALE_SYSTEM_DEFAULT, TIME_NOSECONDS | TIME_FORCE24HOURFORMAT, NULL, "HHmm", szNewTime, 4);
#else
        _GetTimeFormat((char *) "%H%M", szNewTime);
#endif
    }
    else
    {
#ifdef WIN32
        (void) GetTimeFormat(LOCALE_USER_DEFAULT, TIME_NOSECONDS, NULL, "hhmm", szNewTime, 4);
#else
        _GetTimeFormat((char *) "%I%M", szNewTime);
#endif
    }
    // Only update time if it has changed.
    if (!strncmp(szNewTime, m_szDisplayText, 4))
    {
        return false;
    }
    strncpy(m_szDisplayText, szNewTime, 4);

    if (m_pData)
    {
        delete[] m_pData;
    }

    m_nStringPos = 0;
    m_pData = new int[FP_TEXTLED_NUM_DISPLAY_LEDS];
    m_pData[0] = FP_TEXTLED_CHARSET[szNewTime[0] - FP_TEXTLED_CHAR_START];
    m_pData[1] = FP_TEXTLED_CHARSET[szNewTime[1] - FP_TEXTLED_CHAR_START] + 1;
    m_pData[2] = FP_TEXTLED_CHARSET[szNewTime[2] - FP_TEXTLED_CHAR_START] + 1;
    m_pData[3] = FP_TEXTLED_CHARSET[szNewTime[3] - FP_TEXTLED_CHAR_START];
    m_nDataSize = FP_TEXTLED_NUM_DISPLAY_LEDS;

    // Prepare for a display redraw.
    Redisplay();

    return true;
}

void TextLed::UpdateScroll()
{
    // Bump and display from new index.
    // Bump the index each time we're coming on.
    ++m_nStringPos;
    if (m_nStringPos >= m_nDataSize)
    {
        m_nStringPos = 0;
    }
}

void TextLed::GetDisplayLocation(unsigned int *x, unsigned int *y)
{
    // Find the matching meta data information, as retrieved from the image map.
    MetaDataNode *node = m_imageMap->GetMetaDataNode(m_led->m_base.m_name);
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No location data for Text Display %s.\n",
                m_led->m_base.m_name);
        return;
    }

    node = m_imageMap->FindMetaData(node, "location");
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No location data for Text Display %s.\n",
                m_led->m_base.m_name);
        return;
    }

    std::string xLocation = m_imageMap->GetMetaDataValue(node, (char *) "x");
    if (xLocation.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No x location for Text Display %s.\n",
                m_led->m_base.m_name);
        return;
    }
    else
        *x = atoi(xLocation.c_str());

    std::string yLocation = m_imageMap->GetMetaDataValue(node, (char *) "y");
    if (yLocation.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No y location for Text Display %s.\n",
                m_led->m_base.m_name);
        return;
    }
    else
        *y = atoi(yLocation.c_str());
}

void TextLed::GetDisplaySize(unsigned int *width, unsigned int *height)
{
    // Find the matching meta data information, as retrieved from the image map.
    MetaDataNode *node = m_imageMap->GetMetaDataNode(m_led->m_base.m_name);
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No size data for Text Display %s.\n", m_led->m_base.m_name);
    }

    node = m_imageMap->FindMetaData(node, "size");
    if (node == NULL)
    {
        // No meta data by that name.
        RILOG_ERROR("No size data for Text Display %s.\n", m_led->m_base.m_name);
    }

    std::string w = m_imageMap->GetMetaDataValue(node, (char *) "width");
    if (w.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No width for Text Display %s.\n", m_led->m_base.m_name);
    }
    else
        *width = atoi(w.c_str());

    std::string h = m_imageMap->GetMetaDataValue(node, (char *) "height");
    if (h.size() == 0)
    {
        // No meta data by that name.
        RILOG_ERROR("No height for Text Display %s.\n", m_led->m_base.m_name);
    }
    else
        *height = atoi(h.c_str());
}

void TextLed::StartDisplay(RIFrontPanel *fp)
{
    // Reset the display.
    ResetLED();

    // Set the display sizes.
    CalculateTextSizes();

    // (Re)Start the clock timer.
    if (m_clockTimerId != -1)
        (void) g_source_remove(m_clockTimerId);
    m_clockTimerId = g_timeout_add(1000, clock_timer_proc, this);

    // Remember the associated front panel; used by the clock
    // timer callback for rendering the display.
    m_fp = fp;

    // Flag it ready for rendering.
    CanRender(true);
}

void TextLed::StopDisplay()
{
    // Kill the clock timer.
    (void) g_source_remove(m_clockTimerId);
}

void TextLed::CalculateTextSizes()
{
    // Calculate the character metrics in proportion to the size of the client area.
    if ((m_height * 0.07) < 1)
    {
        m_nMargin = 1;
    }
    else
    {
        m_nMargin = (int) (m_height * 07 / 100);
    }
    if ((m_height * 0.35) < 1)
    {
        m_nSegLength = 1;
    }
    else
    {
        m_nSegLength = (int) (m_height * 35 / 100);
    }

    m_nSegWidth = m_nMargin;
}

gboolean clock_timer_proc(gpointer data)
{
    TextLed *textLed = (TextLed *) data;
    ri_textled_t *led = textLed->GetLed();
    //RILOG_INFO("Clock Timer Fired: %s\n", led->m_base.m_name);

    // Update the clock.
    if ((led->m_textMode == RI_FP_MODE_12H_CLOCK) || (led->m_textMode
            == RI_FP_MODE_24H_CLOCK))
    {
        if (textLed->GetFrontPanel() != NULL)
            textLed->GetFrontPanel()->RenderTextDisplay(led);
    }

    // Continue calling this callback until the clock timer is explicitly removed.
    return TRUE;
}
