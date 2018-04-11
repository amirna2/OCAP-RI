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

package org.cablelabs.xlet.RiExerciser.ui;

import java.awt.Font;
import java.awt.event.KeyEvent;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This page displays the DVR Menu options. This general DVR menu page is only
 * accessible from the RiExerciser General Menu if DVR extensions are enabled,
 * thus it is not necessary to verify that DVR extensions are enabled before
 * making calls to OcapAppDriverDVR. For future additions, any HN functionality
 * that might be added would need to follow a verification that HN extensions
 * are enabled. Currently, no HN functionality is required of the Pages accessed
 * from this Page.
 *
 */
public class DvrMenuPage extends QuarterPage
{
    /**
     * The Singleton instance of this Page
     */
    private static DvrMenuPage m_page = new DvrMenuPage();
    
    /**
     * The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverDVR from RiExerciserController for DVR functionality
     */
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    /**
     * A boolean indicating whether buffering is requested or not
     */
    private boolean m_bufferingRequested;
    
    private DvrMenuPage()
    {
        m_bufferingRequested = false;
        m_controller = RiExerciserController.getInstance();
        m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        // Initialize components
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        m_menuBox.write("DVR Menu:");
        m_menuBox.write("0. Return to RiExerciser General Menu");
        m_menuBox.write("1. Start a Recording");
        m_menuBox.write("2. Playback a Recording");
        m_menuBox.write("3. Delete a Recording");
        m_menuBox.write("4. Erase all recordings");
        m_menuBox.write("5. Disable Buffering");
        m_menuBox.write("6. Display Media Control Menu");
        m_menuBox.write("7. Enable/Disable Buffering Request");
        m_menuBox.write("8. Display options for non-selected channel");
        
        // Initialize the Container layout.
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.setVisible(true);
    }
    
    public static DvrMenuPage getInstance()
    {
        return m_page;
    }

    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
                // Menu Option 0: Return to Ri Exerciser General Menu
                m_controller.displayNewPage(RiExerciserConstants.GENERAL_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Start a Recording
                m_controller.displayNewPage(RiExerciserConstants.RECORDING_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Playback a recording
                m_controller.displayNewPage(RiExerciserConstants.DVR_PLAYBACK_PAGE);
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Delete a recording
                m_controller.displayNewPage(RiExerciserConstants.DVR_DELETE_MENU_PAGE);
                break;
            }
            
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Erase all recordings
                
                /**
                 * If there are no recordings, log a message indicating so,
                 * otherwise, log whether the recordings were deleted or not
                 */
                if (m_oadDVR.getNumRecordings() == 0)
                {
                    m_controller.displayMessage("No recordings to delete.");
                }
                else
                {
                    boolean deleted = m_oadDVR.deleteAllRecordings();
                    if (deleted)
                    {
                        m_controller.displayMessage("All recordings deleted.");
                    }
                    else
                    {
                        m_controller.displayMessage("Error deleting recordings.");
                    }
                }
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Disable buffering
                m_oadDVR.toggleBufferingEnabled();
                if (m_oadDVR.isBufferingEnabled())
                {
                    m_controller.displayMessage("Buffering enabled");
                }
                else
                {
                    m_controller.displayMessage("Buffering disabled");
                }
                break;
            }
            case OCRcEvent.VK_6:
            {
                // Menu Option 6: Display Media Control Menu
                m_controller.displayNewPage(RiExerciserConstants.MEDIA_CONTROL_PAGE);
                break;
            }
            case OCRcEvent.VK_7:
            {
                // Menu Option 7: Enable buffering request
                m_bufferingRequested = !m_bufferingRequested;
                if (m_bufferingRequested)
                {
                    m_controller.displayMessage("Buffering requested");
                }
                else
                {
                    m_controller.displayMessage("Buffering request cancelled");
                }
                int currentServiceIndex = m_controller.getCurrentService();
                m_oadDVR.toggleBufferingRequest(currentServiceIndex);
                break;
            }
            case OCRcEvent.VK_8:
            {
                // Menu Option 8: Display options for non-selected channel
                m_controller.displayNewPage(RiExerciserConstants.DVR_NON_SELECTED_CHANNEL_MENU_PAGE);
                break;
            }
        }
    }

    public void destroy()
    {
        m_menuBox.setVisible(false);
        
    }

    public void init()
    {
        m_menuBox.setVisible(true);
        
    }

}
