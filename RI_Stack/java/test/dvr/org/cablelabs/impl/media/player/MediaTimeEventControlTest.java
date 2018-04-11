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

import java.util.Stack;

import javax.media.Player;
import javax.media.Time;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.davic.media.MediaTimeEvent;
import org.davic.media.MediaTimeEventControl;
import org.davic.media.MediaTimeEventListener;
import org.ocap.media.CannedMediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;

import org.cablelabs.impl.media.JMFBaseInterfaceTest;

public class MediaTimeEventControlTest extends JMFBaseInterfaceTest
{
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001d;

    private Player player;

    private MediaTimeEventControl control;

    protected CannedControllerListener listener;

    protected PlayerHelper helper;

    protected CannedMediaAccessHandler mediaAccessHandler;

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(MediaTimeEventControlTest.class, factory);
    }

    public MediaTimeEventControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public MediaTimeEventControlTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
    }

    //
    // test setup/teardown
    // 

    public void setUp() throws Exception
    {
        super.setUp();

        player = (Player) createImplObject();
        listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        helper = new PlayerHelper(player, listener);
        control = (MediaTimeEventControl) player.getControl("org.davic.media.MediaTimeEventControl");

        mediaAccessHandler = new CannedMediaAccessHandler();
        MediaAccessHandlerRegistrar.getInstance().registerMediaAccessHandler(mediaAccessHandler);

    }

    public void tearDown() throws Exception
    {
        player.close();
        player.deallocate();

        control = null;
        player = null;
        mediaAccessHandler = null;
        helper = null;
        listener = null;

        MediaAccessHandlerRegistrar.getInstance().registerMediaAccessHandler(null);
        super.tearDown();

    }

    //
    // tests
    //

    public void testNotNull()
    {
        assertTrue(control != null);
    }

    public void testNotifyWhenNoID()
    {
        CannedMediaTimeEventListener teListener = new CannedMediaTimeEventListener();
        long time = getNanoSecondsFromMilliseconds(500);
        control.notifyWhen(teListener, time);
        helper.startPlayer();

        boolean received = teListener.waitForMediaTimeEvent(1);
        assertTrue("Did not receive the MediaTimeEvent", received);

        MediaTimeEvent event = teListener.getLastReceivedEvent();
        //
        // since no id was specified in the notifyWhen call, it should
        // be zero
        //
        assertTrue("Incorrect media event id", event.getEventId() == 0);
        assertTrue("incorrect media event time", event.getEventTime() == time);
        // assertTrue("Incorrect media event source",
        // event.getSource().equals(player));
    }

    public void testNotifyWhenWithID()
    {
        CannedMediaTimeEventListener teListener = new CannedMediaTimeEventListener();
        long time = getNanoSecondsFromMilliseconds(500);
        int id = 12345;
        control.notifyWhen(teListener, time, id);
        helper.startPlayer();

        boolean received = teListener.waitForMediaTimeEvent(1);
        assertTrue("Did not receive the MediaTimeEvent", received);

        MediaTimeEvent event = teListener.getLastReceivedEvent();

        assertTrue("Incorrect media event id", event.getEventId() == id);
        assertTrue("incorrect media event time", event.getEventTime() == time);
        // assertTrue("Incorrect media event source",
        // event.getSource().equals(player));
    }

    //
    // start from a nonzero media time that is still before the
    // event time and verify that the event is received
    // 
    public void testStartFromNonzeroMediaTimeBeforeEventTime()
    {
        CannedMediaTimeEventListener teListener = new CannedMediaTimeEventListener();
        //
        // set the event at 7 seconds into the media
        //
        long time = getNanoSecondsFromMilliseconds(7 * 1000);
        //
        // start at 5 seconds
        //
        double startTimeDouble = 5;
        int id = 12345;
        control.notifyWhen(teListener, time, id);
        helper.realizePlayer();
        Time startTime = new Time(startTimeDouble);
        player.setMediaTime(startTime);
        assertTrue(startTime.getNanoseconds() < time);

        listener.waitForMediaTimeSetEvent();

        Time mediaTime = player.getMediaTime();
        assertTrue("Could not set start time to " + startTime + ", is " + mediaTime.getSeconds(), checkDoubleEquality(
                mediaTime.getSeconds(), startTime.getSeconds()));

        player.start();
        listener.waitForMediaPresentationEvent();
        boolean received = teListener.waitForMediaTimeEvent(1);

        player.stop();
        listener.waitForStopEvent();

        assertTrue("Did not receive the MediaTimeEvent", received);
    }

    //
    // start the player from a media time that is past the
    // event time registered, the event should not be received
    //
    public void testStartFromNonzeroMediaTimeAfterEventTime()
    {
        CannedMediaTimeEventListener teListener = new CannedMediaTimeEventListener();
        long time = getNanoSecondsFromMilliseconds(500);
        double startTime = 5000;
        int id = 12345;
        control.notifyWhen(teListener, time, id);
        helper.realizePlayer();
        player.setMediaTime(new Time(startTime));
        listener.waitForMediaTimeSetEvent();

        Time mediaTime = player.getMediaTime();
        assertTrue("Could not set start time to " + startTime + ", is " + mediaTime.getSeconds(), checkDoubleEquality(
                mediaTime.getSeconds(), startTime));

        player.start();
        listener.waitForMediaPresentationEvent();

        boolean received = teListener.waitForMediaTimeEvent(1);
        assertTrue("Should receive the MediaTimeEvent", !received);
    }

    //
    // register the same listener to receive two events, stop in
    // the middle of the two and verify that only one was received
    //
    public void testRegisterMultipleEventsWithOneListener()
    {
        CannedMediaTimeEventListener teListener = new CannedMediaTimeEventListener();
        long time1 = getNanoSecondsFromMilliseconds(500);
        int id1 = 12345;
        control.notifyWhen(teListener, time1, id1);
        //
        // register a second event to be delivered in the distant future
        //
        long time2 = getNanoSecondsFromMilliseconds(100 * 1000);
        int id2 = 55555;
        control.notifyWhen(teListener, time2, id2);
        helper.startPlayer();

        boolean received = teListener.waitForMediaTimeEvent(1);
        helper.stopPlayer();
        Time stoppedMediaTime = player.getMediaTime();

        //
        // double check that we stopped the player before the
        // second event should be delivered
        //
        assertTrue(stoppedMediaTime.getNanoseconds() >= time1);
        assertTrue(stoppedMediaTime.getNanoseconds() <= time2);
        assertTrue("Did not receive the MediaTimeEvent", received);
        assertTrue("Should not have received both events", teListener.getNumberOfEventsReceived() == 1);

        MediaTimeEvent event = teListener.getLastReceivedEvent();
        //
        // verify that the first event was received and not the second
        //
        assertTrue("Incorrect media event id", event.getEventId() == id1);
        assertTrue("incorrect media event time", event.getEventTime() == time1);
    }

    public void testRegisterTwoEventsSameTimeSameListener()
    {
        CannedMediaTimeEventListener teListener = new CannedMediaTimeEventListener();
        long time = getNanoSecondsFromMilliseconds(500);
        int id1 = 12345;
        int id2 = 54321;
        control.notifyWhen(teListener, time, id1);
        control.notifyWhen(teListener, time, id2);

        helper.startPlayer();
        boolean received = teListener.waitForMediaTimeEvent(2);
        assertTrue("Did not receive the correct number of events", received);
    }

    //
    // register two listeners for an event at the same time and
    // verify that they both receive it
    //
    public void testRegisterTwoEventsSameTimeDifferentListeners()
    {
        CannedMediaTimeEventListener teListener1 = new CannedMediaTimeEventListener();
        CannedMediaTimeEventListener teListener2 = new CannedMediaTimeEventListener();

        long time = getNanoSecondsFromMilliseconds(500);
        int id1 = 12345;
        int id2 = 54321;
        control.notifyWhen(teListener1, time, id1);
        control.notifyWhen(teListener2, time, id2);

        helper.startPlayer();
        boolean received1 = teListener1.waitForMediaTimeEvent(1);
        boolean received2 = teListener2.waitForMediaTimeEvent(1);

        assertTrue("Did not receive the correct number of events", received1);
        assertTrue("Did not receive the correct number of events", received2);

        MediaTimeEvent evt1 = teListener1.getLastReceivedEvent();
        assertTrue(evt1.getEventId() == id1);
        MediaTimeEvent evt2 = teListener2.getLastReceivedEvent();
        assertTrue(evt2.getEventId() == id2);
    }

    //
    // set the media start time in the middle some place and register events
    // on both sides, set the rate negative and make sure that the correct
    // event is received
    //
    public void testReceiveEventWithNegativeRate()
    {
        CannedMediaTimeEventListener teListener = new CannedMediaTimeEventListener();
        long startTime = getNanoSecondsFromMilliseconds(5000);
        long timeBefore = startTime - getNanoSecondsFromMilliseconds(500);
        long timeAfter = startTime + getNanoSecondsFromMilliseconds(500);
        float rate = -1f;
        int idBefore = 12345;
        int idAfter = 54321;

        control.notifyWhen(teListener, timeBefore, idBefore);
        control.notifyWhen(teListener, timeAfter, idAfter);
        helper.realizePlayer();
        player.setMediaTime(new Time(startTime));
        listener.waitForMediaTimeSetEvent();

        Time mediaTime = player.getMediaTime();
        assertTrue("Could not set start time to " + startTime + ", is " + mediaTime.getNanoseconds(),
                mediaTime.getNanoseconds() == startTime);

        float returnedRate = player.setRate(rate);
        assertTrue("Could not set player rate", rate == returnedRate);

        player.start();
        listener.waitForMediaPresentationEvent();

        //
        // verify that we are progressing backwards and that the
        // stop time is before the earlier event time
        //
        assertTrue("Player rate is not negative - " + player.getRate(), player.getRate() < 0);
        boolean received = teListener.waitForMediaTimeEvent(1);
        player.stop();
        Time stoppedMediaTime = player.getMediaTime();

        assertTrue("Stopped before alarm time (negative rate), stopped at " + stoppedMediaTime.getNanoseconds()
                + " alarm - " + timeBefore + " rate - " + player.getRate(),
                stoppedMediaTime.getNanoseconds() <= timeBefore);
        assertTrue("Should receive the MediaTimeEvent", received);

        MediaTimeEvent evt = teListener.getLastReceivedEvent();

        assertTrue("Did not receive the correct media event", evt.getEventId() == idBefore);
    }

    private long getNanoSecondsFromMilliseconds(long ms)
    {
        return ms * Time.ONE_SECOND / 1000;
    }

    private boolean checkDoubleEquality(double d1, double d2)
    {
        return Math.abs(d1 - d2) < DOUBLE_COMPARISON_DELTA;
    }

    private static class CannedMediaTimeEventListener implements MediaTimeEventListener
    {
        private static final long TIMEOUT = 8000;

        private Object syncObject = new Object();

        private Stack eventStack = new Stack();

        public void receiveMediaTimeEvent(MediaTimeEvent e)
        {
            synchronized (syncObject)
            {
                eventStack.push(e);
                syncObject.notifyAll();
            }
        }

        public boolean waitForMediaTimeEvent(int size)
        {
            synchronized (syncObject)
            {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < TIMEOUT && eventStack.size() < size)
                {
                    try
                    {
                        syncObject.wait(TIMEOUT / 10);
                    }
                    catch (InterruptedException exc)
                    {
                        // just fall through and finish waiting
                    }
                }
            }

            //
            // return true if we have received the number of events
            // we were expecting
            //
            return eventStack.size() == size;
        }

        public MediaTimeEvent getLastReceivedEvent()
        {
            if (eventStack.size() > 0)
            {
                return (MediaTimeEvent) eventStack.peek();
            }
            else
            {
                return null;
            }
        }

        public int getNumberOfEventsReceived()
        {
            return eventStack.size();
        }
    }
}
