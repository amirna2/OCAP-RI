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
 * RIEmulatorApp.cpp
 *
 *  Created on: Jan 7, 2009
 *      Author: Mark Millard
 */

// Include RI Emulator header files.
#include "ri_log.h"
#include "ri_config.h"
#include "ui_opengl_common.h"
#include "ui_window.h"
#include "RIEmulatorApp.h"
#include "RIEmulatorFrame.h"

// Logging category.
#define RILOG_CATEGORY g_uiCat
extern log4c_category_t *g_uiCat;

// Give wxWidgets the means to create a RIEmulatorApp object.
// However, don't enter the application loop or call OnInit() just yet.
IMPLEMENT_APP_NO_MAIN( RIEmulatorApp)

static bool isNumeric(char *str)
{
    bool retValue = true;
    char *ptr = str;
    while (*ptr != '\0')
    {
        if (isdigit(*ptr))
        {
            ptr++;
            continue;
        }
        else
        {
            retValue = false;
            break;
        }
    }
    return retValue;
}

void RIEmulatorApp::SetSubWindowConfig()
{
    char *configValue;

    // Remote configuration.
    m_showRemote = true;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.Remote.show")) != NULL || (configValue
            = ricfg_getValue("RIPlatform", (char *) "RI.Emulator.Remote.show"))
            != NULL)
    {
        if (!strcmp(configValue, "TRUE"))
            m_showRemote = true;
        else
            m_showRemote = false;
    }

    // Console configuration.
    m_showConsole = true;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.Console.show")) != NULL
            || (configValue = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.Console.show")) != NULL)
    {
        if (!strcmp(configValue, "TRUE"))
            m_showConsole = true;
        else
            m_showConsole = false;
    }

    // Front Panel configuration.
    m_showFrontPanel = true;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.FrontPanel.show")) != NULL || (configValue
            = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.FrontPanel.show")) != NULL)
    {
        if (!strcmp(configValue, "TRUE"))
            m_showFrontPanel = true;
        else
            m_showFrontPanel = false;
    }

    // Determine whether to allow resizing.
    m_style = wxMINIMIZE_BOX | wxMAXIMIZE_BOX | wxSYSTEM_MENU | wxCAPTION
            | wxCLOSE_BOX | wxCLIP_CHILDREN;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.Frame.resize")) != NULL
            || (configValue = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.Frame.resize")) != NULL)
    {
        if (!strcmp(configValue, "TRUE"))
            m_style |= wxRESIZE_BORDER;
    }
    else
    {
        // Allow resizing by default.
        m_style |= wxRESIZE_BORDER;
    }

    // Determine location (x,y) for window.
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.Frame.xPosition")) != NULL || (configValue
            = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.Frame.xPosition")) != NULL)
    {
        if (isNumeric(configValue))
            m_position.x = atoi(configValue);
    }
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.Frame.yPosition")) != NULL || (configValue
            = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.Frame.yPosition")) != NULL)
    {
        if (isNumeric(configValue))
            m_position.y = atoi(configValue);
    }
}

// The default constructor.
RIEmulatorApp::RIEmulatorApp()
{
    m_frame = NULL;
    m_tvScreen = NULL;
    m_console = NULL;
    m_remote = NULL;
    m_frontPanel = NULL;
    m_display = NULL;
    m_position.x = -1; // Use default x location.
    m_position.y = -1; // Use default y location.

    // Check Remote, Console, and Front Panel configuration parameters.
    /*lint -sem(RIEmulatorApp::SetSubWindowConfig,initializer)*/
    SetSubWindowConfig();
}

RIEmulatorApp::RIEmulatorApp(UIInfo *uiInfo)
{
    m_frame = NULL;
    m_tvScreen = NULL;
    m_console = NULL;
    m_remote = NULL;
    m_frontPanel = NULL;
    m_display = uiInfo;
    m_position.x = -1; // Use default x location.
    m_position.y = -1; // Use default y location.

    // Check Remote, Console, and Front Panel configuration parameters.
    /*lint -sem(RIEmulatorApp::SetSubWindowConfig,initializer)*/
    SetSubWindowConfig();
}

// The destructor.
RIEmulatorApp::~RIEmulatorApp()
{
    // Note that this does not clean up the reference to m_tvScreen.

    if (m_frame != NULL)
    {
        // Note: do not delete the frame since it will have already been
        // done by the parent widget, wxApp.
        m_frame = NULL;
    }
    m_tvScreen = NULL;
    m_console = NULL;
    m_remote = NULL;
    m_frontPanel = NULL;
    m_display = NULL;
}

// Initialize the application.
bool RIEmulatorApp::OnInit()
{
    RILOG_INFO("Initializing wxWidgets application UI\n");

    // Create the main application window.
    m_frame = new RIEmulatorFrame(wxT("RI Emulator Application"), m_position,
            m_style);

    // Create the TV Screen window.
    long winId = CreateTvScreen(m_frame->GetTopWidget(),
            m_display->pWindowInfo->width, m_display->pWindowInfo->height,
            m_display->pWindowInfo->is_fixed);
    if (0 == winId)
    {
        // Don't bother starting loop because window creation failed
        RILOG_ERROR("Window creation failed\n");
        return false;
    }

    // Create the Remote window.
    if (m_showRemote)
    {
        // TODO - The width should be determined from a configuration file (# pixels wide).
        unsigned int remoteWidth = 100;
        unsigned int remoteHeight = m_display->pWindowInfo->height;
        (void) CreateRemote(m_frame->GetTopWidget(), remoteWidth, remoteHeight);
    }

    // Create the Front Panel window.
    if (m_showFrontPanel)
    {
        // TODO - The default width and height should be determined from a configuration file.
        // (# pixels wide/high).
        unsigned int frontPanelWidth = m_display->pWindowInfo->width;
        unsigned int frontPanelHeight = 100;
        (void) CreateFrontPanel(m_frame->GetTopWidget(), frontPanelWidth,
                frontPanelHeight);
    }

    // Create the Console window.
    if (m_showConsole)
    {
        // TODO - The default height should be determined from a configuration file (# chars high).
        unsigned int consoleWidth = m_display->pWindowInfo->width;
        unsigned int consoleHeight = 200;
        (void) CreateConsole(m_frame->GetTopWidget(), consoleWidth,
                consoleHeight);
    }

    // Tell the frame to initialize its layout.
    (void) m_frame->InitLayout(m_tvScreen, m_remote, m_frontPanel, m_console);

    // Show it.
    char *configValue;

    bool iconify = ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.iconify")) != NULL || (configValue
            = ricfg_getValue("RIPlatform", (char *) "RI.Emulator.iconify"))
            != NULL) ? !strcmp(configValue, "TRUE") : false;

    if (iconify)
    {
        m_frame->Iconize(true);
    }
    else
    {
        (void) m_frame->Show(true);
    }

    // Note: wxGLCanvas::SetCurrent() is only valid after the window has been shown.

    // Initialize OpenGL for TvScreen. This assumes that the current OpenGL context
    // is for the TvScreen wxGLCanvas.
    opengl_init_environment(m_display, winId);
    // Initialize OpenGL for Remote. This routine will make sure to set the OpenGL context
    // for the Remote wxGLCanvas.
    if (m_remote)
        m_remote->InitOpenGL();
    // Initialize OpenGL for FrontPanel. This routine will make sure to set the OpenGL context
    // for the FrontPanel wxGLCanvas.
    if (m_frontPanel)
        m_frontPanel->InitOpenGL();

    // Signal wxWidgets initialization is complete.
    g_mutex_lock(m_display->pWindowInfo->pWindowOSInfo->m_initMutex);
    m_display->pWindowInfo->pWindowOSInfo->m_initialized = TRUE;
    g_cond_signal(m_display->pWindowInfo->pWindowOSInfo->m_initCond);
    g_mutex_unlock(m_display->pWindowInfo->pWindowOSInfo->m_initMutex);

    // Start the event loop.
    return true;
}

