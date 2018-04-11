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

package org.cablelabs.gear.util;

/**
 * <code>AnimationManager</code> is an abstract class which defines an API for
 * managing the playback of <i>animated</i> components.
 * 
 * @see org.cablelabs.gear.util.AnimationManager.AnimationContext
 * @see org.cablelabs.gear.util.AnimationManager.SingleThread
 * 
 * @author Aaron Kamienski
 * @version $Id: AnimationManager.java,v 1.2 2002/06/03 21:31:06 aaronk Exp $
 */
public abstract class AnimationManager
{
    /**
     * Not publicly instantiable.
     */
    protected AnimationManager()
    {
        // empty
    }

    /**
     * Starts playback of the <i>animation</i> represented by
     * <code>context</code>. The
     * {@link AnimationManager.AnimationContext#advancePosition()} method will
     * be called at intervals appropriate to the animation.
     * 
     * @param context
     *            represents the animation to start animating; if
     *            <code>null</code> the method should act as if it weren't
     *            called
     */
    public abstract void start(AnimationContext context);

    /**
     * Stops playback of the <i>animation</i> represented by
     * <code>context</code>. The {@link AnimationContext#advancePosition()}
     * method will not be called until
     * {@link #start(AnimationManager.AnimationContext)} is called again.
     * 
     * @param context
     *            represents the animation to stop animating; if
     *            <code>null</code> the method should act as if it weren't
     *            called
     */
    public abstract void stop(AnimationContext context);

    /**
     * Queries whether the <code>Animation</code> manager believes the
     * <i>animation</i> represented by <code>context</code> is currently
     * <i>playing</i>.
     * 
     * @param context
     *            represents the <i>animation</i> in question; if
     *            <code>null</code> then <code>false</code> will be returned
     * @return <code>true</code> if the <i>animation</i> is currently being
     *         animated by this <code>AnimationManager</code>;
     *         <code>false</code> otherwise
     */
    public abstract boolean isAnimated(AnimationContext context);

    /**
     * Used to halt all current and future animations.
     */
    public abstract void shutdown();

    /**
     * The paused flag.
     */
    private boolean paused = false;

    /**
     * Used to pause/restart <i>all</i> animations managed by this
     * <code>AnimationManager</code>.
     * 
     * @param paused
     *            if <code>true</code> then all animations are paused; if
     *            <code>false</code> then animation is resumed
     */
    public void setPaused(boolean paused)
    {
        this.paused = paused;
    }

    /**
     * Returns whether animation is currently paused or not.
     * 
     * @return <code>true</code> if all animation is currently paused
     */
    public boolean isPaused()
    {
        return paused;
    }

    /**
     * The <code>AnimationContext</code> class is used to represent an
     * <i>animation</i> implementation. It is the interface through which the
     * <code>AnimationManager</code> controls the animation. The
     * {@link AnimationManager.AnimationContext#advancePosition()} method is
     * used by the <code>AnimationManager</code> to advance the current frame
     * position of the <i>animation</i> .
     * 
     * @see AnimationManager
     */
    public static abstract class AnimationContext
    {
        /**
         * Countdown variable used to keep track of how many time periods before
         * next position advancement. Basically a scratch area for
         * AnimationManager implementation to work.
         */
        public int wait = 0;

        /**
         * Returns the <code>Object</code> which uniquely represents the
         * animation in question. Generally, this will be the <i>animation</i>
         * object.
         * 
         * @return the animation <code>Object</code> which this
         *         <code>AnimationContext</code> represents.
         */
        public abstract Object getAnimation();

        /**
         * Returns the <i>delay</i> between frames of the animation. This should
         * be specified in units of 100 milliseconds. For example, a delay of 2
         * means 200 milliseconds.
         * 
         * @return the <i>delay</i> in milliseconds divided by 100
         */
        public abstract int getDelay();

        /**
         * Advances the represented <i>animation</i> by one frame position
         * according to the current playback mode.
         * 
         * @return <code>true</code> if the advanced to frame is the last frame
         *         to play
         */
        public abstract boolean advancePosition();

        /**
         * Used to determine if this animation context is the current one and
         * should be used.
         * 
         * @return <code>true</code> if this <code>AnimationContext</code> is
         *         current and should be used
         */
        public abstract boolean isAnimated();
    }

    /**
     * Simple implementation of an <code>AnimationManager</code> based on a
     * single <code>Thread</code> shared amongst all running animations.
     * 
     * @see AnimationManager
     * @see AnimationManager.AnimationContext
     */
    public static class SingleThread extends AnimationManager implements Runnable
    {
        private final int delayUnit;

