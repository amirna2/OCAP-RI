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

package org.cablelabs.impl.manager.timer;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Java2 compatible implementation of <code>TimerManager</code>. Supported by
 * J2SE as well as J2ME CDC/Foundation.
 * 
 * @author Aaron Kamienski
 */
public class TimerMgrJava2 extends TimerMgr
{
    private static final Logger log = Logger.getLogger(TimerMgrJava2.class);

    /**
     * Creates and returns a new <code>TVTimer</code> instance for the given
     * <code>CallerContext</code>.
     * 
     * @param ctx
     *            the calling context
     * @return a non-null TVTimer object
     */
    protected TVTimer createTimer(CallerContext ctx)
    {
        return new TVTimerImpl();
    }

    /**
     * Disables and disposes of the given <code>TVTimer</code> for the given
     * <code>CallerContext</code>. Should ensure that currently scheduled
     * <code>TVTimerSpec</code>s are descheduled.
     * 
     * @param ctx
     *            the calling context
     * @param timer
     *            the <code>TVTimer</code> to deschedule
     */
    protected void disposeTimer(CallerContext ctx, TVTimer timer)
    {
        ((TVTimerImpl) timer).dispose();
    }

    /**
     * Implementation of <code>TVTimer</code> based upon Java2
     * <code>java.util.Timer</code> class.
     * 
     * @author Aaron Kamienski
     * 
     * @note This class has default access (i.e., package-private) in order to
     *       enable unit testing. If it is to be extended outside of this
     *       package, then it will need to be made <code>protected</code>.
     */
    static class TVTimerImpl extends TVTimer
    {
        /**
         * The Java2 <code>Timer</code> instance used to implement this
         * <code>TVTimer</code>.
         */
        private Timer timer = new Timer(true);

        /**
         * Disables and disposes of this <code>TVTimer</code>.
         */
        public void dispose()
        {
            timer.cancel();
            timer = null;
        }

        /**
         * Begins monitoring a TVTimerSpec.
         * 
         * <p>
         * When the timer specification should go off, the timer will call
         * <code>TVTimerSpec.notifyListeners().</code>
         * </p>
         * 
         * <p>
         * Returns the actual <code>TVTimerSpec</code> that got scheduled. If
         * you schedule a specification that implies a smaller granularity than
         * this timer can provide, or a repeat timer specification that has a
         * smaller repeating interval than this timer can provide, the timer
         * should round to the closest value and return that value as a
         * {@link TVTimerSpec} object. An interested application can use
         * accessor methods {@link #getMinRepeatInterval} and
         * {@link #getGranularity} to obtain the Timer's best knowledge of the
         * Timer's limitation on granularity and repeat interval. If you
         * schedule an absolute specification that should have gone off already,
         * it will go off immediately. If the scheduled specification cannot be
         * satisfied, the exception {@link TVTimerScheduleFailedException}
         * should be thrown.
         * </p>
         * 
         * <p>
         * You may schedule a timer specification with multiple timers. You may
         * schedule a timer specification with the same timer multiple times (in
         * which case it will go off multiple times). If you modify a timer
         * specification after it has been scheduled with any timer, the results
         * are unspecified.
         * </p>
         * 
         * @param t
         *            The timer specification to begin monitoring.
         * @return The real TVTimerSpec that was scheduled.
         * @exception TVTimerScheduleFailedException
         *                is thrown when the scheduled specification cannot be
         *                satisfied.
         */
        public TVTimerSpec scheduleTimerSpec(TVTimerSpec t) throws TVTimerScheduleFailedException
        {
            if (timer == null)
            {
                throw new TVTimerScheduleFailedException("Timer has already been disposed!");
            }

            long newTime = t.getTime();
            if (t.isRepeat() && newTime < timerInterval) newTime = timerInterval;
            if (timerGranularity != -1) newTime = newTime - (newTime % timerGranularity);
            if (t.isRepeat() && newTime < timerInterval) newTime += timerGranularity;
            if (t.isAbsolute())
            {
                final long currentTime = System.currentTimeMillis();
                if(newTime < currentTime) 
                {
                    newTime = currentTime;
                }
            }
            t.setTime(newTime);

            // Must map TVTimerSpec to TimerTask(s)
            // Since a TimerTask can only be scheduled once,
            TimerTask task = createTask(t, t.getTime());

            try
            {
                scheduleTimerSpec(task, t, t.getTime());
            }
            catch (IllegalStateException e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("exception scheduling timer spec: " + task + ", " + t, e);
                }
                // Something bad has happened that caused our timer thread to
                // be terminated. Possibly an OutOfMemory condition. Try to
                // create
                // a new timer and reschedule all of our registered timer specs
                try
                {
                    timer = new Timer(true);
                    synchronized(specs)
                    {
                        for (Enumeration en = specs.elements(); en.hasMoreElements();)
                        {
                            TVTimerSpec spec = (TVTimerSpec) en.nextElement();
                            Vector tasks = (Vector) specs.get(spec);
                            if (tasks == null) continue;

                            // Create new TimerTasks for each spec
                            int size = tasks.size();
                            for (int i = 0; i < size; ++i)
                            {
                                // TODO(Todd): See bug 4734
                                TimerTask timerTask = createTask(spec, spec.getTime());
                                scheduleTimerSpec(timerTask, spec, spec.getTime());
                            }
                        }
                    }
                }
                catch (Exception badException)
                {
                    // If we get here, we have a serious problem (possibly out
                    // of memory).
                    SystemEventUtil.logRecoverableError(badException);

                    throw new TVTimerScheduleFailedException (badException.getMessage());
                }
            }

