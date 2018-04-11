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

import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextPermission;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;
import java.security.Permission;

/**
 * Tests org.cablelabs.impl.storage.TimeShiftBufferOptionImpl
 * 
 * @author Arlis Dodson
 */
public class TimeShiftBufferOptionImplTest extends TestCase
{
    private DummySecurityManager m_sm;

    /**
     * Tests that attach(ServiceContext) and detach() invoke
     * SecurityManager.checkPermission() with ServiceContextPermission("access",
     * "*")
     * 
     * We do not currently need a valid ServiceContext Object reference for this
     * test.
     */
    public void testAttachDetachServiceContextPermission()
    {
        StorageProxy proxyAry[] = StorageManager.getInstance().getStorageProxies();
        assertFalse("StorageManager.getStorageProxies() returned zero-length array", 0 == proxyAry.length);

        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * TimeShiftBufferOptionImpl tsbo = new
         * TimeShiftBufferOptionImpl(proxyAry[0]);
         * assertNotNull("Failed to allocate new TimeShiftBufferOptionImpl",
         * tsbo) ;
         * 
         * ServiceContext svcCtx = null; try { m_sm.checkIt = true ;
         * m_sm.invoked = false ; tsbo.attach(svcCtx);
         * assertTrue("SecurityManager.checkPermission() not invoked",
         * m_sm.invoked) ; } catch (Exception e) { } finally { m_sm.invoked =
         * false ; tsbo.detach();
         * assertTrue("SecurityManager.checkPermission() not invoked",
         * m_sm.invoked) ; m_sm.invoked = false ; m_sm.checkIt = false ; }
         */
    }

    /**
     * Tests duration of a timeshift buffer is set This creates a TSB and then
     * calls setDuration() method with a give duration in milli-seconds.
     * 
     * We do not currently need a valid ServiceContext Object reference for this
     * test.
     */

    public void testTsbSetDuration()
    {

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
        TestSuite suite = new TestSuite(TimeShiftBufferOptionImplTest.class);
        return suite;
    }

    public TimeShiftBufferOptionImplTest(String name)
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
                System.out.println("DummySecurityManager.checkPermission: " + p.toString());
                assertTrue("Permission not instanceof ServiceContextPermission", p instanceof ServiceContextPermission);
                String name = p.getName();
                assertTrue("ServiceContextPermission name not \"access\"", name.equals("access"));
                String actions = ((ServiceContextPermission) p).getActions();
                assertTrue("ServiceContextPermission actions not \"*\"", actions.equals("*"));
            }
        }
    }
}
