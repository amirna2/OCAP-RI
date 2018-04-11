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

package org.cablelabs.impl.manager.recording;

/**
 * A signal implementaion.
 * 
 * @author Jeff Spruiel
 * 
 */
public class ConditionVariable
{
    // Flag to indicate object is signalled or not.
    private boolean m_isSignalled;

    // No timeout
    public static final long INFINITE = Long.MAX_VALUE;

    static public class TimeOutException extends RuntimeException
    {
        public TimeOutException()
        {
            super();
        }

        public TimeOutException(String s)
        {
            super(s);
        }
    }

    /**
     * Constructor
     * 
     * @param initialState
     *            True if signaled, false if not signaled.
     */
    public ConditionVariable(boolean initialState)
    {
        m_isSignalled = initialState;
    }

    // Reset the object to not signalled.
    public void reset()
    {
        synchronized (this)
        {
            m_isSignalled = false;
        }
    }

    // A message to signal the object and let a waiting thread through.
    public void signal()
    {
        synchronized (this)
        {
            m_isSignalled = true;
            notify();
        }
    }

    // Tests if the object is signalled or not signalled.
    public final boolean isSignalled()
    {
        return m_isSignalled;
    }

    /**
     * Called to wait until this object becomes signalled by another thread. If
     * the object is already signalled the call returns immediately. If the
     * timeToWait is reached a ConditionVariable.TimeOutException is thrown or
     * if the thread is interrupted a InterruptedException is thrown.
     */
    public final boolean waitForSignal(long timeToWait) throws InterruptedException, TimeOutException
    {
        synchronized (this)
        {
            long expiration = 0;

            // if don't wait or already signalled
            if ((timeToWait == 0) || (m_isSignalled))
            {
                return m_isSignalled;
            }

            return (timeToWait == INFINITE) ? doWaitForEver() : doTimedWait(timeToWait);
        }
    }

    // Waits for ever if an IterruptedException isn't thrown.
    private boolean doWaitForEver() throws InterruptedException
    {
        synchronized (this)
        {
            while (!m_isSignalled)
            {
                wait();
            }
            return true;
        }
    }

    // Performs the wait for timeout algorithm.
    private boolean doTimedWait(long duration) throws InterruptedException, TimeOutException
    {
        synchronized (this)
        {
            // calculate absolute endTime
            long endTime = System.currentTimeMillis() + duration;
            while (!m_isSignalled)
            {
                // check if we've reached endTime.
                long timeLeft = endTime - System.currentTimeMillis();

                // if endtime reached, time out.
                // otherwise wait the remaining time or until
                // we've been signalled.
                if (timeLeft <= 0)
                {
                    throw new TimeOutException();
                }

                // I will wakeup if:
                // 1) I ame interrupted (propagate)
                // 2) His time expired (throw exp)
                // 3) I am signalled
                wait(timeLeft);
            }
        }
        return true;
    }
}
