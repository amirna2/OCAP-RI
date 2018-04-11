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

import org.cablelabs.impl.awt.EventDispatchable;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.util.TaskQueue;

import java.awt.Toolkit;
import java.util.Properties;

import org.ocap.system.event.ErrorEvent;

/**
 * Implementation of the <i>system</i> <code>CallerContext</code>. A single
 * instance of this class is maintained by the <code>CallerContextManager</code>
 * {@link AppManager implementation} and returned by its
 * {@link AppManager#getSystemContext() getSystemContext()} method.
 * 
 * @author Aaron Kamienski
 */
class SystemContext extends AbstractCallerContext
{
    /**
     * Creates an instance of SystemContext.
     * 
     * @param ccMgr
     *            the <code>CCMgr</code> managing this caller context
     * @param tp
     *            the <code>ThreadPool</code> to use for
     *            {@link AbstractCallerContext#runInContext(Runnable)}
     * @param threadPriority
     *            priority at which context's threads should run
     */
    SystemContext(CCMgr ccMgr, ThreadPool tp, int threadPriority)
    {
        super(ccMgr, tp, new AppExecQueue(threadPriority));
    }

    /**
     * Always returns <code>true</code> as the system context is eternal.
     * 
     * @return <code>true</code>
     */
    public boolean isAlive()
    {
        // system context is eternal
        return true;
    }

    /**
     * Does nothing.
     * 
     * @see org.cablelabs.impl.manager.CallerContext#checkAlive()
     */
    public void checkAlive()
    {
        // Does nothing
    }

    /**
     * Does nothing.  Only application contexts have AWT event queues
     * 
     * @see org.cablelabs.impl.manager.CallerContext#checkAlive()
     */
    public void runInContextAWT(Runnable run)
    {
        // Does nothing
    }

    /**
     * Always returns <code>true</code> as the system context is eternal.
     * 
     * @return <code>true</code>
     */
    public boolean isActive()
    {
        return true;
    }

    /**
     * Returns <code>null</code> as no context attributes are supported at this
     * time for <code>SystemContext</code>.
     * 
     * @return <code>null</code>
     */
    public Object get(Object key)
    {
        // Currently nothing's supported...
        return null;
    }

    /**
     * Simply returns the given <code>Properties</code> unchanged.
     * 
     * @param base
     *            the original system properties
     * @return the original system properties
     */
    public Properties getProperties(Properties base)
    {
        return base;
    }

    /**
     * Simply returns the default value (<i>def</i>).
     * 
     * @param key
     *            property to lookup
     * @param def
     *            original system property
     * @return the original system property
     */
    public String getProperty(String key, String def)
    {
        return def;
    }

    /**
     * Logs the uncaught exception against the system.
     * 
     * @param thread
     * @param throwable
     * 
     * @see AppManager#logErrorEvent(int, Throwable)
     * @see ErrorEvent#SYS_REC_GENERAL_ERROR
     */
    public void uncaughtException(Thread thread, Throwable throwable)
    {
        AppManager.logErrorEvent(ErrorEvent.SYS_REC_GENERAL_ERROR, throwable);
    }

    /**
     * Overrides {@link Object#toString()} to return a simpler respresentation
     * of this object.
     * 
     * @return a string representation of this object
     */
    public String toString()
    {
        return "SystemContext@" + System.identityHashCode(this);
    }

    /**
     * Creates and returns a new {@link ContextDemandExecQueue}.
     * 
     * @return a new <code>DemandExecQueue</code>
     */
    public TaskQueue createTaskQueue()
    {
        return new DemandExecQueue(tp, 750L);
    }

    /**
     * Dispose of the system context.
     * <p>
     * As the system context is not normally destroyed, this is really present
     * for testing only.
     */
    void dispose()
    {
        q.dispose();
    }

    /**
     * Does nothing, as we don't need to remember threads added/removed on to
     * the system context.
     */
    protected void addThread(Thread thread, Runnable run)
    {
        // Does nothing
    }

    /**
     * Does nothing, as we don't need to remember threads added/removed on to
     * the system context.
     */
    protected void removeThread(Thread thread)
    {
        // Does nothing
    }
}