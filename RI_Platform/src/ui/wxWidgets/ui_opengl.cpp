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

// Include system header files.
#ifdef __WXMSW__
#include <gl/gl.h>
#include <gl/glext.h>
#endif /* __WXMSW__ */
#ifdef __WXX11__
#include <GL/gl.h>
#include <GL/glext.h>
#include <GL/glx.h>
#endif /* __WXX11__ */
#include <wx/wx.h>

// Include RI Emulator header files.
#include "ui_window.h"
#include "ui_info.h"
#include "ui_opengl.h"
#include "RIEmulatorApp.h"
#include "RITvScreen.h"

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
    // Note that the pixel format in wxWidgets is established when the wxGLCanvas
    // is created. The wxGLCanvas is created by the RI API window_open() which occurs
    // previous to this call.

    // Create and enable the render context (RC). For now, we are using the default
    // wxGLContext that is created by default when the wxGLCanvas is constructed.
    RIEmulatorApp &theApp = wxGetApp();
    RITvScreen *tvScreen = theApp.GetTvScreen();
    tvScreen->SetCurrent();
}

/**
 * Make the OpenGL calls to display the current video and graphic image buffers
 * to the display.
 *
 * @param windowOSInfo native OS Window information
 */
void opengl_swap_buffers(UIInfo* uiInfo)
{
    // Swap the back buffer with the front buffer
    //RIEmulatorApp &theApp = wxGetApp();
    //RITvScreen *tvScreen = theApp.GetTvScreen();
    //tvScreen->SwapBuffers();
}

/**
 * Prints out log messages which report OpenGL related information
 * about versions, extensions, etc.
 *
 * @param windowOSInfo native OS window information
 */
void opengl_report_information(WindowOSInfo* windowOSInfo)
{
    // Print renderer.
    g_print("OpenGL Renderer: %s\n", (char*) glGetString(GL_RENDERER));

    // Print vendor.
    g_print("OpenGL Vendor: %s\n", (char*) glGetString(GL_VENDOR));

    // Print version.
    g_print("OpenGL Version: %s\n", (char*) glGetString(GL_VERSION));

    // Determine if hardware acceleration is possible with current configuration.
    // Not sure how to do this with current wxGLCanvas implementation.
}