int RIEmulatorApp::OnExit()
{
    // Note: the shutdown sequence for wxWidgets should already have been executed
    // by the RIEmulatorFrame since it is capturing the system close event. By the
    // time we have reached here, the windows have may already been destroyed. This
    // is a place holder for killing the rest of the emulator (i.e. RI Platform and
    // stack).
    return 1;
}

void RIEmulatorApp::SetTvScreen(RITvScreen *tvScreen)
{
    // Note that the previous reference to the associated TV Screen should have
    // already been deleted prior to making this call (so that we don't have a
    // dangling pointer).
    m_tvScreen = tvScreen;
}

void RIEmulatorApp::SetConsole(RIConsole *console)
{
    // Note that the previous reference to the associated Console should have
    // already been deleted prior to making this call (so that we don't have a
    // dangling pointer).
    m_console = console;
}

void RIEmulatorApp::SetRemote(RIRemote *remote)
{
    // Note that the previous reference to the associated Remote should have
    // already been deleted prior to making this call (so that we don't have a
    // dangling pointer).
    m_remote = remote;
}

void RIEmulatorApp::SetFrontPanel(RIFrontPanel *frontPanel)
{
    // Note that the previous reference to the associated FrontPanel should have
    // already been deleted prior to making this call (so that we don't have a
    // dangling pointer).
    m_frontPanel = frontPanel;
}