            // Return the timer specification with the adjusted time
            return t;
        }

        private void scheduleTimerSpec(TimerTask task, TVTimerSpec spec, long specTime)
        {
            if (log.isDebugEnabled())
            {
                log.debug("scheduleTimerSpec: " + task + ", spec: " + spec + ", time: " + specTime);
            }
            // TVTimerSpec may be absolute or delayed...
            if (spec.isAbsolute())
            {
                timer.schedule(task, new Date(specTime));
            }
            // Delayed may be non-repeating or repeating
            else if (!spec.isRepeat())
            {
                // Delayed (non-repeating) goes off after a specified time
                timer.schedule(task, specTime);
            }
            // Repeating goes off repeatedly
            else
            {
                // Regular means repeated timers are rescheduled at regular
                // intervals
                if (spec.isRegular())
                {
                    // Fixed rate
                    timer.scheduleAtFixedRate(task, specTime, specTime);
                }
                // Non-regular means the timer is rescheduled after listeners
                // are notified
                else
                {
                    // A TimerTask that reschedules after notifying listeners is
                    // returned
                    // Don't schedule as repeating
                    timer.schedule(task, specTime);
                }
            }
        }

        /**
         * Removes a timer specification from the set of monitored
         * specifications. The descheduling happens as soon as practical, but
         * may not happen immediately. If the timer specification has been
         * scheduled multiple times with this timer, all the schedulings are
         * canceled.
         * 
         * @param t
         *            The timer specification to end monitoring.
         */
        public void deschedule(TVTimerSpec t)
        {
            // Removes all scheduled timers created with this timer spec

            Vector tasks = (Vector) specs.remove(t);

            if (tasks != null)
            {
                for (Enumeration e = tasks.elements(); e.hasMoreElements();)
                {
                    TimerTask task = (TimerTask) e.nextElement();
                    task.cancel();
                }
                tasks.removeAllElements();
                tasks = null;
            }
            else
            {
                // Bad timer spec - perhaps never scheduled or descheduled
                // twice?
                if (log.isDebugEnabled())
                {
                    log.debug("deschedule called with an unscheduled spec: " + (t == null ? "null" : t.toString()));
                }
        }
        }

        /**
         * Report the minimum interval that this timer can repeat tasks. For
         * example, it's perfectly reasonable for a Timer to specify that the
         * minimum interval for a repeatedly performed task is 1000 milliseconds
         * between every run. This is to avoid possible system overloading.
         * 
         * @return The timer's best knowledge of minimum repeat interval in
         *         milliseconds. Return -1 if this timer doesn't know its
         *         repeating interval limitation.
         * 
         * @todo It would probably be a good idea to try and determine the
         *       minimum repeat interval. How can we estimate this? Schedule a
         *       really short repeat interval and see what we get.
         */
        public long getMinRepeatInterval()
        {
            return timerInterval;
        }

