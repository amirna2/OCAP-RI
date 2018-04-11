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

package javax.tv.util;

import junit.framework.*;
import java.util.*;

/**
 * Tests TVTimer.
 * 
 * @author Aaron Kamienski
 */
public class TVTimerTest extends TestCase
{
    private static final boolean DEBUG = false;

    /**
     * Tests getTimer().
     */
    public void testGetTimer()
    {
        TVTimer timer = TVTimer.getTimer();
        assertNotNull("getTimer should not return null", timer);
        // Although, "one per call" allowed... we'll expect a singleton
        assertSame("getTimer is expected to return a singleton", timer, TVTimer.getTimer());
    }

    protected void assertEquals(String msg, TVTimerSpec expected, TVTimerSpec actual)
    {
        if (expected == actual) return;

        assertEquals(msg + ": absolute", expected.isAbsolute(), actual.isAbsolute());
        assertEquals(msg + ": repeat", expected.isRepeat(), actual.isRepeat());
        assertEquals(msg + ": regular", expected.isRegular(), actual.isRegular());
        assertEquals(msg + ": time", expected.getTime(), actual.getTime());
    }

    /**
     * Tests scheduleTimerSpec().
     */
    public void testSchedule() throws Exception
    {
        TVTimerSpec t = new TVTimerSpec();

        // -- Verify that equivalent is returned
        // Shouldn't have any trouble scheduling this...
        t.setDelayTime(10000);

        try
        {
            TVTimerSpec t2 = timer.scheduleTimerSpec(t);
            boolean isEquivalent = t.isAbsolute() == t2.isAbsolute() && t.isRegular() == t2.isRegular()
                    && t.isRepeat() == t2.isRepeat() && Math.abs(t.getTime() - t2.getTime()) < timer.getGranularity();
            assertTrue("Expected timer.scheduleTimerSpec(t) to return equivalent", isEquivalent);
        }
        finally
        {
            timer.deschedule(t);
        }
    }

