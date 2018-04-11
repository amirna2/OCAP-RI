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

package org.cablelabs.impl.manager;

import junit.framework.*;
import org.dvb.application.*;
import org.ocap.application.*;
import javax.tv.xlet.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;

public class ApplicationManagerTest extends ManagerTest
{
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ApplicationManagerTest.class);
        suite.setName(ApplicationManager.class.getName());
        return suite;
    }

    public ApplicationManagerTest(String name, ImplFactory f)
    {
        super(name, ApplicationManager.class, f);
    }

    protected ApplicationManager createApplicationManager()
    {
        return (ApplicationManager) createManager();
    }

    private ApplicationManager appmgr;

    protected void setUp() throws Exception
    {
        super.setUp();
        appmgr = (ApplicationManager) mgr;
    }

    protected void tearDown() throws Exception
    {
        appmgr = null;
        super.tearDown();
    }

    /**
     * Tests getAppManagerProxy(). Verifies that a singleton AppManagerProxy is
     * returned. Verifies that the SecurityManager is queried.
     */
    public void testGetAppManagerProxy() throws Exception
    {
        AppManagerProxy proxy = appmgr.getAppManagerProxy();

        assertNotNull("The value returned by getAppManagerProxy should not be null", proxy);

        assertSame("The AppManagerProxy should be the same on successive calls", proxy, appmgr.getAppManagerProxy());
    }

    /**
     * Tests getAppsDatabase().
     */
    public void testGetAppsDatabase() throws Exception
    {
        AppsDatabase db = appmgr.getAppsDatabase();

        assertNotNull("The value returned by getAppsDatabase should not be null", db);

        assertSame("The AppsDatabase should be the same on successive calls", db, appmgr.getAppsDatabase());
    }

    /**
     * Tests getAppClassLoader().
     */
    public void testGetAppClassLoader() throws Exception
    {
        // !!!! This test needs to be called FROM a running Xlet!!!!
        // Otherwise it will fail with an error!!!!
        ClassLoader cl = appmgr.getAppClassLoader(null);

        assertNotNull("The value returned by getAppClassLoader should not be null", cl);

        assertSame("The ClassLoader should be the same on successive calls", cl, appmgr.getAppClassLoader(null));
    }

    /**
     * Tests getAppAttributes().
     */
    public void xxxtestGetAppAttributes() throws Exception
    {
        // !!!! This test needs to be called FROM a running Xlet!!!!
        // Otherwise it will fail with an error!!!!
        fail("Unimplemented test");
    }

    /**
     * Tests registerResidentApp().
     */
    public void xxxtestRegisterResidentApp() throws Exception
    {
        fail("Unimplemented test");
    }

    /**
     * Tests destroy(). Overridden to make sure that other calls fail somehow.
     */
    /*
     * public void testDestroy() throws Exception { super.testDestroy();
     * 
     * // getAppManagerProxy try { AppManagerProxy proxy =
     * appmgr.getAppManagerProxy(); if (proxy != null)
     * fail("getAppManagerProxy should fail if manager is destroyed"); else
     * assertNull("getAppManagerProxy should fail if manager is destroyed",
     * proxy); } catch(Exception e) {}
     * 
     * // getAppID try { AppID appid = appmgr.getAppID(); if (appid != null)
     * fail("getAppID should fail if manager is destroyed"); else
     * assertNull("getAppID should fail if manager is destroyed", appid); }
     * catch(Exception e) {}
     * 
     * // getAppsDatabase try { AppsDatabase db = appmgr.getAppsDatabase(); if
     * (db != null) fail("getAppsDatabase should fail if manager is destroyed");
     * else assertNull("getAppsDatabase should fail if manager is destroyed",
     * db); } catch(Exception e) {}
     * 
     * // getAppClassLoader try { ClassLoader cl = appmgr.getAppClassLoader();
     * if (cl != null)
     * fail("getAppClassLoader should fail if manager is destroyed"); else
     * assertNull("getAppClassLoader should fail if manager is destroyed", cl);
     * } catch(Exception e) {} }
     */

    /*
     * public Throwable runTtest(String test) {
     * 
     * }
     * 
     * public static class TestXlet implements Xlet extends TestCase { private
     * String[] args; public static Throwable throw; public void
     * initXlet(XletContext context) { this.context = context; } public void
     * startXlet() throws XletStateChangeException { args =
     * context.getXletProperty(); runTest(args[0]); } public void pauseXlet() {}
     * public void destroyXlet(boolean) {} public void runTest(String test) {
     * boolean ok = true; try { if ("testGetAppClassLoader".equals(test))
     * testGetAppClassLoader(); else if ("testGetAppID".equals(test))
     * testGetAppID(); else if ("testGetAppsDatabase".equals(test))
     * testGetAppsDatabase(); else if ("testGetAppManagerProxy".equals(test))
     * testGetAppManagerProxy(); else ok = false; } catch(Throwable t) { throw =
     * t; } if (!ok) throw new XletStateChangeException("Invalid test"); }
     * 
     * public void testGetAppClassLoader() throws Exception { }
     * 
     * public void testGetAppID() throws Exception { }
     * 
     * public void testGetAppsDatabase() throws Exception { }
     * 
     * public void testGetAppManagerProxy() throws Exception { } }
     */
}
