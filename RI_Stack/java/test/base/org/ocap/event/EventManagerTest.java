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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

import java.awt.event.KeyEvent;
import java.security.Permission;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.event.UserEventRepositoryTest;
import org.havi.ui.event.HRcEvent;
import org.ocap.event.UserEventTest.DummySecurityManager;
import org.ocap.system.MonitorAppPermission;
import org.ocap.ui.event.OCRcEvent;

/**
 * Tests EventManager implementation
 * 
 * @todo Implement "interface" test that tests dispatch of events to listener
 *       callbacks. Will be used by TestCase for implementation class (e.g.,
 *       EventMgr) which will provide interface for causing events to be
 *       dispatched and potentially for faking/acquiring the CallerContext.
 * @todo Implement "interface" test to test dispatch of userEventFilter.
 */
//findbugs complains about this pattern - shadowing superclass' name.
//Unfortunately, its a common pattern in the RI (so we ignore it).
public class EventManagerTest extends org.dvb.event.EventManagerTest
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
        super.testGetInstance();

        assertTrue("EventManager should be OCAP EventManager", EventManager.getInstance() instanceof EventManager);
    }

    private void checkPermission(String method, Permission perm)
    {
        assertNotNull("SecurityManager should've been consulted by " + method, perm);
        assertTrue("MonitorAppPermission should've been requested by " + method, perm instanceof MonitorAppPermission);
        assertEquals("MonitorAppPermission(\"filterUserEvents\") expected from " + method, "filterUserEvents",
                perm.getName());
    }

    /**
     * Tests get|setFilteredRepository().
     */
    public void testGetSetFilteredRepository()
    {
        EventManager em = (EventManager) eventmanager;

        try
        {
            UserEventRepository uer;
            org.dvb.event.UserEvent[] events;

            // With setFilteredRepository not being called, should return
            // default empty one
            uer = em.getFilteredRepository();
            assertNotNull("Non-null filteredRepository should be exist by default", uer);
            events = uer.getUserEvent();
            assertNotNull("Default filteredRepository's getUserEvents() should not return null", events);
            assertEquals("Default filteredRepository should be empty", 0, events.length);

            // Set filter should be retrieved filter
            UserEventRepository uer0 = new UserEventRepository("filter0");
            uer0.addKey(HRcEvent.VK_CHANNEL_UP);
            em.setFilteredRepository(uer0);
            uer = em.getFilteredRepository();
            assertNotNull("Set filteredRepository should be returned", uer);
            UserEventRepositoryTest.assertEquals("Get filteredRepository should be same as set", uer0, uer);

            // Set filter to null should be retrieved as null
            em.setFilteredRepository(null);
            assertNull("Set filteredRepository=null should returned as null", em.getFilteredRepository());
        }
        finally
        {
            em.setFilteredRepository(null);
        }
    }

    /**
     * Tests for IllegalArgumentException given overlap with mandatory ordinary
     * key codes.
     */
    public void testSetFilteredRepository_mandatoryOverlap()
    {
        EventManager em = (EventManager) eventmanager;

        try
        {
            // Verify Mandatory Ordinary Keycodes check
            for (int i = 0; i < mandatoryOverlap.length; ++i)
            {
                try
                {
                    em.setFilteredRepository(mandatoryOverlap[i]);
                    fail("Repository " + mandatoryOverlap[i] + " intersects with Ordinary Mandatory Keycodes");
                }
                catch (IllegalArgumentException e)
                { /* expected */
                }
            }
        }
        finally
        {
            em.setFilteredRepository(null);
        }
    }

    /**
     * Tests that a repository cannot be modified after it is set.
     */
    public void testSetFilteredRepository_modified()
    {
        EventManager em = (EventManager) eventmanager;

        try
        {
            UserEventRepository original = new UserEventRepository("original");
            original.addKey(MINIMUM_MINUS_MANDATORY[0]);

            UserEventRepository copy = new UserEventRepository("original");
            org.dvb.event.UserEvent[] events = original.getUserEvent();
            for (int i = 0; i < events.length; ++i)
                copy.addUserEvent(events[i]);
            UserEventRepositoryTest.assertEquals("Internal Error - copy should be same as original", original, copy);

            em.setFilteredRepository(original);

            UserEventRepositoryTest.assertEquals("Get filteredRepository should be same as set", original,
                    em.getFilteredRepository());

            // Modify original
            original.addAllArrowKeys();
            original.addAllColourKeys();
            original.addAllNumericKeys();

            UserEventRepositoryTest.assertEquals("Modification should not affect set filter", copy,
                    em.getFilteredRepository());

            UserEventRepository got = em.getFilteredRepository();
            got.addAllArrowKeys();
            got.addAllColourKeys();
            got.addAllNumericKeys();
            UserEventRepositoryTest.assertEquals("Modification should not affect set filter (get returns copy)", copy,
                    em.getFilteredRepository());
        }
        finally
        {
            em.setFilteredRepository(null);
        }
    }

    /**
     * Tests security checks on setFilteredRepository().
     */
    public void testSetFilteredRepository_security()
    {
        EventManager em = (EventManager) eventmanager;

        // Verify Security check
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);

        try
        {
            sm.clear();
            em.setFilteredRepository(new UserEventRepository("security"));
            checkPermission("setFilteredRepository()", sm.perm);

            sm.clear();
            em.setFilteredRepository(null);
            checkPermission("setFilteredRepository(null)", sm.perm);
        }
        finally
        {
            ProxySecurityManager.pop();
            em.setFilteredRepository(null);
        }
    }

    /**
     * Tests setUserEventFilter().
     */
    public void testSetUserEventFilter()
    {
        EventManager em = (EventManager) eventmanager;

        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);

        try
        {
            // Set filter to null should be allowed
            sm.clear();
            em.setUserEventFilter(null);
            checkPermission("setUserEventFilter(null)", sm.perm);

            try
            {
                sm.clear();
                em.setUserEventFilter(new UserEventFilter()
                {
                    public UserEventAction filterUserEvent(UserEvent e)
                    {
                        return null;
                    }
                });
                checkPermission("setUserEventFilter()", sm.perm);
                sm.clear();
                em.setUserEventFilter(new UserEventFilter()
                {
                    public UserEventAction filterUserEvent(UserEvent e)
                    {
                        return null;
                    }
                });
                checkPermission("setUserEventFilter()", sm.perm);
            }
            finally
            {
                em.setUserEventFilter(null);
            }
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests that UserEventFilter is invoked correctly.
     * <ul>
     * <li>In correct context.
     * <li>For correct events.
     * </ul>
     */
    public void testUserEventFilterInvoke_context() throws Exception
    {
        Context app = new Context(new AppID(102, 10));

        replaceManagers();
        try
        {
            Filter uef = new Filter();
            app.setUserEventFilter(uef);

            // Dispatch shouldn't do anything (default empty repository)
            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            uef.expectNoEvents("No Repository: ", 500L);
            // Mandatory ordinary keycodes are never filtered
            dispatch(KeyEvent.VK_0);
            uef.expectNoEvents("Mandatory Ordinary", 500L);

            UserEventRepository uer = new UserEventRepository("empty");
            app.setFilteredRepository(uer);

            // Dispatch shouldn't do anything (explicit empty repository)
            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            uef.expectNoEvents("Empty Repository: ", 500L);
            // Mandatory ordinary keycodes are never filtered
            dispatch(KeyEvent.VK_0);
            uef.expectNoEvents("Mandatory Ordinary", 500L);

            uer = new UserEventRepository("filtered");
            uer.addKey(HRcEvent.VK_CHANNEL_UP);
            app.setFilteredRepository(uer);

            // Dispatch should work (repository includes CH+)
            dispatch(HRcEvent.VK_CHANNEL_UP);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_CHANNEL_UP, 2000L, app);
            // Mandatory ordinary keycodes are never filtered
            dispatch(KeyEvent.VK_0);
            uef.expectNoEvents("Mandatory Ordinary", 500L);

            // Dispatch shouldn't do anything for other keys (repository doesn't
            // include CH-)
            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            uef.expectNoEvents("Not in repository", 500L);

            // Set repository to null
            app.setFilteredRepository(null);

            // Dispatch should work (null implies everything)
            dispatch(HRcEvent.VK_CHANNEL_UP);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_CHANNEL_UP, 2000L, app);
            // Mandatory ordinary keycodes are never filtered
            dispatch(KeyEvent.VK_0);
            uef.expectNoEvents("Mandatory Ordinary", 500L);

            // Remove UserEventFilter
            app.setUserEventFilter(null);

            // Dispatch shouldn't do anything (filter has been removed)
            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            uef.expectNoEvents("Empty Repository: ", 500L);
            // Mandatory ordinary keycodes are never filtered
            dispatch(KeyEvent.VK_0);
            uef.expectNoEvents("Mandatory Ordinary", 500L);
        }
        finally
        {
            app.cleanup();
        }
    }

    /**
     * Tests that UserEventFilter is removed following destruction of installer.
     */
    public void testUserEventFilterInvoke_destroyed() throws Exception
    {
        Context app = new Context(new AppID(102, 20));

        replaceManagers();
        try
        {
            Filter uef = new Filter();
            app.setUserEventFilter(uef);

            UserEventRepository uer = new UserEventRepository("filtered");
            uer.addKey(HRcEvent.VK_VOLUME_DOWN);
            app.setFilteredRepository(uer);

            // Dispatch should work
            dispatch(HRcEvent.VK_VOLUME_DOWN);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_VOLUME_DOWN, 2000L, app);

            // Destroy app
            app.cleanup();

            // Dispatch shouldn't invoke filter anymore
            dispatch(HRcEvent.VK_VOLUME_DOWN);
            uef.expectNoEvents("Destroyed ", 500L);
        }
        finally
        {
            app.cleanup();
        }
    }

    /**
     * Tests that modifying the filtered repository doesn't affect what is
     * filtered.
     */
    public void testUserEventFilterInvoke_modifiedRepository() throws Exception
    {
        Context app = new Context(new AppID(102, 30));

        replaceManagers();
        try
        {
            Filter uef = new Filter();
            app.setUserEventFilter(uef);

            UserEventRepository uer = new UserEventRepository("filtered");
            uer.addKey(HRcEvent.VK_VOLUME_UP);
            app.setFilteredRepository(uer);

            // Dispatch should work
            dispatch(HRcEvent.VK_VOLUME_UP);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_VOLUME_UP, 2000L, app);

            // Update UER
            uer.addKey(HRcEvent.VK_MUTE);

            // Dispatch should not work for new key
            dispatch(HRcEvent.VK_MUTE);
            uef.expectNoEvents("Modified ", 500L);

            // But should still work for old keys
            dispatch(HRcEvent.VK_VOLUME_UP);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_VOLUME_UP, 2000L, app);

            // Re-set repository
            app.setFilteredRepository(uer);

            // Dispatch should work
            dispatch(HRcEvent.VK_VOLUME_UP);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_VOLUME_UP, 2000L, app);
            dispatch(HRcEvent.VK_MUTE);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_MUTE, 2000L, app);

            // Remove key shouldn't affect filtering
            uer.removeKey(HRcEvent.VK_VOLUME_UP);
            dispatch(HRcEvent.VK_VOLUME_UP);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_VOLUME_UP, 2000L, app);
            dispatch(HRcEvent.VK_MUTE);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_MUTE, 2000L, app);

            // Re-set repository
            app.setFilteredRepository(uer);
            dispatch(HRcEvent.VK_MUTE);
            uef.expectUserEvents("Filtered: ", HRcEvent.VK_MUTE, 2000L, app);
            dispatch(HRcEvent.VK_VOLUME_UP);
            uef.expectNoEvents("Removed: ", 500L);
        }
        finally
        {
            app.cleanup();
        }
    }

    /**
     * OCAP K.2.1 says that only "qualified" events are filtered; and that those
     * are events "defined in Table 25-5 [the minimum set] that are not defined
     * as Mandatory Ordinary Keycodes."
     */
    public void testUserEventFilterInvoke_minimum() throws Exception
    {
        Context app = new Context(new AppID(102, 40));

        replaceManagers();
        try
        {
            Filter uef = new Filter();
            app.setUserEventFilter(uef);

            UserEventRepository uer = new UserEventRepository("available");
            for (int i = 0; i < MINIMUM_MINUS_MANDATORY.length; ++i)
                uer.addKey(MINIMUM_MINUS_MANDATORY[i]);
            app.setFilteredRepository(uer);

            // Ensure that all of these events gets to the UserEventFilter
            for (int i = 0; i < MINIMUM_MINUS_MANDATORY.length; ++i)
            {
                dispatch(MINIMUM_MINUS_MANDATORY[i]);
                uef.expectUserEvents("Available for filtering [" + i + "]: ", MINIMUM_MINUS_MANDATORY[i], 3000L, app);
            }
        }
        finally
        {
            app.cleanup();
        }
    }

    /**
     * OCAP K.2.1 says that only "qualified" events are filtered; and that those
     * are events "defined in Table 25-5 [the minimum set] that are not defined
     * as Mandatory Ordinary Keycodes."
     */
    public void testUserEventFilterInvoke_outsideMinimum() throws Exception
    {
        Context app = new Context(new AppID(102, 40));

        replaceManagers();
        try
        {
            Filter uef = new Filter();
            app.setUserEventFilter(uef);

            UserEventRepository uer = new UserEventRepository("outside");

            UserEvent[] addlOutside = { new UserEvent(COMPONENT, org.dvb.event.UserEvent.UEF_KEY_EVENT, 'a', 0L),
                    new UserEvent(COMPONENT, org.dvb.event.UserEvent.UEF_KEY_EVENT, 'A', 0L),
                    new UserEvent(COMPONENT, org.dvb.event.UserEvent.UEF_KEY_EVENT, '*', 0L),
                    new UserEvent(COMPONENT, org.dvb.event.UserEvent.UEF_KEY_EVENT, '/', 0L), };

            for (int i = 0; i < OUTSIDE_MINIMUM.length; ++i)
                uer.addKey(OUTSIDE_MINIMUM[i]);
            for (int i = 0; i < addlOutside.length; ++i)
                uer.addUserEvent(addlOutside[i]);
            app.setFilteredRepository(uer);

            // Ensure that none of these keys gets to the UserEventFilter
            for (int i = 0; i < OUTSIDE_MINIMUM.length; ++i)
            {
                dispatch(OUTSIDE_MINIMUM[i]);
                uef.expectNoEvents("Outside minimum set", 500L);
            }
            for (int i = 0; i < addlOutside.length; ++i)
            {
                dispatch(addlOutside[i]);
                uef.expectNoEvents("Outside minimum set (KEY_TYPED)", 500L);
            }
        }
        finally
        {
            app.cleanup();
        }
    }

    private static interface TestParam
    {
        public Object add(UserEventRepository uer, EventManager em, FocusManager fm) throws Exception;

        public void remove(Object o, EventManager em, FocusManager fm) throws Exception;

        public Object add(UserEventRepository uer, Context cc) throws Exception;

        public void remove(Object o, Context cc) throws Exception;

        public void expectEvents(Object o, int key, long timeout) throws Exception;

        public void expectNoEvents(Object o, long timeout) throws Exception;
    }

    private static class UserEventTestParam implements TestParam
    {
        public Object add(UserEventRepository uer, EventManager em, FocusManager fm)
        {
            EventListener l = new EventListener();
            em.addUserEventListener(l, uer);
            return l;
        }

        public void remove(Object l, EventManager em, FocusManager fm)
        {
            em.removeUserEventListener((UserEventListener) l);
        }

        public Object add(UserEventRepository uer, Context cc)
        {
            EventListener l = new EventListener();
            cc.addUserEventListener(l, uer);
            return l;
        }

        public void remove(Object l, Context cc)
        {
            cc.removeUserEventListener((UserEventListener) l);
        }

        public void expectEvents(Object o, int key, long timeout) throws Exception
        {
            ((EventListener) o).expectUserEvents(key, timeout);
        }

        public void expectNoEvents(Object o, long timeout) throws Exception
        {
            ((EventListener) o).expectNoUserEvents(timeout);
        }
    }

    private static final TestParam USEREVENT_SHARED = new UserEventTestParam();

    private static final TestParam USEREVENT_EXCL = new UserEventTestParam()
    {
        Client client = new Client();

        public Object add(UserEventRepository uer, EventManager em, FocusManager fm)
        {
            EventListener l = new EventListener();
            assertTrue("Expected to be able to reserve UserEvents", em.addUserEventListener(l, client, uer));
            return l;
        }

        public Object add(UserEventRepository uer, Context cc)
        {
            EventListener l = new EventListener();
            assertTrue("Expected to be able to reserve UserEvents", cc.addUserEventListener(l, client, uer));
            return l;
        }
    };

    private static class AWTEventTestParam implements TestParam
    {
        public Object add(UserEventRepository uer, EventManager em, FocusManager fm) throws Exception
        {
            Focus focus = new Focus();
            fm.requestActivate(focus, true);
            focus.waitForActivated(3000L);
            return focus;
        }

        public void remove(Object l, EventManager em, FocusManager fm)
        {
            fm.notifyDeactivated((Focus) l);
        }

        public Object add(UserEventRepository uer, Context cc) throws Exception
        {
            Focus focus = new Focus(cc);
            cc.requestFocus(focus);
            focus.waitForActivated(3000L);
            return focus;
        }

        public void remove(Object o, Context cc)
        {
            // don't do anything... let cleanup() take care of things
        }

        public void expectEvents(Object o, int key, long timeout) throws Exception
        {
            ((Focus) o).expectKeyEvents(key, timeout);
        }

        public void expectNoEvents(Object o, long timeout) throws Exception
        {
            ((Focus) o).expectNoKeyEvents(timeout);
        }
    }

    private static final TestParam AWTEVENT_SHARED = new AWTEventTestParam();

    private static final TestParam AWTEVENT_EXCL = new AWTEventTestParam()
    {
        Client client = new Client();

        public Object add(UserEventRepository uer, EventManager em, FocusManager fm) throws Exception
        {
            assertTrue("Expected to be able to reserve AWTEvents", em.addExclusiveAccessToAWTEvent(client, uer));
            return super.add(uer, em, fm);
        }

        public void remove(Object l, EventManager em, FocusManager fm)
        {
            super.remove(l, em, fm);
            em.removeExclusiveAccessToAWTEvent(client);
        }

        public Object add(UserEventRepository uer, Context cc) throws Exception
        {
            assertTrue("Expected to be able to reserve AWTEvents", cc.addExclusiveAccessToAWTEvent(client, uer));
            return super.add(uer, cc);
        }

        public void remove(Object o, Context cc)
        {
            super.remove(o, cc);
            cc.removeExclusiveAccessToAWTEvent(client);
        }

    };

    /**
     * Tests that unfiltered events are dispatched to listeners as shared
     * UserEvents.
     */
    public void testFilterDispatch_unfiltered_UserEvent() throws Exception
    {
        doTestFilterDispatch_unfiltered(USEREVENT_SHARED);
    }

    /**
     * Tests that filtered, but unmodified events are dispatched to listeners as
     * shared UserEvents.
     */
    public void testFilterDispatch_unchanged_UserEvent() throws Exception
    {
        doTestFilterDispatch_unchanged(USEREVENT_SHARED);
    }

    /**
     * Tests that filtered and modified events are dispatched to listeners as
     * shared UserEvents.
     */
    public void testFilterDispatch_changed_UserEvent() throws Exception
    {
        doTestFilterDispatch_changed(USEREVENT_SHARED);
    }

    /**
     * Tests that "eaten" events aren't dispatched to listeners as shared
     * UserEvents. Method of "eating" is to return null instead of a
     * UserEventAction.
     */
    public void testFilterDispatch_eat1_UserEvent() throws Exception
    {
        doTestFilterDispatch_eat(USEREVENT_SHARED, true);
    }

    /**
     * Tests that "eaten" events aren't dispatched to listeners as shared
     * UserEvents. Method of "eating" is to return null from
     * {@link UserEventAction#getEvent()}.
     */
    public void testFilterDispatch_eat2_UserEvent() throws Exception
    {
        doTestFilterDispatch_eat(USEREVENT_SHARED, false);
    }

    /**
     * Tests that events are routed to targeted apps only.
     */
    public void testFilterDispatch_targeted_UserEvent() throws Exception
    {
        doTestFilterDispatch_targeted(USEREVENT_SHARED);
    }

    /**
     * Tests that unfiltered events are dispatched to listeners as exclusive
     * UserEvents.
     */
    public void testFilterDispatch_unfiltered_UserEventExclusive() throws Exception
    {
        doTestFilterDispatch_unfiltered(USEREVENT_EXCL);
    }

    /**
     * Tests that filtered, but unmodified events are dispatched to listeners as
     * exclusive UserEvents.
     */
    public void testFilterDispatch_unchanged_UserEventExclusive() throws Exception
    {
        doTestFilterDispatch_unchanged(USEREVENT_EXCL);
    }

    /**
     * Tests that filtered and modified events are dispatched to listeners as
     * exclusive UserEvents.
     */
    public void testFilterDispatch_changed_UserEventExclusive() throws Exception
    {
        doTestFilterDispatch_changed(USEREVENT_EXCL);
    }

    /**
     * Tests that "eaten" events aren't dispatched to listeners as exclusive
     * UserEvents. Method of "eating" is to return null instead of a
     * UserEventAction.
     */
    public void testFilterDispatch_eat1_UserEventExclusive() throws Exception
    {
        doTestFilterDispatch_eat(USEREVENT_EXCL, true);
    }

    /**
     * Tests that "eaten" events aren't dispatched to listeners as exclusive
     * UserEvents. Method of "eating" is to return null from
     * {@link UserEventAction#getEvent()}.
     */
    public void testFilterDispatch_eat2_UserEventExclusive() throws Exception
    {
        doTestFilterDispatch_eat(USEREVENT_EXCL, false);
    }

    /**
     * Tests that events are routed to targeted apps only.
     */
    public void testFilterDispatch_targeted_UserEventExclusive() throws Exception
    {
        doTestFilterDispatch_targeted(USEREVENT_EXCL);
    }

    /**
     * Tests that unfiltered events are dispatched to listeners as shared
     * AWTEvents.
     */
    public void testFilterDispatch_unfiltered_AWTEvent() throws Exception
    {
        doTestFilterDispatch_unfiltered(AWTEVENT_SHARED);
    }

    /**
     * Tests that filtered, but unmodified events are dispatched to listeners as
     * shared AWTEvents.
     */
    public void testFilterDispatch_unchanged_AWTEvent() throws Exception
    {
        doTestFilterDispatch_unchanged(AWTEVENT_SHARED);
    }

    /**
     * Tests that filtered and modified events are dispatched to listeners as
     * shared AWTEvents.
     */
    public void testFilterDispatch_changed_AWTEvent() throws Exception
    {
        doTestFilterDispatch_changed(AWTEVENT_SHARED);
    }

    /**
     * Tests that "eaten" events aren't dispatched to listeners as shared
     * AWTEvents. Method of "eating" is to return null instead of a
     * UserEventAction.
     */
    public void testFilterDispatch_eat1_AWTEvent() throws Exception
    {
        doTestFilterDispatch_eat(AWTEVENT_SHARED, true);
    }

    /**
     * Tests that "eaten" events aren't dispatched to listeners as shared
     * AWTEvents. Method of "eating" is to return null from
     * {@link UserEventAction#getEvent()}.
     */
    public void testFilterDispatch_eat2_AWTEvent() throws Exception
    {
        doTestFilterDispatch_eat(AWTEVENT_SHARED, false);
    }

    /**
     * Tests that events are routed to targeted apps only.
     */
    public void testFilterDispatch_targeted_AWTEvent() throws Exception
    {
        doTestFilterDispatch_targeted(AWTEVENT_SHARED);
    }

    /**
     * Tests that unfiltered events are dispatched to listeners as exclusive
     * AWTEvents.
     */
    public void testFilterDispatch_unfiltered_AWTEventExclusive() throws Exception
    {
        doTestFilterDispatch_unfiltered(AWTEVENT_EXCL);
    }

    /**
     * Tests that filtered, but unmodified events are dispatched to listeners as
     * exclusive AWTEvents.
     */
    public void testFilterDispatch_unchanged_AWTEventExclusive() throws Exception
    {
        doTestFilterDispatch_unchanged(AWTEVENT_EXCL);
    }

    /**
     * Tests that filtered and modified events are dispatched to listeners as
     * exclusive AWTEvents.
     */
    public void testFilterDispatch_changed_AWTEventExclusive() throws Exception
    {
        doTestFilterDispatch_changed(AWTEVENT_EXCL);
    }

    /**
     * Tests that "eaten" events aren't dispatched to listeners as exclusive
     * AWTEvents. Method of "eating" is to return null instead of a
     * UserEventAction.
     */
    public void testFilterDispatch_eat1_AWTEventExclusive() throws Exception
    {
        doTestFilterDispatch_eat(AWTEVENT_EXCL, true);
    }

    /**
     * Tests that "eaten" events aren't dispatched to listeners as exclusive
     * AWTEvents. Method of "eating" is to return null from
     * {@link UserEventAction#getEvent()}.
     */
    public void testFilterDispatch_eat2_AWTEventExclusive() throws Exception
    {
        doTestFilterDispatch_eat(AWTEVENT_EXCL, false);
    }

    /**
     * Tests that events are routed to targeted apps only.
     */
    public void testFilterDispatch_targeted_AWTEventExclusive() throws Exception
    {
        doTestFilterDispatch_targeted(AWTEVENT_EXCL);
    }

    /**
     * Tests that unfiltered events are dispatched to their recipient(s).
     */
    private void doTestFilterDispatch_unfiltered(TestParam param) throws Exception
    {
        EventManager em = (EventManager) eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);

        Filter uef = new Filter();
        uef.returnNull = true;
        UserEventRepository filtered = new UserEventRepository("filtered");
        filtered.addKey(HRcEvent.VK_CHANNEL_DOWN);
        filtered.addKey(HRcEvent.VK_CHANNEL_UP);

        UserEventRepository uer = new UserEventRepository("unfiltered");
        uer.addKey(HRcEvent.VK_VOLUME_DOWN);

        Object listener = param.add(uer, em, fm);
        try
        {
            em.setFilteredRepository(filtered);
            em.setUserEventFilter(uef);

            dispatch(HRcEvent.VK_VOLUME_DOWN);
            param.expectEvents(listener, HRcEvent.VK_VOLUME_DOWN, 2000L);
        }
        finally
        {
            em.setFilteredRepository(null);
            em.setUserEventFilter(null);
            param.remove(listener, em, fm);
        }
    }

    /**
     * Tests that filtered, but unmodified events are dispatched to their
     * recipient(s).
     */
    private void doTestFilterDispatch_unchanged(TestParam param) throws Exception
    {
        EventManager em = (EventManager) eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);

        Filter uef = new Filter();
        uef.returnNull = false;
        uef.returnNullEvent = false;
        UserEventRepository filtered = new UserEventRepository("filtered");
        filtered.addKey(HRcEvent.VK_CHANNEL_DOWN);
        filtered.addKey(HRcEvent.VK_CHANNEL_UP);

        UserEventRepository uer = new UserEventRepository("filtered");
        uer.addKey(HRcEvent.VK_CHANNEL_DOWN);

        Object listener = param.add(uer, em, fm);
        try
        {
            em.setFilteredRepository(filtered);
            em.setUserEventFilter(uef);

            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            param.expectEvents(listener, HRcEvent.VK_CHANNEL_DOWN, 2000L);
        }
        finally
        {
            em.setFilteredRepository(null);
            em.setUserEventFilter(null);
            param.remove(listener, em, fm);
        }
    }

    /**
     * Tests that filtered and modified events are dispatched to their
     * recipients.
     */
    private void doTestFilterDispatch_changed(TestParam param) throws Exception
    {
        EventManager em = (EventManager) eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);

        Filter uef = new Filter();
        uef.returnNull = false;
        uef.returnNullEvent = false;
        uef.keyCode = HRcEvent.VK_VOLUME_DOWN; // change all keys to C
        UserEventRepository filtered = new UserEventRepository("filtered");
        filtered.addKey(HRcEvent.VK_CHANNEL_DOWN);
        filtered.addKey(HRcEvent.VK_CHANNEL_UP);

        UserEventRepository uer = new UserEventRepository("filtered");
        uer.addKey(HRcEvent.VK_VOLUME_DOWN);

        Object listener = param.add(uer, em, fm);
        try
        {
            em.setFilteredRepository(filtered);
            em.setUserEventFilter(uef);

            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            param.expectEvents(listener, HRcEvent.VK_VOLUME_DOWN, 2000L);
        }
        finally
        {
            em.setFilteredRepository(null);
            em.setUserEventFilter(null);
            param.remove(listener, em, fm);
        }
    }

    /**
     * Tests that "eaten" events aren't dispatched to their recipients.
     */
    private void doTestFilterDispatch_eat(TestParam param, boolean nullAction) throws Exception
    {
        EventManager em = (EventManager) eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);

        Filter uef = new Filter();
        if (nullAction)
        {
            uef.returnNull = true;
            uef.returnNullEvent = false; // unnecessary
        }
        else
        {
            uef.returnNullEvent = true;
            uef.returnNull = false; // necessary, else will return null
                                    // UserEventAction first
        }
        UserEventRepository filtered = new UserEventRepository("filtered");
        filtered.addKey(HRcEvent.VK_CHANNEL_DOWN);
        filtered.addKey(HRcEvent.VK_CHANNEL_UP);

        UserEventRepository uer = new UserEventRepository("filtered");
        uer.addKey(HRcEvent.VK_CHANNEL_DOWN);

        Object listener = param.add(uer, em, fm);
        try
        {
            em.setFilteredRepository(filtered);
            em.setUserEventFilter(uef);

            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            param.expectNoEvents(listener, 500L);
        }
        finally
        {
            em.setFilteredRepository(null);
            em.setUserEventFilter(null);
            param.remove(listener, em, fm);
        }
    }

    /**
     * Tests that events targeted for a specific App are delivered to that app.
     * Tests that events NOT targeted for a specific App aren't delivered to
     * that app.
     */
    private void doTestFilterDispatch_targeted(TestParam param) throws Exception
    {
        EventManager em = (EventManager) eventmanager;
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);

        Filter uef = new Filter();
        uef.returnNull = false;
        uef.returnNullEvent = false;
        uef.keyCode = HRcEvent.VK_VOLUME_UP;
        UserEventRepository filtered = new UserEventRepository("filtered");
        filtered.addKey(HRcEvent.VK_CHANNEL_DOWN);
        filtered.addKey(HRcEvent.VK_CHANNEL_UP);

        UserEventRepository uer = new UserEventRepository("filtered");
        uer.addKey(HRcEvent.VK_VOLUME_UP);

        AppID[] included = { new AppID(199, 100), new AppID(199, 101), new AppID(199, 99) };
        AppID[] excluded = { new AppID(199, 100), new AppID(199, 102), new AppID(199, 99) };
        AppID[] excluded2 = {};

        Context app = new Context(included[1]);
        replaceManagers();

        Object listener = param.add(uer, app);
        try
        {
            em.setFilteredRepository(filtered);
            em.setUserEventFilter(uef);

            // Targeted for us, we get
            uef.ids = included;
            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            param.expectEvents(listener, HRcEvent.VK_VOLUME_UP, 3000L);

            // Not targeted for us, we don't get
            uef.ids = excluded;
            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            param.expectNoEvents(listener, 500L);

            // Not targeted for us, we don't get
            uef.ids = excluded2;
            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            param.expectNoEvents(listener, 500L);

            // Targeted for us, we still get
            uef.ids = null;
            dispatch(HRcEvent.VK_CHANNEL_DOWN);
            param.expectEvents(listener, HRcEvent.VK_VOLUME_UP, 3000L);
        }
        finally
        {
            em.setFilteredRepository(null);
            em.setUserEventFilter(null);

            app.cleanup();
        }
    }

    class Context extends org.dvb.event.EventManagerTest.Context
    {
        Context(AppID id)
        {
            super(id);
        }

        public void setUserEventFilter(final UserEventFilter uef)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = (EventManager) EventManager.getInstance();
                    em.setUserEventFilter(uef);
                }
            });
        }

        public void setFilteredRepository(final UserEventRepository uer)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    EventManager em = (EventManager) EventManager.getInstance();
                    em.setFilteredRepository(uer);
                }
            });
        }
    }

    static final UserEventRepository[] mandatoryOverlap;
    static
    {
        mandatoryOverlap = new UserEventRepository[] { new OverallRepository(), new UserEventRepository("bad1"),
                new UserEventRepository("bad2"), new UserEventRepository("bad3"), new UserEventRepository("bad4"),
                new UserEventRepository("bad5"), };
        mandatoryOverlap[1].addAllNumericKeys();
        mandatoryOverlap[2].addAllArrowKeys();
        mandatoryOverlap[3].addKey(KeyEvent.VK_PAGE_DOWN);
        mandatoryOverlap[4].addKey(KeyEvent.VK_ENTER);
        mandatoryOverlap[5].addUserEvent(new UserEvent("", org.dvb.event.UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED,
                KeyEvent.VK_ENTER, 0, 0L));
    }

    private static final int[] MANDATORY = { KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
            KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_UP,
            KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER, KeyEvent.VK_PAGE_UP,
            KeyEvent.VK_PAGE_DOWN, };

    private static final int[] MINIMUM_MINUS_MANDATORY = { HRcEvent.VK_CHANNEL_DOWN, HRcEvent.VK_CHANNEL_UP,
            HRcEvent.VK_VOLUME_UP, HRcEvent.VK_VOLUME_DOWN, HRcEvent.VK_MUTE, KeyEvent.VK_PAUSE, HRcEvent.VK_PLAY,
            HRcEvent.VK_STOP, HRcEvent.VK_RECORD, HRcEvent.VK_FAST_FWD, HRcEvent.VK_REWIND, HRcEvent.VK_GUIDE,
            OCRcEvent.VK_MENU, HRcEvent.VK_INFO, OCRcEvent.VK_EXIT, OCRcEvent.VK_LAST, HRcEvent.VK_COLORED_KEY_0,
            HRcEvent.VK_COLORED_KEY_1, HRcEvent.VK_COLORED_KEY_2, HRcEvent.VK_COLORED_KEY_3,
            OCRcEvent.VK_NEXT_FAVORITE_CHANNEL, OCRcEvent.VK_ON_DEMAND, };

    // A subset of the keys that are outside of the minimum OCAP key set
    private static final int[] OUTSIDE_MINIMUM = { KeyEvent.VK_A, KeyEvent.VK_F1, KeyEvent.VK_HOME,
            HRcEvent.VK_COLORED_KEY_4, HRcEvent.VK_COLORED_KEY_5, HRcEvent.VK_BALANCE_LEFT, HRcEvent.VK_BALANCE_RIGHT,
            OCRcEvent.VK_APPS, OCRcEvent.VK_BACK, OCRcEvent.VK_LIST, OCRcEvent.VK_LIVE, };

    static class Filter extends BasicListener implements UserEventFilter
    {
        public boolean returnNull = true;

        public boolean returnNullEvent = false;

        public AppID[] ids;

        public int keyCode = -1;

        public char keyChar = (char) -1;

        /**
         * By default, returns null which indicates the event is consumed.
         */
        public UserEventAction filterUserEvent(UserEvent event)
        {
            event(event);

            UserEventAction uea = null;
            if (!returnNull)
            {
                // If don't have an event, return original event
                UserEvent e = null;

                if (!returnNullEvent)
                {
                    e = event;
                    if (keyCode != -1 && (event.getKeyChar() == KeyEvent.CHAR_UNDEFINED || event.getKeyChar() == 0) // JDK
                                                                                                                    // 1.1.8
                                                                                                                    // CHAR_UNDEFINED
                    )
                    {
                        event.setCode(keyCode);
                    }
                    else if (keyChar != -1 && event.getKeyChar() != KeyEvent.VK_UNDEFINED) event.setKeyChar(keyChar);
                }
                uea = new UserEventAction(e, ids);
            }
            return uea;
        }

        public void expectUserEvents(int code, long timeout) throws InterruptedException
        {
            expectUserEvents("", code, timeout, null);
        }

        public void expectUserEvents(int code, long timeout, CallerContext ctx) throws InterruptedException
        {
            expectUserEvents("", keyCode, timeout, cc);
        }

        public void expectUserEvents(String msg, int code, long timeout) throws InterruptedException
        {
            expectUserEvents(msg, code, timeout, null);
        }

        public synchronized void expectUserEvents(String msg, int code, long timeout, CallerContext ctx)
                throws InterruptedException
        {
            syncForEvents(2, timeout);

            assertEquals(msg + "Expected listener to be called", 2, events.size());
            assertEquals(msg + "Expected specified key event to be dispatched", code,
                    ((UserEvent) events.elementAt(0)).getCode());
            assertEquals(msg + "Expected specified key event to be dispatched", code,
                    ((UserEvent) events.elementAt(1)).getCode());
            if (ctx != null) assertSame(msg + "Expected same CallerContext to be used during invocation", ctx, cc);
            reset();
        }
    }

    /* ===== Boilerplate ===== */
    public EventManagerTest(String name)
    {
        super(name);
    }

    protected UserEventRepository filteredRepository;

    protected void setUp() throws Exception
    {
        super.setUp();

        // Save off the current repository for later restore
        filteredRepository = ((EventManager) eventmanager).getFilteredRepository();
    }

    protected void tearDown() throws Exception
    {
        // Restore the repository
        ((EventManager) eventmanager).setFilteredRepository(filteredRepository);

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
