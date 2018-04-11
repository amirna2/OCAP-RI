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
import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.event.KeyEvent;
import org.havi.ui.event.HRcEvent;

/**
 * Tests UserEventRepository implementation
 */
public class UserEventRepositoryTest extends RepositoryDescriptorTest
{
    /**
     * Compares two sets of UserEvent arrays for equivalence.
     */
    public static void assertEquals(String msg, UserEvent[] events0, UserEvent[] events1)
    {
        assertEquals(msg + "Number of events contained in repository should be the same", events0.length,
                events1.length);

        for (int i = 0; i < events0.length; ++i)
        {
            boolean found = false;
            // Find events0[i] in events1
            for (int j = 0; !found && j < events1.length; ++j)
            {
                if (events1[j] != null && UserEventTest.isEquals(events0[i], events1[j]))
                {
                    // Found the one we want, don't use it anymore
                    events1[j] = null;
                    found = true;
                }
            }
            assertTrue(msg + "Expected to find " + events0[i] + " in UserEvents[]", found);
        }
        for (int j = 0; j < events1.length; ++j)
            assertNull(msg + "All events should've been accounted for", events1[j]);
    }

    /**
     * Compares two UserEventRepositories for equivalence. Requires:
     * <ul>
     * <li>Equals name
     * <li>Same client
     * <li>Same user events
     * </ul>
     */
    public static void assertEquals(String msg, UserEventRepository uer0, UserEventRepository uer1)
    {
        if (uer0 == uer1) return;

        assertNotNull(msg + "UserEventRepository should not be null", uer0);
        assertNotNull(msg + "UserEventRepository should not be null", uer1);

        // Same name
        assertEquals(msg + "Expect same name for UserEventRepositories", uer0.getName(), uer1.getName());

        // Same events
        assertEquals(msg + "Expect same events", uer0.getUserEvent(), uer1.getUserEvent());
    }

    /**
     * Tests the UserEventRepository(String) constructor.
     */
    public void testConstructor()
    {
        UserEventRepository uer = new UserEventRepository("xyz");
        assertEquals("Expected name given in constructor", "xyz", uer.getName());
        assertNotNull("UserEventRepository should not be null", uer.getUserEvent());
        assertEquals("UserEventRepository should initially be empty", 0, uer.getUserEvent().length);
    }

    /**
     * Tests getUserEvent().
     */
    public void testGetUserEvent()
    {
        // Mostly tested in other routines.
        // Simply verify non-null
        UserEvent[] ue0 = userEventRepository.getUserEvent();
        assertNotNull("getUserEvent should not return null", ue0);

        // And equivalence
        UserEvent[] ue1 = userEventRepository.getUserEvent();
        assertEquals("Expected getUserEvent() to return equiv ", ue0, ue1);

        // And modifications to one shouldn't affect later access
        for (int i = 0; i < ue1.length; ++i)
            if (1 == (i & 1)) // arbitrarily clear odd ones
                ue1[i] = null;
        UserEvent[] ue2 = userEventRepository.getUserEvent();
        assertEquals("", ue0, ue2);
    }

    /**
     * Tests getClient().
     */
    public void testGetClient()
    {
        assertNull("No client should be set", userEventRepository.getClient());
    }

    /**
     * Tests getName().
     */
    public void testGetName()
    {
        // Created with name of test class, verify
        assertEquals("getName should return name given on construction", getClass().getName(),
                userEventRepository.getName());
    }

    /**
     * Tests add|removeAllArrowKeys().
     */
    public void testAddRemoveAllArrowKeys()
    {
        int[] arrows = { KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT };

        // Add keys and verify
        for (int i = 0; i < arrows.length; ++i)
            keyset.addKey(arrows[i]);
        userEventRepository.addAllArrowKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());

