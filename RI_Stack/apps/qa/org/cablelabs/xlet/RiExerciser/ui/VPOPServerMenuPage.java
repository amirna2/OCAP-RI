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
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * A menu to provide VPOP Server options. Since this Page is only accessible
 * from the HN Server Menu, which is only accessible if HN extensions are
 * enabled, it can be assumed that HN extensions are enabled. Future additions
 * to this class may require DVR or HN/DVR functionality. In that case, it will
 * need to be verified that DVR extensions are enabled before making any calls
 * to OcapAppDriverDVR or OcapAppDriverHNDVR.
 *
 */
public class VPOPServerMenuPage extends QuarterPage
{   
    private static VPOPServerMenuPage m_page = new VPOPServerMenuPage();
    
    /**
     * Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    private OcapAppDriverInterfaceCore m_oadIfCore;
    private int m_clockServiceIndex = 7;
    private int m_cloudsServiceIndex  = 3;
    private int m_golferServiceIndex = 1;

    private VPOPServerMenuPage()
    {   
        // Initialize components
        m_oadIfCore = OcapAppDriverCore.getOADCoreInterface();
        m_controller = RiExerciserController.getInstance();
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        
        add(m_menuBox);
        m_menuBox.setVisible(true);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        m_menuBox.write("VPOP Server Menu:");
        m_menuBox.write("0. Return to HN Server Menu");
        m_menuBox.write("  ");
        m_menuBox.write("   Use the VPOP Client Menu Page on another host");
        m_menuBox.write("   to control the tuning on this VPOP server");
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.repaint();
        this.repaint();
    }
    
    public static VPOPServerMenuPage getInstance()
    {
        return m_page;
    }
    
    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
        	// Menu Option 0: Return to HN Player Options page
            case OCRcEvent.VK_0:
            {
            	m_controller.displayNewPage(RiExerciserConstants.HN_SERVER_OPTIONS_PAGE);
            	break;
            }
            // Menu Option 1: Change to Clock channel
            case OCRcEvent.VK_1:
            {
            	m_controller.displayMessage(" ... Tuning to Clock channel");
                m_oadIfCore.serviceSelectByIndex(m_clockServiceIndex);
            	break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Change to Clouds channel
            	m_controller.displayMessage(" ... Tuning to Clouds channel");
                m_oadIfCore.serviceSelectByIndex(m_cloudsServiceIndex);
            	break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Change to Golfer channel
            	m_controller.displayMessage(" ... Tuning to Golfer channel");
                m_oadIfCore.serviceSelectByIndex(m_golferServiceIndex);
                break;
            }
            default:
                m_controller.displayMessage(" ...  Arbitrary Channel requested: ");
                break;
        }
    }

    public void destroy()
    {   
    }

    public void init()
    {        
    }
}