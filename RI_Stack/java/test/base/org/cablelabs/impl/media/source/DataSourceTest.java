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
package org.cablelabs.impl.media.source;

import java.util.List;

import javax.media.Duration;

import junit.framework.TestCase;

import org.davic.media.MediaLocator;
import org.davic.mpeg.TransportStream;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterfaceManager;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.media.protocol.ocap.DataSource;

/**
 * DataSourceTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class DataSourceTest extends TestCase
{

    private DataSource ds;

    private ServiceManager oldSM;

    private NetManager oldNM;

    // private CannedServiceDataSourceCallback callback;
    private CannedNetworkInterface cni;

    private CannedServiceMgr csm;

    private CannedSIDatabase sidb;

    /**
     * 
     */
    public DataSourceTest()
    {
        super();
    }

    /**
     * @param name
     */
    public DataSourceTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(DataSourceTest.class);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        oldNM = (NetManager) ManagerManager.getInstance(NetManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        sidb = (CannedSIDatabase) csm.getSIDatabase();
        CannedNetMgr nm = (CannedNetMgr) CannedNetMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, true, nm);

        ds = new DataSource();

        cni = (CannedNetworkInterface) CannedNetworkInterfaceManager.getInstance().getNetworkInterfaces()[0];
        TransportStream ts = sidb.transportStream7.getDavicTransportStream(cni);
        cni.cannedSetCurrentTransportStream(ts);
    }

    protected void tearDown() throws Exception
    {
        // ds.setTuningCallback(null);
        cni.cannedClearCallbacks();
        // callback = null;
        ds = null;
        cni = null;

        ManagerManagerTest.updateManager(NetManager.class, oldNM.getClass(), true, oldNM);
        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);

        sidb = null;
        csm.destroy();
        csm = null;
        oldNM = null;
        oldSM = null;

        super.tearDown();
    }

    // Test section

    public void testConnect() throws Exception
    {
        // First we'll do a successful connect
        ds.setLocator(new MediaLocator(sidb.l100));
        ds.connect();
        assertEquals("Returned Service is incorrect", sidb.service15, ds.getService());

        ds = new DataSource();
        try
        {
            ds.connect();
            fail("Exception should've been thrown with null MediaLocator");
        }
        catch (NullPointerException ex)
        { /* expected */
        }

    }

    public void testDisconnect()
    {
        // ds.setNI(cni);
        cni.addNetworkInterfaceCallback(null, 0);
        ds.disconnect();
        // assertNull("NI should be null", ds.getNI());
        assertFalse("DataSource did not correctly remove callback from NI", cni.cannedGetCallbacks().contains(ds));
    }

    public void testGetContentType()
    {
        assertEquals("Content type is incorrect", "ocap.broadcast", ds.getContentType());
    }

    public void testGetDuration()
    {
        assertEquals("Duration is incorrect", Duration.DURATION_UNBOUNDED, ds.getDuration());
    }

    /*
     * This test is disable currently until it can be verified that it makes
     * sense to test for a callback on a DataSource.
     */
    public void xtestNotifyTuneComplete() throws Exception
    {
        // Notify tune complete
        ds.setLocator(new MediaLocator(sidb.l100));
        ds.connect();
        // ds.setNI(null);
        List callbacks = cni.cannedGetCallbacks();
        NetworkInterfaceCallback nicb = (NetworkInterfaceCallback) callbacks.get(0);
        nicb.notifyTuneComplete(cni, null, true, true);
        // assertTrue("Callback was not notified correctly",
        // callback.tunedBack);

        // Now let's notify a tune complete again, but this time we already have
        // the NI
        // callback.reset();
        nicb.notifyTuneComplete(cni, null, true, true);
        // assertFalse("Callback should not have been notified",
        // callback.tunedBack);

        // Finally, we'll set the TransportStream on the NI to null
        // callback.reset();
        cni.cannedSetCurrentTransportStream(null);
        nicb.notifyTuneComplete(cni, null, true, true);
        // assertFalse("Callback should not have been notified",
        // callback.tunedBack);
    }

    /*
     * This test is disable currently until it can be verified that it makes
     * sense to test for a callback on a DataSource.
     */
    public void xtestNotifyTuneStart() throws Exception
    {
        // Notify tune start with the NetworkInterface being used
        ds.setLocator(new MediaLocator(sidb.l100));
        ds.connect();
        List callbacks = cni.cannedGetCallbacks();
        NetworkInterfaceCallback nicb = (NetworkInterfaceCallback) callbacks.get(0);
        // ds.setNI(cni);
        nicb.notifyTunePending(cni, null);
        // assertTrue("Callback was not notified correctly",
        // callback.tunedAway);

        // Now notify when there isn't a NetworkInterface assigned
        // callback.reset();
        // ds.setNI(null);
        nicb.notifyTunePending(cni, null);
        // assertFalse("Callback was incorrectly notified", callback.tunedAway);

        // Now just for the sake of coverage, we'll try to notify with no
        // callback installed
        // callback.reset();
        // ds.setNI(cni);
        // ds.setTuningCallback(null);
        nicb.notifyTunePending(cni, null);
        // assertFalse("Callback was incorrectly notified", callback.tunedAway);
    }
    //    
    // private class CannedServiceDataSourceCallback implements
    // ServiceDataSourceCallback
    // {
    //
    // boolean tunedAway;
    // boolean tunedBack;
    //        
    // CannedServiceDataSourceCallback()
    // {
    // reset();
    // }
    //        
    // public void notifyTuneAway()
    // {
    // tunedAway = true;
    // }
    //
    // public void notifyTuneBack()
    // {
    // tunedBack = true;
    // }
    //        
    // public void reset()
    // {
    // tunedAway = false;
    // tunedBack = false;
    // }
    //        
    // }
}
