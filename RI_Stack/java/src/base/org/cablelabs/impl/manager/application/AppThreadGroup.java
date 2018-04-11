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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;

/**
 * An <code>AppThreadGroup</code> is an application-specific
 * <code>ThreadGroup</code>. All application threads are created as part of the
 * application-specific thread group. This includes any application-specific
 * <code>EventQueue</code>s as they implicilty create an
 * <code>EventDispatchThread</code>.
 * 
 * @author Aaron Kamienski
 */
public class AppThreadGroup extends ThreadGroup
{
    /**
     * Constructs an application-specific ThreadGroup.
     * 
     * @param cc
     *            the <code>CallerContext</code> associated with an application
     */
    public AppThreadGroup(CallerContext cc)
    {
        this(cc.toString());
        this.cc = cc;
    }

    /**
     * Constructs an application-specific ThreadGroup, w/out specifying the
     * <code>CallerContext</code>. This method allows for creation of an
     * <code>AppThreadGroup</code> before the creation of the associated
     * <code>CallerContext</code>. It is imperitive that
     * {@link #setCallerContext} be called as soon as possible.
     * 
     * @param name
     *            <code>AppThreadGroup</code> name
     */
    AppThreadGroup(String name)
    {
        super(name);
        // Set maxPriority=NORM_PRIORITY according to MHP 11.2.10
        setMaxPriority(Thread.NORM_PRIORITY);
    }

    /**
     * Sets the <code>CallerContext</code> that this thread group is associated
     * with. Should only be used with the {@link #AppThreadGroup(String)}
     * constructor.
     * 
     * @param cc
     *            the <code>CallerContext</code> associated with an application
     */
    void setCallerContext(CallerContext cc)
    {
        if (this.cc != null) throw new IllegalStateException("Cannot reset caller context");
        this.cc = cc;
    }

    /**
     * Overrides <code>Object.toString()</code>.
     * 
     * @return string representation of this object
     */
    public String toString()
    {
        return "AppThreadGroup@" + System.identityHashCode(this) + "[name=" + getName() + "maxpri=" + getMaxPriority()
                + "]";
    }

    /**
     * Returns the <code>CallerContext</code> that this
     * <code>AppThreadGroup</code> is associated with.
     * <p>
     * This will be called by the <code>CallerContextManager</code>
     * implementation of {@link CallerContextManager#getCurrentContext()} in a
     * manner resembling the following:
     * 
     * <pre>
     * ThreadGroup tg = Thread.currentThread().getThreadGroup();
     * return ((AppThreadGroup) tg).getCallerContext();
     * </pre>
     * 
     * @return the <code>CallerContext</code> that this <code>ThreadGroup</code>
     *         is associated with.
     */
    public CallerContext getCallerContext()
    {
        return cc;
    }

    /**
     * Overrides the super-class definition of <code>uncaughtException</code> to
     * provide support for the use of an <code>ExceptionListener</code>.
     * 
     * @param th
     *            the Thread on which an uncaught exception occurred
     * @param t
     *            the instance of <code>Throwable</code> that was not caught
     */
    public void uncaughtException(Thread thread, Throwable throwable)
    {
        super.uncaughtException(thread, throwable);

        ExceptionListener listener = this.listener;
        if (listener != null) listener.uncaughtException(thread, throwable);
    }

    /**
     * Adds an <code>ExceptionListener</code> to be notified when uncaught
     * exceptions occur on threads within this thread group.
     * 
     * This may include logging information to the OCAP system error manager.
     * <code>ThreadDeath</code> exceptions will be watched for in order to
     * monitor the successful stopping of threads of a non-compliant
     * application.
     * 
     * @param l
     *            The listener to be added
     */
    public void addExceptionListener(ExceptionListener l)
    {
        if (listener != null) throw new IllegalArgumentException("Only one EventListener is allowed");
        listener = l;
    }

    /**
     * Removes an exception listener from this thread group.
     * 
     * @param l
     *            the listener to be removed
     */
    public void removeExceptionListener(ExceptionListener l)
    {
        if (listener != l) throw new IllegalArgumentException("No such listener installed!");
        listener = null;
    }

    /**
     * Listener interface to be implemented by entities (namely the Application
     * Manager) that wish to be notified about uncaught exceptions within this
     * <code>ThreadGroup</code> (which maps directly to a given application).
     */
    public interface ExceptionListener
    {
        /**
         * Called by the <code>AppThreadGroup</code> when a code running on a
         * contained thread does not catch an exception.
         * 
         * @param thread
         *            the Thread on which an uncaught exception occurred
         * @param throwable
         *            the instance of <code>Throwable</code> that was not caught
         * @see ThreadGroup#uncaughtException(Thread,Throwable)
         */
        public void uncaughtException(Thread thread, Throwable throwable);
    }

    /**
     * Only one exception listener is supported. If more than one is installed,
     * then an exception should be thrown.
     */
    private ExceptionListener listener;

    /**
     * The <code>CallerContext</code that this <code>ThreadGroup</code> is
     * associated with.
     */
    private CallerContext cc;
}
