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
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.ui.event.OCRcEvent;


/**
 * 
 * @author Nicolas Metts
 * This page displays DVR options for the non-selected channel, specifically the
 * channel that can be accessed from pressing Channel Up on the remote.
 * Since this Page is only accessible from the DVR General Menu, which is only
 * accessible if DVR extensions are enabled, it can be assumed that DVR 
 * extensions are enabled. For future additions, any HN functionality
 * that might be added would need to follow a verification that HN extensions
 * are enabled. 
 *
 */
public class DvrNonSelectedChannelMenuPage extends QuarterPage
{
    private static DvrNonSelectedChannelMenuPage m_Page = new DvrNonSelectedChannelMenuPage();
    
    private boolean m_bufferingRequested;
    
    private RiExerciserController m_controller;
    
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    private OcapAppDriverInterfaceCore m_oadCore;
    
    private long m_startTime;

    private long m_stopTime;

    private long m_recordingCompleteTime;
    
    private boolean m_currentlyRecording;
    
    private boolean m_completed;
    
    private int m_nextServiceIndex;
    
    private DvrNonSelectedChannelMenuPage()
    {
        m_bufferingRequested = false;
        m_controller = RiExerciserController.getInstance();
        m_oadCore = OcapAppDriverCore.getOADCoreInterface();
        m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        // Initialize components
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        m_menuBox.write("DVR Non-Selected Channel Menu:");
        m_menuBox.write("The following options are performed on the next channel");
        m_menuBox.write("0. Return to DVR specific Menu");
        m_menuBox.write("1. Start a 10-second background recording");
        m_menuBox.write("2. Start a 30-second background recording");
        m_menuBox.write("3. Start a 5-min background recording");
        m_menuBox.write("4. Enable/Disable Buffering");
        m_menuBox.write("5. Request or Cancel Buffering");
        
        // Initialize the Container layout.
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.setVisible(true);
    }
    
    public static DvrNonSelectedChannelMenuPage getInstance()
    {
        return m_Page;
    }
    
    public void processUserEvent(KeyEvent event)
    {
        switch (event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
            	// Menu Option 0: Return to DVR Specific Menu
            	m_controller.displayNewPage(RiExerciserConstants.DVR_MENU_PAGE);
            	break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Start a 10-second background recording
                m_nextServiceIndex = getNextServiceIndex();
                m_controller.displayMessage("Next service index: " + m_nextServiceIndex);
                m_controller.displayMessage("Beginning 10-second background recording");
                record(m_nextServiceIndex, 10, 0);
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Start a 30-second background recording
                m_nextServiceIndex = getNextServiceIndex();
                m_controller.displayMessage("Beginning 30-second background recording");
                record(m_nextServiceIndex, 30, 0);
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Start a 5-min background recording
                m_nextServiceIndex = getNextServiceIndex();
                m_controller.displayMessage("Beginning 5-minute background recording");
                record(m_nextServiceIndex, 300, 0);
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Enable/Disable Buffering
                m_oadDVR.toggleBufferingEnabled();
                if (m_oadDVR.isBufferingEnabled())
                {
                    m_controller.displayMessage("Buffering enabled");
                }
                else
                {
                    m_controller.displayMessage("Buffering disabled");
                }
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Request or Cancel Buffering
                m_bufferingRequested = !m_bufferingRequested;
                if (m_bufferingRequested)
                {
                    m_controller.displayMessage("Buffering requested");
                }
                else
                {
                    m_controller.displayMessage("Buffering request cancelled");
                }
                m_nextServiceIndex = getNextServiceIndex();
                m_oadDVR.toggleBufferingRequest(m_nextServiceIndex);
                break;
            }
        }
    }
    
    // This method logs whether the recording was successful or not
    private void postRecordingStatusMessage()
    {
        if (m_completed)
        {
            m_controller.displayMessage("Recording completed in " + m_recordingCompleteTime + " seconds.");
        }
        else
        {
            m_controller.displayMessage("Recording failed.");
        }
    }
    
    // Starts the timer when recording is started
    private void startTimer()
    {
        m_startTime = System.currentTimeMillis();
    }
    
    // Stops the timer when recording is finished
    private void stopTimer()
    {
        m_stopTime = System.currentTimeMillis();
        m_recordingCompleteTime = (m_stopTime - m_startTime)/1000;
    }
    
    /**
     * Schedules a recording
     * @param index the index of the tuner to record
     * @param duration the duration of the recording in seconds
     * @param delay how long to delay the start of the recording in seconds
     */
    private void record(final int index, final long duration, final long delay)
    {
        new Thread()
        {
            public void run()
            {
                m_currentlyRecording = m_oadDVR.recordService(index, duration, delay, true);
                startTimer();
                m_completed = m_oadDVR.waitForRecordingState(1, duration + 5, LeafRecordingRequest.COMPLETED_STATE);
                m_currentlyRecording = false;
                stopTimer();
                postRecordingStatusMessage();
            }
        }.start();
    }
    
    private int getNextServiceIndex()
    {
        int currentServiceIndex = m_controller.getCurrentService();
        int numServices = m_oadCore.getNumServices();
        int retVal = (currentServiceIndex + 1) % numServices; 
        return retVal;
    }
    
    public void destroy()
    {
        // TODO Auto-generated method stub

    }

    public void init()
    {
        // TODO Auto-generated method stub

    }

}
