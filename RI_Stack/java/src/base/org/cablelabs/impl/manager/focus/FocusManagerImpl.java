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

package org.cablelabs.impl.manager.focus;

import java.awt.Component;

import org.cablelabs.impl.awt.KeyboardFocusManager;
import org.cablelabs.impl.awt.KeyboardFocusManagerFactory;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;

import org.apache.log4j.Logger;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InvocationEvent;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class FocusManagerImpl extends KeyboardFocusManagerFactory implements FocusManager
{
    /**
     * Private constructor. Instances should be created via {@link #getInstance}
     * .
     */
    protected FocusManagerImpl()
    {
    }

    /**
     * Returns the singleton instance of the <code>FocusManager</code>. Intended
     * to be called by the {@link org.cablelabs.impl.manager.ManagerManager
     * ManagerManager} only and not called directly.
     * 
     * @return the singleton instance of the <code>FocusManager</code>.
     * @see org.cablelabs.impl.manager.ManagerManager#getInstance(Class)
     */
    public static Manager getInstance()
    {
        return new FocusManagerImpl();
    }

    /**
     * <p>
     * This method is implemented as follows:
     * <ol>
     * <li>The {@link RootContainer} that contains (or is) the requesting
     * <code>Component</code> is located.
     * <li>If located, then {@link RootContainer#handleRequestFocus(Component)}
     * is invoked.
     * </ol>
     * 
     * This method must be called from the application that owns the component
     * ONLY
     * 
     * @param c
     *            the component requesting focus
     * @return <code>true</code> indicating that this focus handler handled the
     *         request
     */
    public void requestFocus(Component c, boolean temporary)
    {
        // Find parent RootContainer
        RootContainer root = null;
        Component parent = c;
        while (parent != null)
        {
            if (parent instanceof RootContainer)
            {
                root = (RootContainer) parent;
                break;
            }
            parent = parent.getParent();
        }

        // If root container found, the post event
        if (root != null)
        {
            // This request is synchronous...
            // (Any subsequent activation request will be asynchronous)
            root.handleRequestFocus(c, temporary);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Ignoring focus request from orphan " + c);
            }
    }
    }

    /**
     * Implements
     * {@link FocusManager#notifyDeactivated}.
     * <p>
     * This method is implemented as follows:
     * <ul>
     * <li>The given context is located in the <i>activable</i> list.
     * <li>If it was at the head of the list, and currently focused, then
     * {@link FocusContext#notifyDeactivated()} will be invoked. After which, no
     * component will currently have focus.
     * <li>A request to restore focus to the next activable context is queued up
     * via the AWT event queue to occur asynchronously.
     * </ul>
     * 
     * @param context
     *            the context which is notifying us of its own deactivation
     */
    public void notifyDeactivated(FocusContext context)
    {
        boolean wasFocused = false;
        synchronized (activable)
        {
            // Remove from the list of activable contexts
            if (activable.removeElement(context))
            {
                // Ensure that this focus context has been removed from the list
                // for this application's KeyboardFocusManager
                KeyboardFocusManagerImpl kfm = (KeyboardFocusManagerImpl) getKeyboardFocusManager();
                kfm.removeFocusContext(context);

                if (log.isDebugEnabled())
                {
                    log.debug("Deactivated " + context);
                }

                // if currently focused, will send deactivate event
                if (context == focused)
                {
                    wasFocused = true;
                }
            }
        }

        // Don't hold lock while invoking oldContext...
        if (wasFocused)
        {
            // restore focus to next activable component (asynchronously)
            runLater(new Runnable()
            {
                public void run()
                {
                    activateSync();
                }
            });
        }
    }

    /**
     * Implements
     * {@link org.cablelabs.impl.manager.FocusManager#requestActivate}.
     * <p>
     * The method is implemented as follows:
     * <ol>
     * <li>The given <i>context</i> is added to the <i>activable</i> list, and
     * made eligible to receive focus.
     * <li>If <i>focusRequired</i> is <code>true</code> then a request to move
     * the given <i>context</i> to the head of the <i>activable</i> list and
     * give it focus is queued up via the AWT event queue.
     * </ol>
     * 
     * @param context
     *            the <code>FocusContext</code> requesting to be made activable
     * @param focusRequested
     *            if <code>true</code> then the <i>context</i> is requesting to
     *            be moved to the front of the <i>activable</i> list
     */
    public void requestActivate(final FocusContext context, boolean focusRequested)
    {
        boolean focusGranted = false;

        boolean hadFocus = false;

        // Add the context to the end of the activable list synchronously
        synchronized (activable)
        {
            if (!activable.contains(context))
            {
                // Ensure that this focus context has been added to the list
                // for this application's KeyboardFocusManager
                KeyboardFocusManagerImpl kfm = (KeyboardFocusManagerImpl) getKeyboardFocusManager();
                kfm.addFocusContext(context);

                if (log.isDebugEnabled())
                {
                    log.debug("Now activable " + context);
                }
            }
            else
            {
                // Remove context so that it can be re-inserted at the
                // front of the list
                                        
                FocusContext fc = (FocusContext)activable.get(0);
                if (fc == context)
                {
                    hadFocus = true;
                }
                activable.removeElement(context);
            }

            // Add context to list according to priority
            int priority = context.getPriority();
            
            // The activable list is the list of scenes that are eligible
            // to receive the input focus.  The closer a scene is to the
            // front of the list, the sooner it is eligible to receive
            // focus. The list is further divided by focus priority. 
            
            // If a request for focus has been made, we want to move this
            // context as far forward in the list as priority will allow.
            int i;
            if (focusRequested)
            {
                for (i = 0; i < activable.size(); i++)
                {
                    FocusContext fc = (FocusContext)activable.get(i);
                    if (priority >= fc.getPriority())
                    {
                        break;
                    }
                }
                if (i == 0)
                {
                    focusGranted = true;
                }
            }
            // However, if focus has not been requested, we need to add
            // the context at the rear of its priority group within the list
            else
            {
                if (hadFocus)
                {
                    i=0;
                }
                else
                {
                    for (i = (activable.size()-1); i >= 0; i--)
                    {
                        FocusContext fc = (FocusContext)activable.get(i);
                        if (priority >= fc.getPriority())
                        {
                            break;
                        }
                    }
                    i++; // Insert after
                }
            }
            activable.insertElementAt(context, i); // Insert before
        }

        // Request focus asynchronously
        if (focusGranted)
        {
            runLater(new Runnable()
            {
                public void run()
                {
                    activateSync();
                }
            });
        }
    }

    /**
     * Creates focus state such that no <code>Component</code> has focus.
     * Remembers which <code>Component</code> has focus when called, and that
     * same <code>Component</code> will be restored as having focus when
     * <code>restoreFocus() </code> is called
     */

    public void suspendFocus()
    {
        boolean updateRequired = false;
        synchronized (activable)
        {
            if (!focusSuspended)
            {
                focusSuspended = true;
                updateRequired = true;
            }
        }

        if (updateRequired)
        {
            runLater(new Runnable()
            {
                public void run()
                {
                    activateSync();
                }
            });
        }
    }

    /**
     * Restores the state of the focus system to state that existed at the point
     * that the last call to <code>restoreFocus() was made
     */

    public void restoreFocus()
    {
        boolean updateRequired = false;
        synchronized (activable)
        {
            if (focusSuspended)
            {
                focusSuspended = false;
                updateRequired = true;
            }
        }

        if (updateRequired)
        {
            runLater(new Runnable()
            {
                public void run()
                {
                    activateSync();
                }
            });
        }
    }

    /**
     * Implements the portion of {@link #requestActivate} that actually gives
     * focus to a <code>FocusContext</code>.
     * <p>
     * 
     * Gives the focus to the <i>context</i> at the front of the
     * <i>activable</i> list. The current focus owner, if any, is notified of
     * the loss of focus via {@link FocusContext#notifyDeactivated} and then the
     * new focus owner is notified of the gain of focus via
     * {@link FocusContext#notifyActivated}.
     * 
     * @see #requestActivate(FocusContext, boolean)
     */
    private void activateSync()
    {
        FocusContext oldFocus = null;
        FocusContext newFocus = null;

        synchronized (activable)
        {
            if (!focusSuspended && !activable.isEmpty())
            {
                newFocus = (FocusContext) activable.elementAt(0);
            }

            if (focused != newFocus)
            {
                oldFocus = focused;
                focused = null;
            }
            // Note: if focused == newFocus (i.e., the focused context
            // isn't changing), newFocus.notifyActivated() still needs
            // to be called because the focus may be moving between
            // components within the context.
        }

        // Send events
        if (oldFocus != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("focus removed from " + oldFocus);
            }
            oldFocus.notifyDeactivated();
        }
        if (newFocus != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("focus given to " + newFocus);
            }
            newFocus.notifyActivated();
        }
        
        // Now that we have notified our listeners, officially update the focus owner
        synchronized (activable)
        {
            focused = newFocus;
        }
    }

    /**
     * Implements {@link FocusManager#getFocusOwnerContext()}
     */
    public FocusContext getFocusOwnerContext()
    {
        synchronized (activable)
        {
            return focused;
        }
    }

    /**
     * Runs the given <code>Runnable</code> asynchronously using the AWT event
     * queue. This posts a new <code>ActiveEvent</code> to the AWT event queue.
     * This can be used to ensure that something is executed from the AWT event
     * queue dispatch thread (later).
     * 
     * @param run
     *            the runnable to execute
     */
    private void runLater(Runnable run)
    {
        getEventQueue().postEvent(new InvocationEvent(run, run));
    }

    /**
     * Returns the AWT <code>EventQueue</code>.
     * 
     * @return the AWT <code>EventQueue</code>.
     */
    private EventQueue getEventQueue()
    {
        return (EventQueue) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return Toolkit.getDefaultToolkit().getSystemEventQueue();
            }
        });
    }

    /**
     * Common method used to determine the CallerContext. This dynamically
     * determines the CallerContextManager, rather than accessing a single
     * instance variable, to enable testing.
     * 
     * @return the CallerContextManager
     */
    private static CallerContext getContext()
    {
        CallerContextManager cm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        return cm.getCurrentContext();
    }

    public void destroy()
    {
        // TODO Auto-generated method stub

    }

    /**
     * Implements {@link KeyboardFocusManagerFactory#getKeyboardFocusManager}
     */
    public KeyboardFocusManager getKeyboardFocusManager()
    {
        return getData(getContext()).getKeyboardFocusManager();
    }

    /**
     * Holds context-specific data. Specifically, the KeyboardFocusManager
     * implementation for each application
     */
    private class Data implements CallbackData
    {
        public KeyboardFocusManagerImpl getKeyboardFocusManager()
        {
            return kfm;
        }

        public void destroy(CallerContext ctx)
        {
        }

        public void active(CallerContext ctx)
        {
        }

        public void pause(CallerContext ctx)
        {
        }

        private KeyboardFocusManagerImpl kfm = new KeyboardFocusManagerImpl();
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
    private Data getData(CallerContext ctx)
    {
        synchronized (lock)
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

    private Object lock = new Object();

    /**
     * The list of currently <i>activable</i> components. If one is currently
     * activated (i.e., focused) then it is the first element in the list.
     * 
     * @see #focused
     */
    private Vector activable = new Vector();

    /**
     * The currently focused <code>FocusContext</code>. If <code>null</code>
     * then no <code>FocusContext</code> is currently activated.
     * 
     * @see #activable
     */
    private FocusContext focused = null;

    private boolean focusSuspended = false;

    /** Log4J Logger. */
    private static final Logger log = Logger.getLogger(FocusManagerImpl.class.getName());
}
