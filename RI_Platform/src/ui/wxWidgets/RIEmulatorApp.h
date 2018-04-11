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
 * RIEmulatorApp.h
 *
 *  Created on: Jan 7, 2009
 *      Author: Mark Millard
 */

#ifndef _RI_EMULATORAPP_H_
#define _RI_EMULATORAPP_H_

// Include wxWidgets header files.
#include <wx/wx.h>

// Include RI Emulator header files.
#include "ui_info.h"
#include "RITvScreen.h"
#include "RIConsole.h"
#include "RIRemote.h"
#include "RIFrontPanel.h"

// Declare external classes.
class RIEmulatorFrame;

/**
 * The RI Emulator application.
 */
class RIEmulatorApp: public wxApp
{
public:

    /**
     * The default constructor.
     */
    RIEmulatorApp();

    /**
     * A constructor that initializes the screen context.
     *
     * @param uiInfo A point to the screen context.
     */
    RIEmulatorApp(UIInfo *uiInfo);

    /**
     * The destructor.
     */
    ~RIEmulatorApp();

    /**
     * Initialize the application.
     * <p>
     * Called on application startup via <code>wxEntry()</code>.
     * </p>
     */
    virtual bool OnInit();

    /**
     * Exit the application.
     */
    virtual int OnExit();

    /**
     * Retrieve the associated frame widget.
     *
     * @return A pointer to the frame is returned.
     */
    RIEmulatorFrame *GetFrame()
    {
        return m_frame;
    }

    /**
     * Retrieve the associated TV Screen.
     *
     * @return A pointer to the TV Screen is returned.
     */
    RITvScreen *GetTvScreen()
    {
        return m_tvScreen;
    }

    /**
     * Set the associated TV Screen.
     *
     * @param tvScreen A pointer to the TV Screen to use with the RI
     * Emulator application.
     */
    void SetTvScreen(RITvScreen *tvScreen);

    /**
     * Retrieve the associated Console.
     *
     * @return A pointer to the Console is returned.
     */
    RIConsole *GetConsole()
    {
        return m_console;
    }

    /**
     * Set the associated Console.
     *
     * @param console A pointer to the Console to use with the RI
     * Emulator application.
     */
    void SetConsole(RIConsole *console);

    /**
     * Retrieve the associated Remote.
     *
     * @return A pointer to the Remote is returned.
     */
    RIRemote *GetRemote()
    {
        return m_remote;
    }

    /**
     * Set the associated Remote.
     *
     * @param remote A pointer to the Remote to use with the RI
     * Emulator application.
     */
    void SetRemote(RIRemote *remote);

    /**
     * Retrieve the associated Front Panel.
     *
     * @return A pointer to the Front Panel is returned.
     */
    RIFrontPanel *GetFrontPanel()
    {
        return m_frontPanel;
    }

    /**
     * Set the associated Front Panel.
     *
     * @param frontPanel A pointer to the Front Panel to use with the RI
     * Emulator application.
     */
    void SetFrontPanel(RIFrontPanel *frontPanel);

    /**
     * Shutdown the application gracefully.
     *
     * @param close Flag indicating whether to close the the window or not.
     * If <b>true</b>, close the window; otherwise, if <b>false> then don't
     * explicitly close the window.
     */
    void Shutdown(bool close);

    /**
     * Get the display context.
     *
     * @return A pointer to the context is returned.
     */
    UIInfo *GetDisplayContext()
    {
        return m_display;
    }

private:

    // Create the OpenGL window for the TV Screen window.
    unsigned long CreateTvScreen(wxWindow *parent, unsigned int width,
            unsigned int height, bool isFixed);
    // Create the window for the console.
    unsigned long CreateConsole(wxWindow *parent, unsigned int width,
            unsigned int height);
    // Create the window for the remote.
    unsigned long CreateRemote(wxWindow *parent, unsigned int width,
            unsigned int height);
    // Create the window for the front panel.
    unsigned long CreateFrontPanel(wxWindow *parent, unsigned int width,
            unsigned int height);
    // Set the SubWindow configurations.
    void SetSubWindowConfig();

    // The application's main window.
    RIEmulatorFrame *m_frame;
    // The RI Emulator TV Screen window.
    RITvScreen *m_tvScreen;
    // The RI Emulator Console window.
    RIConsole *m_console;
    // The RI Emulator Remote window.
    RIRemote *m_remote;
    // The RI Emulator Front Panel window.
    RIFrontPanel *m_frontPanel;
    // The display context.
    UIInfo *m_display;
    // Show the Remote.
    bool m_showRemote;
    // Shoe the Front Panel.
    bool m_showFrontPanel;
    // Show the Console.
    bool m_showConsole;
    // Frame style.
    long m_style;
    // The location for the window frame.
    wxPoint m_position;
};

// Implements RIEmulatorApp &wxGetApp()
DECLARE_APP( RIEmulatorApp)

#endif /* _RI_EMULATORAPP_H_ */
