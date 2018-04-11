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
import java.awt.Font;
import java.util.Vector;

import org.havi.ui.HListElement;
import org.havi.ui.HListGroup;
import org.havi.ui.HVisible;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;

/**
 * Purpose of this class is to provide a list to display items in RiExerciser.
 */
public class SelectorList extends HListGroup implements HItemListener
{
    private static final long serialVersionUID = 1L;
    
    private Vector m_list;
    
    private String m_firstItemText;

    /**
     * Constructs an instance of SelectorList.
     */
    public SelectorList()
    {
        this.addItemListener(this);
        
        // establish some display characteristics
        setBackground(Color.white);
        setBackgroundMode(HVisible.BACKGROUND_FILL);
        setFont(new Font("Tiresias", Font.PLAIN, 14));
        setHorizontalAlignment(HALIGN_LEFT);
        m_firstItemText = "Close selection window (press select key to begin)";

        // only allow a single selection
        setMultiSelection(false);        
    }
    
    public void setFirstItemText(String text)
    {
        m_firstItemText = text;
    }
    
    /**
     * Initialize list using the supplied source for item to be displayed.
     * 
     * @param source    data to display in this list
     */
    public void initialize(Vector source)
    {
        reset();
        
        m_list = source;

        addItem(new HListElement(m_firstItemText), 0);
        
        // Add each item to list displayed
        for (int i = 0; i < m_list.size(); i++)
        {
            String displayStr = (String)m_list.get(i);
            addItem(new HListElement(displayStr), (i + 1));
        }
        
        setCurrentItem(0);
    }
    
    /**
     * Returns indication if there is currently a source associated with this list.
     * 
     * @return  true if this list has an associated source, false otherwise
     */
    public boolean hasSource()
    {
        boolean hasSource = false;
        if (m_list != null)
        {
            hasSource = true;
        }
        return hasSource;
    }

    /**
     * Obtain the item selected by the user.
     * 
     * @return the selected item, may be null, if no items are in list or if
     * user dismissed list by selecting item 0.
     */
    public int getSelectedItemIndex()
    {
        int index = -1;
        
        // get the selection index (there should only be one)
        int[] selection = getSelectionIndices();
        
        // if the user made a selection (note that selecting the item at
        // index = 0 dismisses the selection menu w/o making a selection)...
        if ((null != selection) && (0 != selection[0]))
        {
            // ...get the item corresponding to the selection
            index = selection[0] - 1;
        }
        return index;
    }
    
    /**
     * Initiates actions based on user selecting an item.
     * Resets the list after item selection has been processed.
     */
    public void itemSelected()
    {      
        reset();
    }
    
    /**
     * Performs the necessary actions to clear out this list including
     * setting source to null and removing all items from list.
     */
    private void reset()
    {
        if (m_list != null)
        {
            m_list.removeAllElements();
        }
        this.clearSelection();
        this.removeAllItems();        
    }

    /**
     * Called when the user makes a selection.
     * This method processes the user's selection by
     * getting the string from the underlying data source and
     * logging it in DvrExerciser display window.
     */
    public void selectionChanged(HItemEvent e)
    {
        setVisible(false);
        setEnabled(false);
    }

    // exists to satisfy HItemListener declaration
    public void currentItemChanged(HItemEvent e)
    {
    }
}
