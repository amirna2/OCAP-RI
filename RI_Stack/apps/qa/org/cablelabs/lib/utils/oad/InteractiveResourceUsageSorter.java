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

package org.cablelabs.lib.utils.oad;

import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.SimpleCondition;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.resource.ResourceUsage;
import org.ocap.resource.SharedResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;


/**
 * This class sorts ResourceUsage Objects from InteractiveResourceContentionHandler
 * and then returns the sorted list back to InteractiveResourceContentionHandler.
 */
public class InteractiveResourceUsageSorter
{
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(InteractiveResourceUsageSorter.class);

    /**
     * A HashMap containing String labels as keys with ResourceUsage Objects
     * as values
     */
    private HashMap m_rus;

    /**
     * A SimpleCondition that signals when the UI List of ResourceUsage items
     * has been reorganized and dismissed by the user. Note that the UI 
     * implementation is independent of OcapAppDriver (for an example, see
     * org.cablelabs.xlet.RiExerciser.ui.InteractiveResourceUsageList)
     */
    public SimpleCondition m_dialogDismissed;

    /**
     * Maintains a list of the String labels representing the ResourceUsage
     * Objects. This Vector should contain the same elements as m_rus.keySet(), 
     * but maintains them in a particular order and is used to modify the order
     * of ResourceUsage Objects as directed by the user.
     */
    private final Vector m_list;

    /**
     * A new request for ResourceUsage
     */
    private ResourceUsage m_newRequest;

    /**
     * An array of ResourceUsage Objects
     */
    private ResourceUsage [] m_currentReservations;

    /**
     * List of resource usages which are specific to extensions supported.
     */
    private Vector m_interactiveResourceUsages;
    
    /**
     * Constructor for InteractiveResourceUsageSorter. This class is meant
     * to be used as a member of InteractiveResourceContentionHandler.
     * @param newRequest a new ResourceUsage request
     * @param currentReservations an array of current ResourceUsage requests
     */
    public InteractiveResourceUsageSorter()
    {
        if (log.isInfoEnabled())
        {
            log.info("InteractiveResourceUsageSorter constructing...");
        }
        m_interactiveResourceUsages = new Vector();
        m_list = new Vector();
   }

    /**
     * This method builds the HashMap of String label representations of the
     * ResourceUsage objects and ResourceUsage Objects, and also populates
     * the list containing String representations of the ResourceUsage objects
     */
    public void init(ResourceUsage newRequest, ResourceUsage [] currentReservations )
    {
        if (log.isInfoEnabled())
        {
            log.info("InteractiveResourceUsageSorter.init()");
        }
        m_newRequest = newRequest;
        m_currentReservations = currentReservations;
        m_dialogDismissed = new SimpleCondition(false);
        m_rus = new HashMap(m_currentReservations.length + 1);
 
        String label = "NEW REQUEST: " + stringForUsage(m_newRequest);
        m_list.add(label);
        m_rus.put(label, m_newRequest);
        if (log.isInfoEnabled())
        {
            log.info("Added item: " + label);
        }

        for (int i = 0; i < m_currentReservations.length; i++)
        {
            label = "CURRENT USE " + (i+1) + ": " 
            + stringForUsage(m_currentReservations[i]);
            m_list.add(label);
            m_rus.put(label, m_currentReservations[i]);
            if (log.isInfoEnabled())
            {
                log.info("Added item: " + label);
            }
        }
    }
    
    /**
     * Adds the supplied resource usage to the list.  This allows for extension
     * specific support such as HN or DVR resource usages.
     * 
     * @param usage called to get resource usage string which is specific to an OCAP extension.
     */
    public void addInteractiveResourceUsage(InteractiveResourceUsage usage)
    {
        m_interactiveResourceUsages.add(usage);
    }

    /**
     * A method to access the number of ResourceUsage requests (both the new
     * request and the array of current requests)
     * @return the number of ResourceUsage requests
     */
    public int getNumReservations()
    {
        return m_list.size();
    }
    
    /**
     * This method clears the Vector of String representations of Resource Usage
     * Objects and also clears the HashMap mapping String representations to the
     * Resource Usage Objects. 
     */
    public void clearReservations()
    {
        m_list.clear();
        m_rus.clear();
    }

