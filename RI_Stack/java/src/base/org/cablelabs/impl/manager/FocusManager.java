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

package org.cablelabs.impl.manager;

import java.awt.AWTEvent;
import java.awt.Component;

import org.cablelabs.impl.manager.focus.KeyboardFocusManagerImpl;

/**
 * The <code>FocusManager</code> is a <code>Manager</code> whose responsibility
 * is to manage the event focus <i>between</i> application root containers.
 * <p>
 * At any one time at most one AWT {@link java.awt.Component Component} may have
 * focus -- that component will receive all input
 * {@link java.awt.event.KeyEvent KeyEvent}s not reserved for
 * {@link org.ocap.event.EventManager#addExclusiveAccessToAWTEvent exclusive}
 * access. Each application {@link org.havi.ui.HScene scene} (one per
 * application per graphics device) will maintain it's own focus owner
 * <code>Component</code>; it is the job of this manager to manage focus between
 * application scenes.
 * <p>
 * Among it's responsibilities are:
 * <ul>
 * <li>Maintain a reference to the currently focused application. This is done
 * by maintaining access to the currently focused application's
 * {@link FocusContext}.
 * <li>Determine which focus should receive focus when the focused application/
 * <code>Component</code> gives up focus directly.
 * </ul>
 * 
 * @see org.cablelabs.impl.manager.ManagerManager
 * @see org.cablelabs.impl.manager.EventManager
 * @see FocusContext
 * 
 * @author Aaron Kamienski
 * @author Greg Rutz
 */
public interface FocusManager extends Manager
{
    /**
     * Requests that the root container represented by the given
     * <code>FocusContext</code> be activated. If focus is given to this
     * <i>context</i> then the following will occur:
     * <ul>
     * <li> {@link FocusContext#notifyActivated} will be invoked on the currently
     * focused <code>FocusContext</code>, if there is any
     * <li> {@link FocusContext#notifyDeactivated} will be invoked on the given
     * <code>FocusContext</code>
     * </ul>
     * 
     * This method should only be called by a root container (e.g., an
     * <code>HScene</code>) when it is eligible for focus. Within OCAP, an
     * <code>HScene</code> is eligible for focus if and only if the following
     * are all <code>true</code>:
     * <ul>
     * <li>it is {@link org.havi.ui.HScene#isVisible visible}
     * <li>it is {@link org.havi.ui.HScene#setActive active}
     * <li>focus has been {@link java.awt.Component#requestFocus} by it or a
     * sub-component at least once
     * <li>the owning application is in the <i>Active</i> state
     * </ul>
     * 
     * This may be invoked when focus is not explicitly desired, but the
     * component simply wants to be considered eligible to receive focus again.
     * For example if the root container was recently made visible again. In
     * such cases, the <i>focus</i> parameter should be <code>false</code>.
     * 
     * @param context
     *            focus context for the application root container desiring
     *            focus
     * @param focus
     *            if <code>true</code> then focus is actively being requesed; if
     *            <code>false</code> then request it simply to be made
     *            <i>activable</i> (again)
     */
    public void requestActivate(FocusContext context, boolean focus);

    /**
     * Notifies the <code>FocusManager</code> that the given
     * <code>FocusContext</code> should no longer be considered eligible to
     * receive focus. This method does nothing if the given context was not
     * previously eligible for focus.
     * <p>
     * If the given context is the currently activated context, then it will
     * lose focus and focus will be given to the next eligible context. If the
     * given context is not the currently focused context, but is otherwise
     * eligible for focus, it will simply become ineligible for focus.
     * 
     * <p>
     * This method should be called by a root container (e.g., an
     * <code>HScene</code>) when it should be made ineligible for focus. Within
     * OCAP, an <code>HScene</code> is ineligible for focus if at least one of
     * the following is true:
     * <ul>
     * <li>it is not {@link org.havi.ui.HScene#isVisible visible}
     * <li>it is not {@link org.havi.ui.HScene#setActive active}
     * <li>the app is not in an Active
     * {@link org.dvb.application.AppProxy#getState state}
     * </ul>
     * 
     * @param context
     *            focus context for the application root container that is
     *            releasing itself of focus
     */
    public void notifyDeactivated(FocusContext context);

    /**
     * Initiates a focus request by the given target <code>Component</code>.
     * Must only be called by the application that owns the component
     * 
     * @param target
     *            the target <code>Component</code>
     * @param temporary
     *            true if this is a temporary focus request, false otherwise
     */
    public void requestFocus(Component target, boolean temporary);

    /**
     * Returns the <code>FocusContext</code> that currently holds the focus
     * 
     * @return the focus owner, or null if no context currently holds the focus
     */
    public FocusContext getFocusOwnerContext();

    /**
     * Creates focus state such that no <code>Component</code> has focus.
     * Remembers which <code>Component</code> has focus when called, and that
     * same <code>Component</code> will be restored as having focus when
     * <code>restoreFocus() </code> is called
     */

    public void suspendFocus();

    /**
     * Restores the state of the focus system to state that existed at the point
     * that the last call to <code>restoreFocus() was made
     */

    public void restoreFocus();