        // Add again and verify
        userEventRepository.addAllArrowKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());

        // Remove and verify
        for (int i = 0; i < arrows.length; ++i)
            keyset.removeKey(arrows[i]);
        userEventRepository.removeAllArrowKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());
    }

    /**
     * Tests add|removeAllColourKeys().
     */
    public void testAddRemoveAllColourKeys()
    {
        int[] colors = { HRcEvent.VK_COLORED_KEY_0, HRcEvent.VK_COLORED_KEY_1, HRcEvent.VK_COLORED_KEY_2,
                HRcEvent.VK_COLORED_KEY_3, };

        // Add keys and verify
        for (int i = 0; i < colors.length; ++i)
            keyset.addKey(colors[i]);
        userEventRepository.addAllColourKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());

        // Add again and verify
        userEventRepository.addAllColourKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());

        // Remove and verify
        for (int i = 0; i < colors.length; ++i)
            keyset.removeKey(colors[i]);
        userEventRepository.removeAllColourKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());
    }

    /**
     * Tests add|removeAllNumericKeys().
     */
    public void testAddRemoveAllNumericKeys()
    {
        int[] numbers = { KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
                KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9 };

        // Add keys and verify
        for (int i = 0; i < numbers.length; ++i)
            keyset.addKey(numbers[i]);
        userEventRepository.addAllNumericKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());

        // Add again and verify
        userEventRepository.addAllNumericKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());

        // Remove and verify
        for (int i = 0; i < numbers.length; ++i)
            keyset.removeKey(numbers[i]);
        userEventRepository.removeAllNumericKeys();
        keyset.checkUserEvents(userEventRepository.getUserEvent());
    }

    /**
     * Tests add|removeKey().
     */
    public void testAddRemoveKey()
    {
        // Added keys should be retrieved via getUserEvent
        // Keys added more than once should be retrieved once
        for (int i = KeyEvent.VK_A; i < KeyEvent.VK_Z; ++i)
        {
            keyset.addKey(i);
            userEventRepository.addKey(i);

            // Make sure they are the same
            keyset.checkUserEvents(userEventRepository.getUserEvent());
        }

        // Removed keys shouldn't be retrieved via getUserEvent
        for (int i = KeyEvent.VK_A; i < KeyEvent.VK_Z; ++i)
        {
            // Add the key again, should have no effect
            userEventRepository.addKey(i);
            keyset.checkUserEvents(userEventRepository.getUserEvent());

            // Remove the key
            keyset.removeKey(i);
            userEventRepository.removeKey(i);

            keyset.checkUserEvents(userEventRepository.getUserEvent());
        }
    }

    /**
     * Tests add|removeUserEvent().
     */
    public void testAddRemoveUserEvent()
    {
        // Added keys should be retrieved via getUserEvent
        // Keys added more than once should be retrieved once
        for (int i = KeyEvent.VK_A; i < KeyEvent.VK_Z; ++i)
        {
            // pseudo-random generation of UserEvent vars...
            UserEvent ue = ((i & 1) != 0) ? (new UserEvent("", 0, ((i & 2) != 0) ? KeyEvent.KEY_PRESSED
                    : KeyEvent.KEY_RELEASED, i, 0, 0L)) : (new UserEvent("", 0, (char) i, 0L));

            keyset.addUserEvent(ue);
            userEventRepository.addUserEvent(ue);

            // Make sure they are the same
            keyset.checkUserEvents(userEventRepository.getUserEvent());
        }

        // Removed keys shouldn't be retrieved via getUserEvent
        for (int i = KeyEvent.VK_A; i < KeyEvent.VK_Z; ++i)
        {
            UserEvent ue = ((i & 1) != 0) ? (new UserEvent("", 0, ((i & 2) != 0) ? KeyEvent.KEY_PRESSED
                    : KeyEvent.KEY_RELEASED, i, 0, 0L)) : (new UserEvent("", 0, (char) i, 0L));

            // Add the key again, should have no effect
            userEventRepository.addUserEvent(ue);
            keyset.checkUserEvents(userEventRepository.getUserEvent());

            // Remove the key
            keyset.removeUserEvent(ue);
            userEventRepository.removeUserEvent(ue);

            keyset.checkUserEvents(userEventRepository.getUserEvent());
        }
    }

    protected static class KeySet
    {
        protected void checkUserEvents(UserEvent[] events)
        {
            // Verify that each expected UserEvent is found
            for (Enumeration e = keyset.keys(); e.hasMoreElements();)
            {
                KeyValue val = (KeyValue) e.nextElement();
                boolean found = false;
                for (int i = 0; !found && i < events.length; ++i)
                {
                    if (val.isEquals(events[i]))
                    {
                        events[i] = null;
                        found = true;
                    }
                }
                assertTrue("Expected UserEvent not found (" + val + ")", found);
            }
            // Verify that all UserEvent's are accounted for
            for (int i = 0; i < events.length; ++i)
                if (events[i] != null)
                    assertNull("Unexpected UserEvent found " + (new KeyValue(events[i])), events[i]);
        }

        public void addKey(int keycode)
        {
            addKeyValue(KeyEvent.KEY_PRESSED, keycode, UserEventTest.CHAR_UNDEFINED);
            addKeyValue(KeyEvent.KEY_RELEASED, keycode, UserEventTest.CHAR_UNDEFINED);
        }

        public void removeKey(int keycode)
        {
            removeKeyValue(KeyEvent.KEY_PRESSED, keycode, UserEventTest.CHAR_UNDEFINED);
            removeKeyValue(KeyEvent.KEY_RELEASED, keycode, UserEventTest.CHAR_UNDEFINED);
        }

        public void addKeyChar(char keychar)
        {
            addKeyValue(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, keychar);
        }

        public void removeKeyChar(char keychar)
        {
            removeKeyValue(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, keychar);
        }

        public void addUserEvent(UserEvent e)
        {
            addKeyValue(e.getType(), e.getCode(), e.getKeyChar());
        }

        public void removeUserEvent(UserEvent e)
        {
            removeKeyValue(e.getType(), e.getCode(), e.getKeyChar());
        }

        public void addKeyValue(int type, int keyCode, char keyChar)
        {
            KeyValue v = new KeyValue(type, keyCode, keyChar);
            keyset.put(v, v);
        }

        public void removeKeyValue(int type, int keyCode, char keyChar)
        {
            KeyValue v = new KeyValue(type, keyCode, keyChar);
            keyset.remove(v);
        }

        private class KeyValue
        {
            int type;

            int keyCode;

            char keyChar;

            public KeyValue(UserEvent e)
            {
                this(e.getType(), e.getCode(), e.getKeyChar());
            }

            public KeyValue(int type, int keyCode, char keyChar)
            {
                this.type = type;
                this.keyCode = keyCode;
                this.keyChar = keyChar;
            }

            public boolean isEquals(UserEvent e)
            {
                return e != null && type == e.getType() && keyCode == e.getCode() && keyChar == e.getKeyChar();
            }

            public boolean equals(Object obj)
            {
                return obj != null && (obj instanceof KeyValue) && type == ((KeyValue) obj).type
                        && keyCode == ((KeyValue) obj).keyCode && keyChar == ((KeyValue) obj).keyChar;
            }

            public int hashCode()
            {
                return ((int) keyChar << 16) ^ (type << 24) ^ (type >>> 8) ^ keyCode;
            }

            public String toString()
            {
                String typeName;
                switch (type)
                {
                    case KeyEvent.KEY_PRESSED:
                        typeName = "KEY_PRESSED";
                        break;
                    case KeyEvent.KEY_RELEASED:
                        typeName = "KEY_RELEASED";
                        break;
                    case KeyEvent.KEY_TYPED:
                        typeName = "KEY_TYPED";
                        break;
                    default:
                        typeName = "?" + type + "?";
                }
                return super.toString() + "[type=" + typeName + ",code=" + keyCode + ",char='" + keyChar + "']";
            }
        }

        protected Hashtable keyset = new Hashtable();
    }

    protected KeySet keyset;

    protected UserEventRepository userEventRepository;

    protected UserEventRepository createUserEventRepository()
    {
        return new UserEventRepository(getClass().getName());
    }

    /* ===== Boilerplate ===== */
    public UserEventRepositoryTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        keyset = new KeySet();
        userEventRepository = createUserEventRepository();
    }

    protected void tearDown() throws Exception
    {
        keyset = null;
        userEventRepository = null;
        super.tearDown();
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
        TestSuite suite = new TestSuite(UserEventRepositoryTest.class);
        return suite;
    }
}
