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

package org.cablelabs.impl.util;

import org.apache.log4j.Logger;


/**
 * This represents an abortable task that can be run by any
 * {@link Thread}. Subclasses must implement these abstract methods:
 * <ul>
 * <li>{@link #doTask()} &mdash; does the main work of the {@link Task}</li>
 * <li>{@link #completeTask()} &mdash; called after {@link #doTask()} if it was
 * successful</li>
 * <li>{@link #failTask(Object)} &mdash; called after {@link #doTask()} if it
 * failed</li>
 * </ul>
 * Execution of the {@link Task} is started by calling the {@link #run()}
 * method, which can only be called once for a {@link Task} instance. Typically,
 * a {@link Task} is scheduled for execution on a separate
 * {@link java.lang.Thread} from the caller. In particular, it has been designed
 * for use with the {@link org.cablelabs.impl.util.TaskQueue}, which schedules
 * {@link java.lang.Runnable} objects for execution on an asynchronous queue.
 * <p>
 * After a {@link Task} is started, it can be aborted by {@link #abort()}. If
 * the {@link Task} has not yet started execution or has already finished
 * execution, {@link #abort()} will return immediately. If the {@link Task} is
 * currently executing, then {@link #abort()} will interrupt the {@link Thread}
 * on which it is running and will not return until the {@link Task} has
 * finished execution.
 * 
 * @author schoonma
 */
public abstract class Task implements Runnable
{
    /** logging */
    private static final Logger log = Logger.getLogger(Task.class);

    /*
     * 
     * State
     */

    /**
     * This abstract class represents the state of the Task. Different Task
     * States extend this class and override the {@link #doRun()} method.
     */
    static public class State
    {
        String name;

        State(String stateName)
        {
            this.name = stateName;
        }

        public String toString()
        {
            return this.name;
        }
    }

    /**
     * This {@link State} indicates that the {@link Task} has not yet started
     * execution.
     */
    public static final State PENDING = new State("PENDING");

    /** This {@link State} indicates that the {@link Task} is currently running. */
    public static final State RUNNING = new State("RUNNING");

    /** This {@link State} indicates that the {@link Task} completed execution. */
    public static final State COMPLETE = new State("COMPLETE");

    /** Current {@link State} of {@link Task}. */
    private State state = PENDING;

    /*
     * 
     * Construction
     */

    private String taskName = null;

    /**
     * Construct a Task with the specified name.
     * 
     * @param name
     *            The Task's name.
     */
    public Task(String name)
    {
        this.taskName = name;
    }

    public String toString()
    {
        return taskName;
    }

    /*
     * 
     * Synchronization
     */

    /** Object for synchronizing threads. */
    private Object sync = new Object();

    /** Indicates whether {@link #abort()} has been called. */
    private boolean aborted = false;

    /**
     * This is the {@link Thread} on which the {@link Task} executes. If this is
     * non-null, it means the {@link #run()} method is executing.
     */
    private Thread thread = null;

    final public void run()
    {
        synchronized (sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug("run task " + this + "[" + state + "]");
            }

            // Only continue if the state is PENDING and it hasn't been aborted.
            if (state != PENDING || aborted)
                return;

            // Set the reference to the current thread and set the state to
            // RUNNING.
            thread = Thread.currentThread();
            state = RUNNING;
        }

        // Call doTask(). If an Error/RuntimeException occurs, call
        // handleError(),
        // passing it the thrown Error.
        try
        {
            doTask();
        }
        catch (Throwable t)
        {
            try
            {
                SystemEventUtil.logRecoverableError(t);
                handleError(t);
            }
            catch (Throwable t2)
            {
                // If an Error/RuntimeException occurs while handling the Error,
                // log it.
                SystemEventUtil.logRecoverableError(t2);
            }
        }

        synchronized (sync)
        {
            // Clear the thread reference and set the state to COMPLETE.
            thread = null;
            state = COMPLETE;
        }
    }

    /**
     * Try to abort the {@link Task}. The behavior depends on the current
     * {@link #state} value:
     * <dl>
     * <dt>{@link #PENDING}</dt>
     * <dd>Change the state to {@link #COMPLETE} so that when the {@link #run()}
     * method is invoked, it won't do anything.</dd>
     * <dt>{@link #RUNNING}</dt>
     * <dd>Interrupt the {@link Thread} on which the {@link Task} is currently
     * running.</dd>
     * <dt>{@link #COMPLETE}</dt>
     * <dd>Does nothing because it has already executed.</dd>
     * </dl>
     * This method will not block.
     * 
     * @return Returns {@link State} of the {@link Task} (i.e., {@link #PENDING}
     *         , {@link #RUNNING}, or {@link #COMPLETE}) at the time
     *         {@link #abort()} was invoked.
     */
    public final State abort()
    {
        synchronized (sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug("abort Task " + this);
            }

            // Only allow abort once.
            if (aborted)
                return state;

            // Mark it as aborted.
            aborted = true;

            // If it's running, interrupt the thread.
            if (state == RUNNING)
            {
                if (thread != null) thread.interrupt();
            }

            // Return the current state.
            return state;
        }
    }

    /**
     * This method does the main work of the {@link Task}.
     * <p>
     * NOTE: This method must <em>not</em> throw any {@link Error}s or
     * {@link Exception}s. If it does, the Player will be closed.
     */
    protected abstract void doTask();

    /**
     * This method is called by {@link #run()} if {@link #doTask()} throws an
     * {@link Error}.
     * 
     * @param e
     *            the {@link Error} that was thrown by {@link #doTask()}
     */
    protected abstract void handleError(Throwable e);
}
