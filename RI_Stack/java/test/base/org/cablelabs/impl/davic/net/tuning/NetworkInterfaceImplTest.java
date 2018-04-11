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

package org.cablelabs.impl.davic.net.tuning;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.spi.ProviderRegistry;
import org.dvb.spi.selection.MockKnownServiceReference;
import org.dvb.spi.selection.MockSelectionProvider;
import org.dvb.spi.selection.MockServiceReference;
import org.dvb.spi.selection.SelectionSession;
import org.dvb.spi.selection.MockSelectionProvider.MockServiceDescription;
import org.dvb.spi.util.MultilingualString;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.util.EventCallback;
import org.cablelabs.test.TestUtils;

/**
 * Tests the NetworkInterface class.
 */
public class NetworkInterfaceImplTest extends TestCase
{

    private TestNetworkInterfaceImpl ni;

    private NetworkInterfaceController nic;

    private TestResourceClient client;

    private TestNetworkInterfaceCallback nicb;

    private ProviderRegistry registry;

    private MockSelectionProvider provider;

    private ServiceManager oldSM;

    private NetManager oldNM;

    private CannedServiceMgr csm;

    private TestNetManager tnm;

    public void testAncestry()
    {
        TestUtils.testExtends(NetworkInterfaceImpl.class, ExtendedNetworkInterface.class);
    }

    /**
     * Test the single constructor of NetworkInterfaceImpl.
     * <ul>
     * NetworkInterfaceImpl(int)
     * </ul>
     */
    public void testConstructors()
    {
        // NetworkInterfaceImpl is unable to be explicitly instantiated.
        TestUtils.testNoPublicConstructors(NetworkInterfaceImpl.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(NetworkInterfaceImpl.class);
    }

    public void testTuneSPIServiceKnown() throws Exception
    {
        String loc = "ocap://0x1";
        MockServiceReference oldRef = new MockServiceReference(loc, loc);

        int freq = 5000;
        int prog = 2;
        int mod = 1;
        OcapLocator locator = new OcapLocator(freq, prog, mod);
        MockKnownServiceReference newRef = new MockKnownServiceReference(loc, loc, locator);
        MultilingualString[] names = new MultilingualString[] { new MultilingualString("testService1", "eng") };
        MockServiceDescription newSd = new MockServiceDescription(names);
        provider.cannedUpdateServiceReference(oldRef, newRef, newSd);

        nic.tune(new OcapLocator(loc));
        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_SYNCED);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }

