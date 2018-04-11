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

package org.cablelabs.impl.media;

import javax.media.Clock;
import javax.media.ClockStartedError;
import javax.media.ClockStoppedException;
import javax.media.StopTimeSetError;
import javax.media.Time;
import javax.media.TimeBase;

import junit.framework.TestCase;

import org.cablelabs.impl.media.player.ClockImpl;
import org.cablelabs.impl.media.player.MediaTimeBase;

public class ClockImplTest extends TestCase
{
    private static final long NANOSPERSEC = 1000000000L;

    private static final int MILLISPERSEC = 1000;

    public static void Sleep(int millies)
    {
        try
        {
            Thread.sleep(millies);
        }
        catch (Exception x)
        {
        }
    }

    public static void AssertStopped(ClockImpl clock, String msg)
    {
        // try to call a method that isn't allowed on a stopped clock; it should
        // fail
        try
        {
            clock.mapToTimeBase(new Time(0));
            fail(msg);
        }
        catch (ClockStoppedException x)
        {
            // expected
        }
        catch (Exception x)
        {
            fail("Unexpected exception: " + x);
        }
    }

    public static void AssertStarted(ClockImpl clock, String msg)
    {
        // try to call a method that isn't allowed on a started clock; it should
        // fail
        try
        {
            clock.setRate(0);
            fail(msg);
        }
        catch (ClockStartedError x)
        {
            // expected
        }
        catch (Exception x)
        {
            fail("Unexpected exception: " + x);
        }
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ClockImplTest.class);
    }

    public ClockImplTest(String arg0)
    {
        super(arg0);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.media.ClockImpl.setTimeBase(TimeBase)'
     */
    public final void testSetTimeBase()
    {
        ClockImpl clock = new ClockImpl();
        TimeBase tb = new MediaTimeBase();
        try
        {
            // assign a MediaTimeBase
            clock.setTimeBase(tb);
            // reset to current value
            clock.setTimeBase(null);
        }
        catch (Exception x)
        {
            fail("Unexpected exception: " + x.toString());
        }
        // start the clock and try to assign new time base; it should fail
        try
        {
            clock.syncStart(new Time(0));
            clock.setTimeBase(tb);
            fail("Didn't get expected ClockStartedError exception.");
        }
        catch (ClockStartedError x)
        {
        }
        catch (Exception x)
        {
            fail("Unexpected exception: " + x);
        }
    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.getTimeBase()'
     */
    public final void testGetTimeBase()
    {
        ClockImpl clock = new ClockImpl();
        TimeBase tb = clock.getTimeBase();
        assertNotNull("got null TimeBase from Clock", tb);
        assertTrue("TimeBase is not a MediaTimeBase", tb instanceof MediaTimeBase);
    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.syncStart(Time)'
     */
    public final void testSyncStart_now()
    {
        ClockImpl clock = new ClockImpl();
        clock.syncStart(new Time(0));
        try
        {
            clock.syncStart(new Time(0));
            fail("Didn't get expected ClockStartedError calling syncStart() on started ClockImpl.");
        }
        catch (ClockStartedError x)
        {
            // expected it
        }
    }

    public final void testSyncStart_later()
    {
        ClockImpl clock = new ClockImpl();
        TimeBase tb = clock.getTimeBase();
        long now = tb.getNanoseconds();
        long later = now + 3 * NANOSPERSEC;
        clock.syncStart(new Time(later));

        // since sync time not yet arrived, getSyncTime() should return a
        // decreasing value until 0;
        // then it should return the actual media time

        Time syncTime = clock.getSyncTime();
        long syncNanos = syncTime.getNanoseconds();
        assertTrue("getSyncTime() is too small (less than zero)", syncNanos > 0);
        assertTrue("getSyncTime() is too large (greater than syncStart - now)", syncNanos <= (later - now));

        // Now sleep for a second and get the sync time. It should be less than
        // previous value.
        Sleep(1 * MILLISPERSEC);
        assertTrue("getSyncTime() is not decreasing", clock.getSyncTime().getNanoseconds() < syncNanos);

        AssertStarted(clock, "Clock not started before syncStart time.");

        // now sleep for 4 seconds and try again; should still be started
        Sleep(4 * MILLISPERSEC);
        AssertStarted(clock, "Clock not started and after syncStart time.");

        // now we are passed the syncStart time, so getSyncTime() should return
        // an increasing media time
        syncNanos = clock.getSyncTime().getNanoseconds();
        Sleep(1 * MILLISPERSEC);
        assertTrue("media time is not increasing after syncStart time has passed",
                clock.getSyncTime().getNanoseconds() > syncNanos);
    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.stop()'
     */
    public final void testStop_states()
    {
        ClockImpl clock = new ClockImpl();

        AssertStopped(clock, "Newly constructed Clock not stopped.");

        // should be able to stop a clock that is already stopped
        try
        {
            clock.stop();
        }
        catch (Exception x)
        {
            fail("Unexpected exception from Clock.stop(): " + x);
        }

        // now try to stop a started clock
        try
        {
            clock.syncStart(new Time(0));
        }
        catch (Exception x)
        {
            fail("Unexpected exception from Clock.syncStart(): " + x);
        }

        clock.stop();
        AssertStopped(clock, "stop() didn't stop the Clock.");
    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.setStopTime(Time)'
     */
    public final void testSetStopTime_forward()
    {
        ClockImpl clock = new ClockImpl();

        // stop after a while
        clock.setStopTime(new Time(2 * NANOSPERSEC));
        clock.syncStart(new Time(0));

        AssertStarted(clock, "syncStart() didn't start the Clock.");

        // sleep until we're sure that stop time has been reached
        Sleep(3 * MILLISPERSEC);
        AssertStopped(clock, "Clock didn't stop at stopTime.");
    }

    public final void testSetStopTime_reverse()
    {
        ClockImpl clock = new ClockImpl();

        // media time runs in reverse, starting at five seconds and stopping at
        // 2 seconds
        clock.setRate(-1);
        clock.setMediaTime(new Time(5 * NANOSPERSEC));
        clock.setStopTime(new Time(3 * NANOSPERSEC));
        clock.syncStart(new Time(0));

        // clock should be started for next 2 seconds
        AssertStarted(clock, "Clock not started immediately after syncStart().");
        Sleep(3 * MILLISPERSEC);
        AssertStopped(clock, "Clock not stopped after hitting media stop time in reverse.");
    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.setStopTime(Time)'
     */
    public final void testSetStopTime_calledTwice()
    {
        ClockImpl clock = new ClockImpl();

        // stop after a while
        clock.setStopTime(new Time(2 * NANOSPERSEC));
        clock.syncStart(new Time(0));

        try
        {
            clock.setStopTime(new Time(0));
            fail("setStopTime() didn't throw StopTimeSetError when stop time set twice.");
        }
        catch (StopTimeSetError x)
        {
            // expected this
        }
    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.setStopTime(Time)'
     */
    public final void testSetStopTime_reset()
    {
        ClockImpl clock = new ClockImpl();

        // stop after a while
        clock.setStopTime(new Time(2 * NANOSPERSEC));
        clock.syncStart(new Time(0));
        // now reset so it shouldn't stop
        clock.setStopTime(Clock.RESET);

        assertSame(clock.getStopTime(), Clock.RESET);

        AssertStarted(clock, "syncStart() didn't start the Clock.");

        // sleep until we're sure that original stop time has been reached
        Sleep(3 * MILLISPERSEC);
        AssertStarted(clock, "stopTime didn't stop the Clock.");
    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.getStopTime()'
     */
    public final void testGetStopTime()
    {
        ClockImpl clock = new ClockImpl();
        Time stopTime = clock.getStopTime();
        assertSame("getStopTime() on stopped clock didn't return Clock.RESET", stopTime, Clock.RESET);

        stopTime = new Time(5 * NANOSPERSEC);
        clock.setStopTime(stopTime);
        assertEquals("getStopTime() returned different value than was assigned", stopTime.getNanoseconds(),
                clock.getStopTime().getNanoseconds());

    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.setMediaTime(Time)'
     */
    public final void testSetMediaTime_states()
    {
        ClockImpl clock = new ClockImpl();

        clock.setMediaTime(new Time(0));

        clock.syncStart(new Time(0));
        try
        {
            clock.setMediaTime(new Time(0));
            fail("setMediaTime() didn't fail on a started Clock");
        }
        catch (ClockStartedError x)
        {
            // expected
        }
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.media.ClockImpl.getMediaNanoseconds()'
     */
    public final void testGetMediaNanoseconds()
    {
        ClockImpl clock = new ClockImpl();

        assertEquals(clock.getMediaNanoseconds(), 0);

        clock.syncStart(new Time(0));
        Sleep(1000);
        assertTrue(clock.getMediaNanoseconds() > 0);
    }

    // /*
    // * Test method for 'org.cablelabs.impl.media.ClockImpl.getSyncTime()'
    // */
    // public final void testGetSyncTime()
    // {
    // ClockImpl clock = new ClockImpl();
    // }
    //
    // /*
    // * Test method for
    // 'org.cablelabs.impl.media.ClockImpl.mapToTimeBase(Time)'
    // */
    // public final void testMapToTimeBase()
    // {
    // ClockImpl clock = new ClockImpl();
    // }
    //
    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.getRate()'
     */
    public final void testGetRate()
    {
        ClockImpl clock = new ClockImpl();
        assertEquals("default Clock rate isn't 1", clock.getRate(), 1, 0);
    }

    /*
     * Test method for 'org.cablelabs.impl.media.ClockImpl.setRate(float)'
     */
    public final void testSetRate_states()
    {
        ClockImpl clock = new ClockImpl();
        try
        {
            clock.setRate(1);
        }
        catch (Exception x)
        {
            fail("Unexpected exception: " + x);
        }

        clock.syncStart(new Time(0));
        try
        {
            clock.setRate(0);
            fail("setRate() on started Clock didn't get expected ClockStartedError.");
        }
        catch (ClockStartedError x)
        {
            // expected
        }
    }

    public final void testSetRate_accuracy()
    {
        ClockImpl clock = new ClockImpl();
        clock.setRate(0.5f);
        clock.syncStart(new Time(0));

        // Play for four seconds of time-base time
        Sleep(4 * MILLISPERSEC);
        // Media time should be about half the expired time because rate is 0.5
        // Expect to be within a two tenths of a second accuracy
        double mt = clock.getMediaNanoseconds();
        double expectedMt = 2 * NANOSPERSEC;
        double variance = 0.2 * NANOSPERSEC;
        assertEquals("Media time (" + mt + ") didn't match expected (" + expectedMt + ").", mt, expectedMt, variance);
    }

}