        private java.util.Hashtable anims = new java.util.Hashtable();

        private boolean done = false;

        private Thread thread;

        private static final int PRIOR_INCR = 2;

        /**
         * Creates a singleton thread for this <code>AnimationManager</code>
         * with a base delay unit of <code>100</code> milliseconds (as required
         * by HAVi). This results in a maximum framerate of 10 fps.
         */
        public SingleThread()
        {
            this(100);
        }

        /**
         * Creates a singleton thread for this <code>AnimationManager</code>
         * with a base delay unit of <code>delayUnit</code> milliseconds.
         * 
         * @param delayUnit
         *            base delay unit in milliseconds
         */
        public SingleThread(int delayUnit)
        {
            this.delayUnit = delayUnit;
            thread = new Thread(this, toString());
            int newPriority = thread.getPriority() + PRIOR_INCR;
            thread.setPriority(newPriority <= Thread.MAX_PRIORITY ? newPriority : Thread.MAX_PRIORITY);
            thread.start();
        }

        // Comments copied from AnimationManager
        public synchronized void start(AnimationContext context)
        {
            if (context != null)
            {
                context.wait = context.getDelay();
                anims.put(context.getAnimation(), context);
                notifyAll();
            }
        }

        // Comments copied from AnimationManager
        public synchronized void stop(AnimationContext context)
        {
            Object a;
            if (context != null && anims.get(a = context.getAnimation()) == context)
            {
                anims.remove(a);
            }
        }

        // Comments copied from AnimationManager
        public synchronized boolean isAnimated(AnimationContext context)
        {
            Object a;
            return context != null && anims.get(a = context.getAnimation()) == context;
        }

        // Comments copied from AnimationManager
        public synchronized void shutdown()
        {
            done = true;
            thread.interrupt();
        }

        // Comments copied from AnimationManager
        public synchronized void setPaused(boolean paused)
        {
            super.setPaused(paused);
            notifyAll();
        }

        /**
         * Implements <code>Runnable.run()</code>. This method should not be
         * called directly.
         * 
         * <p>
         * 
         * This method loops infinitely (or at least until {@link #shutdown()}
         * is called). During each iteration of the loop, the following occurs:
         * 
         * <ol>
         * <li>Sleep for <i>delayUnit</i> ms.
         * <li>Advance all managed animations (which may result in animations
         * stopping themselves after completing a run).
         * <li>Take care of animation management bookkeeping changes resulting
         * from animation stopping themselves.
         * </ol>
         * After <code>shutdown()</code>, all currently managed and running
         * animations are stopped and <i>unmanaged</i>.
         * 
         * @see AnimationManager.SingleThread#doAdvance()
         * @see AnimationManager.SingleThread#stopAll()
         */
        public void run()
        {
            while (!done)
            {
                try
                {
                    long time;

                    // Wait for available animations
                    waitForAnims();

                    // Sleep for base delay unit
                    if (DEBUG) time = System.currentTimeMillis();
                    Thread.sleep(delayUnit); // minimum sleep time
                    if (DEBUG)
                    {
                        time = System.currentTimeMillis() - time;
                        if (time > (delayUnit + 15)) System.err.println(time + " > " + delayUnit);
                    }

                    // Advance animations as appropriate
                    doAdvance();
                }
                catch (InterruptedException e)
                {
                    continue;
                }
            }
            if (DEBUG) System.err.println("Animation Manager - Exiting");

            stopAll();
        }

        /**
         * Waits for animations to be added to the set of currently managed
         * animations.
         * 
         * @throws InterruptedException
         *             if the blocking <code>wait</code> is interrupted.
         */
        protected synchronized void waitForAnims() throws InterruptedException
        {
            while (anims.size() == 0 || isPaused())
                wait();
        }

        /**
         * Advances all animations.
         * 
         * @see AnimationManager.SingleThread#doAdvance(AnimationManager.AnimationContext)
         */
        protected synchronized void doAdvance()
        {
            if (anims.size() == 0 || isPaused()) return;

            java.util.Enumeration e = anims.elements();
            while (e.hasMoreElements())
                doAdvance((AnimationContext) e.nextElement());
        }

        /**
         * Advance a single animation.
         * 
         * @see AnimationManager.SingleThread#doAdvance()
         */
        protected void doAdvance(AnimationContext anim)
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

        /**
         * Stop animating all currently managed animations.
         * 
         * @see AnimationManager.SingleThread#stop(AnimationManager.AnimationContext)
         * @see AnimationManager.SingleThread#start(AnimationManager.AnimationContext)
         */
        protected void stopAll()
        {
            anims.clear();
        }
    }

    private static final boolean DEBUG = false;
}
