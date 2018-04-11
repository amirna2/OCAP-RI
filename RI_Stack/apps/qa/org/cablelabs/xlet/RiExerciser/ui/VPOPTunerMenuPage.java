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
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This Page provides VPOP tuner options. Since this Page is only accessible
 * from the HN General Menu, which is only accessible if HN extensions are 
 * enabled, it can be assumed that HN extensions are enabled. Future additions
 * to this class may require DVR or HN/DVR functionality. In that case, it will
 * need to be verified that DVR extensions are enabled before making any calls
 * to OcapAppDriverDVR or OcapAppDriverHNDVR.
 *
 */
public class VPOPTunerMenuPage extends QuarterPage
{
    private static VPOPTunerMenuPage s_page = new VPOPTunerMenuPage();
    
    /**
     * The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverHN from RiExerciserController for HN functionality
     */
    private OcapAppDriverInterfaceHN m_oadHN;
    
    private int m_majorChannel = 114;
    
    private int m_minorChannel = 0;
    
    private VPOPTunerMenuPage()
    {
        // Initialize components
        m_controller = RiExerciserController.getInstance();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        
        add(m_menuBox);
        m_menuBox.setVisible(true);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.repaint();
        this.repaint();
    }
    
    public static VPOPTunerMenuPage getInstance()
    {
        return s_page;
    }
    
    private void writeMenuOptions()
    {
        m_menuBox.reset();
        m_menuBox.write("Tune Invocation Menu:");
        m_menuBox.write("0 Return to VPOP Client Menu");
        m_menuBox.write("1 VPOP Tune Server to Clock");
        m_menuBox.write("2 VPOP Tune Server to Clouds");
        m_menuBox.write("3 VPOP Tune Server to Golfer");     
        m_menuBox.write("4 Tune ( " + m_majorChannel + " , " + m_minorChannel + " )");
        m_menuBox.write(" ");
        m_menuBox.write("Up/Down to set Major channel: " + m_majorChannel);
        m_menuBox.write("Left/Right to set Minor channel: " + m_minorChannel);
       
        m_controller.displayMessage("*** Reminder 1. Navigate to VPOP Server page on the VPOP server.");
        m_controller.displayMessage("*** Reminder 2. VPOP server uses canned streams and default config.properties");
    }

    public void processUserEvent(KeyEvent event)
    {       
        // Menu Option 0: Return to VPOP Client Menu
        if ( event.getKeyCode() == OCRcEvent.VK_0 )
        {
            m_controller.displayNewPage(RiExerciserConstants.VPOP_CLIENT_MENU_PAGE);
            return;
        }
        
        //Get the connectionID
        int serverIndex = VPOPClientMenuPage.getInstance().getServerIndex();
        String[] ids = m_oadHN.invokeCmGetConnectionIds(serverIndex);
        for (int x = 0; x < ids.length; x++)
        {
            m_controller.displayMessage("invokeCmGetConnectionIds returned connectionId: " + ids[x]);
        }
        if (ids.length != 2)
        {
            m_controller.displayMessage("Unexpected number of connectionIds: " + ids.length);
            return;
        }
        
        /* Keys 1-3 demonstrate the client requesting a channel change (on the VPOP server).  
         * Here is the scenario.
         * On another host the RI is running and plays the role of the client.
         * The client is running RiExerciser and has navigated to this page.
         * Pressing Keys 1, 2 or 3 on the client page invokes the VPOP tune service and
         *    sends the tune number 1, 2 or 3 to the RI Server.  As is posted in the
         *    reminders on this page, the server MUST be running RiExerciser,
         *    MUST be playing the canned streams with the default config.properties,
         *    and MUST be navigated to the VPOP Server Menu page.  
         * When the server receives the number 1, 2 or 3 as a key event in this method, 
         *    it will force tune to one of the channels.
         * Hence, pressing 1,2 or 3 on the client will cause the VPOP server to tune
         *    to one of the canned streams.  
         * If all is working well the client will also soon tune to the same channel.
        */

        switch (event.getKeyCode())
        {
            case OCRcEvent.VK_1:
            {
            	m_controller.displayMessage("Client requesting tune to Clock channel");
                String message = m_oadHN.invokeVpopTune(serverIndex, ids[1], String.valueOf(1) );
                m_controller.displayMessage(message);
                break;
            }
            case OCRcEvent.VK_2:
            {
                m_controller.displayMessage("Client requesting tune to Clouds channel");
                String message = m_oadHN.invokeVpopTune(serverIndex, ids[1], String.valueOf(2) );
                m_controller.displayMessage(message);
                break;
            }
            case OCRcEvent.VK_3:
            {
                m_controller.displayMessage("Client requesting tune to Golfer channel");
                String message = m_oadHN.invokeVpopTune(serverIndex, ids[1], String.valueOf(3) );
                m_controller.displayMessage(message);
               break;
            } 
            /* Key 4 allows the tester to enter an arbitrary major and minor channel number
             * from the client.  This is handy when testing apps where the video comes from
             * the cable plant and not from canned streams.  When pressed, the current major 
             * and minor channel numbers are sent via a Tune invocation to the client.
             * 
             * If all is working well the client will also soon tune to the same channel.
             */
            
            case OCRcEvent.VK_4:
            {
                String arbitraryChannel = m_majorChannel + "," + m_minorChannel;
            	m_controller.displayMessage("Client requesting tune to channel " + arbitraryChannel);
                String message = m_oadHN.invokeVpopTune(serverIndex, ids[1], arbitraryChannel);
                m_controller.displayMessage(message);
               break;
            } 
            /*
             * The left/right and up/down keys allow the tester to increment or decrement
             * the major and minor channel values.  Most of the interesting channels used for
             * testing on our cable plant begin at 114,0.  Hence we start there.  As the values
             * are changed the current values are displayed on the menu.  No channels are changed
             * until Key 4 is selected.
             */
            case OCRcEvent.VK_UP:
            {
            	m_majorChannel++;
            	writeMenuOptions();
            	break;
            }
            case OCRcEvent.VK_DOWN:
            {
            	m_majorChannel--;
            	if ( m_majorChannel < 1 )
            	{
            	    m_controller.displayMessage("Major channel number cannot be less than 1");
            	    m_majorChannel = 1;
            	}
            	writeMenuOptions();
            	break;            	
            }
            case OCRcEvent.VK_LEFT:
            {
               	m_minorChannel--;
            	if ( m_minorChannel < 1 )
            	{
            	    m_controller.displayMessage("Minor channel number cannot be less than 1");
            	    m_minorChannel = 1;
            	}
            	writeMenuOptions();
            	break;            	

            }
            case OCRcEvent.VK_RIGHT:
            {
            	m_minorChannel++;
            	writeMenuOptions();
                break;
            }
        }        
    }

    public void destroy()
    {
        // TODO Auto-generated method stub

    }

    public void init()
    {
        writeMenuOptions();
    }

}
