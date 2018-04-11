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
 * RIConsole.cpp
 *
 *  Created on: Jan 30, 2009
 *      Author: Mark Millard
 */

//lint -e429 -e1579

// Include system header files.
#include <wx/sizer.h>

// Include RI Emulator header files.
#include "ri_log.h"
#include "ui_window.h"
#include "RIConsole.h"

#define SOCKET_DGRAM_SIZE 1024

// Logging category
#define RILOG_CATEGORY g_uiConsoleCat
log4c_category_t* g_uiConsoleCat;

// RITraceCtrlEvent implementation.
DEFINE_EVENT_TYPE( riEVT_COMMAND_CONSOLE_MESSAGE)
IMPLEMENT_DYNAMIC_CLASS(RITraceCtrlEvent, wxNotifyEvent)

// Event table for RITraceCtrl.
BEGIN_EVENT_TABLE(RITraceCtrl, wxTextCtrl)
EVT_KEY_UP(RITraceCtrl::OnKeyUp)
EVT_KEY_DOWN(RITraceCtrl::OnKeyDown)
EVT_ENTER_WINDOW(RITraceCtrl::OnEnterWindow)
EVT_RIGHT_DOWN(RITraceCtrl::OnRightClick)
EVT_MENU(wxID_CLEAR, RITraceCtrl::OnClear)
EVT_UPDATE_UI(wxID_CLEAR, RITraceCtrl::OnUpdateClear)
EVT_CONSOLE_MESSAGE(wxID_ANY, RITraceCtrl::OnConsoleMessage)
END_EVENT_TABLE()

// Flag for starting/stopping message processing.
bool RIConsole::g_processingMsgs;

// The message thread mutex.
GMutex *RIConsole::g_msgMutex = NULL;

void RITraceCtrlEvent::SetMessage(wxString *msg)
{
    if (m_msg)
        delete m_msg;
    m_msg = new wxString(*msg);
}

wxString *RITraceCtrlEvent::GetMessage()
{
    return m_msg;
}

RITraceCtrl::RITraceCtrl(wxWindow* parent, wxWindowID id,
        const wxString& value, const wxPoint& pos, const wxSize& size,
        long style, const wxValidator& validator, const wxString& name) :
    wxTextCtrl(parent, id, value, pos, size, style, validator, name)
{
    m_menu = NULL;
}

RITraceCtrl::~RITraceCtrl()
{
    if (m_menu)
        delete m_menu;
}

void RITraceCtrl::OnKeyUp(wxKeyEvent &event)
{
    int code = event.GetKeyCode();
    //RILOG_DEBUG("Handling wxKeyEvent Up: %d.\n", code);

    //if (event.GetModifiers() == wxMOD_CONTROL)  /* Use this with version 2.8.9 of wxWidgets. */
#ifdef __WXMSW__
    if (event.ControlDown() && !event.AltDown() && !event.ShiftDown() && !event.MetaDown())
#endif
#if defined(__WXX11__) || defined(__WXGTK__)
    // MetaDown not supported on wxWidgets v2.6.4 under Linux.
    if (event.ControlDown() && !event.AltDown() && !event.ShiftDown())
#endif
    {
        if (code == 0x41)
            // Test for Ctrl-A; Select All
            this->SetSelection(-1, -1);
        else if (code == 0x43)
            // Test for Ctrl-C; Copy
            this->Copy();
    }
else// Pass the event along to the RI Emulator.
window_handle_key_event(code, true);
}

void RITraceCtrl::OnKeyDown(wxKeyEvent &event)
{
    int code = event.GetKeyCode();
    //RILOG_DEBUG("Handling wxKeyEvent Down: %d.\n", code);
    window_handle_key_event(code, false);
}

void RITraceCtrl::OnEnterWindow(wxMouseEvent &event)
{
    // Set focus to the wxTextCtrl; this facilitates
    // key event processing.
    SetFocus();
}

void RITraceCtrl::OnRightClick(wxMouseEvent &event)
{
    if (!m_menu)
    {
        m_menu = new wxMenu();
        (void) m_menu->Append(wxID_COPY, wxT("Copy\tCtrl+C"));
        (void) m_menu->Append(wxID_CLEAR, wxT("Clear"));
        (void) m_menu->AppendSeparator();
        (void) m_menu->Append(wxID_SELECTALL, wxT("Select All\tCtrl+A"));
    }

    (void) PopupMenu(m_menu, event.GetPosition());
}

void RITraceCtrl::OnClear(wxCommandEvent &event)
{
    this->Clear();
}

