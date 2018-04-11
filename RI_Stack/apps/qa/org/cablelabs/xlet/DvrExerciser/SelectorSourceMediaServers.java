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

package org.cablelabs.xlet.DvrExerciser;

import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;

import java.util.Enumeration;
import java.util.Vector;

/**
 * 
  * Purpose: This class displays list of media servers which were found on
  * the network when this class was instantiated. 
 */
public class SelectorSourceMediaServers extends SelectorSource
{
    /**
     * Generate a list of items to be displayed based on the type supplied.
     * Put items into the supplied vector.
     * 
     * @param   list    put items into this vector
     */
    public void populateSelectorList(Vector list)
    {
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);
        if (serviceList.size() > 0)
        {
            System.out.println("SelectorSource.populateSelectorList() - initial list size: " +
                    serviceList.size());
            Enumeration enumerator = serviceList.getElements();
            while (enumerator.hasMoreElements())
            {
                Object obj = enumerator.nextElement();
                if (obj instanceof ContentServerNetModule)
                {
                    System.out.println("SelectorSource.populateSelectorList() - adding item");
                    list.add(obj);
                }
            }
        }
    }
    
    /**
     * Formulate string to be used for a specific item when list of items
     * is displayed in SelectorList
     * 
     * @param   list    list of data items to be displayed
     * @param   i       index of the item in list of items to formulate display
     *                  string 
     * @return  string to be used for displaying the item at the specified index
     * from the supplied list, null if problems are encountered                 
     */
    public String getSelectorItemDisplayStr(Vector list, int i)
    {
        ContentServerNetModule cds = (ContentServerNetModule)list.get(i);
        return cds.getDevice().getProperty(Device.PROP_FRIENDLY_NAME) + ", " + 
                        cds.getDevice().getInetAddress() + ", " + 
                        cds.getDevice().getProperty(Device.PROP_UDN);
    }

    /**
     * Formulate a special string to be displayed in DvrExerciser log message
     * area which reports that an item was selected.  If no message is required
     * for a specific type, it can return null.
     * 
     * @param item  item that was selected from list
     * @return  string to be displayed in DvrExerciser log message screen area.
     */
    public String getSelectorItemSelectedStr(Object item)
    {
        ContentServerNetModule mediaServer = (ContentServerNetModule)item;
        return "Current Media Server: " + mediaServer.getDevice().getProperty(Device.PROP_FRIENDLY_NAME) + 
                    " " + mediaServer.getDevice().getProperty(Device.PROP_UDN);
    }
    
    /**
     * Perform necessary actions since item has been selected from list.
     * 
     * @param item  item from list which was selected
     */
    public void itemSelected(Object item)
    {
        HNTest.m_mediaServer = (ContentServerNetModule)item;
    }
}
