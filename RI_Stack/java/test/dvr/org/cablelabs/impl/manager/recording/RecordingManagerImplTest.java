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

package org.cablelabs.impl.manager.recording;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.Permission;

import javax.tv.service.selection.ServiceContextPermission;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.dvr.storage.SpaceAllocationHandler;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingManager;

/**
 * Tests org.cablelabs.impl.manager.recording.RecordingManagerImpl
 * 
 * @author Arlis Dodson
 */
public class RecordingManagerImplTest extends TestCase
{
    private DummySecurityManager m_sm;

    /**
     * Tests that addRecordingChangedListener() invokes
     * SecurityManager.checkPermission() with RecordingPermission("read","own").
     */
    public void testAddRecordingChangedListenerRecPerm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("read", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.addRecordingChangedListener() ...");
            RecordingChangedListener listener = null; // okay for this test
            // mgr.addRecordingChangedListener(listener);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by addRecordingChangedListener()",
                    m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that getEntries() invokes SecurityManager.checkPermission() with
     * RecordingPermission("read","own").
     */
    public void testGetEntries1RecPerm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("read", "own"));
            m_sm.setInvokedFlag(false);
            // System.out.println("Invoking RecordingManagerImpl.getEntries(void) ...");
            // RecordingList ignore = mgr.getEntries();
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by getEntries(void)", m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that getEntries(RecordingListFilter) invokes
     * SecurityManager.checkPermission() with RecordingPermission("read","own").
     */
    public void testGetEntries2RecPerm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("read", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.getEntries(RecordingListFilter) ...");
            RecordingListFilter filter = null; // okay for this test
            // RecordingList ignore = mgr.getEntries(filter);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by getEntries(RecordingListFilter)",
                    m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that getPrioritizedResourceUsages() invokes
     * SecurityManager.checkPermission() with
     * MonitorAppPermission("handler.recording").
     */
    public void testGetPrioritizedResourceUsagesMonAppPerm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new MonitorAppPermission("handler.recording"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.getPrioritizedResourceUsages() ...");
            RecordingRequest request = null; // okay for this test
            // ResourceUsage [] ignore =
            // mgr.getPrioritizedResourceUsages(request);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by getPrioritizedResourceUsages()",
                    m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that record() invokes SecurityManager.checkPermission() with
     * RecordingPermission("create", "own").
     */
    public void testRecordRec1Perm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("create", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.record() ...");
            RecordingSpec source = null; // okay for this test
            // RecordingRequest ignore = mgr.record(source);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by record()", m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that record() invokes SecurityManager.checkPermission() with
     * ServiceContextPermission("access", "own") if the argument is of type
     * ServiceContextRecordingSpec.
     * 
     * PROBLEM: checkPermission() is called first to verify
     * RecordingPermission("create","own") Then checkPermission() is called a
     * second time to verify ServiceContextPermission("access","own"). SOLUTION:
     * use a "kludge"
     */
    public void testRecordRec2Perm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.useRecordKludge();
            m_sm.setCheckFlag(true);
            // m_sm.setPermToCheck(new
            // ServiceContextPermission("access","own"));
            m_sm.setPermToCheck(new RecordingPermission("create", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.record() ...");
            // A null argument would not suffice for this test, which depends
            // on the type of the RecordingSpec ...
            ServiceContextRecordingSpec source = new ServiceContextRecordingSpec(null, null, 1000, null);
            // new ServiceContextRecordingSpec(svcCtx, startTime, duration,
            // props);
            assertNotNull("Failed to create ServiceContextRecordingSpec Object", source);
            // RecordingRequest ignore = mgr.record(source);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by record()", m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that resolve() invokes SecurityManager.checkPermission() with
     * MonitorAppPermission("handler.recording").
     */
    public void testResolveMonAppPerm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new MonitorAppPermission("handler.recording"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.resolve() ...");
            RecordingRequest request = null; // okay for this test
            RecordingSpec spec = null; // okay for this test
            int resState = 0; // okay for this test
            // RecordingRequest ignore = mgr.resolve(request, spec, resState);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by resolve()", m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that setPrioritization() invokes SecurityManager.checkPermission()
     * with MonitorAppPermission("handler.recording").
     */
    public void testSetPrioritizationMonAppPerm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new MonitorAppPermission("handler.recording"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.setPrioritization() ...");
            ResourceUsage[] ru = null; // okay for this test
            // mgr.setPrioritization(ru);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by setPrioritization()", m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that setRequestResolutionHandler() invokes
     * SecurityManager.checkPermission() with
     * MonitorAppPermission("handler.recording").
     */
    public void testSetRequestResolutionHandlerMonAppPerm()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new MonitorAppPermission("handler.recording"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.setRequestResolutionHandler() ...");
            RequestResolutionHandler rrh = null; // okay for this test
            // mgr.setRequestResolutionHandler(rrh);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by setRequestResolutionHandler()",
                    m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that setSpaceAllocationHandler() invokes
     * SecurityManager.checkPermission() with
     * MonitorAppPermission("handler.recording").
     */
    public void testSetSpaceAllocationHandlerMonAppPerm() throws Exception
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new MonitorAppPermission("handler.recording"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingManagerImpl.setSpaceAllocationHandler() ...");
            SpaceAllocationHandler sah = null; // okay for this test
            // mgr.setSpaceAllocationHandler(sah);
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by setSpaceAllocationHandler()",
                    m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that space allocation handler isn't leaked.
     */
    public void setSpaceAllocationHandler_ClearLeak()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);

        SpaceAllocationHandler sah = new Handler();
        Reference r = new WeakReference(sah);

        // mgr.setSpaceAllocationHandler(sah);
        try
        {
            sah = null;
            System.gc();
            System.gc();
            assertNotNull("Set handler should not be collected yet", r.get());

            // mgr.setSpaceAllocationHandler(null);
            System.gc();
            System.gc();
            assertNull("Cleared handler should be collected", r.get());
        }
        finally
        {
            // mgr.setSpaceAllocationHandler(null);
        }
    }

    /**
     * Tests that space allocation handler isn't leaked.
     */
    public void setSpaceAllocationHandler_ReplaceLeak()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);

        SpaceAllocationHandler h1 = new Handler();
        SpaceAllocationHandler h2 = new Handler();
        Reference r1 = new WeakReference(h1);
        Reference r2 = new WeakReference(h1);

        // mgr.setSpaceAllocationHandler(h1);
        try
        {
            h1 = null;
            System.gc();
            System.gc();
            assertNotNull("Set handler should not be collected yet", r1.get());

            // mgr.setSpaceAllocationHandler(h2);
            h2 = null;
            System.gc();
            System.gc();
            assertNull("Cleared handler should be collected", r1.get());
            assertNotNull("Set handler should not be collected yet", r1.get());
        }
        finally
        {
            // mgr.setSpaceAllocationHandler(null);
        }
    }

    /**
     * Tests that space allocation handler isn't leaked.
     */
    public void setRequestResolutionHandler_ClearLeak()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);

        SpaceAllocationHandler sah = new Handler();
        Reference r = new WeakReference(sah);

        // mgr.setSpaceAllocationHandler(sah);
        try
        {
            sah = null;
            System.gc();
            System.gc();
            assertNotNull("Set handler should not be collected yet", r.get());

            // mgr.setSpaceAllocationHandler(null);
            System.gc();
            System.gc();
            assertNull("Cleared handler should be collected", r.get());
        }
        finally
        {
            // mgr.setSpaceAllocationHandler(null);
        }
    }

    /**
     * Tests that space allocation handler isn't leaked.
     */
    public void setRequestResolutionHandler_ReplaceLeak()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        assertNotNull("Failed to retrieve RecordingManagerImpl Object ref", mgr);

        RequestResolutionHandler h1 = new Handler();
        RequestResolutionHandler h2 = new Handler();
        Reference r1 = new WeakReference(h1);
        Reference r2 = new WeakReference(h1);

        // mgr.setRequestResolutionHandler(h1);
        try
        {
            h1 = null;
            System.gc();
            System.gc();
            assertNotNull("Set handler should not be collected yet", r1.get());

            // mgr.setRequestResolutionHandler(h2);
            h2 = null;
            System.gc();
            System.gc();
            assertNull("Cleared handler should be collected", r1.get());
            assertNotNull("Set handler should not be collected yet", r1.get());
        }
        finally
        {
            // mgr.setRequestResolutionHandler(null);
        }
    }

    private class Handler implements SpaceAllocationHandler, RequestResolutionHandler
    {
        public long allowReservation(LogicalStorageVolume volume, AppID app, long spaceRequested)
        {
            return spaceRequested;
        }

        public void requestResolution(RecordingRequest request)
        {
        }
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
        TestSuite suite = new TestSuite(RecordingManagerImplTest.class);
        return suite;
    }

    public RecordingManagerImplTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        m_sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(m_sm);
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        ProxySecurityManager.pop();
    }

    /*
     * Not sure this approach is kosher, but it seemed to meet my immediate
     * need, which was simply to confirm that (a)
     * SecurityManager.checkPermission() is invoked (b) the correct Permission
     * is passed to checkPermission()
     */
    public static class DummySecurityManager extends NullSecurityManager
    {
        public Permission p;

        private Permission m_permToCheck;

        private boolean m_checkIt = false;

        private boolean m_invoked = false;

        private boolean m_recordKludge = false;

        private boolean m_kludgeFlag = false;

        public void checkPermission(Permission p)
        {
            m_invoked = false;
            if (m_checkIt)
            {
                // Using a kludge to handle sequential calls to
                // checkPermission() ...
                if (m_recordKludge)
                {
                    if (!m_kludgeFlag)
                    {
                        m_kludgeFlag = true;
                    }
                    else
                    {
                        m_recordKludge = false;
                        m_kludgeFlag = false;
                        m_permToCheck = new ServiceContextPermission("access", "own");
                    }
                }
                m_invoked = true;
                System.out.println("RecordingManagerImplTest.DummySecurityManager.checkPermission: " + p.toString());
                System.out.println("\tCompare to: " + m_permToCheck.toString());
                assertEquals("Permission to be checked does not match reference Permission", p, m_permToCheck);
            }
        }

        public void useRecordKludge()
        {
            m_recordKludge = true;
            m_kludgeFlag = false;
        }

        public void setPermToCheck(Permission p)
        {
            m_permToCheck = p;
        }

        public void setCheckFlag(boolean flag)
        {
            m_checkIt = flag;
        }

        public boolean getCheckFlag()
        {
            return m_checkIt;
        }

        public boolean getInvokedFlag()
        {
            return m_invoked;
        }

        public void setInvokedFlag(boolean flag)
        {
            m_invoked = flag;
        }
    }
}
