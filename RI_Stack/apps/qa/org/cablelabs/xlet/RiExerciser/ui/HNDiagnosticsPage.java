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
import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;

import org.cablelabs.lib.utils.VidTextBox;
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
 * This Page displays options for HN Diagnostics, such as listing devices on the
 * network, sending ssdp messages, and more.
 *
 */
public class HNDiagnosticsPage extends QuarterPage
{
    private static HNDiagnosticsPage m_page = new HNDiagnosticsPage();
    
    /**
     * An instance of RiExerciserController for loading new pages and accessing
     * the instance of OcapAppDriverHN
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverHN from RiExerciserController for HN functionality
     */
    private OcapAppDriverInterfaceHN m_oadHN;
    
    /**
     * A list of devices on the network
     */
    private SelectorList m_devicesList;
    
    /**
     * A list of network interfaces
     */
    private SelectorList m_networkInterfacesList;
    
    /**
     * A list of media servers that support live streaming
     */
    private SelectorList m_liveStreamingMediaServerList;
    
    /**
     * The index of the selected device on the network
     */
    private int m_deviceIndex;
    
    /**
     * The index of the selected network interface
     */
    private int m_networkInterfaceIndex;
    
    /**
     * The index of the selected live streaming media server
     */
    private int m_liveStreamingMediaServerIndex;
    
    RiExerciserContainer m_container;
    
    private HNDiagnosticsPage()
    {
        m_deviceIndex = -1;
        m_networkInterfaceIndex = -1;
        m_liveStreamingMediaServerIndex = -1;
        
        // Initialize components
        m_controller = RiExerciserController.getInstance();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
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
        m_menuBox.write("HN Diagnostics Options:");
        m_menuBox.write("0. Return to HN General Menu");
        m_menuBox.write("1. Display List of Devices on Network");
        m_menuBox.write("2. Display Last 10 Events Received");
        m_menuBox.write("3. Enable/Disable Listen for Events from NetManager");
        m_menuBox.write("4. Enable/Disable Listen for Event from Selected Device");
        m_menuBox.write("5. Retrieve Network Interface Info");
        m_menuBox.write("6. Display Media Servers that support Live Streaming");
        m_menuBox.write("7. Send ByeBye Message");
        m_menuBox.write("8. Send Alive Message");
        m_menuBox.write("INFO. Media Server Info");
        setLayout(null);
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox.repaint();
        this.repaint();
    }
    
    public static HNDiagnosticsPage getInstance()
    {
        return m_page;
    }
    
