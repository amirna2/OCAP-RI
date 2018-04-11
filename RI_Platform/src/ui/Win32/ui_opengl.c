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

#include "ui_window.h"
#include "ui_info.h"
#include <gl/gl.h>
#include <gl/glext.h>
#include <ri_log.h>

// Logging category
#define RILOG_CATEGORY g_uiCat
static log4c_category_t* g_uiCat = NULL;
static char* LOG_CAT = "RI.UI.OpenGL";

// Forward declarations
static int opengl_choose_pixel_format(WindowOSInfo* windowOSInfo, int* p_bpp,
        int* p_depth, int* p_dbl, int* p_acc);

/**
 * Chooses an OpenGL pixel format and make necessary calls to
 * assign the selected format.
 *
 * @param win                       ID of window using openGL
 * @param windowOSInfo              native OS window information
 * @param hw_acceleration_disabled  flag indicating that openGL
 *                                  hardware acceleration should be disabled
 */
void opengl_set_pixel_format(gulong win, WindowOSInfo* windowOSInfo,
        gboolean hw_acceleration_disabled)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Get the device context (DC)
    windowOSInfo->hDC = GetDC((HWND) win);

    // Enable OpenGL for the window
    PIXELFORMATDESCRIPTOR pfd;
    int format;

    // Set the pixel format for the DC
    ZeroMemory(&pfd, sizeof(pfd));

    pfd.nSize = sizeof(pfd);
    pfd.nVersion = 1;
    pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
    pfd.iPixelType = PFD_TYPE_RGBA;
    pfd.cColorBits = 24;
    pfd.cDepthBits = 16;
    pfd.iLayerType = PFD_MAIN_PLANE;

    // If hardware acceleration has not been disabled, use the standard pixel format
    // selection which will select hardware acceleration if available
    if (!hw_acceleration_disabled)
    {
        RILOG_DEBUG("%s -- Using standard pixel format selection method\n",
                __FUNCTION__);

        // Use standard pixel format choosing method
        format = ChoosePixelFormat(windowOSInfo->hDC, &pfd);
    }
    else
    {
        // Use alternate Pixel format choosing method
        int bpp = -1; // don't care. (or a positive integer)
        int depth = -1; // don't care. (or a positive integer)
        int dbl = 1; // we want double-buffering. (or -1 for 'don't care', or 0 for 'none')
        int acc = 0; // we don't want hardware acceleration. (0 to indicate we don't want it)

        RILOG_DEBUG("%s -- Using alternate pixel format selection method\n",
                __FUNCTION__);

        format = opengl_choose_pixel_format(windowOSInfo, &bpp, &depth, &dbl,
                &acc);
    }

    RILOG_DEBUG("%s -- Setting pixel format to: %d\n", __FUNCTION__, format);

    (void) SetPixelFormat(windowOSInfo->hDC, format, &pfd);

    // Create and enable the render context (RC)
    windowOSInfo->hRC = wglCreateContext(windowOSInfo->hDC);

    (void) wglMakeCurrent(windowOSInfo->hDC, windowOSInfo->hRC);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Make the OpenGL calls to display the current video and graphic image buffers
 * to the display.
 *
 * @param windowOSInfo    native OS Window information
 */
void opengl_swap_buffers(UIInfo* uiInfo)
{
    // Swap the back buffer with the front buffer
    (void) SwapBuffers(uiInfo->pWindowInfo->pWindowOSInfo->hDC);
}

/**
 * Prints out log messages which report OpenGL related information
 * about versions, extensions, etc.
 *
 * @param windowOSInfo  native OS window information
 */
