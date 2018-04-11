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
 * RIGLCanvas.cpp
 *
 *  Created on: Feb 19, 2009
 *      Author: Mark Millard
 */

// Include wxWidgets header files.
#include <wx/wx.h>

// Include RI Emulator header files.
#include "ri_log.h"
#include "ri_config.h"
#include "RIGLCanvas.h"

// List of OpenGL attributes for the device.
int RIGLCanvas::g_attrList[] =
{ WX_GL_RGBA, WX_GL_MIN_RED, 1, WX_GL_MIN_GREEN, 1, WX_GL_MIN_BLUE, 1,
        WX_GL_MIN_ALPHA, 1, WX_GL_STENCIL_SIZE, 1, WX_GL_DEPTH_SIZE, 16,
        WX_GL_DOUBLEBUFFER, 0 };

// Logging category
#define RILOG_CATEGORY g_uiCat
extern log4c_category_t* g_uiCat;

RIGLCanvas::RIGLCanvas(wxWindow *parent, const wxString &name, long style,
        const wxSize size) :
    wxGLCanvas(parent, -1, wxDefaultPosition, size, style, name, g_attrList,
            wxNullPalette)
{
    // Create a mutex for rendering synchronization.
    m_renderMutex = g_mutex_new();

    // Set the background color to Black/No Transparency.
    m_backgroundColor = 0x00000000;

    m_canRender = false;
}

RIGLCanvas::~RIGLCanvas()
{
    g_mutex_free( m_renderMutex);
}

bool RIGLCanvas::IsExtensionSupported(const char *extension)
{
    const GLubyte *extensions = NULL;
    const GLubyte *start;
    GLubyte *where, *terminator;

    // Extension names should not have spaces.
    where = (GLubyte *) strchr(extension, ' ');
    if (where || *extension == '\0')
        return 0;
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
                break;
            terminator = where + strlen(extension);
            if (where == start || *(where - 1) == ' ')
                if (*terminator == ' ' || *terminator == '\0')
                    return 1;
            start = terminator;
        }
    }
    return 0;
}

void RIGLCanvas::InitOpenGL(int width, int height)
{
    // Make this canvas the current OpenGL context.
    this->SetCurrent();

    // Set up the data to be used by OpenGL. Using 1-byte alignment.
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

    // Get the config value to see if pixel byte swapping has been requested
    // Default is false, set to true to compensate for issues with some hw graphics drivers
    char *cfgVal = NULL;
    if ((cfgVal = ricfg_getValue("RIPlatform",
            "RI.Platform.opengl.swap_bytes_in_pixel_store")) != NULL)
    {
        if (strcmp(cfgVal, "TRUE") == 0)
        {
            glPixelStorei(GL_UNPACK_SWAP_BYTES, 1);
        }
    }

    // Initialize the transparency blending.
    glEnable( GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // Set up the OpenGL viewport.
    glViewport(0, // x
            0, // y
            width + 1, // width
            height + 1); // height

    // Specifies the current matrix is the projection matrix.
    glMatrixMode( GL_PROJECTION);

    // Replaces projection matrix with the identity matrix.
    glLoadIdentity();

    // Sets up the clipping region.
#if 0
    glOrtho(0.0, // left
            width, // right
            height, // bottom
            0.0, // top
            -1.0, // near
            1.0); // far
#endif
    glOrtho(0.0, // left
            width + 1, // right
            0.0, // bottom
            height + 1, // top
            -1.0, // near
            1.0); // far

    // Set current matrix back to model view.
    glMatrixMode( GL_MODELVIEW);
    glLoadIdentity();

    // Disable 3D support.
    glDisable( GL_DEPTH_TEST);
    glDepthMask( GL_FALSE);

    // Set the clear color.
    // The background color is specified as an RGBA8888 value, so we need to
    // extract the individual color components to establish the background color.
    float red = ((m_backgroundColor & 0x00FF000000) >> 24) / (float) 0xFF;
    float green = ((m_backgroundColor & 0x0000FF0000) >> 16) / (float) 0xFF;
    float blue = ((m_backgroundColor & 0x000000FF00) >> 8) / (float) 0xFF;
    float alpha = (m_backgroundColor & 0x00000000FF) / (float) 0xFF;

    glClearColor(red, green, blue, alpha);

    // Display the base Image.
    m_canRender = true;
}

void RIGLCanvas::SetBackgroundColor(unsigned int red, unsigned int green,
        unsigned int blue, unsigned int alpha)
{
    m_backgroundColor = (red << 24) & 0x00FF000000;
    m_backgroundColor = ((green << 16) & 0x0000FF0000) | m_backgroundColor;
    m_backgroundColor = ((blue << 8) & 0x000000FF00) | m_backgroundColor;
    m_backgroundColor = (alpha & 0x00000000FF) | m_backgroundColor;
}

void RIGLCanvas::GetBackgroundColor(unsigned int *red, unsigned int *green,
        unsigned int *blue, unsigned int *alpha)
{
    *red = ((m_backgroundColor & 0x00FF000000) >> 24);
    *green = ((m_backgroundColor & 0x0000FF0000) >> 16);
    *blue = ((m_backgroundColor & 0x000000FF00) >> 8);
    *alpha = (m_backgroundColor & 0x00000000FF);
}
