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

public class HNNetAuthorizationHandlerMenuPage extends QuarterPage
{
    
    public static HNNetAuthorizationHandlerMenuPage m_page = new 
        HNNetAuthorizationHandlerMenuPage();
    
    /**
     *  The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverHN from RiExerciserController for HN functionality
     */
    private OcapAppDriverInterfaceHN m_oadHN;
    
    /**
     * A boolean indicating the NAH policy
     */
    private boolean m_nahReturnPolicy = true;
    
    /**
     * A boolean indicating whether NAH2 has been registered
     */
    private boolean m_nah2Registered = false;
    
    /**
     * A boolean indicating whether only one NAH message will be delivered
     */
    private boolean m_firstNAHMessageOnly;
    
    private RiExerciserContainer m_container;
    
    private HNNetAuthorizationHandlerMenuPage()
    {
        // Initialize the components
        setLayout(null);

        m_controller = RiExerciserController.getInstance();
        m_container = RiExerciserContainer.getInstance();
        m_oadHN  = OcapAppDriverHN.getOADHNInterface();
        m_firstNAHMessageOnly = m_oadHN.getNAHFirstMessageOnlyPolicy();
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true); 
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options. Only write options for which extensions are 
        // enabled
        m_menuBox.write("HN Net Authorization Handler Options");
        m_menuBox.write("0. Return to HN Server Menu");
        m_menuBox.write("1. Register NetAuthorizationHandler2");
        m_menuBox.write("2. Change NAH policy (currently returning " + m_nahReturnPolicy + ")");
        m_menuBox.write("3. Revoke Last Activity -1");
        m_menuBox.write("4. Limit NAH message to first activity (currently " 
                        + m_firstNAHMessageOnly + ")");
    }
    
    public static HNNetAuthorizationHandlerMenuPage getInstance()
    {
        return m_page;
    }
    
    public void processUserEvent(KeyEvent event)
    {
        switch (event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
            	// Menu option 0: Return to HN Server Menu Page
            	m_controller.displayNewPage(RiExerciserConstants.HN_SERVER_OPTIONS_PAGE);
            	break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Register NetAuthorizationHandler2
                m_nah2Registered = m_oadHN.registerNAH2();
                m_controller.setNetAuthHandlerActive(m_nah2Registered);
                if (m_nah2Registered)
                {
                    m_controller.displayMessage("NAH2 registered");
                }
                else
                {
                    m_controller.displayMessage("NAH2 unregistered");
                }
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Change NAH policy
                m_oadHN.toggleNAHReturnPolicy();
                m_nahReturnPolicy = m_oadHN.getNAHReturnPolicy();
                updateMenuBox();
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Revoke Last Activity
                m_controller.displayMessage("Revoking last activity");
                m_oadHN.revokeActivity();
                break;
            }
            case OCRcEvent.VK_4:
            {
                m_firstNAHMessageOnly = !m_firstNAHMessageOnly;
                m_oadHN.toggleNAHFirstMessageOnlyPolicy();
                updateMenuBox();
                break;
            }
        }
    }
    
    private void updateMenuBox()
    {
        m_menuBox.reset();
        m_menuBox.write("HN Net Authorization Handler Options");
        m_menuBox.write("0. Return to HN Server Menu");
        m_menuBox.write("1. Register NetAuthorizationHandler2");
        m_menuBox.write("2. Change NAH policy (currently returning " + m_nahReturnPolicy + ")");
        m_menuBox.write("3. Revoke Last Activity -1");
        m_menuBox.write("4. Limit NAH message to first activity (currently " 
                    + m_firstNAHMessageOnly + ")");
        m_menuBox.repaint();
        m_page.repaint();
        m_container.repaintScene();
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
