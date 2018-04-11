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

package org.dvb.event;

import junit.framework.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import org.cablelabs.test.TestUtils;
import java.awt.event.KeyEvent;

/**
 * Tests UserEvent implementation
 */
public class UserEventTest extends TestCase
{
    /**
     * The value to be used for CHAR_UNDEFINED. This should be zero for
     * OCAP-1.0, which is based upon JDK 1.1.8.
     */
    public static final char CHAR_UNDEFINED = 0;

    private static final String[] fieldNames = { "UEF_KEY_EVENT", };

    private static final int[] fieldValues = { 1, };

    public static boolean isEquals(UserEvent ue0, UserEvent ue1)
    {
        return ue0 == ue1
                || (ue0.getFamily() == ue1.getFamily()
                // && ue0.getSource() == ue1.getSource()
                        && ue0.getType() == ue1.getType() && ue0.getCode() == ue1.getCode() && ue0.getKeyChar() == ue1.getKeyChar()
                // && ue0.getModifiers() == ue1.getModifiers()
                // && ue0.getWhen() == ue1.getWhen()
                );
    }

    public static void assertEquals(UserEvent ue0, UserEvent ue1)
    {
        if (ue0 == ue1) return;
        assertSame("UserEvent.getSource expected same", ue0.getSource(), ue1.getSource());
        assertEquals("UserEvent.getFamily() expected same", ue0.getFamily(), ue1.getFamily());
        assertEquals("UserEvent.getType() expected same", ue0.getType(), ue1.getType());
        assertEquals("UserEvent.getCode() expected same", ue0.getCode(), ue1.getCode());
        assertEquals("UserEvent.getKeyChar() expected same", ue0.getKeyChar(), ue1.getKeyChar());
        assertEquals("UserEvent.getModifiers() expected same", ue0.getModifiers(), ue1.getModifiers());
        assertEquals("UserEvent.getWhen() expected same", ue0.getWhen(), ue1.getWhen());
    }

    /**
     * Verifies there are no added public fields.
     */
    public void testNoAddedFields()
    {
        TestUtils.testNoAddedFields(UserEvent.class, fieldNames);
    }

    /**
     * Verifies value of public constants.
     */
    public void testPublicConstants()
    {
        TestUtils.testFieldValues(UserEvent.class, fieldNames, fieldValues);
    }

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

    public static class UserEventParams implements ImplFactory, Cloneable
    {
        public Object source;

        public int family;

        public int type;

        public int code;

        public char keyChar;

        public int modifiers;

        public long when;

        public UserEventParams(Object source, int family, int type, int code, char keyChar, int modifiers, long when)
        {
            this.source = source;
            this.family = family;
            this.type = type;
            this.code = code;
            this.keyChar = keyChar;
            this.modifiers = modifiers;
            this.when = when;
        }

        public Object createImplObject()
        {
            try
            {
                return this.clone();
            }
            catch (CloneNotSupportedException e)
            {
                fail("Could not clone UserEventParams");
                return null; // not reachable...
            }
        }
    }

    public static abstract class SubTest extends InterfaceTestCase
    {
        public SubTest(String name, ImplFactory f)
        {
            super(name, UserEventParams.class, f);
        }

        protected UserEventParams getUserEventParams()
        {
            return params = (UserEventParams) createImplObject();
        }

        protected UserEventParams params;

        protected UserEvent userevent;

        protected abstract UserEvent createUserEvent(UserEventParams params);

        protected void setUp() throws Exception
        {
            super.setUp();
            params = (UserEventParams) createImplObject();
            userevent = createUserEvent(params);
        }

        protected void tearDown() throws Exception
        {
            params = null;
            userevent = null;
            super.tearDown();
        }

        /**
         * Tests getSource().
         */
        public void testGetSource()
        {
            assertSame("Incorrect value", params.source, userevent.getSource());
        }

        /**
         * Tests getCode().
         */
        public void testGetCode()
        {
            this.assertEquals("Incorrect value", params.code, userevent.getCode());
        }

        /**
         * Tests getFamily().
         */
        public void testGetFamily()
        {
            this.assertEquals("Incorrect value", params.family, userevent.getFamily());
        }

