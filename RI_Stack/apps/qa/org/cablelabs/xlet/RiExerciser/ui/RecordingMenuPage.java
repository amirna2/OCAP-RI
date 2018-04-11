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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverHNDVR;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverInterfaceHNDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This Page provides options for DVR Recordings for a variety of durations
 * and includes options for delay recordings and making background recordings.
 * Since this Page is only accessible from the DVR General Menu, which is only
 * accessible if DVR extensions are enabled, it can be assumed that DVR 
 * extensions are enabled.
 *
 */
public class RecordingMenuPage extends QuarterPage
{
    private int m_recordingDelayTime = 0;
    
    private int m_recordingDuration = 30; 
    
    private long m_recordingCompleteTime;
    
    private long m_startTime;
    
    private long m_stopTime;
    
    private static RecordingMenuPage m_page = new RecordingMenuPage();
    
    private boolean m_completed;
    
    private String m_localOrRemote = "Local";
    
    /**
     * The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverDVR from RiExerciserController for DVR functionality
     */
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    private OcapAppDriverInterfaceHNDVR m_oadHNDVR;
    
    private boolean m_currentlyRecording;
    
    private boolean m_pageActive;
    
    private Color m_recordingColor = Color.RED;
    
    private RiExerciserContainer m_container;
    
    // flag to identify how the recording has to be scheduled (Local or Remote)
    private boolean m_recordRemotely = false;
    
    private RecordingMenuPage()
    {
        m_controller = RiExerciserController.getInstance();
        m_container = RiExerciserContainer.getInstance();
        m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        if(m_controller.isHnEnabled())
        {
            m_oadHNDVR = OcapAppDriverHNDVR.getOADHNDVRInterface();
        }
        // Initialize the Container layout.
        initMenuBox();
        m_completed = false;
        m_currentlyRecording = false;
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
    }
    
    public static RecordingMenuPage getInstance()
    {
        return m_page;
    }
    
    private synchronized void initMenuBox()
    {
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        
        add(m_menuBox);
        m_menuBox.setVisible(true);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        // Write the menu options
        writeMenuOptions();
        m_menuBox.repaint();
        this.repaint();
    }
    
    private void writeMenuOptions()
    {
        m_menuBox.write("Recording Menu:");
        m_menuBox.write("0. Return to DVR Specific Menu");
        m_menuBox.write("1. Start 30-sec Instant Recording");
        m_menuBox.write("2. Start 5-min Instant Recording");
        m_menuBox.write("3. Start 30-sec Background Recording");
        m_menuBox.write("4. Start 5-min Background Recording");
        m_menuBox.write("5. Schedule 3 20 sec b2b Background Recordings");
        m_menuBox.write("6. Start " + m_recordingDuration + "-second Recording " +  m_recordingDelayTime + " seconds from now");
        m_menuBox.write("LEFT/RIGHT: Adjust recording start time");
        m_menuBox.write("UP/DOWN: Adjust recording duration");
        m_menuBox.write("SETTINGS: Toggle between local and remote recording, currently " + m_localOrRemote);
        m_menuBox.repaint();
        m_container.repaintScene();
    }
    