        /**
         * Report the granularity of this timer, i.e., the length of time
         * between "ticks" of this timer.
         * 
         * @return The timer's best knowledge of the granularity in
         *         milliseconds. Return -1 if this timer doesn't know its
         *         granularity.
         * 
         * @todo It would probably be a good idea to try and determine the
         *       granularity. How can we estimate this? Schedule a really short
         *       repeat interval and see what we get.
         */
        public long getGranularity()
        {
            return timerGranularity;
        }

        /**
         * Records the given <code>TimerTask</code> as representing the given
         * <code>TVTimerSpec</code>. Additionally, removes the
         * <code>TimerTask</code> <i>toRemove</i> from such responsibility.
         * 
         * @return <code>TVTimerTask</code> <i>task</i>
         * 
         * @note This method has default access (i.e., package-private) in order
         *       to enable unit testing.
         */
        TimerTask addSpecTask(TVTimerSpec spec, TimerTask task, TimerTask toRemove)
        {
            synchronized (specs)
            {
                Vector tasks = (Vector) specs.get(spec);
                if (tasks == null)
                {
                    tasks = new Vector();
                    specs.put(spec, tasks);
                }
                if (toRemove != null) tasks.removeElement(toRemove);

                tasks.addElement(task);
            }
            return task;
        }

        /**
         * <i>Forgets</i> the given <code>TimerTask</code> as representing the
         * given <code>TVTimerSpec</code>. Additionally, if this is the last
         * <code>TimerTask</code> for the given <code>TVTimerSpec</code>, then
         * the <code>TVTimerSpec</code> is <i>forgotten</i> as well.
         * <p>
         * This is called only for <i>non-repeating</i> <code>TVTimerSpec</code>
         * s. All others must be manually <code>deschedule</code>d. This has the
         * effect of implicitly descheduling non-repeating specs.
         * 
         * @param spec
         *            the <code>TVTimerSpec</code> to dissociate the given
         *            <code>TimerTask</code> from
         * @param toRemove
         *            the <code>TimerTask</code> to forget
         * 
         * @see #deschedule
         */
        private void removeSpecTask(TVTimerSpec spec, TimerTask toRemove)
        {
            synchronized (specs)
            {
                Vector tasks = (Vector) specs.get(spec);
                if (tasks != null)
                {
                    tasks.removeElement(toRemove);
                    if (tasks.size() == 0) specs.remove(spec);
                }
            }
        }

        /**
         * Creates, stores, and returns a new <code>TimerTask</code> to
         * represent a new scheduling of the given <code>TVTimerSpec</code>.
         * 
         * @param spec
         *            the TVTimerSpec
         */
        private TimerTask createTask(final TVTimerSpec spec, final long specTime)
        {
            // Create new TimerTask that will notify TVTimerSpec's listeners
            TimerTask task;
            if (spec.isAbsolute() || !spec.isRepeat() || spec.isRegular())
            {
                // General-purpose TimerTask
                task = new TimerTask()
                {
                    boolean repeat = spec.isRepeat();

                    public void run()
                    {
                        // Do not run this task if it is repeating and too late.
                        // Cancel, remove it, and re-schedule to correct for the
                        // time change
                        long now = System.currentTimeMillis();

                        if (repeat && now - scheduledExecutionTime() > MAX_TASK_TARDINESS)
                        {
                            removeSpecTask(spec, this);
                            cancel();

                            try
                            {
                                scheduleTimerSpec(spec);
                            }
                            catch (TVTimerScheduleFailedException e)
                            {
                                // Failed to reschedule our timer spec to
                                // account for time
                                // change
                                SystemEventUtil.logRecoverableError(e);
                            }
                        }
                        else
                        {
                            // notify spec
                            spec.notifyListeners(TVTimerImpl.this);

                            // cleanup if non-repeating
                            if (!repeat) removeSpecTask(spec, this);
                        }
                    }
                };
            }
            else
            {
                // TimerTask used for non-regular repeating timer specifications
                class NonRegularRepeating extends TimerTask
                {
                    /**
                     * Notifies listeners and then reschedules a copy of itself.
                     */
                    public void run()
                    {
                        spec.notifyListeners(TVTimerImpl.this);

                        // This task's timer spec may have been descheduled by
                        // the listener,
                        // so make sure the spec is still registered and only
                        // reschedule this
                        // task if it is.
                        if (specs.containsKey(spec))
                        {
                            // Schedule new timertask, forget this one.
                            timer.schedule(addSpecTask(spec, new NonRegularRepeating(), this), specTime);
                        }
                    }
                }
                task = new NonRegularRepeating();
            }
            // Add TimerTask to hash (keyed off of TVTimerSpec) of vectors of
            // TimerTasks
            return addSpecTask(spec, task, null);
        }

