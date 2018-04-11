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

package org.cablelabs.impl.manager.environment;

import org.ocap.environment.EnvironmentEvent;
import org.ocap.environment.EnvironmentListener;
import org.ocap.environment.EnvironmentState;
import org.ocap.environment.EnvironmentStateChangedEvent;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.util.EventMulticaster;

/**
 * A base class to implement common functionality that any
 * 
 * @author koldinger
 */
public abstract class EnvironmentImplBase extends ExtendedEnvironment
{
    /**
     * Add a listener for environment events.
     * 
     * @param l
     *            the listener to add
     */
    public void addEnvironmentListener(EnvironmentListener l)
    {

        CallerContext context = m_ccm.getCurrentContext();

        synchronized (m_sync)
        {
            // Listeners are maintained in-context
            Data data = getData(context);

            // Update listener/multicaster
            data.envListeners = EventMulticaster.add(data.envListeners, l);

            // Manage context/multicaster
            m_contexts = Multicaster.add(m_contexts, context);
        }
    }

    /**
     * Remove a listener for environment events.
     * 
     * @param l
     *            the listener to remove
     */
    public void removeEnvironmentListener(EnvironmentListener l)
    {
        CallerContext context = m_ccm.getCurrentContext();

        synchronized (m_sync)
        {
            // Listeners are maintained in-context
            Data data = getData(context);

            // Update listener/multicaster
            data.envListeners = EventMulticaster.remove(data.envListeners, l);

            // Manage context/multicaster
            m_contexts = Multicaster.remove(m_contexts, context);
        }
    }

    /**
     * Holds context-specific data. Specifically the set of
     * <code>NetworkInterfaceListener</code>s.
     */
    private class Data implements CallbackData
    {
        public EnvironmentListener envListeners;

        public void destroy(CallerContext cc)
        {
            removeListeners(cc);
        }

        public void active(CallerContext cc)
        { /* empty */
        }

        public void pause(CallerContext cc)
        { /* empty */
        }
    }

    /**
     * Remove all listeners associated with this <code>CallerContext</code>.
     * This is done simply by setting the reference to <code>null</code> and
     * letting the garbage collector take care of the rest.
     * 
     * @param c
     *            the <code>CallerContext</code> to remove
     */
    private void removeListeners(CallerContext c)
    {
        synchronized (m_sync)
        {
            c.removeCallbackData(this);
            m_contexts = Multicaster.remove(m_contexts, c);
        }
    }

    /**
     * Access this device's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     * 
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    protected Data getData(CallerContext ctx)
    {
        synchronized (m_sync)
        {
            Data data = (Data) ctx.getCallbackData(this);
            if (data == null)
            {
                data = new Data();
                ctx.addCallbackData(data, this);
            }
            return data;
        }
    }

    /**
     * Send an event to all event listeners.
     * 
     * @param e
     *            The event to send.
     */
    protected void sendEvent(EnvironmentEvent e)
    {
        final EnvironmentEvent fe = e;
        synchronized (m_sync)
        {
            // BUG? Run in context fires off an asynchronous routine
            // Is there any risk of the m_contexts "list" changing due to add or
            // remove listener
            // while this runs?
            // Current thought says no, but should check carefully. How do other
            // users do this?
            if (m_contexts != null)
            {
                m_contexts.runInContext(new Runnable()
                {
                    public void run()
                    {
                        CallerContext context = m_ccm.getCurrentContext();
                        if (context != null)
                        {
                            Data d = getData(context);
                            if (d != null && d.envListeners != null)
                            {
                                d.envListeners.notify(fe);
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * Create and send a state changed event to all listeners.
     * 
     * @param fromState
     *            The state this Environment is transitioning from.
     * @param toState
     *            The state this Environment is transitioning to.
     */
    protected void sendStateEvent(EnvironmentState fromState, EnvironmentState toState)
    {
        EnvironmentStateChangedEvent e = new EnvironmentStateChangedEvent(this, fromState, toState);
        sendEvent(e);
    }

    private Object m_sync = new Object();

    private CallerContextManager m_ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private CallerContext m_contexts = null;
}