    // This method updates the menu box when the left, right, up, or down
    // keys are pressed
    private synchronized void updateMenuBox()
    {
        new Thread()
        {
            public void run()
            {
                m_menuBox.reset();
                writeMenuOptions();
            }
        }.start();

    }

    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_LEFT:
            case OCRcEvent.VK_KP_LEFT:
            {
                // Decrease m_recordingStartTime
                m_recordingDelayTime -= 5;
                updateMenuBox();
                break;
            }
            case OCRcEvent.VK_RIGHT:
            case OCRcEvent.VK_KP_RIGHT:
            {
                // Increase m_recordingStartTime
                m_recordingDelayTime += 5;
                updateMenuBox();
                break;
            }
            case OCRcEvent.VK_UP:
            case OCRcEvent.VK_KP_UP:
            {
                // Increase m_recordingDuration
                m_recordingDuration += 5;
                updateMenuBox();
                break;
            }
            case OCRcEvent.VK_DOWN:
            case OCRcEvent.VK_KP_DOWN:
            {
                // Decrease m_recordingDuration
                if (m_recordingDuration > 0)
                {
                    m_recordingDuration -= 5;
                    updateMenuBox();
                }
                break;
            }
            case OCRcEvent.VK_0:
            {
                // Menu Option 0: Return to DVR Menu Page
                m_controller.displayNewPage(RiExerciserConstants.DVR_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Start 30-second instant recording
                m_controller.displayMessage("Beginning 30-second instant recording:");
                record(0, 30, 0, false);
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Start 5-min instant recording
                m_controller.displayMessage("Beginning 5-min instant recording:");
                record(0, 300, 0, false);
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Start 30-second background recording
                m_controller.displayMessage("Beginning 30-second background recording:");
                record(0, 30, 0, true);
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Start 5-min background recording
                m_controller.displayMessage("Beginning 5-min background recording:");
                record(0, 300, 0, true);
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Schedule 3 30 sec b2b Background Recordings
                m_controller.displayMessage("Beginning back to back recordings");
                record(0, 20, 0, true);
                record(0, 20, 0, true);
                record(0, 20, 0, true);
                break;
            }
            case OCRcEvent.VK_6:
            {
                // Menu Option 6: Start recording with duration m_recording duration
                // and delay m_recordingStartTime
                record(0, m_recordingDuration, m_recordingDelayTime, true);
                break;
            }
            case OCRcEvent.VK_SETTINGS:
            {
                // Toggle between local and remote recordings
                if (m_controller.isHnEnabled())
                {
                    m_recordRemotely = !m_recordRemotely;
                    m_localOrRemote = m_recordRemotely ? "Remote" : "Local";
                    m_menuBox.reset();
                    writeMenuOptions();
                }
                break;
            }
        }
    }
    
    // This method logs whether the recording was successful or not
    private void postRecordingStatusMessage()
    {
        if (m_completed && m_recordRemotely)
        {
            m_controller.displayMessage("Recording completed in " + m_recordingCompleteTime
                    + " seconds and auto published with NRE.");
        }
        else if (m_completed && !m_recordRemotely)
        {
            m_controller.displayMessage("Recording completed in " + m_recordingCompleteTime + " seconds");
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
    private void record(final int index, final long duration, final long delay, final boolean background)
    {
        new Thread()
        {
            public void run()
            {
                startTimer();
                if(m_recordRemotely && m_oadHNDVR != null)
                {
                    m_currentlyRecording = m_oadHNDVR.createScheduledRecording(index, duration, delay, background);
                }
                else
                {
                    m_currentlyRecording = m_oadDVR.recordTuner(index, duration, delay, background);
                }
                m_completed = m_oadDVR.waitForRecordingState( index, 
                                                              Math.max(0, duration + delay) + 5, 
                                                              OcapAppDriverDVR.COMPLETED_STATE );
                m_currentlyRecording = false;
                stopTimer();
                postRecordingStatusMessage();
            }
        }.start();
    }
    
    // A method to display a countdown once recording has begun
    private void countDownRecordingTimeout(long timeout)
    {
        for (long i = timeout; i >= 0; i--)
        {   
            int recordingState = m_oadDVR.getCurrentRecordingState(0);
            if (recordingState != OcapAppDriverDVR.COMPLETED_STATE)
            {
                m_controller.displayMessage("Recording: " + i);
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                
                }
            }
        }
    }
    
    private void indicateRecordingState()
    {
        new Thread()
        {
            public void run()
            {
                Graphics g = m_container.getSceneGraphics();
                DVBGraphics dvbG = (DVBGraphics)g;
                while (m_pageActive)
                {
                    if (m_currentlyRecording)
                    {
                        dvbG.setColor(m_recordingColor);
                        Toolkit tk = Toolkit.getDefaultToolkit();
                        Dimension dim = tk.getScreenSize();
                        int x = dim.width;
                        int y = dim.height;
                        g.fillOval(x/32,y/32, 10, 10);
                        m_page.repaint();
                        m_container.repaintScene();
                        try
                        {
                            Thread.sleep(500);
                            m_page.repaint();
                            m_container.repaintScene();
                        }
                        catch(InterruptedException e)
                        {
                            
                        }
                    }
                }
            }
        }.start();
    }
    
    public void destroy()
    {
        m_pageActive = false;
        m_menuBox.setVisible(false);
    }

    public void init()
    {
        m_menuBox.setVisible(true);
        m_pageActive = true;
        indicateRecordingState();
    }

}
