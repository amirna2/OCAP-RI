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
import java.util.Vector;

import org.apache.log4j.Logger;
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


public class HNContentTransformationMenuPage extends QuarterPage 
{

    private static HNContentTransformationMenuPage m_page = new HNContentTransformationMenuPage();
    private RiExerciserController m_controller;
    private RiExerciserContainer m_container;
    private OcapAppDriverInterfaceHN m_oadHN;
    
    private boolean m_transformationEventsActive;
    
    private SelectorList m_defaultTransforms;

    private HNContentTransformationMenuPage()
    {
        // Initialize the components
        setLayout(null);
        m_controller = RiExerciserController.getInstance();
        m_container = RiExerciserContainer.getInstance();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        m_transformationEventsActive = false;
        setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_menuBox = new VidTextBox(RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 0, RiExerciserConstants.QUARTER_PAGE_WIDTH/2, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2+50, 14, 5000);
        m_menuBox.setVisible(true); 
        add(m_menuBox);
        
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        // Write the menu options. Only write options for which extensions are enabled
        writeMenuOptions();
    }
    
    public static HNContentTransformationMenuPage getInstance()
    {
        return m_page;
    }
    
    private void writeMenuOptions()
    {
        m_menuBox.reset();
        m_menuBox.write("HN Content Transformation Menu");
        m_menuBox.write("0. Return to HN Specific Test Menu");
        m_menuBox.write("1. View Supported Transformations");        
        m_menuBox.write("2. Add Default Transformation");
        m_menuBox.write("3. Remove Default Transformation");
        m_menuBox.write("4. Apply Default Transformations to all content");
        if (m_controller.isDvrEnabled())
        {
            m_menuBox.write("5. Transform Recordings Page");
        }
        m_menuBox.write("6. Transform Channels Page");
        
        String transformationEventString = m_transformationEventsActive ? "Deactivate" : "Activate";
        
        m_menuBox.write("7. " + transformationEventString + " TransformationListener");
    }
    
    public void init() 
    {
        // TODO Auto-generated method stub

    }

    public void processUserEvent(KeyEvent event) 
    {
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_0:
            {
                m_controller.displayNewPage(RiExerciserConstants.HN_TEST_PAGE);
                break;
            }
            
            case OCRcEvent.VK_1:
            {
                // Menu Option 1: View Supported Transformations
                String[] supportedTransformations = m_oadHN.getSupportedTransformations();
                m_controller.displayMessage("Supported Transformations: ");
                for (int i = 0; i < supportedTransformations.length; i++)
                {
                    m_controller.displayMessage(supportedTransformations[i]);
                }
                break;
            }
            
            case OCRcEvent.VK_2:
            {
                // Menu Option 2: ADD Default Transformation
                addDefaultTransformation();
                break;
            }
            
            case OCRcEvent.VK_3:
            {
                // Menu Option 3: REMOVE default Transformation
                removeDefaultTransformation();
                break;
            }
            
            case OCRcEvent.VK_4:
            {
                // Menu Option 4: Apply Default Transformations to all content
                m_controller.displayMessage("Applying default transformations to all content");
                m_oadHN.setDefaultTransformations();
                break;
            }
            
            case OCRcEvent.VK_5:
            {
                // Menu Option 5: Transform Recordings Page
                if (m_controller.isDvrEnabled())
                {
                    m_controller.displayNewPage(RiExerciserConstants.HN_CONTENT_TRANSFORMATION_RECORDING_PAGE);
                }
                break;
            }
            case OCRcEvent.VK_6:
            {
                // Menu Option 6: Transform Channels Page
                m_controller.displayNewPage(RiExerciserConstants.HN_CONTENT_TRANSFORMATION_CHANNEL_PAGE);
                break;
            }
            case OCRcEvent.VK_7:
            {
                // Menu Option 7: Activate/Deactivate TransformationEvent listener
                m_transformationEventsActive = !m_transformationEventsActive;
                if (m_transformationEventsActive)
                {
                    m_controller.displayMessage("Activating TransformationListener");
                }
                else
                {
                    m_controller.displayMessage("Deactivating TransformationListener");
                }
                m_controller.setTransformationEventsActive(m_transformationEventsActive);
                m_oadHN.listenForTransformationEvents(m_transformationEventsActive);
                writeMenuOptions();
                break;
            }
        }
    }
    
    private void addDefaultTransformation()
    {
        // Initialize the Vector of supported Transformation strings
        final Vector supportedTransformations = new Vector();
        m_defaultTransforms = new SelectorList();
        String[] supportedTransforms = m_oadHN.getSupportedTransformations();
        supportedTransformations.addAll(Arrays.asList(supportedTransforms));
        m_defaultTransforms.initialize(supportedTransformations);
        m_defaultTransforms.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_defaultTransforms.getSelectedItemIndex();
                // Add the Transformation to the list of supported Transformations
                if (selectedIndex != -1)
                {
                    m_oadHN.addDefaultTransformation(selectedIndex);
                    m_controller.displayMessage("Added Transformation " + 
                    supportedTransformations.get(selectedIndex) + " to the list of" + 
                    " default Transformations.");
                }
                // After selection has occurred, remove the list and disable item
                m_defaultTransforms.setVisible(false);
                m_defaultTransforms.setEnabled(false);
                
                m_container.removeFromScene(m_defaultTransforms);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initializing the SelectorList 
        m_defaultTransforms.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(m_defaultTransforms);
        m_defaultTransforms.setVisible(true);
        m_container.scenePopToFront(m_defaultTransforms);
        m_defaultTransforms.setEnabled(true);
        m_defaultTransforms.setFocusable(true);
        m_defaultTransforms.requestFocus();
    }
    
    private void removeDefaultTransformation()
    {
        // Initialize the Vector of supported Transformation strings
        final Vector defaultTransformations = new Vector();
        m_defaultTransforms = new SelectorList();
        String[] defaultTransforms = m_oadHN.getDefaultTransformations();
        defaultTransformations.addAll(Arrays.asList(defaultTransforms));
        m_defaultTransforms.initialize(defaultTransformations);
        m_defaultTransforms.addItemListener(new HItemListener()
        {

            public void currentItemChanged(HItemEvent e)
            {
                
            }

            public void selectionChanged(HItemEvent e)
            {
                int selectedIndex = m_defaultTransforms.getSelectedItemIndex();
                // Remove the Transformation from the list of default Transformations
                if (selectedIndex != -1)
                {
                    m_oadHN.removeDefaultTransformation(selectedIndex);
                    m_controller.displayMessage("Removed Transformation " + 
                    defaultTransformations.get(selectedIndex) + " from the list of" + 
                    " default Transformations.");
                }
                // After selection has occurred, remove the list and disable item
                m_defaultTransforms.setVisible(false);
                m_defaultTransforms.setEnabled(false);
                
                m_container.removeFromScene(m_defaultTransforms);

                m_container.requestSceneFocus();
                m_container.repaintScene();
            }
            
        });
        
        // Initializing the SelectorList 
        m_defaultTransforms.setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        m_container.addToScene(m_defaultTransforms);
        m_defaultTransforms.setVisible(true);
        m_container.scenePopToFront(m_defaultTransforms);
        m_defaultTransforms.setEnabled(true);
        m_defaultTransforms.setFocusable(true);
        m_defaultTransforms.requestFocus();
    }
    
    public void destroy() 
    {
        // TODO Auto-generated method stub

    }

}
