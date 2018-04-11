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

package org.cablelabs.impl.manager.timer;

import org.cablelabs.impl.manager.TimerManager;
import org.cablelabs.impl.util.JavaVersion;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.CallbackData.SimpleData;
import javax.tv.util.TVTimer;

/**
 * This abstract implementation of <code>TimerManager</code> simply provides the
 * requisite <code>Manager</code> <code>getInstance</code> method.
 * 
 * @see TimerMgrPJava
 * @see TimerMgrJava2
 * 
 * @author Aaron Kamienski
 */
public abstract class TimerMgr implements TimerManager
{
    /**
     * Not publicly instantiable.
     */
    protected TimerMgr()
    {
    }

    /**
     * Destroys this manager, causing it to release any and all resources.
     * Should only be called by the <code>ManagerManager</code>.
     */
    public void destroy()
    {
    }

    /**
     * Returns the singleton instance of TimerManager. Will be called only once.
     * <p>
     * The implementation is different for different platforms. Currently two
     * implementations are supported:
     * <ol>
     * <li>TimerMgrPJava
     * <li>TimerMgrJava2
     * </ol>
     * 
     * @return the singleton instance
     */
    public static synchronized Manager getInstance()
    {
        if (mgr == null)
        {
            String name = null;
            if (JavaVersion.JAVA_2)
                name = "Java2";
            else if (JavaVersion.PJAVA_12)
                name = "PJava";
            else
                return null;

            try
            {
                Class cl = Class.forName("org.cablelabs.impl.manager.timer.TimerMgr" + name);
                mgr = (TimerManager) cl.newInstance();
            }
            catch (ClassNotFoundException cnfe)
            {
                return null;
            }
            catch (InstantiationException ie)
            {
                return null;
            }
            catch (IllegalAccessException iae)
            {
                return null;
            }
        }
        return mgr;
    }

    /**
     * Returns the default timer for the calling context.
     * 
     * @return a non-null TVTimer object
     */
    public TVTimer getTimer()
    {
        return getTimer(ccm.getCurrentContext());
    }

    /**
     * Returns the default timer for the given calling context. If a default
     * timer instance does not currently exist for the calling context, then one
     * will be created.
     * 
     * @param ctx
     *            the calling context
     * @return a non-null TVTimer object
     */
    public synchronized TVTimer getTimer(CallerContext ctx)
    {
        final Class key = getClass();
        SimpleData data = (SimpleData) ctx.getCallbackData(key);
        TVTimer timer = null;
        if (data == null || (timer = (TVTimer) data.getData()) == null)
        {
            timer = createTimer(ctx);
            data = new SimpleData(timer)
            {
                public void destroy(CallerContext ctx)
                {
                    ctx.removeCallbackData(key);
                    disposeTimer(ctx, (TVTimer) getData());
                }
            };
            ctx.addCallbackData(data, key);
        }
        return timer;
    }

    /**
     * Creates and returns a new <code>TVTimer</code> instance for the given
     * <code>CallerContext</code>.
     * 
     * @param ctx
     *            the calling context
     * @return a non-null TVTimer object
     */
    protected abstract TVTimer createTimer(CallerContext ctx);

    /**
     * Disables and disposes of the given <code>TVTimer</code> for the given
     * <code>CallerContext</code>. Should ensure that currently scheduled
     * <code>TVTimerSpec</code>s are descheduled.
     * 
     * @param ctx
     *            the calling context
     * @param timer
     *            the <code>TVTimer</code> to deschedule
     */
    protected abstract void disposeTimer(CallerContext ctx, TVTimer timer);

    /**
     * Reference to the singleton CallerContextManager.
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * The singleton instance.
     */
    private static TimerManager mgr;
}