    /**
     * Tests scheduleTimerSpec() of an absolute TVTimerSpec.
     */
    public void testScheduleAbsolute() throws Exception
    {
        TVTimerSpec t = new TVTimerSpec();
        Listener l = new Listener();
        t.addTVTimerWentOffListener(l);

        // Test 0
        t.setAbsoluteTime(0L);
        try
        {
            timer.scheduleTimerSpec(t);
            Thread.sleep(granularity * 50);
            assertTrue("Absolute timer in the past should've gone off immeditely", l.event != null);
            assertEquals("Timer should've gone off once", 1, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        // Test NOW-100
        t.setAbsoluteTime(System.currentTimeMillis() - 100);
        try
        {
            timer.scheduleTimerSpec(t);
            Thread.sleep(granularity * 50);
            assertTrue("Absolute timer in the past (NOW-100) should've gone off immeditely", l.event != null);
            assertEquals("Timer should've gone off once", 1, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        // Test NOW
        t.setAbsoluteTime(System.currentTimeMillis());
        try
        {
            timer.scheduleTimerSpec(t);
            Thread.sleep(granularity * 50);
            assertTrue("Absolute timer scheduled for now should've gone off immeditely", l.event != null);
            assertEquals("Timer should've gone off once", 1, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        // Test NOW+granularity*50
        long time = System.currentTimeMillis() + granularity * 50;
        t.setAbsoluteTime(time);
        try
        {
            timer.scheduleTimerSpec(t);
            assertTrue("Absolute timer scheduled for later went off immediately", l.event == null);
            Thread.sleep(granularity * 25);
            assertTrue("Absolute timer scheduled for later went off too soon", l.event == null);
            //
            // the timer should go off sometime in the between after 25
            //
            Thread.sleep(granularity * 50);
            assertTrue("Absolute timer scheduled for later should've gone off by now, scheduled for " + time
                    + " and is now " + System.currentTimeMillis(), l.event != null);
            assertEquals("Timer should've gone off once", 1, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
        }
    }

    /**
     * Tests scheduleTimerSpec() non-repeating, delayed TVTimerSpec.
     */
    public void testScheduleDelayed() throws Exception
    {
        TVTimerSpec t = new TVTimerSpec();
        Listener l = new Listener();
        t.addTVTimerWentOffListener(l);

        // Test delay of 0
        t.setDelayTime(0);
        try
        {
            timer.scheduleTimerSpec(t);
            Thread.sleep(100);
            assertTrue("Delayed timer scheduled for 0 delay should've gone off immeditely", l.event != null);
            assertEquals("Timer should've gone off once", 1, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        // Test delay of granularity*2
        t.setDelayTime(granularity * 40);
        try
        {
            timer.scheduleTimerSpec(t);
            assertTrue("Delayed timer scheduled for later went off immediately", l.event == null);
            Thread.sleep(granularity * 25);
            assertTrue("Delayed timer scheduled for later went off too soon", l.event == null);
            Thread.sleep(granularity * 30);
            assertTrue("Delayed timer scheduled for later should've gone off by now", l.event != null);
            assertEquals("Timer should've gone off once", 1, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
        }
    }

    /**
     * Tests scheduleTimerSpec() regular repeating TVTimerSpec.
     */
    public void testScheduleRepeatingRegular() throws Exception
    {
        TVTimerSpec t = new TVTimerSpec();
        Listener l = new Listener();
        t.addTVTimerWentOffListener(l);

        final long delay = interval * 20;

        // Verify repetition
        t.setDelayTime(delay);
        t.setRepeat(true);
        t.setRegular(true);
        try
        {
            int expect = 3;
            timer.scheduleTimerSpec(t);
            Thread.sleep((delay * expect) + interval * 10);
            assertTrue("Repeated timer scheduled should've gone off", l.event != null);
            assertEquals("Repeated timer should've gone off n times after delay", expect, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        // Assert that interval doesn't change if listener wastes time
        t.removeTVTimerWentOffListener(l);
        l = new Listener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                super.timerWentOff(e);
                try
                {
                    Thread.sleep(delay / 2);
                }
                catch (Exception ex)
                {
                }
            }
        };
        t.addTVTimerWentOffListener(l);

        t.setDelayTime(delay);
        t.setRepeat(true);
        t.setRegular(true);
        try
        {
            int expect = 5;
            timer.scheduleTimerSpec(t);
            Thread.sleep((delay * expect) + interval * 10);
            assertTrue("Repeated timer scheduled should've gone off", l.event != null);
            assertEquals("Repeated timer should've gone off n times after delay " + "- in regular fashion", expect,
                    l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }
    }

    /**
     * Tests scheduleTimerSpec() repeating TVTimerSpec.
     */
    public void testScheduleRepeating() throws Exception
    {
        TVTimerSpec t = new TVTimerSpec();
        Listener l = new Listener();
        t.addTVTimerWentOffListener(l);

        final long delay = interval * 20;

        // Verify repetition
        t.setDelayTime(delay);
        t.setRepeat(true);
        t.setRegular(false);
        try
        {
            int expect = 3;
            timer.scheduleTimerSpec(t);
            Thread.sleep((delay * expect) + interval * 19);
            assertTrue("Repeated timer scheduled should've gone off", l.event != null);
            assertEquals("Repeated timer should've gone off n times after delay", expect, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        // Assert that interval changes if listener wastes time
        t.removeTVTimerWentOffListener(l);
        l = new Listener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                try
                {
                    Thread.sleep(interval * 10);
                }
                catch (Exception ex)
                {
                }
                super.timerWentOff(e);
            }
        };
        t.addTVTimerWentOffListener(l);

        t.setDelayTime(delay);
        t.setRepeat(true);
        t.setRegular(false);
        try
        {
            int expect = 5;
            TVTimerSpec actual = timer.scheduleTimerSpec(t);
            Thread.sleep((actual.getTime() * expect) + (interval * 10 * expect) + interval * 10);
            assertTrue("Repeated timer scheduled should've gone off", l.event != null);
            assertEquals("Repeated timer should've gone off n times after delay " + "- in non-regular fashion", expect,
                    l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }
    }

    /**
     * Tests scheduleTimerSpec() invalid cases.
     */
    public void testScheduleInvalid() throws Exception
    {
        try
        {
            timer.scheduleTimerSpec(null);
            fail("Expected exception when scheduling null TimerSpec");
        }
        catch (NullPointerException e)
        {
        }
    }

    /**
     * Tests scheduleTimerSpec() multiple scheduling of same spec. Also tests
     * deschedule(), which only needs to be called once.
     */
    public void testScheduleMultipleSame() throws Exception
    {
        TVTimerSpec t = new TVTimerSpec();
        Listener l = new Listener();
        t.addTVTimerWentOffListener(l);

        final long delay = granularity * 20;

        // Try delayed timer first
        t.setDelayTime(delay);
        try
        {
            timer.scheduleTimerSpec(t);
            timer.scheduleTimerSpec(t);
            timer.scheduleTimerSpec(t);
            assertTrue("Delayed timer scheduled for later went off immediately", l.event == null);
            Thread.sleep(delay / 2);
            assertTrue("Delayed timer scheduled for later went off too soon", l.event == null);
            Thread.sleep((delay / 2) + granularity * 10);
            assertTrue("Delayed timer scheduled for later should've gone off by now", l.event != null);
            assertEquals("Delay timer should've gone off as many times as scheduled", 3, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        final long repeatDelay = interval * 10;

        // Try repeated timer
        t.setDelayTime(repeatDelay);
        t.setRepeat(true);
        try
        {
            timer.scheduleTimerSpec(t);
            timer.scheduleTimerSpec(t);
            timer.scheduleTimerSpec(t);
            assertTrue("Repeated timer scheduled for later went off immediately", l.event == null);
            Thread.sleep(repeatDelay / 2);
            assertTrue("Repeated timer scheduled for later went off too soon", l.event == null);
            Thread.sleep((repeatDelay / 2) + interval * 5);
            assertTrue("Repeated timer scheduled for later should've gone off by now", l.event != null);
            assertEquals("Repeat timer should've gone off as many times as scheduled", 3, l.events.size());
        }
        finally
        {
            timer.deschedule(t);
        }
    }

    /**
     * Tests scheduleTimerSpec() multiple scheduling of other specs.
     */
    public void testScheduleMultiple() throws Exception
    {
        final Object syncObject = new Object();

        class TSpec extends TVTimerSpec implements TVTimerWentOffListener
        {
            int events = 0;

            public TSpec()
            {
                addTVTimerWentOffListener(this);
            }

            public TSpec(boolean absolute, long delay)
            {
                this();
                if (absolute)
                    setAbsoluteTime(delay);
                else
                    setDelayTime(delay);
            }

            public TSpec(long delay, boolean regular)
            {
                this();
                setAbsolute(false);
                setTime(delay);
                setRepeat(true);
                setRegular(regular);
            }

            public void timerWentOff(TVTimerWentOffEvent e)
            {
                synchronized (syncObject)
                {
                    ++events;
                }
            }
        }

        final long delay = interval * 20;

        TSpec specs[] = { new TSpec(true, 0L), new TSpec(false, granularity), new TSpec(delay, false),
                new TSpec(delay, true), null, null, null, null };
        // schedule each twice (we expect that down below)
        specs[4] = specs[0];
        specs[5] = specs[3];
        specs[6] = specs[2];
        specs[7] = specs[1];

        Listener l = new Listener();
        try
        {
            System.gc();
            System.gc();
            // Add listener
            for (int i = 0; i < specs.length; ++i)
                specs[i].addTVTimerWentOffListener(l);

            // Schedule all of them
            for (int i = 0; i < specs.length; ++i)
                timer.scheduleTimerSpec(specs[i]);

            //
            // By the 11th delay period, the 10th event should have be delivered
            //
            Thread.sleep(delay * 11);

            synchronized (syncObject)
            {
                // See if we got as many as we expected...
                for (int i = 0; i < specs.length; ++i)
                {
                    //
                    // The 10th event should have been delivered, but the
                    // 11th event might have also been delivered
                    //
                    int minE = !specs[i].isRepeat() ? 1 : 10;
                    int maxE = !specs[i].isRepeat() ? 1 : 11;
                    assertTrue("Scheduled twice, expected between " + minE + " and " + maxE + " events [" + i
                            + "] but received " + specs[i].events, minE * 2 <= specs[i].events
                            && specs[i].events <= 2 * maxE);
                }

            }
        }
        finally
        {
            // Deschedule all of them
            for (int i = 0; i < specs.length; ++i)
                timer.deschedule(specs[i]);
        }
    }

    /**
     * Tests callback of multiple listeners.
     */
    public void testMultipleListeners() throws Exception
    {
        TVTimerSpec t = new TVTimerSpec();
        class Listener implements TVTimerWentOffListener
        {
            int expected = 0;

            int events = 0;

            public void timerWentOff(TVTimerWentOffEvent evt)
            {
                ++events;
            }
        }
        Listener listeners[] = { new Listener(), new Listener(), new Listener(), null, null, null, null, new Listener() };
        listeners[3] = listeners[1];
        listeners[4] = listeners[0];
        listeners[5] = listeners[2];
        listeners[6] = listeners[1];

        // Add Listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            for (int j = 0; j <= i; ++j)
                listeners[j].events = 0;

            // Add listener
            listeners[i].expected++;
            t.addTVTimerWentOffListener(listeners[i]);

            // notify
            timer.scheduleTimerSpec(t);
            Thread.sleep(10 * (i + 1));
            timer.deschedule(t);
            for (int j = 0; j <= i; ++j)
                assertEquals("Expected listener to be called as many times as added", listeners[j].expected,
                        listeners[j].events);
        }

        // Remove listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            for (int j = 0; j <= i; ++j)
                listeners[j].events = 0;

            // Remove listener
            listeners[i].expected--;
            t.removeTVTimerWentOffListener(listeners[i]);

            // notify
            timer.scheduleTimerSpec(t);
            Thread.sleep(10 * (i + 1));
            timer.deschedule(t);
            for (int j = 0; j <= i; ++j)
                assertEquals("Expected listener to be called as many times as added", listeners[j].expected,
                        listeners[j].events);
        }

        for (int j = 0; j < listeners.length; ++j)
            listeners[j].events = 0;

        // notify
        timer.scheduleTimerSpec(t);
        Thread.sleep(10 * listeners.length);
        timer.deschedule(t);
        for (int j = 0; j < listeners.length; ++j)
            assertEquals("Expected listener to be called as many times as added", listeners[j].expected,
                    listeners[j].events);
    }

    /**
     * Tests deschedule().
     */
    public void testDeschedule() throws Exception
    {
        TVTimerSpec t = new TVTimerSpec();
        Listener l = new Listener();
        t.addTVTimerWentOffListener(l);

        // Deschedule future absolute spec
        t.setAbsoluteTime(System.currentTimeMillis() + granularity * 4);
        try
        {
            timer.scheduleTimerSpec(t);
            Thread.sleep(10);
            timer.deschedule(t);
            Thread.sleep(granularity * 5);

            assertTrue("Descheduled future spec should not have gone off", l.event == null);
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        // Deschedule future delayed spec
        t.setDelayTime(granularity * 4);
        try
        {
            timer.scheduleTimerSpec(t);
            Thread.sleep(10);
            timer.deschedule(t);
            Thread.sleep(granularity * 5);

            assertTrue("Descheduled future spec should not have gone off", l.event == null);
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }

        // Deschedule repeating spec
        t.setDelayTime(interval * 2);
        t.setRepeat(true);
        try
        {
            int expect = 3;
            timer.scheduleTimerSpec(t);
            Thread.sleep(interval * (2 * expect + 1));
            assertTrue("Repeated timer scheduled should've gone off", l.event != null);

            timer.deschedule(t);
            l.clear();
            Thread.sleep(interval * (2 * expect + 1));
            assertTrue("Descheduled retpeat timer should not have gone off", l.event == null);
        }
        finally
        {
            timer.deschedule(t);
            l.clear();
        }
    }

    /**
     * Tests getMinRepeatInterval().
     */
    public void testGetMinRepeatInterval() throws Exception
    {
        long interval = timer.getMinRepeatInterval();

        if (interval == -1) return;

        assertTrue("Invalid interval <-1", interval >= 0);
    }

    /**
     * Tests getGranularity().
     */
    public void testGetGranularity() throws Exception
    {
        long granularity = timer.getGranularity();

        if (granularity == -1) return;

        assertTrue("Invalid granularity <-1", granularity >= 0);
    }

    private class Listener implements TVTimerWentOffListener
    {
        public TVTimerWentOffEvent event;

        public Vector events = new Vector();

        public void clear()
        {
            event = null;
            events.removeAllElements();
        }

        public void timerWentOff(TVTimerWentOffEvent e)
        {
            if (DEBUG) System.out.println("Timer: " + e + ", " + e.getTimerSpec() + ", " + this);
            event = e;
            events.addElement(e);
        }
    }

    public TVTimerTest(String name)
    {
        super(name);
    }

    protected TVTimer timer;

    protected long interval;

    protected long granularity;

    protected Vector specs = new Vector();

    public void setUp() throws Exception
    {
        super.setUp();
        timer = TVTimer.getTimer();

        interval = timer.getMinRepeatInterval();
        if (interval <= 0) interval = 40;

        granularity = timer.getGranularity();
        if (granularity <= 0) granularity = 10;

        // Run GarbageCollection so that it does not interfere with our tests
        System.gc();
        System.gc();
    }

    public void tearDown() throws Exception
    {
        for (Enumeration e = specs.elements(); e.hasMoreElements();)
        {
            timer.deschedule((TVTimerSpec) e.nextElement());
        }
        timer = null;
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
        if (tests == null || tests.length == 0) return suite();

        TestSuite suite = new TestSuite(TVTimerTest.class.getName());
        for (int i = 0; i < tests.length; ++i)
            suite.addTest(new TVTimerTest(tests[i]));
        return suite;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TVTimerTest.class);
        return suite;
    }
}
