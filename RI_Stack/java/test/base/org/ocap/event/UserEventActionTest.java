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
import org.cablelabs.test.TestUtils;
import org.dvb.application.AppID;

/**
 * Tests UserEvent implementation
 * 
 * @author Aaron Kamienski
 */
public class UserEventActionTest extends TestCase
{
    /**
     * Verifies that there are no public fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(UserEventAction.class);
    }

    /**
     * Tests UserEventAction(UserEvent, AppID[]).
     * <ul>
     * <li>(UserEvent, AppID[])
     * <li>(UserEvent, null)
     * <li>(null, AppID[])
     * <li>(null, null)
     * </ul>
     */
    public void testContructor()
    {
        for (int e = 0; e < events.length; ++e)
        {
            for (int i = 0; i < ids.length; ++i)
            {
                checkConstructor("UserEventAction(" + events[e] + "," + ids[i] + ")", new UserEventAction(events[e],
                        ids[i]), events[e], ids[i]);
            }
        }
    }

    /**
     * Implements assertEquals() for UserEvents.
     */
    public void assertEquals(String msg, UserEvent e1, UserEvent e2)
    {
        if (e1 == null)
            assertSame(msg + " Expected null UserEvent", e1, e2);
        else
        {
            assertNotNull(msg + " Expected non-null UserEvent", e2);
            assertSame(msg + " UserEvent: Unexpected sourc", e1.getSource(), e2.getSource());
            assertEquals(msg + " UserEvent: Unexpected code", e1.getCode(), e2.getCode());
            assertEquals(msg + " UserEvent: Unexpected keyChar", e1.getKeyChar(), e2.getKeyChar());
            assertEquals(msg + " UserEvent: Unexpected modifiers", e1.getModifiers(), e2.getModifiers());
            assertEquals(msg + " UserEvent: Unexpected family", e1.getFamily(), e2.getFamily());
            assertEquals(msg + " UserEvent: Unexpected when", e1.getWhen(), e2.getWhen());

            // Following is done by platform, not class
            /*
             * assertFalse(msg+" Should not propagate UserDefinedUserEvent", e2
             * instanceof UserDefinedUserEvent);
             */
        }
    }

    /**
     * Implements assertEquals() for AppID[].
     */
    public void assertEquals(String msg, AppID[] id1, AppID[] id2)
    {
        if (id1 == null)
            assertSame(msg + " Expected null AppIDs", id1, id2);
        else
        {
            assertEquals(msg + " Expected same number of AppIDs\n", id1.length, id2.length);
            for (int i = 0; i < id1.length; ++i)
            {
                assertEquals(msg + " AppID[" + i + "]: Expected equal", id1[i], id2[i]);
            }
        }
    }

    /**
     * Checks construction of UserEventAction.
     */
    private void checkConstructor(String msg, UserEventAction uea, UserEvent e, AppID[] ids)
    {
        assertEquals(msg, e, uea.getEvent());
        assertEquals(msg, ids, uea.getAppIDs());
    }

    /**
     * Tests setEvent/getEvent.
     */
    public void testEvent()
    {
        for (int i = 0; i < events.length; ++i)
        {
            UserEventAction uea = new UserEventAction(events[i], ids[0]);

            UserEvent ue = uea.getEvent();
            assertEquals("Expected event passed to constructor", events[i], ue);
            assertEquals("Expected same event", ue, uea.getEvent());

            // Set to other
            ue = events[(i + 1) % events.length];
            uea.setEvent(ue);
            assertEquals("Expected new event", ue, uea.getEvent());

            // Set back
            uea.setEvent(events[i]);
            assertEquals("Expected original event", events[i], uea.getEvent());
        }
    }

    /**
     * Tests setAppIDs/getAppIDs.
     */
    public void testAppIDs()
    {
        for (int i = 0; i < ids.length; ++i)
        {
            UserEventAction uea = new UserEventAction(events[0], ids[i]);

            AppID[] id = uea.getAppIDs();
            assertEquals("Expected array passed to contructor", ids[i], id);
            assertEquals("Expected same array", id, uea.getAppIDs());

            // Set to other
            id = ids[(i + 1) % ids.length];
            uea.setAppIDs(id);
            assertEquals("Expected new array", id, uea.getAppIDs());

            // Set back
            uea.setAppIDs(ids[i]);
            assertEquals("Expected original array", ids[i], uea.getAppIDs());
        }
    }

    private static final UserEvent events[] = { new UserEvent("", 0, '\0', 0L), new UserEvent("", 0, 0, 0, 0, 0L),
            new UserDefinedUserEvent(), null };

    private static final AppID[] ids[] = { new AppID[] { new AppID(10, 20), new AppID(3, 4), }, new AppID[] {}, null };

    static class UserDefinedUserEvent extends UserEvent
    {
        public UserDefinedUserEvent()
        {
            super("", 0, 0, 0, 0, 0L);
        }
    }

    /* ===== Boilerplate ===== */
    public UserEventActionTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(UserEventActionTest.class);
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
