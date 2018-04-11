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

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.havi.ui.HListElement;
import org.havi.ui.event.HItemEvent;

public class InteractiveResourceUsageList extends SelectorList
{
    private static final Logger log = Logger.getLogger(InteractiveResourceUsageList.class);
    
    private RiExerciserController m_controller;
    
    private OcapAppDriverInterfaceCore m_oadCore;
    
    private RiExerciserContainer m_container;
    
    public InteractiveResourceUsageList()
    {
        super();
        m_controller = RiExerciserController.getInstance();
        m_oadCore = OcapAppDriverCore.getOADCoreInterface();
    }
    
    public void show()
    {
        if (log.isInfoEnabled())
        {
            log.info("InteractiveResourceUsageSorter.show()");
        }
        
        m_container = RiExerciserContainer.getInstance();

        addItem(new HListElement("RESOURCE CONTENTION DETECTED"), 0);
        addItem(new HListElement("[Select a usage to move to the bottom - select here to use current order]"), 1);
        String label = null;
        int numReservations = m_oadCore.getNumReservations();
        for (int i = 0; i < numReservations; i++)
        {
            label = m_oadCore.getReservationString(i);
            addItem(new HListElement(label), i + 2);
            if (log.isInfoEnabled())
            {
                log.info("Added item: " + label);
            }
        }
        setCurrentItem(1);
        
        setBounds(10, 10, RiExerciserConstants.QUARTER_PAGE_WIDTH - 100, 
                RiExerciserConstants.QUARTER_PAGE_HEIGHT - 100);
        setScrollPosition(0);
        
        addItemListener(this);

        m_container.add(this);
        m_container.addToScene(this);

        // ... have it process input
        setVisible(true);
        m_container.popToFront(this);
        m_container.scenePopToFront(this);
        setEnabled(true);
        setFocusable(true);

        requestFocus();
    }

    public void currentItemChanged(HItemEvent e)
    {
        // TODO Auto-generated method stub

    }

    public void selectionChanged(HItemEvent e)
    {
        if (e.getID() == HItemEvent.ITEM_SELECTED)
        {
            int[] indices = getSelectionIndices();
            if (indices != null)
            {
                int selectionIndex = indices[0];
                if (selectionIndex == 0)
                { 
                    // Ignore - we're using item 0 as a "title" 
                    return;
                }
                    
                
                if (selectionIndex == 1)
                { 
                    // Index 1 indicates that we're done.
                    removeAllItems();
                    m_controller.setResourceContentionListActive(false);
                    setVisible(false);
                    setEnabled(false);
                    m_container.remove(this);
                    m_container.requestSceneFocus();
                    m_container.repaintScene();
                    m_oadCore.setResourceContentionHandled();
                    return;
                }
                
                    
                // Otherwise, we're just going to move things around...
                
                // The list contains two elements in addition to the number of
                // resource usages
                m_oadCore.moveResourceUsageToBottom(selectionIndex - 2);
                HListElement selectedElement = removeItem(selectionIndex);
                
                if (log.isInfoEnabled())
                {
                    log.info("InteractiveResourceUsageList.selectionChanged: Removed item " 
                        + selectedElement ); 
                    log.info("InteractiveResourceUsageList.selectionChanged: Moving item #" 
                            + selectionIndex + " (" + selectedElement.getLabel() 
                            + ") to the bottom..." );
                }
                addItem(selectedElement, getNumItems());
                setCurrentItem(1);
                repaint();
                m_container.repaintScene();
            }
        }
    }

}
