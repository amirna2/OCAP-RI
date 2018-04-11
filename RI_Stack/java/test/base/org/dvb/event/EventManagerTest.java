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

import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.FocusManager.DispatchFilter;
import org.cablelabs.impl.manager.FocusManager.FocusContext;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.test.TestUtils;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.application.AppID;
import org.havi.ui.event.HRcEvent;

/**
 * Tests EventManager implementation.
 * <p>
 * Some things to note:
 * <ul>
 * <li>These tests purposefully use different keys in the UserEventRepositories.
 * This is because events are accumulated. And the spec doesn't tell you how
 * they can be removed -- if they can!
 * <li>These tests are essentially for a single app.
 * </ul>
 * 
 * @todo Expand/refactor the testDispatchResourceStatusEvent() test.
 * @todo Implement "interface" test that tests dispatch of events to listener
 *       callbacks. Will be used by TestCase for implementation class (e.g.,
 *       EventMgr) which will provide interface for causing events to be
 *       dispatched and potentially for faking/acquiring the CallerContext.
 */
public class EventManagerTest extends TestCase
{
    /**
     * Ensure Constructor isn't public.
     */
    public void testConstructor()
    {
        TestUtils.testNoPublicConstructors(EventManager.class);
    }

    /**
     * Tests getInstance().
     */
    public void testGetInstance()
    {
        EventManager em = EventManager.getInstance();

        assertNotNull("EventManager should not be null", em);
        assertSame("Repeated calls should return same instance", em, EventManager.getInstance());
    }

    private static interface AddListener
    {
        public void add(EventManager em, UserEventListener uel, UserEventRepository uer);
    }

    private static AddListener ADD_SHARED = new AddListener()
    {
        public void add(EventManager em, UserEventListener uel, UserEventRepository uer)
        {
            em.addUserEventListener(uel, uer);
        }
    };

    private static AddListener ADD_EXCLUSIVE = new AddListener()
    {
        private ResourceClient rc = new Client();

        public void add(EventManager em, UserEventListener uel, UserEventRepository uer)
        {
            assertTrue("Expected to be able to reserve UserEvent", em.addUserEventListener(uel, rc, uer));
        }
    };

    /**
     * Basically an internal test to make sure that other tests can manage AWT
     * focus and dispatch using custom FocusContexts.
     */
    public void testSimpleAWTdispatch() throws Exception
    {
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        Focus focus = new Focus();
        Focus focus2 = new Focus();

        // Request that app be given focus
        // And verify that it can
        fm.requestActivate(focus, true);
        try
        {
            int keyCode = KeyEvent.VK_Z;

            assertTrue("Internal error - could not activate FocusContext", focus.waitForActivated(3000));

            // Verify that it is notified of events
            dispatch(keyCode);
            focus.expectKeyEvents("Internal Error - ", keyCode, 2000L);

            // Move focus
            fm.requestActivate(focus2, true);
            assertTrue("Internal error - could not activate FocusContext", focus2.waitForActivated(3000));
            assertFalse("Internal error - expected other FocusContext to be deactivated", focus.activated);

            // Verify that correct app is notified of events
            dispatch(keyCode = KeyEvent.VK_Y);
            focus2.expectKeyEvents("Internal Error - ", keyCode, 2000L);
            focus.expectNoKeyEvents("Internal Error - ", 500L);
        }
        finally
        {
            fm.notifyDeactivated(focus);
            fm.notifyDeactivated(focus2);
        }
    }

    /**
     * Tests addUserEventListener() with no events in the repository.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListener_emptyRepository() throws Exception
    {
        doTestAddUserEventListener_emptyRepository(ADD_SHARED);
    }

    /**
     * Tests addUserListener (exclusive access) with no events in the
     * repository.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserListenerExclusive_emptyRepository() throws Exception
    {
        doTestAddUserEventListener_emptyRepository(ADD_EXCLUSIVE);
    }

    /**
     * Tests addExclusiveAccessToAWTEvent() with no events in the repository.
     * <p>
     * <i>Single app context.</i> Note that AWTEvents don't function any
     * different in a single app context if the event is reserved or not (either
     * way, they should be received).
     */
    public void testAddExclusiveAccessToAWTEvent_emptyRepository() throws Exception
    {
        EventManager em = eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        ResourceClient rc[] = { new Client(), new Client(), new Client() };
        Focus focus = new Focus();

        try
        {
            // Add multiple clients with same empty repository
            UserEventRepository uer = new UserEventRepository("empty0");
            for (int i = 0; i < rc.length; ++i)
            {
                assertTrue("Expected to be able to reserve AWTEvent: " + i, em.addExclusiveAccessToAWTEvent(rc[i], uer));

                // Dispatch w/out focus
                fm.notifyDeactivated(focus);
                dispatch(KeyEvent.VK_A);
                focus.expectNoKeyEvents("!Focused - ", 500L);

                // Dispatch w/ focus
                int keyCode = KeyEvent.VK_B;
                fm.requestActivate(focus, true);
                assertTrue("Expected to receive focus", focus.waitForActivated(1000));
                dispatch(keyCode);
                focus.expectKeyEvents("Focused - ", keyCode, 2000L);
            }
        }
        finally
        {
            fm.notifyDeactivated(focus);
            for (int i = 0; i < rc.length; ++i)
            {
                em.removeExclusiveAccessToAWTEvent(rc[i]);
            }
        }
    }

    /**
     * Tests addUserEventListener() with no events in the repository.
     * <p>
     * <i>Single app context.</i>
     */
    private void doTestAddUserEventListener_emptyRepository(AddListener add) throws Exception
    {
        EventManager em = eventmanager;
        EventListener uel[] = new EventListener[3];

        try
        {
            // Add multiple listeners with same empty repository
            UserEventRepository uer = new UserEventRepository("empty0");
            for (int i = 0; i < uel.length; ++i)
            {
                uel[i] = new EventListener();
                add.add(em, uel[i], uer);

                // Dispatch
                dispatch(KeyEvent.VK_A);

                // Make sure no listeners were called
                for (int j = 0; j <= i; ++j)
                    uel[j].expectNoUserEvents(500L);
            }
        }
        finally
        {
            for (int i = 0; i < uel.length; ++i)
            {
                if (uel[i] != null) em.removeUserEventListener(uel[i]);
            }
        }
    }

    /**
     * Tests addUserEventListener() with other events in the respository.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListener_otherEvents() throws Exception
    {
        doTestAddUserEventListener_otherEvents(ADD_SHARED);
    }

    /**
     * Tests addUserEventListener() (exclusive) with other events in the
     * respository.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListenerExclusive_otherEvents() throws Exception
    {
        doTestAddUserEventListener_otherEvents(ADD_EXCLUSIVE);
    }

    /**
     * Tests addExclusiveAccessToAWTEvent() with other events in the
     * respository.
     * <p>
     * <i>Single app context.</i> Note that AWTEvents don't function any
     * different in a single app context if the event is reserved or not (either
     * way, they should be received).
     */
    public void testAddExclusiveAccessToAWTEvent_otherEvents() throws Exception
    {
        EventManager em = eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        ResourceClient rc[] = { new Client(), new Client(), new Client() };
        Focus focus = new Focus();

        try
        {
            // Add multiple clients with same empty repository
            UserEventRepository uer = new UserEventRepository("other0");
            uer.addKey(KeyEvent.VK_A);
            uer.addKey(KeyEvent.VK_B);
            for (int i = 0; i < rc.length; ++i)
            {
                assertTrue("Expected to be able to reserve AWTEvent: " + i, em.addExclusiveAccessToAWTEvent(rc[i], uer));

                int keyCode = KeyEvent.VK_C;

                // Dispatch "other" focus is not focused
                fm.notifyDeactivated(focus);
                dispatch(keyCode);

                focus.expectNoKeyEvents("!Focused - ", 500L);

                // Dispatch "other" focus is focused
                fm.requestActivate(focus, true);
                assertTrue("Expected to receive focus", focus.waitForActivated(1000));
                dispatch(keyCode + 1);
                focus.expectKeyEvents("Focused - ", keyCode + 1, 2000L);
            }
        }
        finally
        {
            fm.notifyDeactivated(focus);
            for (int i = 0; i < rc.length; ++i)
            {
                em.removeExclusiveAccessToAWTEvent(rc[i]);
            }
        }
    }

    /**
     * Tests addUserEventListener() with other events in the respository.
     * <p>
     * <i>Single app context.</i>
     */
    private void doTestAddUserEventListener_otherEvents(AddListener add) throws Exception
    {
        EventManager em = eventmanager;
        EventListener uel[] = new EventListener[3];

        try
        {
            // Add multiple listeners with same "other" repository
            UserEventRepository uer = new UserEventRepository("other0");
            uer.addKey(KeyEvent.VK_A);
            uer.addKey(KeyEvent.VK_B);
            for (int i = 0; i < uel.length; ++i)
            {
                uel[i] = new EventListener();
                add.add(em, uel[i], uer);

                // Dispatch
                dispatch(KeyEvent.VK_C);

                // Make sure no listeners were called
                for (int j = 0; j <= i; ++j)
                    uel[j].expectNoUserEvents(100L);
            }
        }
        finally
        {
            for (int i = 0; i < uel.length; ++i)
            {
                if (uel[i] != null) em.removeUserEventListener(uel[i]);
            }
            for (int i = 0; i < uel.length; ++i)
            {
                if (uel[i] != null)
                {
                    dispatch(KeyEvent.VK_A);
                    dispatch(KeyEvent.VK_B);
                    uel[i].expectNoUserEvents("Following removal:", 500L);
                }
            }
        }
    }

    /**
     * Tests addUserEventListener() with multiple listeners/same repository.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListener_multiListeners() throws Exception
    {
        doTestAddUserEventListener_multiListeners(ADD_SHARED);
    }

    /**
     * Tests addUserEventListener() (exclusive) with multiple listeners/same
     * repository.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListenerExclusive_multiListeners() throws Exception
    {
        doTestAddUserEventListener_multiListeners(ADD_EXCLUSIVE);
    }

    /**
     * Tests addAddExclusiveAccessToAWTEvent() with multiple RCs/same
     * repository.
     * <p>
     * <i>Single app context.</i> Note that AWTEvents don't function any
     * different in a single app context if the event is reserved or not (either
     * way, they should be received).
     */
    public void testAddExclusiveAccessToAWTEvent_multiClients() throws Exception
    {
        EventManager em = eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        ResourceClient rc[] = { new Client(), new Client(), new Client() };
        Focus focus = new Focus();

        try
        {
            // Add multiple clients with same empty repository
            UserEventRepository uer = new UserEventRepository("other0");
            uer.addKey(KeyEvent.VK_A);
            uer.addKey(KeyEvent.VK_B);
            for (int i = 0; i < rc.length; ++i)
            {
                assertTrue("Expected to be able to reserve AWTEvent: " + i, em.addExclusiveAccessToAWTEvent(rc[i], uer));

                int keyCode = KeyEvent.VK_A;

                // Dispatch event; focus is not focused
                fm.notifyDeactivated(focus);
                dispatch(keyCode);
                focus.expectNoKeyEvents("!Focused - ", 500L);

                // Dispatch event; focus is focused
                fm.requestActivate(focus, true);
                assertTrue("Expected to receive focus", focus.waitForActivated(1000));
                dispatch(keyCode);
                focus.expectKeyEvents("Focused - ", keyCode, 2000L);
            }
        }
        finally
        {
            fm.notifyDeactivated(focus);
            for (int i = 0; i < rc.length; ++i)
            {
                em.removeExclusiveAccessToAWTEvent(rc[i]);
            }
        }
    }

    /**
     * Tests addUserEventListener() with multiple listeners/same repository.
     * <p>
     * <i>Single app context.</i>
     */
    private void doTestAddUserEventListener_multiListeners(AddListener add) throws Exception
    {
        EventManager em = eventmanager;
        EventListener uel[] = new EventListener[3];
        boolean noErrorsTestMore = false;

        try
        {
            // Add multiple listeners with same repository
            UserEventRepository uer = new UserEventRepository("repo0");
            uer.addKey(KeyEvent.VK_C);
            uer.addKey(KeyEvent.VK_D);
            for (int i = 0; i < uel.length; ++i)
            {
                uel[i] = new EventListener();
                add.add(em, uel[i], uer);

                // Dispatch
                dispatch(KeyEvent.VK_C);

                // Make sure listeners were called
                for (int j = 0; j <= i; ++j)
                {
                    uel[j].expectUserEvents("[" + i + ":" + j + "] ", KeyEvent.VK_C, 6000L);
                }
            }
            noErrorsTestMore = true;
        }
        finally
        {
            for (int i = 0; i < uel.length; ++i)
            {
                if (uel[i] != null) em.removeUserEventListener(uel[i]);
            }
            if (noErrorsTestMore)
            {
                for (int i = 0; i < uel.length; ++i)
                {
                    if (uel[i] != null)
                    {
                        dispatch(KeyEvent.VK_C);
                        dispatch(KeyEvent.VK_D);
                        uel[i].expectNoUserEvents("Following removal:", 500L);
                    }
                }
            }
        }
    }

    /**
     * Tests addUserEventListener() with one listener, multiple repositories.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListener_multiRepository() throws Exception
    {
        doTestAddUserEventListener_multiRepository(ADD_SHARED);
    }

    /**
     * Tests addUserEventListener() (exclusive) with one listener, multiple
     * repositories.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListenerExclusive_multiRepository() throws Exception
    {
        doTestAddUserEventListener_multiRepository(ADD_EXCLUSIVE);
    }

    /**
     * Tests addExclusiveAccessToAWTEvent() with one RC, multiple repositories.
     * <p>
     * <i>Single app context.</i> Note that AWTEvents don't function any
     * different in a single app context if the event is reserved or not (either
     * way, they should be received).
     */
    public void testAddExclusiveAccessToAWTEvent_multiRepository() throws Exception
    {
        EventManager em = eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        Focus focus = new Focus();
        UserEventRepository uer[] = new UserEventRepository[3];
        ResourceClient client = new Client();
        int keyCode = KeyEvent.VK_E;

        try
        {
            // Add ResourceClient multiple times with different repositories
            for (int i = 0; i < uer.length; ++i)
            {
                uer[i] = new UserEventRepository("multi" + i);
                uer[i].addKey(keyCode + i);

                assertTrue("Expected to be able to reserve AWTEvents: " + i, em.addExclusiveAccessToAWTEvent(client,
                        uer[i]));

                // Dispatch/test all added events
                for (int j = 0; j <= i; ++j)
                {
                    // Dispatch event; focus is not focused
                    fm.notifyDeactivated(focus);
                    dispatch(keyCode + j);
                    focus.expectNoKeyEvents("!Focused - ", 500L);

                    // Dispatch event; focus is focused
                    fm.requestActivate(focus, true);
                    assertTrue("Expected to receive focus", focus.waitForActivated(1000));
                    dispatch(keyCode + j);
                    focus.expectKeyEvents("Focused - ", keyCode + j, 2000);
                }
            }

            // Expect only need to remove ResourceClient once
            em.removeExclusiveAccessToAWTEvent(client);
            for (int i = 0; i < uer.length; ++i)
            {
                // Dispatch event; focus is not focused
                fm.notifyDeactivated(focus);
                dispatch(keyCode + i);
                focus.expectNoKeyEvents("!Focused - ", 500L);

                // Dispatch event; focus is focused
                fm.requestActivate(focus, true);
                assertTrue("Expected to receive focus", focus.waitForActivated(1000));
                dispatch(keyCode + i);
                focus.expectKeyEvents("Focused - ", keyCode + i, 2000L);
            }
        }
        finally
        {
            em.removeExclusiveAccessToAWTEvent(client);
        }
    }

