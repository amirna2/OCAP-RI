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
 * RITvScreen.h
 *
 *  Created on: Jan 13, 2009
 *      Author: Mark Millard
 */

#ifndef _RI_TVSCREEN_H_
#define _RI_TVSCREEN_H_

// Include wxWidgets header files.
#include <wx/wx.h>
#include <wx/glcanvas.h>

// Include RI Emulator header files.
#include "ui_info.h"

/**
 * The RI Emulator TV Screen window.
 */
class RITvScreen: public wxGLCanvas
{
public:

    /** The default device width, if one isn't provided via a configuration file. */
    static const int DEFAULT_DEVICE_WIDTH = 640;
    /** The default device height, if one isn't provided via a configuration file. */
    static const int DEFAULT_DEVICE_HEIGHT = 480;

    /**
     * A constructor that creates a window for the RI Emulator graphics display.
     *
     * @param parent The window's parent.
     * @param name The window's name.
     * @param style The style template.
     * @param size The width and height of the window to create.
     */
    RITvScreen(wxWindow *parent, const wxString &name, long style,
            const wxSize size);

    /**
     * The destructor.
     */
    virtual ~RITvScreen();

    /**
     * Get the associated display.
     *
     * @return A pointer to the <code>UIInfo</code> context is returned.
     */
    UIInfo *GetDisplay()
    {
        return m_display;
    }

    /**
     * Set the associated display.
     *
     * @param display A pointer to the <ocde>UIInfo</code> context.
     */
    void SetDisplay(UIInfo *display)
    {
        m_display = display;
    }

    // Event handlers.

    /**
     * The paint event handler.
     *
     * @param event The paint event.
     */
    void OnPaint(wxPaintEvent &event);

    /**
     * The key release event handler.
     *
     * @param event The key event.
     */
    void OnKeyUp(wxKeyEvent &event);

    /**
     * The key pressed event handler.
     *
     * @param event The key event.
     */
    void OnKeyDown(wxKeyEvent &event);

    /**
     * The mouse entered window event handler.
     *
     * @param event The mouse event.
     */
    void OnEnterWindow(wxMouseEvent &event);

    /**
     * The message loop idle event handler.
     *
     * @param event The idle event.
     */
    void OnIdle(wxIdleEvent &event);

    /**
     * The window destroyed event handler.
     *
     * @param event The window destroy event.
     */
    void OnWindowDestroy(wxWindowDestroyEvent &event);

    /**
     * Render the next screen frame.
     */
    void Render(wxDC &dc);

    /**
     * Determine whether the screen can be rendered.
     *
     * @return <b>true</b> will be returned if the window is in a
     * state that can render OpenGL. Otherwise, <b>false</b> will be
     * returned.
     */
    bool CanRender();

    /**
     * Activate the rendering cycle.
     *
     * @param flag If <b>true</b>, then the rendering cycle is activated.
     * Otherwise, use <b>false</b> to deactivate the loop.
     */
    void ActivateRenderLoop(bool flag);

    // Mutex for signaling frame refresh.
    GMutex *m_renderMutex;

    // Flag indicating that the rendering cycle is activated or not.
    bool m_renderLoopActivated;

private:

    // List of OpenGL attributes for the device.
    static int g_attrList[];

    // The device context.
    UIInfo *m_display;

    // Render thread.
    GThread *m_renderThread;

    // This class handles events.
    /*lint -e(1516)*/
DECLARE_EVENT_TABLE()
};

#endif /* _RI_TVSCREEN_H_ */
