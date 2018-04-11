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

package org.cablelabs.impl.storage;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.cablelabs.test.TestUtils;

import org.dvb.application.AppID;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;
import java.security.Permission;
import org.ocap.system.MonitorAppPermission;

/**
 * Tests org.cablelabs.impl.storage.MediaStorageVolumeImpl
 * 
 * @author Arlis Dodson
 */
public class MediaStorageVolumeImplTest extends TestCase
{
    private DummySecurityManager m_sm;

    /**
     * 
     * TODO - Tests security-related behavior of allowAccess() and
     * removeAccess() for the case in which the calls are made by the process
     * that owns the MediaStorageVolume.
     * 
     */

    /**
     * Tests that allocate(), allowAccess(), and removeAccess() invoke
     * SecurityManager.checkPermission() with MonitorAppPermission("storage").
     */
    public void testAllocateAllowRemoveAccessMonAppPerm() throws Exception
    {
        StorageProxy proxyAry[] = StorageManager.getInstance().getStorageProxies();
        assertFalse("StorageManager.getStorageProxies() returned zero-length array", 0 == proxyAry.length);
        DVRStorageProxyImpl dvrProxy = null;
        for (int i = 0; i < proxyAry.length; i++)
        {
            if (proxyAry[i] instanceof DVRStorageProxyImpl)
            {
                dvrProxy = (DVRStorageProxyImpl) proxyAry[i];
                break;
            }
        }
        assertTrue("Did not find a dvr storage proxy", dvrProxy != null);

        MediaStorageVolumeImpl msVol = new MediaStorageVolumeImpl(dvrProxy, "MediaStorageVolumeImplTest", new AppID(0,
                0), new ExtendedFileAccessPermissions(true, true, true, true, true, true, new int[0], new int[0]));
        try
        {
            assertNotNull("Failed to allocate new MediaStorageVolumeImpl", msVol);

            String[] orgs = { "Alpha Video", "Beta Video", "Gamma Video" };

            try
            {
                m_sm.checkIt = true;
                m_sm.invoked = false;
                System.out.println("Invoking MediaStorageVolume.allocate() ...");
                msVol.allocate(32768);
                assertTrue("SecurityManager.checkPermission() not invoked by allocate()", m_sm.invoked);
            }
            catch (Exception e)
            {
            }
            finally
            {
                m_sm.invoked = false;
                m_sm.checkIt = false;
            }

            try
            {
                m_sm.checkIt = true;
                m_sm.invoked = false;
                System.out.println("Invoking MediaStorageVolume.allowAccess() ...");
                msVol.allowAccess(orgs);
                assertTrue("SecurityManager.checkPermission() not invoked by allowAccess()", m_sm.invoked);
            }
            catch (Exception e)
            {
            }
            finally
            {
                m_sm.invoked = false;
                m_sm.checkIt = false;
            }

            try
            {
                m_sm.checkIt = true;
                m_sm.invoked = false;
                System.out.println("Invoking MediaStorageVolume.removeAccess() ...");
                msVol.removeAccess("Beta Video");
                assertTrue("SecurityManager.checkPermission() not invoked by removeAccess()", m_sm.invoked);
            }
            catch (Exception e)
            {
            }
            finally
            {
                m_sm.invoked = false;
                m_sm.checkIt = false;
            }
        }
        finally
        {
            msVol.delete();
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
        TestSuite suite = new TestSuite(MediaStorageVolumeImplTest.class);
        return suite;
    }

    public MediaStorageVolumeImplTest(String name)
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
     * need.
     */
    public static class DummySecurityManager extends NullSecurityManager
    {
        public Permission p;

        public boolean checkIt = false;

        public boolean invoked = false;

        public void checkPermission(Permission p)
        {
            invoked = false;
            if (true == checkIt)
            {
                invoked = true;
                System.out.println("MediaStorageVolumeImplTest.DummySecurityManager.checkPermission: " + p.toString());
                assertTrue("Permission not instanceof MonitorAppPermission", p instanceof MonitorAppPermission);
                String name = p.getName();
                assertTrue("MonitorAppPermission name not \"storage\"", name.equals("storage"));
            }
        }
    }
}
