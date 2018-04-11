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

package org.cablelabs.impl.service.javatv.selection;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.Vector;

import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerListener;
import javax.media.GainControl;
import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.TimeBase;
import javax.media.protocol.DataSource;
import javax.tv.media.MediaSelectFailedEvent;
import javax.tv.media.MediaSelectListener;
import javax.tv.media.MediaSelectSucceededEvent;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceMediaHandler;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceController;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceTuningEvent;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceTuningOverEvent;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.media.player.AlarmClock.Alarm.Callback;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.media.player.SessionChangeCallback;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.session.Session;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.test.SICannedConcreteTest;
import org.davic.mpeg.TransportStream;
import org.davic.net.InvalidLocatorException;
import org.davic.net.Locator;
import org.davic.net.tuning.DeliverySystemType;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.davic.resources.ResourceClient;
import org.dvb.media.DVBMediaSelectControl;
import org.dvb.media.VideoTransformation;
import org.ocap.media.NormalMediaPresentationEvent;
import org.ocap.net.OcapLocator;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests ServiceContextImpl
 * 
 * @author Todd Earles
 */
public class ServiceContextImplTest extends SICannedConcreteTest
{
    // Completion status for operations that we force to happen
    static final int SUCCEEDED = 1;

    static final int FAILED = 2;

    private ServiceManager oldSM;

    private CannedServiceMgr superServiceManager;

    public void setUp() throws Exception
    {
        super.setUp();
        superServiceManager = csm;

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = new LocalCannedServiceManager();
        ManagerManagerTest.updateManager(ServiceManager.class, csm.getClass(), true, csm);

    }

