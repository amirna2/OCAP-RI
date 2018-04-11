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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.cablelabs.xlet.RiExerciser.ui.QuarterPage;
import org.dvb.ui.DVBColor;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.ui.event.OCRcEvent;

public class HNContentTransformationRecordingPage extends QuarterPage 
{

    private static HNContentTransformationRecordingPage m_page = new HNContentTransformationRecordingPage();
    private RiExerciserController m_controller;
    private OcapAppDriverInterfaceHN m_oadHN;
    
    private int m_localCDSIndex;
    private int m_recordingIndex;
    private int m_transformIndex;
    private long m_localCDSTimeout;
    private long m_refreshItemsTimeout;
    
    private RiExerciserContainer m_container;
    
    private HashMap m_recordingItemMap;
    
    
    private SelectorList m_recordingList;
    private SelectorList m_transformationList;
    private SelectorList m_resourceList;
    private SelectorList m_currentList;
    
    private boolean m_listActive;
    
    private HNContentTransformationRecordingPage()
    {
        // Initialize the components
        setLayout(null);
        m_listActive = false;
        m_controller = RiExerciserController.getInstance();
        m_container = RiExerciserContainer.getInstance();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        m_localCDSIndex = -1;
        m_localCDSTimeout = m_controller.getLocalServerTimeoutSecs();
        m_refreshItemsTimeout = m_controller.getHNActionTimeoutMS();
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true); 
        add(m_menuBox);
        
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options. Only write options for which extensions are enabled
        m_menuBox.write("HN Content Transformation Recording Menu");
        m_menuBox.write("0. Return to HN Content Transformation Menu");
        m_menuBox.write("1. Add transformation to current recording");
        m_menuBox.write("2. Add transformation to selected recording");
        m_menuBox.write("3. Remove transformation from current recording");
        m_menuBox.write("4. Remove transformation from selected recording");
        m_menuBox.write("5. Remove resource from current recording");
        m_menuBox.write("6. Remove resource from selected recording");
    }
    
    public static HNContentTransformationRecordingPage getInstance()
    {
        return m_page;
    }
    
    public void init() 
    {

    }

    public void processUserEvent(KeyEvent event) 
    {
        switch(event.getKeyCode())
        {
            // Options 1-6 rely on the local media server, so find the media
            // server index if necessary
            case OCRcEvent.VK_1:
            case OCRcEvent.VK_2:
            case OCRcEvent.VK_3:
            case OCRcEvent.VK_4:
            case OCRcEvent.VK_5:
            case OCRcEvent.VK_6:
            {
                // If the local media server hasn't been discovered yet, wait
                // for it to be discovered
                if (m_localCDSIndex == -1)
                {
                    if (!waitForLocalCDS())
                    {
                        return;
                    }
                    else
                    {
                        m_localCDSIndex = m_oadHN.findLocalMediaServer();
                    }
                }
            }
        }
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
                // Menu Option 0: Return to HN Content Transformation Menu
                m_controller.displayNewPage(RiExerciserConstants.HN_CONTENT_TRANSFORMATION_MENU_PAGE);
                break;
            }
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: Add transformation to current recording
                m_recordingIndex = getCurrentRecordingIndex();
            	if (m_recordingIndex >= 0)
            	{
            	    selectTransformToAdd();
            	}
            	else
            	{
            	    m_controller.displayMessage("No Recording Content Items");
            	}
                break;
            }
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: Add transformation to selected recording
            	addTransformation();
                break;
            }
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: Remove transformation from current recording
                m_recordingIndex = getCurrentRecordingIndex();
                if (m_recordingIndex >= 0)
                {
                    selectTransformToRemove();
                }
                else
                {
                    m_controller.displayMessage("No Recording Content Items");
                }
                break;
            }
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Remove transformation from selected recording
                removeTransformation();
                break;
            }
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Remove resource from current recording
                m_recordingIndex = getCurrentRecordingIndex();
                if (m_recordingIndex >= 0)
                {
                    selectResourceToRemove();
                }
                else
                {
                    m_controller.displayMessage("No Recording Content Items");
                }
                break;
            }
            case OCRcEvent.VK_6:
            {
                // Menu Option 6: Remove resource from selected recording
                removeResource();
                break;
            }
            case OCRcEvent.VK_ENTER:
            {
                if (m_listActive)
                {
                    m_currentList.requestFocus();
                }
                break;
            }
        }
    }
    
    private Vector getRecordingContentItemsVector()
    {
        Vector recordingItems = new Vector();
        m_recordingItemMap = new HashMap();
        m_oadHN.refreshServerContentItems(m_localCDSIndex, RiExerciserConstants.ROOT_CONTAINER_ID,
                                            m_refreshItemsTimeout, false, true);
        int numContentItems = m_oadHN.getNumServerContentItems(m_localCDSIndex);
        int recordingItemIndex = 0;
        for (int i = 0; i < numContentItems; i++)
        {
            if (m_oadHN.isRecordingContentItem(m_localCDSIndex, i))
            {
                recordingItems.add(m_oadHN.getServerContentItemInfo(m_localCDSIndex, i));
                m_recordingItemMap.put(new Integer(recordingItemIndex), new Integer(i));
                recordingItemIndex++;
            }
        }
        return recordingItems;
    }
    
    private int getCurrentRecordingIndex()
    {
        m_recordingItemMap = new HashMap();
        m_oadHN.refreshServerContentItems(m_localCDSIndex, RiExerciserConstants.ROOT_CONTAINER_ID,
                                            m_refreshItemsTimeout, false, true);
        int numContentItems = m_oadHN.getNumServerContentItems(m_localCDSIndex);
        if (numContentItems == 0)
        {
            return -1;
        }
        int recordingItemIndex = 0;
        for (int i = 0; i < numContentItems; i++)
        {
            if (m_oadHN.isRecordingContentItem(m_localCDSIndex, i))
            {
                m_recordingItemMap.put(new Integer(recordingItemIndex), new Integer(i));
                recordingItemIndex++;
            }
        }
        Integer temp = (Integer)m_recordingItemMap.get(new Integer(recordingItemIndex -1));
        return temp.intValue();
    }
    
    private boolean waitForLocalCDS()
    {
        if (!m_oadHN.waitForLocalContentServerNetModule(m_localCDSTimeout))
        {
            m_controller.displayMessage("Unable to find local media server");
            return false;
        }
        return true;
    }
    
    /**
     * Displays the given SelectorList and passes the selection index to handleSelection()
     * @param list the SelectorList to display
     * @param action the action to take when a selection is made
     */
    private void showList(final SelectorList list, final int action)
    {
        m_currentList = list;
        m_listActive = true;
        list.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = list.getSelectedItemIndex();
                // Remove the Transformation from the list of default Transformations
                if (selectedIndex != -1)
                {
                    m_listActive = false;
                    list.setVisible(false);
                    list.setEnabled(false);
                    list.setFocusable(false);
                    m_container.removeFromScene(list);
                    handleSelection(action, selectedIndex);
                }
                // After selection has occurred, remove the list and disable item
                list.setVisible(false);
                list.setEnabled(false);
                list.setFocusable(false);
                
                m_container.removeFromScene(list);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initializing the SelectorList 
        list.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(list);
        list.setVisible(true);
        m_container.scenePopToFront(list);
        list.setEnabled(true);
        list.setFocusable(true);
        list.requestFocusInWindow();
    }
    
    private void handleSelection(int actionToTake, int selectionIndex)
    {
        switch(actionToTake)
        {
            case RiExerciserConstants.ADD_TRANSFORMATION:
            {
                Integer temp = (Integer)m_recordingItemMap.get(new Integer(selectionIndex));
                m_recordingIndex = temp.intValue();
                selectTransformToAdd();
                break;
            }
            case RiExerciserConstants.REMOVE_TRANSFORMATION:
            {
                Integer temp = (Integer)m_recordingItemMap.get(new Integer(selectionIndex));
                m_recordingIndex = temp.intValue();
                selectTransformToRemove();
                break;
            }
            case RiExerciserConstants.REMOVE_RESOURCE:
            {
                Integer temp = (Integer)m_recordingItemMap.get(new Integer(selectionIndex));
                m_recordingIndex = temp.intValue();
                selectResourceToRemove();
                break;
            }
            case RiExerciserConstants.SELECT_TRANSFORM_TO_ADD:
            {
                m_controller.displayMessage("Adding Transformation " + selectionIndex + 
                        " for RecordingContentItem " + m_recordingIndex);
                m_transformIndex = selectionIndex;
                m_oadHN.setTransformation(m_recordingIndex, m_transformIndex);
                break;
            }
            case RiExerciserConstants.SELECT_TRANSFORM_TO_REMOVE:
            {
                m_controller.displayMessage("Removing Transformation " + selectionIndex + 
                        " for RecordingContentItem " + m_recordingIndex);
                m_transformIndex = selectionIndex;
                m_oadHN.removeTransformation(m_recordingIndex, m_transformIndex);
                break;
            }
            case RiExerciserConstants.SELECT_RESOURCE_TO_REMOVE:
            {
                m_controller.displayMessage("Deleting ContentResource " + selectionIndex + 
                        " for RecordingContentItem " + m_recordingIndex);
                if(m_oadHN.deleteContentResource(m_recordingIndex, selectionIndex))
                {
                    m_controller.displayMessage("Content Resource deleted");
                }
                else
                {
                    m_controller.displayMessage("Failed to delete Content Resource");
                }
                break;
            }
        }
    }
    
    private void addTransformation()
    {
        // Initialize the Vector of Recording Content Items
        final Vector recordingItems = getRecordingContentItemsVector();
        m_recordingList = new SelectorList();
        m_recordingList.initialize(recordingItems);
        showList(m_recordingList, RiExerciserConstants.ADD_TRANSFORMATION);
    }
    
    private void removeTransformation()
    {
        // Initialize the Vector of Recording Content Items
        final Vector recordingItems = getRecordingContentItemsVector();
        m_recordingList = new SelectorList();
        m_recordingList.initialize(recordingItems);
        showList(m_recordingList, RiExerciserConstants.REMOVE_TRANSFORMATION);
    }
    
    private void removeResource()
    {
        // Initialize the Vector of Recording Content Items
        final Vector recordingItems = getRecordingContentItemsVector();
        m_recordingList = new SelectorList();
        m_recordingList.initialize(recordingItems);
        showList(m_recordingList, RiExerciserConstants.REMOVE_RESOURCE);
    }
    
    private void selectTransformToAdd()
    {
        // Initialize the Vector of supported Transformation strings
        final Vector supportedTransformations = new Vector();
        m_transformationList = new SelectorList();
        String[] supportedTransformationStrings = m_oadHN.getSupportedTransformations();
        supportedTransformations.addAll(Arrays.asList(supportedTransformationStrings));
        m_transformationList.initialize(supportedTransformations);
        showList(m_transformationList, RiExerciserConstants.SELECT_TRANSFORM_TO_ADD);


    }
    
    private void selectTransformToRemove()
    {
        // Initialize the Vector of supported Transformation strings
        final Vector supportedTransformations = new Vector();
        m_transformationList = new SelectorList();
        String[] contentItemTransformations = m_oadHN.getTransformations(m_recordingIndex);
        supportedTransformations.addAll(Arrays.asList(contentItemTransformations));
        m_transformationList.initialize(supportedTransformations);
        showList(m_transformationList, RiExerciserConstants.SELECT_TRANSFORM_TO_REMOVE);
    }
    
    private void selectResourceToRemove()
    {
        // Initialize the Vector of supported Transformation strings
        final Vector contentResources = new Vector();
        m_resourceList = new SelectorList();
        String[] resources = m_oadHN.getContentResourceStrings(m_recordingIndex);
        contentResources.addAll(Arrays.asList(resources));
        m_resourceList.initialize(contentResources);
        showList(m_resourceList, RiExerciserConstants.SELECT_RESOURCE_TO_REMOVE);
    }
    
    public void destroy() 
    {
        
    }
}
