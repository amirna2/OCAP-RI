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
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBColor;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This Page displays options for the HN Player. Since this Page is only 
 * accessible from the HN General Menu, which is only accessible if HN 
 * extensions are enabled, it can be assumed that HN extensions are enabled.
 * Future additions to this class may require DVR or HN/DVR functionality. In
 * that case, it will need to be verified that DVR extensions are enabled before
 * making any calls to OcapAppDriverDVR or OcapAppDriverHNDVR.
 *
 */
public class HNPlayerMenuPage extends QuarterPage
{
    private static final Logger log = Logger.getLogger(HNPlayerMenuPage.class);
    
    /**
     * The Singleton instance of this class
     */
    private static HNPlayerMenuPage m_page = new HNPlayerMenuPage();
    
    /**
     *  The Singleton instance of RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverCore from RiExerciserController for basic functionality 
     * like tuning and resizing the playback
     */
    private OcapAppDriverInterfaceCore m_oadCore;
    
    /**
     * OcapAppDriverHN from RiExerciserController for HN functionality
     */
    private OcapAppDriverInterfaceHN m_oadHN;
    
    /**
     * A boolean indicating whether media servers should be searched or browsed
     */
    private boolean m_browseContainer;
    
    /**
     * A String indicating whether media servers will be searched or browsed for 
     * content
     */
    private String m_searchOrBrowse;
    
    /**
     * The value for search
     */
    private final String SEARCH = "Search";
    
    /**
     * The value for browse
     */
    private final String BROWSE = "Browse";
    
    /**
     *  The index of the selected content item
     */
    private int m_contentIndex;
    
    /**
     *  The index of the selected media server
     */
    private int m_mediaServerIndex = -1;
    
    // SelectorLists corresponding to menu options 1 through 5
    
    /**
     *  The list of Media Servers
     */
    private SelectorList m_mediaServerList;
    
    /**
     * The list of content items associated with the selected media server
     */
    private SelectorList m_contentItemList;
    
    /**
     * The list of remote services associated with the selected media server
     */
    private SelectorList m_remoteServiceList;
    
    /**
     * The ID of the current container being browsed or searched
     */
    private String m_currentContainerID;
    
    /**
     * The ID of the parent Container
     */
    private String m_parentContainerID;
    
    /**
     * The name of the current Content Container
     */
    private String m_currentContainerName;
    
    /**
     * The name of the parent of the current Content Container
     */
    private String m_parentContainerName;
    
    /**
     * Timeout in milliseconds to wait for HN action to complete
     */
    private final long m_hnActionTimeoutMS;
    
    /**
     * A String representation of the currently selected media server
     */
    private String m_mediaServerSelected;
    
    private RiExerciserContainer m_container;
    
    /**
     * A Stack to keep track of the IDs of the Containers being browsed or
     * searched. This Object should use Stack methods only (i.e., pop() and push())
     */
    private Stack m_containerIds;
    
    /**
     * A Stack to keep track of the names of the Containers being browsed or
     * searched. This Object should use Stack methods only (i.e., pop() and push())
     */
    private Stack m_containerNames;
    
