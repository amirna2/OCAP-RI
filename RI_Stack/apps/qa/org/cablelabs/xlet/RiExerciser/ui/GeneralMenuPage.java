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
 * This is the General Menu page for the Ri Exerciser Xlet. This is the main
 * gateway to general functionality such as DVR and HN options. The menu options
 * for DVR and HN functionality will only be active if their respective extensions
 * are enabled. If both HN and DVR extensions are disabled, this page will only
 * display a title and will not offer menu options. In general, this is the only
 * page where DVR and HN extensions will be checked, with the exception of some
 * HN Pages that also require DVR extensions (such as publishing a recording). 
 *
 */
public class GeneralMenuPage extends QuarterPage
{
    /**
     * The Singleton instance of this Page
     */
    private static GeneralMenuPage m_page = new GeneralMenuPage();
    
    /**
     * The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;

    private GeneralMenuPage()
    {
        // Add menu with options
        m_controller = RiExerciserController.getInstance();
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options. Only display menus for enabled extensions
        m_menuBox.write("RiExerciser General Menu:");
        if (m_controller.isDvrEnabled())
        {
            m_menuBox.write("1. Display DVR Menu");
        }
        if (m_controller.isHnEnabled())
        {
            m_menuBox.write("2. Display Home Networking Menu");
        }
        
        // Initialize the Container layout.
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.setVisible(true);
    }
    
    public static GeneralMenuPage getInstance()
    {
        return m_page;
    }

    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_1:
            {
                if (m_controller.isDvrEnabled())
                {
                    // Menu Option 1: Display DVR Menu
                    RiExerciserController.getInstance().displayNewPage(RiExerciserConstants.DVR_MENU_PAGE);
                }
                break;
            }
            case OCRcEvent.VK_2:
            {
                if (m_controller.isHnEnabled())
                {
                    // Menu Option 2: Display Home Networking Menu
                    RiExerciserController.getInstance().displayNewPage(RiExerciserConstants.HN_GENERAL_MENU_PAGE);
                }
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