void RITraceCtrl::OnUpdateClear(wxUpdateUIEvent &event)
{
    if (m_menu)
        m_menu->Enable(wxID_CLEAR, TRUE);
}

void RITraceCtrl::OnConsoleMessage(RITraceCtrlEvent &event)
{
    //printf("***** OnConsoleMessage event *****");
    wxString *msg = event.GetMessage();
    AppendText(*msg);
}

RIConsole::RIConsole(wxWindow *parent, const wxString &name, long style,
        const wxSize size) :
    wxPanel(parent, -1, wxDefaultPosition, size, style, name),
            m_msgThread(NULL)
{
    // Initialize logging for Console functionality.
    g_uiConsoleCat = log4c_category_get("RI.UI.Console");

    g_msgMutex = g_mutex_new();

    // Create the text control widget.
    m_textControl = new RITraceCtrl(this, /* The parent window. */
    wxID_ANY, /* The window identifier. */
    wxEmptyString, /* Default text value. */
    wxDefaultPosition, /* The control's position. */
    wxSize(size.GetWidth(), size.GetHeight()), /* The control's size. */
    wxTE_MULTILINE | wxTE_READONLY); /* Style flags. */

    // Create a layout sizer.
    wxBoxSizer *sizer = new wxBoxSizer(wxVERTICAL);
    (void) sizer->Add(m_textControl, /* The window to be added. */
    1, /* Indicate whether child can resize. */
    wxEXPAND | wxALL, /* Flags for border and expansion policy. */
    1); /* Size of border. */
    SetSizer(sizer);

    m_socket = INVALID_SOCKET;
    m_initialized = FALSE;
    m_port = 0;
}

RIConsole::~RIConsole()
{
    ShutdownConsole();
    if (m_textControl)
        delete m_textControl;
    g_mutex_free( g_msgMutex);
}

void RIConsole::SetPort(unsigned short port)
{
    if (m_port != port)
        ShutdownConsole();
    m_port = port;
}

void RIConsole::StartLogging()
{
    GError *err;

    // Make sure that the console has been initialized.
    if (!m_initialized)
        if (!InitConsole())
        {
            RILOG_ERROR("Unable to initialize logging.\n");
            return;
        }

    // Start a thread to process incoming messages.
    m_msgThread = g_thread_create((GThreadFunc) ProcessMessages, this, TRUE,
            &err);
}

void RIConsole::StopLogging()
{
    if (m_msgThread == NULL)
        return;

    // Flag the message thread to stop processing messages.
    g_mutex_lock( g_msgMutex);
    g_processingMsgs = TRUE;
    g_mutex_unlock(g_msgMutex);

    // Wait until message thread finishes.
    (void) g_thread_join(m_msgThread);

    m_msgThread = NULL;
}

bool RIConsole::InitConsole()
{
#ifdef __WXMSW__
    WSADATA wsd;
#endif /* __WXMSW__ */
    struct sockaddr_in sockAddrIn;

#ifdef __WXMSW__
    // Initialize Windows sockets.
    if (WSAStartup(MAKEWORD(2, 2), &wsd))
    {
        RILOG_ERROR("Unable to initialize Windows sockets.\n");
        return FALSE;
    }
#endif /* __WXMSW__ */

    // Create socket for sending/receiving datagramms.
    m_socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
#ifdef __WXMSW__
    if (m_socket == INVALID_SOCKET)
#else
    if (m_socket == -1)
#endif
    {
        RILOG_ERROR("Unable to create a socket.\n");
        ShutdownConsole();
        return FALSE;
    }

    // Construct the local address structure.
    memset(&sockAddrIn, 0, sizeof(SOCKADDR_IN));
    sockAddrIn.sin_family = AF_INET;
    sockAddrIn.sin_addr.s_addr = htonl(INADDR_ANY); /* Any incoming interface */
    sockAddrIn.sin_port = htons(m_port); /* Local port */

    // Bind to the local address.
    if (bind(m_socket, (struct sockaddr *) &sockAddrIn, sizeof(sockaddr)) < 0)
    {
        RILOG_ERROR("Unable to bind to local address.\n");
        ShutdownConsole();
        return FALSE;
    }

    m_initialized = TRUE;
    return TRUE;
}

void RIConsole::ShutdownConsole()
{
    // Stop processing messages.
    StopLogging();

    // Clean up sockets.
    if (m_socket != INVALID_SOCKET)
    {
        (void) closesocket(m_socket);
        m_socket = INVALID_SOCKET;
    }
    if (m_initialized)
    {
#ifdef __WXMSW__
        WSACleanup();
#endif /* __WXMSW__ */
        m_initialized = FALSE;
    }
}

