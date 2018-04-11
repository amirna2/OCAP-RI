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

package org.ocap.system;

import org.cablelabs.test.ProxySecurityManager;

import java.io.File;
import java.io.IOException;
import java.security.Permission;
import java.util.Enumeration;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppProxyTest.DummySecurityManager;

/**
 * Tests the RegisteredApiManager.
 * 
 * @author Aaron Kamienski
 */
public class RegisteredApiManagerTest extends TestCase
{
    /**
     * Tests getInstance().
     */
    public void testGetInstance()
    {
        RegisteredApiManager ram = RegisteredApiManager.getInstance();

        assertNotNull("Expected non-null return value", ram);
        assertSame("Expected same return value for multiple calls", ram, RegisteredApiManager.getInstance());
    }

    /**
     * Tests register(). Also tests getNames()/getVersion().
     * 
     * @todo disabled per 4598
     */
    public void xxxtestRegister() throws Exception
    {
        // Register two APIs and ensure that they are there
        // Then unregister them
        String name = getName() + 1;
        String ver = "0.1";
        String name2 = getName() + 2;
        String ver2 = "0.1a";

        int length = checkApi(name, ver, -1, false);

        // Register one
        ram.register(name, ver, new File(APIDIR + "test-scdf.xml"), (short) 100);

        try
        {
            length = checkApi(name, ver, length + 1, true);
            checkApi(name2, ver2, -1, false);

            // Register a 2nd one
            ram.register(name2, ver2, new File(APIDIR + "test-scdf.xml"), (short) 200);

            try
            {
                length = checkApi(name2, ver2, length + 1, true);
                checkApi(name, ver, -1, true);
            }
            finally
            {
                ram.unregister(name2);
            }
        }
        finally
        {
            ram.unregister(name);
        }
    }

    /**
     * Tests unregister(). Also tests getNames()/getVersion().
     * 
     * @todo disabled per 4598
     */
    public void xxxtestUnregister() throws Exception
    {
        String name = getName() + 1;
        String ver = "0.1b";
        String name2 = getName() + 2;
        String ver2 = "0.1c";

        int origLength = checkApi(name, ver, -1, false);
        int length = origLength;
        checkApi(name2, ver2, -1, false);
        try
        {
            ram.register(name, ver, new File(APIDIR + "test-scdf.xml"), (short) 100);
            length = checkApi(name, ver, length + 1, true);
            ram.register(name2, ver2, new File(APIDIR + "test-scdf.xml"), (short) 200);
            length = checkApi(name2, ver2, length + 1, true);

            // Unregister one
            ram.unregister(name);

            // Should not be able to find first
            length = checkApi(name, ver, length - 1, false);

            // Unregister other
            ram.unregister(name2);

            // Should not be able to find second
            length = checkApi(name, ver, length - 2, false);

            assertEquals("Did not remove all apis that were added", origLength, length);
        }
        finally
        {
            unregister(name);
            unregister(name2);
        }
    }

    /**
     * Tests register(), called with same version of an API.
     * 
     * @todo disabled per 4598
     */
    public void xxxtestRegister_same() throws Exception
    {
        // Register two APIs and ensure that they are there
        // Then unregister them
        String name = getName();
        String ver = "0.A";

        int length = checkApi(name, ver, -1, false);

        // Register one
        ram.register(name, ver, new File(APIDIR + "test-scdf.xml"), (short) 100);

        try
        {
            length = checkApi(name, ver, length + 1, true);

            // Register a 2nd one
            ram.register(name, ver, new File(APIDIR + "test-scdf.xml"), (short) 100);

            length = checkApi(name, ver, length, true);
        }
        finally
        {
            ram.unregister(name);
        }
    }

    /**
     * Tests register(), replacing an existing API. Also tests
     * getNames()/getVersion().
     * 
     * @todo disabled per 4598
     */
    public void xxxtestRegister_replace() throws Exception
    {
        // Register an API and ensure that it is there
        // Then replace and ensure that it is there
        // And ensure that the old one isn't there anymore
        String name = getName();
        String ver1 = "0.2";
        String ver2 = "0.1";

        int length = checkApi(name, ver1, -1, false);

        // Register one
        ram.register(name, ver1, new File(APIDIR + "test-scdf.xml"), (short) 100);

        try
        {
            length = checkApi(name, ver1, length + 1, true);

            // Register a 2nd one
            ram.register(name, ver2, new File(APIDIR + "test-scdf.xml"), (short) 200);

            length = checkApi(name, ver2, length, true);
        }
        finally
        {
            ram.unregister(name);
        }
    }

