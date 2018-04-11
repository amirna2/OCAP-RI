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
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.ui.event.OCRcEvent;

public class HNPublishChannelMenuPage extends QuarterPage
{
    private static HNPublishChannelMenuPage m_page = new HNPublishChannelMenuPage();
    
    /**
     *  The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverHN from RiExerciserController for HN functionality
     */
    private OcapAppDriverInterfaceHN m_oadHN;
    
    /**
     * A boolean indicating whether publishing completed
     */
    private boolean m_publishCompleted;
    
    /**
     * A SelectorList to display ChannelContentItem URLS
     */
    private SelectorList m_channelContentItemURLList;

    /**
     * Timeout in milliseconds to wait for HN action to complete
     */
    private final long m_hnActionTimeoutMS;
    
    private boolean m_cdsFound;
    
    private RiExerciserContainer m_container;
    
    private HNPublishChannelMenuPage()
    {
        // Initialize the components
        setLayout(null);
        m_publishCompleted = false;
        m_cdsFound = false;
        m_controller = RiExerciserController.getInstance();
        m_container = RiExerciserContainer.getInstance();
        m_oadHN  = OcapAppDriverHN.getOADHNInterface();
        m_channelContentItemURLList = new SelectorList();
        
        m_hnActionTimeoutMS = m_controller.getHNActionTimeoutMS();

        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true); 
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options. Only write options for which extensions are 
        // enabled
        m_menuBox.write("HN Publish Channel Options");
        m_menuBox.write("0. Return to HN Server Menu");
        m_menuBox.write("1. Publish current channel in CDS");
        m_menuBox.write("2. Publish all channels in CDS");
        m_menuBox.write("3. Display list of channel item URLs");
        m_menuBox.write("4. Publish current channel in CDS- alternate res");
        m_menuBox.write("5. Publish all channels in CDS- alternate res");
        m_menuBox.write("6. Publish all channels with SRH");
        m_menuBox.write("7. Update Tuning Locator With SRH");
        m_menuBox.write("8. Publish current channel as VOD");
    }
    
    public static HNPublishChannelMenuPage getInstance()
    {
        return m_page;
    }

    public void processUserEvent(KeyEvent event)
    {
        switch (event.getKeyCode())
        {
            // Menu Option 0:  Return to HN Server Menu
            case OCRcEvent.VK_0:
            {
                m_controller.displayNewPage(RiExerciserConstants.HN_SERVER_OPTIONS_PAGE);
                break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Publish current channel in CDS root container
                publishCurrentChannel(false);
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Publish all channels in CDS root container
                publishAllChannels();
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Display list of channel item URLs
                displayChannelContentItemURLList();
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Publish current channel in CDS- alternate res
                if (!m_cdsFound)
                {
                    if (!waitForMediaServer())
                    {
                        m_controller.displayMessage("Unable to find local media server");
                        break;
                    }
                }
                int currentService = m_controller.getCurrentService();
                m_publishCompleted = m_oadHN.publishServiceUsingAltRes(currentService, m_hnActionTimeoutMS);
                postPublishStatusMessage();
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Publish all channels in CDS- alternate res
                if (!m_cdsFound)
                {
                    if (!waitForMediaServer())
                    {
                        m_controller.displayMessage("Unable to find local media server");
                        break;
                    }
                }
                m_publishCompleted = m_oadHN.publishAllServicesUsingAltRes(m_hnActionTimeoutMS);
                postPublishStatusMessage();
                break;
            }
            
            case OCRcEvent.VK_6:
            {
                m_publishCompleted = m_oadHN.publishAllServicesWithSRH(m_hnActionTimeoutMS);
                postPublishStatusMessage();
                break;
            }
            

            case OCRcEvent.VK_7:
            {
                // Menu Option 7: Update Tuning Locator With SRH
                // Specific to ServiceResolutionHandler
                m_controller.displayMessage("Calling updateTuningLocatorWithSRH");
                m_oadHN.updateTuningLocatorWithSRH();
                break;
            }
            case OCRcEvent.VK_8:
            {
                // Menu Option 8: Publish current channel as VOD in CDS root container
                publishCurrentChannel(true);
                break;
            }
            case OCRcEvent.VK_ENTER:
            {
                // This prevents the display of HN Server Menu Page when
                // an item is selected from a list
                
                m_channelContentItemURLList.setVisible(false);
                m_channelContentItemURLList.setEnabled(false);
                m_channelContentItemURLList.setFocusable(false);
                m_container.removeFromScene(m_channelContentItemURLList);
                
                m_container.requestSceneFocus();
                m_container.popToFront(m_page);
                m_page.repaint();;
                m_container.repaintScene();
                
                break;
            }
        }
    }

    /**
     * This method will publish the current channel to the CDS
     * @param publishAsVOD set to true if channel is published as VOD type
     */
    public void publishCurrentChannel(boolean publishAsVOD)
    {
        if (!m_cdsFound)
        {
            if (!waitForMediaServer())
            {
                m_controller.displayMessage("Unable to find local media server");
                return;
            }
        }
        int currentService = m_controller.getCurrentService();
        m_publishCompleted = m_oadHN.publishServiceToChannelContainer(currentService, m_hnActionTimeoutMS, publishAsVOD);
        postPublishStatusMessage();
    }

    /**
     * This method will publish all channels to the CDS
     */
    public void publishAllChannels()
    {
        if (!m_cdsFound)
        {
            if (!waitForMediaServer())
            {
                m_controller.displayMessage("Unable to find local media server");
                return;
            }
        }
        m_publishCompleted = m_oadHN.publishAllServicesToChannelContainer(m_hnActionTimeoutMS);
        postPublishStatusMessage();
    }
    
    public boolean publishSuccessful()
    {
        return m_publishCompleted;
    }
    
    private void displayChannelContentItemURLList()
    {
        m_channelContentItemURLList = new SelectorList();
        // Initialize the list of recordings
        if (!m_cdsFound)
        {
            m_cdsFound = waitForMediaServer();
        }
        int mediaServerIndex = m_oadHN.findLocalMediaServer();
        m_oadHN.refreshServerContentItems(mediaServerIndex, "0", m_hnActionTimeoutMS, false, false);
        int numChannelContentItems = m_oadHN.getNumChannelContentItems(mediaServerIndex);
        final Vector channelContentItemURLs = new Vector();
        for (int i = 0; i < numChannelContentItems; i++)
        {
            channelContentItemURLs.add(m_oadHN.getChannelContentItemURL(mediaServerIndex, i));
        }
        m_channelContentItemURLList.initialize(channelContentItemURLs);
        m_channelContentItemURLList.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_channelContentItemURLList.getSelectedItemIndex();
                
                // Set m_recordingIndex to the index of the selected item
                if (selectedIndex != -1)
                {

                }
                
                // Once an item has been selected, remove and disable the list
                m_channelContentItemURLList.setVisible(false);
                m_channelContentItemURLList.setEnabled(false);

                m_container.removeFromScene(m_channelContentItemURLList);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initialize the SelectorList
        m_channelContentItemURLList.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(m_channelContentItemURLList);
        m_channelContentItemURLList.setVisible(true);
        m_container.scenePopToFront(m_channelContentItemURLList);
        m_channelContentItemURLList.setEnabled(true);
        m_channelContentItemURLList.setFocusable(true);
        m_channelContentItemURLList.requestFocus();
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
        
        // Make sure local content server net module has been found so content can be published
        if (m_oadHN.waitForLocalContentServerNetModule(m_controller.getLocalServerTimeoutSecs()))
        {
            // Do search on local server
            m_oadHN.refreshServerContentItems(m_oadHN.findLocalMediaServer(), "0", m_hnActionTimeoutMS, false, false);
        }
    }

}