        /**
         * Tests getKeyChar().
         */
        public void testGetKeyChar()
        {
            this.assertEquals("Incorrect value", params.keyChar, userevent.getKeyChar());
        }

        /**
         * Tests getType().
         */
        public void testGetType()
        {
            this.assertEquals("Incorrect value", params.type, userevent.getType());
        }

        /**
         * Tests getWhen().
         */
        public void testGetWhen()
        {
            this.assertEquals("Incorrect value", params.when, userevent.getWhen());
        }

        /**
         * Tests getModifiers().
         */
        public void testGetModifiers()
        {
            this.assertEquals("Incorrect value", params.modifiers, userevent.getModifiers());
        }

        /**
         * Tests isAltDown().
         */
        public void testIsAltDown()
        {
            this.assertEquals("Incorrect value", (params.modifiers & KeyEvent.ALT_MASK) != 0, userevent.isAltDown());
        }

        /**
         * Tests isControlDown().
         */
        public void testIsControlDown()
        {
            this.assertEquals("Incorrect value", (params.modifiers & KeyEvent.CTRL_MASK) != 0,
                    userevent.isControlDown());
        }

        /**
         * Tests isMetaDown().
         */
        public void testIsMetaDown()
        {
            this.assertEquals("Incorrect value", (params.modifiers & KeyEvent.META_MASK) != 0, userevent.isMetaDown());
        }

        /**
         * Tests isShiftDown().
         */
        public void testIsShiftDown()
        {
            this.assertEquals("Incorrect value", (params.modifiers & KeyEvent.SHIFT_MASK) != 0, userevent.isShiftDown());
        }
    }

    /**
     * Tests UserEvent generated with the type==KEY_TYPED constructor.
     */
    public static class TypedEventTest extends SubTest
    {
        public TypedEventTest(String name, ImplFactory f)
        {
            super(name, f);
        }

        protected UserEvent createUserEvent(UserEventParams params)
        {
            params.type = KeyEvent.KEY_TYPED;
            params.modifiers = 0;
            params.code = KeyEvent.VK_UNDEFINED;
            return new UserEvent(params.source, params.family, params.keyChar, params.when);
        }
    }

    /**
     * Tests UserEvent generated with the type!=KEY_TYPED constructor.
     */
    public static class PressedEventTest extends SubTest
    {
        public PressedEventTest(String name, ImplFactory f)
        {
            super(name, f);
        }

        protected UserEvent createUserEvent(UserEventParams params)
        {
            params.keyChar = CHAR_UNDEFINED;
            return new UserEvent(params.source, params.family, params.type, params.code, params.modifiers, params.when);
        }
    }

    /* ===== Boilerplate ===== */
    public UserEventTest(String name)
    {
        super(name);
    }

    private static final UserEventParams[] TEST_PARAMS = {
            new UserEventParams("", -1, -1, -1, '\u7FFF', 0xFFFFFFFF, 0x7FFFFFFFFFFFFFFFL),
            new UserEventParams("", 0, 0, 0, '\0', 0, 0L),
            new UserEventParams("Dummy1", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, '\n',
                    KeyEvent.ALT_MASK | KeyEvent.META_MASK, 1234567890L),
            new UserEventParams("Dummy2", UserEvent.UEF_KEY_EVENT + 1, KeyEvent.KEY_RELEASED, KeyEvent.VK_A, 'A',
                    KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK, 9876543210L), };

    public static Test isuite(TestSuite suite, Class classes[])
    {
        for (int i = 0; i < classes.length; ++i)
        {
            InterfaceTestSuite is = new InterfaceTestSuite(classes[i]);

            String name = classes[i].getName();
            int idx = name.lastIndexOf(".");
            if (idx >= 0) name = name.substring(idx + 1);
            is.setName(name);

            for (int j = 0; j < TEST_PARAMS.length; ++j)
                is.addFactory(TEST_PARAMS[j]);
            suite.addTest(is);
        }

        return suite;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(UserEventTest.class);
        return isuite(new TestSuite(UserEventTest.class), new Class[] { PressedEventTest.class, TypedEventTest.class });
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