    /**
     * Tests addUserEventListener() with one listener, multiple repositories.
     * Expect repeated adds of listener w/ different repositories to update
     * previous add with new events.
     * <p>
     * <i>Single app context.</i>
     */
    private void doTestAddUserEventListener_multiRepository(AddListener add) throws Exception
    {
        EventManager em = eventmanager;
        UserEventRepository uer[] = new UserEventRepository[3];
        EventListener uel = new EventListener();
        int keyCode = KeyEvent.VK_E;

        try
        {
            // Add listener multiple times with different repositories
            for (int i = 0; i < uer.length; ++i)
            {
                uer[i] = new UserEventRepository("multi" + i);
                uer[i].addKey(keyCode + i);
                add.add(em, uel, uer[i]);

                // Dispatch all events subscribed to
                for (int j = 0; j <= i; ++j)
                {
                    // Dispatch
                    dispatch(keyCode + j);
                    uel.expectUserEvents("[" + i + "," + j + "] ", keyCode + j, 2000L);
                }
            }

            // Expect only need to remove listener once
            em.removeUserEventListener(uel);
            for (int i = 0; i < uer.length; ++i)
            {
                dispatch(keyCode + i);
                uel.expectNoUserEvents("Removed: ", 500L);
            }
        }
        finally
        {
            em.removeUserEventListener(uel);
            for (int i = 0; i < uer.length; ++i)
                dispatch(keyCode + i);
            uel.expectNoUserEvents("After removal: ", 500L);
        }
    }

    /**
     * Tests addUserEventListener() with an updated repository.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListener_updatedRepository() throws Exception
    {
        doTestAddUserEventListener_updatedRepository(ADD_SHARED);
    }

    /**
     * Tests addUserEventListener() (exclusive) with an updated repository.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListenerExclusive_updatedRepository() throws Exception
    {
        doTestAddUserEventListener_updatedRepository(ADD_EXCLUSIVE);
    }

    /**
     * Tests addExclusiveAccessToAWTEvent() (exclusive) with an updated
     * repository.
     * <p>
     * <i>Single app context.</i> Note that AWTEvents don't function any
     * different in a single app context if the event is reserved or not (either
     * way, they should be received). For example, we cannot tell the difference
     * between a reserved event and one that wasn't reserved.
     */
    public void testAddExclusiveAccessToAWTEvent_updatedRepository() throws Exception
    {
        EventManager em = eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        Focus focus = new Focus();
        ResourceClient client = new Client();

        try
        {
            int keyCode = KeyEvent.VK_H;
            int otherKey = KeyEvent.VK_I;

            // Add multiple listeners with same repository, updating it each
            // time
            UserEventRepository uer = new UserEventRepository("updated");
            uer.addKey(keyCode);
            assertTrue("Expected to be able to reserve event", em.addExclusiveAccessToAWTEvent(client, uer));

            // Update repository
            uer.addKey(otherKey);

            // Dispatch event; focus is not focused
            fm.notifyDeactivated(focus);
            dispatch(otherKey);
            dispatch(keyCode);
            focus.expectNoKeyEvents("!Focused - ", 500L);

            // Dispatch event; focus is focused
            fm.requestActivate(focus, true);
            assertTrue("Expected to receive focus", focus.waitForActivated(1000));
            dispatch(keyCode);
            focus.expectKeyEvents("Focused - ", keyCode, 20000L);

            // Dispatch other event; focus is focused (doesn't matter if
            // reserved or not!)
            dispatch(otherKey);
            focus.expectKeyEvents("Focused - ", otherKey, 20000L);

            // Dispatch event; focus is not focused
            fm.notifyDeactivated(focus);
            dispatch(otherKey);
            dispatch(keyCode);
            focus.expectNoKeyEvents("DeActivated - ", 500L);

            // Dispatch event; focus is focused
            fm.requestActivate(focus, true);
            assertTrue("Expected to receive focus", focus.waitForActivated(1000));
            dispatch(keyCode);
            focus.expectKeyEvents("ReActivated - ", keyCode, 20000L);

            // Dispatch other event; focus is focused (doesn't matter if
            // reserved or not!)
            dispatch(otherKey);
            focus.expectKeyEvents("ReActivated - ", otherKey, 20000L);
        }
        finally
        {
            em.removeExclusiveAccessToAWTEvent(client);
        }
    }

    /**
     * Tests addUserEventListener() with an updated repository.
     * <p>
     * <i>Single app context.</i>
     */
    private void doTestAddUserEventListener_updatedRepository(AddListener add) throws Exception
    {
        EventManager em = eventmanager;
        EventListener uel = new EventListener();

        try
        {
            // Add multiple listeners with same repository, updating it each
            // time
            UserEventRepository uer = new UserEventRepository("updated");
            uer.addKey(KeyEvent.VK_H);
            add.add(em, uel, uer);

            // Update repository
            uer.addKey(KeyEvent.VK_I);

            // Dispatch
            dispatch(KeyEvent.VK_H);
            dispatch(KeyEvent.VK_I); // should not receive this key
            uel.expectUserEvents(KeyEvent.VK_H, 2000L);

            // Remove and re-add
            uel.reset();
            em.removeUserEventListener(uel);
            add.add(em, uel, uer);

            // Dispatch
            dispatch(KeyEvent.VK_I);
            uel.expectUserEvents(KeyEvent.VK_I, 2000L);
        }
        finally
        {
            if (uel != null) em.removeUserEventListener(uel);
            dispatch(KeyEvent.VK_H);
            dispatch(KeyEvent.VK_I);
            uel.expectNoUserEvents("After removal: ", 500L);
        }
    }

