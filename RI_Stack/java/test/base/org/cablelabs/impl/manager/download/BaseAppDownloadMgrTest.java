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

/*
 * Created on Nov 16, 2006
 */
package org.cablelabs.impl.manager.download;

import org.cablelabs.impl.manager.AppDownloadManagerTest;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerTest;
import org.cablelabs.impl.manager.AppDownloadManager.Callback;
import org.cablelabs.impl.manager.AppDownloadManager.DownloadedApp;
import org.cablelabs.impl.manager.AppDownloadManagerTest.DummyCallback;
import org.cablelabs.impl.manager.download.BaseAppDownloadMgr.PendingRequest;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.test.TestUtils;
import org.cablelabs.test.iftc.InterfaceTestSuite;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;

import org.dvb.application.AppID;
import org.dvb.dsmcc.ServiceDomain;

/**
 * Tests the {@link BaseAppDownloadMgr} abstract base class.
 * 
 * By default this tests a dummy extension of this class. However the test may
 * be subclassed to test a specific extension of this class.
 * 
 * @author Aaron Kamienski
 */
public class BaseAppDownloadMgrTest extends TestCase
{
    /**
     *
     */
    public void testNoPublicConstructor()
    {
        TestUtils.testNoPublicConstructors(BaseAppDownloadMgr.class);
    }

