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

package org.cablelabs.impl.manager.application;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

/**
 * <code>WatchDogTask</code> is a <code>Runnable</code> implementation suitable
 * for execution by a thread. The <code>WatchDogTask</code> will set a timer
 * prior to executing the component <code>Runnable</code>; if the timer expires
 * before the <code>Runnable.run()</code> method returns then the
 * {@link #timeoutExpired} method is invoked.
 * <p>
 * The timer is implemented using the context-specific <code>TVTimer</code>. So
 * whatever context the <code>WatchDogTask.run()</code> method is invoked in is
 * the context that <code>timeoutExpired()</code> will be invoked in.
 * 
 * @author Aaron Kamienski
 */
class WatchDogTask implements Runnable, TVTimerWentOffListener
{
    /**
     * Creates an instance of WatchDogTask based on the given component
     * <code>Runnable</code> and fixed timeout.
     * 
     * @param runnable
     *            the <code>Runnable</code> to execute
     * @param timeout
     *            the timeout period in ms
     * 
     * @throws NullPointerException
     *             if runnable is null
     * @throws IllegalArgumentException
     *             if timeout <= 0
     */
    WatchDogTask(Runnable runnable, long timeout)
    {
        if (runnable == null) throw new NullPointerException("Runnable should not be null");
        if (timeout <= 0) throw new IllegalArgumentException("timeout must be > 0, was " + timeout);
        this.runnable = runnable;
        this.timeout = timeout;
    }

    /**
     * Sets a timer before executing the component <code>Runnable</code>. If the
     * timer expires before the <code>Runnable</code> returns, then
     * <code>timeoutExpired()</code> is invoked. Once the <code>Runnable</code>
     * returns, the timer is cancelled.
     * 
     * @see java.lang.Runnable#run()
     * @see #timeoutExpired
     */
    public void run()
    {
        // Create timer spec
        TVTimerSpec spec = new TVTimerSpec();
        spec.setDelayTime(timeout);
        spec.addTVTimerWentOffListener(this);

        // Schedule the timer
        TVTimer timer = TVTimer.getTimer();
        try
        {
            spec = timer.scheduleTimerSpec(spec);
        }
        catch (TVTimerScheduleFailedException e)
        {
            // A failure is totally unexpected here - but handled just in case
            spec = null;
            if (timerFailure()) return;
        }

        // Execute the runnable
        try
        {
            runnable.run();
        }
        finally
        {
            // Finally cancel the timer
            if (spec != null) timer.deschedule(spec);
        }
    }

    /**
     * Implements {@link TVTimerWentOffListener#timerWentOff}. This is invoked
     * if the timeout expires while executing the component
     * <code>Runnable</code>. This method is not considered part of the standard
     * API for this class, but merely a side-effect of its implementation using
     * a <code>TVTimer</code>.
     * <p>
     * Simply invokes {@link timeoutExpired}.
     * 
     * @param e
     *            timeout event
     */
    public void timerWentOff(TVTimerWentOffEvent e)
    {
        timeoutExpired();
    }

    /**
     * Invoked if there is a problem setting the timeout timer. The
     * <code>Runnable</code> is only invoked based on the return value of this
     * method. This implementation always returns <code>false</code> indicating
     * that the <code>Runnable</code> should still be executed. If
     * <code>true</code> were returned, then the <code>Runnable</code> would not
     * be executed at all.
     * 
     * @return <code>false</code> indicating that the <code>Runnable</code>
     *         should execute anyhow
     */
    protected boolean timerFailure()
    {
        return false;
    }

    /**
     * Called when timeout expires because the component
     * <code>Runnable.run()</code> method took too long to complete.
     */
    protected void timeoutExpired()
    {
        // Do nothing by default
    }

    /**
     * The component runnable to execute.
     */
    private Runnable runnable;

    /**
     * The timeout period.
     */
    private long timeout;
}
