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

import org.apache.log4j.Logger;
import org.dvb.application.AppID;

/**
 * An <code>ExecQueue</code> used as the central execution queue for an
 * <code>AppContext</code>.
 * 
 * @author Aaron Kamienski
 */
class AppExecQueue extends ExecQueue
{
    /**
     * Create an instance of <code>AppExecQueue</code> for handling serialized
     * tasks for a specific application.
     * 
     * @param id
     *            the unique identifier for the app in question
     * @param cl
     *            the application class loader
     * @param tg
     *            thread group the thread group to use for any new threads
     * @return an instance of <code>AppExecQueue</code>
     */
    static AppExecQueue createInstance(AppID id, ThreadGroup tg)
    {
        return new AppExecQueue(id, tg);
    }

    /**
     * Create an instance of <code>AppExecQueue</code> for handling serialized
     * tasks for system purposes.
     * 
     * @param priority
     *            thread priority
     * 
     * @return an instance of <code>AppExecQueue</code>
     */
    static AppExecQueue createInstance(int priority)
    {
        return new AppExecQueue(priority);
    }

    /**
     * Creates an instance of AppExecQueue for non-application purposes. Creates
     * an <code>ExecThread</code> that will serially execute tasks posted to
     * this queue. The thread will be named <code>"System-"+<i>i</i></code>
     * where <i>i</i> is an integer value.
     * 
     * @param priority
     *            thread priority
     */
    AppExecQueue(int priority)
    {
        thread = new Thread(newWorker(), "System-" + nextInt());
        thread.setPriority(priority);
        thread.start();
    }

    /**
     * Creates an instance of AppExecQueue for application-specific purposes.
     * Creates an <code>ExecThread</code> that will serially execute taks posted
     * to this queue. The thread will be named based on the <code>AppID</code>.
     * 
     * @param id
     *            the unique identifier for the app in question
     * @param cl
     *            the application class loader
     * @param tg
     *            thread group the thread group to use for any new threads
     */
    AppExecQueue(AppID id, ThreadGroup tg)
    {
        thread = new Thread(tg, newWorker(), "App-" + id + "-" + nextInt());
        // Used to set the context class loader...
        // However, AFAIK, the only place that this is used is the
        // java.awt.EventDispatchThread
        // to call a handler.
        // thread.setContextClassLoader(cl);
        thread.start();
    }

    private WorkerTask newWorker()
    {
        return new WorkerTask(this, false, true)
        {
            public void run()
            {
                super.run();
                if (log.isInfoEnabled())
                {
                    log.info("AppExecQueue finished...");
                }
            }

            public boolean handleThrowable(Throwable e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Exception in AppExecQueue", e);
                }
                // return super.handleThrowable(e);
                return false;
            }
        };
    }

    private static synchronized int nextInt()
    {
        return ++nextInt;
    }

    /**
     * The execution thread for this <code>AppExecQueue</code>.
     */
    private Thread thread;

    private static int nextInt = 0;

    /** Log4J Logger. */
    private static final Logger log = Logger.getLogger(AppExecQueue.class.getName());
}
