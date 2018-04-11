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

package org.cablelabs.impl.manager.application;

import junit.framework.*;
import org.cablelabs.impl.manager.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;
import java.security.Permission;

/**
 * Tests the AppManager implementation.
 * 
 * @author Aaron Kamienski
 */
public class AppManagerTest extends TestCase
{
    /*
     * NOTE!!!!!! Any of the following tests overlap with interface tests. If
     * there is no interface test, perform the test there. Otherwise, do
     * whatever additional testing we can using knowledge about how the
     * AppManager and the rest of this package is implemented.
     */

    /* ---- Manager ---- */
    public void xxxtestDestroy() throws Exception
    {
        fail("Unimplemented test");
    }

    public void testGetInstance() throws Exception
    {
        Manager mgr = AppManager.getInstance();

        assertNotNull("AppManager.getInstance() should return a Manager", mgr);
        assertTrue("The Manager returned by AppManager.getInstance should be " + "instanceof AppManager",
                mgr instanceof AppManager);

        Manager mgr2 = AppManager.getInstance();
        assertSame("The same AppManager instance should be returned on successive " + "calls to getInstance", mgr, mgr2);

        // Forget instance
        mgr.destroy();

        // Forget instance
        mgr2.destroy();
    }

    /* ---- AppManagerProxy ---- */
    public void XtestSetSecurityPolicyHandler() throws Exception
    {
        // Is it enough to test AppManagerProxy?
        fail("Unimplemented test");
    }

    public void XtestSetGetAppFilter() throws Exception
    {
        // Is it enough to test AppManagerProxy?
        // No, because it doesn't provide getAppFilter
        fail("Unimplemented test");

        /*
         * AppFilter filter = new AppFilter() { public boolean accept(AppID id)
         * { return false; } }; Manager mgr = AppManager.getInstance();
         * 
         * AppFilter save = mgr.getAppFilter();
         * 
         * mgr.setAppFilter(null);
         * assertNull("Retrieved appFilter should be set appFilter (null)",
         * mgr.getAppFilter());
         * 
         * mgr.setAppFilter(filter);
         * assertSame("Retrieved appFilter should be set appFilter (filter)",
         * filter, mgr.getAppFilter());
         * 
         * mgr.setAppFilter(save);
         * assertSame("Retrieved appFilter should be set appFilter (save)",
         * save, mgr.getAppFilter());
         */
    }

    public void XtestSetAppSignalHandler() throws Exception
    {
        fail("Unimplemented test");
    }

    /* ----- AppManager ----- */

    /**
     * Tests the creation of an <code>AppContext</code>. Should return
     * <code>null</code> if one already exists for a given AppID.
     */
    public void XtestCreateAppContext()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests set/getApplicationPriority(). Should not be able to modify priority
     * of active app. Should not affect OcapAppAttributes.getPriority(). Should
     * be reflected in getApplicationPriority().
     */
    public void XtestSetGetApplicationPriority()
    {
        fail("Unimplemented test");
    }

    public static class DummySecurityManager extends NullSecurityManager
    {
        public Permission p;

        public void checkPermission(Permission p)
        {
            if (this.p == null) this.p = p;
        }
    }

    public AppManagerTest(String name)
    {
        super(name);
    }

    protected AppManager appmgr;

    protected void setUp() throws Exception
    {
        super.setUp();
        appmgr = (AppManager) AppManager.getInstance();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AppManagerTest.class);

        ImplFactory factory = new ManagerTest.ManagerFactory()
        {
            public Object createImplObject()
            {
                return AppManager.getInstance();
            }

            public void destroyImplObject(Object obj)
            {
                // since we get the singleton, don't destroy it
                // ((Manager)obj).destroy();
            }
        };
        InterfaceTestSuite appmgr = ApplicationManagerTest.isuite();
        InterfaceTestSuite ctxmgr = CallerContextManagerTest.isuite();
        appmgr.addFactory(factory);
        ctxmgr.addFactory(factory);
        suite.addTest(appmgr);
        suite.addTest(ctxmgr);

        return suite;
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
}
