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

package org.cablelabs.test;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.application.CannedApplicationManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.CannedServicesDatabase;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.util.MPEEnv;

/**
 * SICannedInterfaceTest is just a super class for all of the canned tests. It
 * merely provides a common setup for all of the tests to use to avoid
 * duplicating code.
 * 
 * @author Joshua Keplinger
 */
public abstract class SICannedInterfaceTest extends InterfaceTestCase
{

    /**
     * Constructor that just passes the values up to the parent
     * 
     * @param name
     *            The name of our test
     * @param c
     *            The class of the test case, used for reflection
     * @param fac
     *            The factory that creates the implementation of the interface
     *            to be tested.
     */
    public SICannedInterfaceTest(String name, Class c, ImplFactory fac)
    {
        super(name, c, fac);
    }

    public static final int asyncInterval = 500;

    public static final int asyncTimeout = 2000;

    public static final int flushInterval = 1000;

    public static final int maxAge = 8000;

    public static final int waitForRequest = 4000;

    /**
     * Sets up the test for the subclasses.
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        MPEEnv.setEnv("OCAP.sicache.asyncInterval", Integer.toString(asyncInterval));
        MPEEnv.setEnv("OCAP.sicache.asyncTimeout", Integer.toString(asyncTimeout));
        MPEEnv.setEnv("OCAP.sicache.flushInterval", Integer.toString(flushInterval));
        MPEEnv.setEnv("OCAP.sicache.maxAge", Integer.toString(maxAge));

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        oldNM = (NetManager) ManagerManager.getInstance(NetManager.class);
        oldAM = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        nm = (CannedNetMgr) CannedNetMgr.getInstance();
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        am = (CannedApplicationManager) CannedApplicationManager.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, true, nm);
        ManagerManagerTest.updateManager(ApplicationManager.class, CannedApplicationManager.class, true, am);
        csm.createSIManager().setPreferredLanguage("eng");
        csidb = (CannedSIDatabase) csm.getSIDatabase();
        sic = (SICache) csm.getSICache();
        csidb.cannedFullReset();
        csdb = (CannedServicesDatabase) CannedServicesDatabase.getInstance();
    }

    /**
     * Cleans up after each test.
     */
    protected void tearDown() throws Exception
    {
        //
        // if any of the tuners were reserved, release them
        //
        NetworkInterfaceManager nim = nm.getNetworkInterfaceManager();
        NetworkInterface[] niArray = nim.getNetworkInterfaces();
        for (int i = 0; i < niArray.length; i++)
        {
            CannedNetworkInterface ni = (CannedNetworkInterface) niArray[i];
            if (ni.getReservationOwner() != null)
            {
                ni.cannedStealTuner();
            }
        }

        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);
        csm.destroy();
        ManagerManagerTest.updateManager(NetManager.class, oldNM.getClass(), true, oldNM);
        nm.destroy();
        ManagerManagerTest.updateManager(ApplicationManager.class, oldAM.getClass(), true, oldAM);
        am.destroy();

        csm = null;
        csidb = null;
        csdb = null;
        sic = null;
        nm = null;
        am = null;
        oldSM = null;
        oldNM = null;
        oldAM = null;
    }

    protected void tune(org.cablelabs.impl.service.javatv.transport.TransportStreamImpl ts)
    {
        CannedNetworkInterface cni = (CannedNetworkInterface) NetworkInterfaceManager.getInstance()
                .getNetworkInterfaces()[0];
        cni.cannedSetCurrentTransportStream(ts.getDavicTransportStream(cni));
    }

    protected SICache sic = null;

    protected CannedServiceMgr csm = null;

    protected CannedNetMgr nm = null;

    protected CannedApplicationManager am = null;

    private ServiceManager oldSM;

    private NetManager oldNM;

    private ApplicationManager oldAM;

    protected CannedSIDatabase csidb = null;

    protected CannedServicesDatabase csdb = null;
}
