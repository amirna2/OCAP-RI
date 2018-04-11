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
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverInterfaceHNDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.ui.event.OCRcEvent;


/**
 * 
 * @author Nicolas Metts
 * This page is for publishing recordings to the CDS, with or without associated
 * NetRecordingEntry objects. Since publishing a recording requires DVR extensions
 * and HN extensions enabled, this page will not be available unless DVR extensions
 * are enabled. HN extensions are assumed to be enabled since the previous page is 
 * only available if HN extensions are enabled.
 *
 */
public class HNPublishRecordingMenuPage extends QuarterPage
{
    /**
     * The Singleton instance of this Page
     */
    private static HNPublishRecordingMenuPage m_page = new HNPublishRecordingMenuPage();
    
    /**
     *  The Singleton instance of RiExerciserController
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
     * OcapAppDriverHNDVR from RiExerciserController for DVR and HN cross 
     * functionality
     */
    private OcapAppDriverInterfaceHNDVR m_oadHNDVR;
    
    /**
     * A boolean indicating whether publishing completed
     */
    private boolean m_publishCompleted;
    
    /**
     * A SelectorList to display recordings
     */
    private SelectorList m_recordingList;

    /**
     * Timeout in milliseconds to wait for HN action to complete
     */
    private final long m_hnActionTimeoutMS;

    private RiExerciserContainer m_container;
    
    private boolean m_cdsFound;
    
    private int m_recordingIndex;
    
    private HNPublishRecordingMenuPage()
    {
        // Initialize the components
        setLayout(null);
        m_publishCompleted = false;
        m_cdsFound = false;
        m_controller = RiExerciserController.getInstance();
        m_container = RiExerciserContainer.getInstance();
        m_oadHN  = OcapAppDriverHN.getOADHNInterface();
        m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        m_oadHNDVR = OcapAppDriverHNDVR.getOADHNDVRInterface();
        m_recordingList = new SelectorList();
                
        m_hnActionTimeoutMS = m_controller.getHNActionTimeoutMS();

        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true); 
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options.
        m_menuBox.write("HN Publish Recording Options");
        m_menuBox.write("0. Return to HN Server Menu");
        m_menuBox.write("1. Publish current recording to CDS");
        m_menuBox.write("2. Display list of recordings to publish to CDS");
        m_menuBox.write("3. Publish all recordings to CDS");
        m_menuBox.write("4. Unpublish all recordings");
    }
    
    public static HNPublishRecordingMenuPage getInstance()
    {
        return m_page;
    }
    
    public void processUserEvent(KeyEvent event)
    {
        switch (event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
                // Menu Option 0: Return to HN Server Menu
                m_controller.displayNewPage(RiExerciserConstants.HN_SERVER_OPTIONS_PAGE);
                break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Publish current recording in CDS
                if (recordingsToPublish())
                {
                    int numRecordings = m_oadDVR.getNumRecordings();
                    if(!m_oadHNDVR.isRemoteScheduledRecording(numRecordings - 1))
                    {
                        m_controller.displayMessage("Publishing current recording");
                        publishRecording(numRecordings - 1);                       
                    }
                    else
                    {
                        //TODO: Log message if remote recording
                    }
                }
                else
                {
                    m_controller.displayMessage("No recordings to publish");
                }
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Display list of recordings to publish to CDS
                displayRecordingList();
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Publish all recordings to CDS
                if (recordingsToPublish())
                {
                    m_controller.displayMessage("Publishing all recordings");
                    publishAllRecordings();
                }
                else
                {
                    m_controller.displayMessage("No recordings to publish");
                }
                break;
            }
            // Menu Option 4: Unpublish all recordings from CDS
            case OCRcEvent.VK_4:
            {
                m_controller.displayMessage("Unpublishing all recordings from CDS");
                if (m_oadHNDVR.unPublishAllRecordings())
                {
                    m_controller.displayMessage("All recordings successfully unpublished from CDS");
                }
                else
                {
                    m_controller.displayMessage("Error unpublishing recordings from CDS");
                }
                break;
            }
            case OCRcEvent.VK_ENTER:
            {
                // This prevents the display of HomeNetworkingGeneralMenuPage when
                // an item is selected from a list
                m_recordingList.setVisible(false);
                m_recordingList.setEnabled(false);
                m_recordingList.setFocusable(false);
                m_container.removeFromScene(m_recordingList);
                
                m_container.requestSceneFocus();
                m_container.popToFront(m_page);
                m_page.repaint();;
                m_container.repaintScene();
                
                break;
            }
        }
    }
    
    public boolean publishSuccessful()
    {
        return m_publishCompleted;
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
                if (!m_cdsFound)
                {
                    m_cdsFound = waitForMediaServer();
                }
                int cdsIndex = m_oadHN.findLocalMediaServer();
                if (cdsIndex == -1)
                {
                    m_controller.displayMessage("Error finding media servers.");
                }
                else
                {
                    // Publish the recording and log whether the publish was successful or not
                    m_publishCompleted = m_oadHNDVR.publishRecording(recordingIndex, m_hnActionTimeoutMS, false);
                    postPublishStatusMessage();
                }
            }
        }.start();
    }
    
    private void publishAllRecordings()
    {
        new Thread()
        {
            public void run()
            {
                
                if (!m_cdsFound)
                {
                    m_cdsFound = waitForMediaServer();
                }
                int cdsIndex = m_oadHN.findLocalMediaServer();
                if (cdsIndex == -1)
                {
                    m_controller.displayMessage("Error finding media servers.");
                }
                else
                {
                    m_publishCompleted = m_oadHNDVR.publishAllRecordings(m_hnActionTimeoutMS, false);
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
            int numPublishedItems = m_oadHN.getNumPublishedContentItems();
            for (int i = 0; i < numPublishedItems; i++)
            {
                String publishedContent = m_oadHN.getPublishedContentString();
                m_controller.displayMessage("Published " + publishedContent);
            }
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
            // Only display local recordings
            if(!m_oadHNDVR.isRemoteScheduledRecording(i))
            {
                recordings.add(recording + ": " + duration + " seconds");   
            }
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
                    publishRecording(m_recordingIndex);
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
    
    private boolean waitForMediaServer()
    {
        return m_oadHN.waitForLocalContentServerNetModule(
                        m_controller.getLocalServerTimeoutSecs());
    }
    
    public void destroy()
    {
        
    }

    public void init()
    {

    }

}
