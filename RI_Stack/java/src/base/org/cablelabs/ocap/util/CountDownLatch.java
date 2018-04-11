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

package org.cablelabs.ocap.util;

/**
 * A synchronization aid that allows one or more threads to wait until a set of
 * operations being performed in other threads completes.
 * <p>
 * A <code>CountDownLatch</code> is initialized with a given <var>count</var>.
 * The {@link #await() await} methods block until the current
 * {@link #getCount() count} reaches zero due to invocations of the
 * {@link #countDown() countDown} method, after which all waiting threads are
 * released and any subsequent invocations of {@link #await() await} return
 * immediately. This is a one-shot phenomenon -- the count cannot be reset.
 * <p>
 * A <code>CountDownLatch</code> is a synchronization tool that can be used for
 * a number of purposes. A <code>CountDownLatch</code> initialized with a count
 * of one serves as a simple on/off latch, or gate: all threads invoking
 * {@link #await() await} wait at the gate until it is opened by a thread
 * invoking {@link #countDown()}. A <code>CountDownLatch</code> initialized to N
 * can be used to make one thread wait until N threads have completed some
 * action, or some action has been completed N times.
 * <p>
 * A useful property of a <code>CountDownLatch</code> is that it doesn't require
 * that threads calling {@link #countDown() countDown} wait for the count to
 * reach zero before proceeding, it simply prevents any thread from proceeding
 * past an {@link #await() await} until all threads could pass.
 * <p>
 * This is a simplified implementation for use within the OCAP Reference
 * Implementation and is not intended to be a general purpose concurrent library
 * implementation. However, the same API as in a general purpose library is used
 * in case a choice is made to incorporate such a library.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class CountDownLatch
{
    // Instance Fields

    private long m_count;

    // Constructors

    /**
     * Constructs a new instance of the receiver initialized with the given
     * count.
     * 
     * @param count
     *            the number of times {@link #countDown()} must be invoked
     *            before threads can pass through {@link #await()}
     * @throws IllegalArgumentException
     *             if <code>count</code> is less than zero
     */
    public CountDownLatch(final int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be zero or a positive integer:<" + count + ">");
        }

        this.m_count = count;
    }

    // Instance Methods

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is interrupted.
     * <p>
     * If the current {@link #getCount() count} is zero then this method returns
     * immediately.
     * <p>
     * If the current {@link #getCount() count} is greater than zero then the
     * current thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the {@link #countDown()}
     * method; or</li>
     * <li>Some other thread interrupts the current thread.</li>
     * </ul>
     * 
     * @throws InterruptedException
     *             if the current thread is interrupted while waiting, or has
     *             its interrupted status set on entry to this method
     */
    public void await() throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }

        synchronized (this)
        {
            while (this.m_count > 0)
            {
                wait();
            }
        }
    }

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is interrupted, or the specified waiting time has
     * elapsed.
     * <p>
     * If the current {@link #getCount() count} is zero then this method returns
     * immediately with the value <code>true</code>.
     * <p>
     * If the current {@link #getCount() count} is greater than zero then the
     * current thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the {@link #countDown()}
     * method; or</li>
     * <li>Some other thread interrupts the current thread; or</li>
     * <li>The specified waiting time elapses.</li>
     * </ul>
     * If the specified waiting time elapses then the value <code>false</code>
     * is returned. If the time is less than or equal to zero, the method will
     * not wait at all.
     * </p>
     * 
     * @param timeout
     *            the maximum time to wait, in milliseconds
     * @return <code>true</code> if the count reached zero and
     *         <code>false</code> if the waiting time elapsed before the count
     *         reached zero
     * @throws InterruptedException
     *             if the current thread is interrupted while waiting, or has
     *             its interrupted status set on entry to this method
     */
    public boolean await(final long timeout) throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }

        synchronized (this)
        {
            if (this.m_count <= 0L)
            {
                return true;
            }
            else if (timeout <= 0L)
            {
                return false;
            }
            else
            {
                long deadline = System.currentTimeMillis() + timeout;
                long delta = timeout;
                while (true)
                {
                    wait(delta);

                    if (this.m_count <= 0)
                    {
                        return true;
                    }
                    else if ((delta = deadline - System.currentTimeMillis()) <= 0)
                    {
                        return false;
                    }
                }
            }
        }
    }

    /**
     * Decrements the count of the latch, releasing all waiting threads if the
     * count reaches zero.
     * <p>
     * If the current count is greater than zero, then it is decremented. If the
     * new count is zero, then all waiting threads are re-enabled for thread
     * scheduling purposes.
     * <p>
     * If the current count equals zero, then nothing happens.
     */
    public synchronized void countDown()
    {
        if (this.m_count > 0)
        {
            if (--this.m_count == 0)
            {
                notifyAll();
            }
        }
    }

    /**
     * Returns the current count.
     * 
     * @return the current count
     */
    public synchronized long getCount()
    {
        return this.m_count;
    }

    /**
     * Returns a string identifying this latch.
     */
    public String toString()
    {
        return "CountDownLatch: count=" + getCount();
    }
}