    private HNPlayerMenuPage()
    {
        m_mediaServerSelected = "None";
        // Initialize the components
        setLayout(null);
        m_containerIds = new Stack();
        m_containerNames = new Stack();
        m_browseContainer = false;
        m_searchOrBrowse = SEARCH;
        // Begin all browsing or searching at the root container
        m_currentContainerID = RiExerciserConstants.ROOT_CONTAINER_ID;
        m_parentContainerID = null;
        m_currentContainerName = RiExerciserConstants.ROOT_CONTAINER_NAME;
        m_parentContainerName = null;
        m_controller = RiExerciserController.getInstance();
        m_container = RiExerciserContainer.getInstance();
        m_oadCore = OcapAppDriverCore.getOADCoreInterface();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        
        add(m_menuBox);
        
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));

        m_hnActionTimeoutMS = m_controller.getHNActionTimeoutMS();
        
        // Write menu options
        writeMenuOptions();
        m_menuBox.setVisible(true);
        m_menuBox.repaint();
        this.repaint();
    }
    
    public static HNPlayerMenuPage getInstance()
    {
        return m_page;
    }

    public void processUserEvent(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
                // Menu Option 0: Return to Home Networking Menu Page
                RiExerciserController.getInstance().displayNewPage(RiExerciserConstants.HN_GENERAL_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Display media servers
                boolean cdsFound = m_oadHN.waitForLocalContentServerNetModule(
                                        m_controller.getLocalServerTimeoutSecs());
                if (cdsFound)
                {
                    displayMediaServers();
                }
                else
                {
                    m_controller.displayMessage("Error finding local media server");
                }
                break;
            }
            case OCRcEvent.VK_2:
            {
                /**
                 * Menu Option 2: Display content items if a media server has
                 * been selected, otherwise log a message stating that no media
                 * server has been selected
                 */
                if (m_mediaServerIndex == -1)
                {
                    m_controller.displayMessage("No media server selected.");
                }
                
                else
                {
                    m_oadHN.refreshServerContentItems(m_mediaServerIndex, "0", m_hnActionTimeoutMS, m_browseContainer, false);
                    displayContentItems();
                }
                break;
            }
            case OCRcEvent.VK_3:
            {
             // Menu Option 3: Display remote services
                if (m_mediaServerIndex == -1)
                {
                    m_controller.displayMessage("No media server selected.");
                }
                else
                {
                    m_oadHN.refreshServerContentItems(m_mediaServerIndex, "0", m_hnActionTimeoutMS, m_browseContainer, false);
                    displayRemoteServices();
                }
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: VPOP Player Test Menu Page
                RiExerciserController.getInstance().displayNewPage(RiExerciserConstants.VPOP_CLIENT_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_UP:
            case OCRcEvent.VK_DOWN:
            {
                 m_browseContainer = !m_browseContainer;
                 m_searchOrBrowse = m_browseContainer ? BROWSE : SEARCH;
                 m_menuBox.reset();
                 writeMenuOptions();
                 break;
            }
        }
    }
    
    private void writeMenuOptions()
    {
        m_menuBox.write("HN Player Options:");
        m_menuBox.write("0. Return to HN General Menu");
        m_menuBox.write("1. Display list of all Media Servers on a network");
        m_menuBox.write("2. Select Content Item for JMF Playback (" + m_searchOrBrowse + ")");
        m_menuBox.write("3. Select Remote Service for Playback (" + m_searchOrBrowse + ")");
        m_menuBox.write("4. VPOP Player Test Menu Page");
        m_menuBox.write("UP/DOWN- Toggle between search and browse");
    }
    
    /**
     * Returns a string representation of supplied player type.
     * 
     * @param playerType    get string for this type
     * 
     * @return  string describing player type
     */
    protected static String getPlayerTypeStr(int playerType)
    {
        String playerTypeStr = "Unknown";
        
        switch (playerType)
        {
        case OcapAppDriverInterfaceCore.PLAYBACK_TYPE_SERVICE:
            playerTypeStr = "Service";
            break;
        case OcapAppDriverInterfaceCore.PLAYBACK_TYPE_JMF:
            playerTypeStr = "JMF";
            break;
        default:
            if (log.isWarnEnabled())
            {
                log.warn("getPlayerTypeStr() - unsupported player type: " +
                        playerType);
            }
        }
        
        return playerTypeStr;    
    }
        
    /**
     * This method creates and displays the list of media servers on the network, 
     * which is Menu Option 1
     */
    private void displayMediaServers()
    {   
        // Initialize the Vector of MediaServer strings
        final Vector mediaServers = new Vector();
        m_mediaServerList = new SelectorList();
        m_oadHN.refreshNetModules();
        int numServers = m_oadHN.getNumMediaServersOnNetwork();
        for (int i = 0; i < numServers; i++)
        {
            mediaServers.add(m_oadHN.getMediaServerInfo(i));
        }
        m_mediaServerList.initialize(mediaServers);
        m_mediaServerList.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_mediaServerList.getSelectedItemIndex();
                // Set m_mediaServerIndex to the index of the selected media server
                if (selectedIndex != -1)
                {
                    m_mediaServerIndex = selectedIndex;
                    m_mediaServerSelected = m_oadHN.getMediaServerFriendlyName(selectedIndex);
                    m_controller.displayMessage("Media server selected: " + mediaServers.get(m_mediaServerIndex));
                    m_controller.displayMessage("Selected MediaServer index " + m_mediaServerIndex);
                }
                // After selection has occurred, remove the list and disable item
                m_mediaServerList.setVisible(false);
                m_mediaServerList.setEnabled(false);
                
                m_container.removeFromScene(m_mediaServerList);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initializing the SelectorList 
        m_mediaServerList.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(m_mediaServerList);
        m_mediaServerList.setVisible(true);
        m_container.scenePopToFront(m_mediaServerList);
        m_mediaServerList.setEnabled(true);
        m_mediaServerList.setFocusable(true);
        m_mediaServerList.requestFocus();
    }
    
    private void displayContentItems()
    {
        m_contentItemList = new SelectorList();
        
        if (!m_currentContainerID.equals(RiExerciserConstants.ROOT_CONTAINER_ID))
        {
            m_contentItemList.setFirstItemText("Return to " + m_parentContainerName);
        }
        
        // Initialize the list of content items associated with m_mediaServerIndex
        int numContentItems = m_oadHN.getNumServerContentItems(m_mediaServerIndex);
        final Vector contentItems = new Vector();
        for (int i = 0; i < numContentItems; i++)
        {
            if (m_oadHN.isContentContainer(m_mediaServerIndex, i))
            {
                contentItems.add(m_oadHN.getServerContentItemInfo(m_mediaServerIndex, i));
            }
            else
            {
                contentItems.add(m_oadHN.getVideoURL(m_mediaServerIndex, i));
            }
        }
        m_contentItemList.initialize(contentItems);
        m_contentItemList.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_contentItemList.getSelectedItemIndex();
                
                /** 
                 * Set m_contentIndex to the index of the selected item, then
                 * play the selected content item
                 */
                if (selectedIndex != -1)
                {
                    if (m_oadHN.isContentContainer(m_mediaServerIndex, selectedIndex))
                    {
                        if (updateCurrentContainer(selectedIndex, m_contentItemList))
                        {
                            displayContentItems();
                        }
                    }
                    
                    else
                    {
                        m_contentIndex = m_contentItemList.getSelectedItemIndex();
                        disableAndRemoveList(m_contentItemList);
                        String contentItemString = contentItems.get(selectedIndex).toString();
                        m_controller.displayMessage("Content item selected: " + contentItemString);
                        // If selected Content Item is VPOP and local media server 
                        // is selected, display message and don't play video
                        if (m_mediaServerIndex == m_oadHN.findLocalMediaServer())
                        {
                            if (m_oadHN.isVPOPContentItem(m_mediaServerIndex, m_contentIndex))
                            {
                                m_controller.displayMessage("Unable to stream VPOP to self");
                                return;
                            }
                        }
                        playVideo(OcapAppDriverCore.PLAYBACK_TYPE_JMF);
                    }
                }
                
                else if (selectedIndex == -1 && 
                        !(m_currentContainerID.equals
                                (RiExerciserConstants.ROOT_CONTAINER_ID)))
                {
                    returnToParentContainer(m_contentItemList);
                    displayContentItems();
                }
                
                else
                {
                    // Remove and disable the list once an item has been selected
                    disableAndRemoveList(m_contentItemList);
                }
            }
            
        });
        
        // Initialize the SelectorList
        enableAndInitializeList(m_contentItemList);
    }
    
    private void displayRemoteServices()
    {
        m_remoteServiceList = new SelectorList();
        
        if (!m_currentContainerID.equals(RiExerciserConstants.ROOT_CONTAINER_ID))
        {
            m_remoteServiceList.setFirstItemText("Return to " + m_parentContainerName);
        }
        
        // Initialize the list of remote service items from m_mediaServerIndex
        int numRemoteServices = m_oadHN.getNumServerContentItems(m_mediaServerIndex);
        final Vector remoteServices = new Vector();
        final Vector remoteServiceIndices = new Vector();
        for (int i = 0; i < numRemoteServices; i++)
        {
            remoteServices.add(m_oadHN.getServerContentItemInfo(m_mediaServerIndex, i));
            remoteServiceIndices.add(new Integer(i));
        }
        m_remoteServiceList.initialize(remoteServices);
        m_remoteServiceList.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_remoteServiceList.getSelectedItemIndex();
                
                /** 
                 * Set m_remoteIndex to the index of the selected item, then
                 * play the selected content item
                 */
                if (selectedIndex != -1)
                {
                    if (m_oadHN.isContentContainer(m_mediaServerIndex, selectedIndex))
                    {
                        if (updateCurrentContainer(selectedIndex, m_remoteServiceList))
                        {
                            displayRemoteServices();
                        }
                    }
                    
                    else
                    {
                        m_contentIndex = ((Integer)remoteServiceIndices.get(selectedIndex)).intValue();
                        // Remove and disable the list after an item has been selected
                        disableAndRemoveList(m_remoteServiceList);
                        
                        String remoteServiceString = remoteServices.get(selectedIndex).toString();
                        m_controller.displayMessage("Remote service selected: " + remoteServiceString);
                        // If selected Content Item is VPOP and local media server 
                        // is selected, display message and don't play video
                        if (m_mediaServerIndex == m_oadHN.findLocalMediaServer())
                        {
                            // TODO: Find better way to determine VPOP content. For
                            // now, check for View Primary Output Port in title
                            if (remoteServiceString.indexOf("View Primary Output Port") != -1)
                            {
                                m_controller.displayMessage("Unable to stream VPOP to self");
                                return;
                            }
                        }
                        playVideo(OcapAppDriverInterfaceCore.PLAYBACK_TYPE_SERVICE);
                    }
                }
                else if (selectedIndex == -1 && 
                        !(m_currentContainerID.equals
                                (RiExerciserConstants.ROOT_CONTAINER_ID)))
                {
                    returnToParentContainer(m_remoteServiceList);
                    displayRemoteServices();
                }
                else
                {
                    // Remove and disable the list after an item has been selected
                    disableAndRemoveList(m_remoteServiceList);
                }
            }
            
        });
        
        // Initialize the SelectorList
        enableAndInitializeList(m_remoteServiceList);
    }
    
        
    /**
     * Initiates playback of content item using supplied player type.
     * 
     * @param playerType    JMF or Server type player
     */
    private void playVideo(int playerType)
    {
        m_oadCore.serviceSelectStop(15);

        m_controller.setVideoMode(RiExerciserController.REMOTE_PLAYBACK_MODE);
        m_controller.displayNewPage(RiExerciserConstants.HN_PLAYBACK_PAGE);
        m_controller.displayMessage("Beginning " + HNPlayerMenuPage.getPlayerTypeStr(playerType) + " playback");
        m_controller.displayMessage("Creating playback for server index " + 
                m_mediaServerIndex + ", content index " + m_contentIndex);

        m_container.hide();
        m_controller.hidePage();
        m_container.repaintScene();

        if (log.isInfoEnabled()) 
        {
            log.info("playVideo - playerType: " + playerType + ", index: " + m_contentIndex);
        }
        if (!HNPlaybackPage.getInstance().playVideo(playerType, m_mediaServerIndex, m_contentIndex))
        {
            m_controller.displayMessage("Playback did not start - player type: " + playerType + ", index: " + m_contentIndex);

            refreshAfterPlayback();
        }        
    }
    
    private void enableAndInitializeList(SelectorList currentList)
    {
        currentList.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(currentList);
        currentList.setVisible(true);
        m_container.scenePopToFront(currentList);
        currentList.setEnabled(true);
        currentList.setFocusable(true);
        currentList.requestFocus();
    }
    
    private void disableAndRemoveList(SelectorList currentList)
    {
        currentList.setVisible(false);
        currentList.setEnabled(false);
        currentList.setFocusable(false);
        m_container.removeFromScene(currentList);

        m_container.requestSceneFocus();
        m_container.repaintScene();
    }
    
    private boolean updateCurrentContainer(int selectedIndex, SelectorList currentList)
    {
        m_containerIds.push(m_currentContainerID);
        m_currentContainerID = m_oadHN.getEntryID(m_mediaServerIndex, selectedIndex);
        if (m_currentContainerID != null)
        {
            m_parentContainerName = m_currentContainerName;
            m_containerNames.push(m_parentContainerName);
            m_currentContainerName = m_oadHN.getContainerName(m_mediaServerIndex, selectedIndex);
            
            currentList.setVisible(false);
            currentList.setEnabled(false);
            currentList.setFocusable(false);

            m_container.removeFromScene(currentList);
            m_container.requestFocus();
            m_container.requestSceneFocus();
            m_container.repaintScene();
            
            // Refresh the list with the contents of the selected container
            m_oadHN.refreshServerContentItems
               (m_mediaServerIndex, 
                m_currentContainerID, 
                m_hnActionTimeoutMS,
                m_browseContainer, false);
            return true;
        }
        else
        {
            m_controller.displayMessage("Null ID for entry: " + selectedIndex);
            m_contentItemList.setVisible(false);
            m_contentItemList.setEnabled(false);
            m_contentItemList.setFocusable(false);

            m_container.removeFromScene(currentList);
            m_container.requestFocus();
            m_container.requestSceneFocus();
            m_container.repaintScene();
            return false;
        }
    }
    
    private void returnToParentContainer(SelectorList currentList)
    {
        m_parentContainerID = (String)m_containerIds.pop();
        m_currentContainerID = m_parentContainerID;
        if (m_currentContainerID.equals(RiExerciserConstants.ROOT_CONTAINER_ID))
        {
            m_parentContainerName = null;
            m_currentContainerName = RiExerciserConstants.ROOT_CONTAINER_NAME;
        }
        else
        {
            m_currentContainerName = (String)m_containerNames.pop();
            m_parentContainerName = (String)m_containerNames.peek();
        }
        
        currentList.setVisible(false);
        currentList.setEnabled(false);
        currentList.setFocusable(false);

        m_container.removeFromScene(currentList);
        m_container.requestFocus();
        m_container.requestSceneFocus();
        m_container.repaintScene();
        // Refresh the list with the contents of the selected container
        m_oadHN.refreshServerContentItems
            (m_mediaServerIndex, 
            m_currentContainerID, 
            m_hnActionTimeoutMS,
            m_browseContainer, false);
    }
    
    /**
     * A method to set the media server index
     * @param index the index of the media server selected
     */
    public void setMediaServerIndex(int index)
    {
        m_mediaServerIndex = index;
        m_mediaServerSelected = m_oadHN.getMediaServerInfo(index);
        
        // Perform a search on this content server
        m_oadHN.refreshServerContentItems(m_mediaServerIndex, "0", m_hnActionTimeoutMS, false, false);
    }
    
    /**
     * Accessor method for the media server selected
     * @return a String representation of the media server selected, or None
     * if no media server has been selected
     */
    public String getSelectedMediaServer()
    {
        return m_mediaServerSelected;
    }
    
    public void destroy()
    {
        m_menuBox.setVisible(false);
    }

    public void init()
    {
        m_menuBox.setVisible(true);
        
    }
    
    /**
     * Perform necessary actions to return this page to the state it was
     * in prior to doing a remote playback.
     */
    public void refreshAfterPlayback()
    {
        if (log.isInfoEnabled())
        {
            log.info("refreshAfterPlayback() - called");
        }
        
        m_controller.displayNewPage(RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE);
        
        m_controller.setVideoMode(RiExerciserController.LIVE_TUNING_MODE);
        m_oadCore.playbackTransformVideo(0.5f, 0.5f, 0, 0);
        m_controller.tuneToCurrentIndex();
        
        m_menuBox.setVisible(true);
        m_page.setVisible(true);
        m_container.show();
        m_container.requestFocus();
        m_container.repaintScene();       
        
        this.repaint();
    }
    
    /**
     * method to get the index of the selected media server.
     * @return index of the selected media server
     */
    public int getMediaServerIndex()
    {
        return m_mediaServerIndex;
    }

}
