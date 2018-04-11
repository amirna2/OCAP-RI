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

package org.cablelabs.lib.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;

/**
 * Manages a list of 'live' services available for presentation. A vector
 * contains the services available for presentation, while a specific service is
 * selected as the 'current' service. Usage notes: 1. Create an instance of this
 * class. 2. Initiate the list of services by calling
 * <code>buildChannelVector()</code>. This will set the 'current' service to the
 * first service in the vector. 3. Call <code>getNextService()</code>,
 * <code>getCurrentService()</code>, or <code>getPreviousService()</code> to
 * obtain a service from the list of services.
 * 
 * This class was derived from the TuneTest application.
 * 
 * 
 * @author andy
 * 
 */

public class LiveServiceManager
{
    // contains all available services
    private Vector m_serviceList;
    private Vector m_channelName;
    
    public String UNKNOWN = "Unknown";
    
    private static final Logger log =
        Logger.getLogger(LiveServiceManager.class);
    
    // index of the 'current' service.
    private int m_iIndex = 0;
    
    // Default constructor to initialize Vectors, in order to eliminate 
    // NullPointerExceptions or null checks
    public LiveServiceManager()
    {
        m_serviceList = new Vector();
        m_channelName = new Vector();
    }
    
    /**
     * Gets the next service (numerically) from the list of services. This
     * method rolls over to the first service when the end of the service list
     * is reached.
     * 
     * @return the next service from the service list
     */
    public Service getNextService()
    {
        m_iIndex++;
        if (m_serviceList.size() <= m_iIndex)
        {
            m_iIndex = 0;
        }
        return getCurrentService();
    }

    /**
     * Gets the previous service (numerically) from the list of services. This
     * method rolls over to the last service when the beginning of the service
     * list is reached.
     * 
     * @return the previous service from the service list.
     */
    public Service getPreviousService()
    {
        m_iIndex--;
        if (0 > m_iIndex)
        {
            m_iIndex = m_serviceList.size() - 1;
        }
        return getCurrentService();
    }

    /**
     * Gets the 'current' service from the list of services.
     * 
     * @return the current service, <code>null</code> if unavailable.
     */
    public Service getCurrentService()
    {
        Service retVal = null;

        retVal = (Service) m_serviceList.get(m_iIndex);

        return retVal;
    }

    /**
     * Returns list of all services currently available
     * 
     * @param serviceList
     */
    public Vector getServiceList()
    {
        return (Vector)m_serviceList.clone();
    }

    /**
     * Returns list of all channel names currently available
     * 
     * @param channelName
     */
    public Vector getChannelName()
    {
        if (m_channelName == null) 
        { 
            return null; 
        }
        return (Vector)m_channelName.clone();
    }
    
    /**
     * Set the Service index
     * @param index the new value of the Service index
     * @return true if the index was set successfully, false otherwise
     */
    public boolean setServiceIndex(int index)
    {
        if (index >= 0 && index < m_serviceList.size())
        {
            m_iIndex = index;
            return true;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("serServiceIndex() - index: " + index + " is out of bounds");
            }
            return false;
        }
    }
    /**
     * Builds the list of available services from a configuration file or by use
     * of SI. Usage notes:
     * 
     * 
     * @param useJavaTVChannelMap
     *            set to <code>true</code> to build the list of services based
     *            on SI, <code>false</code> to build a the list of services from
     *            a configuration file.
     * 
     * @param channelFileName
     *            name of file containing a list of services.
     * @return <code>true</code> if at least one service has been found,
     *         <code>false</code> otherwise.
     */
    public boolean buildChannelVector(boolean useJavaTVChannelMap, String channelFileName)
    {
        if (true == useJavaTVChannelMap)
        {
            return buildChannelMapFromJavaTV();
        }
        else
        {
            return buildChannelMapFromLocators(channelFileName);
        }
    }

    /**
     * Builds a list of services using information contained in a configuration
     * file.
     * 
     * @param channelFileName
     *            name of file containing service list information.
     * 
     * @return <code>true</code> if at least on service was read from the config
     *         file, <code>false</code> if no services were read.
     */
    private boolean buildChannelMapFromLocators(String channelFileName)
    {
        // Create our ChanProperties object based on the channel file
        ChanProperties cp = null;
        try
        {
            cp = new ChanProperties(channelFileName);
        }
        catch (FileNotFoundException e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Channel file not found");
                log.info(e);
            }
            return false;
        }
        catch (IOException e)
        {
            if (log.isInfoEnabled())
            {
                log.info(e);
            }
            return false;
        }

        // Retrieve a list of OcapLocators built from the channel map file
        Vector locators = cp.buildChannelMap();
        m_serviceList = new Vector(locators.size());
        m_channelName = cp.getChannelNames();

        if (locators.size() == 0) return false;

        // SIManager will provide us access to services
        SIManager siManager = SIManager.createInstance();
        siManager.setPreferredLanguage("eng");

        // Query the SIManager with each locator. Grab the service and add it to
        // our list if successful, otherwise log
        Enumeration e = locators.elements();
        while (e.hasMoreElements())
        {
            OcapLocator ol = (OcapLocator) (e.nextElement());
            try
            {
                m_serviceList.addElement(siManager.getService(ol));
            }
            catch (SecurityException e1)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Service specified in channel map is unavailable (Security) -- " + ol.toString());
                    log.info(e1);
                }
            }
            catch (InvalidLocatorException e1)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Service specified in channel map is unavailable (InvalidLocator) -- "
                            + ol.toString());
                    log.info(e1);
                }
            }
        }

        return (m_serviceList.size() > 0) ? true : false;
    }

    // Returns true if we have found at least one service, false otherwise
    private boolean buildChannelMapFromJavaTV()
    {
        // SIManager will provide us with our list of available services
        SIManager siManager = SIManager.createInstance();
        siManager.setPreferredLanguage("eng");

        // filter all abstract services.
        ServiceFilter broadcastSvcFilter = new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                if (service instanceof AbstractService)
                    return false;
                else
                    return true;
            }
        };
        ServiceList serviceList = siManager.filterServices(broadcastSvcFilter);

        // Allocate our service list data structure
        m_serviceList = new Vector(serviceList.size());

        // Populate our list from services returned by SIManager
        ServiceIterator sitter = serviceList.createServiceIterator();
        while (sitter.hasNext())
        {
            m_serviceList.addElement(sitter.nextService());
        }

        // Validate that we have a non-zero-length service list and print
        // out the list of services
        if (m_serviceList.size() > 0)
        {
            int i = 0;
            if (log.isInfoEnabled())
            {
                log.info("Discovered the following list of services:");
            }

            // Print a list of services to the log
            Enumeration e = m_serviceList.elements();
            while (e.hasMoreElements())
            {
                Service service = (Service) (e.nextElement());
                OcapLocator oLoc = ((OcapLocator) (service.getLocator()));
                if (log.isInfoEnabled())
                {
                    log.info("serviceIndex[" + i++ + "] " +
                                    service.getName() + ": " +
                                    oLoc.toString() + ", " +
                                    oLoc.getSourceID());
                }
            }

            return true;
        }
        else
            return false;
    }
}
