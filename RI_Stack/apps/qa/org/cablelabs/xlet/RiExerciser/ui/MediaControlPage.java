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

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This Page provides Media Options for DVR such as trick modes and enabling
 * and disabling TSB and logging of buffer stats. Since this Page is only
 * accessible from the DVR General Menu, which is only accessible if DVR 
 * extensions are enabled, it can be assumed that DVR extensions are enabled.
 *
 */
public class MediaControlPage extends QuarterPage
{
    
    private static MediaControlPage m_page = new MediaControlPage();
    
    /**
     * OcapAppDriverCore from RiExerciserController for core functionality like
     * tuning and resizing playback
     */
    private OcapAppDriverInterfaceCore m_oadCore;
    
    /**
     * OcapAppDriverDVR from RiExerciserController for DVR functionality
     */
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    /**
     * A boolean indicating whether tsb is enabled or not
     */
    private boolean m_tsbEnabled;
    
    /**
     * A boolean indicating whether logging of buffer stats is enabled
     */
    private boolean m_bufferStatLoggingEnabled = false;
    
    private TVTimerSpec m_tsbLoggingTimerSpec;

    private TVTimer m_tsbLoggingTimer;
    
    private TVTimerWentOffListener m_tsbLoggingTimerWentOffListener;
    
    /**
     * A boolean indicating whether English is the preferred language
     */
    private boolean m_englishPreferred;
    
    /**
     * The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    private MediaControlPage()
    {
        m_tsbLoggingTimerWentOffListener = new TSBLoggingTimerWentOffListener();
        m_tsbLoggingTimer = TVTimer.getTimer();
        m_tsbLoggingTimerSpec = new TVTimerSpec();
        m_tsbLoggingTimerSpec.setAbsolute(false); // Always a delay
        m_tsbLoggingTimerSpec.setTime(1000); // Always the same delay
        m_tsbLoggingTimerSpec.setRepeat(true); // Only once
        // Initialize components
        m_controller = RiExerciserController.getInstance();
        m_oadCore = OcapAppDriverCore.getOADCoreInterface();
        m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        m_tsbEnabled = m_oadDVR.isTsbEnabled();
        final String langPref[] = m_oadCore.getLanguagePreference();
        final String prefLanguage = (langPref.length > 0) ? langPref[0] : "undefined";
        m_englishPreferred = prefLanguage.equals("eng"); 
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        
        add(m_menuBox);
        m_menuBox.setVisible(true);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write menu options
        m_menuBox.write("Media Control Options:");
        m_menuBox.write("0. Return to DVR Specific Menu");
        m_menuBox.write("1. Toggle Authorization and Run Auth Check");
        m_menuBox.write("2. Run Auth Check without toggling authorization");
        m_menuBox.write("3. Skip 10 seconds back");
        m_menuBox.write("4. Skip 10 seconds forward");
        m_menuBox.write("5. Enable/disable logging of buffer stats");
        m_menuBox.write("6. Enable/disable TSB");
        m_menuBox.write("7. Print CCI for Current Service");
        m_menuBox.write("8. Change language preference (currently " +  prefLanguage + ')');
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.repaint();
        this.repaint();
    }
    
    public static MediaControlPage getInstance()
    {
        return m_page;
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
                // Menu Option 1: toggle authorization and run auth check
                m_oadCore.runAuthorization(true);
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: run auth check without toggling authorization
                m_oadCore.runAuthorization(false);
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: skip 10 seconds back
                m_oadCore.skipBackward(10);
                
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: skip 10 seconds forward
                m_oadCore.skipForward(10);
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: enable/disable logging of buffer stats
                m_bufferStatLoggingEnabled = !m_bufferStatLoggingEnabled;
                if (m_bufferStatLoggingEnabled)
                {
                    // no need to hold ref, listener will remove itself when triggered
                    m_tsbLoggingTimerSpec.addTVTimerWentOffListener(m_tsbLoggingTimerWentOffListener);
                    try
                    {
                        m_tsbLoggingTimerSpec = m_tsbLoggingTimer.scheduleTimerSpec(m_tsbLoggingTimerSpec);
                    }
                    catch (Exception e)
                    {
                        m_controller.displayMessage("Unable to schedule timer spec - " + e.getMessage());
                    }
                }
                else
                {
                    m_tsbLoggingTimerSpec.removeTVTimerWentOffListener(m_tsbLoggingTimerWentOffListener);
                    m_tsbLoggingTimer.deschedule(m_tsbLoggingTimerSpec);
                }
                break;
            }
            case OCRcEvent.VK_6:
            {
                // Menu Option 6: Implement enable/disable TSB
                m_tsbEnabled = !m_tsbEnabled;
                m_oadDVR.tsbControl(m_tsbEnabled);
                if (m_tsbEnabled)
                {
                    m_controller.displayMessage("TSB Enabled");
                }
                else
                {
                    m_controller.displayMessage("TSB Disabled");
                }
                break;
            }
            case OCRcEvent.VK_7:
            {
                // Menu Option 7: Print CCI for Current Service
                int cciBits = m_oadCore.getCCIBits();
                m_controller.displayMessage("Retrieved CCI: " + cciBits);
                break;
            }
            case OCRcEvent.VK_8:
            {
                // Menu Option 8: Change language preference
                m_englishPreferred = !m_englishPreferred;
                if (m_englishPreferred)
                {
                    m_oadCore.setLanguagePreference(new String[] {"eng","spa"});
                }
                else
                {
                    m_oadCore.setLanguagePreference(new String[] {"spa","eng"});
                }
                    
                if (m_englishPreferred)
                {
                    m_controller.displayMessage("Language preference set to English");
                }
                else
                {
                    m_controller.displayMessage("Language preference set to Spanish");
                }
                break;
            }
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
    
    class TSBLoggingTimerWentOffListener implements TVTimerWentOffListener
    {
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            int precision = 100; // two digits
            float nanosPerSecond = 1000000000.0F;
            double buffStart = m_oadDVR.getBufferTime(true);
            long mediaTimeNano = m_oadCore.getMediaTime();
            double mediaTime = Math.floor((mediaTimeNano/ nanosPerSecond) * precision
                    + .5)
                    / precision;
            double buffEnd = m_oadDVR.getBufferTime(false);
            double deltaToBuffEnd = Math.floor((buffEnd - mediaTime) * precision + .5) / precision;
            if (mediaTimeNano == Long.MIN_VALUE)
            {
                m_controller.displayMessage("Unable to obtain media time");
            }
            if (Double.isNaN(buffStart) || Double.isNaN(buffEnd))
            {
                m_controller.displayMessage("Unable to obtain buffer stats");
            }
            else
            {
                m_controller.displayMessage("buffer start: " + buffStart + ", media time: " + mediaTime + ", buffer end: "
                    + buffEnd + ", secs to buff end: " + deltaToBuffEnd);
            }
        }
    }
}
