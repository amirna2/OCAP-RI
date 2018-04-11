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
 * RIConsole.h
 *
 *  Created on: Jan 30, 2009
 *      Author: Mark Millard
 */

#ifndef _RI_CONSOLE_H_
#define _RI_CONSOLE_H_

// Include wxWidgets header files.
#include <wx/wx.h>
#include <wx/textctrl.h>
#ifdef __WXMSW__
#include <winsock2.h>
#endif /* __WXMSW__ */
#if defined(__WXX11__) || defined(__WXGTK__)
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#endif /* __WXX11 */
#include <glib.h>

// Include RI Emulator header files.
//#include "ui_info.h"

// If not on Windows, adapt to some Windows-like conventions.
#ifndef __WXMSW__
typedef int SOCKET;
#define INVALID_SOCKET -1
#define SOCKET_ERROR -1
#define SOCKADDR_IN struct sockaddr_in
#define closesocket close
#endif /* __WXMSW__ */

/**
 * The RI Emulator Trace control event.
 * <p>
 * This event is used to post messages to the <code>RITraceCtrl</code>.
 * </p>
 */
class RITraceCtrlEvent: public wxNotifyEvent
{
public:

    RITraceCtrlEvent(wxEventType commandType = wxEVT_NULL, int id = 0) :
        wxNotifyEvent(commandType, id), m_msg(NULL)
    { /* Do nothing extra. */
    }

    RITraceCtrlEvent(const RITraceCtrlEvent &event) :
        wxNotifyEvent(event)
    {
        m_msg = new wxString(*(event.m_msg));
    }

    virtual ~RITraceCtrlEvent()
    {
        if (m_msg)
            delete m_msg;
    }

    virtual wxEvent *Clone() const
    {
        return new RITraceCtrlEvent(*this);
    }

    virtual void SetMessage(wxString *msg);

    virtual wxString *GetMessage();

private:

    // The message payload.
    wxString *m_msg;

    /*lint -e(19) -e(1516)*/
    DECLARE_DYNAMIC_CLASS( RITraceCtrlEvent);
};

// The event handler function.
typedef void (wxEvtHandler::*RITraceCtrlEventFunction)(RITraceCtrlEvent &);
#define riID_CONSOLE_MSG 801

// RITraceCtrl events and macros for handling them.
BEGIN_DECLARE_EVENT_TYPES()
DECLARE_EVENT_TYPE(riEVT_COMMAND_CONSOLE_MESSAGE, 801)
END_DECLARE_EVENT_TYPES()

#define EVT_CONSOLE_MESSAGE(id, fn)                                                \
    DECLARE_EVENT_TABLE_ENTRY(riEVT_COMMAND_CONSOLE_MESSAGE, id, -1,               \
        (wxObjectEventFunction) (wxEventFunction) (RITraceCtrlEventFunction) &fn,  \
        (wxObject *) NULL),

/**
 * The RI Emulator Trace control.
 */
class RITraceCtrl : public wxTextCtrl
{
public:

    /**
     * A constructor that creates and shows a text control.
     *
     * @param parent The parent window. Should not be <b>NULL</b>.
     * @param id The control identifier. A value of <b>-1</b> denotes a default value.
     * @param value The default text value.
     * @param pos The text control position.
     * @param size The text control size.
     * @param style The window style.
     * @param validator The window validator.
     * @param name The window name.
     */
    RITraceCtrl(wxWindow* parent, wxWindowID id, const wxString& value = "",
            const wxPoint& pos = wxDefaultPosition, const wxSize& size = wxDefaultSize,
            long style = 0, const wxValidator& validator = wxDefaultValidator,
            const wxString& name = wxTextCtrlNameStr);

    /**
     * The destructor.
     */
    virtual ~RITraceCtrl();

    /**
     * Override wxTextCtrl, disabling ability to cut text.
     */
    virtual bool CanCut() const
    {
        return FALSE;
    }

    /**
     * Override wxTextCtrl, disabling ability to paste text.
     */
    virtual bool CanPaste() const
    {
        return FALSE;
    }

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
     * The right mouse button pressed event handler.
     *
     * @param event The mouse event.
     */
    void OnRightClick(wxMouseEvent &event);

    /**
     * The menu command event handler for clearing the text control.
     *
     * @param event The menu command event.
     */
    void OnClear(wxCommandEvent &event);

    /**
     * The UI update event handler for enabling the Clear menu entry.
     * <p>
     * Normally the Clear menu entry is disabled since the wxTextCtrl
     * is read only. This handler is a workaround to enable the Clear
     * menu entry so that the control can be cleared regardless of its
     * editable state.
     * </p>
     *
     * @param event The update UI event.
     */
    void OnUpdateClear(wxUpdateUIEvent &event);

    void OnConsoleMessage(RITraceCtrlEvent &event);

private:

    // The context menu.
    wxMenu *m_menu;

    // This class handles events.
    /*lint -e(1516)*/
    DECLARE_EVENT_TABLE()
};

/**
 * The RI Emulator Console window.
 */
class RIConsole: public wxPanel
{
public:

    /** The default Server port, if one isn't provided via a configuration file. */
    static const int DEFAULT_SERVER_PORT = 51400;

    /**
     * A constructor that creates a window for the RI Emulator console.
     *
     * @param parent The window's parent.
     * @param name The window's name.
     * @param style The style template.
     * @param size The width and height of the window to create.
     */
    RIConsole(wxWindow *parent, const wxString &name, long style,
            const wxSize size);

    /**
     * The destructor.
     */
    virtual ~RIConsole();

    /**
     * Get the Server's port number.
     *
     * @return The port identifier is returned.
     */
    unsigned short GetPort()
    {
        return m_port;
    }

    /**
     * Set the Server's port number.
     *
     * @param port The port identifier.
     */
    void SetPort(unsigned short port);

    /**
     * Start logging messages to the console.
     */
    void StartLogging();

    /**
     * Stop logging messages to the console.
     */
    void StopLogging();

private:

    // Initialize the console.
    bool InitConsole();
    // Shutdown the console.
    void ShutdownConsole();
    // Process messages.
    static int ProcessMessages(RIConsole *console);

    // The Server port.
    unsigned short m_port;
    // The UDP socket.
    SOCKET m_socket;
    // Flag indicating whether Windows sockets has been initialized.
    bool m_initialized;
    // The thread for processing messages.
    GThread *m_msgThread;
    // The mutex for the message processing thread.
    static GMutex *g_msgMutex;
    // Flag indicating whether to continue processing messages.
    static bool g_processingMsgs;

    // The text control widget.
    RITraceCtrl *m_textControl;
};

#endif /* _RI_CONSOLE_H_ */
