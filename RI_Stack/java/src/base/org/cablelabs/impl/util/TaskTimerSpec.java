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
import javax.tv.util.TVTimerSpec;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.TimerManager;

/**
 * An extension of {@link TVTimerSpec} that can be used to ensure that all timer
 * notifications occur within the context of a given {@link TaskQueue}. The
 * intention is to allow the asynchronous dispatching of timer events for this
 * <code>TVTimerSpec</code> with respect to other <code>TVTimerSpec</code>s
 * scheduled with the same {@link TVTimer}, but to still ensure in-order
 * dispatching of events for this <code>TVTimerSpec</code>.
 * 
 * This implementation takes advantage of the fact that
 * {@link TVTimerSpec#notifyListeners(TVTimer)} is publicly defined.
 * <p>
 * An example of usage follows:
 * 
 * <pre>
 * CallerContextManager ccm = ...;
 * TimerManager tm = ...;
 * CallerContext sysCC = ccm.getSystemContext();
 * TVTimer timer = tm.getTimer(sysCC);
 * TaskQueue tq = sysCC.createTaskQueue();
 * 
 * // Create the spec
 * TaskTimerSpec spec = new TaskTimerSpec(tq);
 * 
 * // Set up and schedule the spec (no differently than usual)
 * TVTimerWentOffListener l = ...;
 * spec.setDelayTime(5000L);
 * spec.addTVTimerWentOffListener(l);
 * timer.schedule(spec);
 * </pre>
 * 
 * @author Aaron Kamienski
 */
public class TaskTimerSpec extends TVTimerSpec
{
    /**
     * Creates an instance of <code>TaskTimerSpec</code>.
     * <p>
     * Note it is important that the {@link CallerContext} that
     * {@link CallerContext#createTaskQueue() created} the given
     * <code>TaskQueue</code> and the <code>CallerContext</code> with which the
     * scheduling {@link TVTimer} is
     * {@link TimerManager#getTimer(org.cablelabs.impl.manager.CallerContext)
     * associated} be the same. Otherwise, listeners may be invoked within a
     * different context than they otherwise should be.
     * 
     * @param tq
     *            the <code>TaskQueue</code> to be used for execution
     */
    public TaskTimerSpec(TaskQueue tq)
    {
        super();
        this.tq = tq;
    }

    /**
     * Overrides super implementation, ensuring that listeners are notified
     * using the {@link TaskQueue} associated with this
     * <code>TaskTimerSpec</code>.
     * 
     * @see javax.tv.util.TVTimerSpec#notifyListeners(javax.tv.util.TVTimer)
     */
    public void notifyListeners(final TVTimer source)
    {
        tq.post(new Runnable()
        {
            public void run()
            {
                TaskTimerSpec.super.notifyListeners(source);
            }
        });
    }

    /**
     * The <code>TaskQueue</code> that should be used to dispatch notifications
     * to all listeners.
     */
    private TaskQueue tq;
}
