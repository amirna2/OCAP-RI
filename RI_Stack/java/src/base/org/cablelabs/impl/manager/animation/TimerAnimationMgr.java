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

package org.cablelabs.impl.manager.animation;

import java.util.Date;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.cablelabs.impl.havi.AnimationContext;
import org.cablelabs.impl.manager.AnimationManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.timer.TimerSpecExt;
import org.ocap.system.event.SystemEventManager;

/**
 * Implementation of an <code>AnimationManager</code> based on a
 * <code>TVTimer</code>. All animations run on a base delay unit of 100
 * milliseconds.
 * 
 * @see AnimationManager
 * @see AnimationContext
 */
public class TimerAnimationMgr extends AnimationMgr implements TVTimerWentOffListener
{
    private java.util.Hashtable anims = new java.util.Hashtable();

    private boolean done = false;

    /**
     * Returns an instance of this <code>AnimationManager</code>. Intended to be
     * called by the {@link org.cablelabs.impl.manager.ManagerManager
     * ManagerManager} only and not called directly. The singleton instance of
     * this manager is maintained by the <code>ManagerManager</code>.
     * 
     * @return an instance of the <code>AnimationManager</code>.
     * 
     * @see org.cablelabs.impl.manager.ManagerManager#getInstance(Class)
     */
    public static synchronized Manager getInstance()
    {
        return new TimerAnimationMgr();
    }

    // Description copied from ManagerManager
    public void destroy()
    {
        // Nothing to cleanup; all animations would
        // have been released during application destruction.
    }

    // Comments copied from AnimationManager
    public synchronized boolean isAnimated(AnimationContext context)
    {
        if (context != null)
        {
            AnimationTimerSpec ats = (AnimationTimerSpec) anims.get(context.getAnimation());

            if (ats != null) return (ats.getContext() == context);
        }
        return false;
    }

    // Comments copied from AnimationManager
    public synchronized void start(AnimationContext context)
    {
        if (context != null)
        {
            context.wait = context.getDelay();
            final AnimationTimerSpec spec = new AnimationTimerSpec(context);

            // Associate the animation with its timer spec.
            anims.put(context.getAnimation(), spec);

            spec.addTVTimerWentOffListener(this);
            spec.start(delayUnit);
        }
    }

    // Comments copied from AnimationManager
    public synchronized void stop(AnimationContext context)
    {
        if (context != null)
        {
            Object a = context.getAnimation();
            AnimationTimerSpec spec = (AnimationTimerSpec) anims.get(a);

            if (spec != null)
            {
                if (spec.getContext() == context)
                {
                    spec.stop();
                    anims.remove(a);
                }
            }
        }
    }

    // Comments copied from AnimationManager
    public synchronized void timerWentOff(TVTimerWentOffEvent e)
    {
        if (!done && (isPaused() == false))
        {
            TVTimerSpec retSpec = e.getTimerSpec();
            AnimationTimerSpec timerSpec = (AnimationTimerSpec) retSpec;

            if (timerSpec.delayElapsed() == false)
            {
                return;
            }

            // Get the AnimationContext from the specialized TimerSpec.
            final AnimationContext aContext = timerSpec.getContext();

            // Make sure we have a CallerContext
            if ((aContext != null) && (aContext.getCallerContext() != null))
            {
                Runnable run = new Runnable()
                {
                    AnimationContext anim = aContext;

                    public void run()
                    {
                        // Is it animated?
                        if (!anim.isAnimated())
                            stop(anim);
                        // Should it advance yet?
                        else if (--anim.wait == 0)
                        {
                            // Reset delay counter
                            anim.wait = anim.getDelay();
                            // Advance position (and remove when done)
                            if (anim.advancePosition()) stop(anim);
                        }
                    }
                };
                aContext.getCallerContext().runInContext(run);
            }
        }
    }

    /**
     * This class is used by the <code>TimerAnimationMgr</code> to specify the
     * parameters for a <code>TVTimer</code>.
     */
    class AnimationTimerSpec extends TVTimerSpec implements CallbackData
    {
        private AnimationContext context;

        private TVTimer timer = null;

        private boolean running = false;

        private long lastTime = 0;

        private long delay = 0;

        TVTimerSpec scheduledTimerSpec = new TVTimerSpec();

        public AnimationTimerSpec(AnimationContext context)
        {
            this.context = context;
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            // Add application's animation timer spec to app's context
            CallerContext ctx = ccm.getCurrentContext();
            ctx.addCallbackData(this, context);
        }

        /**
         * Returns the <code>AnimationContext</code> associated with the timer.
         * 
         * @return Returns the <code>AnimationContext</code>.
         */
        public AnimationContext getContext()
        {
            return context;
        }

        /**
         * Starts the timer using the delay specified.
         * 
         * @param delay
         *            the amount of time (in milliseconds) between timer events
         */
        public void start(long delay)
        {
            if ((delay != 0) && (running == false))
            {
                if (timer == null) timer = TVTimer.getTimer();

                this.delay = delay;
                setAbsolute(false);
                setRegular(true);
                setTime(delay);
                setRepeat(true);

                try
                {
                    scheduledTimerSpec = timer.scheduleTimerSpec(this);
                    // Made it this far. We must be running.
                    running = true;
                }
                catch (TVTimerScheduleFailedException e)
                {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Stops the timer should it currently be running.
         * 
         */
        public void stop()
        {
            if (running == true)
            {
                timer.deschedule(scheduledTimerSpec);
                running = false;
            }
        }

        /**
         * Tests to see if the time elapsed is greater than or equal to the
         * delay. This is required to throttle the timer events following a
         * system wide delay such as a GC.
         * 
         * @return <code>true</code> if the delay has elapsed,
         *         <code>false</code> otherwise.
         */
        public boolean delayElapsed()
        {
            // Let's be optimistic
            boolean canProceed = true;
            // Get the current time in terms of milliseconds since 1970
            long currTime = new Date().getTime();

            // If we have a last time, then we need to make sure that the
            // difference between the last time and current time is greater
            // than or equal to our delay. This is to prevent the manager
            // from trying to "catch up".
            // Fix for bug 1614: the elapsed delay is frequently slightly
            // less than the requested delay, so changed the criteria to be
            // that 90% of the requested delay has elapsed.
            if (lastTime != 0)
            {
                canProceed = ((currTime - lastTime) >= (long) (0.9 * delay)) ? true : false;
            }

            // Save the current time for a future check
            lastTime = currTime;

            return canProceed;
        }

        // Methods for CallbackData interface:

        // On application destruction make sure to remove
        // any animation and animation timer spec association.
        public void destroy(CallerContext callerContext)
        {
            callerContext.removeCallbackData(getContext());
            stop(); // Make sure it's stopped (should be).

            // Remove animation from hashtable.
            anims.remove(getContext().getAnimation());
        }

        public void pause(CallerContext callerContext)
        {
        }

        public void active(CallerContext callerContext)
        {
        }
    }
}
