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

import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;

import junit.framework.TestCase;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.SICacheImpl;

/**
 * SICannedConcreteTest provides a common base for all of the implementation
 * canned tests.
 * 
 * @author Joshua Keplinger
 */
public abstract class SICannedConcreteTest extends TestCase
{

    /**
     * Constructor that just takes the name of this test.
     * 
     * @param name
     *            The name of this test.
     */
    public SICannedConcreteTest(String name)
    {
        super(name);
    }

    public static final int waitForRequest = SICannedInterfaceTest.waitForRequest;

    /**
     * Sets up common variables for the subclasses
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        oldNM = (NetManager) ManagerManager.getInstance(NetManager.class);
        nm = (CannedNetMgr) CannedNetMgr.getInstance();
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, true, nm);
        csidb = (CannedSIDatabase) csm.getSIDatabase();
        csidb.cannedFullReset();
        sic = (SICacheImpl) csm.getSICache();
    }

    /**
     * Cleans up after each tests.
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

        csm = null;
        csidb = null;
        sic = null;
        oldSM = null;
        oldNM = null;
    }

    /**
     * Concrete implementation of SIRequestor. This is used in the unit tests to
     * handle the request responses.
     * 
     * @author Joshua Keplinger
     */
    protected class SIRequestorTest implements SIRequestor
    {
        private SIRetrievable[] results = new SIRetrievable[0];

        private SIRequestFailureType failtype = null;

        private boolean success = false;

        public SIRequestorTest()
        {
            results = new SIRetrievable[0];
            failtype = null;
            success = false;
        }

        public synchronized void notifyFailure(SIRequestFailureType reason)
        {
            failtype = reason;
            assertNotNull("Failure type should not be null", failtype);
            success = true;
            notifyAll();
        }

        public synchronized void notifySuccess(SIRetrievable[] result)
        {
            this.results = result;
            assertNotNull("Results should not be null", results);
            this.success = true;
            notifyAll();
        }

        public SIRetrievable[] getResults()
        {
            return results;
        }

        public boolean succeeded()
        {
            return success;
        }

        public SIRequestFailureType getFailure()
        {
            return failtype;
        }

        public void reset()
        {
            failtype = null;
            results = new SIRetrievable[0];
            success = false;
        }

        public synchronized void waitForEvents(long millis) throws InterruptedException
        {
            if (!success) wait(millis);
        }

        public synchronized void waitForEvents(int numEvents, long millis) throws InterruptedException
        {
            for (int i = 0; i < millis / 100; i++)
            {
                if (success) break;
                // wait(100);
                Thread.sleep(100);
            }
        }
    }

    protected void tune(org.cablelabs.impl.service.javatv.transport.TransportStreamImpl ts)
    {
        CannedNetworkInterface cni = (CannedNetworkInterface) NetworkInterfaceManager.getInstance()
                .getNetworkInterfaces()[0];
        cni.cannedSetCurrentTransportStream(ts.getDavicTransportStream(cni));
    }

    protected CannedServiceMgr csm;

    private ServiceManager oldSM;

    protected CannedSIDatabase csidb;

    protected SICacheImpl sic;

    protected CannedNetMgr nm = null;

    private NetManager oldNM;
}
