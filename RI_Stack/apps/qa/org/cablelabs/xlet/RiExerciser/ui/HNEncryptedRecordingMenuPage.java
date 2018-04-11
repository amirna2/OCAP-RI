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

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.ui.event.OCRcEvent;

public class HNEncryptedRecordingMenuPage extends QuarterPage
{
    
    private static HNEncryptedRecordingMenuPage m_page = new HNEncryptedRecordingMenuPage();
    
    private RiExerciserController m_controller;
    
    private static final Logger log = Logger.getLogger(HNEncryptedRecordingMenuPage.class);
    
    private static final String[] m_telnetCommands = new String[] { "TSB Buffering,c",
        "2\r\r", "0\r\r", "0\r\r", "0\r\r" };
    private static final String[] m_resetTelnetCommands = new String[] { "TSB Buffering,c",
        "0\r\r", "0\r\r", "0\r\r", "0\r\r" };
    private static final String[] m_telnetResultString = new String[] { 
        "EMI Values and Copy Permissions", "APS Value Definitions",
            "CIT Value Definitions", "RCT Value Definitions", "RESULT: " };
    
    private OcapAppDriverInterfaceDVR m_oadIfDvr;
    
    private OcapAppDriverInterfaceHN m_oadIfHn;

    protected boolean m_currentlyRecording;

    protected boolean m_completed;

    private long m_startTime;

    private long m_stopTime;

    private long m_recordingCompleteTime;
    
    private HNEncryptedRecordingMenuPage()
    {
        // Initialize the components
        setLayout(null);
        m_controller = RiExerciserController.getInstance();
        if (m_controller.isDvrEnabled())
        {
            m_oadIfDvr = OcapAppDriverDVR.getOADDVRInterface();
        }
        m_oadIfHn = OcapAppDriverHN.getOADHNInterface();
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true); 
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options. Only write options for which extensions are 
        // enabled
        m_menuBox.write("HN Encrypted Recording Menu");
        m_menuBox.write("0. Return to HN General Menu");
        m_menuBox.write("1. Start 30-second encrypted instant recording");
        m_menuBox.write("2. Start 5-min encrypted instant recording");
    }
    
    public static HNEncryptedRecordingMenuPage getInstance()
    {
        return m_page;
    }

    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
                // Menu Option 0: Return to HN General Menu
                m_controller.displayNewPage(RiExerciserConstants.HN_GENERAL_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 2: Start 30-second encrypted instant recording
                m_controller.displayMessage("Beginning 30-second encrypted instant recording");
                record(0, 30);
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 3: Start 5-min encrypted instant recording
                m_controller.displayMessage("Beginning 5-min encrypted instant recording");
                record(0, 300);
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
     * @param background a boolean indicating whether the recording should be a background recording
     */
    private void record(final int index, final long duration)
    {
        new Thread()
        {
            public void run()
            {
                m_currentlyRecording = m_oadIfDvr.recordTuner(index, duration, 0, false);
                startTimer();
                // Insert CCI bits for encryption once recording is in progress
                if (m_oadIfDvr.waitForRecordingState(0, duration, OcapAppDriverDVR.IN_PROGRESS_STATE))
                {
                    boolean cciBitsSet = m_oadIfHn.setCCIbits(m_telnetCommands, m_telnetResultString);
                    if (cciBitsSet)
                    {
                        m_controller.displayMessage("Set CCI bits for in-progress recording");
                    }
                    else
                    {
                        m_controller.displayMessage("Faild to set CCI bits for in-progress recording");
                        m_completed = false;
                        return;
                    }
                }
                else
                {
                    m_controller.displayMessage("Recording failed to reach in-progress state");
                    m_completed = false;
                    return;
                }
                m_completed = m_oadIfDvr.waitForRecordingState(index, duration + 5, OcapAppDriverDVR.COMPLETED_STATE);
                boolean cciBitsResetSet = m_oadIfHn.setCCIbits(m_resetTelnetCommands, m_telnetResultString);
                if (cciBitsResetSet)
                {
                    m_controller.displayMessage("Reset CCI bits to COPY_FREELY");
                    if (log.isInfoEnabled())
                    {
                        log.info("Reset CCI bits to COPY_FREELY");
                    }
                }
                else
                {
                    // Failure to reset need not stop any other encrypted
                    // recordings to follow
                    m_controller.displayMessage("Reset CCI bits to COPY_FREELY failed");
                    if (log.isInfoEnabled())
                    {
                        log.info("Reset CCI bits to COPY_FREELY failed");
                    }
                }
                m_currentlyRecording = false;
                stopTimer();
                postRecordingStatusMessage();
            }
        }.start();
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
