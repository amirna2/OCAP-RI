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
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverHNDVR;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverInterfaceHNDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This Page provides HN Test Options. Some of the functionality in this class,
 * such as publishing a recording to the CDS, requires DVR extensions to be 
 * enabled. Thus, it should be verified that DVR extensions are enabled before 
 * making any calls to OcapAppDriverDVR or OcapAppDriverHNDVR.
 *
 *
 */
public class HNTestPage extends QuarterPage
{
    private static HNTestPage m_page = new HNTestPage();
    
    /**
     * The instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverHN from RiExerciserController for HN functionality
     */
    private OcapAppDriverInterfaceHN m_oadHN;
    
    /**
     * OcapAppDriverDVr from RiExerciserController for DVR functionality
     */
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    /**
     * OcapAppDriverHNDVR from RiExerciserController for HN and DVR cross 
     * functionality
     */
    private OcapAppDriverInterfaceHNDVR m_oadHNDVR;
    
    /**
     * A boolean indicating whether a publish completed or not
     */
    private boolean m_publishCompleted;
    
    /**
     * Timeout in milliseconds to wait for HN action to complete
     */
    private final long m_hnActionTimeoutMS;

    private boolean m_cdsFound;
    
    private HNTestPage()
    {
        // Initialize the components
        setLayout(null);
        m_controller = RiExerciserController.getInstance();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        if (m_controller.isDvrEnabled())
        {
            m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
            m_oadHNDVR = OcapAppDriverHNDVR.getOADHNDVRInterface();
        }
        m_cdsFound = false;
        
        m_hnActionTimeoutMS = m_controller.getHNActionTimeoutMS();

        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true); 
        add(m_menuBox);
        
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options. Only write options for which extensions are enabled
        m_menuBox.write("HN Specific Test Options");
        m_menuBox.write("0. Return to HN General Menu");
        if (m_controller.isDvrEnabled())
        {
            m_menuBox.write("1. Publish current recording in CDS");
            m_menuBox.write("2. Log in use count of current recording");
            m_menuBox.write("3. Log in use count of current CDS recording");
        }
        m_menuBox.write("4. DLNA CTT Options");
        m_menuBox.write("5. Publish current channel in CDS- alternate res");
        m_menuBox.write("6. Remote UI Server Manager Options");
        m_menuBox.write("7. View Content Transformation Menu");
    }
    
    public static HNTestPage getInstance()
    {
        return m_page;
    }

    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
            	// Menu Option 0: Return to HN General Menu Page
            	m_controller.displayNewPage(RiExerciserConstants.HN_GENERAL_MENU_PAGE);
            	break;
            }
            case OCRcEvent.VK_1:
            {
                if (m_controller.isDvrEnabled())
                {
                    // Menu Option 1: Publish current recording in CDS
                    if (recordingsToPublish())
                    {
                        int numRecordings = m_oadDVR.getNumRecordings();
                        m_controller.displayMessage("Publishing current recording");
                        publishRecording(numRecordings - 1);
                    }
                    else
                    {
                        m_controller.displayMessage("No recordings to publish");
                    }
                }
                break;
            }
            case OCRcEvent.VK_2:
            {
                if (m_controller.isDvrEnabled())
                {
                    // Menu Option 2: Log in use count of current recording
                    m_oadHNDVR.logRecordingInUse();
                    int messages = m_oadHNDVR.getNumRecordingLogUses();
                    for (int i = 0; i < messages; i++)
                    {
                        String message = m_oadHNDVR.getRecordingLogMessage(0);
                        m_controller.displayMessage(message);
                    }
                }
                break;
            }
            case OCRcEvent.VK_3:
            {
                if (m_controller.isDvrEnabled())
                {
                    // Menu Option 3: Log in use count of current CDS recording
                    m_oadHNDVR.logRecordingInUseViaCDS(m_hnActionTimeoutMS);
                    int messages = m_oadHNDVR.getNumCDSRecordingLogUses();
                    for (int i = 0; i < messages; i++)
                    {   
                        String message = m_oadHNDVR.getCDSRecordingLogMessage(0);
                        m_controller.displayMessage(message);
                    }
                }
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: DLNA CTT Options
                m_controller.displayNewPage(RiExerciserConstants.HN_DLNA_CTT_TEST_PAGE);
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Publish current channel in CDS- alternate res
                int currentService = m_controller.getCurrentService();
                if (!m_cdsFound)
                {
                    m_cdsFound = m_oadHN.waitForLocalContentServerNetModule(
                                    m_controller.getLocalServerTimeoutSecs());
                }
                boolean channelPublished = m_oadHN.publishServiceUsingAltRes(currentService, m_hnActionTimeoutMS);
                if (channelPublished)
                {
                    m_controller.displayMessage("Channel successfully published");
                }
                else
                {
                    m_controller.displayMessage("Error publishing channel");
                }
                break;
            }
            case OCRcEvent.VK_6:
            {
                m_controller.displayNewPage(RiExerciserConstants.REMOTE_UI_SERVER_MANAGER_PAGE);
                break;
            }
            
            case OCRcEvent.VK_7:
            {
            	m_controller.displayNewPage(RiExerciserConstants.HN_CONTENT_TRANSFORMATION_MENU_PAGE);
            	break;
            }
        }
    }
    
    private boolean recordingsToPublish()
    {
        int recordings = m_oadDVR.getNumRecordings();
        return recordings > 0;
    }
    
    private void publishRecording(final int recordingIndex)
    {
        new Thread()
        {
            public void run()
            {
                // Log the current number of recordings
                if (!m_cdsFound)
                {
                    m_cdsFound = m_oadHN.waitForLocalContentServerNetModule(
                                    m_controller.getLocalServerTimeoutSecs());
                }
                int cdsIndex = m_oadHN.findLocalMediaServer();
                if (cdsIndex == -1)
                {
                    m_controller.displayMessage("Error finding media servers.");
                }
                else
                {
                    // Publish the recording and log whether the publish was successful or not
                    m_controller.displayMessage("Publishing current recording.");
                    m_publishCompleted = m_oadHNDVR.publishRecording(recordingIndex, m_hnActionTimeoutMS, false);
                    postPublishStatusMessage();
                }
            }
        }.start();
    }
    
    // This method logs whether publishing was successful or not
    private void postPublishStatusMessage()
    {
        if (m_publishCompleted)
        {
            m_controller.displayMessage("Publish completed.");
        }
        else
        {
            m_controller.displayMessage("Publish failed.");
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