    /**
     *
     */
    public void testDownload()
    {
        // TODO implement download test
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.manager.download.BaseAppDownloadMgr.createPendingRequest()
     */
    public void testCreatePendingRequest()
    {
        XAppEntry entry = new XAppEntry();
        DummyCallback cb = new DummyCallback(false);
        OcTransportProtocol[] oc = { new OcTransportProtocol()
        {
            {
                protocol = 0x0001;
                label = 1;
                remoteConnection = true;
                serviceId = 0x400;
                componentTag = 0x10;
            }
        }, };

        PendingRequest req = mgr.createPendingRequest(entry, false, true, cb, oc);

        assertNotNull("Expected non-null PendingRequest be created", req);
        assertSame("Expected original AppEntry be stored", entry, req.entry);
        assertEquals("Expected auth value to be stored", false, req.authenticate);
        // assertSame("Expected original Callback be stored", cb, req.cb);
        assertSame("Exspected original OC[] be stored", oc, req.oc);

        DownloadedApp app = new DummyDownload();

        // Verify callback invocation (synchronously)
        req.downloadSuccess(app);
        assertEquals("Expected downloadSuccess() to be invoked", 1, cb.success);
        assertEquals("Expected downloadFailure() not to be invoked", 0, cb.failure);
        assertSame("Expected downloadSuccess() to be invoked with the given DownloadedApp", app, cb.app);
        cb.reset();

        // Verify callback invocation (synchronously)
        int reason = Callback.AUTH_FAILURE;
        String msg = "boo!";
        req.downloadFailure(reason, msg);
        assertEquals("Expected downloadSuccess() not to be invoked", 0, cb.success);
        assertEquals("Expected downloadFailure() to be invoked", 1, cb.failure);
        assertEquals("Expected given failure reason", reason, cb.reason);
        assertEquals("Expected given failure message", msg, cb.msg);
        cb.reset();
    }

    /**
     * Test implementation of PendingRequest -- specifically that compareTo
     * works as expected.
     */
    public void testPendingRequest_compareTo() throws Exception
    {
        DummyCallback cb = new DummyCallback();
        OcTransportProtocol[] oc = { new OcTransportProtocol()
        {
            {
                protocol = 0x0001;
                label = 1;
                remoteConnection = true;
                serviceId = 0x400;
                componentTag = 0x10;
            }
        }, };

        // Set of PendingRequests, already expected to be in order
        PendingRequest[] requests = new PendingRequest[5];

        requests[4] = mgr.createPendingRequest(new XAppEntry()
        {
            {
                priority = 3;
                id = new AppID(0x1, 0x1);
            }
        }, false, true, cb, oc);
        requests[3] = mgr.createPendingRequest(new XAppEntry()
        {
            {
                priority = 3;
                id = new AppID(0x1, 0x1);
            }
        }, false, true, cb, oc);
        requests[2] = mgr.createPendingRequest(new XAppEntry()
        {
            {
                priority = 2;
                id = new AppID(0x1, 0x2);
            }
        }, false, true, cb, oc);
        requests[1] = mgr.createPendingRequest(new XAppEntry()
        {
            {
                priority = 1;
                id = new AppID(0x3, 0x1);
            }
        }, false, true, cb, oc);
        requests[0] = mgr.createPendingRequest(new XAppEntry()
        {
            {
                priority = 1;
                id = new AppID(0x2, 0x1);
            }
        }, false, true, cb, oc);

        for (int i = 0; i < requests.length; ++i)
        {
            for (int j = 0; j < requests.length; ++j)
            {
                if (i == j)
                    assertEquals("Expected same entries to compare equals", 0, requests[i].compareTo(requests[j]));
                else if (i < j)
                    assertTrue("Expected entries to compare less than", requests[i].compareTo(requests[j]) < 0);
                else
                    assertTrue("Expected entries to compare greater than", requests[i].compareTo(requests[j]) > 0);
            }
        }
    }

    /**
     * Tests PendingRequest implementation. cancel() before downloadSuccess().
     */
    public void testPendingRequest_cancel_thenSuccess()
    {
        XAppEntry entry = new XAppEntry()
        {
            {
                priority = 1;
                id = new AppID(0x2, 0x1);
            }
        };
        DummyCallback cb = new DummyCallback();
        OcTransportProtocol[] oc = { new OcTransportProtocol()
        {
            {
                protocol = 0x0001;
                label = 1;
                remoteConnection = true;
                serviceId = 0x400;
                componentTag = 0x10;
            }
        }, };
        DummyDownload app = new DummyDownload();

        PendingRequest req = mgr.createPendingRequest(entry, false, true, cb, oc);

        // Cancel, then request
        assertTrue("Cancelled request should return true", req.cancel());
        req.downloadSuccess(app);

        // Callback should not have been invoked.
        assertEquals("Callback success should not have been invoked", 0, cb.success);
        assertEquals("Callback failure should not have been invoked", 0, cb.failure);

        // DownloadedApp should've been disposed of.
        assertTrue("DownloadedApp should've been disposed implicitly", app.disposed);

        // Second cancel request should fail
        assertFalse("Second cancel request should fail", req.cancel());
    }

    /**
     * Tests PendingRequest implementation. cancel() before downloadFailure().
     */
    public void testPendingRequest_cancel_thenFailure()
    {
        XAppEntry entry = new XAppEntry()
        {
            {
                priority = 1;
                id = new AppID(0x2, 0x1);
            }
        };
        DummyCallback cb = new DummyCallback();
        OcTransportProtocol[] oc = { new OcTransportProtocol()
        {
            {
                protocol = 0x0001;
                label = 1;
                remoteConnection = true;
                serviceId = 0x400;
                componentTag = 0x10;
            }
        }, };

        PendingRequest req = mgr.createPendingRequest(entry, false, true, cb, oc);

        // Cancel, then request
        assertTrue("Cancelled request should return true", req.cancel());
        req.downloadFailure(Callback.IO_FAILURE, "flcl!");

        // Callback should not have been invoked.
        assertEquals("Callback success should not have been invoked", 0, cb.success);
        assertEquals("Callback failure should not have been invoked", 0, cb.failure);

        // Second cancel request should fail
        assertFalse("Second cancel request should fail", req.cancel());
    }

    /**
     * Tests PendingRequest implementation. downloadSuccess() before cancel().
     */
    public void testPendingRequest_success_thenCancel()
    {
        XAppEntry entry = new XAppEntry()
        {
            {
                priority = 1;
                id = new AppID(0x2, 0x1);
            }
        };
        DummyCallback cb = new DummyCallback();
        OcTransportProtocol[] oc = { new OcTransportProtocol()
        {
            {
                protocol = 0x0001;
                label = 1;
                remoteConnection = true;
                serviceId = 0x400;
                componentTag = 0x10;
            }
        }, };
        DummyDownload app = new DummyDownload();

        PendingRequest req = mgr.createPendingRequest(entry, false, true, cb, oc);

        // Cancel, then request
        req.downloadSuccess(app);
        assertFalse("Cancel request should fail", req.cancel());

        // Callback should not have been invoked.
        assertEquals("Callback success should have been invoked", 1, cb.success);
        assertSame("Unexpected DownloadedApp instance", app, cb.app);
        assertEquals("Callback failure should not have been invoked", 0, cb.failure);

        // DownloadedApp should have been disposed of.
        assertTrue("DownloadedApp should be disposed implicitly", app.disposed);
    }

    /**
     * Tests PendingRequest implementation. downloadFailure() before cancel().
     */
    public void testPendingRequest_failure_thenCancel()
    {
        XAppEntry entry = new XAppEntry()
        {
            {
                priority = 1;
                id = new AppID(0x2, 0x1);
            }
        };
        DummyCallback cb = new DummyCallback();
        OcTransportProtocol[] oc = { new OcTransportProtocol()
        {
            {
                protocol = 0x0001;
                label = 1;
                remoteConnection = true;
                serviceId = 0x400;
                componentTag = 0x10;
            }
        }, };
        DummyDownload app = new DummyDownload();

        PendingRequest req = mgr.createPendingRequest(entry, false, true, cb, oc);

        // Cancel, then request
        req.downloadFailure(Callback.AUTH_FAILURE, "denied!");
        assertFalse("Cancel request should fail", req.cancel());

        // Callback should not have been invoked.
        assertEquals("Callback success should not have been invoked", 0, cb.success);
        assertEquals("Callback failure should have been invoked", 1, cb.failure);

        // DownloadedApp should NOT have been disposed of.
        assertFalse("DownloadedApp should not be disposed implicitly", app.disposed);
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.manager.download.BaseAppDownloadMgr.download(PendingRequest,
     * ServiceDomain)'
     */
    public void testDownloadPendingRequestServiceDomain()
    {
        // TODO: implement download() test
    }

    private static class DummyDownload implements DownloadedApp
    {
        boolean disposed;

        public void dispose()
        {
            disposed = true;
        }

        public File getBaseDirectory()
        {
            return new File("/");
        }
    }

    // TODO: need to override tuning and oc mounting... so can avoid such for
    // testing...
    private static class BaseAppDownloadMgrImpl extends BaseAppDownloadMgr
    {
        protected PendingRequest createPendingRequest(XAppEntry entry, boolean authenticate, Callback callback,
                OcTransportProtocol[] oc)
        {
            // TODO implement createPendingRequest
            return super.createPendingRequest(entry, authenticate, true, callback, oc);
        }

        protected DownloadedApp download(PendingRequest pending, ServiceDomain domain) throws DownloadFailureException
        {
            // TODO implement download
            return null;
        }

        protected DownloadedApp isDownloaded(PendingRequest pending)
        {
            return null;
        }
    }

    /* ===================== boilerplate ======================= */

    protected BaseAppDownloadMgr createInstance()
    {
        return new BaseAppDownloadMgrImpl();
    }

    protected BaseAppDownloadMgr mgr;

    protected void setUp() throws Exception
    {
        super.setUp();

        mgr = createInstance();
    }

    protected void tearDown() throws Exception
    {
        if (mgr != null) mgr.destroy();

        super.tearDown();
    }

    public BaseAppDownloadMgrTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(BaseAppDownloadMgrTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new BaseAppDownloadMgrTest(tests[i]));

            ImplFactory factory = new ManagerTest.ManagerFactory()
            {
                public Object createImplObject()
                {
                    return new BaseAppDownloadMgrImpl();
                }

                public void destroyImplObject(Object obj)
                {
                    ((Manager) obj).destroy();
                }
            };
            InterfaceTestSuite adm = AppDownloadManagerTest.isuite(tests); // filters
                                                                           // tests
            adm.addFactory(factory);
            suite.addTest(adm);

            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BaseAppDownloadMgrTest.class);

        ImplFactory factory = new ManagerTest.ManagerFactory()
        {
            public Object createImplObject()
            {
                return new BaseAppDownloadMgrImpl();
            }

            public void destroyImplObject(Object obj)
            {
                ((Manager) obj).destroy();
            }
        };
        InterfaceTestSuite asm = AppDownloadManagerTest.isuite();
        asm.addFactory(factory);
        suite.addTest(asm);

        return suite;
    }
}
