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

import javax.media.Controller;
import javax.media.Player;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;

import junit.framework.Assert;

public class PlayerHelper
{
    private Player player;

    private CannedControllerListener listener;

    public PlayerHelper(Player p, CannedControllerListener l)
    {
        player = p;
        listener = l;
    }

    /**
     * Transition a player from the unrealized state to the realized state
     */
    public void realizePlayer()
    {
        Assert.assertTrue(player.getState() == Controller.Unrealized);
        player.realize();
        listener.waitForRealizeCompleteEvent();
        Assert.assertTrue(player.getState() == Controller.Realized);
    }

    /**
     * Starts a prefetched player
     */
    public void callSyncStartWithNoWait()
    {
        Assert.assertTrue("Initial Player State is not prefetched -- " + player.getState(),
                player.getState() == Controller.Prefetched);

        listener.reset();
        player.syncStart(new Time(0));
        listener.waitForStartEvent();
        // Assert.assertTrue("Did not receive the correct StartEvent " +
        // listener.getEvent(0),
        // listener.getEvent(0) instanceof StartEvent);
        Assert.assertTrue("Player State is not Started -- " + player.getState(),
                player.getState() == Controller.Started);
    }

    /**
     * Transition a player from the unrealized state to the prefetched state
     */
    public void prefetchPlayer()
    {
        Assert.assertTrue(player.getState() == Controller.Unrealized);
        player.prefetch();
        listener.waitForPrefetchCompleteEvent();
        // Assert.assertTrue("After prefetch, player state was " +
        // player.getState(),
        // player.getState() == Controller.Prefetched);
    }

    /**
     * Stops a started player
     */
    public void stopPlayer()
    {
        Assert.assertTrue("Initial Player State is not Started -- " + player.getState(),
                player.getState() == Controller.Started);
        listener.reset();
        player.stop();
        listener.waitForEvents(1);
        // assertTrue("Player state is not prefetched -- " + player.getState(),
        // player.getState() == Controller.Prefetched);
        Assert.assertTrue("Did not receive the expected StopEvent " + listener.getEvent(0),
                listener.getEvent(0) instanceof StopEvent);
    }

    /**
     * Starts an unrealized player
     */
    public void startPlayer()
    {
        player.start();
        listener.waitForMediaPresentationEvent();
        // Assert.assertTrue("Did not receive the correct StartEvent " +
        // listener.getEvent(4),
        // listener.getEvent(4) instanceof StartEvent);
        Assert.assertTrue("Player State is not Started", player.getState() == Controller.Started);
    }

    public void setMediaTime(double mediaTime)
    {
        listener.reset();
        player.setMediaTime(new Time(mediaTime));
        boolean mtChanged = listener.waitForMediaTimePositionChangedEvent();
        boolean mtSet = listener.waitForMediaTimeSetEvent();
        Assert.assertTrue(mtChanged || mtSet);
    }

    public void setStopTime(double stopMediaTime)
    {
        listener.reset();
        player.setStopTime(new Time(stopMediaTime));
        boolean stopTimeChangeEventReceived = listener.waitForStopTimeChangeEvent();
        Assert.assertTrue(stopTimeChangeEventReceived);
    }

    public void setRate(float rate)
    {
        listener.reset();
        player.setRate(rate);
        boolean rateChangeEventReceived = listener.waitForRateChangeEvent();
        Assert.assertTrue(rateChangeEventReceived);
    }

}