    /**
     * Tests register(), IllegalArgumentException
     * 
     * @todo disabled per 4598
     */
    public void xxxtestRegister_illegal() throws Exception
    {
        String name = getName();

        short[] invalid = { -1, 256 };
        short[] priorities = { 0, 11, 255 };
        short[] reserved = { 1, 10 };

        try
        {
            for (int i = 0; i < invalid.length; ++i)
            {
                try
                {
                    ram.register(name, "invalid" + i, new File(APIDIR + "test-scdf.xml"), invalid[i]);
                    fail("Exception expected for illegal priority");
                }
                catch (IllegalArgumentException e)
                {
                }
            }

            for (int i = 0; i < reserved.length; ++i)
            {
                try
                {
                    ram.register(name, "invalid" + i, new File(APIDIR + "test-scdf.xml"), reserved[i]);
                    fail("Exception expected for reserved priority");
                }
                catch (IllegalArgumentException e)
                {
                }
            }

            for (int i = 0; i < priorities.length; ++i)
            {
                ram.register(name, "valid" + i, new File(APIDIR + "test-scdf.xml"), priorities[i]);
                unregister(name);
            }
        }
        finally
        {
            unregister(name);
        }
    }

    /**
     * Tests register(), IllegalStateException
     */
    public void XtestRegister_inUse()
    {
        // Test in impl test
    }

    /**
     * Tests register(), IOException due to problems with SCDF.
     */
    public void testRegister_no_scdf()
    {
        String name = getName();

        // No such scdf
        try
        {
            ram.register(name, "1", new File("/no/such/file"), (short) 100);
            fail("Expected IOException for non-existent scdf");
        }
        catch (IOException e)
        {
        }
        finally
        {
            unregister(name);
        }
    }

    /**
     * Tests register(), IOException due to problems with SCDF.
     */
    public void testRegister_not_scdf()
    {
        String name = getName();

        // Invalid scdf (definitely NOT an SCDF)
        try
        {
            ram.register(name, "1", new File(APIDIR + "org/cablelabs/api/apitest/TestApi.class"), (short) 100);
            fail("Expected IOException for non-existent scdf");
        }
        catch (IOException e)
        {
        }
        finally
        {
            unregister(name);
        }
    }

    /**
     * Tests register(), IOException due to problems with SCDF.
     */
    public void testRegister_error_scdf()
    {
        String name = getName();

        // Invalid scdf (close but not cigar)
        try
        {
            ram.register(name, "1", new File(APIDIR + "error.xml"), (short) 100);
            fail("Expected IOException for invalid scdf");
        }
        catch (IOException e)
        {
        }
        finally
        {
            unregister(name);
        }
    }

    /**
     * Tests register(), IOException due to problems with referenced files.
     */
    public void testRegister_no_file()
    {
        String name = getName();

        // Problems with files described by scdf

        // Doesn't exist
        try
        {
            ram.register(name, "1", new File(APIDIR + "nonexist.xml"), (short) 100);
            fail("Expected IOException for non-existent file");
        }
        catch (IOException e)
        {
        }
        finally
        {
            unregister(name);
        }
    }

    /**
     * Tests register(), IOException due to problems with referenced files.
     */
    public void XtestRegister_noload_file()
    {
        String name = getName();

        // Problems with files described by scdf

        // Cannot be loaded
        // ????? How to test this?????
        // May need to leave up to impl test.
        try
        {
            ram.register(name, "1", new File(APIDIR + "noload.xml"), (short) 100);
            fail("Expected IOException for non-existent scdf");
        }
        catch (IOException e)
        {
        }
        finally
        {
            unregister(name);
        }
    }

    /**
     * Tests register(), IOException due to problems with referenced files.
     */
    public void XtestRegister_unsigned_file()
    {
        String name = getName();

        // Problems with files described by scdf

        // Not correctly signed
        try
        {
            ram.register(name, "1", new File(APIDIR + "unsigned.xml"), (short) 100);
            fail("Expected IOException for non-existent scdf");
        }
        catch (IOException e)
        {
        }
        finally
        {
            unregister(name);
        }
    }

