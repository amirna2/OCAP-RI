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

import javax.media.Time;

import org.cablelabs.impl.media.presentation.AbstractPresentation;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.presentation.PresentationContext;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

/**
 * CannedPlayerBase
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedPlayerBase extends AbstractPlayer
{

    protected ControlBase[] controlsArray;

    protected CannedControlBase control1;

    protected CannedControlBase control2;

    protected CannedComponent component;

    protected Time time;

    protected float rate;

    protected boolean waiting = false;

    protected Object waitingLockObject = new Object();

    protected boolean throwStartDecodingException = false;

    protected int startDecodingExceptionReason;

    private boolean notified;

    /**
     * 
     */
    public CannedPlayerBase()
    {
        super(null, new Object(), new ResourceUsageImpl(null));

        control1 = new CannedControlBase();
        control1.setEnabled(true);
        control2 = new CannedControlBase();
        controlsArray = new ControlBase[] { control1, control2 };

        component = new CannedComponent();
        cannedLockObject = new Object();
        time = new Time(0);

        cannedFailAcquirePrefetchResources = false;
        cannedFailAcquireRealizeResources = false;
        cannedStallAcquirePrefetchResources = false;
        cannedStallAcquireRealizeResources = false;
        notified = false;

        addControls(controlsArray);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.PlayerBase#doGetVisualComponent()
     */
    protected Component doGetVisualComponent()
    {
        return component;
    }

    protected Object doAcquireRealizeResources()
    {
        if (cannedFailAcquireRealizeResources) return new Error("canned failure");
        if (cannedStallAcquireRealizeResources)
        {
            try
            {
                synchronized (cannedLockObject)
                {
                    if (!notified)
                    {
                        synchronized (waitingLockObject)
                        {
                            waiting = true;
                        }
                        cannedLockObject.wait();

                        synchronized (waitingLockObject)
                        {
                            waiting = false;
                        }
                    }
                }
            }
            catch (InterruptedException ex)
            {
            }
        }
        return null;
    }

    protected void doReleaseRealizedResources()
    {

    }

    protected Object doAcquirePrefetchResources()
    {
        if (cannedFailAcquirePrefetchResources) return new Error("canned failure");
        if (cannedStallAcquirePrefetchResources)
        {
            try
            {
                synchronized (cannedLockObject)
                {
                    if (!notified)
                    {
                        waiting = true;
                        cannedLockObject.wait();
                        waiting = false;
                    }
                }
            }
            catch (InterruptedException ex)
            {
            }
        }
        return null;
    }

    protected void doReleasePrefetchedResources()
    {

    }

    public void cannedSetFailAcquireRealizeResources(boolean value)
    {
        cannedFailAcquireRealizeResources = value;
    }

    public void cannedSetFailAcquirePrefetchResources(boolean value)
    {
        cannedFailAcquirePrefetchResources = value;
    }

    public void cannedSetStallAcquireRealizeResources(boolean value)
    {
        cannedStallAcquireRealizeResources = value;
    }

    public void cannedSetStallAcquirePrefetchResources(boolean value)
    {
        cannedStallAcquirePrefetchResources = value;
    }

    public Object cannedGetLockObject()
    {
        return cannedLockObject;
    }

    public void cannedNotify()
    {
        notified = true;
        synchronized (cannedLockObject)
        {
            cannedLockObject.notify();
        }
    }

    public void stallUntilWaitingForResource()
    {
        //
        // the trick here is that we want the player to be waiting for the
        // locked object
        //
        while (true)
        {
            synchronized (waitingLockObject)
            {
                if (waiting)
                {
                    break;
                }
            }
            Thread.yield();
        }
    }

    private boolean cannedFailAcquireRealizeResources;

    private boolean cannedFailAcquirePrefetchResources;

    private boolean cannedStallAcquireRealizeResources;

    private boolean cannedStallAcquirePrefetchResources;

    private Object cannedLockObject;

    public boolean getMute()
    {
        return false;
    }

    public float getGain()
    {
        return 0.0F;
    }

    public class CannedControlBase extends ControlBase
    {

    }

    private class CannedComponent extends Component
    {

    }

    protected void doReleaseAllResources()
    {
        // TODO Auto-generated method stub
    }

    protected Presentation createPresentation()
    {
        return new CannedPresentation(this);
    }

    private class CannedPresentation extends AbstractPresentation
    {

        public CannedPresentation(PresentationContext pc)
        {
            super(pc);
        }

        protected void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent)
        {
            time = mt;
        }

        protected float doSetRate(float newRate)
        {
            return rate = newRate;
        }

        protected void doStart()
        {
        }

        protected void doStopInternal(boolean shuttingDown)
        {
        }

        protected void doStop(boolean shuttingDown)
        {
        }

        public Time doGetMediaTime()
        {
            return time;
        }

    }

}