void opengl_report_information(WindowOSInfo* windowOSInfo)
{
    if (NULL == g_uiCat)
        g_uiCat = log4c_category_get(LOG_CAT);
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Print renderer
    char* r = (char*) glGetString(GL_RENDERER);
    if (NULL != r)
    {
        RILOG_INFO("%s -- OpenGL Renderer: %s\n", __FUNCTION__, r);
    }
    // Print vendor
    char* v = (char*) glGetString(GL_VENDOR);
    if (NULL != v)
    {
        RILOG_INFO("%s -- OpenGL Vendor: %s\n", __FUNCTION__, v);
    }

    // Print version
    char* ver = (char*) glGetString(GL_VERSION);
    if (NULL != ver)
    {
        RILOG_INFO("%s -- OpenGL Version: %s\n", __FUNCTION__, ver);
    }

    // Determine if hardware acceleration is possible with current pixel format.
    // Any format with the PFD_GENERIC_FORMAT attribute bit set and
    // PFD_GENERIC_ACCELERATED clear will not be hardware accelerated.
    if (NULL != windowOSInfo)
    {
        int format = GetPixelFormat(windowOSInfo->hDC);

        RILOG_INFO("%s -- OpenGL using pixel format: %d\n", __FUNCTION__,
                format);

        if ((format & PFD_GENERIC_FORMAT) && (!(format
                & PFD_GENERIC_ACCELERATED)))
        {
            RILOG_INFO("%s -- OpenGL Hardware Acceleration is NOT possible\n",
                    __FUNCTION__);
        }
        else
        {
            RILOG_INFO("%s -- OpenGL Hardware Acceleration may be possible\n",
                    __FUNCTION__);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Alternate method of choosing the pixel format to use, finds the best
 * format based on the supplied parameters.
 *
 * @param   windowOSInfo   native OS window information
 * @param   p_bpp          bits per pixel
 * @param   p_depth        color depth
 * @param   p_dbl          double buffering requested
 * @param   p_acc          hardware acceleration requested
 *
 * @return  best matching pixel format
 */
static int opengl_choose_pixel_format(WindowOSInfo* windowOSInfo, int* p_bpp,
        int *p_depth, int *p_dbl, int *p_acc)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    HDC hdc = windowOSInfo->hDC;

    int wbpp;
    if (p_bpp == NULL)
    {
        wbpp = -1;
    }
    else
    {
        wbpp = *p_bpp;
    }

    int wdepth;
    if (p_depth == NULL)
    {
        wdepth = 16;
    }
    else
    {
        wdepth = *p_depth;
    }

    int wdbl;
    if (p_dbl == NULL)
    {
        wdbl = -1;
    }
    else
    {
        wdbl = *p_dbl;
    }

    int wacc;
    if (p_acc == NULL)
    {
        wacc = 1;
    }
    else
    {
        wacc = *p_acc;
    }

    PIXELFORMATDESCRIPTOR pfd;
    ZeroMemory(&pfd, sizeof(pfd));
    pfd.nSize = sizeof(pfd);
    pfd.nVersion = 1;
    int num = DescribePixelFormat(hdc, 1, sizeof(pfd), &pfd);
    if (num == 0)
    {
        return 0;
    }

    unsigned int maxqual = 0;
    int maxindex = 0;
    int max_bpp = 0;
    int max_depth = 0;
    int max_dbl = 0;
    int max_acc = 0;
    int i = 1;
    for (i = 1; i <= num; i++)
    {
        ZeroMemory(&pfd, sizeof(pfd));
        pfd.nSize = sizeof(pfd);
        pfd.nVersion = 1;
        (void) DescribePixelFormat(hdc, i, sizeof(pfd), &pfd);
        int bpp = pfd.cColorBits;
        int depth = pfd.cDepthBits;
        gboolean pal = (pfd.iPixelType == PFD_TYPE_COLORINDEX);
        gboolean mcd = ((pfd.dwFlags & PFD_GENERIC_FORMAT) && (pfd.dwFlags
                & PFD_GENERIC_ACCELERATED));
        gboolean soft = ((pfd.dwFlags & PFD_GENERIC_FORMAT) && !(pfd.dwFlags
                & PFD_GENERIC_ACCELERATED));
        gboolean icd = (!(pfd.dwFlags & PFD_GENERIC_FORMAT) && !(pfd.dwFlags
                & PFD_GENERIC_ACCELERATED));
        gboolean opengl = (pfd.dwFlags & PFD_SUPPORT_OPENGL);
        gboolean window = (pfd.dwFlags & PFD_DRAW_TO_WINDOW);
        gboolean bitmap = (pfd.dwFlags & PFD_DRAW_TO_BITMAP);
        gboolean dbuff = (pfd.dwFlags & PFD_DOUBLEBUFFER);

        unsigned int q = 0;
        if (opengl && window)
        {
            q = q + 0x8000;
        }

        if (wdepth == -1 || (wdepth > 0 && depth > 0))
        {
            q = q + 0x4000;
        }
        if (wdbl == -1 || (wdbl == 0 && !dbuff) || (wdbl == 1 && dbuff))
        {
            q = q + 0x2000;
        }
        if (wacc == -1 || (wacc == 0 && soft) || (wacc == 1 && (mcd || icd)))
        {
            q = q + 0x1000;
        }
        if (mcd || icd)
        {
            q = q + 0x0040;
        }
        if (icd)
        {
            q = q + 0x0002;
        }
        if (wbpp == -1 || (wbpp == bpp))
        {
            q = q + 0x0800;
        }
        if (bpp >= 16)
        {
            q = q + 0x0020;
        }
        if (bpp == 16)
        {
            q = q + 0x0008;
        }
        if (wdepth == -1 || (wdepth == depth))
        {
            q = q + 0x0400;
        }
        if (depth >= 16)
        {
            q = q + 0x0010;
        }
        if (depth == 16)
        {
            q = q + 0x0004;
        }
        if (!pal)
        {
            q = q + 0x0080;
        }
        if (bitmap)
        {
            q = q + 0x0001;
        }
        if (q > maxqual)
        {
            maxqual = q;
            maxindex = i;
            max_bpp = bpp;
            max_depth = depth;
            max_dbl = dbuff ? 1 : 0;
            max_acc = soft ? 0 : 1;
        }
    } // end of for loop

    if (maxindex == 0)
    {
        return maxindex;
    }
    if (p_bpp != NULL)
    {
        *p_bpp = max_bpp;
    }
    if (p_depth != NULL)
    {
        *p_depth = max_depth;
    }
    if (p_dbl != NULL)
    {
        *p_dbl = max_dbl;
    }
    if (p_acc != NULL)
    {
        *p_acc = max_acc;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return maxindex;
}