void RIEmulatorApp::Shutdown(bool closeIt)
{
    // Deactivate the TvScreen rendering cycle.
    if (m_tvScreen)
    {
        g_mutex_lock(m_tvScreen->m_renderMutex);
        m_tvScreen->ActivateRenderLoop(false);
        g_mutex_unlock(m_tvScreen->m_renderMutex);
    }

    // Tell the GStreamer pipeline to stop refreshing video/graphics.
    g_mutex_lock(m_display->window_lock);
    m_display->pWindowInfo->win = 0;
    g_mutex_unlock(m_display->window_lock);

    // Note: Let the RIConsole destructor stop logging messages. No need to call
    // m_console->StopLogging.

    // Display a message to remind the user to kill the emulator
    wxMessageDialog
            dialog(
                    NULL,
                    wxT(
                            "Please kill the emulator using an external procedure\n(i.e. <CTRL-c>).\n\nUsing the window GUI to terminate the emulator does\nnot completely shutdown the OCAP Stack."),
                    wxT("RI Emulator"), wxNO_DEFAULT | wxOK | wxICON_WARNING);
    (void) dialog.ShowModal();

    // Close the GUI frame. This will destroy the windows.
    if (closeIt)
        //m_frame->Close(true);
        (void) m_frame->Destroy();
}

unsigned long RIEmulatorApp::CreateTvScreen(wxWindow *parent,
        unsigned int width, unsigned int height, bool isFixed)
{
    // Open visible window with border and title bar.
    long style = wxWANTS_CHARS;
    if (isFixed)
    {
        // Open a visible fixed window with no title bar.
        style = wxBORDER_NONE | wxWANTS_CHARS;
    }

    // Create the OpenGL wxWidgets window.
    wxSize size;
    size.SetWidth(width);
    size.SetHeight(height);

    RITvScreen *tvScreen =
            new RITvScreen(parent, wxT("TV Screen"), style, size);
    if (tvScreen != NULL)
    {
        SetTvScreen(tvScreen);
        tvScreen->SetDisplay(m_display);
    }
    else
        return 0;

    RILOG_INFO("Created TV Screen.\n");

    // Set the return value based on window created.
    unsigned long winRetVal = (unsigned long) tvScreen->GetId();
    return winRetVal;
}

unsigned long RIEmulatorApp::CreateConsole(wxWindow *parent,
        unsigned int width, unsigned int height)
{
    // Create the wxWidgets window.
    wxSize size;
    size.SetWidth(width);
    size.SetHeight(height);

    RIConsole *console = new RIConsole(parent, wxT("Console"), 0, size);
    if (console != NULL)
    {
        SetConsole(console);
    }
    else
        return 0;

    // Set the Server port number.
    // Retrieve value from the configuration file.
    char *configValue = NULL;
    unsigned short port = RIConsole::DEFAULT_SERVER_PORT;
    if ((configValue = ricfg_getValue("twb_config",
            (char *) "RI.Emulator.Console.port")) != NULL
            || (configValue = ricfg_getValue("RIPlatform",
                    (char *) "RI.Emulator.Console.port")) != NULL)
    {
        if ((port = atoi(configValue)) == 0)
            port = RIConsole::DEFAULT_SERVER_PORT;
    }
    console->SetPort(port);
    // And start logging.
    console->StartLogging();

    RILOG_INFO("Created Console.\n");

    // Set the return value based on window created.
    unsigned long winRetVal = (unsigned long) console->GetId();
    return winRetVal;
}

unsigned long RIEmulatorApp::CreateRemote(wxWindow *parent, unsigned int width,
        unsigned int height)
{
    // Create the wxWidgets window.
    wxSize size;
    size.SetWidth(width);
    size.SetHeight(height);

    RIRemote *remote = new RIRemote(parent, wxT("Remote"), 0, size);
    if (remote != NULL)
    {
        SetRemote(remote);
    }
    else
        return 0;

    // The remote should now be initialized with the Image Map
    // specified in the configuration file. The wxGLCanvas has not yet
    // been initialized for OpenGL.

    RILOG_INFO("Created Remote.\n");

    // Set the return value based on window created.
    unsigned long winRetVal = (unsigned long) remote->GetId();
    return winRetVal;
}

unsigned long RIEmulatorApp::CreateFrontPanel(wxWindow *parent,
        unsigned int width, unsigned int height)
{
    // Create the wxWidgets window.
    wxSize size;
    size.SetWidth(width);
    size.SetHeight(height);

    RIFrontPanel *frontPanel = new RIFrontPanel(parent, wxT("FrontPanel"), 0,
            size);
    if (frontPanel != NULL)
    {
        SetFrontPanel(frontPanel);
    }
    else
        return 0;

    // The front panel should now be initialized with the Image Map
    // specified in the configuration file. The wxGLCanvas has not yet
    // been initialized for OpenGL.

    RILOG_INFO("Created Front Panel.\n");

    // Set the return value based on window created.
    unsigned long winRetVal = (unsigned long) frontPanel->GetId();
    return winRetVal;
}