    /**
     * An interface representing the <i>focus context</i> for an application
     * root container. An instance of this interface is provided to the
     * <code>FocusManager</code> when focus is
     * {@link FocusManager#requestActivate requested}. The same instance may
     * also be provided later when focus is to be
     * {@link FocusManager#notifyDeactivated released}.
     * <p>
     * The <code>FocusManager</code> will notify the application root container
     * that it has {@link #notifyActivated gained} or {@link #notifyDeactivated
     * lost} focus via this interface.
     * 
     * @see FocusManager
     */

    public static interface FocusContext
    {
        /**
         * Invoked when this <code>FocusContext</code> is activated (i.e., made
         * the focused context).
         * <p>
         * This will be called in response to
         * {@link FocusManager#requestActivate}.
         */
        public void notifyActivated();

        /**
         * Invoked when this <code>FocusContext</code> is deactivated (i.e.,
         * loses focus).
         * <p>
         * This will be called when focus is withdrawn either in response to
         * {@link FocusManager#notifyDeactivated} or as a result of focus being
         * given to another <code>FocusContext</code>.
         */
        public void notifyDeactivated();

        /**
         * Indicates low focus priority.
         */
        public static final int PRIORITY_LOW = 3;

        /**
         * Indicates normal focus priority.
         */       
        public static final int PRIORITY_NORMAL = 5;

        /**
         * Indicates high focus priority.
         */       
        public static final int PRIORITY_HIGH = 7;

        /**
         * Returns the focus priority of this <code>Component</code>.
         * <code>Component</code>s cannot take focus away from another 
         * <code>Component</code> with higher focus priority.
         * @return Focus priority level
         */
        public int getPriority();
            
        /**
         * Invoked on the currently activated (i.e., focused)
         * <code>FocusContext</code> to dispatch the given event to the proper
         * <i>focus owner</i> (i.e., the <code>Component</code> that most
         * recently called {@link Component#requestFocus}).
         * 
         * @param e
         *            the event to dispatch (the <i>source</i> should be set to
         *            reference the proper focus owner)
         * @param filter
         *            dispatch filter callback that should be invoked with the
         *            owning <code>CallerContext</code> to decide if the event
         *            should be dispatched or not
         * @param interestFilter
         *            if <code>true</code> then the <code>FocusContext</code>
         *            should filter the event such that only those that the
         *            context is interested in are dispatched; if
         *            <code>false</code> then all events should be dispatched
         */
        public void dispatchEvent(AWTEvent e, DispatchFilter filter, boolean interestFilter);

        /**
         * Requests this <code>RootContainer</code> to clear its current focus
         * owner. This causes a <code>FocusEvent.FOCUS_LOST</code> event to be
         * dispatched to the focused <code>Component</code> but the window will
         * not be deactivated. If this context does not own the focus, this
         * method does nothing,
         */
        public void clearFocus();

        /**
         * Returns the currently focused <code>Component</code> owned by this
         * container.
         * 
         * @return the currently focused <code>Component</code> or null if not
         *         focused
         */
        public Component getFocusOwner();
    }

    /**
     * A <code>DispatchFilter</code> may be given to a <code>FocusContext</code>
     * on a {@link FocusContext#dispatchEvent} call. The
     * <code>DispatchFilter</code> should be used to determine if the event
     * should be dispatched by the <code>FocusContext</code> or not.
     * 
     * @author Aaron Kamienski
     */
    public static interface DispatchFilter
    {
        /**
         * This method should be called by a <code>FocusContext</code> as part
         * of the implementation of {@link FocusContext#dispatchEvent}. If
         * <code>true</code> is returned, then the event should be dispatched.
         * If <code>false</code> is returned, then the event should not be
         * dispatched.
         * <p>
         * This is provided by the caller of <code>dispatchEvent()</code> for
         * the proper implementation of OCAP event delivery semantics. It takes
         * into account both
         * {@link org.dvb.event.EventManager#addExclusiveAccessToAWTEvent
         * exclusive} event ownership and event
         * {@link org.ocap.event.EventManager#setFilteredRepository filtering}.
         * 
         * @param cc
         *            the target <code>CallerContext</code>
         * @return <code>true</code> if the event should be dispatched;
         *         <code>false</code> if the event should be ignored
         */
        public boolean accept(CallerContext cc);
    }

    /**
     * An interface that must be implemented by an application root container
     * (e.g., the {@link org.havi.ui.HScene} implementation) to support focus
     * requests
     * <p>
     * This interface serves several purposes:
     * <ul>
     * <li>As a tagging interface, it allows the implementation to find the root
     * container of a component.
     * <li>Once the root container is found, {@link #handleRequestFocus} can be
     * invoked to pass along a component {@link Component#requestFocus focus
     * request}.
     * <li>The root container can provide focus state information regarding its
     * contained <code>Component<code>s
     * 
     * @author Aaron Kamienski
     * @author Greg Rutz
     */
    public static interface RootContainer
    {
        /**
         * The <code>RootContainer</code> should do the following upon
         * invocation of this method:
         * <ul>
         * <li>Remember the component currently requesting focus.
         * <li>Request activation (if appropriate) via
         * {@link FocusManager#requestActivate}
         * 
         * @param c
         *            component requesting focus that is contained within this
         *            root container
         * @param temporary
         *            indicates whether or not this is a request for temporary
         *            focus
         */
        public void handleRequestFocus(Component c, boolean temporary);
    }
}
