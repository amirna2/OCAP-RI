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
 * A menu to provide VPOP Client options. Since this Page is only accessible
 * from the HN General Menu, which is only accessible if HN extensions are 
 * enabled, it can be assumed that HN extensions are enabled. Future additions
 * to this class may require DVR or HN/DVR functionality. In that case, it will
 * need to be verified that DVR extensions are enabled before making any calls
 * to OcapAppDriverDVR or OcapAppDriverHNDVR.
 *
 */
public class VPOPClientMenuPage extends QuarterPage
{   
    private static VPOPClientMenuPage m_page = new VPOPClientMenuPage();
    
    /**
     * Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverHN from RiExerciserController for HN functionality
     */
    private OcapAppDriverInterfaceHN m_oadHN;
    
    private OcapAppDriverInterfaceCore m_oadCore;
    
    private boolean m_isConnected = false;
    private String m_scid = "0";
    private boolean m_serverSelected = false;
    private int m_serverIndex = -1;
    private int m_playerIndex;
    private String m_mediaServerName = ""; 
    
    private VPOPClientMenuPage()
    {   
        // Initialize components
        m_controller = RiExerciserController.getInstance();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        m_oadCore = OcapAppDriverCore.getOADCoreInterface();
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        
        add(m_menuBox);
        m_menuBox.setVisible(true);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        m_menuBox.write("VPOP Client Menu:");
        m_menuBox.write("0. Return to HN Player Menu");
        m_menuBox.write("1. Stream VPOP");
        m_menuBox.write("2. Invoke Power Status");
        m_menuBox.write("3. Invoke Power Off");
        m_menuBox.write("4. Invoke Power On");
        m_menuBox.write("5. Invoke Audio Mute");
        m_menuBox.write("6. Invoke Audio Restore");
        m_menuBox.write("7. Tune Invocation Menu");
        m_menuBox.write("8. Refresh ConnectionID");
        m_menuBox.write("9. Set Invalid ConnectionID");

        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.repaint();
        this.repaint();
    }
    
    public static VPOPClientMenuPage getInstance()
    {
        return m_page;
    }
    