int RIConsole::ProcessMessages(RIConsole *console)
{
    int len; // The size of the client's address.
    struct sockaddr_in sockAddrIn; // The client's address.
    int recvMsgSize;
    char msgBuffer[SOCKET_DGRAM_SIZE];

    g_processingMsgs = FALSE;
    while (!g_processingMsgs)
    {
        // Socket descriptor sets.
        fd_set readFDS;
        fd_set xcptFDS;
        struct timeval timeOut;

        // Lock the message thread.
        g_mutex_lock( g_msgMutex);

        // Clear all sockets from the FDS structure, then put our socket
        // into the socket descriptor set.
        FD_ZERO(&readFDS);
        FD_ZERO(&xcptFDS);
        FD_SET(console->m_socket, &readFDS);
        FD_SET(console->m_socket, &xcptFDS);

        // Initialize the timeout value to 1 second. TODO - may want to make this
        // configurable.
        timeOut.tv_sec = 1;
        timeOut.tv_usec = 0;

        // Call select() to check for readability until timeout.
#ifdef __WXMSW__
        int nfds = -1; // Ignored by Windows.
#else
        int nfds = console->m_socket + 1;
#endif /* __WXMSW__ */
        int retVal = select(nfds, &readFDS, NULL, &xcptFDS, &timeOut);
        if (retVal == SOCKET_ERROR)
        {
            // select() failed. Exit from the process message loop.
            RILOG_ERROR("select() failed.\n");
            g_mutex_unlock(g_msgMutex);
            g_processingMsgs = TRUE;
            continue;
        }
        else if (retVal != 0)
        {
            // Check for exception first.
            if (FD_ISSET(console->m_socket, &xcptFDS))
            {
                RILOG_DEBUG("A select() exception occurred.\n");
                g_mutex_unlock(g_msgMutex);
                g_processingMsgs = TRUE;
                continue;
            }
            if (!(FD_ISSET(console->m_socket, &readFDS)))
            {
                // This should never happen!!! If select returned a positive
                // value, something should be set in either our exception or
                // our read socket set.
                RILOG_ERROR("select() successful, but corrupt fd_set\n");
                g_mutex_unlock(g_msgMutex);
                g_processingMsgs = TRUE;
                continue;
            }
        }
        else
        {
            // select() timed out, unlock the message thread and continue;
            // thus checking if we should still be processing messages.
            RILOG_DEBUG("select() timed out,\n");
            g_mutex_unlock(g_msgMutex);
#ifdef __WXGTK__
            g_usleep(10000);
#endif /* __WXGTK__ */
            continue;
        }

        // If we're here we know our socket was in readFDS socket set, so we
        // should be able to receive data from it.

        // Set the size of the in-out parameter.
        len = sizeof(sockaddr);

        // Block until we receive message from a client. It should return immediately
        // since we know there is something in our readFDS set.
#ifndef __WXMSW__
        if ((recvMsgSize = recvfrom(console->m_socket, msgBuffer,
                SOCKET_DGRAM_SIZE, 0, (struct sockaddr *) &sockAddrIn,
                (socklen_t *) &len)) < 0)
#else
        if ((recvMsgSize = recvfrom(console->m_socket, msgBuffer, SOCKET_DGRAM_SIZE, 0,
                                (struct sockaddr *) &sockAddrIn, &len)) < 0)
#endif /* ! __WXMSW__ */
        {
            RILOG_ERROR("Error reading from socket.\n");
            g_processingMsgs = TRUE;
        }
        else
        {
            //printf("Handling client %s\n", inet_ntoa(sockaddr.sin_addr));

            // Post the message to the text control for display.
            //msgBuffer[recvMsgSize] = '\0';
            //wxString *msg = new wxString(msgBuffer, recvMsgSize);
            //console->m_textControl->AppendText(*msg);
            //delete msg;
            wxString *msg = new wxString(msgBuffer, recvMsgSize);
            RITraceCtrlEvent event(riEVT_COMMAND_CONSOLE_MESSAGE,
                    console->GetId());
            event.SetEventObject(console);
            event.SetMessage(msg); // A copy of the message is made in the event.
            wxPostEvent(console->m_textControl, event);
            delete msg;
        }

        // Unlock the message thread.
        g_mutex_unlock(g_msgMutex);
    }

    return 0;
}
