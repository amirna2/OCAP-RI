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

package org.ocap.system.event;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.security.SecurityManagerImplTest.Loader;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.dvb.event.EventManagerTest.Context;
import org.ocap.system.MonitorAppPermission;

/**
 * Tests SystemEventManager.
 * 
 * @author Aaron Kamienski
 */
public class SystemEventManagerTest extends TestCase
{
    /**
     * Tests public fields.
     */
    public void testPublicFields()
    {
        TestUtils.testNoPublicFields(SystemEventManager.class);
    }

    /**
     * Tests no public constructors.
     */
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(SystemEventManager.class);
    }

    /**
     * Tests getInstance().
     */
    public void testGetInstance()
    {
        assertNotNull("getInstance() should not return null", SystemEventManager.getInstance());
        // The following isn't necessarily required...
        assertSame("Multiple getInstance() should return same", sem, SystemEventManager.getInstance());
    }

    /**
     * Tests setListener().
     */
    public void testSetListener()
    {
        try
        {
            for (int i = 0; i < TYPES.length; ++i)
                sem.setEventListener(TYPES[i], new Listener());
        }
        finally
        {
            for (int i = 0; i < TYPES.length; ++i)
                sem.unsetEventListener(TYPES[i]);
        }
    }

    /**
     * Tests log().
     */
    public void testLog() throws Exception
    {
        // NOTE: in same order as TYPES
        SystemEvent[] events = { new DeferredDownloadEvent(SystemEvent.BEGIN_SYS_DNLD_EVENT_TYPES + 1),
                new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "Howdy"),
                new RebootEvent(RebootEvent.REBOOT_BY_IMPLEMENTATION, "Greetings"),
                new ResourceDepletionEvent(ResourceDepletionEvent.RESOURCE_RC_BANDWIDTH_DEPLETED, "Hi"), };
        Listener[] listeners = { new Listener(), new Listener(), new Listener(), new Listener(), };

        // Set listeners
        for (int i = 0; i < TYPES.length; ++i)
            sem.setEventListener(TYPES[i], listeners[i]);
        try
        {
            // Send events
            for (int i = 0; i < events.length; ++i)
                sem.log(events[i]);
            // Receive events
            for (int i = 0; i < events.length; ++i)
            {
                assertEquals("Expected one event per listener[" + i + "]", 1, listeners[i].waitEvents(10000));
                assertSame("Expected same event for listener[" + i + "]", events[i], listeners[i].events.elementAt(0));
            }
            // Make sure that those were all of the events
            for (int i = 0; i < events.length; ++i)
            {
                assertEquals("Expected one event per listener[" + i + "]", 1, listeners[i].waitEvents(2000));
                listeners[i].events.clear();
            }
        }
        finally
        {
            for (int i = 0; i < TYPES.length; ++i)
                sem.unsetEventListener(TYPES[i]);
        }
    }

    /**
     * Tests unsetListener().
     */
    public void testUnsetListener() throws Exception
    {
        Listener listener = new Listener();
        sem.setEventListener(SystemEventManager.ERROR_EVENT_LISTENER, listener);
        try
        {
            sem.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "Howdy"));
            assertEquals("Expected one event to be dispatched", 1, listener.waitEvents(10000));

            sem.unsetEventListener(SystemEventManager.ERROR_EVENT_LISTENER);
            listener.events.clear();
            sem.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "Hello"));
            assertEquals("Expected no events to be dispatched", 0, listener.waitEvents(2500L));
        }
        finally
        {
            sem.unsetEventListener(SystemEventManager.ERROR_EVENT_LISTENER);
        }
    }

    /**
     * Make sure that setting a null listener doesn't break things.
     */
    public void testSetListener_clear()
    {
        sem.setEventListener(SystemEventManager.ERROR_EVENT_LISTENER, null);
        sem.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "..."));

        // TODO: Cannot test this because of obfuscation
        /*
         * if ( sem instanceof ExtendedSystemEventManager ) {
         * ((ExtendedSystemEventManager)sem).log(new
         * ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "?"), 1000L); }
         */

        // Still... exceptions might not get propogated out...
    }

    /**
     * Tests that unsetEventListener() doesn't leak the listener.
     */
    public void testSetListener_UnsetLeak() throws Exception
    {
        for (int i = 0; i < TYPES.length; ++i)
        {
            SystemEventListener l = new Listener();
            Reference r = new WeakReference(l);

            sem.setEventListener(TYPES[i], l);
            try
            {
                l = null;
                System.gc();
                System.gc();
                assertNotNull("Listener should still be remembered", r.get());

                sem.unsetEventListener(TYPES[i]);
                System.gc();
                System.gc();
                assertNull("Listener should be gc'ed after unset", r.get());
            }
            finally
            {
                sem.unsetEventListener(TYPES[i]);
            }
        }
    }

    /**
     * Tests that setEventListener(null) doesn't leak the previous listener.
     */
    public void testSetListener_ClearLeak() throws Exception
    {
        for (int i = 0; i < TYPES.length; ++i)
        {
            SystemEventListener l = new Listener();
            Reference r = new WeakReference(l);

            sem.setEventListener(TYPES[i], l);
            try
            {
                l = null;
                System.gc();
                System.gc();
                assertNotNull("Listener should still be remembered", r.get());

                sem.setEventListener(TYPES[i], null);
                System.gc();
                System.gc();
                assertNull("Listener should be gc'ed after clear", r.get());
            }
            finally
            {
                sem.unsetEventListener(TYPES[i]);
            }
        }
    }

    /**
     * Tests that setEventListener(non-null) doesn't leak the previous listener.
     */
    public void testSetListener_ReplaceLeak() throws Exception
    {
        for (int i = 0; i < TYPES.length; ++i)
        {
            SystemEventListener l1 = new Listener();
            SystemEventListener l2 = new Listener();
            Reference r1 = new WeakReference(l1);
            Reference r2 = new WeakReference(l2);

            sem.setEventListener(TYPES[i], l1);
            try
            {
                l1 = null;
                System.gc();
                System.gc();
                assertNotNull("Listener should still be remembered", r1.get());

                sem.setEventListener(TYPES[i], l2);
                l2 = null;
                System.gc();
                System.gc();
                assertNull("Listener should be gc'ed after replace", r1.get());
                assertNotNull("Replacement listener should still be remembered", r2.get());
            }
            finally
            {
                sem.unsetEventListener(TYPES[i]);
            }
        }
    }

    /**
     * Tests that listeners are invoked within proper CallerContext.
     */
    public void testCC() throws Exception
    {
        replaceCCMgr();
        try
        {
            final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            Context cc = new Context(new AppID(1, 2));
            try
            {
                class CCListener extends Listener
                {
                    Vector contexts = new Vector();

                    public synchronized void notifyEvent(SystemEvent event)
                    {
                        super.notifyEvent(event);

                        contexts.addElement(ccm.getCurrentContext());
                    }
                }

                // Add listeners
                CCListener l1 = new CCListener();
                final CCListener l2 = new CCListener();
                sem.setEventListener(SystemEventManager.ERROR_EVENT_LISTENER, l1);
                cc.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        sem.setEventListener(SystemEventManager.RESOURCE_DEPLETION_EVENT_LISTENER, l2);
                    }
                });

                // Log events
                SystemEvent e1 = new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "!");
                SystemEvent e2 = new ResourceDepletionEvent(ResourceDepletionEvent.RESOURCE_CPU_BANDWIDTH_DEPLETED, "!");

                sem.log(e1);
                sem.log(e2);

                assertEquals("Expected 1 event to be delivered", 1, l1.waitEvents(10000L));
                assertEquals("Expected 1 event to be delivered", 1, l2.waitEvents(10000L));
                assertSame("Unexpected event delivered", e1, l1.events.elementAt(0));
                assertSame("Unexpected event delivered", e2, l2.events.elementAt(0));
                assertSame("Unexpected CC used for dispatch", ccm.getSystemContext(), l1.contexts.elementAt(0));
                assertSame("Unexpected CC used for dispatch", cc, l2.contexts.elementAt(0));
            }
            finally
            {
                cc.cleanup();
            }
        }
        finally
        {
            sem.unsetEventListener(SystemEventManager.ERROR_EVENT_LISTENER);
            sem.unsetEventListener(SystemEventManager.RESOURCE_DEPLETION_EVENT_LISTENER);
            restoreCCMgr();
        }
    }

    /**
     * Tests that listeners are forgotten following CallerContext destruction.
     */
    public void testCCDestroy() throws Exception
    {
        replaceCCMgr();
        try
        {
            Context cc = new Context(new AppID(1, 1));
            try
            {
                // Add listeners
                final Listener[] listeners = { new Listener(), new Listener(), new Listener(), new Listener(), };
                cc.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        for (int i = 0; i < TYPES.length; ++i)
                        {
                            sem.setEventListener(TYPES[i], listeners[i]);
                        }
                    }
                });

                // Destroy CC
                class CCData implements CallbackData
                {
                    boolean destroyed;

                    public synchronized void destroy(CallerContext c)
                    {
                        destroyed = true;
                        notifyAll();
                    }

                    public void active(CallerContext d)
                    { /* empty */
                    }

                    public void pause(CallerContext c)
                    { /* empty */
                    }

                    public synchronized void waitDestroyed(long to) throws InterruptedException
                    {
                        if (!destroyed) wait(to);
                    }
                }
                CCData data = new CCData();
                cc.addCallbackData(data, CCData.class);
                cc.cleanup();
                data.waitDestroyed(10000L);
                assertTrue("Expected cc to be destroyed!", data.destroyed);
                Thread.sleep(500L); // kludge: wait for destroy to go through...

                // Log events
                // NOTE: in same order as TYPES
                SystemEvent[] events = { new DeferredDownloadEvent(SystemEvent.BEGIN_SYS_DNLD_EVENT_TYPES + 1),
                        new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "Howdy"),
                        new RebootEvent(RebootEvent.REBOOT_BY_IMPLEMENTATION, "Greetings"),
                        new ResourceDepletionEvent(ResourceDepletionEvent.RESOURCE_RC_BANDWIDTH_DEPLETED, "Hi"), };

                // Send events
                for (int i = 0; i < events.length; ++i)
                    sem.log(events[i]);
                // Make sure that events aren't sent
                for (int i = 0; i < events.length; ++i)
                    assertEquals("Events should not be sent[" + i + "]", 0, listeners[i].waitEvents(500L));
            }
            finally
            {
                // Destroy app
                cc.cleanup();

                for (int i = 0; i < TYPES.length; ++i)
                    sem.unsetEventListener(TYPES[i]);
            }
        }
        finally
        {
            restoreCCMgr();
        }
    }

    /**
     * Tests setListener() security.
     */
    public void testSetListener_security()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            SystemEventManager mgr = SystemEventManager.getInstance();
            mgr.setEventListener(SystemEventManager.ERROR_EVENT_LISTENER, new Listener());
            assertNotNull("setEventListener(int, SystemEventListener) should check with SecurityManager", sm.p);
            assertTrue("setEventListener should check for MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("MonitorAppPermission name should be 'systemevent'", "systemevent",
                    ((MonitorAppPermission) sm.p).getName());
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests unsetListener() security.
     */
    public void testUnsetListener_security()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            SystemEventManager mgr = SystemEventManager.getInstance();
            mgr.unsetEventListener(SystemEventManager.ERROR_EVENT_LISTENER);
            assertNotNull("unsetEventListener(int) should check with SecurityManager", sm.p);
            assertTrue("unsetEventListener should check for MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("MonitorAppPermission name should be 'systemevent'", "systemevent",
                    ((MonitorAppPermission) sm.p).getName());
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests setListener() illegal arguments.
     */
    public void testSetListener_illegal()
    {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < TYPES.length; ++i)
        {
            if (TYPES[i] < min) min = TYPES[i];
            if (TYPES[i] > max) max = TYPES[i];
        }

        int[] illegalTypes = { Integer.MIN_VALUE, Integer.MAX_VALUE, min - 1, max + 1, };
        for (int i = 0; i < illegalTypes.length; ++i)
        {
            try
            {
                sem.setEventListener(illegalTypes[i], new Listener());
                fail("Expected an illegalArgumentException for type=" + illegalTypes[i]);
            }
            catch (IllegalArgumentException e)
            {
                // expected
            }
        }
    }

    /**
     * Tests log() illegal arguments.
     */
    public void testLog_illegal() throws Exception
    {
        Loader loader = new Loader();
        String[] illegalClasses = { DDE.class.getName(), EE.class.getName(), RE.class.getName(), RDE.class.getName(), };
        for (int i = 0; i < illegalClasses.length; ++i)
        {
            loader.addClass(illegalClasses[i]);
            Class clazz = loader.loadClass(illegalClasses[i]);
            SystemEvent e = (SystemEvent) clazz.newInstance();

            try
            {
                sem.log(e);
                fail("Expected an illegalArgumentException for " + illegalClasses[i]);
            }
            catch (IllegalArgumentException ex)
            {
                // expected
            }
        }
    }

    public static class DDE extends DeferredDownloadEvent
    {
        public DDE()
        {
            super(SystemEvent.BEGIN_SYS_DNLD_EVENT_TYPES + 1);
        }
    }

    public static class EE extends ErrorEvent
    {
        public EE()
        {
            super(ErrorEvent.APP_INFO_GENERAL_EVENT, "Howdy");
        }
    }

    public static class RE extends RebootEvent
    {
        public RE()
        {
            super(RebootEvent.REBOOT_BY_IMPLEMENTATION, "Greetings");
        }
    }

    public static class RDE extends ResourceDepletionEvent
    {
        public RDE()
        {
            super(ResourceDepletionEvent.RESOURCE_RC_BANDWIDTH_DEPLETED, "Hi");
        }
    }

    /**
     * Tests synchronous callback of listener.
     */
    public void XtestLogCallsListener()
    {
        fail("Unimplemented test");
    }

    private CallerContextManager save;

    private void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(save));
    }

    private void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    private class Listener implements SystemEventListener
    {
        public Vector events = new Vector();

        public synchronized void notifyEvent(SystemEvent event)
        {
            events.addElement(event);
            notifyAll();
        }

        public synchronized int waitEvents(long timeout) throws InterruptedException
        {
            if (events.size() <= 0) wait(timeout);
            return events.size();
        }
    }

    static final int[] TYPES = { SystemEventManager.DEFERRED_DOWNLOAD_EVENT_LISTENER,
            SystemEventManager.ERROR_EVENT_LISTENER, SystemEventManager.REBOOT_EVENT_LISTENER,
            SystemEventManager.RESOURCE_DEPLETION_EVENT_LISTENER, };

    /*  ***** Boilerplate ***** */
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
        TestSuite suite = new TestSuite(SystemEventManagerTest.class);
        return suite;
    }

    public SystemEventManagerTest(String name)
    {
        super(name);
    }

    protected SystemEventManager sem;

    protected void setUp() throws Exception
    {
        sem = SystemEventManager.getInstance();
    }
}