        assertEquals("Network interface did not tune properly", TestNetworkInterfaceCallback.TUNED, nicb.state);
        assertNotNull("Current selection session should not be null", ni.getCurrentSelectionSession());

    }

    public void testTuneSPIServiceUnknown() throws Exception
    {
        String loc = "ocap://0x1";
        nic.tune(new OcapLocator(loc));
        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_SYNCED);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }

        assertEquals("Network interface did not tune properly", TestNetworkInterfaceCallback.TUNED, nicb.state);
        assertNotNull("Current selection session should not be null", ni.getCurrentSelectionSession());
    }

    public void testTuneSPIServiceRemap() throws Exception
    {
        // We'll go ahead and tune like normal to get started
        String loc = "ocap://0x1";
        nic.tune(new OcapLocator(loc));
        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_SYNCED);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }

        assertEquals("Network interface did not tune properly", TestNetworkInterfaceCallback.TUNED, nicb.state);
        SelectionSession session = ni.getCurrentSelectionSession();
        assertNotNull("Current selection session should not be null", session);

        int freq = 6500;
        int prog = 8;
        int mod = 2;
        OcapLocator locator = new OcapLocator(freq, prog, mod);
        MockKnownServiceReference newRef = new MockKnownServiceReference(loc, loc, locator);
        MultilingualString[] names = new MultilingualString[] { new MultilingualString("testService1", "eng") };
        MockServiceDescription newSd = new MockServiceDescription(names);
        MockServiceReference oldRef = new MockServiceReference(loc, loc);
        provider.cannedUpdateServiceReference(oldRef, newRef, newSd);

        assertEquals("Network interface did not start retuning properly", TestNetworkInterfaceCallback.RETUNING,
                nicb.state);
        assertNotNull("Current selection session should not be null", ni.getCurrentSelectionSession());

        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_SYNCED);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }
        assertEquals("Network interface did not finish retuning properly", TestNetworkInterfaceCallback.TUNED,
                nicb.state);
        assertEquals("Current selection session does not match", session, ni.getCurrentSelectionSession());
    }

    public void testTuneSPIServiceRemapFailure() throws Exception
    {
        // We'll go ahead and tune like normal to get started
        String loc = "ocap://0x1";
        nic.tune(new OcapLocator(loc));
        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_SYNCED);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }

        assertEquals("Network interface did not tune properly", TestNetworkInterfaceCallback.TUNED, nicb.state);
        SelectionSession session = ni.getCurrentSelectionSession();
        assertNotNull("Current selection session should not be null", session);

        int freq = 6500;
        int prog = 8;
        int mod = 2;
        OcapLocator locator = new OcapLocator(freq, prog, mod);
        MockKnownServiceReference newRef = new MockKnownServiceReference(loc, loc, locator);
        MultilingualString[] names = new MultilingualString[] { new MultilingualString("testService1", "eng") };
        MockServiceDescription newSd = new MockServiceDescription(names);
        MockServiceReference oldRef = new MockServiceReference(loc, loc);
        provider.cannedUpdateServiceReference(oldRef, newRef, newSd);

        assertEquals("Network interface did not start retuning properly", TestNetworkInterfaceCallback.RETUNING,
                nicb.state);
        assertNotNull("Current selection session should not be null", ni.getCurrentSelectionSession());

        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_FAIL);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }
        assertEquals("Network interface did not finish retuning properly", TestNetworkInterfaceCallback.UNTUNED,
                nicb.state);
        assertNull("Current selection session should be null", ni.getCurrentSelectionSession());
    }

    public void testTuneSPIServiceUnmap() throws Exception
    {
        // We'll go ahead and tune like normal to get started
        String loc = "ocap://0x1";
        nic.tune(new OcapLocator(loc));
        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_SYNCED);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }

        assertEquals("Network interface did not tune properly", TestNetworkInterfaceCallback.TUNED, nicb.state);
        SelectionSession session = ni.getCurrentSelectionSession();
        assertNotNull("Current selection session should not be null", session);
        int freq = 5000;
        int prog = 2;
        int mod = 1;
        OcapLocator locator = new OcapLocator(freq, prog, mod);
        MockKnownServiceReference oldRef = new MockKnownServiceReference(loc, loc, locator);
        MockServiceReference newRef = new MockServiceReference(loc, loc);
        provider.cannedUpdateServiceReference(oldRef, newRef, null);

        assertEquals("Network interface did not untune properly", TestNetworkInterfaceCallback.UNTUNED, nicb.state);
        session = ni.getCurrentSelectionSession();
        assertNull("Current selection session should be null", session);
    }

    public void testTuneSPIServiceUnmapRemap() throws Exception
    {
        // We'll go ahead and tune like normal to get started
        String loc = "ocap://0x1";
        nic.tune(new OcapLocator(loc));
        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_SYNCED);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }

        assertEquals("Network interface did not tune properly", TestNetworkInterfaceCallback.TUNED, nicb.state);
        SelectionSession session = ni.getCurrentSelectionSession();
        assertNotNull("Current selection session should not be null", session);
        int freq = 5000;
        int prog = 2;
        int mod = 1;
        OcapLocator locator = new OcapLocator(freq, prog, mod);
        MockKnownServiceReference oldRef = new MockKnownServiceReference(loc, loc, locator);
        MockServiceReference newRef = new MockServiceReference(loc, loc);
        provider.cannedUpdateServiceReference(oldRef, newRef, null);

        assertEquals("Network interface did not untune properly", TestNetworkInterfaceCallback.UNTUNED, nicb.state);
        session = ni.getCurrentSelectionSession();
        assertNull("Current selection session should be null", session);

        freq = 6500;
        prog = 8;
        mod = 2;
        locator = new OcapLocator(freq, prog, mod);
        MockKnownServiceReference newKRef = new MockKnownServiceReference(loc, loc, locator);
        MultilingualString[] names = new MultilingualString[] { new MultilingualString("testService1", "eng") };
        MockServiceDescription newSd = new MockServiceDescription(names);
        MockServiceReference oldSRef = new MockServiceReference(loc, loc);
        provider.cannedUpdateServiceReference(oldSRef, newKRef, newSd);

        assertEquals("Network interface did not start retuning properly", TestNetworkInterfaceCallback.RETUNING,
                nicb.state);
        assertNotNull("Current selection session should not be null", ni.getCurrentSelectionSession());

        ni.sendNativeEvent(EventCallback.TUNE_STARTED);
        ni.sendNativeEvent(EventCallback.TUNE_SYNCED);
        synchronized (this)
        {
            wait(1000); // Wait a couple seconds for the tune to complete.
        }
        assertEquals("Network interface did not finish retuning properly", TestNetworkInterfaceCallback.TUNED,
                nicb.state);
        assertNotNull("Current selection session should not be null", ni.getCurrentSelectionSession());
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(NetworkInterfaceImplTest.class);
        return suite;
    }

    public NetworkInterfaceImplTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);
        oldNM = (NetManager) ManagerManager.getInstance(NetManager.class);
        tnm = new TestNetManager();
        ManagerManagerTest.updateManager(NetManager.class, TestNetManager.class, true, tnm);

        ni = (TestNetworkInterfaceImpl) tnm.getNetworkInterfaceManager().getNetworkInterfaces()[0];
        client = new TestResourceClient();
        nic = new NetworkInterfaceController(client);
        nic.reserve(ni, null);
        nicb = new TestNetworkInterfaceCallback();
        ni.addNetworkInterfaceCallback(nicb, 1);

        registry = ProviderRegistry.getInstance();
        provider = new MockSelectionProvider();
        registry.registerSystemBound(provider);
    }

    protected void tearDown() throws Exception
    {
        ni.removeNetworkInterfaceCallback(nicb);
        nic.release();

        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);
        ManagerManagerTest.updateManager(NetManager.class, oldNM.getClass(), true, oldNM);
        csm.destroy();
        tnm.destroy();

        registry.unregister(provider);

        client = null;
        nic = null;
        ni = null;
        nicb = null;
        csm = null;

        super.tearDown();
    }

    private static class TestNetManager extends CannedNetMgr
    {
        public NetworkInterfaceManager getNetworkInterfaceManager()
        {
            // Create it if it doesn't exist yet.
            if (nim == null) nim = new TestNetworkInterfaceManager();

            return nim;
        }

        private TestNetworkInterfaceManager nim = null;
    }

    private static class TestNetworkInterfaceManager extends NetworkInterfaceManagerImpl
    {
        public void setInterfaces()
        {
            int numTuners = 1;
            networkInterfaces = new ExtendedNetworkInterface[numTuners];
            for (int i = 0; i < networkInterfaces.length; ++i)
                networkInterfaces[i] = new TestNetworkInterfaceImpl(i + 1);
        }

        // Description copied from NetworkInterfaceManager
        public static NetworkInterfaceManager getInstance()
        {
            return instance;
        }

        /**
         * Singleton instance of CannedNetworkInterfaceManager.
         */
        private static NetworkInterfaceManager instance = new TestNetworkInterfaceManager();
    }

    private static class TestNetworkInterfaceImpl extends NetworkInterfaceImpl
    {

        private EDListener listener;

        protected TestNetworkInterfaceImpl(int tunerHandle)
        {
            super(tunerHandle);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.davic.net.tuning.NetworkInterfaceImpl#nativeTune
         * (int,
         * org.cablelabs.impl.davic.net.tuning.NetworkInterfaceImpl.NIListener,
         * int, int, int)
         */
        protected boolean nativeTune(int tunerId, EDListener listener, int frequency, int programNum, int qam)
        {
            this.listener = listener;
            return true;
        }

        public void sendNativeEvent(int event)
        {
            listener.asyncEvent(event, 0, 0);
        }

    }

    private static class TestResourceClient implements ResourceClient
    {

        public void notifyRelease(ResourceProxy proxy)
        {

        }

        public void release(ResourceProxy proxy)
        {

        }

        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return true;
        }

    }

    private static class TestNetworkInterfaceCallback implements NetworkInterfaceCallback
    {

        public static final int UNTUNED = 1;

        public static final int TUNING = 2;

        public static final int RETUNING = 3;

        public static final int TUNED = 4;

        public int state = UNTUNED;

        public void notifyRetuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success,
                boolean synced)
        {
            if (success)
                state = TUNED;
            else
                state = UNTUNED;
        }

        public void notifyRetunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            state = RETUNING;
        }

        public void notifyTuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success, boolean synced)
        {
            if (success)
                state = TUNED;
            else
                state = UNTUNED;
        }

        public void notifyTunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            state = TUNING;
        }

        public void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            state = UNTUNED;
        }

        public void notifySyncAcquired(ExtendedNetworkInterface ni, Object tuneInstance)
        {
        }

        public void notifySyncLost(ExtendedNetworkInterface ni, Object tuneInstance)
        {
        }

    }

}
