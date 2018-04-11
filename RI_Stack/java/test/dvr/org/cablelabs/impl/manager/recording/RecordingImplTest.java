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

import java.security.Permission;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.cablelabs.impl.storage.MediaStorageOptionImpl;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;

/**
 * Tests org.cablelabs.impl.manager.recording.RecordingImpl
 * 
 * @author Arlis Dodson
 */
public class RecordingImplTest extends TestCase
{
    private DummySecurityManager m_sm;

    /**
     * Tests that addAppData() invokes SecurityManager.checkPermission() with
     * RecordingPermission("modify","own").
     */
    public void testAddAppDataRecPerm() throws Exception
    {
        RecordingImplMock recording = new RecordingImplMock();
        assertNotNull("Failed to retrieve RecordingImplMock Object ref", recording);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("modify", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingImpl.addAppData() ...");
            recording.addAppData("BogusKey", "BogusValue");
        }
        catch (Exception exc)
        {
        }
        assertTrue("SecurityManager.checkPermission() not invoked by addAppData()", m_sm.getInvokedFlag());
        assertTrue("Expected permission was not checked ", m_sm.wasExpectedPermissionChecked());
        m_sm.setCheckFlag(false);
        m_sm.setInvokedFlag(false);
    }

    /**
     * Tests that cancel() invokes SecurityManager.checkPermission() with
     * RecordingPermission("cancel","own").
     */
    public void testCancelRecPerm()
    {
        RecordingImplMock recording = new RecordingImplMock();
        assertNotNull("Failed to retrieve RecordingImplMock Object ref", recording);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("cancel", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingImpl.cancel() ...");
            recording.cancel();
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by cancel()", m_sm.getInvokedFlag());
            assertTrue("Expected permission was not checked ", m_sm.wasExpectedPermissionChecked());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that delete() invokes SecurityManager.checkPermission() with
     * RecordingPermission("delete","own").
     */
    public void testDeleteRecPerm()
    {
        RecordingImplMock recording = new RecordingImplMock();
        assertNotNull("Failed to retrieve RecordingImplMock Object ref", recording);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("delete", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingImpl.delete() ...");
            recording.delete();
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by delete()", m_sm.getInvokedFlag());
            assertTrue("Expected permission was not checked ", m_sm.wasExpectedPermissionChecked());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that removeAppData() invokes SecurityManager.checkPermission() with
     * RecordingPermission("modify","own").
     */
    public void testRemoveAppDataRecPerm()
    {
        RecordingImplMock recording = new RecordingImplMock();
        assertNotNull("Failed to retrieve RecordingImplMock Object ref", recording);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("modify", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingImpl.removeAppData() ...");
            recording.removeAppData("BogusKey");
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by removeAppData()", m_sm.getInvokedFlag());
            assertTrue("Expected permission was not checked ", m_sm.wasExpectedPermissionChecked());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that stop() invokes SecurityManager.checkPermission() with
     * RecordingPermission("cancel","own").
     */
    public void testStopRecPerm()
    {
        RecordingImplMock recording = new RecordingImplMock();
        assertNotNull("Failed to retrieve RecordingImplMock Object ref", recording);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("cancel", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordingImpl.stop() ...");
            recording.stop();
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by stop()", m_sm.getInvokedFlag());
            assertTrue("Expected permission was not checked ", m_sm.wasExpectedPermissionChecked());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
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
        TestSuite suite = new TestSuite(RecordingImplTest.class);
        return suite;
    }

    public RecordingImplTest(String name)
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

        private boolean expectedPermissionChecked = false;

        public void checkPermission(Permission p)
        {
            m_invoked = false;
            if (m_checkIt)
            {
                m_invoked = true;
                System.out.println("RecordingImplTest.DummySecurityManager.checkPermission: " + p.toString());
                System.out.println("\tCompare to: " + m_permToCheck.toString());
                if (p.implies(m_permToCheck))
                {
                    expectedPermissionChecked = true;
                }
            }
        }

        public boolean wasExpectedPermissionChecked()
        {
            return expectedPermissionChecked;
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

    public class RecordingImplMock extends org.cablelabs.impl.manager.recording.RecordingImpl
    {
        private org.cablelabs.impl.manager.recording.RecordingImpl m_recording;

        public RecordingImplMock()
        {
            OcapLocator[] source = null;
            try
            {
                source = new OcapLocator[] { new OcapLocator("ocap://0x44f") };
            }
            catch (org.davic.net.InvalidLocatorException e)
            {
                fail("Failed to allocate OcapLocator array");
            }
            java.util.Date startTime = new java.util.Date();
            long duration = 10000;

            StorageProxy proxyAry[] = StorageManager.getInstance().getStorageProxies();
            assertFalse("StorageManager.getStorageProxies() returned zero-length array", 0 == proxyAry.length);

            MediaStorageVolume dest = (new MediaStorageOptionImpl(proxyAry[0])).getDefaultRecordingVolume();
            assertNotNull("Failed to obtain MediaStorageVolume Object ref", dest);
            ExtendedFileAccessPermissions fap = new ExtendedFileAccessPermissions(true, true, true, true, true, true,
                    null, null);
            assertNotNull("Failed to instantiate ExtendedFileAccessPermissions Object", fap);
            OcapRecordingProperties props = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, 1000,
                    OcapRecordingProperties.DELETE_AT_EXPIRATION, OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, fap,
                    "Acme Video", dest);
            assertNotNull("Failed to instantiate OcapRecordingProperties Object", props);

            LocatorRecordingSpec lrs = null;
            try
            {
                lrs = new LocatorRecordingSpec(source, startTime, duration, props);
            }
            catch (javax.tv.service.selection.InvalidServiceComponentException e)
            {
                fail("Failed to allocate LocatorRecordingSpec");
            }
            assertNotNull("Failed to allocate LocatorRecordingSpec", lrs);

            RecordingDBManager rdbm = (RecordingDBManager) ManagerManager.getInstance(RecordingDBManager.class);
            assertNotNull("Failed to obtain RecordingDBManager Object ref", rdbm);
            RecordingManagerInterface rmi = (RecordingManagerInterface) RecordingManager.getInstance();
            m_recording = new RecordingImpl(lrs, rdbm, rmi);
            assertNotNull("Failed to allocate RecordingImpl", m_recording);
            m_info = new RecordingInfo2(123);
        }

        public void cancel() throws org.ocap.shared.dvr.AccessDeniedException
        {
            m_recording.cancel();
        }

        public void stop() throws org.ocap.shared.dvr.AccessDeniedException
        {
            m_recording.stop();
        }
    }
}
