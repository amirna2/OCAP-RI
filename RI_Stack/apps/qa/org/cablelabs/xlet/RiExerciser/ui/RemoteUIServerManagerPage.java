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
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.ui.event.OCRcEvent;

public class RemoteUIServerManagerPage extends QuarterPage
{
    private static RemoteUIServerManagerPage m_page = new RemoteUIServerManagerPage();
    
    private RiExerciserController m_controller;
    
    private OcapAppDriverInterfaceHN m_oadHN;
    
    private RemoteUIServerManagerPage()
    {
        m_controller = RiExerciserController.getInstance();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        // Initialize components
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        m_menuBox.write("Remote UI Server Manager Menu:");
        m_menuBox.write("0. Return to HN Test Menu");
        m_menuBox.write("1. Clear UIs");
        m_menuBox.write("2. Query UI");
        m_menuBox.write("3. Single UI");
        m_menuBox.write("4. Many UI");
        
        // Initialize the Container layout.
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.setVisible(true);
    }
    
    public static RemoteUIServerManagerPage getInstance()
    {
        return m_page;
    }

    public void processUserEvent(KeyEvent event)
    {
        switch (event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
            	// Menu Option 0: Return to HN Test Menu
            	m_controller.displayNewPage(RiExerciserConstants.HN_TEST_PAGE);
            	break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Clear UIs
                String listUI = null;
                m_oadHN.setUIList(listUI);
                m_controller.displayMessage ("UI cleared with setUIList(null)");
                break;
            }
            case OCRcEvent.VK_2:
            {
                //  Menu Option 2: Query UI
                int init = m_oadHN.getUpnpRuiServerIndexByName ("RemoteUI Server");
                String deviceInputProfile = "";
                String uIFilter = "\"*\"";
                String returned = m_oadHN.invokeRuissGetCompatibleUIs ( init,
                    deviceInputProfile, uIFilter);
                m_controller.displayMessage 
                   ("Listed: See UI echoed in $PLATFORMROOT/RILog.txt after the string \"returnedui\" ");
                break;
         
            }
            case OCRcEvent.VK_3:
            {
               // Menu Option 3: Single UI
                m_oadHN.setUIList(m_oadHN.getXML("SINGLE_RUI_LIST"));
                m_controller.displayMessage 
                   ("Added: See UI echoed in $PLATFORMROOT/RILog.txt after the string \"remoteui\" ");
                break;
 
            }
            case OCRcEvent.VK_4:
            {
               // Menu Option 3: Many UI
               m_oadHN.setUIList(m_oadHN.getXML("MANY_RUI_LIST"));
               m_controller.displayMessage 
                   ("Added: See UI echoed in $PLATFORMROOT/RILog.txt after the string \"remoteui\" ");
                break;
            }
        }
    }

    public void destroy()
    {

    }

    public void init()
    {

    }

}
