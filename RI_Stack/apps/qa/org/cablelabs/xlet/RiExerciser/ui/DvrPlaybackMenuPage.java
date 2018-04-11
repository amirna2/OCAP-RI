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
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This page displays DVR Playback Options
 *
 */
public class DvrPlaybackMenuPage extends QuarterPage
{
    private static DvrPlaybackMenuPage m_page = new DvrPlaybackMenuPage();
    
    
    /**
     * The instance of the RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverDVR from RiExerciserController for DVR functionality
     */
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    /**
     * The list of DVR Recordings
     */
    private SelectorList m_recordingList;
    
    /**
     * The index of the selected DVR recording to playback
     */
    private int m_recordingIndex;
    
    /**
     * Multiplier for converting seconds to nanoseconds
     */
    private static final long SECS_TO_NANOSECS = 1000000000;
    
    /**
     * An instance of RiExerciserContainer
     */
    private RiExerciserContainer m_container;
    
    private DvrPlaybackMenuPage()
    {
        m_recordingIndex = -1;
        
        // Initialize components
        m_controller = RiExerciserController.getInstance();
        m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        m_container = RiExerciserContainer.getInstance();
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        
        add(m_menuBox);
        m_menuBox.setVisible(true);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        m_menuBox.write("Playback Menu:");
        m_menuBox.write("0. Return to DVR Specific Menu");
        m_menuBox.write("1. Playback Current Recording");
        m_menuBox.write("2. Select Recording for Playback");
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.repaint();
        this.repaint();
    }
    
    public static DvrPlaybackMenuPage getInstance()
    {
        return m_page;
    }
    
    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
            	// Menu Option 0: Return to DVR Specific Menu
            	m_controller.displayNewPage(RiExerciserConstants.DVR_MENU_PAGE);
            	break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Playback current Recording
                final int currentRecording = m_oadDVR.getNumRecordings() - 1;
                if (currentRecording < 0)
                {
                    m_controller.displayMessage("No recordings to playback");
                }
                else
                {
                    playDvrRecording(currentRecording);
                }

                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Select Recording for playback
                displayRecordingList();
                break;
            }
        }
    }

    public void destroy()
    {
        m_menuBox.setVisible(false);
    }
    
    private void playDvrRecording(int recordingIndex)
    {
        OcapAppDriverCore.getOADCoreInterface().playbackTransformVideo(.5f, .5f, 0, 0);

        // Convert seconds to nanoseconds
        long duration = m_oadDVR.getRecordingDuration(recordingIndex);
        duration *= SECS_TO_NANOSECS;

        m_controller.displayMessage("DVR playback starting");
        m_controller.setVideoMode(RiExerciserController.DVR_PLAYBACK_MODE);
        m_container.indicatePlayback(OcapAppDriverInterfaceCore.PLAYBACK_TYPE_SERVICE, duration);
        if (!m_oadDVR.playbackStart(recordingIndex, 15))
        {
            m_controller.displayMessage("Error creating DVR playback");
            m_controller.displayMessage("Failed to start DVR playback");
            m_controller.tuneToCurrentIndex();
        }
        else
        {
            m_controller.displayMessage("DVR playback started");            
        }
    }
    
    private void displayRecordingList()
    {
        m_recordingList = new SelectorList();
        final Vector recordings = new Vector();
        int numRecordings = m_oadDVR.getNumRecordings();
        for (int i = 0; i < numRecordings; i++)
        {
            // For now, OcapAppDriverDVR.getRecordingInfo() returns a very long
            // String that is not very useful, so recordings will be identified
            // by their index until the method returns a more concise String
            //String recordingInfo = m_controller.getAppDriver().getRecordingInfo(i);
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
                    m_recordingIndex = m_recordingList.getSelectedItemIndex();
                    m_controller.displayMessage("Recording selected: " + recordings.get(m_recordingIndex));
                    m_recordingList.setVisible(false);
                    m_recordingList.setEnabled(false);
                    m_recordingList.setFocusable(false);
                    m_container.removeFromScene(m_recordingList);
                    m_menuBox.repaint();
                    m_container.repaintScene();
                    new Thread()
                    {
                        public void run()
                        {
                            playDvrRecording(m_recordingIndex);
                        }
                    }.start();
                }
                
                // Remove and disable the list after an item has been selected
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

    public void init()
    {
        m_menuBox.setVisible(true);
    }
}
