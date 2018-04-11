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
 * RIEmulatorFrame.cpp
 *
 *  Created on: Jan 7, 2009
 *      Author: Mark Millard
 */

// Include RI Emulator header files.
#include "ri_log.h"
#include "ui_info.h"
#include "RIEmulatorApp.h"
#include "RIEmulatorFrame.h"
#include "RIEmulator.xpm"

// Logging category
#define RILOG_CATEGORY g_uiCat
extern log4c_category_t* g_uiCat;

// Event table for RIEmulatorFrame.
BEGIN_EVENT_TABLE(RIEmulatorFrame, wxFrame)
EVT_MENU(wxID_ABOUT, RIEmulatorFrame::OnAbout)
EVT_MENU(wxID_EXIT, RIEmulatorFrame::OnQuit)
EVT_MENU(ID_CONSOLE_VIEW_CONTROL, RIEmulatorFrame::OnConsoleViewControl)
EVT_MENU(ID_REMOTE_VIEW_CONTROL, RIEmulatorFrame::OnRemoteViewControl)
EVT_MENU(ID_FRONTPANEL_VIEW_CONTROL, RIEmulatorFrame::OnFrontPanelViewControl)
EVT_CLOSE(RIEmulatorFrame::OnClose)
END_EVENT_TABLE()

void RIEmulatorFrame::OnAbout(wxCommandEvent &event)
{
    wxString msg;
    (void) msg.Printf(wxT("Welcome to the CableLabs RI Emulator.\n\n"));
    (void) msg.Append(wxT("This emulator is used to develop and debug OCAP\n"));
    (void) msg.Append(wxT("applications using the OCAP Reference Implementation\n"));
    (void) msg.Append(wxT("(OCAP-RI) from Cable Television Laboratories, Inc.\n\n"));
    (void) msg.Append(wxT("For more information, please visit the open source\n"));
    (void) msg.Append(wxT("web site: https://ocap-ri.dev.java.net/\n"));
    (void) wxMessageBox(msg, wxT("About RI Emulator"), wxOK | wxICON_INFORMATION, this);
}

void RIEmulatorFrame::OnQuit(wxCommandEvent &event)
{
    // Shutdown via the main application.
    RIEmulatorApp &theApp = wxGetApp();
    theApp.Shutdown(true);
}

void RIEmulatorFrame::OnClose(wxCloseEvent &event)
{
    RILOG_DEBUG("Handling wxCloseEvent.\n");

    RIEmulatorApp &theApp = wxGetApp();
    theApp.Shutdown(true);
}

void RIEmulatorFrame::OnConsoleViewControl(wxCommandEvent &event)
{
    RILOG_DEBUG("Handling Console view control.\n");

    RIEmulatorApp &theApp = wxGetApp();
    RIConsole *console = theApp.GetConsole();
    RITvScreen *tvScreen = theApp.GetTvScreen();
    RIRemote* remote;
    if (m_remoteViewOn)
        remote = theApp.GetRemote();
    else
        remote = NULL;
    RIFrontPanel *frontpanel;
    if (m_frontpanelViewOn)
        frontpanel = theApp.GetFrontPanel();
    else
        frontpanel = NULL;

    // Toggle console view state.
    if (m_consoleViewOn)
    {
        (void) console->Hide();
        (void) InitLayout(tvScreen, remote, frontpanel, NULL);
        m_consoleViewOn = false;
    }
    else
    {
        (void) console->Show();
        (void) InitLayout(tvScreen, remote, frontpanel, console);
        m_consoleViewOn = true;
    }
}

void RIEmulatorFrame::OnRemoteViewControl(wxCommandEvent &event)
{
    RILOG_DEBUG("Handling Remote view control.\n");

    RIEmulatorApp &theApp = wxGetApp();
    RIRemote *remote = theApp.GetRemote();
    RITvScreen *tvScreen = theApp.GetTvScreen();
    RIConsole* console;
    if (m_consoleViewOn)
        console = theApp.GetConsole();
    else
        console = NULL;
    RIFrontPanel *frontpanel;
    if (m_frontpanelViewOn)
        frontpanel = theApp.GetFrontPanel();
    else
        frontpanel = NULL;

    // Toggle remote view state.
    if (m_remoteViewOn)
    {
        (void) remote->Hide();
        (void) InitLayout(tvScreen, NULL, frontpanel, console);
        m_remoteViewOn = false;
    }
    else
    {
        (void) remote->Show();
        (void) InitLayout(tvScreen, remote, frontpanel, console);
        m_remoteViewOn = true;
    }
}

void RIEmulatorFrame::OnFrontPanelViewControl(wxCommandEvent &event)
{
    RILOG_DEBUG("Handling Front Panel view control.\n");

    RIEmulatorApp &theApp = wxGetApp();
    RIFrontPanel *frontpanel = theApp.GetFrontPanel();
    RITvScreen *tvScreen = theApp.GetTvScreen();
    RIRemote* remote;
    if (m_remoteViewOn)
        remote = theApp.GetRemote();
    else
        remote = NULL;
    RIConsole *console;
    if (m_consoleViewOn)
        console = theApp.GetConsole();
    else
        console = NULL;

    // Toggle front panel view state.
    if (m_frontpanelViewOn)
    {
        (void) frontpanel->Hide();
        (void) InitLayout(tvScreen, remote, NULL, console);
        m_frontpanelViewOn = false;
    }
    else
    {
        (void) frontpanel->Show();
        (void) InitLayout(tvScreen, remote, frontpanel, console);
        m_frontpanelViewOn = true;
    }
}

