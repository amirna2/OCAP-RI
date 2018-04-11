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
package org.cablelabs.impl.ocap.resource;

import org.apache.log4j.Logger;
import org.cablelabs.impl.util.Arrays;
import org.davic.resources.ResourceProxy;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppID;
import org.ocap.resource.SharedResourceUsage;
import org.ocap.resource.ResourceUsage;

import java.util.Vector;


/**
 * Object of this class represents a collection of ResourceUsages that has
 * shared ownership of a resource.
 */
public class SharedResourceUsageImpl implements SharedResourceUsage
{

    public SharedResourceUsageImpl(ResourceUsage[] contextArray)
    {
        m_usages = new ResourceUsage[contextArray.length];
        System.arraycopy(contextArray, 0, m_usages, 0, contextArray.length);
        if (log.isInfoEnabled()) 
        {
            log.info("Constructed: " + this);
        }
    }

    public int getPriority()
    {
        AppsDatabase appDB = AppsDatabase.getAppsDatabase();
        int highestPri = 0;
        for (int i = 0; i < m_usages.length; i++)
        {
            if (m_usages[i].getAppID() != null)
            {
                AppAttributes attr = appDB.getAppAttributes(m_usages[i].getAppID());
                if (attr.getPriority() >= highestPri)
                {
                    highestPri = attr.getPriority();
                }
            }
        }            
              
        return highestPri;

    }
    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.SharedResourceUsage#getResourceUsages()
     */
    public ResourceUsage[] getResourceUsages()
    {
        return m_usages;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.dvr.SharedResourceUsage#getResourceUsages(org.davic.resources
     * .ResourceProxy)
     */
    public ResourceUsage[] getResourceUsages(ResourceProxy resource)
    {
        Vector resUsage = new Vector();
        for (int ii = 0; ii < this.m_usages.length; ii++)
        {
            final String[] names = this.m_usages[ii].getResourceNames();

            for (int jj = 0; jj < names.length; jj++)
            {
                ResourceProxy rp = this.m_usages[ii].getResource(names[jj]);
                if (resource.equals(rp) && !resUsage.contains(this.m_usages[ii]))
                {
                    resUsage.add(this.m_usages[ii]); // ResourceUsage
                }
            }
        }

        ResourceUsageImpl[] rUsage = new ResourceUsageImpl[resUsage.size()];
        resUsage.copyInto(rUsage);
        return rUsage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.resource.ResourceUsage#getAppID()
     */
    public AppID getAppID()
    {
        // return AppID of highest priority app that owns usage
        // or null if usages have no apps associated with them 
        AppsDatabase appDB = AppsDatabase.getAppsDatabase();
        AppID highestAppID = null;
        int highestPri = 0;
        for (int i = 0; i < m_usages.length; i++)
        {
            if (m_usages[i].getAppID() != null)
            {
                AppAttributes attr = appDB.getAppAttributes(m_usages[i].getAppID());
                if (attr.getPriority() >= highestPri)
                {
                    highestAppID = m_usages[i].getAppID();
                    highestPri = attr.getPriority();
                }
            }
        }            
              
        return highestAppID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.resource.ResourceUsage#getResourceNames()
     */
    public String[] getResourceNames()
    {

        Vector names = new Vector();
        for (int ii = 0; ii < this.m_usages.length; ii++)
        {
            String[] n = null;
            n = this.m_usages[ii].getResourceNames();
            for (int jj = 0; jj < n.length; jj++)
            {
                if (!names.contains(n[jj]))
                {
                    names.add(n[jj]);
                    if (log.isDebugEnabled())
                    {
                        log.debug("getResourceNames - adding name: " + n[jj]);
                    }
            }
        }
        }

        String[] resNames = new String[names.size()];
        names.copyInto(resNames);
        return resNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.resource.ResourceUsage#getResource(java.lang.String)
     */
    public ResourceProxy getResource(String resourceName)
    {

        // All of the proxies associated with a specific resource
        // are the same, so just return the first one that matches.
        ResourceProxy proxy = null;
        for (int ii = 0; ii < this.m_usages.length; ii++)
        {
            proxy = this.m_usages[ii].getResource(resourceName);
            if (null != proxy) return proxy;
        }
        return null;
    }

    public String toString() 
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SharedResourceUsage - resource usages: ");
        buffer.append(Arrays.toString(m_usages));
        buffer.append(" (");
        buffer.append(super.toString());
        buffer.append(")");
        return buffer.toString();
    }

    private ResourceUsage[] m_usages = null;

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SharedResourceUsageImpl.class);

}