    public void tearDown() throws Exception
    {

        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);
        csm.destroy();
        csm = superServiceManager;
        super.tearDown();
    }

    /**
     * Test service selection that starts in the NOT_PRESENTING state and
     * completes without problem in the PRESENTING state.
     */
    public void testSelectNPtoPSucceed() throws Exception
    {
        ServiceContextStub sc = new ServiceContextStub();
        try
        {
            EventListener el = new EventListener();
            sc.addListener(el);
            Service service1 = csidb.service15;
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(1, 2000);
            assertEquals("Expected listener to be called 1 time", 1, el.events.size());
            assertTrue("Expected NormalContentEvent", el.events.get(0) instanceof NormalContentEvent);
        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Test service selection that starts in the NOT_PRESENTING state and fails
     * at each step in the selection sequence. This leaves the service context
     * in the NOT_PRESENTING state after each failure.
     */
    public void testSelectNPtoNPFail() throws Exception
    {
        ServiceContextStub sc = new ServiceContextStub();
        try
        {
            EventListener el = new EventListener();
            sc.addListener(el);
            Service service1 = csidb.service15;

            // Fail trying to reserve the network interface
            sc.failNextReserve();
            sc.select(service1);
            pauseAfterSelect();
            assertEquals("Expected listener to be called 2 times", 2, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue(
                    "Expected SelectionFailedEvent reason INSUFFICIENT_RESOURCES",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.INSUFFICIENT_RESOURCES);
            assertTrue("Expected PresentationTerminatedEvent", el.events.get(1) instanceof PresentationTerminatedEvent);
            assertTrue(
                    "Expected PresentationTerminatedEvent reason RESOURCES_REMOVED",
                    ((PresentationTerminatedEvent) el.events.get(1)).getReason() == PresentationTerminatedEvent.RESOURCES_REMOVED);
            assertTrue("Context not left in NOT_PRESENTING state", !sc.isPresenting());
            el.reset();

            // Fail trying to tune
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(FAILED);
            el.syncForEvents(2, 2000);
            assertEquals("Expected listener to be called 2 times", 2, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue("Expected SelectionFailedEvent reason TUNING_FAILURE",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.TUNING_FAILURE);
            assertTrue("Expected PresentationTerminatedEvent", el.events.get(1) instanceof PresentationTerminatedEvent);
            assertTrue(
                    "Expected PresentationTerminatedEvent reason TUNED_AWAY",
                    ((PresentationTerminatedEvent) el.events.get(1)).getReason() == PresentationTerminatedEvent.TUNED_AWAY);
            assertTrue("Context not left in NOT_PRESENTING state", !sc.isPresenting());
            el.reset();

            // Fail during Media Selection
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(FAILED);
            el.syncForEvents(2, 2000);
            assertEquals("Expected listener to be called 2 times", 2, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            int reason1 = ((SelectionFailedEvent) el.events.get(0)).getReason();
            assertTrue("Expected SelectionFailedEvent reason INSUFFICIENT_RESOURCES, received " + reason1,
                    reason1 == SelectionFailedEvent.INSUFFICIENT_RESOURCES);
            assertTrue("Expected PresentationTerminatedEvent", el.events.get(1) instanceof PresentationTerminatedEvent);
            int reason2 = ((PresentationTerminatedEvent) el.events.get(1)).getReason();
            assertTrue("Expected PresentationTerminatedEvent reason RESOURCES_REMOVED, received " + reason2,
                    reason2 == PresentationTerminatedEvent.RESOURCES_REMOVED);
            assertTrue("Context not left in NOT_PRESENTING state", !sc.isPresenting());
        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Test service selection that starts in the PRESENTING state and fails at
     * each step in the selection sequence trying to select a new service. At
     * each failure, the service context falls back to presenting the current
     * service since the new one cannot be selected. This leaves the service
     * context in the PRESENTING state after each failure to select the new
     * service.
     */
    public void testSelectPtoPFail() throws Exception
    {
        ServiceContextStub sc = new ServiceContextStub();
        try
        {
            EventListener el = new EventListener();
            sc.addListener(el);
            Service service1 = csidb.service15;
            Service service2 = csidb.jmfService1;

            // Succeed in selecting Service1
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(1, 2000);
            assertEquals("Expected listener to be called 1 time", 1, el.events.size());
            assertTrue("Expected NormalContentEvent", el.events.get(0) instanceof NormalContentEvent);
            el.reset();
            // Fail trying to reserve the network interface for Service2
            // (fallback to Service1)
            sc.failNextReserve();
            sc.select(service2);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(2, 2000);
            assertEquals("Expected listener to be called 2 times", 2, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue(
                    "Expected SelectionFailedEvent reason INSUFFICIENT_RESOURCES",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.INSUFFICIENT_RESOURCES);
            assertTrue("Expected NormalContentEvent", el.events.get(1) instanceof NormalContentEvent);
            el.reset();

            // Fail trying to tune for Service2 (fallback to Service1)
            sc.select(service2);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(FAILED);
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(2, 2000);
            assertEquals("Expected listener to be called 2 times", 2, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue("Expected SelectionFailedEvent reason TUNING_FAILURE",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.TUNING_FAILURE);
            assertTrue("Expected NormalContentEvent", el.events.get(1) instanceof NormalContentEvent);
            el.reset();

            // Fail during Media Selection for Service2 (fallback to Service1)
            sc.select(service2);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(FAILED);
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(2, 2000);
            assertEquals("Expected listener to be called 2 times", 2, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue(
                    "Expected SelectionFailedEvent reason INSUFFICIENT_RESOURCES",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.INSUFFICIENT_RESOURCES);
            assertTrue("Expected NormalContentEvent", el.events.get(1) instanceof NormalContentEvent);
            el.reset();
        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Test service selection that starts in the PRESENTING state and fails at
     * each step in the selection sequence trying to select a new service. At
     * each failure, the service context attempts to fall back to presenting the
     * current service but this fails also. This leaves the service context in
     * the NOT_PRESENTING state after each failure to select the new service.
     */
    public void testSelectPtoNPFail() throws Exception
    {
        ServiceContextStub sc = new ServiceContextStub();
        try
        {
            EventListener el = new EventListener();
            sc.addListener(el);
            Service service1 = csidb.service15;
            Service service2 = csidb.service15;

            // Succeed in selecting Service1
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(1, 2000);
            assertEquals("Expected listener to be called 1 time", 1, el.events.size());
            assertTrue("Expected NormalContentEvent", el.events.get(0) instanceof NormalContentEvent);
            el.reset();

            // Fail trying to reserve the network interface for Service2 then
            // fail trying
            // to tune back to Service1
            sc.failNextReserve();
            sc.select(service2);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(FAILED);
            el.syncForEvents(3, 2000);
            assertEquals("Expected listener to be called 3 times", 3, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue(
                    "Expected SelectionFailedEvent reason INSUFFICIENT_RESOURCES",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.INSUFFICIENT_RESOURCES);
            assertTrue("Expected SelectionFailedEvent", el.events.get(1) instanceof SelectionFailedEvent);
            assertTrue("Expected SelectionFailedEvent reason TUNING_FAILURE",
                    ((SelectionFailedEvent) el.events.get(1)).getReason() == SelectionFailedEvent.TUNING_FAILURE);
            assertTrue("Expected PresentationTerminatedEvent", el.events.get(2) instanceof PresentationTerminatedEvent);
            assertTrue(
                    "Expected PresentationTerminatedEvent reason TUNED_AWAY",
                    ((PresentationTerminatedEvent) el.events.get(2)).getReason() == PresentationTerminatedEvent.TUNED_AWAY);
            el.reset();

            // Succeed in selecting Service1
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(1, 2000);
            assertEquals("Expected listener to be called 1 time", 1, el.events.size());
            assertTrue("Expected NormalContentEvent", el.events.get(0) instanceof NormalContentEvent);
            el.reset();

            // Fail trying to tune for Service2 then fail trying to tune back to
            // Service1
            sc.select(service2);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(FAILED);
            sc.getNI().startTune();
            sc.getNI().completeTune(FAILED);
            el.syncForEvents(3, 2000);
            assertEquals("Expected listener to be called 3 times", 3, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue("Expected SelectionFailedEvent reason TUNING_FAILURE",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.TUNING_FAILURE);
            assertTrue("Expected SelectionFailedEvent", el.events.get(1) instanceof SelectionFailedEvent);
            assertTrue("Expected SelectionFailedEvent reason TUNING_FAILURE",
                    ((SelectionFailedEvent) el.events.get(1)).getReason() == SelectionFailedEvent.TUNING_FAILURE);
            assertTrue("Expected PresentationTerminatedEvent", el.events.get(2) instanceof PresentationTerminatedEvent);
            assertTrue(
                    "Expected PresentationTerminatedEvent reason TUNED_AWAY",
                    ((PresentationTerminatedEvent) el.events.get(2)).getReason() == PresentationTerminatedEvent.TUNED_AWAY);
            el.reset();

            // Succeed in selecting Service1
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(1, 2000);
            assertEquals("Expected listener to be called 1 time", 1, el.events.size());
            assertTrue("Expected NormalContentEvent", el.events.get(0) instanceof NormalContentEvent);
            el.reset();

            // Fail during Media Selection for Service2 then fail trying to tune
            // back
            // to Service1
            sc.select(service2);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(FAILED);
            sc.getNI().startTune();
            sc.getNI().completeTune(FAILED);
            el.syncForEvents(3, 2000);
            assertEquals("Expected listener to be called 3 times", 3, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue(
                    "Expected SelectionFailedEvent reason INSUFFICIENT_RESOURCES",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.INSUFFICIENT_RESOURCES);
            assertTrue("Expected SelectionFailedEvent", el.events.get(1) instanceof SelectionFailedEvent);
            assertTrue("Expected SelectionFailedEvent reason TUNING_FAILURE",
                    ((SelectionFailedEvent) el.events.get(1)).getReason() == SelectionFailedEvent.TUNING_FAILURE);
            assertTrue("Expected PresentationTerminatedEvent", el.events.get(2) instanceof PresentationTerminatedEvent);
            assertTrue(
                    "Expected PresentationTerminatedEvent reason TUNED_AWAY",
                    ((PresentationTerminatedEvent) el.events.get(2)).getReason() == PresentationTerminatedEvent.TUNED_AWAY);
            el.reset();

        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Test service selection that starts in the NOT_PRESENTING state and
     * performs a user stop at each step in the selection sequence. This leaves
     * the service context in the NOT_PRESENTING state after each stop. Note
     * that a user stop does not trigger a SelectionFailedEvent. However, if the
     * service context makes it to the PRESENTING state, then a user stop will
     * trigger a PresentationTerminatedEvent.
     */
    public void testSelectNPtoNPStop() throws Exception
    {
        ServiceContextStub sc = new ServiceContextStub();
        try
        {
            EventListener el = new EventListener();
            sc.addListener(el);
            Service service1 = csidb.service15;

            // Perform a user stop while waiting for the tune to complete
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.stop();
            el.syncForEvents(1, 1000);
            assertEquals("Expected listener to be called 1 time - " + el.events, 1, el.events.size());
            el.reset();

            // Perform a user stop while waiting for media selection to complete
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.stop();
            el.syncForEvents(1, 1000);
            assertEquals("Expected listener to be called 1 time - " + el.events, 1, el.events.size());
            el.reset();

            // Perform a user stop after media selection completes
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            sc.stop();
            el.syncForEvents(2, 2000);
            assertEquals("Expected listener to be called 2 times", 2, el.events.size());
            assertTrue("Expected NormalContentEvent, but got " + el.events.get(0).getClass(),
                    el.events.get(0) instanceof NormalContentEvent);
            assertTrue("Expected PresentationTerminatedEvent but got " + el.events.get(1).getClass(),
                    el.events.get(1) instanceof PresentationTerminatedEvent);
            assertTrue(
                    "Expected PresentationTerminatedEvent reason USER_STOP",
                    ((PresentationTerminatedEvent) el.events.get(1)).getReason() == PresentationTerminatedEvent.USER_STOP);
            el.reset();

        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Test service selection that starts in the NOT_PRESENTING state and
     * performs a new select at each step in the selection sequence. This forces
     * a SelectionFailedEvent.INTERRUPTED to be sent for the previous selection
     * and the service context is left in the PRESENTATION_PENDING state for the
     * new selection.
     */
    public void testSelectNPtoPInterrupt() throws Exception
    {
        ServiceContextStub sc = new ServiceContextStub();
        try
        {
            EventListener el = new EventListener();
            sc.addListener(el);
            Service service1 = csidb.service15;
            Service service2 = csidb.jmfService1;
            Service service3 = csidb.jmfService2;

            // Select Service1 and wait for the tune to complete
            sc.select(service1);
            pauseAfterSelect();
            sc.getNI().startTune();

            // Select Service2 and wait for the media select to complete
            sc.select(service2);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            el.syncForEvents(1, 2000);
            assertEquals("Expected listener to be called 1 time", 1, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            assertTrue("Expected SelectionFailedEvent reason INTERRUPTED",
                    ((SelectionFailedEvent) el.events.get(0)).getReason() == SelectionFailedEvent.INTERRUPTED);
            el.reset();

            // Select Service3 and wait for it to complete normally
            sc.select(service3);
            pauseAfterSelect();
            sc.getNI().startTune();
            sc.getNI().completeTune(SUCCEEDED);
            sc.getPendingMediaHandler().finishSelect(SUCCEEDED);
            el.syncForEvents(2, 2000);
            assertEquals("Expected listener to be called 2 times", 2, el.events.size());
            assertTrue("Expected SelectionFailedEvent", el.events.get(0) instanceof SelectionFailedEvent);
            SelectionFailedEvent evt = (SelectionFailedEvent) el.events.get(0);
            assertTrue("Expected SelectionFailedEvent reason INTERRUPTED but was " + evt.getReason(),
                    evt.getReason() == SelectionFailedEvent.INTERRUPTED);
            assertTrue("Expected NormalContentEvent", el.events.get(1) instanceof NormalContentEvent);
            el.reset();

        }
        finally
        {
            sc.destroy();
        }
    }

    // TODO(Todd): Add a test that generates every possible event during each
    // state
    // to ensure that the state machine discards events it is not interested in.

    /**
     * Selecting causes some asynchronous tasks to be scheduled. Most tests
     * expect that these are run before the testing resumes. Provide a way for
     * waiting until the tasks have been run.
     */
    private void pauseAfterSelect()
    {
        synchronized (this)
        {
            try
            {
                wait(3000);
            }
            catch (InterruptedException exc)
            {

            }
        }
    }

    /**
     * Basic listener for recording events as they fire.
     */
    public static class BasicListener
    {
        public Vector events = new Vector();

        /**
         * Add the event to the vector of those received so far.
         * 
         * @param e
         *            the event
         */
        protected synchronized void event(Object e)
        {
            events.addElement(e);
            notifyAll();
        }

        /** Reset the listener (remove all events) */
        public void reset()
        {
            events.removeAllElements();
        }

        /**
         * Wait for the specified number of events within a synchronized block.
         * 
         * @param n
         *            number of events to wait for
         * @param millis
         *            the maximum time to wait for all events
         */
        public synchronized void syncForEvents(int n, long millis) throws InterruptedException
        {
            waitForEvents(n, millis);
        }

        /**
         * Wait for the specified number of events.
         * 
         * @param n
         *            number of events to wait for
         * @param millis
         *            the maximum time to wait for all events
         */
        public void waitForEvents(int n, long millis) throws InterruptedException
        {
            long end = System.currentTimeMillis() + millis;
            while (millis > 0 && events.size() < n)
            {
                wait(millis);

                millis = end - System.currentTimeMillis();
            }
        }
    }

    /**
     * Event listener
     */
    public static class EventListener extends BasicListener implements ServiceContextListener
    {
        /** Store the event so it can be examined later. */
        public synchronized void receiveServiceContextEvent(ServiceContextEvent e)
        {
            event(e);
        }
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
        TestSuite suite = new TestSuite(ServiceContextImplTest.class);
        return suite;
    }

    public ServiceContextImplTest(String name)
    {
        super(name);
    }

    private class LocalCannedServiceManager extends CannedServiceMgr
    {
        public ServiceMediaHandler createServiceMediaHandler(DataSource ds, ServiceContextExt sc, Object lock, ResourceUsageImpl resourceUsage)
        {
            ServiceDataSource sds = (ServiceDataSource) ds;
            return new ServiceMediaHandlerStub(sds.getLocator());
        }
    }

    /**
     * Service context implementation stub. This class extends the real service
     * context implementation in such a way as to cause it to use stub objects
     * for testing instead of using the real implementation objects.
     */
    private class ServiceContextStub extends ServiceContextImpl
    {
        boolean failNextReserve = false;

        // Constructor
        public ServiceContextStub()
        {
            super(false);
        }

        // Beware that ServiceContextImpl's constructor calls createNIC() which
        // is overridden here. Therefore, createNIC() ends up being called
        // before
        // any variable assignments. Do not assign a value (e.g. null) to nic
        // here or it will clobber the real value assigned by createNIC().
        NetworkInterfaceControllerStub nic;

        // Description copied from ServiceContextImpl
        protected ExtendedNetworkInterfaceController createNIC(ResourceClient rc)
        {
            // Override base implementation to provide our own stub for the
            // NetworkInterfaceController

            synchronized (ServiceContextImplTest.this)
            {
                nic = new NetworkInterfaceControllerStub(rc);
                if (failNextReserve)
                {
                    nic.failNextReserve();
                    failNextReserve = false;
                }

            }
            return nic;
        }

        public void failNextReserve()
        {
            failNextReserve = true;
        }

        /**
         * Get the NetworkInterfaceController being used by this stub so that
         * the caller can control it.
         */
        public NetworkInterfaceControllerStub getNIC()
        {
            return nic;
        }

        /**
         * Get the NetworkInterface being used by this stub so that the caller
         * can control it.
         */
        public NetworkInterfaceStub getNI()
        {

            return (NetworkInterfaceStub) nic.getNetworkInterface();
        }

        /**
         * Get the pending media handler being used by this stub so that the
         * caller can control it.
         */
        public ServiceMediaHandlerStub getPendingMediaHandler()
        {
            throw new IllegalStateException("pending handler no longer on servicecontext");
        }
    }

    /**
     * The NetworkInterfaceController stub.
     */
    private class NetworkInterfaceControllerStub extends ExtendedNetworkInterfaceController
    {
        ResourceClient rc = null;

        NetworkInterfaceStub ni = null;

        boolean failNextReserve = false;

        // Description copied from NetworkInterfaceController
        public NetworkInterfaceControllerStub(ResourceClient rc)
        {
            // Have to call the superclass constructor but we assume it does
            // nothing
            // to interfer with this stub class. This stub will deal with the
            // ResourceClient directly.
            super(rc);

            // Remember the ResourceClient so we can deal with it
            this.rc = rc;
        }

        // Description copied from NetworkInterfaceController
        public ResourceClient getClient()
        {
            return rc;
        }

        // Description copied from NetworkInterfaceController
        public NetworkInterface getNetworkInterface()
        {
            return ni;
        }

        // Description copied from NetworkInterfaceController
        public void reserve(NetworkInterface ni, Object requestData)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from NetworkInterfaceController
        public void reserveFor(ResourceUsageImpl usage, Locator locator, Object requestData)
                throws NetworkInterfaceException
        {
            if (failNextReserve)
            {
                failNextReserve = false;
                throw new NoFreeInterfaceException();
            }
            else
            {
                ni = new NetworkInterfaceStub(this);
                ni.pendingLocator = locator;
                ni.reserve();
            }
        }

        // Description copied from NetworkInterfaceController
        public synchronized void release(ResourceUsageImpl usage)
        {
            release();
        }

        public synchronized void release()
        {
            if (ni != null)
            {
                ni.release();
                ni = null;
            }
        }

        // Description copied from NetworkInterfaceController
        public void tune(Locator locator)
        {
            ni.tune();
        }

        // Description copied from NetworkInterfaceController
        public void tune(TransportStream ts)
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Force the next reservation to fail
         */
        public void failNextReserve()
        {
            failNextReserve = true;
        }
    }

    /**
     * The NetworkInterface stub.
     */
    private class NetworkInterfaceStub extends CannedNetworkInterface
    {
        Vector listeners = new Vector();

        boolean reserved = false;

        boolean waitingToStartTune = false;

        boolean waitingToCompleteTune = false;

        private Locator locator;

        Locator pendingLocator;

        NetworkInterfaceControllerStub nic;

        public NetworkInterfaceStub(NetworkInterfaceControllerStub nic)
        {
            super(1);
            this.nic = nic;
        }

        // Description copied from NetworkInterface
        public TransportStream getCurrentTransportStream()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from NetworkInterface
        public int getDeliverySystemType()
        {
            return DeliverySystemType.CABLE_DELIVERY_SYSTEM;
        }

        // Description copied from NetworkInterface
        public Locator getLocator()
        {
            return locator;
        }

        // Description copied from NetworkInterface
        public boolean isLocal()
        {
            return true;
        }

        // Description copied from NetworkInterface
        public boolean isReserved()
        {
            return reserved;
        }

        // Description copied from NetworkInterface
        public TransportStream[] listAccessibleTransportStreams()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from NetworkInterface
        public synchronized void addNetworkInterfaceListener(NetworkInterfaceListener listener)
        {
            listeners.add(listener);
        }

        // Description copied from NetworkInterface
        public synchronized void removeNetworkInterfaceListener(NetworkInterfaceListener listener)
        {
            listeners.remove(listener);
        }

        /** Reserve the tuner */
        void reserve()
        {
            reserved = true;
        }

        /** Release the tuner */
        protected void release()
        {
            reserved = false;
        }

        /** Mark as tuning but do not start until told to do so */
        void tune()
        {
            // Wait until told to start the tune
            waitingToStartTune = true;
            waitingToCompleteTune = false;

            synchronized (ServiceContextImplTest.this)
            {
                ServiceContextImplTest.this.notify();
            }
        }

        /** Start the tune */
        public synchronized void startTune()
        {

            if (!waitingToStartTune) throw new IllegalStateException();

            // Mark that the tune started
            waitingToStartTune = false;
            waitingToCompleteTune = true;

            // Notify all listeners.
            for (int i = 0; i < listeners.size(); i++)
            {
                NetworkInterfaceListener listener = (NetworkInterfaceListener) listeners.get(i);
                listener.receiveNIEvent(new ExtendedNetworkInterfaceTuningEvent(this, nic));
            }
        }

        /** Complete the tune with the specified status */
        public synchronized void completeTune(int status)
        {
            // Error if we were not waiting to be told to complete
            if (!waitingToCompleteTune) throw new IllegalStateException();

            // Translate generic status code to a value that is valid for the
            // event
            int tuneStatus;
            if (status == SUCCEEDED)
            {
                tuneStatus = NetworkInterfaceTuningOverEvent.SUCCEEDED;
                locator = pendingLocator;
            }
            else
            {
                tuneStatus = NetworkInterfaceTuningOverEvent.FAILED;
            }
            // Mark that the tune completed
            waitingToStartTune = false;
            waitingToCompleteTune = false;

            // Notify all listeners
            for (int i = 0; i < listeners.size(); i++)
            {
                NetworkInterfaceListener listener = (NetworkInterfaceListener) listeners.get(i);
                listener.receiveNIEvent(new ExtendedNetworkInterfaceTuningOverEvent(this, tuneStatus, nic));
            }
        }
    }

    /**
     * This class is the complete stub for the media handler. This includes any
     * controls required by the handler.
     */
    private class ServiceMediaHandlerStub implements ServiceMediaHandler, DVBMediaSelectControl, ServicePlayer
    {
        MediaLocator serviceLocator;

        Vector controllerListeners = new Vector();

        Vector mscListeners = new Vector();

        boolean waitingToFinishSelect = false;

        javax.tv.locator.Locator[] selectLocators = null;

        javax.tv.locator.Locator[] presentingLocators = null;

        // Constructor
        ServiceMediaHandlerStub(MediaLocator l)
        {
            serviceLocator = l;
        }

        // Description copied from Controller
        public void addControllerListener(ControllerListener listener)
        {
            controllerListeners.add(listener);
        }

        // Description copied from Controller
        public void removeControllerListener(ControllerListener listener)
        {
            controllerListeners.remove(listener);
        }

        // Description copied from Controller
        public void close()
        {
        }

        // Description copied from Controller
        public void deallocate()
        {
        }

        // Description copied from Controller
        public Control getControl(String forName)
        {
            // All controls are part of this stub
            if (forName.equals("org.dvb.media.DVBMediaSelectControl")) return this;

            // Throw an exception for unknow controls.
            throw new UnsupportedOperationException();
        }

        // Description copied from Controller
        public Control[] getControls()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Controller
        public Time getStartLatency()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Controller
        public int getState()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Controller
        public int getTargetState()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Controller
        public void prefetch()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Controller
        public void realize()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public long getMediaNanoseconds()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public Time getMediaTime()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public float getRate()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public Time getStopTime()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public Time getSyncTime()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public TimeBase getTimeBase()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public Time mapToTimeBase(Time t)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public void setMediaTime(Time now)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public float setRate(float factor)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public void setStopTime(Time stopTime)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public void setTimeBase(TimeBase master)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Clock
        public void stop()
        {
        }

        // Description copied from Clock
        public void syncStart(Time at)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from ServiceContentHandler
        public javax.tv.locator.Locator[] getServiceContentLocators()
        {
            return presentingLocators;
        }

        // Description copied from Player
        public void addController(Controller newController)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Player
        public Component getControlPanelComponent()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Player
        public GainControl getGainControl()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Player
        public Component getVisualComponent()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Player
        public void removeController(Controller oldController)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from Player
        public void start()
        {
        }

        // Description copied from Duration
        public Time getDuration()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from MediaHandler
        public void setSource(DataSource source)
        {
        }

        // Description copied from DVBMediaSelectControl
        public void selectServiceMediaComponents(javax.tv.locator.Locator l)
        {
            waitingToFinishSelect = true;

            // Create a set of service component locators for the service
            try
            {
                selectLocators = new javax.tv.locator.Locator[] { new OcapLocator(l.toExternalForm() + ".$Video1"),
                        new OcapLocator(l.toExternalForm() + ".$Audio1") };
            }
            catch (InvalidLocatorException e)
            {
                throw new IllegalArgumentException("Cannot create OcapLocator for service component.");
            }
        }

        // Description copied from MediaSelectControl
        public void add(javax.tv.locator.Locator component)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from MediaSelectControl
        public void remove(javax.tv.locator.Locator component)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from MediaSelectControl
        public void addMediaSelectListener(MediaSelectListener listener)
        {
            mscListeners.add(listener);
        }

        // Description copied from MediaSelectControl
        public void removeMediaSelectListener(MediaSelectListener listener)
        {
            mscListeners.remove(listener);
        }

        // Description copied from MediaSelectControl
        public javax.tv.locator.Locator[] getCurrentSelection()
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from MediaSelectControl
        public void replace(javax.tv.locator.Locator fromComponent, javax.tv.locator.Locator toComponent)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from MediaSelectControl
        public void select(javax.tv.locator.Locator component)
        {
            // This version of select() is never called because we either select
            // a service or an array of components.
            throw new UnsupportedOperationException();
        }

        // Description copied from MediaSelectControl
        public void select(javax.tv.locator.Locator[] components)
        {
            waitingToFinishSelect = true;
            selectLocators = components;
        }

        // Description copied from Control
        public java.awt.Component getControlComponent()
        {
            throw new UnsupportedOperationException();
        }

        public void setInitialSelection(ServiceDetails service, javax.tv.locator.Locator[] componentLocators)
        {
            waitingToFinishSelect = true;
            selectLocators = componentLocators;
        }

        public void updateServiceContextSelection(javax.tv.locator.Locator[] componentLocators) throws javax.tv.locator.InvalidLocatorException, InvalidServiceComponentException {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        public void setInitialVideoSize(VideoTransformation trans, Container container, Rectangle bounds)
        {
            throw new UnsupportedOperationException();
        }

        public void swapDecoders(ServicePlayer otherPlayer, boolean useOtherAudio)
        {
            throw new UnsupportedOperationException();
        }

        public VideoDevice getVideoDevice()
        {
            return null;
        }

        public boolean isComponentPlayer()
        {
            return false;
        }

        public void loseVideoDeviceControl()
        {
        }

        public void notifyNotContributing()
        {
        }

        public void setVideoDevice(VideoDevice vd)
        {
        }

        public CallerContext getOwnerCallerContext()
        {
            return null;
        }

        public int getOwnerPriority()
        {
            return 0;
        }

        public DataSource getSource()
        {
            return null;
        }

        public Alarm createAlarm(AlarmSpec spec, Callback callback)
        {
            return null;
        }

        public void destroyAlarm(Alarm alarm)
        {
        }

        /** Finish the select with the specified status */
        public synchronized void finishSelect(int status)
        {
            // Error if we were not waiting to be told that the select finished
            if (!waitingToFinishSelect) throw new IllegalStateException();

            // Mark that the select is finished
            waitingToFinishSelect = false;
            presentingLocators = selectLocators;
            selectLocators = null;

            // Notify all listeners
            for (int i = 0; i < mscListeners.size(); i++)
            {
                MediaSelectListener listener = (MediaSelectListener) mscListeners.get(i);
                if (status == SUCCEEDED)
                    listener.selectionComplete(new MediaSelectSucceededEvent(this, selectLocators));
                else
                    listener.selectionComplete(new MediaSelectFailedEvent(this, selectLocators));
            }

            for (int i = 0; i < controllerListeners.size(); i++)
            {
                ControllerListener listener = (ControllerListener) controllerListeners.get(i);
                if (status == SUCCEEDED)
                    listener.controllerUpdate(new NormalMediaPresentationEvent(this, 0, 0, 0)
                    {
                    });
                else
                    listener.controllerUpdate(new ControllerClosedEvent(this));
            }
        }

        public int addSessionChangeCallback(SessionChangeCallback cb)
        {
            return Session.INVALID;
        }

        public void removeSessionChangeCallback(SessionChangeCallback cb)
        {
            // TODO Auto-generated method stub
        }

        public ServiceContext getServiceContext()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Presentation getPresentation()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isServiceContextPlayer()
        {
            return false;
        }

        public void switchToAlternativeContent(Class alternativeContentReasonClass, int alternativeContentReasonCode)
        {

        }

        public void setNetworkInterface(ExtendedNetworkInterface networkInterface)
        {
            
        }
        public void setMediaTime(Time mt, boolean sendEvent) 
        {
            
        }
    }
}
