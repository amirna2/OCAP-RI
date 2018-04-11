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
import java.util.Vector;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverHNDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This class provides functionality that is used for DLNA CTT Tests. Some of the
 * functionality requires DVR extensions to be enabled. Any modifications to
 * this class should always verify that DVR extensions are enabled before making
 * calls to OcapAppDriverDVR.
 *
 */
public class HNDlnaCttTestPage extends QuarterPage
{
    /**
     * Singleton instance of this Page
     */
    private static HNDlnaCttTestPage m_page = new HNDlnaCttTestPage();
    
    /**
     * The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverHN from RiExerciserController for HN functionality
     */
    private OcapAppDriverInterfaceHN m_oadHN;
    
    /**
     * OcapAppDriverDVR from RiExerciserController for DVR functionality
     */
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    /**
     * A boolean indicating whether a publish completed or not
     */
    private boolean m_publishCompleted;
    
    /**
     * A SelectorList of recordings to publish
     */
    private SelectorList m_recordingList;
    
    /**
     * The index of the recording to be published
     */
    private int m_recordingIndex;
    
    /**
     * Timeout in milliseconds to wait for HN action to complete
     */
    private final long m_hnActionTimeoutMS;
    
    private boolean m_cdsFound;
    
    RiExerciserContainer m_container;
    
    private HNDlnaCttTestPage()
    {
        // Initialize the components
        setLayout(null);
        m_cdsFound = false;
        m_controller = RiExerciserController.getInstance();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        if (m_controller.isDvrEnabled())
        {
            m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        }
        m_container = RiExerciserContainer.getInstance();
        
        m_hnActionTimeoutMS = m_controller.getHNActionTimeoutMS();

        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true); 
        add(m_menuBox);
        
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options. Only write options for which extensions 
        // are enabled
        m_menuBox.write("HN DLNA CTT Test Options");
        m_menuBox.write("0. Return to HN Specific Test Menu");
        if (m_controller.isDvrEnabled())
        {
            m_menuBox.write("1. Publish current recording in CDS");
            m_menuBox.write("2. Display list of recordings to publish in CDS");
        }
        m_menuBox.write("3. Change friendly name");
        m_menuBox.write("4. Send Root Device ByeBye Messages");
        m_menuBox.write("5. Send root device Alive message");
    }
    
    public static HNDlnaCttTestPage getInstance()
    {
        return m_page;
    }
    
    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            // Menu Option 0: Return to HN specific Test menu
            case OCRcEvent.VK_0:
            {
            	m_controller.displayNewPage(RiExerciserConstants.HN_TEST_PAGE);
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
                        publishRecording(numRecordings - 1, m_hnActionTimeoutMS);
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
                    // Menu Option 2: Display list of recordings to publish
                    displayRecordingList();
                }
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Change friendly name
                changeDeviceFriendlyNames();
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Send root device ByeBye messages                
                boolean byeByeSuccess = m_oadHN.sendRootDeviceByeBye();
                if (byeByeSuccess)
                {
                  m_controller.displayMessage("ByeBye messages sent");  
                }
                else
                {
                    m_controller.displayMessage("Error sending ByeBye messages");
                }
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Send root device alive message
                boolean aliveSuccess = m_oadHN.sendRootDeviceAlive();
                if (aliveSuccess)
                {
                  m_controller.displayMessage("Alive message sent");  
                }
                else
                {
                    m_controller.displayMessage("Error sending alive message");
                }
                break;
                
            }
        }
    }
    
    /**
     * Changes the friendly name of the local media server first then the name of the root device.
     */
    private void changeDeviceFriendlyNames()
    {
        // Change the local media server's name first
        String currentLocalName = m_oadHN.getHnLocalMediaServerFriendlyName();
        m_controller.displayMessage("Local media server's friendly name is currently: " + currentLocalName);
        boolean localNameChanged = m_oadHN.hnChangeLocalMediaServerFriendlyName(currentLocalName + " changed");
        if (localNameChanged)
        {
            String newLocalName = m_oadHN.getHnLocalMediaServerFriendlyName();
            m_controller.displayMessage("Local media server's friendly name has been changed to: " + newLocalName);

            // If local change was successful, change the root device's name
            String currentRootName = m_oadHN.getRootDeviceFriendlyName();
            boolean rootNameChanged = m_oadHN.changeRootDeviceFriendlyName(currentRootName + " changed");
            if (rootNameChanged)
            {
                String newRootName = m_oadHN.getRootDeviceFriendlyName();
                m_controller.displayMessage("OCAP Device friendly name has been changed to: " + newRootName);
            }
        }
        else
        {
            m_controller.displayMessage("Error changing local media server's friendly name");
        }
    }

    private void publishRecording(final int recordingIndex, final long timeoutMS)
    {
        new Thread()
        {
            public void run()
            {
                // Try to find the local media server
                if (!m_cdsFound)
                {
                    m_cdsFound = m_oadHN.waitForLocalContentServerNetModule(
                                            m_controller.getLocalServerTimeoutSecs());
                }
                int cdsIndex = m_oadHN.findLocalMediaServer();
                if (cdsIndex == -1)
                {
                    m_controller.displayMessage("Error finding media server.");
                }
                else
                {
                    // Publish the recording and log whether the publish was successful or not
                    m_controller.displayMessage("Publishing current recording.");
                    m_publishCompleted = OcapAppDriverHNDVR.getOADHNDVRInterface().publishRecording(
                            recordingIndex, timeoutMS, false);
                    postPublishStatusMessage();
                }
            }
        }.start();
    }
    
    private boolean recordingsToPublish()
    {
        int recordings = m_oadDVR.getNumRecordings();
        return recordings > 0;
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
    
    private void displayRecordingList()
    {
        m_recordingList = new SelectorList();
        
        // Initialize the list of recordings
        int numRecordings = m_oadDVR.getNumRecordings();
        final Vector recordings = new Vector();
        for (int i = 0; i < numRecordings; i++)
        {
            String recording = "Recording " + (i+1);
            long duration = m_oadDVR.getRecordingDuration(i);
            recordings.add(recording + ": " + duration + " seconds");
        }
        m_recordingList.initialize(recordings);
        m_recordingList.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_recordingList.getSelectedItemIndex();
                
                // Set m_recordingIndex to the index of the selected item
                if (selectedIndex != -1)
                {
                    m_recordingIndex = selectedIndex;
                    m_controller.displayMessage("Recording selected: " + recordings.get(m_recordingIndex));
                    m_controller.displayMessage("Selected recording index " + m_recordingIndex);
                    m_controller.displayMessage("Publishing recording " + m_recordingIndex);
                    publishRecording(m_recordingIndex, m_hnActionTimeoutMS);
                }
                
                // Once an item has been selected, remove and disable the list
                m_recordingList.setVisible(false);
                m_recordingList.setEnabled(false);

                m_container.removeFromScene(m_recordingList);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initialize the SelectorList
        m_recordingList.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(m_recordingList);
        m_recordingList.setVisible(true);
        m_container.scenePopToFront(m_recordingList);
        m_recordingList.setEnabled(true);
        m_recordingList.setFocusable(true);
        m_recordingList.requestFocus();
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
