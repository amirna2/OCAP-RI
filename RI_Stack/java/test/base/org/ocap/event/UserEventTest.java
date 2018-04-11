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

package org.ocap.event;

import junit.framework.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import java.security.Permission;
import org.dvb.event.UserEventTest.*;
import java.awt.event.KeyEvent;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;
import org.ocap.system.MonitorAppPermission;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;

/**
 * Tests UserEvent implementation
 */
//findbugs complains about this pattern - shadowing superclass' name.
//Unfortunately, its a common pattern in the RI (so we ignore it).
public class UserEventTest extends org.dvb.event.UserEventTest
{
    /**
     * Tests UserEvent(Object,int,int,int,int,long) constructor.
     */
    public void testContructor()
    {
        // Just verify that no exceptions are thrown
        new UserEvent("", 0, 0, 0, 0, 0L);
    }

    /**
     * Tests UserEvent(Object,int,char,long) constructor.
     */
    public void testConstructor_typed()
    {
        // Just verify that no exceptions are thrown
        new UserEvent("", 0, '\0', 0L);
    }

    /**
     * Tests setCode().
     */
    public void testSetCode()
    {
        UserEvent ue = userevent;

        ue.setCode(KeyEvent.VK_UP);
        assertEquals("Retrieved value should be set value", KeyEvent.VK_UP, ue.getCode());

        ue.setCode(KeyEvent.VK_DOWN);
        assertEquals("Retrieved value should be set value", KeyEvent.VK_DOWN, ue.getCode());
    }

    /**
     * Tests setKeyChar().
     */
    public void testSetKeyChar()
    {
        UserEvent ue = userevent;

        ue.setKeyChar('A');
        assertEquals("Retrieved value should be set value", 'A', ue.getKeyChar());

        ue.setKeyChar('\0');
        assertEquals("Retrieved value should be set value", '\0', ue.getKeyChar());
    }

    /**
     * Tests setModifiers(). This was removed, so test that no such method
     * exists.
     */
    public void testSetModifiers()
    {
        Class userEventClass = UserEvent.class;

        try
        {
            userEventClass.getMethod("setModifiers", new Class[] { int.class });
            fail("Did not expect a setModifiers() method, removed in I11");
        }
        catch (NoSuchMethodException e)
        {
        }
    }

    private void checkPermission(String method, Permission perm)
    {
        assertNotNull("SecurityManager should've been consulted by " + method, perm);
        assertTrue("MonitorAppPermission should've been requested by " + method, perm instanceof MonitorAppPermission);
        assertEquals("MonitorAppPermission(\"filterUserEvents\") expected from " + method, "filterUserEvents",
                perm.getName());
    }

    /**
     * Tests set*() security checks.
     */
    public void testSetSecurity()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);

        try
        {

            UserEvent ue = userevent;

            sm.clear();
            ue.setCode(KeyEvent.VK_5);
            checkPermission("setCode()", sm.perm);

            sm.clear();
            ue.setKeyChar(CHAR_UNDEFINED);
            checkPermission("setKeyChar()", sm.perm);
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    public static class DummySecurityManager extends NullSecurityManager
    {
        public Permission perm;

        public synchronized void checkPermission(Permission p)
        {
            perm = p;
        }

        public void clear()
        {
            perm = null;
        }
    }

    /**
     * Tests UserEvent generated with the type!=KEY_TYPED constructor.
     */
    public static class OCPressedEventTest extends PressedEventTest
    {
        public OCPressedEventTest(String name, ImplFactory f)
        {
            super(name, f);
        }

        protected org.dvb.event.UserEvent createUserEvent(UserEventParams params)
        {
            params.keyChar = CHAR_UNDEFINED;
            return new UserEvent(params.source, params.family, params.type, params.code, params.modifiers, params.when);
        }
    }

    /**
     * Tests UserEvent generated with the type==KEY_TYPED constructor.
     */
    public static class OCTypedEventTest extends PressedEventTest
    {
        public OCTypedEventTest(String name, ImplFactory f)
        {
            super(name, f);
        }

        protected org.dvb.event.UserEvent createUserEvent(UserEventParams params)
        {
            params.type = KeyEvent.KEY_TYPED;
            params.modifiers = 0;
            params.code = KeyEvent.VK_UNDEFINED;
            return new UserEvent(params.source, params.family, params.keyChar, params.when);
        }
    }

    public static final int MANDATORY[] = { KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
            KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_ENTER,
            KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            // Removed per I09
            // HRcEvent.VK_COLORED_KEY_0, HRcEvent.VK_COLORED_KEY_1,
            // HRcEvent.VK_COLORED_KEY_2, HRcEvent.VK_COLORED_KEY_3,
            // Added per I09
            KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN, };

    /* ===== Boilerplate ===== */
    public UserEventTest(String name)
    {
        super(name);
    }

    protected UserEvent userevent;

    protected void setUp() throws Exception
    {
        super.setUp();
        userevent = new UserEvent("", 0, 0, 0, 0, 0L);
    }

    protected void tearDown() throws Exception
    {
        userevent = null;
        super.tearDown();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(UserEventTest.class);
        return isuite(new TestSuite(UserEventTest.class), new Class[] { OCPressedEventTest.class,
                OCTypedEventTest.class });
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