RIEmulatorFrame::RIEmulatorFrame(const wxString &title, wxPoint position,
        long style) :
    wxFrame(NULL, wxID_ANY, title, position, wxDefaultSize, style)
{
    // Set the frame icon.
    SetIcon( wxIcon(tru2way_16x16_color_xpm));

    m_topWidget = new wxPanel(this, -1);

    // Create the menu bar.
    wxMenu *fileMenu = new wxMenu();

    // The "About" item should be in the help menu.
    wxMenu *helpMenu = new wxMenu();
    (void) helpMenu->Append(wxID_ABOUT, wxT("&About...\tF1"), wxT(
            "Show about dialog"));

    // Add view control menu.
    wxMenu *controlMenu = new wxMenu();
    (void) controlMenu->Append(ID_CONSOLE_VIEW_CONTROL,
            wxT("Show/hide Console"), wxT("Show/hide console window"));
    (void) controlMenu->Append(ID_REMOTE_VIEW_CONTROL, wxT("Show/hide Remote"),
            wxT("Show/hide remote window"));
    (void) controlMenu->Append(ID_FRONTPANEL_VIEW_CONTROL, wxT(
            "Show/hide Front Panel"), wxT("Show/hide front panel window"));

    (void) fileMenu->Append(wxID_EXIT, wxT("E&xit\tAlt-X"), wxT(
            "Quit this program"));

    // Now append the freshly created menu to the menu bar.
    wxMenuBar *menuBar = new wxMenuBar();
    (void) menuBar->Append(fileMenu, wxT("&File"));
    (void) menuBar->Append(controlMenu, wxT("&Control"));
    (void) menuBar->Append(helpMenu, wxT("&Help"));

    // And attach this menu bar to the frame.
    (void) SetMenuBar(menuBar);

    // Create a status bar.
    (void) CreateStatusBar(2);
    (void) SetStatusText(wxT("Welcome to RI Emulator!"));

    m_consoleViewOn = false;
    m_remoteViewOn = false;
    m_frontpanelViewOn = false;
}

RIEmulatorFrame::~RIEmulatorFrame()
{
    // Tell the GStreamer pipeline to stop refreshing video/graphics.
    // This catches the case where the window hierarchy is destroyed by the user
    // selecting the window close button.
    RIEmulatorApp & theApp = wxGetApp();
    UIInfo *context = theApp.GetDisplayContext();
    context->pWindowInfo->win = 0;

    if (m_topWidget != NULL)
        delete m_topWidget;
}

bool RIEmulatorFrame::InitLayout(wxWindow *tvScreen, wxWindow *remote,
        wxWindow *frontPanel, wxWindow *console)
{
    // Validate the parameters.
    if (tvScreen == NULL)
    {
        RILOG_ERROR("Invalid input argument; tvScreen is NULL");
        return FALSE;
    }

    // Create layout sizers.
    wxBoxSizer *topSizer = new wxBoxSizer(wxVERTICAL);
    wxBoxSizer *remoteSizer = new wxBoxSizer(wxHORIZONTAL);
    wxBoxSizer *consoleSizer = new wxBoxSizer(wxVERTICAL);

    (void) topSizer->Add(tvScreen, /* The window to be added - TvScreen window. */
    0, /* Indicate whether child can resize - no. */
    wxALL, /* Flags for border and expansion policy. */
    2); /* Size of border - 2 pixels. */

    if (frontPanel)
    {
        (void) topSizer->Add(frontPanel, /* The window to be added - FrontPanel window. */
        0, /* Indicate whether child can resize - no. */
        wxALL, /* Flags for border and expansion policy. */
        2); /* Size of border - 2 pixels. */

        m_frontpanelViewOn = true;
    }

    (void) remoteSizer->Add(topSizer, /* The top vertical sizer. */
    0, /* Indicate whether child can resize - no. */
    wxEXPAND | wxALL, /* Flags for border and expansion policy. */
    2); /* Size of border - 2 pixels. */

    if (remote)
    {
        (void) remoteSizer->Add(remote, /* The window to be added - Remote window. */
        0, /* Indicate whether child can resize - no. */
        wxALL, /* Flags for border and expansion policy. */
        2); /* Size of border - 2 pixels. */

        m_remoteViewOn = true;
    }

    if (console)
    {
        (void) consoleSizer->Add(remoteSizer, /* The remote horizontal sizer. */
        0, /* Indicate whether top sizer can resize - no. */
        wxEXPAND | wxALL, /* Flags for border and expansion policy. */
        2); /* Size of border - 2 pixels. */
        (void) consoleSizer->Add(console, /* The window to be added - Console window. */
        1, /* Indicate whether child can resize - yes. */
        wxEXPAND | wxALL, /* Flags for border and expansion policy. */
        2); /* Size of border - 2 pixels. */

        m_consoleViewOn = true;
    }

    wxSize size, minSize;
    if (console)
    {
        m_topWidget->SetSizer(consoleSizer);
        minSize = consoleSizer->GetMinSize();
    }
    else
    {
        m_topWidget->SetSizer(remoteSizer);
        minSize = remoteSizer->GetMinSize();
    }

    size.SetWidth(minSize.GetWidth());
    size.SetHeight(minSize.GetHeight());
    SetClientSize(size.GetWidth(), size.GetHeight());

    return TRUE;
}
