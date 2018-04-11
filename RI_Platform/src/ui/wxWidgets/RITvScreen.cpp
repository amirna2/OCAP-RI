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
 * RITvScreen.cpp
 *
 *  Created on: Jan 13, 2009
 *      Author: Mark Millard
 */

// Include wxWidgets header files.
#include <wx/wx.h>

// Include RI Emulator header files.
#include "ui_window.h"
#include "ri_log.h"
#include "RITvScreen.h"

// Event table for RITvScreen.
BEGIN_EVENT_TABLE(RITvScreen, wxGLCanvas)
EVT_KEY_UP(RITvScreen::OnKeyUp)
EVT_KEY_DOWN(RITvScreen::OnKeyDown)
EVT_ENTER_WINDOW(RITvScreen::OnEnterWindow)
EVT_PAINT(RITvScreen::OnPaint)
EVT_IDLE(RITvScreen::OnIdle)
EVT_WINDOW_DESTROY(RITvScreen::OnWindowDestroy)
END_EVENT_TABLE()

// List of OpenGL attributes for the device.
int RITvScreen::g_attrList[] =
{
    WX_GL_RGBA,
    WX_GL_MIN_RED, 1,
    WX_GL_MIN_GREEN , 1,
    WX_GL_MIN_BLUE, 1,
    WX_GL_MIN_ALPHA, 1,
    WX_GL_STENCIL_SIZE, 1,
    WX_GL_DEPTH_SIZE, 16,
    WX_GL_DOUBLEBUFFER,
    0
};

// Logging category
#define RILOG_CATEGORY g_uiTvScreenCat
log4c_category_t* g_uiTvScreenCat;

RITvScreen::RITvScreen(wxWindow *parent, const wxString &name, long style,
        const wxSize size) :
    wxGLCanvas(parent, -1, wxDefaultPosition, size, style, name, g_attrList,
            wxNullPalette)
{
    m_display = NULL;
    m_renderMutex = g_mutex_new();
    m_renderThread = NULL;

    // Initialize logging for TvScreen functionality.
    g_uiTvScreenCat = log4c_category_get("RI.UI.TvScreen");

    // Initialize rendering loop (not ready yet).
    m_renderLoopActivated = false;
}

RITvScreen::~RITvScreen()
{
    //g_cond_free(m_renderCond);
    g_mutex_free( m_renderMutex);
}

void RITvScreen::OnPaint(wxPaintEvent &event)
{
    // Create a device context. Make sure this is the first thing we do;
    // otherwise, Windows may continue to generate Paint events ad infinitum
    // (creating an infinite loop).
    wxPaintDC dc(this);

    // Attempt to lock the render mutex. If we can't obtain the lock,
    // then assume we can't render the next frame.
    gboolean status = g_mutex_trylock(m_renderMutex);
    if (!status)
        return;

    // Throw away paint events if we aren't ready to render.
    if (!m_renderLoopActivated)
    {
        g_mutex_unlock( m_renderMutex);
        return;
    }

    //RILOG_INFO("Handling wxPaintEvent.\n");

    // Render the screen.
    Render(dc);

    // Clear render flag so we can post the next render request.
    SetClientData((void *) 0);

    // Unlock the render mutex.
    g_mutex_unlock( m_renderMutex);
}

void RITvScreen::OnKeyUp(wxKeyEvent &event)
{
    int code = event.GetKeyCode();
    RILOG_DEBUG("Handling wxKeyEvent Up: %d.\n", code);
    window_handle_key_event(code, true);
}

void RITvScreen::OnKeyDown(wxKeyEvent &event)
{
    int code = event.GetKeyCode();
    RILOG_DEBUG("Handling wxKeyEvent Down: %d.\n", code);
    window_handle_key_event(code, false);
}

void RITvScreen::OnEnterWindow(wxMouseEvent &event)
{
    // Set focus to the wxGLCanvas; this facilitates
    // key event processing.
    SetFocus();
}

void RITvScreen::OnWindowDestroy(wxWindowDestroyEvent &event)
{
    window_handle_close_event( m_display);
}

void RITvScreen::OnIdle(wxIdleEvent &event)
{
    // Create a device context.
    wxClientDC dc(this);

    // Attempt to lock the render mutex. If we can't obtain the lock,
    // then assume we can't render the next frame.
    gboolean status = g_mutex_trylock(m_renderMutex);
    if (!status)
        return;

    // Throw away paint events if we aren't ready to render.
    if (!m_renderLoopActivated)
    {
        g_mutex_unlock( m_renderMutex);
        return;
    }

    // Can render flag should be set to 1.
    void *data = GetClientData();
    int flag = (int) (data);
    if (flag == 0)
    {
        g_mutex_unlock( m_renderMutex);
        return;
    }

    //RILOG_INFO("Handling wxIdleEvent.\n");

    // Render the screen.
    Render(dc);

    // Clear render flag so we can post the next render request.
    SetClientData((void *) 0);

    // Unlock the render mutex.
    g_mutex_unlock( m_renderMutex);
}

void RITvScreen::Render(wxDC &dc)
{
    // Set this canvas as the current OpenGL context so that OpenGL
    // commands can be directed to this window.
    SetCurrent();

    // Make OpenGL calls here.
    if (m_display != NULL)
        window_handle_paint_event( m_display);

    // Show the OpenGL back buffer on this window.
    SwapBuffers();
}

bool RITvScreen::CanRender()
{
    // Try to lock the mutex; if we can't then, don't post a
    // request to render the next frame.
    gboolean status = g_mutex_trylock(m_renderMutex);
    if (!status)
        return false;

    void *data = GetClientData();
    int flag = (int) (data);
    if (flag == 0)
    {
        flag = 1;
        SetClientData((void *) flag);
        //RILOG_INFO("***** Can Render TV Screen *****\n");

        // Unlock the render mutex and return.
        g_mutex_unlock( m_renderMutex);
        return true;
    }
    else
    {
        //RILOG_INFO("===== Can't Render TV Screen =====\n");

        // Unlock the render mutex and return,
        g_mutex_unlock( m_renderMutex);
        return false;
    }
}

void RITvScreen::ActivateRenderLoop(bool flag)
{
    if (flag && !m_renderLoopActivated)
    {
        //Connect(wxID_ANY, wxEVT_IDLE, wxIdleEventHandler(RITvScreen::OnIdle));
        m_renderLoopActivated = flag;
    }
    else if (!flag && m_renderLoopActivated)
    {
        //Disconnect(wxEVT_IDLE, wxIdleEventHandler(RITvScreen::OnIdle));
        m_renderLoopActivated = false;
    }
}
