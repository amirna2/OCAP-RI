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
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This Page displays the general HN Menu Options for the Ri Exerciser Xlet. This
 * Page is only accessible if HN extensions are enabled, thus it is not necessary
 * to verify that HN extesnsions are enabled before making calls to OcapAppDriverHN.
 * However, any DVR or HN/DVR functionality will need to first verify that DVR 
 * extensions are enabled (this includes options such as publishing a recording
 * and logging uses of ContentItems). Some of the Pages accessed from this HN
 * General Menu Page include functionality that requires DVR extensions.
 *
 */
public class HNGeneralMenuPage extends QuarterPage
{
    
    private static HNGeneralMenuPage m_page = new HNGeneralMenuPage();
    
    private RiExerciserController m_controller;
    
    private HNGeneralMenuPage()
    {
        // Initialize the components
        setLayout(null);
        m_controller = RiExerciserController.getInstance();
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true);
        add(m_menuBox); 
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
         
        //Write the menu options
        m_menuBox.write("Home Networking General Options");
        m_menuBox.write("0. Return to Ri Exerciser General Menu");
        m_menuBox.write("1. HN Server Options");
        m_menuBox.write("2. HN Player Options");
        m_menuBox.write("3. HN Test Related Options");
        m_menuBox.write("4. HN Diagnostic Options");
        if (m_controller.isDvrEnabled())
        {
            m_menuBox.write("5. HN Encrypted Recording Menu");
        }
    }
    
    public static HNGeneralMenuPage getInstance()
    {
        return m_page;
    }

    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
            	// Menu Option 0: Return to Ri Exerciser General Menu Page
            	m_controller.displayNewPage(RiExerciserConstants.GENERAL_MENU_PAGE);
            	break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: HN Server Options
                m_controller.displayNewPage(RiExerciserConstants.HN_SERVER_OPTIONS_PAGE);
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: HN Player Options
                m_controller.displayNewPage(RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE);
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: HN Test Related Options
                m_controller.displayNewPage(RiExerciserConstants.HN_TEST_PAGE);
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: HN Diagnostic Options
                m_controller.displayNewPage(RiExerciserConstants.HN_DIAGNOSTICS_PAGE);
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: HN Encrypted Recording Menu
                {
                    if (m_controller.isDvrEnabled())
                    {
                        m_controller.displayNewPage(RiExerciserConstants.HN_ENCRYPTED_RECORDING_MENU_PAGE);
                    }
                    break;
                }
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
