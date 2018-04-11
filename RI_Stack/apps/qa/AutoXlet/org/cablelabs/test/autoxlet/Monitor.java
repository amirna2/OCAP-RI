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

package org.cablelabs.test.autoxlet;

/**
 * This class implements a simple monitor that can be used by an application to
 * synchronize operation between two threads. The monitor enforces the use of a
 * timeout, to ensure that a thread blocking in the wait state will never block
 * indefinitely.
 * <p>
 * To use this monitor, create with the default constructor. The default timeout
 * is 30 seconds, so if different value is needed, use setTimeout to change.
 * Next, within a block synchronized on the monitor object, invoke the
 * waitForReady method. This will cause the calling thread to block until
 * notified by another thread calling the setReady method. Consider the
 * following example, where an asynchronous call is made, and the notification
 * received on a listener:
 * <p>
 * 
 * <pre>
 *    Monitor myMonitor = new Monitor();
 *    myMonitor.setTimeout(10000); // 10 seconds
 *    ...
 *    
 *    // add a listener for some asynchronous event
 *    AsyncListener listener = AsyncMgr.addListener(this);
 *    
 *    // Wrap the asynchronous call and the wait within a synchronized block.
 *    // This must be done so the notification is not received before the
 *    // waitForReady method has been called.  The thread will not block
 *    // until the monitor is &quot;notified&quot; by another thread, or times out.
 *    synchronized(myMonitor)
 *    {
 *        AsyncMgr.doSomethingAsynchronously();
 *        myMonitor.waitForReady();
 *    }
 * 
 *    // check to make sure the asynchronous action was a success
 *    ...  
 *    
 *    
 *    // The listener for the asynchronous event.  This is invoked by a different
 *    // thread.  When setReady is called, the &quot;waiting&quot; thread is resumed. 
 *    public void onAsyncEvent(AsyncEvent e)
 *    {
 *        // check that e meets our condition
 *        if (e.wasSuccessful)
 *        {
 *            // notify monitor
 *            myMonitor.notifyReady();
 *        }
 *    }
 * </pre>
 * 
 * 
 * @author Greg Rutz
 */
public class Monitor
{

    /**
     * 
     * @uml.property name="timeout" multiplicity="(0 1)"
     */

    /** The maximum amount of time to wait */
    private long m_timeout = 30000; // default 30 seconds

    /** Instance variable to hold the start timestamp */
    private long m_start;

    /** Instance variable to hold the time elapsed */
    private long m_elapsed;

    /**
     * 
     * @uml.property name="m_ready" multiplicity="(0 1)"
     */
    private boolean m_ready = false;

    /**
     * This method returns the state of the monitor. When a thread "waits" on
     * the monitor, its ready state is set to false. When another thread
     * "notifies" the monitor, its ready state is set to true. Therefore, when a
     * thread using the monitor resumes after waiting, this method can be called
     * to determine the outcome of the wait. If this method returns true, the
     * monitor was notified by another thread. If it returns false, the monitor
     * timed out without being notified.
     * 
     * @return The ready state of the monitor.
     * 
     * @uml.property name="m_ready"
     */
    public boolean isReady()
    {
        return m_ready;
    }

    /**
     * This method is used to "notify" a waiting thread so it can continue. If a
     * thread is waiting on the monitor when this method is called, it will exit
     * the wait state and continue its execution. If no thread is waiting and
     * this method is called, nothing happens.
     * 
     * @uml.property name="ready"
     */
    public synchronized void notifyReady()
    {

        // set ready value to true and notify all
        m_ready = true;
        this.notifyAll();
    }

    /**
     * This method returns the wait timeout.
     * 
     * @return The timeout (in milliseconds).
     * @uml.property name="timeout"
     */
    public int getTimeout()
    {
        return (int) m_timeout;
    }

    /**
     * This method sets the wait timeout. A thread that calls waitForReady will
     * wait to be notified for this amount of time. If the time elapses before
     * the notifyReady method is called by another thread, the waiting thread
     * will exit the wait state and continue execution.
     * 
     * @param timeout
     * @uml.property name="timeout"
     */
    public void setTimeout(int timeout)
    {
        m_timeout = timeout;
    }

    /**
     * This method causes the calling thread to enter a wait state on the
     * monitor until the monitor is notified or times out. This method MUST be
     * called from within a synchronized block to ensure that the calling thread
     * owns the lock on the monitor object.
     * 
     * @throws InterruptedException
     */
    public void waitForReady()
    {
        m_ready = false;

        if (m_timeout >= 0)
        {
            m_start = System.currentTimeMillis();
            while (m_ready == false && ((m_elapsed = System.currentTimeMillis() - m_start) < m_timeout))
            {
                try
                {
                    this.wait((m_timeout - m_elapsed));
                }
                catch (InterruptedException e)
                {
                    /*
                     * If the thread was interrupted, just return. This probably
                     * means the JVM is shutting down.
                     */
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
