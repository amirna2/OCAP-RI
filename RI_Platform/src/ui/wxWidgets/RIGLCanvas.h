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
 * RIGLCanvas.h
 *
 *  Created on: Feb 19, 2009
 *      Author: Mark Millard
 */

#ifndef _RI_GLCANVAS_H_
#define _RI_GLCANVAS_H_

// Include GLib header files.
#include <glib.h>

// Include wxWidgets header files.
#include <wx/wx.h>
#include <wx/glcanvas.h>

/**
 * The RI wxGLCanvas canvas.
 */
class RIGLCanvas: public wxGLCanvas
{
public:

    /**
     * A constructor that creates a window for the RI wxGLCanvas classes.
     *
     * @param parent The window's parent.
     * @param name The window's name.
     * @param style The style template.
     * @param size The width and height of the window to create.
     */
    RIGLCanvas(wxWindow *parent, const wxString &name, long style,
            const wxSize size);

    /**
     * The destructor.
     */
    virtual ~RIGLCanvas();

    /**
     * Set the background color.
     *
     * @param red The red component value to set.
     * @param green The green component value to set.
     * @param blue The blue component value to set.
     * @param alpha The alpha component value to set.
     */
    void SetBackgroundColor(unsigned int red, unsigned int green,
            unsigned int blue, unsigned int alpha);

    /**
     * Get the background color.
     *
     * @param red A pointer to the value to receive the red component.
     * @param green A pointer to the value to receive the blue component.
     * @param blue A pointer to the value to receive the green component.
     * @param alpha A pointer to the value to receive the alpha component.
     */
    void GetBackgroundColor(unsigned int *red, unsigned int *green,
            unsigned int *blue, unsigned int *alpha);

protected:

    /**
     * Determine if the specified extension is supported.
     *
     * @param extension The name of the extension to test for (e.g. GL_EXT_bgra).
     *
     * @return <b>TRUE</b> will be returned if the the specified extension is supported
     * by the underlying OpenGL platform. Otherwise <b>FALSE</b> will be returned.
     */
    bool IsExtensionSupported(const char *extension);

    /**
     * Initialize OpenGL.
     *
     * @param width The width of the view.
     * @param height The height of the view.
     */
    void InitOpenGL(int width, int height);

    /** List of OpenGL attributes for the device. */
    static int g_attrList[];

    // Mutex for signaling frame refresh.
    GMutex *m_renderMutex;

    // Flag indicating whether the front panel can be rendered.
    bool m_canRender;

private:

    // An RGBA8888 value
    unsigned int m_backgroundColor;
};

#endif /* _RI_GLCANVAS_H_ */