    public void processUserEvent(KeyEvent event)
    {
        String message = "";
        
        if (!m_serverSelected )
        {
            switch(event.getKeyCode())
            {
                case OCRcEvent.VK_1:
                case OCRcEvent.VK_2:
                case OCRcEvent.VK_3:
                case OCRcEvent.VK_4:
                case OCRcEvent.VK_5:
                case OCRcEvent.VK_6:
                case OCRcEvent.VK_7:
                case OCRcEvent.VK_8:
                {
                    
                    m_oadHN.getNumUPnPMediaServersOnNetwork(); //refresh internal media server list
                    m_serverIndex = HNPlayerMenuPage.getInstance().getMediaServerIndex();
                    if (m_serverIndex < 0) 
                    {
                        m_controller.displayMessage("Media server has not been selected.");
                        return;
                    }
                    m_serverSelected = true;
                    break;
                }
            }
        }
        
        if (!m_isConnected)
        {
            switch(event.getKeyCode())
            {
                case OCRcEvent.VK_3:
                case OCRcEvent.VK_5:
                case OCRcEvent.VK_6:
                case OCRcEvent.VK_7:
                {
                    if (!this.refreshConnectionID()) 
                    {
                        m_controller.displayMessage("Error getting connectionId.");
                        return;
                    }
                }
            }
        }
            
        switch(event.getKeyCode())
        {
        	// Menu Option 0: Return to HN Player Options page
            case OCRcEvent.VK_0:
            {
            	m_controller.displayNewPage(RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE);
            	break;
            }
            // Menu Option 1: Stream VPOP
            case OCRcEvent.VK_1:
            {
            	if (m_serverIndex == m_oadHN.findLocalMediaServer())
                {
                    m_controller.displayMessage("Unable to stream VPOP to self.");
                    return;
                }
                //the server will generate an error if the requested server is 
                // the local server-allow that code path to be exercised by Rx
            	m_oadHN.refreshServerContentItems
            	    (m_serverIndex, RiExerciserConstants.ROOT_CONTAINER_ID, 30000, false, false);
                int vpopIndex = m_oadHN.getVpopContentItemIndex(m_serverIndex);
                if (vpopIndex != -1)
                {
                    m_controller.displayMessage("Found VPOP service for server: " + m_serverIndex);
                    
                    m_controller.displayMessage("Beginning playback");
                    boolean playbackStarted = m_oadHN.playbackStart(OcapAppDriverCore.PLAYBACK_TYPE_SERVICE,
                            m_serverIndex, vpopIndex, 30);
                    if (playbackStarted)
                    {
                        m_controller.setVideoMode(RiExerciserController.REMOTE_PLAYBACK_MODE);
                        if (refreshConnectionID())
                        {
                            m_controller.displayMessage("ConnectionId has been set to '" + m_scid + "'");
                        }
                        else
                        {
                            m_controller.displayMessage("Error getting connectionId.");
                        }
                    }
                    else
                    {
                        m_controller.displayMessage("Error starting VPOP playback");
                    }
                }
                else
                {
                    m_controller.displayMessage("No VPOP Service for server " + m_serverIndex);
                }
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Invoke Power Status
                message = m_oadHN.invokeVpopPowerStatus(m_serverIndex);
                m_controller.displayMessage("invokeVpopPowerStatus(): " + message);
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Invoke Power Off
                message = m_oadHN.invokeVpopPowerOff(m_serverIndex, m_scid);
                m_controller.displayMessage("invokeVpopPowerOff(): " + message);
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Invoke Power On
                message = m_oadHN.invokeVpopPowerOn(m_serverIndex);
                m_controller.displayMessage("invokeVpopPowerOn(): " + message);
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Invoke Audio Mute
                message = m_oadHN.invokeVpopAudioMute(m_serverIndex, m_scid);
                m_controller.displayMessage("invokeVpopAudioMute(): " + message);
                break;
            }
            case OCRcEvent.VK_6:
            {
                // Menu Option 6: Invoke Audio Restore
                message = m_oadHN.invokeVpopAudioRestore(m_serverIndex, m_scid);
                m_controller.displayMessage("invokeVpopAudioRestore(): " + message);
                break;
            }
            case OCRcEvent.VK_7:
            {
                // Menu Option 7: Invoke tuner
                m_controller.displayNewPage(RiExerciserConstants.VPOP_TUNER_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_8:
            {
                // Menu Option 8: Refresh ConnectionID.
                if (!this.refreshConnectionID()) 
                {
                    m_controller.displayMessage("Error getting connectionId.");
                    return;
                }
                m_controller.displayMessage("ConnectionId has been set to '" + m_scid + "'");
                break;
            }
            case OCRcEvent.VK_9:
            {
                // Menu Option 9: Set Invalid ConnectionID.
                m_scid = "invalid_ConnectionID";
                m_controller.displayMessage("ConnectionId has been set to '" + m_scid + "'");
                break;
            }
            case OCRcEvent.VK_STOP:
            {
                m_controller.displayMessage("Stopping vpop stream.");
                HNPlayerMenuPage playerPage = (HNPlayerMenuPage)m_controller.getPage
                        (RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE);
                playerPage.refreshAfterPlayback();
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
    
    private boolean refreshConnectionID()
    {
        if (m_serverSelected == false)
        {
            m_controller.displayMessage("Can not get connectionID when no media server is selected.");
            return m_isConnected = false;
        }
        String[] ids = m_oadHN.invokeCmGetConnectionIds(m_serverIndex);
        for (int x = 0; x < ids.length; x++)
        {
            m_controller.displayMessage(m_mediaServerName + " returned connectionId: " + ids[x]);
        }
        //Expecting ID values of "0" (per DLNA req 7.3.53) and 1 valid ID for the VPOP stream.
        if (ids.length != 2)
        {
            m_controller.displayMessage("Unexpected number of connectionIds: " + ids.length);
            return m_isConnected = false;
        }
        if (!(ids[0].equals("0") || ids[1].equals("0")) ||
                (ids[0].equals("0") && ids[1].equals("0")))
        {
            m_controller.displayMessage("Unexpected connectionId values.");
            return m_isConnected = false;
        }
        if (ids[0].equals("0"))
        {
            m_scid = ids[1];
        }
        else
        {
            m_scid = ids[0];
        }
        return m_isConnected = true;
    }
    
    public String getScid()
    {
        return m_scid;
    }
    
    public int getServerIndex()
    {
        return m_serverIndex;
    }
}
