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

/*
 * Created on Oct 31, 2006
 */
package org.cablelabs.impl.util;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.TimerManager;

/**
 * A <code>TaskTimer</code> is a utility class that simplifies the usage of a
 * {@link TaskTimerSpec}.
 * <p>
 * A <code>TaskTimer</code> is effectively a wrapper around a {@link TVTimer}
 * and a {@link TaskQueue}. The <code>TaskTimer</code> provides methods for
 * {@link #createTimerSpec() creating}, {@link #scheduleTimerSpec scheduling}
 * and {@link #deschedule de-scheduling} <code>TaskTimerSpec</code>s, ensuring
 * that the proper <code>TaskQueue</code> and <code>TVTimer</code> are used.
 * <p>
 * Example usage follows:
 * 
 * <pre>
 * TaskTimer timer = new TaskTimer();
 * TVTimer spec = timer.createTimerSpec();
 * spec.setDelayTime(5000L);
 * spec.addTVTimerWentOffListener(l);
 * timer.schedule(spec);
 * </pre>
 * 
 * Basically, when compared to <i>normal</i> <code>TVTimer</code> and
 * <code>TVTimerSpec</code> usage, the only difference is the creation and
 * scheduling of the the <code>TVTimerSpec</code>.
 * 
 * @author Aaron Kamienski
 */
public class TaskTimer
{
    /**
     * Creates a <code>TaskTimer</code> based upon the given
     * <code>TaskQueue</code> and <code>TVTimer</code>. This constructor should
     * be used if the implicit creation of a <code>TaskTimer</code>-specific
     * <code>TaskQueue</code> is not desirable.
     * 
     * @param tq
     *            the <code>TaskQueue</code> upon which all timer events should
     *            be dispatched
     * @param timer
     *            the <code>TVTimer</code> to be used to schedule all
     *            <code>TVTimerSpec</code>s
     */
    public TaskTimer(TaskQueue tq, TVTimer timer)
    {
        this.tq = tq;
        this.timer = timer;
    }

    /**
     * Creates a <code>TaskTimer</code> based upon the given
     * <code>TaskQueue</code> and the <code>TVTimer</code> associated with the
     * given <code>CallerContext</code>. This constructor should be used if the
     * implicit creation of a <code>TaskTimer</code>-specific
     * <code>TaskQueue</code> is not desirable.
     * 
     * @param tq
     *            the <code>TaskQueue</code> upon which all timer events should
     *            be dispatched
     * @param cc
     *            the <code>CallerContext</code> which implies the
     *            {@link TVTimer} to use
     */
    public TaskTimer(TaskQueue tq, CallerContext cc)
    {
        this.tq = tq;
        TimerManager tm = (TimerManager) ManagerManager.getInstance(TimerManager.class);
        this.timer = tm.getTimer(cc);
    }

    /**
     * Creates a <code>TaskTimer</code> based upon the given
     * <code>CallerContext</code>. The <code>TVTimer</code> for the
     * {@link TimerManager#getTimer(CallerContext) given context} and a
     * <code>TaskQueue</code> {@link CallerContext#createTaskQueue() created}
     * will be used implicitly.
     * 
     * @param cc
     *            the <code>CallerContext</code> to base the timer upon
     */
    public TaskTimer(CallerContext cc)
    {
        this.tq = cc.createTaskQueue();
        TimerManager tm = (TimerManager) ManagerManager.getInstance(TimerManager.class);
        this.timer = tm.getTimer(cc);
    }

    /**
     * Creates a <code>TaskTimer</code>. This implicitly uses the
     * {@link CallerContextManager#getSystemContext() system context}'s
     * {@link TimerManager#getTimer(org.cablelabs.impl.manager.CallerContext)
     * TVTimer} and a new <code>TaskQueue</code> created from the system
     * context.
     */
    public TaskTimer()
    {
        this(getSystemContext());
    }

    /**
     * Creates and returns a new {@link TaskTimerSpec}.
     * 
     * @returna new <code>TaskTimerSpec</code>
     */
    public synchronized TVTimerSpec createTimerSpec()
    {
        checkState();
        return new TaskTimerSpec(tq);
    }

    /**
     * Schedules the given {@link TVTimerSpec} with the associated
     * <code>TVTimer</code>.
     * <p>
     * Note that only <code>TVTimerSpec</code>s created {@link #createTimerSpec
     * using} this <code>TaskTimer</code> will be guaranteed to execute using
     * the associated <code>TaskQueue</code>.
     * 
     * @see TVTimer#scheduleTimerSpec(TVTimerSpec)
     */
    public synchronized TVTimerSpec scheduleTimerSpec(TVTimerSpec spec) throws TVTimerScheduleFailedException
    {
        checkState();
        return timer.scheduleTimerSpec(spec);
    }

    /**
     * Deschedules the given {@link TVTimerSpec} with the associated
     * <code>TVTimer</code>.
     * 
     * @see TVTimer#deschedule(TVTimerSpec)
     */
    public synchronized void deschedule(TVTimerSpec spec)
    {
        checkState();
        timer.deschedule(spec);
    }

    /**
     * Disposes this task timer, allowing any necessary resources to be
     * released. This should be invoked when the user or owner of the
     * <code>TaskTimer</code> is finished with it and no longer needs it. After
     * a <code>TaskTimer</code> has been disposed, any subsequent attempt to use
     * it will throw an <code>IllegalStateException</code>.
     */
    public synchronized void dispose()
    {
        if (!disposed)
        {
            disposed = true;
            tq.dispose();
        }
    }

    /**
     * Check the state of the TaskTimer.
     */
    private void checkState()
    {
        if (disposed) throw new IllegalStateException("TaskTimer is disposed");
    }

    /**
     * Utility method that returns the system context.
     * 
     * @return returns the system context
     */
    private static CallerContext getSystemContext()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        return ccm.getSystemContext();
    }

    /**
     * The <code>TaskQueue</code> to be used to create {@link TaskTimerSpec}s.
     */
    private TaskQueue tq;

    /**
     * The <code>TVTimer</code> to be used to {@link #scheduleTimerSpec
     * schedule} and {@link #deschedule de-schedule} <code>TVTimerSpec</code>s.
     */
    private TVTimer timer;

    /** True if disposed */
    private boolean disposed = false;
}