    /**
     * Tests unregister(), IllegalArgumentException.
     */
    public void testUnregister_illegal()
    {
        // No such API
        try
        {
            ram.unregister("noSuchApi");
            fail("Expected IllegalArgumentException for non-existent API");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Tests unregister(), IllegalStateException.
     */
    public void XtestUnregister_inUse()
    {
        // Test in subclass
    }

    /**
     * Tests register() for Security.
     * 
     * @todo disabled per 4598
     */
    public void xxxtestRegister_security()
    {
        doTestSecurity(new Runnable()
        {
            public void run()
            {
                try
                {
                    ram.register(getName(), "0", new File("noSuchFile"), (short) 1);
                }
                catch (IllegalArgumentException e)
                {
                }
                catch (IOException e)
                {
                }
                finally
                {
                    unregister(getName());
                }
            }
        }, null);
    }

    /**
     * Tests unregister() for Security.
     */
    public void testUnregister_security() throws Exception
    {
        // Register one
        ram.register(getName(), "1.3.1_a", new File(APIDIR + "test-scdf.xml"), (short) 100);
        try
        {
            doTestSecurity(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        ram.unregister(getName());
                    }
                    catch (IllegalArgumentException e)
                    {
                    }
                }
            }, null);
        }
        finally
        {
            unregister(getName());
        }
    }

    protected static final String APIDIR;
    static
    {
        //
        // if the snfs directory exists, assume that
        // we should use that for the base directory
        //
        if ((new File("/snfs")).exists())
        {
            APIDIR = "/snfs/qa/xlet/";
        }
        else
        {
            APIDIR = "/syscwd/qa/xlet/";
        }
    }

    /**
     * Tests getNames(). Tests with no registered APIs. Everything else is
     * tested along with other methods.
     */
    public void testGetNames()
    {
        String[] names = ram.getNames();
        assertNotNull("getNames() should not return null", names);
        assertEquals("Did not expect any APIs to be registered", 0, names.length);
    }

    /**
     * Tests getNames() for Security
     */
    public void testGetNames_security()
    {
        doTestSecurity(new Runnable()
        {
            public void run()
            {
                ram.getNames();
            }
        }, null);

    }

    /**
     * Tests getVersions(). Tests with no registered APIs. Everything else is
     * tested along with other methods.
     */
    public void testGetVersions()
    {
        String version = ram.getVersion(getName());
        assertSame("Expected null version for unknown api name", null, version);
    }

    /**
     * Tests getNames() for Security
     */
    public void testGetVersions_security()
    {
        doTestSecurity(new Runnable()
        {
            public void run()
            {
                ram.getVersion(getName());
            }
        }, getName());

    }

    /**
     * Tests getUsedNames(). Actually, expect much of this to be tested by
     * subclass tests. This just tests for non-null array.
     */
    public void XtestGetUsedNames()
    {
        // Untested here
    }

    protected int checkApi(String name, String version, int namesLength, boolean present)
    {
        String[] names = ram.getNames();
        if (namesLength >= 0)
        {
            assertEquals("Unexpected number of names returned", namesLength, names.length);
        }

        boolean found = false;
        for (int i = 0; i < names.length; ++i)
            if (name.equals(names[i]))
            {
                found = true;
                break;
            }
        assertEquals("getNames() " + (found ? "included" : "did not include") + " " + name, present, found);

        String actualVer = ram.getVersion(name);
        if (found)
        {
            assertNotNull("version should not return null", actualVer);
            assertEquals("Unexpected version", version, actualVer);
        }
        else
        {
            assertSame("version should be null", null, actualVer);
        }

        return names.length;
    }

    protected void doTestSecurity(Runnable run, final String useName)
    {
        DummySecurityManager sm = new DummySecurityManager()
        {
            // NOTE: this expects MonAppPerm to be tested first, and only
            // RegApiUserPerm if that fails...
            public void checkPermission(Permission p)
            {
                super.checkPermission(p);
                if (useName != null && p instanceof MonitorAppPermission && "registeredapi.manager".equals(p.getName()))
                    throw new SecurityException("Better ask for RegisteredApiUserPermission as well");
            }
        };
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            sm.p = null;
            sm.all.clear();
            run.run();

            assertNotNull("non-null Permission should be checked", sm.p);

            Permission p = (Permission) sm.all.elementAt(0);
            assertTrue("Permission should be MonitorAppPermission", p instanceof MonitorAppPermission);
            assertEquals("Unexpected permission name", "registeredapi.manager", ((MonitorAppPermission) p).getName());

            if (useName == null)
            {
                for (Enumeration e = sm.all.elements(); e.hasMoreElements();)
                {
                    Object op = e.nextElement();
                    assertFalse("Did not expect any RegisteredApiUserPerms: " + op,
                            op instanceof RegisteredApiUserPermission);
                }
            }
            else
            {
                boolean okay = false;
                for (Enumeration e = sm.all.elements(); e.hasMoreElements();)
                {
                    Object op = e.nextElement();
                    if (op instanceof RegisteredApiUserPermission)
                    {
                        assertEquals("Unexpected permission name", "registeredapi.user." + useName,
                                ((RegisteredApiUserPermission) op).getName());
                        okay = true;
                    }
                }
                assertTrue("Expected RegisteredApiUserPermission", okay);
            }
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    protected void unregister(String name)
    {
        try
        {
            ram.unregister(name);
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    protected RegisteredApiManager getInstance()
    {
        return RegisteredApiManager.getInstance();
    }

    /* ==================== Boilerplate =============== */

    protected RegisteredApiManager ram;

    protected void setUp() throws Exception
    {
        super.setUp();
        ram = getInstance();
    }

    public RegisteredApiManagerTest(String test)
    {
        super(test);
    }

    public static Test suite()
    {
        return new TestSuite(RegisteredApiManagerTest.class);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(RegisteredApiManagerTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new RegisteredApiManagerTest(tests[i]));
            return suite;
        }
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
}
