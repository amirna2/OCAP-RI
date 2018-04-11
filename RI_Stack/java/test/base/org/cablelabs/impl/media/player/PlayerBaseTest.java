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
package org.cablelabs.impl.media.player;

import java.awt.Component;

import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.Duration;
import javax.media.NotRealizedError;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.Time;
import javax.media.TransitionEvent;
import javax.media.protocol.DataSource;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.ServiceMgrImpl;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.source.CannedOcapServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.davic.media.ResourceReturnedEvent;
import org.davic.media.ResourceWithdrawnEvent;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PlayerBaseTest extends TestCase
{

    private CannedPlayerBase player;

    private CannedControllerListener listener;

    private ServiceManager oldSM;

    private CannedServiceMgr csm;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.setName("PlayerBaseTest");
        suite.addTestSuite(PlayerBaseTest.class);
        return suite;
    }

    public PlayerBaseTest(String arg0)
    {
        super(arg0);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

        player = new CannedPlayerBase();
        listener = new CannedControllerListener(1);

        player.addControllerListener(listener);
        player.setSource(new CannedOcapServiceDataSource());
    }

    /**
	 *
	 */
    protected void tearDown() throws Exception
    {

        player.removeControllerListener(listener);

        player.stop();
        player.close();

        listener = null;
        player = null;

        ManagerManagerTest.updateManager(ServiceManager.class, ServiceMgrImpl.class, true, oldSM);
        oldSM = null;

        csm.destroy();
        csm = null;

        super.tearDown();
    }

    // Test Section
    public void testGetAndSetSource() throws Exception
    {
        DataSource ds = new CannedOcapServiceDataSource();
        player.setSource(ds);
        assertSame("The DataSource objects should be the same", ds, player.getSource());
        player.close();
        assertTrue("Player should return null DataSource when closed", player.getSource() == null);
        player.setSource(ds);
        assertTrue("Player should return null DataSource when closed", player.getSource() == null);
    }

    public void testGetDuration() throws Exception
    {
        CannedPlayerBase localPlayer = new CannedPlayerBase();
        CannedOcapServiceDataSource source = new CannedOcapServiceDataSource();
        source.cannedSetDuration(Duration.DURATION_UNKNOWN);
        localPlayer.setSource(source);
        assertEquals("Duration does not match", Duration.DURATION_UNKNOWN, localPlayer.getDuration());

        DataSource ds = new CannedOcapServiceDataSource();
        localPlayer.setSource(ds);
        assertEquals("Duration does not match", new Time(0).getSeconds(), localPlayer.getDuration().getSeconds(), .0001);
        localPlayer.close();
        assertEquals("Duration does not match", AbstractPlayer.CLOSED_TIME, localPlayer.getDuration());
    }

    public void testGetState()
    {
        player.cannedSetStallAcquireRealizeResources(true);
        assertEquals("Initial state is incorrect, should be Unrealized", Controller.Unrealized, player.getState());
        player.realize();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        // we should be stalled in tne realizing task, which synchronizes
        // on the player lock object, getting the state will also try
        // to synchronize on the same object, so this statement would hang
        // assertEquals("Initial state is incorrect, should be Realizing",
        // Controller.Realizing, player.getState());

        player.stallUntilWaitingForResource();
        synchronized (player.cannedGetLockObject())
        {
            player.cannedGetLockObject().notify();
        }
        listener.waitForEvents(2);
        assertEquals("Initial state is incorrect, should be Realized", Controller.Realized, player.getState());
        assertEquals("Event count is incorrect", 2, listener.events.size());

    }

    public void testGetTargetState()
    {
        assertEquals("Initial state is incorrect, should be Unrealized", Controller.Unrealized, player.getTargetState());
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Initial state is incorrect, should be Realized", Controller.Realized, player.getTargetState());
    }

    public void testAddAndRemoveControllerListener()
    {
        listener.reset();
        player.removeControllerListener(listener);
        player.postEvent(new ControllerEvent(player));
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 0, listener.events.size());

        try
        {
            listener.reset();
            player.addControllerListener(listener);
            player.postEvent(new ControllerEvent(player));
            listener.waitForEvents(1);
            assertEquals("Event count is incorrect", 1, listener.events.size());
        }
        finally
        {
            player.addControllerListener(listener);
        }

    }

    // public void testDoStartDecodingExceptionResourceLost()
    // {
    // player.throwStartDecodingException = true;
    // player.startDecodingExceptionReason =
    // StartDecodingException.RESOURCE_LOST;
    // player.start();
    // listener.waitForEvents(6);
    // assertEquals("Event count is incorrect", 6, listener.events.size());
    // assertTrue(listener.getEvent(5) instanceof StopByResourceLossEvent);
    // }
    //    
    // public void testDoStartDecodingExceptionServiceRemoved()
    // {
    // player.throwStartDecodingException = true;
    // player.startDecodingExceptionReason =
    // StartDecodingException.SERVICE_REMOVED;
    // player.start();
    // listener.waitForEvents(6);
    // //System.out.println("XX Done Waiting XX");
    // assertEquals("Event count is incorrect", 6, listener.events.size());
    // assertTrue(listener.getEvent(5) instanceof ServiceRemovedEvent);
    // }
    public void testDispatchEvent()
    {

        // Send a regular event
        player.postEvent(new ControllerEvent(player));
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        listener.reset();

        player.postEvent(new ControllerClosedEvent(player));
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        // Now that the player is closed, let's send another event
        // and make sure it isn't received.

        listener.reset();
        player.postEvent(new ControllerEvent(player));
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 0, listener.events.size());
    }

    public void testDispatchEventSimple()
    {
        // works
        // Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        // fails
        // Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        // Send a regular event
        player.postEvent(new ControllerEvent(player));
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
    }

    public void testDispatchEventClosed()
    {
        // Send a closed event
        // listener.reset();

        // player.dispatchEvent(new ControllerClosedEvent(player));
        player.close();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());

        // Now that the player is closed, let's send another event
        // and make sure it isn't received.

        listener.reset();
        player.postEvent(new ControllerEvent(player));
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 0, listener.events.size());
    }

    public void testDispatchEventMultiple()
    {
        // works
        // Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        // fails
        // Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        // Send a regular event
        player.postEvent(new ControllerEvent(player));
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        player.postEvent(new ControllerClosedEvent(player));
        listener.waitForEvents(2);
        assertEquals("Event count is incorrect", 2, listener.events.size());

        listener.reset();
        player.postEvent(new ControllerEvent(player));
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 0, listener.events.size());
    }

    public void testGetControlPanelComponent()
    {
        // Try this test with the player unrealized
        try
        {
            player.getControlPanelComponent();
            fail("Expected NotRealizedError");
        }
        catch (NotRealizedError err)
        {
            // expected
        }

        // Now realize the player and repeat the test
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        TransitionEvent event = (TransitionEvent) listener.getEvent(1);
        assertEquals("State should be Realized", Controller.Realized, event.getCurrentState());
        assertNull("Returned control panel component should be null", player.getControlPanelComponent());
    }

    public void testGetGainControl()
    {
        // Try this test with the player unrealized
        try
        {
            player.getGainControl();
            fail("Expected NotRealizedError");
        }
        catch (NotRealizedError err)
        {
            // expected
        }

        // Now realize the player and repeat the test
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        TransitionEvent event = (TransitionEvent) listener.getEvent(1);
        assertEquals("State should be Realized", Controller.Realized, event.getCurrentState());
        assertNull("Returned control panel component should be null", player.getGainControl());
    }

    public void testGetVisualComponent()
    {
        // Try this test with the player unrealized
        try
        {
            player.getVisualComponent();
            fail("Expected NotRealizedError");
        }
        catch (NotRealizedError err)
        {
            // expected
        }

        // Now realize the player and repeat the test
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        TransitionEvent event = (TransitionEvent) listener.getEvent(1);
        assertEquals("State should be Realized", Controller.Realized, event.getCurrentState());
        assertSame("Returned visual component is incorrect", player.component, player.getVisualComponent());

        // Finally, let's close the player
        player.close();
        listener.waitForEvents(1);
        assertNull("Returned control panel component should be null", player.getVisualComponent());
    }

    public void testGetStartLatency()
    {
        // Try this test with the player unrealized
        try
        {
            player.getStartLatency();
            fail("Expected NotRealizedError");
        }
        catch (NotRealizedError err)
        {
            // expected
        }

        // Now realize the player and repeat the test
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        TransitionEvent event = (TransitionEvent) listener.getEvent(1);
        assertEquals("State should be Realized", Controller.Realized, event.getCurrentState());
        assertEquals("Returned time does not match expected value", Controller.LATENCY_UNKNOWN,
                player.getStartLatency());
    }

    // Deallocate tests

    // This test will hang because the realize task is run within a sync
    // block and the deallocate event will try to sync on the same object
    // public void testDeallocateRealizing()
    // {
    // player.cannedSetStallAcquireRealizeResources(true);
    // player.realize();
    // listener.waitForEvents(1);
    // assertEquals("Number of events is incorrect", 1, listener.events.size());
    // assertTrue("Expected TransitionEvent",
    // listener.getEvent(0) instanceof TransitionEvent);
    // player.deallocate();
    // listener.waitForEvents(2);
    // assertEquals("Number of events is incorrect", 2, listener.events.size());
    // DeallocateEvent de = new DeallocateEvent(player, Controller.Realizing,
    // Controller.Unrealized, Controller.Unrealized, new Time(0));
    // checkTransitionEvent(de, (TransitionEvent)listener.getEvent(1));
    // assertEquals("Player should be in Unrealized state",
    // Controller.Unrealized, player.getState());
    // }

    // This test will hang because the prefetch task is run within a sync
    // block and the deallocate event will try to sync on the same object
    // public void testDeallocatePrefetching()
    // {
    // player.cannedSetStallAcquirePrefetchResources(true);
    // player.prefetch();
    // listener.waitForEvents(3);
    // assertEquals("Number of events is incorrect", 3, listener.events.size());
    // assertTrue("Expected TransitionEvent",
    // listener.getEvent(2) instanceof TransitionEvent);
    // player.deallocate();
    // listener.waitForEvents(4);
    // assertEquals("Number of events is incorrect", 4, listener.events.size());
    // DeallocateEvent de = new DeallocateEvent(player, Controller.Prefetching,
    // Controller.Realized, Controller.Realized, new Time(0));
    // checkTransitionEvent(de, (TransitionEvent)listener.getEvent(3));
    // assertEquals("Player should be in Realized state",
    // Controller.Realized, player.getState());
    // }

    // Close tests

    // This test will hang because the realize task is run within a sync
    // block and the close method will try to sync on the same object
    // public void testCloseRealizing()
    // {
    // player.cannedSetStallAcquireRealizeResources(true);
    // player.realize();
    // listener.waitForEvents(1);
    // assertEquals("Number of events is incorrect", 1, listener.events.size());
    // assertTrue("Expected TransitionEvent",
    // listener.getEvent(0) instanceof TransitionEvent);
    // listener.reset();
    // player.close();
    // listener.waitForEvents(2);
    // assertEquals("Number of events is incorrect", 1, listener.events.size());
    // assertTrue("Event should be ControllerClosedEvent",
    // listener.getEvent(0) instanceof ControllerClosedEvent);
    // }

    // This test will hang because the prefetch task is run within a sync
    // block and the close method will try to sync on the same object
    // public void testClosePrefetching()
    // {
    // player.cannedSetStallAcquirePrefetchResources(true);
    // player.prefetch();
    // listener.waitForEvents(3);
    // assertEquals("Number of events is incorrect", 3, listener.events.size());
    // assertEquals("State should be Realized", Controller.Prefetching,
    // player.getState());
    // listener.reset();
    // player.close();
    // listener.waitForEvents(2);
    // assertEquals("Number of events is incorrect", 1, listener.events.size());
    // assertTrue("Event should be ControllerClosedEvent",
    // listener.getEvent(0) instanceof ControllerClosedEvent);
    // }

    // Start tests

    // public void testStartRealizing()
    // {
    // player.cannedSetStallAcquireRealizeResources(true);
    // player.realize();
    // listener.waitForEvents(1);
    // assertEquals("Number of events is incorrect", 1, listener.events.size());
    // // Unrealized -> Realizing
    // TransitionEvent te = new TransitionEvent(player, Controller.Unrealized,
    // Controller.Realizing, Controller.Realized);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(0));
    // // Start and notify the canned player to continue
    // player.start();
    // player.cannedNotify();
    // listener.waitForEvents(5);
    // assertEquals("Number of events is incorrect", 5, listener.events.size());
    // // Realizing -> Realized
    // RealizeCompleteEvent rce = new RealizeCompleteEvent(player,
    // Controller.Realizing, Controller.Realized, Controller.Started);
    // checkTransitionEvent(rce, (TransitionEvent)listener.getEvent(1));
    // // Realized -> Prefetching
    // te = new TransitionEvent(player, Controller.Realized,
    // Controller.Prefetching, Controller.Started);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(2));
    // // Prefetching -> Prefetched
    // PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player,
    // Controller.Prefetching, Controller.Prefetched, Controller.Started);
    // checkTransitionEvent(pce, (TransitionEvent)listener.getEvent(3));
    // // Prefetched -> Started
    // StartEvent se = new StartEvent(player, Controller.Prefetched,
    // Controller.Started, Controller.Started, new Time(0), new Time(0));
    // checkTransitionEvent(se, (TransitionEvent)listener.getEvent(4));
    // assertEquals("Player should be in Started state",
    // Controller.Started, player.getState());
    // }

    // public void testStartPrefetching()
    // {
    // player.cannedSetStallAcquirePrefetchResources(true);
    // player.prefetch();
    // listener.waitForEvents(3);
    // assertEquals("Number of events is incorrect", 3, listener.events.size());
    // // Unrealized -> Realizing
    // TransitionEvent te = new TransitionEvent(player, Controller.Unrealized,
    // Controller.Realizing, Controller.Prefetched);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(0));
    // // Realizing -> Realized
    // RealizeCompleteEvent rce = new RealizeCompleteEvent(player,
    // Controller.Realizing, Controller.Realized, Controller.Prefetched);
    // checkTransitionEvent(rce, (TransitionEvent)listener.getEvent(1));
    // // Realized -> Prefetching
    // te = new TransitionEvent(player, Controller.Realized,
    // Controller.Prefetching, Controller.Prefetched);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(2));
    // // Start the player and notify the canned behavior to continue
    // player.start();
    // player.cannedNotify();
    // listener.waitForEvents(5);
    // assertEquals("Number of events is incorrect", 5, listener.events.size());
    // // Prefetching -> Prefetched
    // PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player,
    // Controller.Prefetching, Controller.Prefetched, Controller.Started);
    // checkTransitionEvent(pce, (TransitionEvent)listener.getEvent(3));
    // // Prefetched -> Started
    // StartEvent se = new StartEvent(player, Controller.Prefetched,
    // Controller.Started, Controller.Started, new Time(0), new Time(0));
    // checkTransitionEvent(se, (TransitionEvent)listener.getEvent(4));
    // assertEquals("Player should be in Started state",
    // Controller.Started, player.getState());
    // }

    // Realize tests

    // public void testRealizeRealizing()
    // {
    // player.cannedSetStallAcquireRealizeResources(true);
    // player.realize();
    // listener.waitForEvents(1);
    // assertEquals("Event count is incorrect", 1, listener.events.size());
    // player.realize();
    // listener.waitForEvents(1);
    // assertEquals("Event count is incorrect", 1, listener.events.size());
    // player.stallUntilWaitingForResource();
    // synchronized(player.cannedGetLockObject())
    // {
    // player.cannedGetLockObject().notify();
    // }
    // listener.waitForEvents(2);
    // assertEquals("Event count is incorrect", 2, listener.events.size());
    // // Check the transition event
    // TransitionEvent te = new TransitionEvent(player, Controller.Unrealized,
    // Controller.Realizing, Controller.Realized);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(0));
    // // Check the completion event
    // RealizeCompleteEvent re = new RealizeCompleteEvent(player,
    // Controller.Realizing, Controller.Realized, Controller.Realized);
    // checkTransitionEvent(re, (TransitionEvent)listener.getEvent(1));
    // assertEquals("Player should be in Realized state",
    // Controller.Realized, player.getState());
    // }

    // public void testRealizePrefetching()
    // {
    // player.cannedSetStallAcquirePrefetchResources(true);
    // player.prefetch();
    // listener.waitForEvents(3);
    // assertEquals("Event count is incorrect", 3, listener.events.size());
    // assertEquals("Player is not in Prefetching state",
    // Controller.Prefetching, player.getState());
    // player.realize();
    // listener.waitForEvents(4);
    // assertEquals("Event count is incorrect", 4, listener.events.size());
    // player.stallUntilWaitingForResource();
    // synchronized(player.cannedGetLockObject())
    // {
    // player.cannedGetLockObject().notify();
    // }
    // listener.waitForEvents(5);
    // assertEquals("Event count is incorrect", 5, listener.events.size());
    // RealizeCompleteEvent rce = new RealizeCompleteEvent(player,
    // Controller.Prefetching,
    // Controller.Prefetching, Controller.Prefetched);
    // checkTransitionEvent(rce, (TransitionEvent)listener.getEvent(3));
    // PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player,
    // Controller.Prefetching, Controller.Prefetched, Controller.Prefetched);
    // checkTransitionEvent(pce, (TransitionEvent)listener.getEvent(4));
    // assertEquals("Player should be in Prefetched state",
    // Controller.Prefetched, player.getState());
    // }

    public void testRealizeFailure()
    {
        player.cannedSetFailAcquireRealizeResources(true);
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Event count is incorrect", 2, listener.events.size());
        // Check the transition event
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Realized);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        // Now the ResourceUnavailableEvent
        assertTrue("Event is not of the correct type, should be ResourceUnavailableEvent",
                listener.getEvent(1) instanceof ResourceUnavailableEvent);
        assertTrue("Player state should be Unrealized", player.getState() == Controller.Unrealized);
    }

    // Prefetch tests

    // public void testPrefetchRealizing()
    // {
    // player.cannedSetStallAcquireRealizeResources(true);
    // player.realize();
    // listener.waitForEvents(1);
    // assertEquals("Event count is incorrect", 1, listener.events.size());
    // assertEquals("Player should be in Realizing state",
    // Controller.Realizing, player.getState());
    // player.prefetch();
    // player.stallUntilWaitingForResource();
    // synchronized(player.cannedGetLockObject())
    // {
    // player.cannedGetLockObject().notify();
    // }
    // listener.waitForEvents(4);
    // assertEquals("Number of events is incorrect", 4, listener.events.size());
    // // Realizing -> Realized
    // RealizeCompleteEvent rce = new RealizeCompleteEvent(player,
    // Controller.Realizing, Controller.Realized, Controller.Prefetched);
    // checkTransitionEvent(rce, (TransitionEvent)listener.getEvent(1));
    // // Realized -> Prefetching
    // TransitionEvent te = new TransitionEvent(player, Controller.Realized,
    // Controller.Prefetching, Controller.Prefetched);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(2));
    // // Prefetching -> Prefetched
    // PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player,
    // Controller.Prefetching, Controller.Prefetched, Controller.Prefetched);
    // checkTransitionEvent(pce, (TransitionEvent)listener.getEvent(3));
    // assertEquals("Player should be in Prefetched state",
    // Controller.Prefetched, player.getState());
    // }
    //    

    // public void testPrefetchPrefetching()
    // {
    // player.cannedSetStallAcquirePrefetchResources(true);
    // player.prefetch();
    // listener.waitForEvents(3);
    // assertEquals("Event count is incorrect", 3, listener.events.size());
    // // Unrealized -> Realizing
    // TransitionEvent te = new TransitionEvent(player, Controller.Unrealized,
    // Controller.Realizing, Controller.Prefetched);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(0));
    // // Realizing -> Realized
    // RealizeCompleteEvent rce = new RealizeCompleteEvent(player,
    // Controller.Realizing, Controller.Realized, Controller.Prefetched);
    // checkTransitionEvent(rce, (TransitionEvent)listener.getEvent(1));
    // // Realized -> Prefetching
    // te = new TransitionEvent(player, Controller.Realized,
    // Controller.Prefetching, Controller.Prefetched);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(2));
    // // Prefetch the player and notify the canned behavior to continue
    // player.prefetch();
    // // wait for the realizing event
    // listener.waitForEvents(1);
    // player.stallUntilWaitingForResource();
    // synchronized(player.cannedGetLockObject())
    // {
    // player.cannedGetLockObject().notify();
    // }
    // listener.waitForEvents(4);
    // assertEquals("Number of events is incorrect", 4, listener.events.size());
    // // Prefetching -> Prefetched
    // PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player,
    // Controller.Prefetching, Controller.Prefetched, Controller.Prefetched);
    // checkTransitionEvent(pce, (TransitionEvent)listener.getEvent(3));
    // assertEquals("Player should be in Prefetched state",
    // Controller.Prefetched, player.getState());
    // }

    public void testPrefetchFailure()
    {
        player.cannedSetFailAcquirePrefetchResources(true);
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Event count is incorrect", 4, listener.events.size());
        // Unrealized -> Realizing
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Prefetched);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        // Realizing -> Realized
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Prefetched);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(1));
        // Realized -> Prefetching
        te = new TransitionEvent(player, Controller.Realized, Controller.Prefetching, Controller.Prefetched);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(2));
        // Now the ResourceUnavailableEvent
        assertTrue("Event is not of the correct type, should be ResourceUnavailableEvent" + ", instead is "
                + listener.getEvent(3), listener.getEvent(3) instanceof ResourceUnavailableEvent);
        assertEquals("Player state should be Realized", Controller.Realized, player.getState());
    }

    // Stop tests

    // public void testStopRealizing()
    // {
    // player.cannedSetStallAcquireRealizeResources(true);
    // player.start();
    // listener.waitForEvents(1);
    // assertEquals("Event count is incorrect", 1, listener.events.size());
    // assertEquals("Player should be in Realizing state", Controller.Realizing,
    // player.getState());
    // TransitionEvent te = new TransitionEvent(player, Controller.Unrealized,
    // Controller.Realizing, Controller.Started);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(0));
    // player.stop();
    // listener.waitForEvents(2);
    // assertEquals("Event count is incorrect", 2, listener.events.size());
    // StopByRequestEvent sbre = new StopByRequestEvent(player,
    // Controller.Realizing, Controller.Realizing,
    // Controller.Realized, new Time(0));
    // checkTransitionEvent(sbre, (TransitionEvent)listener.getEvent(1));
    // player.stallUntilWaitingForResource();
    // synchronized(player.cannedGetLockObject())
    // {
    // player.cannedGetLockObject().notify();
    // }
    // listener.waitForEvents(3);
    // assertEquals("Event count is incorrect", 3, listener.events.size());
    // RealizeCompleteEvent re = new RealizeCompleteEvent(player,
    // Controller.Realizing, Controller.Realized, Controller.Realized);
    // checkTransitionEvent(re, (TransitionEvent)listener.getEvent(2));
    // assertEquals("Player should be in Started state",
    // Controller.Realized, player.getState());
    // }

    // public void testStopPrefetching()
    // {
    // player.cannedSetStallAcquirePrefetchResources(true);
    // player.start();
    // listener.waitForEvents(3);
    // assertEquals("Event count is incorrect", 3, listener.events.size());
    // assertEquals("Player should be in Prefetching state",
    // Controller.Prefetching,
    // player.getState());
    // TransitionEvent te = new TransitionEvent(player, Controller.Realized,
    // Controller.Prefetching, Controller.Started);
    // checkTransitionEvent(te, (TransitionEvent)listener.getEvent(2));
    // player.stop();
    // listener.waitForEvents(4);
    // assertEquals("Event count is incorrect", 4, listener.events.size());
    // StopByRequestEvent sbre = new StopByRequestEvent(player,
    // Controller.Prefetching, Controller.Prefetching,
    // Controller.Prefetched, new Time(0));
    // checkTransitionEvent(sbre, (TransitionEvent)listener.getEvent(3));
    // player.stallUntilWaitingForResource();
    // synchronized(player.cannedGetLockObject())
    // {
    // player.cannedGetLockObject().notify();
    // }
    // listener.waitForEvents(5);
    // assertEquals("Event count is incorrect", 5, listener.events.size());
    // PrefetchCompleteEvent pre = new PrefetchCompleteEvent(player,
    // Controller.Prefetching, Controller.Prefetched, Controller.Prefetched);
    // checkTransitionEvent(pre, (TransitionEvent)listener.getEvent(4));
    // assertEquals("Player should be in Prefetched state",
    // Controller.Prefetched, player.getState());
    // }

    // MAS: addControl() removed since it is unused
    // public void testAddControl()
    // {
    // Control control = player.getControl("org.cablelabs.impl.media." +
    // "player.CannedPlayerBase$CannedControlBase");
    // player.addControl((CannedControlBase)control);
    //        
    // boolean controlFound = false;
    // Control[] controlList = player.getControls();
    // for(int i = 0; i < controlList.length; i++)
    // {
    // if(controlList[i].equals(control))
    // {
    // controlFound = true;
    // break;
    // }
    // }
    //        
    // assertTrue(controlFound);
    // }

    public void testLostAndRegainedResource()
    {
        player.lostResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Expected ResourceWithdrawnEvent, instead received " + listener.getEvent(0).getClass().getName(),
                listener.getEvent(0) instanceof ResourceWithdrawnEvent);

        listener.reset();
        player.regainedResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Expected ResourceReturnedEvent, instead received " + listener.getEvent(0).getClass().getName(),
                listener.getEvent(0) instanceof ResourceReturnedEvent);
    }

    public void testLostResourceTwiceButOneEvent()
    {
        player.lostResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        player.lostResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("resourcesAreWithdrawn should return true", player.resourcesAreWithdrawn());
    }

    public void testLoseTwoResourcesOnlyGainOne()
    {
        player.lostResource();
        player.lostResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());

        listener.reset();
        player.regainedResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 0, listener.events.size());
    }

    public void xtestCallerContextClosesPlayer() throws Exception
    {
        CallerContextManager save = null;

        try
        {
            save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            final CCMgr ccmgr = new CCMgr(save);
            CannedPlayerBaseForTestingContextCallback.setCCMgr(ccmgr);

            ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, ccmgr);

            final MyDummyContext cc = new MyDummyContext();
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    CannedPlayerBaseForTestingContextCallback localPlayer = new CannedPlayerBaseForTestingContextCallback();
                    cc.player = localPlayer;
                }
            });

            //
            // verify the callback was created on the player
            //
            // assertTrue(cc.player.ccDataClosePlayer != null);

            //
            // verify that the callback was added
            //
            // assertTrue(cc.callback.equals(cc.player.ccDataClosePlayer));

            //
            // verify that calling the callback closes the player
            //
            // cc.player.ccDataClosePlayer.destroy(cc);
            assertTrue(cc.player.isClosed());

        }
        finally
        {
            if (save != null)
            {
                ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
                CannedPlayerBaseForTestingContextCallback.setCCMgr(save);
            }
        }
    }

    // Helper methods

    private void checkTransitionEvent(TransitionEvent expected, TransitionEvent actual)
    {
        assertTrue("Actual event is not of the expected type, expected: " + expected.getClass() + ", actual: "
                + actual.getClass(), expected.getClass().isInstance(actual));
        assertEquals("Player source does not match", expected.getSourceController(), actual.getSourceController());
        assertEquals("Target state is incorrect", expected.getTargetState(), actual.getTargetState());
        assertEquals("Current state is incorrect", expected.getCurrentState(), actual.getCurrentState());
        assertEquals("Previous state is incorrect", expected.getPreviousState(), actual.getPreviousState());
    }

    private static class MyDummyContext extends DummyContext
    {
        AbstractPlayer player;

        CallbackData callback;

        public void addCallbackData(CallbackData data, Object key)
        {
            callback = data;
            super.addCallbackData(data, key);
        }

    }

    //
    // a test class to ensure that the AbstractPlayer constructor gets called
    // with null
    //
    private static class CannedPlayerBaseForTestingContextCallback extends AbstractPlayer
    {
        CannedPlayerBaseForTestingContextCallback()
        {
            super(null, new Object(), new ResourceUsageImpl(null));
        }

        static void setCCMgr(CallerContextManager mgr)
        {
            AbstractPlayer.ccMgr = mgr;
        }

        protected Object doAcquirePrefetchResources()
        {
            return null;
        }

        protected Object doAcquireRealizeResources()
        {
            return null;
        }

        protected Component doGetVisualComponent()
        {
            return null;
        }

        protected void doReleaseAllResources()
        {
        }

        protected void doReleasePrefetchedResources()
        {
        }

        protected void doReleaseRealizedResources()
        {
        }

        protected Presentation createPresentation()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean getMute()
        {
            return false;
        }

        public float getGain()
        {
            return 0.0F;
        }
    }
}
