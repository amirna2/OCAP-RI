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

import javax.media.MediaLocator;
import javax.media.protocol.DataSource;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceMediaHandler;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.player.CannedBroadcastPlayer;
import org.cablelabs.impl.media.source.CannedOcapServiceDataSource;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceContextFactoryExt;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.javatv.selection.ServiceContextFactoryImpl;
import org.cablelabs.impl.util.MPEEnv;

/**
 * An implementation of the <code>ServiceManager</code> with canned service
 * information (SI) and canned behavior. This allows canned testing of most of
 * JavaTV without the need for real SI. This <code>ServiceManager</code>
 * instantiates a canned version of the <code>SIDatabase</code> and
 * <code>ServiceMediaHandler</code>. This allows test code to control event
 * generation and state changes in portions of the implementation being tested.
 * This allows testing that would otherwise not be possible or would be very
 * difficult due to timing variances or the inability of the real platform to
 * generate failure conditions on demand.
 * 
 * @author Todd Earles
 */
public class CannedServiceMgr implements ServiceManager
{
    /**
     * Construct this object.
     */
    protected CannedServiceMgr()
    {
        // Do not perform any initialization here that indirectly relies on
        // the service manager because we have not finished constructing
        // the service manager yet.
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
        //
        // This isn't implemented as a singleton because different sidatabase
        // and service database implementation could be used depending on
        // system settings. In a testing environment, two different tests
        // could depend upon different implementations.
        //
        try
        {
            CannedServiceMgr mgr = new CannedServiceMgr();
            mgr.createCacheAndDatabase();
            return mgr;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    // Description copied from ManagerManager
    public void destroy()
    {
        api = null;
        scfImpl = null;
        if (siCache != null) siCache.destroy();
        siCache = null;
        siDatabase = null;
    }

    // Description copied from ServiceManager
    public ServiceContextFactory getServiceContextFactory()
    {
        // Create it if it doesn't exist yet.
        if (scfImpl == null) scfImpl = new ServiceContextFactoryImpl();

        return scfImpl;
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
        // If real application support is enabled then get the real services
        // database. Otherwise, use the canned services database.
        if (MPEEnv.getEnv("OCAP.cannedSIDB.realApps") != null)
            return ServicesDatabaseImpl.getInstance();
        else
            return CannedServicesDatabase.getInstance();
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
    private void createCacheAndDatabase()
    {
        if (siCache == null)
        {
            // Instantiate the SICache
            siCache = new SICacheImpl();

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
                siDatabase = getCannedDBInstance();
            }

            // Point the SIDatabase and SICache at each other
            siDatabase.setSICache(siCache);
            siCache.setSIDatabase(siDatabase);
            ((CannedSIDatabase) siDatabase).createStaticSI();
        }
    }

    // public so this can be overridden
    public CannedSIDatabase getCannedDBInstance()
    {
        return new CannedSIDatabase();
    }

    // Description copied from ServiceManager
    public ServiceMediaHandler createMediaHandler()
    {
        CannedBroadcastPlayer handler = new CannedBroadcastPlayer(null, null);
        switch (handlerType)
        {
            case MH_START:
                break;
            case MH_FAIL_SMHSTART:
                handler.cannedSetFailSMHStart(failSMHStartCount);
                handlerType = MH_START;
                break;
            case MH_FAIL_SMHDS:
                handler.cannedSetFailSMHDS(failSMHDSCount);
                handlerType = MH_START;
                break;
            case MH_FAIL_BOTH:
                handler.cannedSetFailSMHStart(failSMHStartCount);
                handler.cannedSetFailSMHDS(failSMHDSCount);
                handlerType = MH_START;
        }
        return handler;
    }

    /**
     * Failure point controlling method that sets what kind of
     * ServiceMediaHandler will be created and when the failures will occur.<br />
     * handlerType can be one of the following values:<br />
     * <code>CannedServiceMgr.MH_START</code> - creates a ServiceMediaHandler
     * starts normally. <code>CannedServiceMgr.MH_FAIL_SMHDS</code> - creates a
     * ServiceMediaHandler that fails while trying to set the DataSource.
     * <code>CannedServiceMgr.MH_FAIL_SMHSTART</code> - creates a
     * ServiceMediaHandler that fails while trying to start.
     * <code>CannedServiceMgr.MH_FAIL_BOTH</code> - creates a
     * ServiceMediaHandler that fails while trying to set the DataSource and
     * trying to start. <br />
     * <br />
     * The reason for this complexity is because setting the DataSource occurs
     * before starting the ServiceMediaHandler, and the flags are used to set
     * when we want each type of failure to occur.
     * 
     * Examples of usage: Create a ServiceMediaHandler that fails while starting
     * on the first try, then fails setting the DataSource during the recovery
     * select... cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_BOTH,
     * CannedServiceMgr.FAIL_SECOND, CannedServiceMgr.FAIL_FIRST);
     * 
     * Create a ServiceMediaHandler that fails once while trying to start...
     * cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART,
     * CannedServiceMgr.IGNORE, CannedServiceMgr.FAIL_FIRST);
     * 
     * Confusingly... Create a ServiceMediaHandler that fails once while setting
     * the DataSource, then fails again trying to start...
     * cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART,
     * CannedServiceMgr.FAIL_FIRST, CannedServiceMgr.FAIL_FIRST);
     * 
     * To clear up the above example, think about the situation in which you are
     * setting the flags. Since setting the DataSource happens before attempting
     * to start, the next time through a selection process will be the 'first'
     * time it reached the start attempt. The FAIL_SECOND flag is used when we
     * want it to succeed the first time, but fail the second time. And, of
     * course, FAIL_BOTH cause a failure both times at the specified spot.
     * 
     * @param handlerType
     *            The type of ServiceMediaHandler type to create.
     * @param failSMHDSCount
     *            The point in time at which setting the DataSource will fail.
     * @param failSMHStartCount
     *            The point in time at which starting will fail.
     */
    public void cannedSetMediaHandlerType(int handlerType, int failSMHDSCount, int failSMHStartCount)
    {
        this.handlerType = handlerType;
        CannedServiceMgr.failSMHDSCount = failSMHDSCount;
        CannedServiceMgr.failSMHStartCount = failSMHStartCount;
    }

    /**
     * Resets all of the flags to their default values.
     */
    public static void cannedResetAllFlags()
    {
        failSMHDSCount = 0;
        failSMHStartCount = 0;
    }

    /**
     * The singleton instance of the service context factory
     * 
     * @supplierCardinality 1
     */
    // protected static ServiceContextFactoryExt scfImpl = null;
    protected ServiceContextFactoryExt scfImpl = null;

    protected MediaAPI api = null;

    /**
     * The singleton instance of the SI cache
     * 
     * @supplierCardinality 1
     */
    private SICacheImpl siCache = null;

    /**
     * The singleton instance of the SI database
     * 
     * @supplierCardinality 1
     */
    private SIDatabase siDatabase = null;

    private int handlerType = MH_START;

    public static final int MH_START = 1;

    public static final int MH_FAIL_SMHSTART = 2;

    public static final int MH_FAIL_SMHDS = 3;

    public static final int MH_FAIL_BOTH = 4;

    public static final int FAIL_FIRST = 1;

    public static final int FAIL_SECOND = -1;

    public static final int FAIL_BOTH = 2;

    public static final int IGNORE = 0;

    private static int failSMHDSCount = 0;

    private static int failSMHStartCount = 0;

    /** @author Michael Schoonover */
    public ServiceDataSource createServiceDataSource(ServiceContextExt ctx, Service svc)
    {
        CannedOcapServiceDataSource ds = new CannedOcapServiceDataSource();
        MediaLocator ml = new MediaLocator(svc.getLocator().toExternalForm());
        ds.setLocator(ml);
        ds.setServiceCtx(ctx);
        ds.setService(svc);
        return ds;
    }

    /** @author Michael Schoonover */
    public ServiceMediaHandler createServiceMediaHandler(DataSource ds, ServiceContextExt sc, Object lock, ResourceUsageImpl resourceUsage)
    {
        CannedBroadcastPlayer handler = new CannedBroadcastPlayer(sc.getCallerContext(), sc);
        switch (handlerType)
        {
            case MH_START:
                break;
            case MH_FAIL_SMHSTART:
                handler.cannedSetFailSMHStart(failSMHStartCount);
                handlerType = MH_START;
                break;
            case MH_FAIL_SMHDS:
                handler.cannedSetFailSMHDS(failSMHDSCount);
                handlerType = MH_START;
                break;
            case MH_FAIL_BOTH:
                handler.cannedSetFailSMHStart(failSMHStartCount);
                handler.cannedSetFailSMHDS(failSMHDSCount);
                handlerType = MH_START;
        }

        return handler;
    }

    public int getOOBWaitTime()
    {
        return 0;
    }

    public int getRequestAsyncTimeout()
    {
        return 0;
    }
}
