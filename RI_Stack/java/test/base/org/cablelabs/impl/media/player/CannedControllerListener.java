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

import java.util.Vector;

import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.MediaTimeSetEvent;
import javax.media.PrefetchCompleteEvent;
import javax.media.RateChangeEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.StopTimeChangeEvent;

import org.davic.media.MediaPresentedEvent;
import org.davic.media.MediaTimePositionChangedEvent;
import org.ocap.media.AlternativeMediaPresentationEvent;
import org.ocap.media.MediaPresentationEvent;
import org.ocap.media.NormalMediaPresentationEvent;

public class CannedControllerListener implements ControllerListener
{
    protected final static int WAIT_TIME = 2000;

    int id;

    volatile Vector events;

    public CannedControllerListener(int id)
    {
        this.id = id;
        events = new Vector();
    }

    public void controllerUpdate(ControllerEvent ce)
    {
        synchronized (events)
        {
            events.add(ce);
            events.notifyAll();
        }
    }

    public ControllerEvent getEvent(int index)
    {
        if (index < events.size())
        {
            return (ControllerEvent) events.get(index);
        }
        else
        {
            return null;
        }
    }

    public void reset()
    {
        synchronized (events)
        {
            events.removeAllElements();
        }
    }

    public String toString()
    {
        return "DummyControllerListener { id=" + id + ", event=" + events + " }";
    }

    public ControllerEvent findEventOfClass(Class eventClass)
    {
        ControllerEvent targetEvent = null;
        for (int i = 0; i < events.size(); i++)
        {
            ControllerEvent evt = (ControllerEvent) events.get(i);
            if (eventClass.isAssignableFrom(evt.getClass()))
            {
                targetEvent = evt;
                break;
            }
        }
        return targetEvent;
    }

    private boolean eventOfClassFound(Class expectedEventClass)
    {
        return findEventOfClass(expectedEventClass) != null;
    }

    protected boolean waitForEvent(Class expectedEventClass, long wait)
    {
        long startTime = System.currentTimeMillis();
        synchronized (events)
        {
            while (!eventOfClassFound(expectedEventClass) && System.currentTimeMillis() < startTime + wait)
            {
                try
                {
                    events.wait(wait / 20);
                }
                catch (InterruptedException exc)
                {
                    break;
                }
            }
        }

        return eventOfClassFound(expectedEventClass);
    }

    public boolean waitForStartEvent()
    {
        return waitForEvent(StartEvent.class, WAIT_TIME);
    }

    public boolean waitForMediaPresentedEvent()
    {
        return waitForEvent(MediaPresentedEvent.class, WAIT_TIME);
    }

    public boolean waitForMediaPresentationEvent()
    {
        return waitForEvent(MediaPresentationEvent.class, WAIT_TIME);
    }

    public boolean waitForNormalMediaPresentationEvent()
    {
        return waitForEvent(NormalMediaPresentationEvent.class, WAIT_TIME);
    }

    public boolean waitForAlternativeMediaPresentationEvent()
    {
        return waitForEvent(AlternativeMediaPresentationEvent.class, WAIT_TIME);
    }

    public boolean waitForStopEvent(long waitMS)
    {
        return waitForEvent(StopEvent.class, waitMS);
    }

    public boolean waitForStopEvent()
    {
        return waitForStopEvent(WAIT_TIME);
    }

    public boolean waitForPrefetchCompleteEvent()
    {
        return waitForEvent(PrefetchCompleteEvent.class, WAIT_TIME);
    }

    public boolean waitForRealizeCompleteEvent()
    {
        return waitForEvent(RealizeCompleteEvent.class, WAIT_TIME);
    }

    public boolean waitForMediaTimePositionChangedEvent()
    {
        return waitForEvent(MediaTimePositionChangedEvent.class, WAIT_TIME);
    }

    public boolean waitForMediaTimeSetEvent()
    {
        return waitForEvent(MediaTimeSetEvent.class, WAIT_TIME);
    }

    public boolean waitForRateChangeEvent()
    {
        return waitForEvent(RateChangeEvent.class, WAIT_TIME);
    }

    public boolean waitForStopTimeChangeEvent()
    {
        return waitForEvent(StopTimeChangeEvent.class, WAIT_TIME);
    }

    public boolean waitForControllerClosedEvent()
    {
        return waitForEvent(ControllerClosedEvent.class, WAIT_TIME);
    }

    public void waitForEvents(int count, long waitMS)
    {
        /*
         * try { Thread.sleep(WAIT_TIME); } catch(InterruptedException exc) { }
         */

        long startTime = System.currentTimeMillis();

        //
        // wait until we get the correct number of events or until
        // we get tired of waiting
        // 
        while (events.size() < count && System.currentTimeMillis() < startTime + waitMS)
        {
            Thread.yield();
            synchronized (events)
            {
                try
                {
                    events.wait(waitMS / 20);
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
    }

    public void waitForEvents(int count)
    {
        waitForEvents(count, WAIT_TIME);
    }
}
