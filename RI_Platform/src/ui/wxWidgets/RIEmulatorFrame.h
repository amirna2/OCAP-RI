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
 * RIEmulatorFrame.h
 *
 *  Created on: Jan 7, 2009
 *      Author: Mark Millard
 */

#ifndef _RI_EMULATORFRAME_H_
#define _RI_EMULATORFRAME_H_

// Include wxWidgets header files.
#include <wx/wx.h>

/**
 * The main window for the RI Emulator.
 */
class RIEmulatorFrame: public wxFrame
{
public:

    /**
     * A constructor.
     *
     * @param title The frame's title.
     * @param style The frame's behavior.
     * @param position The frame's position.
     */
    RIEmulatorFrame(const wxString &title, wxPoint position, long style);

    /**
     * The destructor.
     */
    ~RIEmulatorFrame();

    // Event handlers.

    /**
     * The quit event handler.
     * <p>
     * This event is generated when the user exits via the
     * frame's menu.
     * </p>
     *
     * @param event A command event for the quit menu event
     */
    void OnQuit(wxCommandEvent &event);

    /**
     * The about event handler.
     *
     * @param event A command event for the about menu event.
     */
    void OnAbout(wxCommandEvent &event);

    /**
     * The window close event handler.
     * <p>
     * This event is generated using the window manager (X) or system
     * menu (Windows).
     * </p>
     *
     * @param event The close event.
     */
    void OnClose(wxCloseEvent &event);

    /**
     * Control the Console window view.
     *
     * @param event A command event for the Console show/hide menu event
     */
    void OnConsoleViewControl(wxCommandEvent &event);

    /**
     * Control the Remote window view.
     *
     * @param event A command event for the Remote show/hide menu event
     */
    void OnRemoteViewControl(wxCommandEvent &event);

    /**
     * Control the Front Panel window view.
     *
     * @param event A command event for the Front Panel show/hide menu event
     */
    void OnFrontPanelViewControl(wxCommandEvent &event);

    /**
     * Retrieve the top-level widget.
     *
     * @return A pointer to the window that is the top-level widget
     * in the window hierarchy is returned.
     */
    wxWindow *GetTopWidget()
    {
        return m_topWidget;
    }

    /**
     * Initialize the frame's layout.
     *
     * @param tvScreen The TvScreen window.
     * @param remote The Remote window.
     * @param frontPanel The Front Panel window.
     * @param console The Console window.
     *
     * @return If the layout is successful, then <b>TRUE</b> will be returned.
     * Otherwise <b>FALSE</b> will be returned.
     */
    bool InitLayout(wxWindow *tvScreen, wxWindow *remote, wxWindow *frontPanel,
            wxWindow *console);

private:

    // The top-level wdiget.
    wxPanel *m_topWidget;
    // Flag indicating console window is being shown.
    bool m_consoleViewOn;
    // Flag indicating remote window is being shown.
    bool m_remoteViewOn;
    // Flag indicating front panel windlw is being shown.
    bool m_frontpanelViewOn;

    // This class handles events.
    /*lint -e(1516)*/
DECLARE_EVENT_TABLE()
};

enum
{
    ID_CONSOLE_VIEW_CONTROL = 10000,
    ID_REMOTE_VIEW_CONTROL = 10001,
    ID_FRONTPANEL_VIEW_CONTROL = 10002
};

#endif /* _RI_EMULATORFRAME_H_ */
