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

import java.awt.EventQueue;

import org.apache.log4j.Logger;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceUsage;

/**
 * Purpose: This class implements resource contention handler and provided support
 * for interactive resource contention handling.
*/
public class InteractiveResourceContentionHandler implements ResourceContentionHandler
{        
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(InteractiveResourceContentionHandler.class);
    
    private InteractiveResourceUsageSorter m_ruSorter;
    
    private boolean m_resourceContentionActive;

    public InteractiveResourceContentionHandler()
    {
        m_resourceContentionActive = false;        
    }
    
    public void setResourceUsageSorter(InteractiveResourceUsageSorter sorter)
    {
        m_ruSorter = sorter;
    }
    
    /**
     * {@inheritDoc}
     */
    public ResourceUsage[] resolveResourceContention(final ResourceUsage newRequest, final ResourceUsage[] currentReservations)
    {
        if (log.isInfoEnabled())
        {
            log.info("InteractiveResourceContentionHandler: Entered ResourceContentionHandler...");
        }
        
        if (m_ruSorter == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("WARN: InteractiveResourceContentionHandler: - NULL resource usage sorter");
            }                           
            return null;
        }
        
        try
        {
            EventQueue.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_ruSorter.init(newRequest, currentReservations);
                    m_resourceContentionActive = true;
                }
            });
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("WARN: InteractiveResourceContentionHandler: Error delegating to EventQueue: " + e.getMessage());
            }
        }
        

        m_ruSorter.waitForDialogClose();
        if (log.isInfoEnabled())
        {
            log.info("InteractiveResourceContentionHandler: Exiting ResourceContentionHandler...");
        }
        m_resourceContentionActive = false;
        ResourceUsage[] prioritizedUsages = m_ruSorter.getPrioritizedUsages();
        m_ruSorter.clearReservations();
        return prioritizedUsages;
    }

    /**
     * {@inheritDoc}
     */
    public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
    {
        // TODO Auto-generated method stub
    }
    
    public int getNumReservations()
    {
        if (m_ruSorter == null)
        {
            log.error("InteractiveResourceContentionHandler:getNumReservations() m_ruSorter is null");
            return -1;
        }
        return m_ruSorter.getNumReservations();
    }
    
    public String getReservationString(int index)
    {
        if (m_ruSorter == null)
        {
            log.error("InteractiveResourceContentionHandler: getReservationString() m_ruSorter is null");
            return null;
        }
        return m_ruSorter.getReservationString(index);
    }
    
    public boolean moveResourceUsageToBottom(int index)
    {
        if (m_ruSorter == null)
        {
            log.error("InteractiveResourceContentionHandler: moveResourceUsageToBottom() m_ruSorter is null");
            return false;
        }
        return m_ruSorter.moveResourceUsageToBottom(index);
    }
    
    public void setResourceContentionHandled()
    {
        if (m_ruSorter == null)
        {
            log.error("InteractiveResourceContentionHandler: setResourceContentionHandled() m_ruSorter is null");
            return;
        }
        m_ruSorter.m_dialogDismissed.setTrue();
    }    
    
    public boolean resourceContentionActive()
    {
        return m_resourceContentionActive;
    }
}