    /**
     * This method handles KeyEvents sent from the RiExerciserController
     */
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
                // Menu Option 1: Display list of devices on network
                displayDevices();
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Display last 10 events received
                m_controller.displayLastTenEvents();
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Listen for events from NetManager
                m_oadHN.listenForAllDeviceEvents();
                m_controller.displayMessage("Registered w/ NetMgr for device events");
                m_controller.toggleListenForEvents();
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Listen for Event from Selected Device
                if (m_deviceIndex == -1)
                {
                    m_controller.displayMessage("No device selected");
                }
                else
                {
                    m_oadHN.listenForDeviceEvents(m_deviceIndex);
                    String deviceString = m_oadHN.getDeviceInfo(m_deviceIndex);
                    m_controller.displayMessage("Listening for events from Device: " + deviceString);
                    m_controller.toggleListenForEvents();
                }
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Retrieve network interface info
                displayNetworkInterfaces();
                break;
            }
            case OCRcEvent.VK_6:
            {
                // Menu Option 6: Display media servers that support live streaming
                displayLiveStreamingMediaServers();
                break;
            }
            case OCRcEvent.VK_7:
            {
                // Menu Option 7: Send ByeBye message
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
            case OCRcEvent.VK_8:
            {
                // Menu Option 8: Send Alive message
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
            case OCRcEvent.VK_INFO:
            {
                // Menu Option 1: Display list of devices on network
                displayMediaServerControlPoint();
                break;
            }
        }
    }
    
    /**
     * Displays a list of the devices on this network. This implements menu
     * option 1 for this Page
     */
    private void displayDevices()
    {
        m_devicesList = new SelectorList();
        final Vector devices = new Vector();
        int numDevices = m_oadHN.getNumDevicesOnNetwork();
        for (int i = 0; i < numDevices; i++)
        {
            devices.add(m_oadHN.getDeviceInfo(i));
        }
        m_devicesList.initialize(devices);
        
        m_devicesList.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_devicesList.getSelectedItemIndex();
                
                /** 
                 * Set m_deviceIndex to the index of the selected item
                 */
                if (selectedIndex != -1)
                {
                    m_deviceIndex = m_devicesList.getSelectedItemIndex();
                    m_controller.displayMessage("Device selected: " + devices.get(m_deviceIndex));
                }
                
                // Remove and disable the list after an item has been selected
                m_devicesList.setVisible(false);
                m_devicesList.setEnabled(false);

                m_container.removeFromScene(m_devicesList);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initialize the SelectorList
        m_devicesList.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(m_devicesList);
        m_devicesList.setVisible(true);
        m_container.scenePopToFront(m_devicesList);
        m_devicesList.setEnabled(true);
        m_devicesList.setFocusable(true);
        m_devicesList.requestFocus();
        
    }
    
    /**
     * Displays a list of network interfaces on this network. This implements
     * menu option 5
     */
    private void displayNetworkInterfaces()
    {
        m_networkInterfacesList = new SelectorList();
        int numNetworkInterfaces = m_oadHN.getNumNetworkInterfaces();
        final Vector networkInterfaces = new Vector();
        for (int i = 0; i < numNetworkInterfaces; i++)
        {
            networkInterfaces.add(m_oadHN.getNetworkInterfaceInfo(i));
        }
        m_networkInterfacesList.initialize(networkInterfaces);
        
        m_networkInterfacesList.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_networkInterfacesList.getSelectedItemIndex();
                
                /** 
                 * Set m_networkInterfaceIndex to the index of the selected item
                 */
                if (selectedIndex != -1)
                {
                    m_networkInterfaceIndex = m_networkInterfacesList.getSelectedItemIndex();
                    m_controller.displayMessage("Network interface selected: " + networkInterfaces.get(m_networkInterfaceIndex));
                }
                
                // Remove and disable the list after an item has been selected
                m_networkInterfacesList.setVisible(false);
                m_networkInterfacesList.setEnabled(false);

                m_container.removeFromScene(m_networkInterfacesList);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initialize the SelectorList
        m_networkInterfacesList.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(m_networkInterfacesList);
        m_networkInterfacesList.setVisible(true);
        m_container.scenePopToFront(m_networkInterfacesList);
        m_networkInterfacesList.setEnabled(true);
        m_networkInterfacesList.setFocusable(true);
        m_networkInterfacesList.requestFocus();
    }
    
    
    /**
     * Displays a list of media servers that support live streaming. This implements
     * menu option 6
     */
    private void displayLiveStreamingMediaServers()
    {
        m_liveStreamingMediaServerList = new SelectorList();
        m_controller.displayMessage("Getting list of live streaming media servers");
        int numLiveStreamingMediaServers = m_oadHN.getUpnpNumLiveStreamingMediaServers();
        m_controller.displayMessage("Number of live streaming media servers: " + 
                numLiveStreamingMediaServers);
        final Vector liveStreamingMediaServers = new Vector();
        for (int i = 0; i < numLiveStreamingMediaServers; i++)
        {
            liveStreamingMediaServers.add(m_oadHN.getUpnpLiveStreamingMediaServerInfo(i));
        }
        
        m_liveStreamingMediaServerList.initialize(liveStreamingMediaServers);
        
        m_liveStreamingMediaServerList.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_liveStreamingMediaServerList.getSelectedItemIndex();
                
                /** 
                 * Set m_liveStreamingMediaServerIndex to the index of the selected item
                 */
                if (selectedIndex != -1)
                {
                    m_liveStreamingMediaServerIndex = m_liveStreamingMediaServerList.getSelectedItemIndex();
                    m_controller.displayMessage("Media Server selected: " + liveStreamingMediaServers.get(m_liveStreamingMediaServerIndex));
                }
                
                // Remove and disable the list after an item has been selected
                m_liveStreamingMediaServerList.setVisible(false);
                m_liveStreamingMediaServerList.setEnabled(false);

                m_container.removeFromScene(m_liveStreamingMediaServerList);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initialize the SelectorList
        m_liveStreamingMediaServerList.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(m_liveStreamingMediaServerList);
        m_liveStreamingMediaServerList.setVisible(true);
        m_container.scenePopToFront(m_liveStreamingMediaServerList);
        m_liveStreamingMediaServerList.setEnabled(true);
        m_liveStreamingMediaServerList.setFocusable(true);
        m_liveStreamingMediaServerList.requestFocus();
    }
    /**
     * Displays information on the Media Server and Control point. This implements menu
     * option INFO for this Page
     */
    public void displayMediaServerControlPoint() {	
        ArrayList iPAddressesMediaServer;
        iPAddressesMediaServer = m_oadHN.getIPAddressesMediaServer();   	
        m_controller.displayMessage("IP addresses the Media Server is listening on");
        for (int i=0; i< iPAddressesMediaServer.size(); i++)
        {
            m_controller.displayMessage("   " + iPAddressesMediaServer.get(i));
        }
        
        ArrayList iPAddressesControlPoint;
        iPAddressesControlPoint = m_oadHN.getIPAddressesControlPoint(); 
        m_controller.displayMessage("IP addresses the Control Point is listening on");
        for (int i=0; i< iPAddressesControlPoint.size(); i++)
        {
            m_controller.displayMessage("   " + iPAddressesControlPoint.get(i));
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
}