        /**
         * If a task is trying to run any more than 5 minutes after it was
         * scheduled to run, then it will be canceled and rescheduled. A system
         * time change may have occurred
         */
        static final long MAX_TASK_TARDINESS = MPEEnv.getEnv("OCAP.timer.maxtardiness", 300000L);

        /**
         * Contains all <code>TVTimerSpec</code>s currently scheduled by this
         * <code>TVTimer</code>. <code>TVTimerSpec</code>s serve as keys in a
         * hashtable, mapping <code>TVTimerSpec</code> to multiple
         * <code>TimerTask</code>s. This is because a single
         * <code>TVTimerSpec</code> can be scheduled multiple times, but a a
         * single <code>TimerTask</code> can only be scheduled once.
         */
        private final Hashtable specs = new Hashtable();
    }

    static private long timerGranularity = -1;

    static private long timerInterval = -1;

    static
    {
        String interval = MPEEnv.getEnv("OCAP.timer.interval", "40");
        try
        {
            timerInterval = Integer.parseInt(interval);
        }
        catch (NumberFormatException e)
        {
            timerInterval = 40;
        }

        String granularity = MPEEnv.getEnv("OCAP.timer.granularity", "10");
        try
        {
            timerGranularity = Integer.parseInt(granularity);
        }
        catch (NumberFormatException e)
        {
            timerGranularity = 10;
        }
    }

    /*
     * static private long estimateGranularity() throws Exception { TVTimer
     * granTimer = TVTimer.getTimer();
     * 
     * // This listener simply keeps track of when it went off class WentOff
     * implements TVTimerWentOffListener { long when = -1L; TVTimer myTimer =
     * null;
     * 
     * public WentOff(TVTimer t) { myTimer = t; }
     * 
     * public synchronized void timerWentOff(TVTimerWentOffEvent e) { when =
     * System.currentTimeMillis(); myTimer.deschedule(e.getTimerSpec());
     * notifyAll(); } }
     * 
     * // Create a listener and add it to our timer spec TVTimerSpec spec = new
     * TVTimerSpec(); WentOff l = new WentOff(granTimer);
     * spec.addTVTimerWentOffListener(l); spec.setDelayTime(1);
     * 
     * long last; try { last = System.currentTimeMillis(); synchronized(l) {
     * granTimer.scheduleTimerSpec(spec); l.wait(5000); } } finally {
     * granTimer.deschedule(spec); }
     * 
     * if (l.when < 0L) return -1;
     * 
     * return l.when - last; }
     * 
     * static private long estimateInterval() throws Exception { TVTimer
     * intTimer = TVTimer.getTimer(); TVTimerSpec spec = new TVTimerSpec();
     * 
     * // This listener will keep track of the time between callbacks // and
     * maintain a running average class WentOff implements
     * TVTimerWentOffListener { long count = 20, last, interval = -1; TVTimer
     * myTimer = null;
     * 
     * public WentOff(TVTimer t) { myTimer = t; }
     * 
     * public synchronized void timerWentOff(TVTimerWentOffEvent e) { long now =
     * System.currentTimeMillis(); long i = now-last; last = now; interval =
     * (interval == -1) ? i : (interval+i)/2; if (--count == 0) {
     * myTimer.deschedule(e.getTimerSpec()); notifyAll(); } } }
     * 
     * // Create a listener and add it to our timer spec WentOff l = new
     * WentOff(intTimer); spec.addTVTimerWentOffListener(l);
     * 
     * // Specify a non-regular, repeating timer with the smallest // possible
     * delay spec.setDelayTime(1); spec.setRepeat(true); spec.setRegular(false);
     * try { l.last = System.currentTimeMillis(); synchronized(l) {
     * intTimer.scheduleTimerSpec(spec); l.wait(20000); } } finally {
     * intTimer.deschedule(spec); }
     * 
     * if (l.interval < 0L) return -1;
     * 
     * return l.interval; }
     */
}