    /**
     * Tests that only the desired events are received. In particular, if UER
     * has KEY_PRESSED, but no KEY_RELEASED: only see KEY_PRESSED. Etc...
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListener_partialEvents() throws Exception
    {
        doTestAddUserEventListener_partialEvents(ADD_SHARED);
    }

    /**
     * Tests that only the desired events are received, but that the entire set
     * of events for that code are reserved! In particular, if UER has
     * KEY_PRESSED, but no KEY_RELEASED: only see KEY_PRESSED. Etc...
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddUserEventListenerExclusive_partialEvents() throws Exception
    {
        doTestAddUserEventListener_partialEvents(ADD_EXCLUSIVE);
    }

    /**
     * Implements {@link #testAddUserEventListener_partialEvents} and
     * {@link #testAddUserEventListenerExclusive_partialEvents}.
     */
    private void doTestAddUserEventListener_partialEvents(AddListener add) throws Exception
    {
        EventManager em = eventmanager;

        int[] keys = { KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C };
        char[] chars = { 'a', 'b', 'c' };
        UserEvent[] events = { new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, keys[0], 0, 0L),
                new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, chars[1], 0L),
                new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_RELEASED, keys[2], 0, 0L), };

        UserEventRepository uer = new UserEventRepository("partial");
        for (int i = 0; i < events.length; ++i)
            uer.addUserEvent(events[i]);

        EventListener uel = new EventListener();

        try
        {
            // Add The listener
            add.add(em, uel, uer);

            for (int i = 0; i < events.length; ++i)
            {
                dispatch(keys[i], chars[i]);

                uel.expectUserEvent("Expect event delivered [" + i + "] ", events[i], 2000L);
                uel.expectNoUserEvents("Expect no corresponding events [" + i + "]", 500L);
            }
        }
        finally
        {
            em.removeUserEventListener(uel);
        }
    }

    /**
     * Tests that only the desired events are received, but that the entire set
     * of events for that code are reserved! In particular, if UER has
     * KEY_PRESSED, but no KEY_RELEASED: only see KEY_PRESSED. Etc...
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddExclusiveAccessToAWTEvent_partialEvents() throws Exception
    {
        EventManager em = eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);

        int[] keys = { HRcEvent.VK_CHANNEL_UP, KeyEvent.VK_B, HRcEvent.VK_CHANNEL_DOWN };
        char[] chars = { UserEventTest.CHAR_UNDEFINED, 'b', UserEventTest.CHAR_UNDEFINED };
        UserEvent[] events = { new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, keys[0], 0, 0L),
                new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, chars[1], 0L),
                new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_RELEASED, keys[2], 0, 0L), };

        UserEventRepository uer = new UserEventRepository("partial");
        for (int i = 0; i < events.length; ++i)
            uer.addUserEvent(events[i]);

        Focus focus = new Focus();
        ResourceClient client = new Client();

        try
        {
            assertTrue("Expected to be able to reserve AWTEvents", em.addExclusiveAccessToAWTEvent(client, uer));

            fm.requestActivate(focus, true);
            assertTrue("Expected to be able to acquire focus", focus.waitForActivated(3000L));

            for (int i = 0; i < events.length; ++i)
            {
                dispatch(keys[i], chars[i]);
                focus.expectKeyEvent("Expect event delivered [" + i + "] ", events[i], 2000L);
                focus.expectNoKeyEvents("Expect no corresponding events [" + i + "]", 500L);
            }
        }
        finally
        {
            fm.notifyDeactivated(focus);
            em.removeExclusiveAccessToAWTEvent(client);
        }
    }

    /**
     * Tests removeUserEventListener().
     * <p>
     * <i>Single app context.</i>
     */
    public void testRemoveUserEventListener() throws Exception
    {
        doTestRemoveUserEventListener(ADD_SHARED);
    }

    /**
     * Tests removeUserEventListener() (exclusive).
     * <p>
     * <i>Single app context.</i>
     */
    public void testRemoveUserEventListenerExclusive() throws Exception
    {
        doTestRemoveUserEventListener(ADD_EXCLUSIVE);
    }

    /**
     * Tests removeExclusiveAccessToAWTEvent() (exclusive).
     * <p>
     * <i>Single app context.</i> Note that AWTEvents don't function any
     * different in a single app context if the event is reserved or not (either
     * way, they should be received).
     */
    public void testRemoveExclusiveAccessToAWTEvent() throws Exception
    {
        doTestRemoveUserEventListener(ADD_EXCLUSIVE);
    }

    /**
     * Tests removeUserEventListener().
     * <p>
     * <i>Single app context.</i>
     */
    private void doTestRemoveUserEventListener(AddListener add) throws Exception
    {
        EventManager em = eventmanager;
        EventListener uel[] = new EventListener[5];
        int keyCode = KeyEvent.VK_J;
        UserEventRepository uer = new UserEventRepository("remove");
        uer.addKey(keyCode);

        try
        {
            // Add multiple listeners with new repository
            for (int i = 0; i < uel.length; ++i)
            {
                uel[i] = new EventListener();

                add.add(em, uel[i], uer);

                // Dispatch
                dispatch(keyCode);

                // Make sure listeners were called
                for (int j = 0; j <= i; ++j)
                {
                    uel[j].expectUserEvents(keyCode, 2000L);
                }
            }

            // Now remove listeners
            // even first
            for (int i = 0; i < uel.length; i += 2)
            {
                em.removeUserEventListener(uel[i]);

                // Dispatch
                dispatch(keyCode);

                // Make sure listeners were called
                for (int j = 0; j < uel.length; ++j)
                {
                    if (j <= i && (j & 1) == 0)
                    {
                        // removed
                        uel[j].expectNoUserEvents("Removed: ", 100L);
                    }
                    else
                    {
                        uel[j].expectUserEvents(keyCode, 2000L);
                    }
                    uel[j].reset();
                }
            }

            // Now remove remaining listeners
            for (int i = 0; i < uel.length; ++i)
            {
                // might remove it twice -- should not matter
                em.removeUserEventListener(uel[i]);

                // Dispatch
                dispatch(keyCode);

                // Make sure listeners were called
                for (int j = 0; j < uel.length; ++j)
                {
                    if (j <= i || (j & 1) == 0)
                    {
                        // removed
                        uel[j].expectNoUserEvents("Removed: ", 100L);
                    }
                    else
                    {
                        uel[j].expectUserEvents(keyCode, 2000L);
                    }
                }
            }
        }
        finally
        {
            for (int i = 0; i < uel.length; ++i)
            {
                if (uel[i] != null) em.removeUserEventListener(uel[i]);
            }
        }
    }

    /**
     * Tests add|removeResourceStatusEventListener. Very simple test that
     * doesn't verify a whole lot.
     * <p>
     * <i>Single app context.</i>
     * <p>
     * <i>Note that AWT tests rely on FocusManager/FocusContext semantics. The
     * AWT tests rely on the EventManager implementation being integrated with
     * the FocusManager to handle AWT focus and dispatch.</i>
     */
    public void testAddRemoveResourceStatusEventListener() throws Exception
    {
        EventManager em = eventmanager;

        // Removing unadded listener should be ignored
        em.removeResourceStatusEventListener(new ResourceListener());

        ResourceListener rsl0 = null, rsl1 = null;
        EventListener uel = null;
        Client rc = null;
        try
        {
            // Add same listener more than once (should effectively be added
            // once)
            rsl0 = new ResourceListener();
            em.addResourceStatusEventListener(rsl0);
            em.addResourceStatusEventListener(rsl0);

            // Add another listener
            rsl1 = new ResourceListener();
            em.addResourceStatusEventListener(rsl1);

            uel = new EventListener();
            rc = new Client();
            UserEventRepository uer = new UserEventRepository("status");
            uer.addKey(KeyEvent.VK_0);
            uer.addKey(KeyEvent.VK_1);

            // Exclusive access
            assertTrue("Expected exclusive access", em.addUserEventListener(uel, rc, uer));

            // Wait for UnavailableEvent
            rsl1.expectUnavailEvent(uer, 2000L);

            // Should receive ONCE
            rsl0.expectUnavailEvent(uer, 2000L);

            // Removal of unknown listener shouldn't do anything
            em.removeUserEventListener(new EventListener());

            // Remove listener should produce available event
            em.removeUserEventListener(uel);

            // Wait for AvailableEvent
            rsl1.expectAvailEvent(uer, 2000L);

            // Should receive ONCE
            rsl0.expectAvailEvent(uer, 2000L);

            // Remove some listeners
            em.removeResourceStatusEventListener(rsl1);

            // Exclusive access
            assertTrue("Expected exclusive access", em.addExclusiveAccessToAWTEvent(rc, uer));

            // Wait for UnavailableEvent
            rsl0.expectUnavailEvent(uer, 2000L);
            rsl1.expectNoEvents("ResourceStatus ", 500L);

            // Remove remaining listener
            em.removeResourceStatusEventListener(rsl0);

            // Remove listener should produce available event, but unseen
            em.removeUserEventListener(uel);
            rsl0.expectNoEvents("ResourceStatus[0] ", 500L);
            rsl1.expectNoEvents("ResourceStatus[1] ", 100L);
        }
        finally
        {
            // Now remove all of the listeners
            if (rsl0 != null) em.removeResourceStatusEventListener(rsl0);
            if (rsl1 != null) em.removeResourceStatusEventListener(rsl1);
            if (rsl0 != null) em.removeResourceStatusEventListener(rsl0);
            if (uel != null) em.removeUserEventListener(uel);
            if (rc != null) em.removeExclusiveAccessToAWTEvent(rc);
        }
    }

    /**
     * Tests add|removeUserEventListener() with different listeners but
     * overlapping UserEventRepositories. Ensures that each listener is only
     * notified of the events that they expect. Ensures that remove one listener
     * doesn't affect other listeners.
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddRemoveUserEventListener_overlapping() throws Exception
    {
        doTestAddRemoveUserEventListener_overlapping(ADD_SHARED);
    }

    /**
     * Tests add|removeUserEventListener() (exclusive) with different listeners
     * but overlapping UserEventRepositories. Ensures that each listener is only
     * notified of the events that they expect. Ensures that remove one listener
     * doesn't affect other listeners. Note that AWTEvents don't function any
     * different in a single app context if the event is reserved or not (either
     * way, they should be received).
     * <p>
     * <i>Single app context.</i>
     */
    public void testAddRemoveUserEventListenerExclusive_overlapping() throws Exception
    {
        if (false) // unioning of UERs supported
            doTestAddRemoveUserEventListener_overlapping(ADD_EXCLUSIVE);
        else
            // unioning not supported for overlapping UERs
            doTestAddRemoveUserEventListenerExclusive_overlapping(ADD_EXCLUSIVE);
    }

    /**
     * Tests add/removeExclusiveAccessToAWTEvent given overlapping UERs. Ensures
     * that app is only notified of the events that they expect, and only once.
     * Ensures that remove one RC doesn't affect other reservations.
     * <p>
     * <i>Single app context.</i>
     * <p>
     * <i>Note that AWT tests rely on FocusManager/FocusContext semantics. The
     * AWT tests rely on the EventManager implementation being integrated with
     * the FocusManager to handle AWT focus and dispatch.</i>
     */
    public void testAddRemoveExclusiveAccessToAWTEvent_overlapping() throws Exception
    {
        EventManager em = eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        Focus focus = new Focus();
        int[][] keys = {
                // First event is private, 2nd/3rd overlaps w/ somebody
                { KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_E }, { KeyEvent.VK_B, KeyEvent.VK_E, KeyEvent.VK_F },
                { KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_F }, };
        UserEventRepository uer[] = new UserEventRepository[keys.length];
        ResourceClient rc[] = new ResourceClient[keys.length];

        try
        {
            // Add exclusive access clients
            for (int i = 0; i < uer.length; ++i)
            {
                uer[i] = new UserEventRepository("uer" + 1);
                for (int j = 0; j < keys[i].length; ++j)
                    uer[i].addKey(keys[i][j]);
                rc[i] = new Client();
                assertTrue("Expected to be able to reserve event", em.addExclusiveAccessToAWTEvent(rc[i], uer[i]));
            }

            // Test dispatch of each key, make sure appropriate listeners were
            // called
            for (int key = KeyEvent.VK_A; key <= KeyEvent.VK_F; ++key)
            {
                // Dispatch event; focus is not focused
                fm.notifyDeactivated(focus);
                dispatch(key);
                focus.expectNoKeyEvents("!Focused - ", 500L);

                // Dispatch event; focus is focused
                fm.requestActivate(focus, true);
                assertTrue("Expected to receive focus", focus.waitForActivated(1000));
                dispatch(key);
                focus.expectKeyEvents("Focused - ", key, 2000L);
            }

            // Now do the same, but remove exclusive access clients
            for (int j = 0; j < rc.length; ++j)
            {
                // Remove listener
                em.removeExclusiveAccessToAWTEvent(rc[j]);
            }

            // Test dispatch of each key, make sure appropriate listeners were
            // called
            for (int key = KeyEvent.VK_A; key <= KeyEvent.VK_F; ++key)
            {
                // Dispatch event; focus is not focused
                fm.notifyDeactivated(focus);
                dispatch(key);
                focus.expectNoKeyEvents("!Focused - ", 500L);

                // Dispatch event; focus is focused
                fm.requestActivate(focus, true);
                assertTrue("Expected to receive focus", focus.waitForActivated(1000));
                dispatch(key);
                focus.expectKeyEvents("Focused - ", key, 2000L);
            }
        }
        finally
        {
            for (int i = 0; i < rc.length; ++i)
            {
                if (rc[i] != null) em.removeExclusiveAccessToAWTEvent(rc[i]);
            }
        }
    }

    /**
     * Implements testAddRemoveuserEventListener[Exclusive]_overlapping().
     * <p>
     * <i>Single app context.</i>
     */
    private void doTestAddRemoveUserEventListener_overlapping(AddListener add) throws Exception
    {
        EventManager em = eventmanager;
        int[][] keys = { { KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C },
                { KeyEvent.VK_B, KeyEvent.VK_C, KeyEvent.VK_D }, { KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_A }, };
        UserEventRepository uer[] = new UserEventRepository[keys.length];
        EventListener uel[] = new EventListener[keys.length];

        try
        {
            // Add all listeners
            for (int i = 0; i < uel.length; ++i)
            {
                uer[i] = new UserEventRepository("uer" + 1);
                for (int j = 0; j < keys[i].length; ++j)
                    uer[i].addKey(keys[i][j]);
                uel[i] = new EventListener();
                add.add(em, uel[i], uer[i]);
            }

            // Test dispatch of each key, make sure appropriate listeners were
            // called
            for (int key = KeyEvent.VK_A; key <= KeyEvent.VK_D; ++key)
            {
                dispatch(key);

                // Make sure listeners were called
                for (int i = 0; i < uel.length; ++i)
                {
                    boolean expected = hasKey(keys[i], key);

                    if (!expected)
                        uel[i].expectNoUserEvents("[" + (char) key + ":" + i + "] ", 500L);
                    else
                        uel[i].expectUserEvents("[" + (char) key + ":" + i + "] ", key, 2000L);
                }
            }

            // Now do the same, but remove listeners
            for (int j = 0; j < uel.length; ++j)
            {
                // Remove listener
                em.removeUserEventListener(uel[j]);

                // Dispatch key previously received by that listener
                int key = keys[j][0];
                dispatch(key);

                // Make sure listeners were called
                for (int i = j + 1; i < uel.length; ++i)
                {
                    boolean expected = hasKey(keys[i], key);

                    if (!expected)
                        uel[i].expectNoUserEvents("[" + (char) key + ":" + i + "] ", 500L);
                    else
                        uel[i].expectUserEvents("[" + (char) key + ":" + i + "] ", key, 2000L);
                }
                // Make sure removed listeners weren't called
                for (int i = 0; i <= j; ++i)
                {
                    uel[i].expectNoUserEvents("[" + i + "] ", 500L);
                }
            }
        }
        finally
        {
            for (int i = 0; i < uel.length; ++i)
            {
                if (uel[i] != null) em.removeUserEventListener(uel[i]);
            }
        }
    }

    /**
     * Implements testAddRemoveuserEventListener[Exclusive]_overlapping().
     * <p>
     * This is different from
     * {@link #doTestAddRemoveUserEventListener_overlapping} in that it expects
     * overlapping entries to steal resources from each other.
     * <p>
     * <i>Single app context.</i>
     */
    private void doTestAddRemoveUserEventListenerExclusive_overlapping(AddListener add) throws Exception
    {
        EventManager em = eventmanager;
        int[][] keys = { { KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C },
                { KeyEvent.VK_B, KeyEvent.VK_C, KeyEvent.VK_D }, { KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_A }, };
        UserEventRepository uer[] = new UserEventRepository[keys.length];
        EventListener uel[] = new EventListener[keys.length];

        try
        {
            // Add all listeners (go through twice)
            for (int I = 0; I < uel.length + 1; ++I)
            {
                int i = (I % uel.length);
                if (uer[i] == null)
                {
                    uer[i] = new UserEventRepository("uer" + 1);
                    for (int j = 0; j < keys[i].length; ++j)
                        uer[i].addKey(keys[i][j]);
                    uel[i] = new EventListener();
                }
                add.add(em, uel[i], uer[i]);

                // Show that listener gets events
                for (int j = 0; j < keys[i].length; ++j)
                {
                    dispatch(keys[i][j]);
                    uel[i].expectUserEvents("reserved ", keys[i][j], 2000L);
                    for (int k = 0; k < i; ++k)
                        uel[k].expectNoUserEvents("!reserved ", 500L);
                }
            }
            // At this point, uel[0] should be receiving events

            for (int i = 0; i < uel.length; ++i)
            {
                // (re)Add listener
                add.add(em, uel[i], uer[i]);

                // Verify
                int key = keys[i][0];
                dispatch(key);
                uel[i].expectUserEvents("reserved ", key, 2000L);

                // Remove listener
                em.removeUserEventListener(uel[i]);

                // Verify
                for (int j = 0; j < keys[i].length; ++j)
                {
                    dispatch(keys[i][j]);
                    uel[i].expectNoUserEvents("removed ", 500L);
                }
            }
        }
        finally
        {
            for (int i = 0; i < uel.length; ++i)
            {
                if (uel[i] != null) em.removeUserEventListener(uel[i]);
            }
        }
    }

    private boolean hasKey(int[] keys, int key)
    {
        for (int i = 0; i < keys.length; ++i)
            if (keys[i] == key) return true;
        return false;
    }

    /*
     * =========================== Context-specific
     * =============================== The following sets of tests require
     * multi-app semantics. This is achieved by using custom caller contexts and
     * managers. These tests depend upon the EventManager implementation being
     * based upon the CableLabs Java Management Framework
     * (org.cablelabs.impl.manager).=========================== Context-specific
     * ===============================
     */

    /**
     * Tests add|removeUserEventListener.
     */
    public void testAddRemoveUserEventListener_context() throws Exception
    {
        Context app[] = { new Context(new AppID(10, 20)), new Context(new AppID(10, 21)), };
        try
        {
            EventListener uel[] = new EventListener[app.length];
            UserEventRepository uer[] = new UserEventRepository[app.length];

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // Add listener to app1
            uel[0] = new EventListener();
            uer[0] = new UserEventRepository("app1");
            uer[0].addKey(KeyEvent.VK_A);
            uer[0].addKey(KeyEvent.VK_B);
            app[0].addUserEventListener(uel[0], uer[0]);

            // Dispatch unspecified event - shouldn't be dispatched
            dispatch(KeyEvent.VK_C);
            uel[0].expectNoUserEvents(100L);

            // Dispatch specified event - should be dispatched
            dispatch(KeyEvent.VK_B);
            uel[0].expectUserEvents(KeyEvent.VK_B, 2000L, app[0].tg);

            // Add listener to app2 (w/ non-overlapping repository)
            uel[1] = new EventListener();
            uer[1] = new UserEventRepository("app2");
            uer[1].addKey(KeyEvent.VK_B);
            uer[1].addKey(KeyEvent.VK_C);
            app[1].addUserEventListener(uel[1], uer[1]);

            // Dispatch unspecified event - shouldn't be dispatched to anyone
            dispatch(KeyEvent.VK_Z);
            uel[0].expectNoUserEvents(100L);
            uel[1].expectNoUserEvents(100L);

            // Dispatch event to app1 - should only be seen by app1 listeners
            dispatch(KeyEvent.VK_A);
            uel[0].expectUserEvents(KeyEvent.VK_A, 2000L, app[0].tg);

            // Dispatch event to app2 - should only be seen by app2 listener
            dispatch(KeyEvent.VK_C);
            uel[1].expectUserEvents(KeyEvent.VK_C, 2000L, app[1].tg);

            // Dispatch event to both - should be seen by both
            dispatch(KeyEvent.VK_B);
            for (int i = 0; i < app.length; ++i)
            {
                uel[i].expectUserEvents(KeyEvent.VK_B, 2000L, app[i].tg);
            }

            // Remove listeners for other app shouldn't have an effect
            app[0].removeUserEventListener(uel[1]);
            app[1].removeUserEventListener(uel[0]);

            // Dispatch event to both - should be seen by both
            dispatch(KeyEvent.VK_B);
            for (int i = 0; i < app.length; ++i)
            {
                uel[i].expectUserEvents(KeyEvent.VK_B, 2000L, app[i].tg);
            }

            // Remove listeners and verify that they aren't dispatched to
            app[0].removeUserEventListener(uel[0]);

            // Dispatch event to both - should be seen by one!
            dispatch(KeyEvent.VK_A);
            dispatch(KeyEvent.VK_B);
            uel[0].expectNoUserEvents(500L);
            uel[1].expectUserEvents(KeyEvent.VK_B, 2000L, app[1].tg);

            // Remove app2 listener
            app[1].removeUserEventListener(uel[1]);
            dispatch(KeyEvent.VK_A);
            dispatch(KeyEvent.VK_B);
            dispatch(KeyEvent.VK_C);
            uel[0].expectNoUserEvents(500L);
            uel[0].expectNoUserEvents(100L);
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests add|removeUserEventLIstener (exclusive).
     */
    public void testAddRemoveUserEventListenerExclusive_context() throws Exception
    {
        Context app[] = { new Context(new AppID(11, 20)), new Context(new AppID(11, 21)), };
        try
        {
            EventListener uel[] = new EventListener[app.length + 2];
            UserEventRepository uer[] = new UserEventRepository[app.length + 2];
            Client rc[] = new Client[app.length];

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // uel[0]/uer[0] : VK_A, VK_B
            // uel[2]/uer[2] : VK_C, VK_D
            // uel[1]/uer[1] : VK_Y, VK_Z
            // uel[3]/uer[3] : VK_C, VK_D

            // Add exclusive access listener to app1
            uel[0] = new EventListener();
            uer[0] = new UserEventRepository("app1");
            uer[0].addKey(KeyEvent.VK_A);
            uer[0].addKey(KeyEvent.VK_B);
            rc[0] = new Client();
            assertTrue("Expected exclusive access", app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // Dispatch unspecified event - shouldn't be dispatched
            dispatch(KeyEvent.VK_C);
            uel[0].expectNoUserEvents("app1", 500L);

            // Dispatch specified event - should be dispatched
            dispatch(KeyEvent.VK_B);
            uel[0].expectUserEvents(KeyEvent.VK_B, 2000L, app[0].tg);

            // Add exclusive access listener to app2 (non-overlapping)
            uel[1] = new EventListener();
            uer[1] = new UserEventRepository("app2");
            uer[1].addKey(KeyEvent.VK_Y);
            uer[1].addKey(KeyEvent.VK_Z);
            rc[1] = new Client();
            assertTrue("Expected exclusive access", app[1].addUserEventListener(uel[1], rc[1], uer[1]));

            // Dispatch unspecified event - shouldn't be dispatched
            dispatch(KeyEvent.VK_M);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[1].expectNoUserEvents("app2", 100L);

            // Dispatch app2-specified event - to app2 only
            dispatch(KeyEvent.VK_Z);
            uel[1].expectUserEvents("app2", KeyEvent.VK_Z, 2000L, app[1].tg);
            uel[0].expectNoUserEvents("app1", 500L);

            // Dispatch app1-specified event - to app1 only
            dispatch(KeyEvent.VK_A);
            uel[0].expectUserEvents("app1", KeyEvent.VK_A, 2000L, app[0].tg);
            uel[1].expectNoUserEvents("app2", 500L);

            // Add exclusive access listener to app1 (another set of keys)
            uel[2] = new EventListener();
            uer[2] = new UserEventRepository("app1-b");
            uer[2].addKey(KeyEvent.VK_C);
            uer[2].addKey(KeyEvent.VK_D);
            assertTrue("Expected exclusive access", app[0].addUserEventListener(uel[2], rc[0], uer[2]));

            // Dispatch unspecified event - shouldn't be dispatched
            dispatch(KeyEvent.VK_N);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[1].expectNoUserEvents("app2", 100L);
            uel[2].expectNoUserEvents("app1-b", 100L);

            // Dispatch app1-specified event - should be dispatched to one
            // listener
            dispatch(KeyEvent.VK_C);
            uel[2].expectUserEvents("app1-b", KeyEvent.VK_C, 2000L, app[0].tg);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[1].expectNoUserEvents("app2", 100L);

            // Dispatch app2-specified event - should be dispatched to app2 only
            dispatch(KeyEvent.VK_Y);
            uel[1].expectUserEvents("app2", KeyEvent.VK_Y, 2000L, app[1].tg);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[2].expectNoUserEvents("app1-b", 100L);

            // Add exclusive access listener to app2 (overlapping with app1)
            // ...Should take exclusive access away
            uel[3] = new EventListener();
            uer[3] = new UserEventRepository("app2-b");
            uer[3].addKey(KeyEvent.VK_C);
            uer[3].addKey(KeyEvent.VK_D);
            assertTrue("Expected exclusive access", app[1].addUserEventListener(uel[3], rc[1], uer[3]));

            // Dispatch unspecified event - shouldn't be dispatched
            dispatch(KeyEvent.VK_O);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[1].expectNoUserEvents("app2", 100L);
            uel[2].expectNoUserEvents("app1-b", 100L);
            uel[3].expectNoUserEvents("app2-b", 100L);

            // Dispatch app1-specified event - to app1 only
            dispatch(KeyEvent.VK_A);
            uel[0].expectUserEvents("app1", KeyEvent.VK_A, 2000L, app[0].tg);
            uel[1].expectNoUserEvents("app2", 500L);
            uel[2].expectNoUserEvents("app1-b", 100L);
            uel[3].expectNoUserEvents("app2-b", 100L);

            // Dispatch app2-specified event - to app2 only
            dispatch(KeyEvent.VK_Y);
            uel[1].expectUserEvents("app2", KeyEvent.VK_Y, 2000L, app[1].tg);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[2].expectNoUserEvents("app1-b", 100L);
            uel[3].expectNoUserEvents("app2-b", 100L);

            // Dispatch overlapped app2-specified event - to app2 only
            dispatch(KeyEvent.VK_C);
            uel[3].expectUserEvents("app2-b", KeyEvent.VK_C, 2000L, app[1].tg);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[1].expectNoUserEvents("app2", 100L);
            uel[2].expectNoUserEvents("app1-b", 100L);

            // Remove exclusive access
            app[1].removeUserEventListener(uel[3]); // lose VK_C, VK_D
            // Remove other listener
            app[0].removeUserEventListener(uel[0]); // lose VK_A, VK_B

            // Dispatch event to none (app1-b lost access, app2-b gave it up)
            dispatch(KeyEvent.VK_D);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[1].expectNoUserEvents("app2", 100L);
            uel[2].expectNoUserEvents("app1-b", 100L);
            uel[3].expectNoUserEvents("app2-b", 100L);

            // Remove rest of listeners
            app[1].removeUserEventListener(uel[1]);
            app[0].removeUserEventListener(uel[2]);

            // After listeners are removed, exclusive access is revoked no
            // events are received
            dispatch(KeyEvent.VK_A);
            dispatch(KeyEvent.VK_B);
            dispatch(KeyEvent.VK_C);
            dispatch(KeyEvent.VK_D);
            dispatch(KeyEvent.VK_Y);
            dispatch(KeyEvent.VK_Z);
            uel[0].expectNoUserEvents("app1", 500L);
            uel[1].expectNoUserEvents("app2", 100L);
            uel[2].expectNoUserEvents("app1-b", 100L);
            uel[3].expectNoUserEvents("app2-b", 100L);
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests add|removeExclusiveAccessToAWTEvent(). Verify dispatch via normal
     * AWT mechanisms.
     * <p>
     * <i>Note that AWT tests rely on FocusManager/FocusContext semantics. The
     * AWT tests rely on the EventManager implementation being integrated with
     * the FocusManager to handle AWT focus and dispatch.</i>
     */
    public void testAddRemoveExclusiveAccessToAWTEvent_context() throws Exception
    {
        Context app[] = { new Context(new AppID(11, 20)), new Context(new AppID(11, 21)), };
        try
        {
            UserEventRepository uer[] = new UserEventRepository[app.length + 2];
            Client rc[] = new Client[app.length + 2];
            Focus focus[] = new Focus[app.length];

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // app[0]/rc[0]/uer[0] : VK_A, VK_B
            // app[0]/rc[2]/uer[2] : VK_C, VK_D
            // app[1]/rc[1]/uer[1] : VK_Y, VK_Z
            // app[1]/rc[3]/uer[3] : VK_C, VK_D

            // Add exclusive access listener to app1
            focus[0] = new Focus(app[0]);
            uer[0] = new UserEventRepository("app1");
            uer[0].addKey(KeyEvent.VK_A);
            uer[0].addKey(KeyEvent.VK_B);
            rc[0] = new Client();
            assertTrue("Expected exclusive access", app[0].addExclusiveAccessToAWTEvent(rc[0], uer[0]));

            // Dispatch event - shouldn't be dispatched (w/out focus)
            dispatch(KeyEvent.VK_C); // unreserved
            focus[0].expectNoKeyEvents("app1(!focus) ", 500L);
            dispatch(KeyEvent.VK_B);
            focus[0].expectNoKeyEvents("app1(!focus) ", 500L);

            // Acquire focus
            app[0].requestFocus(focus[0]);
            assertTrue("Expected to be able to get focus", focus[0].waitForActivated(3000L));

            // Dispatch specified event - should be dispatched
            dispatch(KeyEvent.VK_C);
            focus[0].expectKeyEvents("app1(focus) ", KeyEvent.VK_C, 2000L);
            dispatch(KeyEvent.VK_B);
            focus[0].expectKeyEvents("app1(focus) ", KeyEvent.VK_B, 2000L);

            // Add exclusive access listener to app2 (non-overlapping)
            focus[1] = new Focus(app[1]);
            uer[1] = new UserEventRepository("app2");
            uer[1].addKey(KeyEvent.VK_Y);
            uer[1].addKey(KeyEvent.VK_Z);
            rc[1] = new Client();
            assertTrue("Expected exclusive access", app[1].addExclusiveAccessToAWTEvent(rc[1], uer[1]));

            // Dispatch events
            dispatch(KeyEvent.VK_C); // unreserved
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_C, 2000L);
            focus[1].expectNoKeyEvents("app2(!focused) ", 500L);

            // Dispatch app2-specified event - to nobody
            dispatch(KeyEvent.VK_Z);
            focus[1].expectNoKeyEvents("app2(!focused) ", 500L);
            focus[0].expectNoKeyEvents("app1(focused) ", 500L);

            // App2 acquire focus
            app[1].requestFocus(focus[1]);
            assertTrue("Expected to be able to get focus", focus[1].waitForActivated(3000L));

            // Dispatch events
            dispatch(KeyEvent.VK_C); // unreserved
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_C, 2000L);
            focus[0].expectNoKeyEvents("app1(!focused) ", 500L);

            // Dispatch app2-specified event - to app2 only
            dispatch(KeyEvent.VK_Z);
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_Z, 2000L);
            focus[0].expectNoKeyEvents("app1(!focused) ", 500L);

            // Dispatch app1-specified event - to nobody
            dispatch(KeyEvent.VK_A);
            focus[0].expectNoKeyEvents("app1(!focused) ", 500L);
            focus[1].expectNoKeyEvents("app2(focused) ", 500L);

            // App1 re-acquire focus
            app[0].requestFocus(focus[0]);
            assertTrue("Expected to be able to get focus", focus[0].waitForActivated(3000L));

            // Dispatch app1-specified event - to app1
            dispatch(KeyEvent.VK_A);
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_A, 2000L);
            focus[1].expectNoKeyEvents("app2(!focused) ", 500L);

            // Add exclusive access listener to app1 (another set of keys)
            rc[2] = new Client();
            uer[2] = new UserEventRepository("app1-b");
            uer[2].addKey(KeyEvent.VK_C);
            uer[2].addKey(KeyEvent.VK_D);
            assertTrue("Expected exclusive access", app[0].addExclusiveAccessToAWTEvent(rc[2], uer[2]));

            // Dispatch app1-specified event - should be dispatched to app1
            // (once)
            dispatch(KeyEvent.VK_C);
            focus[0].expectKeyEvents("app1-b(focused)", KeyEvent.VK_C, 2000L);
            focus[1].expectNoKeyEvents("app2(!focused)", 100L);

            // Dispatch app2-specified event - should be dispatched to nobody
            dispatch(KeyEvent.VK_Y);
            focus[1].expectNoKeyEvents("app2", 500L);
            focus[0].expectNoKeyEvents("app1", 100L);

            // Add exclusive access listener to app2 (overlapping with app1)
            // ...Should take exclusive access away
            rc[3] = new Client();
            uer[3] = new UserEventRepository("app2-b");
            uer[3].addKey(KeyEvent.VK_C);
            assertTrue("Expected exclusive access", app[1].addExclusiveAccessToAWTEvent(rc[3], uer[3]));

            // Dispatch event - to nobody
            dispatch(KeyEvent.VK_C);
            focus[0].expectNoKeyEvents("app1(focused) ", 500L);
            focus[1].expectNoKeyEvents("app2(!focused) ", 100L);

            // Dispatch event - to focused app
            dispatch(KeyEvent.VK_D); // app1 lost with C, but is focused
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_D, 2000L);
            focus[1].expectNoKeyEvents("app2(!focused) ", 100L);

            // Dispatch app1-specified event - to app1 only
            dispatch(KeyEvent.VK_A);
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_A, 2000L);
            focus[1].expectNoKeyEvents("app2(!focused) ", 500L);

            // App2 acquire focus
            app[1].requestFocus(focus[1]);
            assertTrue("Expected to be able to get focus", focus[1].waitForActivated(3000L));

            // Dispatch event - to app2
            dispatch(KeyEvent.VK_Y);
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_Y, 2000L);
            focus[0].expectNoKeyEvents("app1(focused) ", 500L);

            // Dispatch overlapped app2-specified event - to app2 only
            dispatch(KeyEvent.VK_C);
            focus[1].expectKeyEvents("app2-b(focused) ", KeyEvent.VK_C, 2000L);
            focus[0].expectNoKeyEvents("app1(!focused) ", 500L);

            // Remove exclusive access
            app[1].removeExclusiveAccessToAWTEvent(rc[3]); // lose VK_C, VK_D
            // Remove other listener
            app[0].removeExclusiveAccessToAWTEvent(rc[0]); // lose VK_A, VK_B

            // Still get Y/Z
            dispatch(KeyEvent.VK_Y);
            focus[1].expectKeyEvents("app2", KeyEvent.VK_Y, 2000L);
            focus[0].expectNoKeyEvents("app1", 500L);

            // Remove rest of listeners
            app[1].removeExclusiveAccessToAWTEvent(rc[1]);
            app[0].removeExclusiveAccessToAWTEvent(rc[2]);

            // After listeners are removed, exclusive access is revoked, events
            // received as normal
            dispatch(KeyEvent.VK_A);
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_A, 100L);
            dispatch(KeyEvent.VK_B);
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_B, 100L);
            dispatch(KeyEvent.VK_C);
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_C, 100L);
            dispatch(KeyEvent.VK_D);
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_D, 100L);
            dispatch(KeyEvent.VK_Y);
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_Y, 100L);
            dispatch(KeyEvent.VK_Z);
            focus[1].expectKeyEvents("app2(focused) ", KeyEvent.VK_Z, 100L);
            focus[0].expectNoKeyEvents("app2(!focused)", 500L);

            app[0].requestFocus(focus[0]);
            assertTrue("Expected to be able to get focus", focus[0].waitForActivated(3000L));

            dispatch(KeyEvent.VK_A);
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_A, 100L);
            dispatch(KeyEvent.VK_B);
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_B, 100L);
            dispatch(KeyEvent.VK_C);
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_C, 100L);
            dispatch(KeyEvent.VK_D);
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_D, 100L);
            dispatch(KeyEvent.VK_Y);
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_Y, 100L);
            dispatch(KeyEvent.VK_Z);
            focus[0].expectKeyEvents("app1(focused) ", KeyEvent.VK_Z, 100L);
            focus[1].expectNoKeyEvents("app2(!focused)", 500L);
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests add|removeUserEventListener (exclusive access) ResourceClient
     * callback. Verify that ResourceClient's are called:
     * <ol>
     * <li>To request event for other (via requestRelease).
     * <li>Verify correct ResourceProxy and requestData.
     * <li>Verify correct operation if requestRelease() returns false.
     * <li>Should release()/notifyRelease() be called at any point?
     * </ol>
     */
    public void testResourceClientCallback_context() throws Exception
    {
        // Add exclusive access listener to app1
        Context app[] = { new Context(new AppID(11, 20), 100), new Context(new AppID(11, 21), 1), };
        try
        {
            EventListener uel[] = new EventListener[app.length];
            UserEventRepository uer[] = new UserEventRepository[app.length];
            Client rc[] = new Client[app.length];

            // Add listener so we can see changes
            EventManager em = eventmanager;
            ResourceListener rl = new ResourceListener();
            em.addResourceStatusEventListener(rl);

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // Add exclusive access listener to app1
            uel[0] = new EventListener();
            uer[0] = new UserEventRepository("app1");
            uer[0].addKey(KeyEvent.VK_A);
            uer[0].addKey(KeyEvent.VK_B);
            rc[0] = new Client();
            rc[0].reset(false);
            assertTrue("Expected exclusive access", app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // Verify that resource listener is invoked
            rl.expectUnavailEvent(uer[0], 2000L);

            // Add overlapping exclusive access listener to app2
            uel[1] = new EventListener();
            uer[1] = new UserEventRepository("app2");
            uer[1].addKey(KeyEvent.VK_B);
            uer[1].addKey(KeyEvent.VK_C);
            rc[1] = new Client();
            boolean rsvd = app[1].addUserEventListener(uel[1], rc[1], uer[1]);

            assertFalse("Should not be reserved", rsvd);

            // Verify that requestRelease is called
            rc[0].syncForEvents(1, 2000);
            assertTrue("Expected requestRelease() to be called", rc[0].requestCalled);
            assertTrue("Expected ResourceProxy to be passed", rc[0].events.size() > 0);
            assertNotNull("Expected ResourceProxy to be passed", rc[0].events.elementAt(0));
            assertTrue("Expected RepositoryDescriptor to be passed",
                    rc[0].events.elementAt(0) instanceof RepositoryDescriptor);
            assertEquals("Expected name to be that of the original reservation", uer[0].getName(),
                    ((RepositoryDescriptor) rc[0].events.elementAt(0)).getName());

            // Verify correct operation if requestRelease() returns false (which
            // is based on priority)
            // In case of contention, verify that priority is given higher
            // priority
            rl.expectNoEvents("ResourceStatus ", 500L);
            assertFalse("The ResourceClient.release() method should not have been called", rc[0].releaseCalled);
            assertFalse("The ResourceClient.notifyRelease() method should not have been called", rc[0].notifyCalled);
            rc[0].reset(false);

            // In case of contention, verify that priority is given higher
            // priority
            app[1].priority = app[0].priority + 1;
            rsvd = app[1].addUserEventListener(uel[1], rc[1], uer[1]);
            assertTrue("Should be reserved (given higher priority)", rsvd);

            // Verify that requestRelease is called
            rc[0].syncForEvents(1, 2000);
            assertTrue("Expected requestRelease() to be called", rc[0].requestCalled);
            assertTrue("Expected ResourceProxy to be passed", rc[0].events.size() > 0);
            assertNotNull("Expected ResourceProxy to be passed", rc[0].events.elementAt(0));
            assertTrue("Expected RepositoryDescriptor to be passed",
                    rc[0].events.elementAt(0) instanceof RepositoryDescriptor);
            assertEquals("Expected name to be that of the original reservation", uer[0].getName(),
                    ((RepositoryDescriptor) rc[0].events.elementAt(0)).getName());
            // Verify correct operation if requestRelease() returns false (which
            // is based on priority)
            // In case of contention, verify that priority is given higher
            // priority
            rl.expectAvailUnavailEvent(uer[0], uer[1], 2000L);
            assertTrue("The ResourceClient.release() method should have been called", rc[0].releaseCalled);
            // notifyRelease MAY have been called...
            rc[0].reset(false);
            rc[1].reset(true);

            // Verify correct operation if requestRelease() returns true
            app[0].removeUserEventListener(uel[0]);
            rsvd = app[0].addUserEventListener(uel[0], rc[0], uer[0]);
            assertTrue("Should be reserved", rsvd);

            // Verify that requestRelease is called
            rc[1].syncForEvents(1, 2000);
            assertTrue("Expected requestRelease() to be called", rc[1].requestCalled);
            assertTrue("Expected ResourceProxy to be passed", rc[1].events.size() > 0);
            assertNotNull("Expected ResourceProxy to be passed", rc[1].events.elementAt(0));
            assertTrue("Expected RepositoryDescriptor to be passed",
                    rc[1].events.elementAt(0) instanceof RepositoryDescriptor);
            assertEquals("Expected name to be that of the original reservation", uer[1].getName(),
                    ((RepositoryDescriptor) rc[1].events.elementAt(0)).getName());
            // Verify correct operation if requestRelease() returns false (which
            // is based on priority)
            // In case of contention, verify that priority is given higher
            // priority
            rl.expectAvailUnavailEvent(uer[1], uer[0], 2000L);
            assertFalse("The ResourceClient.release() method should NOT have been called", rc[1].releaseCalled);
            assertFalse("The ResourceClient.notifyRelease() method should NOT have been called", rc[1].notifyCalled);
            rc[0].reset(false);
            rc[1].reset(true);
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests add|removeResourceStatusEventListener: listeners invoked in
     * context.
     */
    public void testAddRemoveResourceStatusEventListener_context() throws Exception
    {
        Context app[] = { new Context(new AppID(11, 70)), new Context(new AppID(11, 71)), };
        EventManager em = eventmanager;
        ResourceListener[] rl = new ResourceListener[app.length];
        ResourceClient rc = new Client();
        UserEventRepository uer = new UserEventRepository("flcl");
        uer.addAllNumericKeys();

        try
        {
            // Replace Managers so we can use our own contexts
            replaceManagers();

            for (int i = 0; i < app.length; ++i)
            {
                rl[i] = new ResourceListener();
                app[i].addResourceStatusEventListener(rl[i]);
            }

            // Reserve something
            assertTrue("Expected to be able to reserve events", em.addExclusiveAccessToAWTEvent(rc, uer));

            for (int i = 0; i < app.length; ++i)
            {
                // Wait for UnavailableEvent
                rl[i].expectUnavailEvent(uer, 2000L, app[i].tg);
            }
        }
        finally
        {
            em.removeExclusiveAccessToAWTEvent(rc);
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * General-purpose event dispatch test.
     * <p>
     * Tests that dispatch implements event distribution described in MHP J.1.
     * That is:
     * <ol>
     * <li>If no app has focus and event is not in a registered event
     * repository, then nothing happens.
     * <li>If an app has the event reserved exclusively:
     * <ol type ="a">
     * <li>If the event has been reserved for exclusive AWT delivery:
     * <ol type="i">
     * <li>If reserving app is focused, distribute to that app's event queue.
     * <li>Else nothing happens.
     * </ol>
     * <li>Else send the event to the UserEventListener with exclusive access.
     * </ol>
     * <li>Else if no app has the event reserved exclusively:
     * <ol type="a">
     * <li>If an app has focus, send the event there.
     * <li>Send the event to all UserEventListeners which shared access.
     * </ol>
     * </ol>
     */
    public void testDispatch() throws Exception
    {
        EventManager em = eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        Focus focus = new Focus();

        int keyCode = KeyEvent.VK_M;

        // No app has focus, no registered events.
        // Can't really test that nothing happens (except that Focus doesn't see
        // events)
        dispatch(keyCode);
        focus.expectNoKeyEvents("!focused ", 500L);

        // Get Focus
        fm.requestActivate(focus, true);
        assertTrue("Expected to receive focus", focus.waitForActivated(3000));

        EventListener uel = new EventListener();
        EventListener uel2 = new EventListener();
        Client rc2 = new Client();

        try
        {
            // Add UserEventListener
            UserEventRepository shared = new UserEventRepository("shared");
            shared.addKey(keyCode);
            em.addUserEventListener(uel, shared);

            // Add Exclusive UserEventListener
            UserEventRepository exclusive = new UserEventRepository("exclusive");
            exclusive.addKey(keyCode + 1);
            Client rc = new Client();
            assertTrue("Expected to be able to reserve event (UserEvent)", em.addUserEventListener(uel2, rc, exclusive));

            // Add Exclusive AWT access
            UserEventRepository awt = new UserEventRepository("awt");
            awt.addKey(keyCode + 2);
            assertTrue("Expected to be able to reserve event (AWT)", em.addExclusiveAccessToAWTEvent(rc2, awt));

            // Test w/out exclusive reservations
            dispatch(keyCode);
            // - awt
            focus.expectKeyEvents("AWT ", keyCode, 2000L);
            // - shared
            uel.expectUserEvents("Shared ", keyCode, 2000L);
            // - exclusive
            uel2.expectNoUserEvents("Exclusive ", 500L);

            // Test UserEvent exclusive reservations
            dispatch(keyCode + 1);
            // - exclusive
            uel2.expectUserEvents("Exclusive ", keyCode + 1, 2000L);
            // - awt
            focus.expectNoKeyEvents("AWT ", 500L);
            // - shared
            uel.expectNoUserEvents("Shared ", 500L);

            // Test w/ AWTEvent exclusive reservations
            dispatch(keyCode + 2);
            // - awt
            focus.expectKeyEvents("AWT ", keyCode + 2, 2000L);
            // - shared
            uel.expectNoUserEvents("Shared ", 500L);
            // - exclusive
            uel2.expectNoUserEvents("Exclusive ", 500L);
        }
        finally
        {
            fm.notifyDeactivated(focus);
            em.removeUserEventListener(uel);
            em.removeUserEventListener(uel2);
            em.removeExclusiveAccessToAWTEvent(rc2);
        }
    }

    /**
     * Tests basic reservation of UserEvents. Owner receives event. Nobody else
     * does.
     * <p>
     * This test relies on CallerContextManager and FocusManager.
     */
    public void testUserEventReservation() throws Exception
    {
        Context app[] = { new Context(new AppID(10, 30)), new Context(new AppID(10, 31)),
                new Context(new AppID(10, 32)) };

        replaceManagers();
        try
        {
            int keyCode = KeyEvent.VK_N;

            // Create multiple apps
            // ...In one app add focus
            Focus focus = new Focus(app[0]);
            app[0].requestFocus(focus);
            assertTrue("Expected to receive focus", focus.waitForActivated(3000));

            // ...In one app get shared user event
            UserEventRepository uer1 = new UserEventRepository("uer1");
            EventListener uel1 = new EventListener();
            uer1.addKey(keyCode);
            app[1].addUserEventListener(uel1, uer1);

            // ...In other app request exclusive access
            UserEventRepository uer2 = new UserEventRepository("uer2");
            EventListener uel2 = new EventListener();
            uer2.addKey(keyCode);
            assertTrue("Expected to be able to reserve UserEvents", app[2].addUserEventListener(uel2, new Client(),
                    uer2));

            // dispatch event
            dispatch(keyCode);

            // only exclusive owner receives event
            uel2.expectUserEvents("Exclusive ", keyCode, 2000L);
            uel1.expectNoUserEvents("Shared ", 500L);
            focus.expectNoKeyEvents("Focused ", 100L);
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests attempt to reserve already reserve event. Owner's
     * ResourceClient.requestRelease() is invoked. Requester gets reservation if
     * requested. Verify that owner receives event, nobody else does.
     * <p>
     * This test relies on CallerContextManager and/or FocusManager.
     */
    public void testUserEventReservation_reserved() throws Exception
    {
        // Create multiple apps
        // Will reserve exclusively in one app
        // Then take away in the other
        // Then attempt to take back
        // Then actuall take back
        Context app[] = { new Context(new AppID(10, 40)), new Context(new AppID(10, 41)), };
        EventListener[] uel = { new EventListener(), new EventListener(), };
        UserEventRepository[] uer = { new UserEventRepository("1"), new UserEventRepository("2") };
        Client[] rc = { new Client(), new Client(), };
        int[] keys = { KeyEvent.VK_X, KeyEvent.VK_Y, KeyEvent.VK_Z };
        for (int i = 0; i < uer.length; ++i)
            for (int j = 0; j < keys.length; ++j)
                uer[i].addKey(keys[j]);

        replaceManagers();
        try
        {
            // Reserve it initially in one app
            assertTrue("Expected to be able to reserve event", app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // Then request the reservation in another app
            assertTrue("Expected to be able to reserve event", app[1].addUserEventListener(uel[1], rc[1], uer[1]));

            // ResourceClient should've been consulted
            assertTrue("RequestRelease should've been called", rc[0].requestCalled);
            assertFalse("Release should NOT've been called", rc[0].releaseCalled);
            rc[0].reset();

            // Dispatch event
            for (int i = 0; i < keys.length; ++i)
            {
                dispatch(keys[i]);

                // Listener should be notified
                uel[1].expectUserEvents("app1", keys[i], 2000L);
                uel[0].expectNoUserEvents("app0", 500L);
            }

            // Attempt to take back, but be denied by ResourceClient
            rc[1].REQUEST = false;
            assertFalse("Expected to be denied reservation of event",
                    app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // ResourceClient should've been consulted
            assertTrue("RequestRelease should've been called", rc[1].requestCalled);
            assertFalse("Release should NOT've been called", rc[1].releaseCalled);
            rc[1].reset();

            // Dispatch event (should be unchanged)
            for (int i = 0; i < keys.length; ++i)
            {
                dispatch(keys[i]);

                // Listener should be notified
                uel[1].expectUserEvents("app1", keys[i], 2000L);
                uel[0].expectNoUserEvents("app0", 500L);
            }

            // Attempt again, this time should be allowed
            rc[1].REQUEST = true;
            assertTrue("Expected to be granted reservation of event",
                    app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // ResourceClient should've been consulted
            assertTrue("RequestRelease should've been called", rc[1].requestCalled);
            assertFalse("Release should NOT've been called", rc[1].releaseCalled);
            rc[1].reset();

            // Dispatch event
            for (int i = 0; i < keys.length; ++i)
            {
                dispatch(keys[i]);

                // Listener should be notified
                uel[0].expectUserEvents("app0", keys[i], 2000L);
                uel[1].expectNoUserEvents("app1", 500L);
            }
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }

    }

    /**
     * Tests attempt to reserve already reserved events. Events are currently
     * reserved by multiple UERs/apps. All owners must be queried. All owners
     * must allow for request to be granted. Verify new owner gets events.
     * Verify old owners lose all events in the original requested UER.
     * <p>
     * This test relies on CallerContextManager and FocusManager.
     */
    public void testUserEventReservation_reservedMultiple() throws Exception
    {
        // Create multiple apps
        // Will reserve exclusively in one app
        // Then take away in the other
        // Then attempt to take back
        // Then actuall take back
        Context app[] = { new Context(new AppID(10, 50)), new Context(new AppID(10, 51)),
                new Context(new AppID(10, 52)), };
        EventListener[] uel = { new EventListener(), new EventListener(), new EventListener(), };
        UserEventRepository[] uer = { new UserEventRepository("1"), new UserEventRepository("2"),
                new UserEventRepository("3"), };
        Client[] rc = { new Client(), new Client(), new Client(), };
        int[][] keys = { { KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C, KeyEvent.VK_D, },
                { KeyEvent.VK_M, KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_P, },
                // Overlaps both keys[0] and keys[1]
                { KeyEvent.VK_A, KeyEvent.VK_M, KeyEvent.VK_B, KeyEvent.VK_N, }, };
        for (int i = 0; i < uer.length; ++i)
            for (int j = 0; j < keys[i].length; ++j)
                uer[i].addKey(keys[i][j]);

        replaceManagers();
        try
        {
            // Reserve events
            assertTrue("Expected to be able to reserve event", app[0].addUserEventListener(uel[0], rc[0], uer[0]));
            assertTrue("Expected to be able to reserve event", app[1].addUserEventListener(uel[1], rc[1], uer[1]));

            // Verify that we receive these events
            for (int j = 0; j < keys.length - 1; ++j)
            {
                for (int i = 0; i < keys[j].length; ++i)
                {
                    dispatch(keys[j][i]);
                    uel[j].expectUserEvents(keys[j][i], 2000L);
                    uel[(j + 1) % uel.length].expectNoUserEvents(500L);
                    uel[(j + uel.length - 1) % uel.length].expectNoUserEvents(100L);
                }
            }

            // Attempt to reserve (but denied by one)
            rc[0].reset(false);
            rc[1].reset(true);
            assertFalse("Expected to be denied event reservation", app[2].addUserEventListener(uel[2], rc[2], uer[2]));

            // ResourceClient should've been consulted
            assertTrue("RequestRelease should've been called", rc[0].requestCalled);
            assertFalse("Release should NOT've been called", rc[0].releaseCalled);
            // ResourceClient rc[1] MIGHT've been consulted (and given up
            // ownership)

            // Test dispatching of events (to owners only)
            for (int j = 0; j < keys.length - 1; ++j)
            {
                for (int i = 0; i < keys[j].length; ++i)
                {
                    dispatch(keys[j][i]);
                    if (rc[j].requestCalled && rc[j].REQUEST) // gave up
                                                              // ownership
                        uel[j].expectNoUserEvents("released ", 500L);
                    else
                        uel[j].expectUserEvents("!released ", keys[j][i], 2000L);
                    uel[(j + 1) % uel.length].expectNoUserEvents(500L);
                    uel[(j + uel.length - 1) % uel.length].expectNoUserEvents(100L);
                }
            }

            // Re-acquire if gave up
            if (rc[1].requestCalled && rc[1].REQUEST)
                assertTrue("Expected to be able to reserve event", app[1].addUserEventListener(uel[1], rc[1], uer[1]));

            // Attempt to reserve (but denied by other one)
            rc[0].reset(true);
            rc[1].reset(false);
            assertFalse("Expected to be denied event reservation", app[2].addUserEventListener(uel[2], rc[2], uer[2]));

            // ResourceClient should've been consulted
            assertTrue("RequestRelease should've been called", rc[1].requestCalled);
            assertFalse("Release should NOT've been called", rc[1].releaseCalled);
            // ResourceClient rc[0] MIGHT've been consulted (and given up
            // ownership)

            // Test dispatching of events (to owners only)
            for (int j = 0; j < keys.length - 1; ++j)
            {
                for (int i = 0; i < keys[j].length; ++i)
                {
                    dispatch(keys[j][i]);
                    if (rc[j].requestCalled && rc[j].REQUEST) // gave up
                                                              // ownership
                        uel[j].expectNoUserEvents("released ", 500L);
                    else
                        uel[j].expectUserEvents(j + ":" + i + " !released ", keys[j][i], 2000L);
                    uel[(j + 1) % uel.length].expectNoUserEvents(500L);
                    uel[(j + uel.length - 1) % uel.length].expectNoUserEvents(100L);
                }
            }

            // Re-acquire if gave up
            if (rc[0].requestCalled && rc[0].REQUEST)
                assertTrue("Expected to be able to reserve event", app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // Reserve successfully
            rc[0].reset(true);
            rc[1].reset(true);
            assertTrue("Expected to be able to reserve event", app[2].addUserEventListener(uel[2], rc[2], uer[2]));

            // ResourceClients should've been consulted
            assertTrue("RequestRelease should've been called", rc[0].requestCalled);
            assertFalse("Release should NOT've been called", rc[0].releaseCalled);
            rc[0].reset(true);
            assertTrue("RequestRelease should've been called", rc[1].requestCalled);
            assertFalse("Release should NOT've been called", rc[1].releaseCalled);
            rc[1].reset(true);

            // Test dispatching of events to new owner
            for (int i = 0; i < keys[2].length; ++i)
            {
                dispatch(keys[2][i]);
                uel[2].expectUserEvents(keys[2][i], 2000L);
                uel[1].expectNoUserEvents(500L);
                uel[0].expectNoUserEvents(100L);
            }
            // Test NO dispatching of events to old owner (including additional
            // events)
            for (int j = 0; j < keys.length - 1; ++j)
            {
                for (int i = 0; i < keys[j].length; ++i)
                {
                    dispatch(keys[j][i]);
                    uel[j].expectNoUserEvents(j + ":" + (char) keys[j][i], 500L);
                }
            }
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests that reservation of:
     * <ul>
     * <li>KEY_PRESSED implies KEY_RELEASED event
     * <li>KEY_RELEASED implies KEY_PRESSED event
     * <li>KEY_TYPED implies corresponding KEY_PRESSED/KEY_RELEASED event(s)
     * </ul>
     */
    public void testUserEventReservation_correspondingEvents() throws Exception
    {
        // Test by adding a shared listener for all events
        // Reserving individual events, shared listener shouldn't be called
        // Releasing individual events, shared listener should still be called

        EventManager em = eventmanager;
        // FocusManager fm =
        // (FocusManager)ManagerManager.getInstance(FocusManager.class);
        // Focus focus = new Focus();

        int[] keys = { KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C };
        char[] chars = { 'a', 'b', 'c' };
        UserEvent[] events = { new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, keys[0], 0, 0L),
                new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, chars[1], 0L),
                new UserEvent(COMPONENT, UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_RELEASED, keys[2], 0, 0L), };

        UserEventRepository exclUer = new UserEventRepository("exclusive");
        UserEventRepository sharedUer = new UserEventRepository("shared");
        for (int i = 0; i < events.length; ++i)
        {
            exclUer.addUserEvent(events[i]);

            sharedUer.addKey(keys[i]);
            if (events[i].getCode() == KeyEvent.VK_UNDEFINED) sharedUer.addUserEvent(events[i]);
        }

        EventListener excl = new EventListener();
        EventListener shared = new EventListener();

        try
        {
            // Add the listener
            em.addUserEventListener(shared, sharedUer);

            // Ensure receives events
            for (int i = 0; i < events.length; ++i)
            {
                dispatch(keys[i], chars[i]);

                shared.expectUserEvents(keys[i], events[i].getKeyChar(), 2000L);
                shared.expectNoUserEvents(500L);
            }

            // Reserve events
            assertTrue("Expected to be able to reserve events", em.addUserEventListener(excl, new Client(), exclUer));

            // Ensure excl receives events
            // Ensure shared receives NOTHING
            for (int i = 0; i < events.length; ++i)
            {
                dispatch(keys[i], chars[i]);

                excl.expectUserEvent("Expect event [" + i + "]", events[i], 2000L);
                shared.expectNoUserEvents("Expect no corresponding events[" + i + "] ", 500L);
            }
        }
        finally
        {
            em.removeUserEventListener(excl);
            em.removeUserEventListener(shared);
        }
    }

    /**
     * Tests that modifying a repository after reservation, doesn't affect
     * things therafter. Here the ResourceClient methods shouldn't be called
     * with the updated repository.
     */
    public void testUserEventReservation_updatedRepository_ResourceClient() throws Exception
    {
        // Add exclusive access listener to app1
        Context app[] = { new Context(new AppID(11, 20), 100), new Context(new AppID(11, 21), 1), };
        try
        {
            EventListener uel[] = new EventListener[app.length];
            UserEventRepository uer[] = new UserEventRepository[app.length];
            Client rc[] = new Client[app.length];

            // Add listener so we can see changes
            EventManager em = eventmanager;

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // Add exclusive access listener to app1
            uel[0] = new EventListener();
            uer[0] = new UserEventRepository("app1");
            uer[0].addKey(KeyEvent.VK_A);
            rc[0] = new Client();
            rc[0].reset(false);
            assertTrue("Expected exclusive access", app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // Create a copy of the UserEventRepository
            UserEventRepository uerCopy = new UserEventRepository("app1");
            UserEvent[] events = uer[0].getUserEvent();
            for (int i = 0; i < events.length; ++i)
                uerCopy.addUserEvent(events[i]);

            // Add overlapping exclusive access listener to app2
            uel[1] = new EventListener();
            uer[1] = new UserEventRepository("app2");
            uer[1].addKey(KeyEvent.VK_A);
            rc[1] = new Client();

            assertFalse("Should not be reserved", app[1].addUserEventListener(uel[1], rc[1], uer[1]));

            // Verify that requestRelease is called
            rc[0].syncForEvents(1, 2000);
            assertTrue("Expected requestRelease() to be called", rc[0].requestCalled);
            assertTrue("Expected ResourceProxy to be passed", rc[0].events.size() > 0);
            assertNotNull("Expected ResourceProxy to be passed", rc[0].events.elementAt(0));
            assertTrue("Expected UserEventRepository to be passed",
                    rc[0].events.elementAt(0) instanceof UserEventRepository);
            UserEventRepositoryTest.assertEquals("Expected original repository given to requestRelease", uerCopy,
                    (UserEventRepository) rc[0].events.elementAt(0));

            // Modify the UserEventRepository
            uer[0].addAllArrowKeys();
            uer[0].addAllColourKeys();
            uer[0].addAllNumericKeys();
            uer[0].removeUserEvent(events[0]);

            // Re-request by app2
            assertFalse("Should not be reserved", app[1].addUserEventListener(uel[1], rc[1], uer[1]));

            // Verify that requestRelease is called
            rc[0].syncForEvents(1, 2000);
            assertTrue("Expected requestRelease() to be called", rc[0].requestCalled);
            assertTrue("Expected ResourceProxy to be passed", rc[0].events.size() > 0);
            assertNotNull("Expected ResourceProxy to be passed", rc[0].events.elementAt(0));
            assertTrue("Expected UserEventRepository to be passed",
                    rc[0].events.elementAt(0) instanceof UserEventRepository);
            UserEventRepositoryTest.assertEquals("Expected unmodified repository given to requestRelease", uerCopy,
                    (UserEventRepository) rc[0].events.elementAt(0));
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }

    }

    /**
     * Tests that removeUserEventListener() releases the event reservation. On a
     * subsequent reservation attempt, the ResourceClient should <i>not</i> be
     * queried.
     * <p>
     * This test relies on CallerContext implementation.
     */
    public void testRemoveListenerReleasesReservation() throws Exception
    {
        // Add a listener for exclusive access
        // Dispatch events to it
        // Remove the listener
        // Reserve for another RC/listener/uer
        // the original RC should not be invoked

        Context app[] = { new Context(new AppID(10, 40)), new Context(new AppID(10, 41)), };
        try
        {
            EventListener uel[] = new EventListener[app.length];
            UserEventRepository uer[] = new UserEventRepository[app.length];
            Client rc[] = new Client[app.length];
            int[] keys = { KeyEvent.VK_R, KeyEvent.VK_S, KeyEvent.VK_T };

            for (int i = 0; i < app.length; ++i)
            {
                uel[i] = new EventListener();
                rc[i] = new Client();
                uer[i] = new UserEventRepository("app" + (i + 1));
                for (int key = 0; key < keys.length; ++key)
                    uer[i].addKey(keys[key]);
            }

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // Add listener to app1
            assertTrue("Expected to be able to reserve UserEvents", app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // Ensure event can be dispatched
            for (int i = 0; i < keys.length; ++i)
            {
                dispatch(keys[i]);
                uel[0].expectUserEvents("Original: ", keys[i], 2000L);
            }

            // Remove listener
            app[0].removeUserEventListener(uel[0]);

            // Ensure events aren't dispatched
            for (int i = 0; i < keys.length; ++i)
            {
                dispatch(keys[i]);
                uel[0].expectNoUserEvents("Original(Removed): ", 500L);
            }

            // Now request ownership for another owner
            assertTrue("Expected to be able to reserve UserEvents", app[1].addUserEventListener(uel[1], rc[1], uer[1]));

            assertFalse("Original ResourceClient should NOT be consulted!", rc[0].requestCalled);
            assertFalse("Original ResourceClient should NOT be told!", rc[0].releaseCalled);
            assertFalse("Original ResourceClient should NOT be notified!", rc[0].notifyCalled);
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests that removeExclusiveAccessToAWTEvent() releases the event
     * reservation. On a subsequent reservation attempt, the ResourceClient
     * should <i>not</i> be queried.
     */
    public void testRemoveClientReleasesReservation() throws Exception
    {
        // Add a listener for exclusive access
        // Dispatch events to it
        // Remove the listener
        // Reserve for another RC/listener/uer
        // the original RC should not be invoked

        Context app[] = { new Context(new AppID(10, 50)), new Context(new AppID(10, 51)), };
        try
        {
            Focus focus[] = new Focus[app.length];
            UserEventRepository uer[] = new UserEventRepository[app.length];
            Client rc[] = new Client[app.length];
            int[] keys = { KeyEvent.VK_R, KeyEvent.VK_S, KeyEvent.VK_T };

            for (int i = 0; i < app.length; ++i)
            {
                focus[i] = new Focus(app[i]);
                rc[i] = new Client();
                uer[i] = new UserEventRepository("app" + (i + 1));
                for (int key = 0; key < keys.length; ++key)
                    uer[i].addKey(keys[key]);
            }

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // Add listener to app1
            assertTrue("Expected to be able to reserve AWTEvents", app[0].addExclusiveAccessToAWTEvent(rc[0], uer[0]));

            // Ensure event can be dispatched
            app[0].requestFocus(focus[0]);
            assertTrue("Expected to gain focus", focus[0].waitForActivated(3000L));
            for (int i = 0; i < keys.length; ++i)
            {
                dispatch(keys[i]);
                focus[0].expectKeyEvents("Original: ", keys[i], 2000L);
            }

            // Remove listener
            app[0].removeExclusiveAccessToAWTEvent(rc[0]);

            // Now request ownership for another owner
            assertTrue("Expected to be able to reserve AWtEvents", app[1].addExclusiveAccessToAWTEvent(rc[1], uer[1]));

            assertFalse("Original ResourceClient should NOT be consulted!", rc[0].requestCalled);
            assertFalse("Original ResourceClient should NOT be told!", rc[0].releaseCalled);
            assertFalse("Original ResourceClient should NOT be notified!", rc[0].notifyCalled);
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests that following requestRelease():true, the previously installed
     * listeners should be removed and no longer receive events.
     */
    public void testRequestReleaseRemovesListener() throws Exception
    {
        // Add a listener for exclusive access
        // Dispatch events to it
        // Add another listener for exclusive access, it should get it (overlaps
        // previous)
        // Dispatch events
        // Events should NOT go to the old listener
        Context app[] = { new Context(new AppID(10, 60)), new Context(new AppID(10, 61)), };
        try
        {
            EventListener uel[] = new EventListener[app.length];
            UserEventRepository uer[] = new UserEventRepository[app.length];
            Client rc[] = new Client[app.length];
            int[] keys = { KeyEvent.VK_H, KeyEvent.VK_I, KeyEvent.VK_J };

            for (int i = 0; i < app.length; ++i)
            {
                uel[i] = new EventListener();
                rc[i] = new Client();
                uer[i] = new UserEventRepository("app" + (i + 1));
            }
            for (int key = 0; key < keys.length; ++key)
                uer[0].addKey(keys[key]);
            uer[1].addKey(keys[0]); // only add one of the keys, leaving the
                                    // others

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // Add listener to app1
            assertTrue("Expected to be able to reserve UserEvents", app[0].addUserEventListener(uel[0], rc[0], uer[0]));

            // Ensure event can be dispatched
            for (int i = 0; i < keys.length; ++i)
            {
                dispatch(keys[i]);
                uel[0].expectUserEvents("Original: ", keys[i], 2000L);
            }

            // Request and acquire access via another app
            assertTrue("Expected to be able to reserve UserEvents", app[1].addUserEventListener(uel[1], rc[1], uer[1]));
            assertTrue("Expected ResourceClient to be consulted", rc[0].requestCalled);

            // Ensure event can be dispatched, but none to previous owner!
            dispatch(keys[0]);
            uel[1].expectUserEvents("Second: ", keys[0], 2000L);
            for (int i = 1; i < keys.length; ++i)
                dispatch(keys[i]);
            uel[0].expectNoUserEvents("Original(Released): ", 500L);
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }

    }

    /**
     * Tests that listeners added for shared access are removed upon
     * destruction.
     */
    public void testDestroyed_listeners() throws Exception
    {
        doTestDestroyed(ADD_SHARED);
    }

    /**
     * Tests that listeners added for exclusive access are removed upon
     * destruction.
     */
    public void testDestroyed_exclusive() throws Exception
    {
        doTestDestroyed(ADD_EXCLUSIVE);
    }

    private void doTestDestroyed(AddListener add) throws Exception
    {
        Context app[] = { new Context(new AppID(10, 70)), new Context(new AppID(10, 71)), };
        try
        {
            EventListener uel[] = new EventListener[app.length];
            UserEventRepository uer[] = new UserEventRepository[app.length];
            int keyCode = KeyEvent.VK_L;

            for (int i = 0; i < app.length; ++i)
            {
                uel[i] = new EventListener();
                uer[i] = new UserEventRepository("app" + (i + 1));
                uer[i].addKey(keyCode + i);
            }

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // Add listeners
            for (int i = 0; i < app.length; ++i)
                app[i].addUserEventListener(add, uel[i], uer[i]);

            // Ensure that each gets their events
            for (int i = 0; i < app.length; ++i)
            {
                dispatch(keyCode + i);
                uel[i].expectUserEvents(keyCode + i, 2000L);
                for (int j = 0; j < app.length; ++j)
                    if (j != i) uel[j].expectNoUserEvents(500L);
            }

            // Destroy them, and ensure that they don't get any events
            for (int i = 0; i < app.length; ++i)
            {
                app[i].cleanup();

                dispatch(keyCode + i);
                uel[i].expectNoUserEvents(500L);
            }
        }
        finally
        {
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }
    }

    /**
     * Tests that AWT access is released upon destruction.
     */
    public void testDestroyed_awt() throws Exception
    {
        Context app[] = { new Context(new AppID(10, 80)), new Context(new AppID(10, 81)),
                new Context(new AppID(10, 82)) };
        EventManager em = eventmanager;
        ResourceListener rl = new ResourceListener();
        try
        {
            em.addResourceStatusEventListener(rl);
            UserEventRepository uer[] = new UserEventRepository[app.length];
            Client[] rc = new Client[app.length];
            int keyCode = KeyEvent.VK_L;

            for (int i = 0; i < app.length; ++i)
            {
                uer[i] = new UserEventRepository("awt" + (i + 1));
                uer[i].addKey(keyCode + i);
                rc[i] = new Client();
                rc[i].REQUEST = false;
            }

            // Replace Managers so we can use our own contexts
            replaceManagers();

            // Add reservations
            for (int i = 0; i < app.length - 1; ++i)
            {
                app[i].addExclusiveAccessToAWTEvent(rc[i], uer[i]);

                // Expect Unavail events
                rl.expectUnavailEvent(uer[i], 2000L);
            }

            int last = app.length - 1;

            // Destroy them, expect avail events, and ability to reserve
            for (int i = 0; i < app.length - 1; ++i)
            {
                app[i].cleanup();

                // expect avail events
                rl.expectAvailEvent(uer[i], 2000L);

                assertTrue("Expected to be able to reserve AWTEvent after owner died",
                        app[last].addExclusiveAccessToAWTEvent(rc[last], uer[i]));

                // expect unavail event
                rl.expectUnavailEvent(uer[i], 2000L);
            }
        }
        finally
        {
            em.removeResourceStatusEventListener(rl);
            for (int i = 0; i < app.length; ++i)
                app[i].cleanup();
        }

    }

    /**
     * Used to dispatch an event to the UserEvent handlers.
     * 
     * @param e
     *            the event to dispatch
     */
    protected void dispatch(AWTEvent e) throws Exception
    {
        org.cablelabs.impl.manager.EventManager em = (org.cablelabs.impl.manager.EventManager) org.cablelabs.impl.manager.ManagerManager.getInstance(org.cablelabs.impl.manager.EventManager.class);

        em.dispatch(e);
    }

    /**
     * Used to dispatch an event to the UserEvent handlers.
     * 
     * @param e
     *            the user event to dispatch
     */
    protected void dispatch(UserEvent e) throws Exception
    {
        dispatch(new KeyEvent(COMPONENT, e.getType(), System.currentTimeMillis(), e.getModifiers(), e.getCode(),
                e.getKeyChar()));
    }

    /**
     * Used to dispatch an event to the UserEvent handlers.
     */
    protected void dispatch(int vk, char vc) throws Exception
    {
        dispatch(new KeyEvent(COMPONENT, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, vk,
                UserEventTest.CHAR_UNDEFINED));
        if (vc != UserEventTest.CHAR_UNDEFINED)
            dispatch(new KeyEvent(COMPONENT, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED,
                    vc));
        dispatch(new KeyEvent(COMPONENT, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, vk,
                UserEventTest.CHAR_UNDEFINED));
    }

    /**
     * Used to dispatch an event to the UserEvent handlers.
     */
    protected void dispatch(int vk) throws Exception
    {
        dispatch(new KeyEvent(COMPONENT, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, vk,
                UserEventTest.CHAR_UNDEFINED));
        dispatch(new KeyEvent(COMPONENT, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, vk,
                UserEventTest.CHAR_UNDEFINED));
    }

    protected static final Component COMPONENT = new Component()
    {/* empty */
    };

    /**
     * Dummy implementation of ResourceClient. TODO: could be reused elsewhere
     * or consolidated
     */
    public static class Client extends BasicListener implements ResourceClient
    {
        public boolean requestCalled;

        public boolean releaseCalled;

        public boolean notifyCalled;

        public boolean REQUEST = true;

        public void reset(boolean request)
        {
            requestCalled = false;
            releaseCalled = false;
            notifyCalled = false;
            this.REQUEST = request;
            super.reset();
        }

        public boolean requestRelease(ResourceProxy p, Object d)
        {
            requestCalled = true;
            event(p);
            return REQUEST;
        }

        public void release(ResourceProxy p)
        {
            releaseCalled = true;
            event(p);
        }

        public void notifyRelease(ResourceProxy p)
        {
            notifyCalled = true;
            event(p);
        }
    }

    public static class BasicListener
    {
        public Vector events = new Vector();

        public ThreadGroup tg;

        public CallerContext cc;

        protected synchronized void event(Object e)
        {
            events.addElement(e);
            tg = Thread.currentThread().getThreadGroup();
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            cc = ccm.getCurrentContext();

            notifyAll();
        }

        public void reset()
        {
            events.removeAllElements();
        }

        public synchronized void syncForEvents(int n, long millis) throws InterruptedException
        {
            waitForEvents(n, millis);
        }

        public void waitForEvents(int n, long millis) throws InterruptedException
        {
            long end = System.currentTimeMillis() + millis;
            while (millis > 0 && events.size() < n)
            {
                wait(millis);

                millis = end - System.currentTimeMillis();
            }
        }

        public void expectNoEvents(String msg, long timeout) throws InterruptedException
        {
            syncForEvents(1, timeout);
            assertEquals(msg + "Expected no events - " + events, 0, events.size());
        }
    }

    /**
     * Dummy implementation of UserEventListener. TODO: Could be used elsewhere
     * or consolidated
     */
    public static class EventListener extends BasicListener implements UserEventListener
    {
        public synchronized void userEventReceived(UserEvent e)
        {
            event(e);
        }

        public void expectUserEvents(int keyCode, long timeout) throws InterruptedException
        {
            expectUserEvents("", keyCode, timeout, null);
        }

        public void expectUserEvents(int keyCode, long timeout, ThreadGroup group) throws InterruptedException
        {
            expectUserEvents("", keyCode, timeout, group);
        }

        public void expectUserEvents(String msg, int keyCode, long timeout) throws InterruptedException
        {
            expectUserEvents(msg, keyCode, timeout, null);
        }

        public synchronized void expectUserEvents(String msg, int keyCode, long timeout, ThreadGroup group)
                throws InterruptedException
        {
            syncForEvents(2, timeout);

            assertEquals(msg + "Expected listener to be called", 2, events.size());
            assertEquals(msg + "Expected specified key event to be dispatched", keyCode,
                    ((UserEvent) events.elementAt(0)).getCode());
            assertEquals(msg + "Expected specified key event to be dispatched", keyCode,
                    ((UserEvent) events.elementAt(1)).getCode());
            if (group != null) assertSame(msg + "Expected same ThreadGroup to be used during invocation", group, tg);
            reset();
        }

        public void expectUserEvents(int keyCode, char keyChar, long timeout) throws InterruptedException
        {
            expectUserEvents("", keyCode, keyChar, timeout, null);
        }

        public void expectUserEvents(int keyCode, char keyChar, long timeout, ThreadGroup group)
                throws InterruptedException
        {
            expectUserEvents("", keyCode, keyChar, timeout, group);
        }

        public void expectUserEvents(String msg, int keyCode, char keyChar, long timeout) throws InterruptedException
        {
            expectUserEvents(msg, keyCode, keyChar, timeout, null);
        }

        public synchronized void expectUserEvents(String msg, int keyCode, char keyChar, long timeout, ThreadGroup group)
                throws InterruptedException
        {
            int nExpected = (keyChar == KeyEvent.CHAR_UNDEFINED || keyChar == 0) ? 2 : 3;
            syncForEvents(nExpected, timeout);

            assertEquals(msg + "Expected listener to be called", nExpected, events.size());
            assertEquals(msg + "Expected specified key event to be dispatched", keyCode,
                    ((UserEvent) events.elementAt(0)).getCode());
            if (nExpected == 3)
                assertEquals(msg + "Expected specified key event to be dispatched", keyChar,
                        ((UserEvent) events.elementAt(1)).getKeyChar());
            assertEquals(msg + "Expected specified key event to be dispatched", keyCode,
                    ((UserEvent) events.elementAt(nExpected - 1)).getCode());
            if (group != null) assertSame(msg + "Expected same ThreadGroup to be used during invocation", group, tg);
            reset();
        }

        public void expectUserEvent(String msg, UserEvent ue, long timeout) throws Exception
        {
            expectUserEvent(msg, ue, timeout, null);
        }

        public void expectUserEvent(UserEvent ue, long timeout) throws Exception
        {
            expectUserEvent("", ue, timeout, null);
        }

        public synchronized void expectUserEvent(String msg, UserEvent ue, long timeout, ThreadGroup group)
                throws Exception
        {
            syncForEvents(2, timeout);

            assertEquals(msg + "Expected listener to be called", 1, events.size());
            UserEvent e = (UserEvent) events.elementAt(0);
            assertEquals(msg + "Expected specified key type to be dispatched", ue.getType(), e.getType());
            assertEquals(msg + "Expected specified key event to be dispatched", ue.getCode(), e.getCode());
            assertEquals(msg + "Expected specified key char to be dispatched", ue.getKeyChar(), e.getKeyChar());
            if (group != null) assertSame(msg + "Expected same ThreadGroup to be used during invocation", group, tg);
            reset();
        }

        public void expectNoUserEvents(long timeout) throws InterruptedException
        {
            expectNoEvents("", timeout);
        }

        public synchronized void expectNoUserEvents(String msg, long timeout) throws InterruptedException
        {
            expectNoEvents(msg, timeout);
        }
    }

    /**
     * Dummy implementation of ResourceStatusListener. TODO: Could be used
     * elsewhere or consolidated
     */
    public static class ResourceListener extends BasicListener implements ResourceStatusListener
    {
        public void statusChanged(ResourceStatusEvent e)
        {
            event(e);
        }

        public void expectUnavailEvent(UserEventRepository unavail, long timeout) throws Exception
        {
            expectUnavailEvent(unavail, timeout, null);
        }

        public synchronized void expectUnavailEvent(UserEventRepository unavail, long timeout, ThreadGroup group)
                throws Exception
        {
            syncForEvents(1, timeout);
            assertEquals("Expected a UserEventUnavailableEvent", 1, events.size());
            checkResourceEvent((ResourceStatusEvent) events.elementAt(0), unavail, true);
            if (group != null) assertSame("Expected listener to be called in same context", group, tg);
            reset();
        }

        public synchronized void expectAvailEvent(UserEventRepository avail, long timeout) throws Exception
        {
            syncForEvents(1, timeout);
            assertEquals("Expected a UserEventAvailableEvent", 1, events.size());
            checkResourceEvent((ResourceStatusEvent) events.elementAt(0), avail, false);
            reset();
        }

        public synchronized void expectAvailUnavailEvent(UserEventRepository avail, UserEventRepository unavail,
                long timeout) throws Exception
        {
            syncForEvents(2, timeout);
            assertEquals("Expected ResourceStatusEvents", 2, events.size());

            // Expect first to be AvailEvent
            checkResourceEvent((ResourceStatusEvent) events.elementAt(0), avail, false);
            // Expect last to be UnavailEvent
            checkResourceEvent((ResourceStatusEvent) events.elementAt(1), unavail, true);
            reset();
        }

        private void checkResourceEvent(ResourceStatusEvent e, UserEventRepository expected, boolean unavail)
        {
            if (unavail)
                assertTrue("Expected a UserEventUnavailableEvent", e instanceof UserEventUnavailableEvent);
            else
                assertTrue("Expected a UserEventAvailableEvent", e instanceof UserEventAvailableEvent);

            UserEventRepository actual = (UserEventRepository) e.getSource();
            assertNotNull("Expected a UserEventRepository for a source", actual);
            checkUserEvents(expected, actual);
        }

        private void checkUserEvents(UserEventRepository expected, UserEventRepository actual)
        {
            UserEvent[] array0 = expected.getUserEvent();
            UserEvent[] array1 = actual.getUserEvent();

            assertNotNull("UserEvents array should not be null", array1);
            assertEquals("Unexpected number of userEvents in repository", array0.length, array1.length);

            for (int i = 0; i < array0.length; ++i)
            {
                boolean found = false;
                for (int j = 0; j < array1.length; ++j)
                {
                    if (array0[i].getFamily() == array1[j].getFamily() && array0[i].getType() == array1[j].getType()
                            && array0[i].getCode() == array1[j].getCode()
                            && array0[i].getKeyChar() == array1[j].getKeyChar()
                            && array0[i].getModifiers() == array1[j].getModifiers())
                    {
                        found = true;
                        break;
                    }
                }

                assertTrue("Expected to find event " + array0[i], found);
            }
        }
    }

    /**
     * Replacement CallerContext implementation. Provides calls to the
     * EventManager that will occur within the context's context. TODO: Could be
     * used elsewhere or consolidated
     */
    public static class Context extends DummyContext
    {
        public AppID id;

        public int priority;

        FocusContext focused;

        private boolean destroyed;

        public Context(AppID id)
        {
            this.id = id;
            this.priority = id.getOID();
        }

        public Context(AppID id, int priority)
        {
            this.id = id;
            this.priority = priority;
        }

        public void cleanup() throws Exception
        {
            if (destroyed) return;
            destroyed = true;

            doRun(new Runnable()
            {
                public void run()
                {
                    for (java.util.Enumeration e = callbackData.elements(); e.hasMoreElements();)
                    {
                        CallbackData data = (CallbackData) e.nextElement();
                        data.destroy(Context.this);
                    }
                    if (focused != null)
                    {
                        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
                        fm.notifyDeactivated(focused);
                    }
                }
            });
            dispose();
        }

        protected void doRun(Runnable r)
        {
            try
            {
                runInContextSync(r);
            }
            catch (java.lang.reflect.InvocationTargetException e)
            {
                Throwable e2 = e.getTargetException();
                if (e2 instanceof RuntimeException)
                    throw (RuntimeException) e2;
                else if (e2 instanceof Error)
                    throw (Error) e2;
                else
                {
                    e2.printStackTrace();
                    fail("Cannot handle exception");
                }
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail("Cannot handle exception");
            }
        }

        public Object get(Object key)
        {
            if (key == APP_ID)
                return id;
            else if (key == APP_PRIORITY)
                return new Integer(priority);
            else
                return super.get(key);
        }

        public void addUserEventListener(final UserEventListener l, final UserEventRepository r)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = EventManager.getInstance();

                    em.addUserEventListener(l, r);
                }
            });
        }

        public boolean addUserEventListener(final UserEventListener l, final ResourceClient c,
                final UserEventRepository r)
        {
            final boolean rc[] = { false };
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = EventManager.getInstance();

                    rc[0] = em.addUserEventListener(l, c, r);
                }
            });
            return rc[0];
        }

        public void addUserEventListener(final AddListener add, final UserEventListener uel,
                final UserEventRepository uer)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = EventManager.getInstance();

                    add.add(em, uel, uer);
                }
            });
        }

        public void removeUserEventListener(final UserEventListener l)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = EventManager.getInstance();

                    em.removeUserEventListener(l);
                }
            });
        }

        public void addResourceStatusEventListener(final ResourceStatusListener l)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = EventManager.getInstance();

                    em.addResourceStatusEventListener(l);
                }
            });
        }

        public void removeResourceStatusEventListener(final ResourceStatusListener l)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = EventManager.getInstance();

                    em.removeResourceStatusEventListener(l);
                }
            });
        }

        public boolean addExclusiveAccessToAWTEvent(final ResourceClient c, final UserEventRepository r)
        {
            final boolean rc[] = { false };
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = EventManager.getInstance();

                    rc[0] = em.addExclusiveAccessToAWTEvent(c, r);
                }
            });
            return rc[0];
        }

        public void removeExclusiveAccessToAWTEvent(final ResourceClient c)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = EventManager.getInstance();

                    em.removeExclusiveAccessToAWTEvent(c);
                }
            });
        }

        public void requestFocus(Focus focus)
        {
            FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
            if (focused != null) fm.notifyDeactivated(focused);
            fm.requestActivate(focus, true);
            focused = focus;
        }
    }

    /**
     * Implementation of a <code>FocusContext</code> used in testing AWT
     * dispatch.
     * 
     * @author Aaron Kamienski
     */
    public static class Focus extends BasicListener implements FocusContext
    {
        boolean activated;

        CallerContext context;

        public Focus()
        {
            this(null);
        }

        public Focus(CallerContext ctx)
        {
            this.context = ctx;
        }

        public void reset(boolean active)
        {
            activated = active;
            super.reset();
        }

        public synchronized void dispatchEvent(AWTEvent e, DispatchFilter filter, boolean interestFilter)
        {
            if (filter == null || context == null || filter.accept(context))
            {
                event(e);
                notifyAll();
            }
        }

        public synchronized void notifyActivated()
        {
            activated = true;
            notifyAll();
        }

        public synchronized void notifyDeactivated()
        {
            activated = false;
            notifyAll();
        }

        public synchronized boolean waitForActivated(long ms) throws InterruptedException
        {
            if (!activated) wait(ms);
            return activated;
        }

        public void expectKeyEvents(int keyCode, long timeout) throws InterruptedException
        {
            expectKeyEvents("", keyCode, timeout);
        }

        public synchronized void expectKeyEvents(String msg, int keyCode, long timeout) throws InterruptedException
        {
            syncForEvents(2, timeout);

            assertEquals(msg + "Expected listener to be called", 2, events.size());
            assertEquals(msg + "Expected specified key event to be dispatched", keyCode,
                    ((KeyEvent) events.elementAt(0)).getKeyCode());
            assertEquals(msg + "Expected specified key event to be dispatched", keyCode,
                    ((KeyEvent) events.elementAt(1)).getKeyCode());
            reset();
        }

        public void expectKeyEvent(UserEvent ue, long timeout) throws Exception
        {
            expectKeyEvent("", ue, timeout);
        }

        public synchronized void expectKeyEvent(String msg, UserEvent ue, long timeout) throws Exception
        {
            syncForEvents(1, timeout);

            assertEquals(msg + "Expected listener to be called", 1, events.size());

            KeyEvent ke = (KeyEvent) events.elementAt(0);
            assertEquals(msg + "Expected specified key type to be dispatched", ue.getType(), ke.getID());
            assertEquals(msg + "Expected specified key event to be dispatched", ue.getCode(), ke.getKeyCode());
            assertEquals(msg + "Expected specified key char to be dispatched", ue.getKeyChar(), ke.getKeyChar());
            reset();
        }

        public void expectNoKeyEvents(long timeout) throws InterruptedException
        {
            expectNoEvents("", timeout);
        }

        public synchronized void expectNoKeyEvents(String msg, long timeout) throws InterruptedException
        {
            expectNoEvents(msg, timeout);
        }

        public void clearFocus()
        {
        }

        public int getPriority()
        {
            return PRIORITY_NORMAL;
        }

        public Component getFocusOwner()
        {
            return null;
        }
    }

    /**
     * Replacement ApplicationManager so we can affect the AppID returned for a
     * CallerContext.
     * <p>
     * Assumes that implementation only use getAppAttributes() and then
     * getIdentifier()/getPriority(). TODO: Could be used elsewhere;
     * consolidated
     */
    public static class AppMgr implements ApplicationManager
    {
        public static Manager getInstance()
        {
            return new AppMgr();
        }

        public void destroy()
        { /* empty */
        }

        public org.ocap.application.AppManagerProxy getAppManagerProxy()
        {
            return null;
        }

        public org.ocap.system.RegisteredApiManager getRegisteredApiManager()
        {
            return null;
        }

        public org.dvb.application.AppsDatabase getAppsDatabase()
        {
            return null;
        }

        public ClassLoader getAppClassLoader(CallerContext ctx)
        {
            return null;
        }

        public org.cablelabs.impl.manager.AppDomain createAppDomain(javax.tv.service.selection.ServiceContext sc)
        {
            return null;
        }

        public org.ocap.application.OcapAppAttributes createAppAttributes(org.cablelabs.impl.signalling.AppEntry entry,
                javax.tv.service.Service service)
        {
            return null;
        }

        public boolean purgeLowestPriorityApp(long x, long y, boolean urgent)
        {
            return false;
        }

        public int getRuntimePriority(AppID id)
        {
            return 0;
        }

        public org.dvb.application.AppAttributes getAppAttributes(final CallerContext ctx)
        {
            if (!(ctx instanceof Context))
                return null;
            else
                return new org.dvb.application.AppAttributes()
                {
                    public AppID getIdentifier()
                    {
                        return ((Context) ctx).id;
                    }

                    public int getPriority()
                    {
                        return ((Context) ctx).priority;
                    }

                    private void die()
                    {
                        fail("Unimplemented - not expected to be called");
                    }

                    public int getType()
                    {
                        die();
                        return 0;
                    }

                    public String getName()
                    {
                        die();
                        return null;
                    }

                    public String getName(String iso)
                    {
                        die();
                        return null;
                    }

                    public String[][] getNames()
                    {
                        die();
                        return null;
                    }

                    public String[] getProfiles()
                    {
                        die();
                        return null;
                    }

                    public int[] getVersions(String profile)
                    {
                        die();
                        return null;
                    }

                    public boolean getIsServiceBound()
                    {
                        die();
                        return false;
                    }

                    public boolean isStartable()
                    {
                        die();
                        return false;
                    }

                    public org.dvb.application.AppIcon getAppIcon()
                    {
                        die();
                        return null;
                    }

                    public org.davic.net.Locator getServiceLocator()
                    {
                        die();
                        return null;
                    }

                    public Object getProperty(String key)
                    {
                        die();
                        return null;
                    }

                    public boolean isVisible()
                    {
                        die();
                        return false;
                    }
                };
        }

        public AppEntry getRunningVersion(AppID id)
        {
            return null;
        }
    }

    private ApplicationManager appmgr;

    private CallerContextManager ccmgr;

    protected void replaceManagers()
    {
        appmgr = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        ManagerManagerTest.updateManager(ApplicationManager.class, AppMgr.class, false, new AppMgr());
        ccmgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(ccmgr));
    }

    private void restoreManagers()
    {
        if (ccmgr != null) ManagerManagerTest.updateManager(CallerContextManager.class, ccmgr.getClass(), true, ccmgr);
        if (appmgr != null)
            ManagerManagerTest.updateManager(ApplicationManager.class, appmgr.getClass(), true, appmgr);
    }

    /* ===== Boilerplate ===== */
    public EventManagerTest(String name)
    {
        super(name);
    }

    protected EventManager createEventManager()
    {
        return EventManager.getInstance();
    }

    protected EventManager eventmanager;

    protected void setUp() throws Exception
    {
        // System.out.println(getName());
        super.setUp();
        eventmanager = createEventManager();
    }

    protected void tearDown() throws Exception
    {
        eventmanager = null;
        restoreManagers();
        super.tearDown();
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

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(EventManagerTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new EventManagerTest(tests[i]));
            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(EventManagerTest.class);
        return suite;
    }
}
