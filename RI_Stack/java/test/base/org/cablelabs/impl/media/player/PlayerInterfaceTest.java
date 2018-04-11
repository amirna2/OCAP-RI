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

import javax.media.ClockStartedError;
import javax.media.ClockStoppedException;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.DeallocateEvent;
import javax.media.IncompatibleTimeBaseException;
import javax.media.Manager;
import javax.media.MediaTimeSetEvent;
import javax.media.NotRealizedError;
import javax.media.PrefetchCompleteEvent;
import javax.media.RateChangeEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.StartEvent;
import javax.media.StopByRequestEvent;
import javax.media.StopEvent;
import javax.media.StopTimeChangeEvent;
import javax.media.StopTimeSetError;
import javax.media.Time;
import javax.media.TimeBase;
import javax.media.TransitionEvent;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.davic.media.ResourceReturnedEvent;
import org.davic.media.ResourceWithdrawnEvent;

import org.cablelabs.impl.media.JMFBaseInterfaceTest;

/**
 * PlayerInterfaceTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class PlayerInterfaceTest extends JMFBaseInterfaceTest
{

    private Player player;

    private CannedControllerListener listener;

    private PlayerHelper helper;

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     */
    public PlayerInterfaceTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public PlayerInterfaceTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
    }

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(PlayerInterfaceTest.class, factory);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        player = (Player) createImplObject();
        listener = new CannedControllerListener(1);
        player.addControllerListener(listener);

        helper = new PlayerHelper(player, listener);
    }

    public void tearDown() throws Exception
    {
        player.close();

        listener = null;
        helper = null;
        player = null;

        super.tearDown();
    }

    // Test Section

    public void testRestartingPlayer()
    {
        helper.prefetchPlayer();
        helper.callSyncStartWithNoWait();
        //
        // at this time, we have at least received the start event,
        // wait for further events that could be sent such as
        // LeavingLiveMode/EnteringLiveMode, MediaPresentedEvent,
        // and MediaPresentationEvent
        //
        listener.waitForMediaPresentedEvent();
        listener.waitForMediaPresentationEvent();
        listener.reset();
        player.stop();
        listener.waitForStopEvent();
        StopByRequestEvent stopEvent = (StopByRequestEvent) listener.findEventOfClass(StopByRequestEvent.class);
        assertTrue(stopEvent != null);
        // prefetchPlayer();
        helper.callSyncStartWithNoWait();
    }

    public void testSyncStartOverdue()
    {
        helper.prefetchPlayer();
        helper.callSyncStartWithNoWait();
    }

    public void testSyncStartWithWait()
    {
        helper.prefetchPlayer();
        TimeBase tb = player.getTimeBase();
        Time now = tb.getTime();
        Time startTime = new Time(now.getSeconds() + Time.ONE_SECOND);
        listener.reset();
        player.syncStart(startTime);
        listener.waitForEvents(1);
        assertTrue("Player State is not Started", player.getState() == Controller.Started);
        assertTrue("Did not receive the correct StartEvent", listener.getEvent(0) instanceof StartEvent);
    }

    public void testStopOnStoppedUnrealizedPlayer()
    {
        //
        // The stop calls should be ignored on an unstarted player
        //
        assertTrue(player.getState() == Controller.Unrealized);
        player.stop();
        assertTrue(player.getState() == Controller.Unrealized);
    }

    public void testStopOnStoppedPrefetchedPlayer()
    {
        helper.prefetchPlayer();
        player.stop();
        assertTrue(player.getState() == Controller.Prefetched);
    }

    public void testCallingSyncStartTwice()
    {
        helper.prefetchPlayer();
        helper.callSyncStartWithNoWait();
        try
        {
            player.syncStart(new Time(0));
            fail("Second syncStart call did not fail");
        }
        catch (ClockStartedError err)
        {
            // expected outcome
        }
    }

    public void testStop()
    {
        helper.prefetchPlayer();
        helper.callSyncStartWithNoWait();
        listener.waitForMediaPresentationEvent();
        //
        // at this time, we have at least received the start event,
        // wait for further events that could be sent such as
        // LeavingLiveMode/EnteringLiveMode, MediaPresentedEvent,
        // and MediaPresentationEvent
        //
        listener.reset();
        player.stop();
        listener.waitForStopEvent();
        StopEvent evt = (StopEvent) listener.findEventOfClass(StopEvent.class);
        assertTrue(evt != null);
    }

    public void testGetControlsNotNull()
    {
        Control[] controls = player.getControls();
        assertTrue(controls != null);
    }

    public void testGetControlByName()
    {
        Control[] controls = player.getControls();
        assertTrue(controls != null);

        for (int i = 0; i < controls.length; i++)
        {
            String className = controls[i].getClass().getName();
            Control controlByClassName = player.getControl(className);
            //
            // assume that there is only one control of that class in the
            // controls list
            //
            assertTrue(controlByClassName.equals(controls[i]));
            //
            // get all of the interfaces implemented
            //
            Class[] allClasses = controls[i].getClass().getInterfaces();
            for (int j = 0; j < allClasses.length; j++)
            {
                String interfaceName = allClasses[j].getName();
                Control controlByInterfaceName = player.getControl(interfaceName);
                //
                // It is reasonable that multiple controllers implement
                // the same interace, so just verify that the returned
                // controller is not null
                //
                assertTrue(controlByInterfaceName != null);
            }
        }
    }

    public void testGetControlBadNameReturnsNull()
    {
        Control test = player.getControl("abcdefg");
        assertTrue(test == null);
    }

    public void testGetRateFromStartedPlayer()
    {
        helper.startPlayer();
        assertEquals("Returned rate is incorrect", 1.0f, player.getRate(), 0.0001f);
    }

    public void testGetRateFromClosedPlayer()
    {
        player.close();
        listener.waitForEvents(1);
        float rate = player.getRate();
        assertTrue(rate == 0.0f);
    }

    public void testGetStopTimeFromClosedPlayer()
    {
        player.close();
        listener.waitForEvents(1);
        Time stopTime = player.getStopTime();
        assertTrue(stopTime.getSeconds() == 0);
    }

    public void testGetSyncTimeFromClosedPlayer()
    {
        player.close();
        listener.waitForEvents(1);
        Time syncTime = player.getSyncTime();
        assertTrue(syncTime.getSeconds() == 0);
    }

    public void testGetTimeBaseFromClosedPlayer()
    {
        player.close();
        listener.waitForEvents(1);
        TimeBase timeBase = player.getTimeBase();
        assertTrue(timeBase.equals(Manager.getSystemTimeBase()));
    }

    public void testMapToTimeBaseOnClosedPlayer() throws Exception
    {
        player.close();
        listener.waitForEvents(1);
        Time time = player.mapToTimeBase(new Time(1000));
        assertTrue(time.getSeconds() == 0);
    }

    public void testSetMediaTimeNotRealized()
    {
        Time time = new Time(100);
        try
        {
            player.setMediaTime(time);
            fail("setMediaTime on an unrealized player didn't fail");
        }
        catch (NotRealizedError e)
        {
            // since the player isn't realized, an exception should occur
        }
    }

    public void testSetMediaTime()
    {
        Time time = new Time(100);
        player.realize();
        listener.waitForEvents(2);
        listener.reset();

        player.setMediaTime(time);

        listener.waitForEvents(1);
        ControllerEvent event = listener.getEvent(0);
        assertTrue(event != null);
        assertTrue(event.getSource().equals(player));
        assertTrue(event instanceof MediaTimeSetEvent);
    }

    public void testSetMediaTimeOnClosed()
    {
        Time time = new Time(100);
        player.realize();
        listener.waitForRealizeCompleteEvent();
        listener.reset();
        player.close();
        listener.waitForEvents(1);

        listener.reset();

        player.setMediaTime(time);

        listener.waitForEvents(1);
        //
        // since the player is closed, we shouldn't have received an event,
        // the wait should have timed out
        //
        if (listener.events.size() > 0)
        {
            System.out.println("EVENT: " + listener.getEvent(0));
        }
        assertEquals("Number of events is incorrect", 0, listener.events.size());
    }

    public void testSetRate()
    {
        player.realize();
        listener.waitForEvents(2);
        listener.reset();
        float newRate = player.setRate(2f);
        listener.waitForEvents(1);
        ControllerEvent event = listener.getEvent(0);
        assertTrue(event != null);
        assertTrue(event.getSource().equals(player));
        assertTrue("event type " + event.getClass().getName(), event instanceof RateChangeEvent);
    }

    public void testSetRateOnClosedPlayer()
    {
        player.close();
        listener.waitForEvents(1);
        float newRate = player.setRate(2f);
        assertTrue(newRate == 0);
        listener.waitForEvents(1);
        // make sure we didn't receive a RateChangeEvent
        for (int i = 0; i < listener.events.size(); i++)
        {
            assertTrue(!(listener.getEvent(i) instanceof RateChangeEvent));
        }
    }

    public void testSetStopTimeOnClosed()
    {
        Time time = new Time(100);
        player.close();
        listener.waitForEvents(1);
        listener.reset();

        player.setStopTime(time);

        listener.waitForEvents(1);
        //
        // since the player is closed, we shouldn't have received an event,
        // the wait should have timed out
        //
        assertEquals("Number of events is incorrect", 0, listener.events.size());
    }

    public void testSetStopTimeOnUnrealized()
    {
        Time time = new Time(100);
        try
        {
            player.setStopTime(time);
            fail("setStopTime on an unrealized player didn't fail");
        }
        catch (NotRealizedError e)
        {
            // since the player isn't realized, an exception should occur
        }
    }

    public void testStopTime()
    {
        Time time = new Time(100);
        player.realize();
        listener.waitForRealizeCompleteEvent();
        listener.reset();

        player.setStopTime(time);
        player.setStopTime(new Time(5));

        listener.waitForEvents(1);
        ControllerEvent event = listener.getEvent(0);
        assertTrue(event != null);
        assertTrue(event.getSource().equals(player));
        assertTrue(event instanceof StopTimeChangeEvent);
    }

    public void testGetAndSetTimeBase() throws Exception
    {
        player.realize();
        listener.waitForRealizeCompleteEvent();
        assertEquals("Player should be in Realized state", Controller.Realized, player.getState());
        TimeBase timeBase = new MyTimeBase();
        player.setTimeBase(timeBase);
        assertEquals("Returned TimeBase is incorrect", timeBase, player.getTimeBase());
    }

    public void testTimeBaseOnClosed() throws Exception
    {
        TimeBase timeBase = new MyTimeBase();
        player.close();
        listener.waitForEvents(1);
        listener.reset();

        player.setTimeBase(timeBase);

        listener.waitForEvents(1);
        //
        // since the player is closed, we shouldn't have received an event,
        // the wait should have timed out
        //
        assertEquals("Number of events is incorrect", 0, listener.events.size());
    }

    public void xtestLostResource()
    {
        // player.lostResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue(listener.getEvent(0) instanceof ResourceWithdrawnEvent);
    }

    public void xtestRegainedResource()
    {
        // player.regainedResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue(listener.getEvent(0) instanceof ResourceReturnedEvent);
    }

    public void xtestLostResourceTwiceButOneEvent()
    {
        // player.lostResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        // player.lostResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        // assertTrue("resourcesAreWithdrawn should return true",
        // player.resourcesAreWithdrawn());
    }

    public void xtestLoseTwoResourcesOnlyGainOne()
    {
        // player.lostResource();
        // player.lostResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());

        listener.reset();
        // player.regainedResource();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 0, listener.events.size());
    }

    public void testSetNullSource() throws Exception
    {
        try
        {
            player.setSource(null);
            fail("No exception occurred when setting a null source");
        }
        catch (Exception exc)
        {
            // The actual exception depends on the type of player.
            // It's probably safe to just assume it'll thrown an Exception
            // of some type.
        }
    }

    public void testGetControlNullname()
    {
        try
        {
            player.getControl(null);
            fail("No exception occurred getting control with a null name");
        }
        catch (NullPointerException exc)
        {
            // expected outcome
        }
    }

    public void testMapToTimeBase() throws Exception
    {
        helper.startPlayer();
        Time t = new Time(100);
        assertNotNull(player.mapToTimeBase(t));
    }

    public void testMapToTimeBaseWithNullTime() throws Exception
    {
        try
        {
            player.mapToTimeBase(null);
            fail("No exception occurred mapping to time base with null");
        }
        catch (NullPointerException exc)
        {
            // expected outcome
        }
    }

    public void testMapToTimeBaseWithNegativeTime() throws Exception
    {
        try
        {
            Time t = new Time(-1l);
            player.mapToTimeBase(t);
            fail("No exception occurred mapping to time base with negative");
        }
        catch (IllegalArgumentException exc)
        {
            // expected outcome
        }
    }

    public void testMapToTimeBaseNotStarted() throws Exception
    {
        try
        {
            player.realize();
            listener.waitForEvents(2);
            assertEquals("Initial state is incorrect, should be Realized", Controller.Realized, player.getTargetState());
            Time t = new Time(10l);
            player.mapToTimeBase(t);
            fail("No exception occurred mapping to time base when not started");
        }
        catch (ClockStoppedException exc)
        {
            // expected outcome
        }
    }

    public void testSetMediaTimeWithNullTime() throws Exception
    {
        try
        {
            player.setMediaTime(null);
            fail("No exception setting media time to null");
        }
        catch (NullPointerException exc)
        {
            // expected outcome
        }
    }

    public void testSetMediaTimeWithNegativeTime() throws Exception
    {
        try
        {
            Time t = new Time(-1l);
            player.setMediaTime(t);
            fail("No exception setting media time to negative");
        }
        catch (IllegalArgumentException exc)
        {
            // expected outcome
        }
    }

    public void testSetTimeBaseNull() throws Exception
    {
        try
        {
            player.setTimeBase(null);
            fail("No exception setting time base to null");
        }
        catch (NullPointerException exc)
        {
            // expected outcome
        }
    }

    public void testSyncStartWithNullTime() throws Exception
    {
        try
        {
            player.syncStart(null);
            fail("No exception syncstarting with null");
        }
        catch (NullPointerException exc)
        {
            // expected outcome
        }
    }

    public void testSyncStartWithNegativeTime() throws Exception
    {
        try
        {
            Time t = new Time(-1l);
            player.syncStart(t);
            fail("No exception syncstarting with negative time");
        }
        catch (IllegalArgumentException exc)
        {
            // expected outcome
        }
    }

    public void testSyncStartClosed()
    {
        player.close();
        listener.waitForEvents(1);
        listener.reset();
        player.syncStart(new Time(1));
        listener.waitForEvents(1);
        //
        // since the player is closed, it shouldn't start
        //
        assertTrue(listener.events.size() == 0);
    }

    public void testSetTimeBase() throws Exception
    {
        helper.realizePlayer();
        TimeBase tb = new MyTimeBase();
        player.setTimeBase(tb);
        assertTrue(tb.equals(player.getTimeBase()));
    }

    public void testGetDuration()
    {
        assertNotNull("Returned time should not be null", player.getDuration());
    }

    public void testAddController()
    {
        try
        {
            player.addController(player);
            fail("Call to addController() should've thrown an IncompatibleTimeBaseException");
        }
        catch (IncompatibleTimeBaseException ex)
        {
            // expected
        }
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
        helper.realizePlayer();
        try
        {
            player.getVisualComponent();
        }
        catch (NotRealizedError err)
        {
            fail("NotRealizedError should not have been thrown with Realized Player");
        }
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

    // Deallocate tests

    public void testDeallocateUnrealized()
    {
        player.deallocate();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        DeallocateEvent de = new DeallocateEvent(player, Controller.Unrealized, Controller.Unrealized,
                Controller.Unrealized, new Time(0));
        checkTransitionEvent(de, (TransitionEvent) listener.getEvent(0));
        assertEquals("Player should be in Unrealized state", Controller.Unrealized, player.getState());
    }

    public void xtestDeallocateRealizing()
    {
        // player.cannedSetStallAcquireRealizeResources(true);
        player.realize();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Expected TransitionEvent", listener.getEvent(0) instanceof TransitionEvent);
        player.deallocate();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        DeallocateEvent de = new DeallocateEvent(player, Controller.Realizing, Controller.Unrealized,
                Controller.Unrealized, new Time(0));
        checkTransitionEvent(de, (TransitionEvent) listener.getEvent(1));
        assertEquals("Player should be in Unrealized state", Controller.Unrealized, player.getState());
    }

    public void testDeallocateRealized()
    {
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        assertTrue("Expected RealizeCompleteEvent", listener.getEvent(1) instanceof RealizeCompleteEvent);
        player.deallocate();
        listener.waitForEvents(3);
        assertEquals("Number of events is incorrect", 3, listener.events.size());
        DeallocateEvent de = new DeallocateEvent(player, Controller.Realized, Controller.Realized, Controller.Realized,
                new Time(0));
        checkTransitionEvent(de, (TransitionEvent) listener.getEvent(2));
        assertEquals("Player should be in Realized state", Controller.Realized, player.getState());
    }

    public void xtestDeallocatePrefetching()
    {
        // player.cannedSetStallAcquirePrefetchResources(true);
        player.prefetch();
        listener.waitForEvents(3);
        assertEquals("Number of events is incorrect", 3, listener.events.size());
        assertTrue("Expected TransitionEvent", listener.getEvent(2) instanceof TransitionEvent);
        player.deallocate();
        listener.waitForEvents(4);
        assertEquals("Number of events is incorrect", 4, listener.events.size());
        DeallocateEvent de = new DeallocateEvent(player, Controller.Prefetching, Controller.Realized,
                Controller.Realized, new Time(0));
        checkTransitionEvent(de, (TransitionEvent) listener.getEvent(3));
        assertEquals("Player should be in Realized state", Controller.Realized, player.getState());
    }

    public void testDeallocatePrefetched()
    {
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Number of events is incorrect", 4, listener.events.size());
        assertTrue("Expected PrefetchCompleteEvent", listener.getEvent(3) instanceof PrefetchCompleteEvent);
        player.deallocate();
        listener.waitForEvents(5);
        assertEquals("Number of events is incorrect", 5, listener.events.size());
        DeallocateEvent de = new DeallocateEvent(player, Controller.Prefetched, Controller.Realized,
                Controller.Realized, new Time(0));
        checkTransitionEvent(de, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Realized state", Controller.Realized, player.getState());
    }

    //
    // The TCK Test suite expects that calling close on a started player
    // will not throw an exception
    //
    public void testCloseOnStartedDoesNotThrowsException()
    {
        helper.startPlayer();
        try
        {
            player.close();
        }
        catch (ClockStartedError err)
        {
            fail("Calling close on a started player threw an exception");
        }
    }

    public void testDeallocateStarted()
    {
        helper.startPlayer();
        try
        {
            player.deallocate();
            fail("Call to deallocate() should fail when player is started");
        }
        catch (ClockStartedError err)
        {
            // expected
        }
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
    }

    public void testDeallocateClosed()
    {
        player.close();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
        player.deallocate();
        listener.waitForEvents(2);
        assertEquals("Received unexpected event when calling deallocate on a closed Player", 1, listener.events.size());
    }

    // Close tests

    public void testCloseUnrealized()
    {
        // We'll call close on an unrealized player
        player.close();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
    }

    public void xtestCloseRealizing()
    {
        // player.cannedSetStallAcquireRealizeResources(true);
        player.realize();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Expected TransitionEvent", listener.getEvent(0) instanceof TransitionEvent);
        listener.reset();
        player.close();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
    }

    public void testCloseRealized()
    {
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        assertEquals("State should be Realized", Controller.Realized, player.getState());
        listener.reset();
        player.close();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
    }

    public void xtestClosePrefetching()
    {
        // player.cannedSetStallAcquirePrefetchResources(true);
        player.prefetch();
        listener.waitForEvents(3);
        assertEquals("Number of events is incorrect", 3, listener.events.size());
        assertEquals("State should be Realized", Controller.Prefetching, player.getState());
        listener.reset();
        player.close();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
    }

    public void testClosePrefetched()
    {
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Number of events is incorrect", 4, listener.events.size());
        ControllerEvent event = listener.getEvent(3);
        assertEquals("State should be Realized", Controller.Prefetched, ((TransitionEvent) event).getCurrentState());
        listener.reset();
        player.close();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
    }

    public void xtestCloseStarted()
    {
        helper.startPlayer();
        try
        {
            player.close();
            fail("Call to close() should fail when player is started");
        }
        catch (ClockStartedError err)
        {
            // expected
        }
    }

    public void testCloseClosed()
    {
        player.close();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
        listener.reset();
        player.close();
        listener.waitForEvents(1);
        assertEquals("Received unexpected event when calling close() on a closed Player", 0, listener.events.size());
    }

    // Start tests

    public void testStartUnrealized()
    {
        helper.startPlayer();
        // Unrealized -> Realizing
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Started);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        // Realizing -> Realized
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Started);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(1));
        // Realized -> Prefetching
        te = new TransitionEvent(player, Controller.Realized, Controller.Prefetching, Controller.Started);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(2));
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Started);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        // Prefetched -> Started
        StartEvent se = new StartEvent(player, Controller.Prefetched, Controller.Started, Controller.Started, new Time(
                0), new Time(0));
        checkTransitionEvent(se, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
    }

    public void xtestStartRealizing()
    {
        // player.cannedSetStallAcquireRealizeResources(true);
        player.realize();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        // Unrealized -> Realizing
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Realized);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        // Start and notify the canned player to continue
        player.start();
        // player.cannedNotify();
        listener.waitForEvents(5);
        assertEquals("Number of events is incorrect", 5, listener.events.size());
        // Realizing -> Realized
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Started);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(1));
        // Realized -> Prefetching
        te = new TransitionEvent(player, Controller.Realized, Controller.Prefetching, Controller.Started);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(2));
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Started);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        // Prefetched -> Started
        StartEvent se = new StartEvent(player, Controller.Prefetched, Controller.Started, Controller.Started, new Time(
                0), new Time(0));
        checkTransitionEvent(se, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
    }

    public void testStartRealized()
    {
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        // Unrealized -> Realizing
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Realized);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        // Realizing -> Realized
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Realized);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(1));
        // We're in the realized state, so let's go ahead and start
        player.start();
        listener.waitForEvents(5);
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
        // Realized -> Prefetching
        te = new TransitionEvent(player, Controller.Realized, Controller.Prefetching, Controller.Started);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(2));
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Started);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        // Prefetched -> Started
        StartEvent se = new StartEvent(player, Controller.Prefetched, Controller.Started, Controller.Started, new Time(
                0), new Time(0));
        checkTransitionEvent(se, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
    }

    public void xtestStartPrefetching()
    {
        // player.cannedSetStallAcquirePrefetchResources(true);
        player.prefetch();
        listener.waitForEvents(3);
        assertEquals("Number of events is incorrect", 3, listener.events.size());
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
        // Start the player and notify the canned behavior to continue
        player.start();
        // player.cannedNotify();
        listener.waitForEvents(5);
        assertEquals("Number of events is incorrect", 5, listener.events.size());
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Started);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        // Prefetched -> Started
        StartEvent se = new StartEvent(player, Controller.Prefetched, Controller.Started, Controller.Started, new Time(
                0), new Time(0));
        checkTransitionEvent(se, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
    }

    public void testStartPrefetched()
    {
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Number of events is incorrect", 4, listener.events.size());
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
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        // We're now prefetched, let's go ahead and start
        player.start();
        listener.waitForEvents(5);
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
        // Prefetched -> Started
        StartEvent se = new StartEvent(player, Controller.Prefetched, Controller.Started, Controller.Started, new Time(
                0), new Time(0));
        checkTransitionEvent(se, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
    }

    public void testStartStarted()
    {
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();
        listener.reset();
        player.start();
        listener.waitForStartEvent();
        assertTrue("Number of events is incorrect", 1 <= listener.events.size());
        StartEvent startEvent = (StartEvent) listener.findEventOfClass(StartEvent.class);
        assertTrue("Did not receive a start event " + listener.events, startEvent != null);
        StartEvent se = new StartEvent(player, Controller.Started, Controller.Started, Controller.Started, new Time(0),
                new Time(0));
        checkTransitionEvent(se, startEvent);
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
    }

    public void testStartClosed()
    {
        player.close();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
        listener.reset();
        player.start();
        listener.waitForEvents(1);
        assertEquals("Received unexpected event when calling start() on a closed Player", 0, listener.events.size());
    }

    // Realize tests

    public void testRealizeUnrealized()
    {
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Event count is incorrect", 2, listener.events.size());
        // Check the transition event
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Realized);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        // Check the completion event
        RealizeCompleteEvent re = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Realized);
        checkTransitionEvent(re, (TransitionEvent) listener.getEvent(1));
        assertEquals("Player should be in Started state", Controller.Realized, player.getState());
    }

    public void xtestRealizeRealizing()
    {
        // player.cannedSetStallAcquireRealizeResources(true);
        player.realize();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        player.realize();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        // player.stallUntilWaitingForResource();
        // synchronized(player.cannedGetLockObject())
        // {
        // player.cannedGetLockObject().notify();
        // }
        listener.waitForEvents(2);
        assertEquals("Event count is incorrect", 2, listener.events.size());
        // Check the transition event
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Realized);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        // Check the completion event
        RealizeCompleteEvent re = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Realized);
        checkTransitionEvent(re, (TransitionEvent) listener.getEvent(1));
        assertEquals("Player should be in Realized state", Controller.Realized, player.getState());
    }

    public void testRealizeRealized()
    {
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Event count is incorrect", 2, listener.events.size());
        assertTrue("Event is not of the correct type, should be TransitionEvent",
                listener.getEvent(1) instanceof TransitionEvent);
        player.realize();
        listener.waitForEvents(3);
        assertEquals("Event count is incorrect", 3, listener.events.size());
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Realized, Controller.Realized,
                Controller.Realized);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(2));
        assertEquals("Player should be in Realized state", Controller.Realized, player.getState());
    }

    public void xtestRealizePrefetching()
    {
        // player.cannedSetStallAcquirePrefetchResources(true);
        player.prefetch();
        listener.waitForEvents(3);
        assertEquals("Event count is incorrect", 3, listener.events.size());
        assertEquals("Player is not in Prefetching state", Controller.Prefetching, player.getState());
        player.realize();
        listener.waitForEvents(4);
        assertEquals("Event count is incorrect", 4, listener.events.size());
        // player.stallUntilWaitingForResource();
        // synchronized(player.cannedGetLockObject())
        // {
        // player.cannedGetLockObject().notify();
        // }
        listener.waitForEvents(5);
        assertEquals("Event count is incorrect", 5, listener.events.size());
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Prefetching, Controller.Prefetching,
                Controller.Prefetched);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(3));
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
    }

    public void testRealizePrefetched()
    {
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Event count is incorrect", 4, listener.events.size());
        assertEquals("Player is not in Prefetched state", Controller.Prefetched, player.getState());
        player.realize();
        listener.waitForEvents(5);
        assertEquals("Event count is incorrect", 5, listener.events.size());
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Prefetched, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player is not in Prefetched state", Controller.Prefetched, player.getState());
    }

    public void testRealizeStarted()
    {
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();
        listener.reset();
        player.realize();
        listener.waitForRealizeCompleteEvent();
        assertTrue("Event count is incorrect", 1 <= listener.events.size());
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Started, Controller.Started,
                Controller.Started);
        ControllerEvent evt = listener.findEventOfClass(RealizeCompleteEvent.class);
        assertTrue("Did not receive RealizeCompleteEvent" + listener.events, evt != null);
        checkTransitionEvent(rce, (TransitionEvent) evt);
        assertEquals("Player is not in Started state", Controller.Started, player.getState());
    }

    public void testRealizeClosed()
    {
        player.close();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
        player.realize();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
    }

    public void xtestRealizeFailure()
    {
        // player.cannedSetFailAcquireRealizeResources(true);
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

    public void testPrefetchUnrealized()
    {
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Event count is incorrect", 4, listener.events.size());
        // Check the transition event
        TransitionEvent te = new TransitionEvent(player, Controller.Realized, Controller.Prefetching,
                Controller.Prefetched);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(2));
        // Check the completion event
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
    }

    public void xtestPrefetchRealizing()
    {
        // player.cannedSetStallAcquireRealizeResources(true);
        player.realize();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        assertEquals("Player should be in Realizing state", Controller.Realizing, player.getState());
        player.prefetch();
        // player.stallUntilWaitingForResource();
        // synchronized(player.cannedGetLockObject())
        // {
        // player.cannedGetLockObject().notify();
        // }
        listener.waitForEvents(4);
        assertEquals("Number of events is incorrect", 4, listener.events.size());
        // Realizing -> Realized
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Prefetched);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(1));
        // Realized -> Prefetching
        TransitionEvent te = new TransitionEvent(player, Controller.Realized, Controller.Prefetching,
                Controller.Prefetched);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(2));
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
    }

    public void testPrefetchRealized()
    {
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Number of events is incorrect", 2, listener.events.size());
        // Unrealized -> Realizing
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Realized);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        // Realizing -> Realized
        RealizeCompleteEvent rce = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Realized);
        checkTransitionEvent(rce, (TransitionEvent) listener.getEvent(1));
        // We're in the realized state, so let's go ahead and prefetch
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Number of events is incorrect", 4, listener.events.size());
        // Realized -> Prefetching
        te = new TransitionEvent(player, Controller.Realized, Controller.Prefetching, Controller.Prefetched);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(2));
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
    }

    public void xtestPrefetchPrefetching()
    {
        // player.cannedSetStallAcquirePrefetchResources(true);
        player.prefetch();
        listener.waitForEvents(3);
        assertEquals("Event count is incorrect", 3, listener.events.size());
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
        // Prefetch the player and notify the canned behavior to continue
        player.prefetch();
        // wait for the realizing event
        listener.waitForEvents(1);
        // player.stallUntilWaitingForResource();
        // synchronized(player.cannedGetLockObject())
        // {
        // player.cannedGetLockObject().notify();
        // }
        listener.waitForEvents(4);
        assertEquals("Number of events is incorrect", 4, listener.events.size());
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
    }

    public void testPrefetchPrefetched()
    {
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Number of events is incorrect", 4, listener.events.size());
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
        // Prefetching -> Prefetched
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(3));
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
        // We're now prefetched, let's go ahead and prefetch again
        player.prefetch();
        listener.waitForEvents(5);
        assertEquals("Number of events is incorrect", 5, listener.events.size());
        pce = new PrefetchCompleteEvent(player, Controller.Prefetched, Controller.Prefetched, Controller.Prefetched);
        checkTransitionEvent(pce, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
    }

    public void testPrefetchStarted()
    {
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();
        listener.reset();
        player.prefetch();
        listener.waitForPrefetchCompleteEvent();
        assertTrue("Number of events is incorrect", 1 <= listener.events.size());
        PrefetchCompleteEvent pce = new PrefetchCompleteEvent(player, Controller.Started, Controller.Started,
                Controller.Started);
        PrefetchCompleteEvent evt = (PrefetchCompleteEvent) listener.findEventOfClass(PrefetchCompleteEvent.class);
        checkTransitionEvent(pce, evt);
        assertEquals("Player should be in Started state", Controller.Started, player.getState());
    }

    public void testPrefetchClosed()
    {
        player.close();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
        player.prefetch();
        listener.waitForEvents(1);
        assertEquals("Received unexpected event when calling start() on a closed Player", 1, listener.events.size());
    }

    public void xtestPrefetchFailure()
    {
        // player.cannedSetFailAcquirePrefetchResources(true);
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

    public void testStopUnrealized()
    {
        player.stop();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        StopByRequestEvent sbre = new StopByRequestEvent(player, Controller.Unrealized, Controller.Unrealized,
                Controller.Unrealized, new Time(0));
        checkTransitionEvent(sbre, (TransitionEvent) listener.getEvent(0));
    }

    public void xtestStopRealizing()
    {
        // player.cannedSetStallAcquireRealizeResources(true);
        player.start();
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 1, listener.events.size());
        assertEquals("Player should be in Realizing state", Controller.Realizing, player.getState());
        TransitionEvent te = new TransitionEvent(player, Controller.Unrealized, Controller.Realizing,
                Controller.Started);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(0));
        player.stop();
        listener.waitForEvents(2);
        assertEquals("Event count is incorrect", 2, listener.events.size());
        StopByRequestEvent sbre = new StopByRequestEvent(player, Controller.Realizing, Controller.Realizing,
                Controller.Realized, new Time(0));
        checkTransitionEvent(sbre, (TransitionEvent) listener.getEvent(1));
        // player.stallUntilWaitingForResource();
        // synchronized(player.cannedGetLockObject())
        // {
        // player.cannedGetLockObject().notify();
        // }
        listener.waitForEvents(3);
        assertEquals("Event count is incorrect", 3, listener.events.size());
        RealizeCompleteEvent re = new RealizeCompleteEvent(player, Controller.Realizing, Controller.Realized,
                Controller.Realized);
        checkTransitionEvent(re, (TransitionEvent) listener.getEvent(2));
        assertEquals("Player should be in Started state", Controller.Realized, player.getState());
    }

    public void testStopRealized()
    {
        player.realize();
        listener.waitForEvents(2);
        assertEquals("Event count is incorrect", 2, listener.events.size());
        assertEquals("Player should be in Realized state", Controller.Realized, player.getState());
        player.stop();
        listener.waitForEvents(3);
        assertEquals("Event count is incorrect", 3, listener.events.size());
        StopByRequestEvent sbre = new StopByRequestEvent(player, Controller.Realized, Controller.Realized,
                Controller.Realized, new Time(0));
        checkTransitionEvent(sbre, (TransitionEvent) listener.getEvent(2));
    }

    public void xtestStopPrefetching()
    {
        // player.cannedSetStallAcquirePrefetchResources(true);
        player.start();
        listener.waitForEvents(3);
        assertEquals("Event count is incorrect", 3, listener.events.size());
        assertEquals("Player should be in Prefetching state", Controller.Prefetching, player.getState());
        TransitionEvent te = new TransitionEvent(player, Controller.Realized, Controller.Prefetching,
                Controller.Started);
        checkTransitionEvent(te, (TransitionEvent) listener.getEvent(2));
        player.stop();
        listener.waitForEvents(4);
        assertEquals("Event count is incorrect", 4, listener.events.size());
        StopByRequestEvent sbre = new StopByRequestEvent(player, Controller.Prefetching, Controller.Prefetching,
                Controller.Prefetched, new Time(0));
        checkTransitionEvent(sbre, (TransitionEvent) listener.getEvent(3));
        // player.stallUntilWaitingForResource();
        // synchronized(player.cannedGetLockObject())
        // {
        // player.cannedGetLockObject().notify();
        // }
        listener.waitForEvents(5);
        assertEquals("Event count is incorrect", 5, listener.events.size());
        PrefetchCompleteEvent pre = new PrefetchCompleteEvent(player, Controller.Prefetching, Controller.Prefetched,
                Controller.Prefetched);
        checkTransitionEvent(pre, (TransitionEvent) listener.getEvent(4));
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
    }

    public void testStopPrefetched()
    {
        player.prefetch();
        listener.waitForEvents(4);
        assertEquals("Event count is incorrect", 4, listener.events.size());
        assertEquals("Player should be in Prefetched state", Controller.Prefetched, player.getState());
        player.stop();
        listener.waitForEvents(5);
        assertEquals("Event count is incorrect", 5, listener.events.size());
        StopByRequestEvent sbre = new StopByRequestEvent(player, Controller.Prefetched, Controller.Prefetched,
                Controller.Prefetched, new Time(0));
        checkTransitionEvent(sbre, (TransitionEvent) listener.getEvent(4));
    }

    public void testStopStarted()
    {
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();
        listener.reset();
        player.stop();
        listener.waitForStopEvent();
        assertTrue("Event count is incorrect", 1 <= listener.events.size());
        StopByRequestEvent sbre = new StopByRequestEvent(player, Controller.Started, Controller.Prefetched,
                Controller.Prefetched, new Time(0));
        StopByRequestEvent stopEvent = (StopByRequestEvent) listener.findEventOfClass(StopByRequestEvent.class);
        assertTrue("Did not receive a stop event - " + listener.events, stopEvent != null);
        checkTransitionEvent(sbre, stopEvent);
    }

    public void testStopClosed()
    {
        player.close();
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        assertTrue("Event should be ControllerClosedEvent", listener.getEvent(0) instanceof ControllerClosedEvent);
        player.stop();
        listener.waitForEvents(1);
        assertEquals("Received unexpected event when calling start() on a closed Player", 1, listener.events.size());
    }

    public void testGetMediaTimeForStartedPlayer()
    {
        helper.startPlayer();
        assertNotNull(player.getMediaTime());
    }

    public void testGetMediaTimeForClosedPlayer()
    {
        player.close();
        listener.waitForEvents(1);

        Time time = player.getMediaTime();
        assertEquals("Returned time is incorrect", 0L, time.getNanoseconds());
    }

    public void testStopBySpecifyingStopTimeAndPositiveWholeRate()
    {
        helper.prefetchPlayer();
        helper.setStopTime(2d);
        helper.setRate(1f);
        helper.callSyncStartWithNoWait();
        assertTrue(listener.waitForStopEvent(3000));
    }

    public void testSetStopTimeTwice()
    {
        helper.prefetchPlayer();
        helper.callSyncStartWithNoWait();
        //
        // set the stop time far enough in the future that it won't
        // be encountered
        //
        helper.setStopTime(20d);
        try
        {
            helper.setStopTime(40d);
            fail("Calling setStopTime twice should result in an exception");
        }
        catch (StopTimeSetError err)
        {

        }
    }

    public void testSetStopTimeReset()
    {
        helper.prefetchPlayer();
        helper.setStopTime(2d);
        Time stopTime = player.getStopTime();
        assertTrue(stopTime != null);

        player.setStopTime(Player.RESET);
        boolean received = listener.waitForStopTimeChangeEvent();
        assertTrue(received);

        Time stopTimeAfterReset = player.getStopTime();
        assertTrue("Stop time wasn't cleared", stopTimeAfterReset.equals(Player.RESET));
    }

    public void testStopBySpecifyingStopTimeBeforeSyncStart()
    {
        helper.prefetchPlayer();
        //
        // before starting, set to stop in 2 seconds
        //
        double timerOffset = 4d;
        player.setStopTime(new Time(timerOffset));
        boolean stopTimeChangeEventReceived = listener.waitForStopTimeChangeEvent();
        assertTrue(stopTimeChangeEventReceived);

        //
        // get the start time now because the actual process of starting
        // the player could take a while
        //
        double currentTime = System.currentTimeMillis() / 1000;
        player.syncStart(new Time(0));
        boolean stopEventReceived = listener.waitForStopEvent((long) ((timerOffset + 2) * 1000)); // wait
                                                                                                  // longer
                                                                                                  // than
                                                                                                  // stop
                                                                                                  // time
        double startedTime = System.currentTimeMillis() / 1000;
        //
        // do a very rough check that the timer started after
        // the offset, but before too long
        //
        double difference = startedTime - currentTime;
        assertTrue("Difference is " + difference + " offset was " + Math.abs(timerOffset),
                difference >= Math.abs(timerOffset));

        assertTrue("The stop event was not received - " + listener.events, stopEventReceived);

        assertTrue("Player state is not prefetched -- " + player.getState(), player.getState() == Controller.Prefetched);
    }

    public void testStopBySpecifyingStopTimeAfterSyncStart()
    {
        double timerOffset = 2d;
        helper.prefetchPlayer();

        player.syncStart(new Time(0));
        boolean startEventReceived = listener.waitForStartEvent();
        assertTrue(startEventReceived);
        listener.waitForMediaPresentationEvent();
        listener.reset();
        double currentTime = System.currentTimeMillis() / 1000;
        //
        // now that we are started, set to stop in 1 seconds
        //
        Time currentMediaTime = player.getMediaTime();
        player.setStopTime(new Time(currentMediaTime.getSeconds() + timerOffset));
        boolean stopTimeChangeEventReceived = listener.waitForStopTimeChangeEvent();
        assertTrue(stopTimeChangeEventReceived);

        boolean stopEventReceived = listener.waitForStopEvent((long) (timerOffset + 1) * 1000);
        double startedTime = System.currentTimeMillis() / 1000;
        //
        // do a very rough check that the timer started after
        // the offset, but before too long
        //
        double difference = startedTime - currentTime;
        assertTrue("Difference is " + difference + " offset was " + Math.abs(timerOffset),
                difference >= Math.abs(timerOffset));

        assertTrue("The stop event was not received - " + listener.events, stopEventReceived);

        assertTrue("Player state is not prefetched -- " + player.getState(), player.getState() == Controller.Prefetched);
    }

    // Test support

    private void checkTransitionEvent(TransitionEvent expected, TransitionEvent actual)
    {
        assertTrue("Actual event is not of the expected type, expected: " + expected.getClass() + ", actual: "
                + actual.getClass(), expected.getClass().isInstance(actual));
        assertEquals("Player source does not match", expected.getSourceController(), actual.getSourceController());
        assertEquals("Target state is incorrect", expected.getTargetState(), actual.getTargetState());
        assertEquals("Current state is incorrect", expected.getCurrentState(), actual.getCurrentState());
        assertEquals("Previous state is incorrect", expected.getPreviousState(), actual.getPreviousState());
    }

    private static class MyTimeBase implements TimeBase
    {
        Time time;

        public long getNanoseconds()
        {
            return 0;
        }

        public Time getTime()
        {
            return time;
        }

    }
}
