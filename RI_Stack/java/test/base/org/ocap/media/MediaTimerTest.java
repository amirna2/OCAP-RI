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
package org.ocap.media;

import java.util.Vector;

import javax.media.Player;
import javax.media.Time;

import junit.framework.TestCase;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;
import org.cablelabs.impl.media.player.CannedBroadcastPlayer;
import org.cablelabs.impl.media.player.CannedControllerListener;
import org.cablelabs.impl.media.player.CannedPlayerBase;
import org.cablelabs.impl.media.player.PlayerHelper;
import org.cablelabs.impl.media.protocol.ocap.DataSource;
import org.cablelabs.impl.media.source.CannedOcapServiceDataSource;

/**
 * MediaTimerTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class MediaTimerTest extends TestCase
{
    CannedServiceMgr csm;

    MediaTimer timer;

    CannedMediaTimerListener listener;

    CannedControllerListener cclistener;

    Player player;

    ServiceManager oldSM;

    PlayerHelper helper;

    /**
	 * 
	 */
    public MediaTimerTest()
    {
        this("MediaTimerTest");
    }

    /**
     * @param name
     */
    public MediaTimerTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(MediaTimerTest.class);
        System.exit(0);
    }

    // Test Setup

    public void setUp() throws Exception
    {
        super.setUp();

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

        player = new CannedPlayerBase();
        cclistener = new CannedControllerListener(1);

        player.addControllerListener(cclistener);
        player.setSource(new CannedOcapServiceDataSource());
        helper = new PlayerHelper(player, cclistener);
        listener = new CannedMediaTimerListener();
        timer = new MediaTimer(player, listener);
    }

    public void tearDown() throws Exception
    {
        csm.destroy();
        player.close();
        timer.stop();

        timer = null;
        listener = null;
        player = null;
        cclistener = null;
        helper = null;

        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);

        super.tearDown();
    }

    // Test section

    public void testSetAndGetFirstTime()
    {
        Time ft = new Time(1000L);
        timer.setFirstTime(ft);
        assertEquals("Returned Time is incorrect", ft.getNanoseconds(), timer.getFirstTime().getNanoseconds());
    }

    public void testSetAndGetLastTime()
    {
        Time lt = new Time(1000L);
        timer.setLastTime(lt);
        assertEquals("Returned Time is incorrect", lt.getNanoseconds(), timer.getLastTime().getNanoseconds());
    }

    public void testStartTimerFirst()
    {
        // Start the timer.
        timer.start();
        // Wait for a potential event
        timerCheck(MediaTimerListener.TIMER_START);

        listener.reset();
        // Start the player
        helper.startPlayer();
        // Make sure we don't get another event
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 0, listener.events.size());
        assertEquals("Player count is incorrect", 0, listener.players.size());
    }

    public void testStartTimerSecond()
    {
        // Start the player
        helper.startPlayer();
        // Start timer
        timer.start();
        timerCheck(MediaTimerListener.TIMER_START);
    }

    public void testStopTimerThruPlayer()
    {
        // Start player and timer
        helper.startPlayer();
        timer.start();
        timerCheck(MediaTimerListener.TIMER_START);

        listener.reset();
        // Stop the player
        helper.stopPlayer();

        // The timer should not stop when the player does
        listener.waitForEvents(1);
        assertEquals("Event count is incorrect", 0, listener.events.size());
        assertEquals("Player count is incorrect", 0, listener.players.size());
    }

    public void testStopTimerThruTimer()
    {
        // Start player and timer
        helper.startPlayer();
        timer.start();
        timerCheck(MediaTimerListener.TIMER_START);

        // Stop the timer and check the event
        listener.reset();
        timer.stop();
        timerCheck(MediaTimerListener.TIMER_STOP);
    }

    public void testReceiveFirstEvent() throws Exception
    {
        player.realize();
        cclistener.waitForRealizeCompleteEvent();

        player.setMediaTime(new Time(30000000000L)); // 30 seconds
        timer.setFirstTime(new Time(29000000000L)); // 29 seconds
        timer.start();
        timerCheck(MediaTimerListener.TIMER_START);
        player.start();
        cclistener.waitForMediaPresentationEvent();
        assertEquals("Player is not started", Player.Started, player.getState());

        listener.reset();
        // Timer successfully started, let's set the rate to -1.0f
        player.setRate(-1.0f);
        // Now that the clock is running backwards, we'll wait a few
        // seconds until our expected time is reached and our
        // listener is notified
        synchronized (this)
        {
            wait(8000);
        }
        timerCheck(MediaTimerListener.TIMER_WENTOFF_FIRST);

        // TODO: Should we test the accuracy of the event arrival?
        // If so, what values should we use?
    }

    public void testReceiveLastEvent()
    {
        // We'll set the timer to go off 1 second into the playback
        timer.setLastTime(new Time(1000000000L)); // 1 second
        timer.start();
        timerCheck(MediaTimerListener.TIMER_START);
        listener.reset();
        helper.startPlayer();

        timerCheck(MediaTimerListener.TIMER_WENTOFF_LAST);

        // TODO: Should we test the accuracy of the event arrival?
        // If so, what values should we use?
    }

    public void testGetPlayer()
    {
        assertSame("Returned Player is incorrect", player, timer.getPlayer());
    }

    // Test Support section

    private void timerCheck(int event)
    {
        // Wait for the events
        listener.waitForEvents(1);

        assertEquals("Event count is incorrect", 1, listener.events.size());
        assertEquals("Player count is incorrect", 1, listener.players.size());

        assertEquals("Event value is incorrect", event, listener.getEvent(0));
        assertSame("Player is incorrect", player, listener.getPlayer(0));
    }

    private class CannedMediaTimerListener implements MediaTimerListener
    {
        public Vector events = new Vector();

        public Vector players = new Vector();

        public long totalWaitTime = 0L;

        public int getEvent(int index)
        {
            return ((Integer) events.get(index)).intValue();
        }

        public Player getPlayer(int index)
        {
            return (Player) players.get(index);
        }

        public void notify(int event, Player p)
        {
            synchronized (events)
            {
                events.add(new Integer(event));
                players.add(p);
                events.notify();
            }
        }

        public void waitForEvents(int count)
        {
            long startTime = System.currentTimeMillis();

            //
            // wait until we get the correct number of events or until
            // we get tired of waiting
            // 
            while (events.size() < count && System.currentTimeMillis() < startTime + 12000)
            {
                Thread.yield();
                synchronized (events)
                {
                    try
                    {
                        events.wait(8000 / 20);
                    }
                    catch (InterruptedException exc)
                    {
                        //
                        // if we got interrupted, break out of the loop and
                        // return
                        //
                        exc.printStackTrace();
                        return;
                    }
                }

            }
            totalWaitTime = System.currentTimeMillis() - startTime;
        }

        public void reset()
        {
            synchronized (events)
            {
                events.removeAllElements();
                players.removeAllElements();
            }
        }
    }
}
