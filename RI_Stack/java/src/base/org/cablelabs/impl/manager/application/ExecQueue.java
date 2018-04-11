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

import org.cablelabs.impl.util.TaskQueue;

import java.util.Vector;

/**
 * A FIFO queue of <code>Runnable</code> objects.
 * <p>
 * Note that this class does not automatically provide a thread that retrieves
 * and executes <code>Runnable</code>s posted to this queue. A separate
 * <code>Thread</code> must be created and run to provide this capability. This
 * may be provided implicitly by subclasses.
 * 
 * @author Aaron Kamienski
 * @see AppExecQueue
 * @see DemandExecQueue
 */
class ExecQueue implements TaskQueue
{
    /**
     * Posts a new <code>Runnable</code> to this queue for serialized retrieval
     * via {@link #getNext}.
     * 
     * @param exec
     *            the <code>Runnable</code> to post to the queue
     */
    public synchronized void post(Runnable exec)
    {
        if (DISPOSED_POST_ILLEGAL && disposed)
            throw new IllegalStateException("Cannot post to disposed queue: " + this);
        q.addElement(exec);
        notify();
    }

    /**
     * Retrieves the next entry in the queue, blocking as long as it is empty.
     * Will return <code>null</code> if the queue is disposed of.
     * 
     * @return the oldest <code>Runnable</code> in the queue; or
     *         <code>null</code> if this queue is disposed
     */
    public synchronized Runnable getNext()
    {
        while (!disposed && q.size() == 0)
        {
            numWaitingThreads++;
            
            try
            {
                if (waitOnQueue())
                {
                    // If returned from wait, then return whatever we have right
                    // now.
                    // This way waitOnQueue() can timeout and indicate this via
                    // null
                    break;
                }
            }
            catch (InterruptedException e)
            {
                continue;
            }
            finally
            {
                numWaitingThreads--;
            }
        }

        // We're done here
        if (disposed || q.size() == 0)
        {
            return null;
        }
        
        Runnable next = (Runnable) q.elementAt(0);
        q.removeElementAt(0);
        return next;
    }

    /**
     * Disposes of this queue.
     */
    public synchronized void dispose()
    {
        disposed = true;
        notifyAll();
    }
    
    /**
     * Get the number of outstanding tasks on this queue waiting to be serviced.  This
     * value can be negative if there are more threads waiting to service tasks than
     * there are threads to be serviced
     * 
     * @return the number of outstanding tasks
     */
    public synchronized int numOutstandingTasks()
    {
        return q.size() - numWaitingThreads;
    }

    /**
     * Waits for the queue to be updated. This invokes {@link #wait()} by
     * default, but may be overridden to call {@link #wait(long)} to implement a
     * timeout.
     * <p>
     * The return value indicates whether {@link #getNext} should continue
     * waiting or not (when the queue is empty). For a timeout based approach,
     * <code>true</code> should be returned, otherwise <code>false</code> should
     * be returned.
     * 
     * @return <code>false</code> indicating that <code>getNext()</code> should
     *         continue waiting if the queue is empty
     * 
     * @throws InterruptedException
     */
    protected synchronized boolean waitOnQueue() throws InterruptedException
    {
        wait();
        return false;
    }

    /**
     * The list of queued <code>Runnable</code> objects.
     */
    protected Vector q = new Vector();

    /**
     * Whether this queue is disposed or not.
     */
    protected boolean disposed = false;
    
    /**
     * Number of threads waiting to execute tasks
     */
    private int numWaitingThreads = 0;
    
    /**
     * I don't think that post() to a disposed queue should be legal. However it
     * is not enabled for this release so as not to affect current operation
     * unexpectedly.
     */
    private static final boolean DISPOSED_POST_ILLEGAL = true;
}
