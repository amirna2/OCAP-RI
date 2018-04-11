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

package org.cablelabs.impl.media;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.ServiceMgrImpl;
import org.cablelabs.impl.media.access.MediaAccessSuite;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;
import org.cablelabs.impl.media.mpe.MediaAPITest;
import org.cablelabs.impl.media.player.AudioPlayerBaseTest;
import org.cablelabs.impl.media.player.BroadcastPlayerTest;
import org.cablelabs.impl.media.player.BroadcastServiceMediaHandlerTest;
import org.cablelabs.impl.media.player.DripFeedPlayerTest;
import org.cablelabs.impl.media.player.PlayerBaseTest;
import org.cablelabs.impl.media.presentation.AbstractPresentationTest;
import org.cablelabs.impl.media.presentation.AbstractServicePresentationTest;
import org.cablelabs.impl.media.presentation.AbstractVideoPresentationTest;
import org.cablelabs.impl.media.protocol.file.FileDataSourceTest;
import org.cablelabs.impl.media.session.BroadcastSessionTest;
import org.cablelabs.impl.media.source.BroadcastDataSourceTest;
import org.cablelabs.impl.media.source.DataSourceTest;

/**
 * This is a JUnit test suite the runs all JUnit tests for JMF-related classes.
 * 
 * @author schoonma
 */
public class JMFTests
{

    public static ServiceManager oldSM;

    public static MediaAPIManager oldAPI;

    public static NetManager oldNM;

    public static NetManager nm;

    public static NetworkInterfaceManager nim;

    public static CannedMediaAPI cannedMediaAPI;

    public static CannedServiceMgr csm;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(JMFTests.suite());
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Test Suite for JMF-related classes");
        suite.addTest(MediaAccessSuite.suite());

        suite.addTest(MediaAPITest.suite());
        suite.addTestSuite(FileDataSourceTest.class);
        suite.addTest(BroadcastPlayerTest.suite());
        suite.addTest(AudioPlayerBaseTest.suite());
        suite.addTest(DripFeedPlayerTest.suite());

        suite.addTest(PlayerBaseTest.suite());

        suite.addTestSuite(ClockImplTest.class);
        suite.addTestSuite(DataSourceTest.class);
        suite.addTestSuite(BroadcastServiceMediaHandlerTest.class);

        suite.addTestSuite(BroadcastDataSourceTest.class);

        suite.addTestSuite(AbstractPresentationTest.class);
        suite.addTestSuite(AbstractVideoPresentationTest.class);
        suite.addTestSuite(AbstractServicePresentationTest.class);

        suite.addTest(BroadcastSessionTest.suite());

        return suite;
    }

    public static void setUpJMF() throws Exception
    {
        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

        oldAPI = (MediaAPIManager) ManagerManager.getInstance(MediaAPIManager.class);
        cannedMediaAPI = (CannedMediaAPI) CannedMediaAPI.getInstance();
        ManagerManagerTest.updateManager(MediaAPIManager.class, CannedMediaAPI.class, true, cannedMediaAPI);

        oldNM = (NetManager) ManagerManager.getInstance(NetManager.class);
        nm = (CannedNetMgr) CannedNetMgr.getInstance();
        nim = nm.getNetworkInterfaceManager();
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, true, nm);
        //
        // A lot of the tests expect events to be delivered in a certain time
        // window. These will sometimes fail if gc is performed at an
        // inopportune time. Run gc here to try to prevent that.
        //
        System.gc();
    }

    public static void tearDownJMF() throws Exception
    {
        //
        // if any of the tuners were reserved, release them
        //
        NetworkInterface[] niArray = nim.getNetworkInterfaces();
        for (int i = 0; i < niArray.length; i++)
        {
            CannedNetworkInterface ni = (CannedNetworkInterface) niArray[i];
            if (ni.getReservationOwner() != null)
            {
                ni.cannedStealTuner();
            }
        }

        ManagerManagerTest.updateManager(ServiceManager.class, ServiceMgrImpl.class, true, oldSM);
        ManagerManagerTest.updateManager(MediaAPIManager.class, oldAPI.getClass(), true, oldAPI);
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, true, oldNM);

        if (csm != null)
        {
            csm.destroy();
        }

        cannedMediaAPI.destroy();

        oldSM = null;
        oldAPI = null;
        oldNM = null;
        nm = null;
        nim = null;
        cannedMediaAPI = null;
    }

    public static CannedSIDatabase getCannedSIDB()
    {
        return (CannedSIDatabase) csm.getSIDatabase();
    }

}
