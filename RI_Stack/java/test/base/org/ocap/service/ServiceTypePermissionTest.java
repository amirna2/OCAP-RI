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

package org.ocap.service;

import junit.framework.*;
import java.security.BasicPermission;
import org.cablelabs.test.TestUtils;
import javax.tv.service.selection.SelectPermission;

/**
 * Tests ServiceTypePermission.
 * 
 * Test MSO, MFR, and BROADCAST. Actions of "*" and "own".
 * 
 * @author Aaron Kamienski
 */
public class ServiceTypePermissionTest extends TestCase
{
    /**
     * Test public static fields.
     */
    public void testFields() throws Exception
    {
        TestUtils.testNoAddedFields(ServiceTypePermission.class, FIELDS);

        assertEquals(NAMES[0], ServiceTypePermission.MFR);
        assertEquals(NAMES[1], ServiceTypePermission.MSO);
        assertEquals(NAMES[2], ServiceTypePermission.BROADCAST);
    }

    /**
     * Tests constructor.
     */
    public void testConstructor_invalid()
    {
        try
        {
            new ServiceTypePermission(null, "*");
            fail("Expected null type to fail");
        }
        catch (Exception e)
        {
        }

        try
        {
            new ServiceTypePermission("*", null);
            fail("Expected null actions to fail");
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Tests getName().
     */
    public void testGetName() throws Exception
    {
        for (int i = 0; i < NAMES.length; ++i)
        {
            ServiceTypePermission p = new ServiceTypePermission(NAMES[i], ACTIONS[0]);

            assertEquals("Unexpected name", NAMES[i], p.getName());
            assertEquals("Unexpected name", NAMES[i], p.getName());
        }
    }

    /**
     * Tests getActions().
     */
    public void testGetActions() throws Exception
    {
        for (int i = 0; i < ACTIONS.length; ++i)
        {
            ServiceTypePermission p = new ServiceTypePermission(NAMES[0], ACTIONS[i]);
            assertEquals("Unexpected action", ACTIONS[i], p.getActions());
            assertEquals("Unexpected act", ACTIONS[i], p.getActions());
        }
    }

    /**
     * Tests implies().
     */
    public void testImplies() throws Exception
    {
        String[] types = { NAMES[0], NAMES[1], NAMES[2], "*" };

        // Imply same
        for (int type = 0; type < types.length; ++type)
        {
            for (int action = 0; action < ACTIONS.length; ++action)
            {
                ServiceTypePermission p = new ServiceTypePermission(types[type], ACTIONS[action]);
                assertTrue("Permission should imply itself " + p, p.implies(p));
                assertTrue("Permission should imply same kind of permission " + p, p.implies(new ServiceTypePermission(
                        p.getName(), p.getActions())));
            }
        }

        // Implied by "*"
        ServiceTypePermission all = new ServiceTypePermission("*", "*");
        for (int type = 0; type < types.length; ++type)
        {
            for (int action = 0; action < ACTIONS.length; ++action)
            {
                ServiceTypePermission p = new ServiceTypePermission(types[type], ACTIONS[action]);

                ServiceTypePermission allType = new ServiceTypePermission("*", ACTIONS[action]);
                assertTrue("type=\"*\" should imply all types " + p, allType.implies(p));

                ServiceTypePermission allAction = new ServiceTypePermission(types[type], "*");
                assertTrue("action=\"*\" should imply all actions " + p, allAction.implies(p));

                assertTrue("type=\"*\" and action=\"*\" imply all ServiceTypePermission", all.implies(p));
            }
        }
    }

    /**
     * Tests implies() w/ SelectPermission.
     */
    public void testImplies_select() throws Exception
    {
        // SelectPermission(loc, action)
        String[] locs = { "ocap://0x10000", "ocap://0x1FFFF", "ocap://0x20000", "ocap://0xFFFFFF", "ocap://0x1",
                "ocap://0x1234", "ocap://0xFFFF", "ocap://f=0xabcdef.0x23", "ocap://n=WB", };
        String[] types = { ServiceTypePermission.MFR, ServiceTypePermission.MFR, ServiceTypePermission.MSO,
                ServiceTypePermission.MSO, ServiceTypePermission.BROADCAST, ServiceTypePermission.BROADCAST,
                ServiceTypePermission.BROADCAST, ServiceTypePermission.BROADCAST, ServiceTypePermission.BROADCAST, };

        for (int loc = 0; loc < locs.length; ++loc)
        {
            for (int action = 0; action < ACTIONS.length; ++action)
            {
                SelectPermission p = new SelectPermission(locs[loc], ACTIONS[action]);
                ServiceTypePermission tp;

                for (int type = 0; type < types.length; ++type)
                {
                    tp = new ServiceTypePermission(types[type], ACTIONS[action]);

                    assertEquals("Unxpected " + types[type] + ":" + ACTIONS[action] + " implies " + locs[loc] + ":"
                            + ACTIONS[action], types[type].equals(types[loc]), tp.implies(p));
                }

                // type=='*'
                tp = new ServiceTypePermission("*", ACTIONS[action]);
                assertTrue("Expected type=\"*\" to imply " + p, tp.implies(p));
                // action=='*'
                tp = new ServiceTypePermission(types[loc], "*");
                assertTrue("Expected type=\"*\" to imply " + p, tp.implies(p));
            }
        }
    }

    /**
     * Tests implies().
     */
    public void testImplies_not() throws Exception
    {
        // Not same class
        // Not same type
        // Not same action
        for (int type = 0; type < NAMES.length; ++type)
        {
            ServiceTypePermission p;
            for (int action = 0; action < ACTIONS.length; ++action)
            {
                p = new ServiceTypePermission(NAMES[type], ACTIONS[action]);
                assertFalse("Should not imply permission of different class " + p, p.implies(new DummyPermission(
                        p.getName(), p.getActions())));

                for (int type2 = 0; type2 < NAMES.length; ++type2)
                {
                    if (type != type2)
                    {
                        assertFalse("Should not imply different type " + p, p.implies(new ServiceTypePermission(
                                NAMES[type2], ACTIONS[action])));
                    }
                }
                assertFalse("Should not imply type=\"*\" " + p, p.implies(new ServiceTypePermission("*",
                        ACTIONS[action])));
            }

            p = new ServiceTypePermission(NAMES[type], "own");
            assertFalse("Should not imply action=\"*\" " + p, p.implies(new ServiceTypePermission(NAMES[type], "*")));
        }
    }

    /**
     * Tests equals().
     */
    public void testEquals() throws Exception
    {
        ServiceTypePermission p = new ServiceTypePermission(NAMES[0], ACTIONS[0]);

        // self
        assertEquals("Permission should equal itself", p, p);
        // equivalent
        assertEquals("Permission should equal same kind of permission", p, new ServiceTypePermission(p.getName(),
                p.getActions()));
        // not different
        assertFalse("Permission should not equal one with different name", p.equals(new ServiceTypePermission(NAMES[1],
                ACTIONS[0])));
        assertFalse("Permission should not equal one with different action", p.equals(new ServiceTypePermission(
                NAMES[0], ACTIONS[1])));
        // not another type
        assertFalse("Permission should not equal one of different class", p.equals(new DummyPermission(p.getName(),
                p.getActions())));
        // not null
        assertFalse("Permission should not equal null", p.equals(null));
    }

    /**
     * Tests hashCode().
     */
    public void testHashCode() throws Exception
    {
        ServiceTypePermission p = new ServiceTypePermission(NAMES[1], ACTIONS[1]);

        assertEquals("HashCode should return same value on successive calls", p.hashCode(), p.hashCode());
        assertEquals("HashCode should be the same for equivalent permissions", p.hashCode(),
                (new ServiceTypePermission(p.getName(), p.getActions())).hashCode());
        assertFalse("HashCode should be different for different name", p.hashCode() == (new ServiceTypePermission(
                NAMES[0], p.getActions())).hashCode());
        assertFalse("HashCode should be different for different actions", p.hashCode() == (new ServiceTypePermission(
                p.getName(), ACTIONS[0])).hashCode());
        assertFalse("HashCode should be different for different permissions", p.hashCode() == (new DummyPermission(
                p.getName(), p.getActions())).hashCode());
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(ServiceTypePermissionTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new ServiceTypePermissionTest(tests[i]));
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ServiceTypePermissionTest.class);
        return suite;
    }

    public ServiceTypePermissionTest(String name)
    {
        super(name);
    }

    private String[] FIELDS = { "MFR", "MSO", "BROADCAST" };

    private String[] NAMES = { "abstract.manufacturer", "abstract.mso", "broadcast" };

    private String[] ACTIONS = { "*", "own" };

    private class DummyPermission extends BasicPermission
    {
        public DummyPermission(String name, String actions)
        {
            super(name, actions);
        }
    }
}
