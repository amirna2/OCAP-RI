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
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.ui.event.OCRcEvent;

public class DvrDeleteMenuPage extends QuarterPage
{
    /**
     * The Singleton instance of this Page
     */
    private static DvrDeleteMenuPage m_page = new DvrDeleteMenuPage();
    
    /**
     * An instance of OcapAppDriverDVR from RiExerciserController for DVR 
     * functionality
     */
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    /**
     * A SelectorList of recordings
     */
    private SelectorList m_recordingList;
    
    /**
     * The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * The Singleton instance of RiExerciserContainer for GUI functionality
     */
    private RiExerciserContainer m_container;
    
    private DvrDeleteMenuPage()
    {
        m_recordingList = new SelectorList();
        m_controller = RiExerciserController.getInstance();
        m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        m_container = RiExerciserContainer.getInstance();
        // Initialize components
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        m_menuBox.write("Delete Menu:");
        m_menuBox.write("0. Return to DVR Menu Page");
        m_menuBox.write("1. Delete Current Recording");
        m_menuBox.write("2. Select a Recording to Delete");
        
        // Initialize the Container layout.
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.setVisible(true);
    }

    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
                // Menu Option 0: Return to DVR Menu Page
                m_controller.displayNewPage(RiExerciserConstants.DVR_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Delete current recording
                int currentRecording = m_oadDVR.getNumRecordings() - 1;
                if (currentRecording >= 0)
                {
                    boolean deleted = m_oadDVR.deleteRecording(currentRecording);
                    if (deleted)
                    {
                        m_controller.displayMessage("Current recording deleted");
                    }
                    else
                    {
                        m_controller.displayMessage("Error deleting current recording");
                    }
                }
                else
                {
                    m_controller.displayMessage("No recording to delete");
                }
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Select a recording to delete
                displayRecordingList();
                break;
            }
        }
        
    }
    
    public static DvrDeleteMenuPage getInstance()
    {
        return m_page;
    }
    
    private void displayRecordingList()
    {
        m_recordingList = new SelectorList();
        final Vector recordings = new Vector();
        int numRecordings = m_oadDVR.getNumRecordings();
        for (int i = 0; i < numRecordings; i++)
        {
            // For now, OcapAppDriver.getRecordingInfo() returns a very long
            // String that is not very useful, so recordings will be identified
            // by their index until the method returns a more concise String
            long recordingDuration = m_oadDVR.getRecordingDuration(i);
            recordings.add("Recording " + (i+1) +  ": " + recordingDuration  + " seconds");
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
                
                /** 
                 * Set m_recordingIndex to the index of the selected item, and then
                 * play the selected recording
                 */
                if (selectedIndex != -1)
                {
                    m_recordingList.setVisible(false);
                    m_recordingList.setEnabled(false);
                    m_container.removeFromScene(m_recordingList);
                    m_container.repaintScene();
                    m_container.requestSceneFocus();
                    boolean deleted = m_oadDVR.deleteRecording(selectedIndex);
                    if (deleted)
                    {
                        m_controller.displayMessage("Recording successfully deleted");
                    }
                    else
                    {
                        m_controller.displayMessage("Error deleting recording");
                    }
                }
                
                // Remove and disable the list after an item has been selected
                m_recordingList.setVisible(false);
                m_recordingList.setEnabled(false);

                m_container.removeFromScene(m_recordingList);

                m_container.repaintScene();
                m_container.requestSceneFocus();
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
