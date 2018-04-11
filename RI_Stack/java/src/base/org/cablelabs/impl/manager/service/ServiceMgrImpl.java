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

package org.cablelabs.impl.manager.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.protocol.DataSource;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceContextFactoryExt;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.javatv.selection.ServiceContextFactoryImpl;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * The <code>ServiceManager</code> implementation. If the property named
 * OCAP.servicemgr.siDatabase is defined, then its value names the SIDatabase
 * implementation to be used.
 * 
 * @author Todd Earles
 */
public class ServiceMgrImpl implements ServiceManager
{
    private static final Logger log = Logger.getLogger(ServiceMgrImpl.class);

    /**
     * The singleton instance of the service context factory
     * 
     * @supplierCardinality 1
     */
    private ServiceContextFactoryExt serviceContextFactory = new ServiceContextFactoryImpl();

    /**
     * The singleton instance of the SI cache
     * 
     * @supplierCardinality 1
     */
    private SICache siCache = null;

    /**
     * The singleton instance of the SI database
     * 
     * @supplierCardinality 1
     */
    private SIDatabase siDatabase = null;

    private final List serviceMgrDelegates;

    private static final String DELEGATE_PARAM_PREFIX = "OCAP.serviceMgrDelegate";

    // default wait time is 2.5 minutes
    private int siWaitTime;
    // SI async request timeout is 15 sec
    private int asyncRequestTimeout;
    
    /**
     * Construct this object.
     */
    public ServiceMgrImpl()
    {
        // Do not perform any initialization here that indirectly relies on
        // the service manager because we have not finished constructing
        // the service manager yet.
        serviceMgrDelegates = getRegisteredServiceMgrDelegates();
    }

    /**
     * Returns an instance of this <code>ServiceManager</code>. Intended to be
     * called by the {@link org.cablelabs.impl.manager.ManagerManager
     * ManagerManager} only and not called directly. The singleton instance of
     * this manager is maintained by the <code>ManagerManager</code>.
     * 
     * @return an instance of the <code>ServiceManager</code>.
     * 
     * @see org.cablelabs.impl.manager.ManagerManager#getInstance(Class)
     */
    public static synchronized Manager getInstance()
    {
        try
        {
            return new ServiceMgrImpl();
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
            return null;
        }
    }

    // Description copied from ManagerManager
    public void destroy()
    {
        serviceContextFactory = null;
        if (siCache != null) siCache.destroy();
        siCache = null;
        siDatabase = null;
    }

    // Description copied from ServiceManager
    public ServiceContextFactory getServiceContextFactory()
    {
        return serviceContextFactory;
    }

    // Description copied from ServiceManager
    public SIManager createSIManager()
    {
        // Create a new instance each time
        return new SIManagerImpl();
    }

    // Description copied from ServiceManager
    public ServicesDatabase getServicesDatabase()
    {
        return ServicesDatabaseImpl.getInstance();
    }

    // Description copied from ServiceManager
    public synchronized SICache getSICache()
    {
        if (siCache == null) createCacheAndDatabase();

        return siCache;
    }

    // Description copied from ServiceManager
    public synchronized SIDatabase getSIDatabase()
    {
        if (siDatabase == null) createCacheAndDatabase();

        return siDatabase;
    }

    /**
     * Construct the SICache and the SIDatabase
     */
    public void createCacheAndDatabase()
    {
        // Instantiate the SICache
        siCache = new SICacheImpl();
        // Default 2.5 min
        siWaitTime = MPEEnv.getEnv("OCAP.siDatabase.siWaitTimeout", 150000);
        
        // Instantiate the SIDatabase
        String sidbName = MPEEnv.getEnv("OCAP.servicemgr.siDatabase");
        if (sidbName != null)
        {
            try
            {
                siDatabase = (SIDatabase) Class.forName(sidbName).newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Cannot instantiate " + sidbName);
            }
        }
        else
        {
            siDatabase = new SIDatabaseImpl();
        }

        // Point the SIDatabase and SICache at each other
        siDatabase.setSICache(siCache);
        siCache.setSIDatabase(siDatabase);
       
        
        asyncRequestTimeout = siCache.getRequestAsyncTimeout();
    }

    public int getOOBWaitTime()
    {
        return siWaitTime;
    }

    public int getRequestAsyncTimeout()
    {
        return asyncRequestTimeout;
    }
    
    // Description copied from ServiceManager
    public ServiceDataSource createServiceDataSource(ServiceContextExt sc, Service svc)
    {
        for (Iterator iter = serviceMgrDelegates.iterator(); iter.hasNext();)
        {
            ServiceMgrDelegate serviceMgrDelegate = (ServiceMgrDelegate) iter.next();
            if (log.isDebugEnabled())
            {
                log.debug("attempting to create a ServiceDataSource for service: " + svc + " with ServiceMgrDelegate " + serviceMgrDelegate);
            }

            ServiceDataSource result = serviceMgrDelegate.createServiceDataSource(sc, svc);
            if (result != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ServiceMgrDelegate " + serviceMgrDelegate + " created a ServiceDataSource: " + result);
                }
                return result;
            }
        }
        if (log.isWarnEnabled())
        {
            log.warn("unable to find a serviceMgrDelegate to create serviceDataSource for: " + sc + ", " + svc);
        }
        return null;
    }

    // Description copied from ServiceManager
    public ServiceMediaHandler createServiceMediaHandler(DataSource ds, ServiceContextExt sc, Object lock, ResourceUsageImpl resourceUsage)
    {
        if (ds == null || sc == null)
        {
            throw new IllegalArgumentException("null argument - dataSource: " + ds + ", serviceContext: " + sc);
        }
        for (Iterator iter = serviceMgrDelegates.iterator(); iter.hasNext();)
        {
            ServiceMgrDelegate serviceMgrDelegate = (ServiceMgrDelegate) iter.next();
            if (log.isDebugEnabled())
            {
                log.debug("attempting to create a ServiceMediaHandler for dataSource: " + ds + " with ServiceMgrDelegate " + serviceMgrDelegate);
            }
            ServiceMediaHandler result = serviceMgrDelegate.createServiceMediaHandler(ds, sc, lock, resourceUsage);
            if (result != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ServiceMgrDelegate " + serviceMgrDelegate + " created a ServiceMediaHandler: " + result);
                }
                return result;
            }
        }
        if (log.isWarnEnabled())
        {
            log.warn("unable to find a serviceMgrDelegate to create serviceMediaHandler for: " + ds + ", " + sc);
        }
        return null;
    }

    private List getRegisteredServiceMgrDelegates()
    {
        return new ArrayList(PropertiesManager.getInstance().getInstancesByPrecedence(DELEGATE_PARAM_PREFIX));
    }
}