    /**
     * A method to access a String representation of the ResourceUsage Object
     * contained in m_list at the specified index
     * @param index the index of the ResourceUsage object
     * @return a String representation of the specified ResourceUsage object,
     * or null if index is out of bounds for the current list of ResourceUsage
     * Objects
     */
    public String getReservationString(int index)
    {
        if (index < m_list.size() && index >= 0)
        {
            return (String)m_list.get(index);
        }
        return null;
    }

    /**
     * A method to move the specified String representation of a ResourceUsage
     * Object to the bottom of the Vector m_list
     * @param index the index of the String representation of the ResourceUsage
     * Object to be moved to the bottom of the list
     * @return true if the item was successfully moved, false otherwise
     */
    public boolean moveResourceUsageToBottom(int index)
    {
        String ru = (String)m_list.remove(index);
        return m_list.add(ru);
    }

    /**
     * A method to wait until the UI list of ResourceUsage Objects has been
     * closed by the user
     */
    public void waitForDialogClose()
    {
        if (log.isInfoEnabled())
        {
            log.info("InteractiveResourceUsageSorter.waitForDialogClose()...");
        }
        try
        {
            m_dialogDismissed.waitUntilTrue();
        }
        catch (InterruptedException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("WARN: InteractiveResourceUsageSorter.waitForDialogClose() INTERRRUPTED: " + e.getMessage());
            }
        }
    }

    /**
     * Returns a sorted list of ResourceUsage objects, as specified by the
     * user.
     * @return an array of ResourceUsage requests in the order of priority
     * as determined by the user
     */
    public ResourceUsage [] getPrioritizedUsages()
    {
        Object[] elements = m_list.toArray();

        ResourceUsage orderedList[] = new ResourceUsage[elements.length];

        // The label order dictates the RU order...
        for (int i = 0; i < orderedList.length; i++)
        {
            String curLabel = (String)elements[i];
            ResourceUsage ru = (ResourceUsage)m_rus.get(curLabel);
            orderedList[i] = ru;
        }

        return orderedList;
    }

    /**
     * A class to generated a String representation of a given ResourceUsage
     * Object.
     * @param ru the ResourceUsage Object for which to generate a String
     * @return a String representation of the given ResourceUsage Object, or
     * the result of the Objects toString() method if the ResourceUsage Object
     * is not one of the expected subtypes
     * 
     */
    public String stringForUsage(ResourceUsage ru)
    {
        // Determine if this is a core type usage
        if (ru instanceof ServiceContextResourceUsage)
        {
            ServiceContextResourceUsage scru = 
                (ServiceContextResourceUsage)ru;
            return "ServiceContext (service: " 
            + scru.getRequestedService().getLocator().toExternalForm()
            + ')';
        }
        
        if (ru instanceof ApplicationResourceUsage)
        {
            ApplicationResourceUsage aru = (ApplicationResourceUsage)ru;
            return "AppResourceUsage (appID " 
            + aru.getAppID().toString()
            + ')';
        }

        if (ru instanceof SharedResourceUsage)
        {
            SharedResourceUsage sharedRU = (SharedResourceUsage) ru;
            ResourceUsage ruarray [] = sharedRU.getResourceUsages();
            StringBuffer sruSB = new StringBuffer("SharedResourceUsage");
            sruSB.append(" (" + ruarray.length + " elements)\n");

            for (int i=0; i<ruarray.length; i++)
            {
                sruSB.append("  SUBUSE ")
                .append(i+1)
                .append(": ")
                .append(stringForUsage(ruarray[i]))
                .append('\n');
            }

            return sruSB.toString();
        }
        
        // Wasn't a core type usage, check to see if other ext based usages are registered
        // Try to find usage which may be based on ext support
        String usageStr = null;
        
        for (int i= 0; i < m_interactiveResourceUsages.size(); i++)
        {
            InteractiveResourceUsage iru = (InteractiveResourceUsage)m_interactiveResourceUsages.elementAt(i);
            usageStr = iru.stringForUsage(ru);
            if (usageStr != null)
            {
                return usageStr;
            }
        }

        // Last resort...
        return ru.toString();
    }
}


