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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyEventDispatcher;
import java.awt.KeyEventPostProcessor;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.cablelabs.impl.awt.KeyboardFocusManager;
import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.FocusManager.FocusContext;

/**
 * This class performs all of the standard duties required by the
 * java.awt.KeyboardFocusManager.
 * 
 * @author Greg Rutz
 */
public class KeyboardFocusManagerImpl extends KeyboardFocusManager
{
    /**
     * 
     * @param scene
     */
    public void addFocusContext(FocusContext fc)
    {
        if (!contexts.contains(fc)) contexts.add(fc);
    }

    /**
     * 
     * @param fc
     */
    public void removeFocusContext(FocusContext fc)
    {
        if (contexts.contains(fc)) contexts.remove(fc);
    }

    /**
     * Requests that the current focus owner release the focus
     */
    public void clearGlobalFocusOwner()
    {
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        FocusContext focus = fm.getFocusOwnerContext();
        if (focus != null)
        {
            focus.clearFocus();
            removeFocusContext(focus);
        }
    }

    /**
     * Handles the dispatching of certain AWT events
     * 
     * @param e
     * @return
     */
    public boolean dispatchEvent(AWTEvent e)
    {
        if (e instanceof FocusEvent)
        {
            FocusEvent fe = (FocusEvent) e;

            Object oldPropertyValue = null;
            Object newPropertyValue = null;

            // Update our internal focus state
            synchronized (this)
            {
                if (fe.getID() == FocusEvent.FOCUS_LOST)
                {
                    if (focusOwner == fe.getComponent())
                    {
                        oldPropertyValue = focusOwner;
                        focusOwner = null;

                        // If this is temporary focus change, our current focus
                        // owner becomes the permanent owner
                        if (fe.isTemporary())
                        {
                            permanentFocusOwner = fe.getComponent();
                        }
                    }
                    else if (permanentFocusOwner == fe.getComponent())
                    {
                        oldPropertyValue = permanentFocusOwner;
                        permanentFocusOwner = null;
                    }
                }
                else if (fe.getID() == FocusEvent.FOCUS_GAINED)
                {
                    // Set property values for change listeners
                    oldPropertyValue = focusOwner;
                    newPropertyValue = fe.getComponent();

                    focusOwner = fe.getComponent();
                }
            }

            // Redispatch the event to its component
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(fe.getComponent(), e);

            // Notify property listeners
            notifyPropertyChange("focusOwner", oldPropertyValue, newPropertyValue);

            return true;
        }

        return false;
    }

    /**
     * 
     * @param e
     * @return
     */
    public boolean dispatchKeyEvent(KeyEvent e)
    {
        return false;
    }

    /**
     * Transfer focus down from the given container to the next focus cycle
     * 
     * @param c
     *            the container from which to transfer the focus cycle
     */
    public void downFocusCycle(Container c)
    {
        if (c != null && c.isFocusCycleRoot())
        {
            c.transferFocusDownCycle();
        }
    }

    /**
     * Transfer focus to the next component in the current focus cycle from the
     * given component
     * 
     * @param c
     *            the component from which to transfer focus
     */
    public void focusNextComponent(Component c)
    {
        if (c != null)
        {
            c.transferFocus();
        }
    }

    /**
     * Transfer focus to the previous component in the current focus cycle
     * 
     * @param c
     *            the component from which to transfer focus
     */
    public void focusPreviousComponent(Component c)
    {
        if (c != null)
        {
            c.transferFocusBackward();
        }
    }

    /**
     * Returns the current focus cycle root
     * 
     * @return the current focus cycle root <code>Container</code> or null if
     *         not currently focused
     */
    public Container getCurrentFocusCycleRoot()
    {
        Component c = focusOwner;

        Container root = null;
        if (c != null)
        {
            // Search for the first Container parent of the focused component
            // that is the focus cycle root
            do
            {
                if (c instanceof Container)
                {
                    Container cont = (Container) c;
                    if (cont.isFocusCycleRoot())
                    {
                        root = cont;
                        break;
                    }
                }
                c = c.getParent();
            }
            while (c != null);
        }
        return root;
    }

    /**
     * 
     * @param id
     * @return
     */
    public Set getDefaultFocusTraversalKeys(int id)
    {
        return null;
    }

    /**
     * 
     * @return
     */
    public FocusTraversalPolicy getDefaultFocusTraversalPolicy()
    {
        return defaultFocusTraversalPolicy;
    }

    /**
     * Returns the current focus owner if held by the calling application
     * 
     * @return the current focus owner, or null if the calling application does
     *         not hold the focus
     */
    public Component getFocusOwner()
    {
        Component c = null;
        for (Iterator i = contexts.iterator(); i.hasNext();)
        {
            c = ((FocusContext) i.next()).getFocusOwner();
            if (c != null) break;
        }
        return c;
    }

    /**
     * 
     * @return
     */
    public Component getPermanentFocusOwner()
    {
        return permanentFocusOwner;
    }

    /**
     * 
     * @param e
     * @return
     */
    public boolean postProcessKeyEvent(KeyEvent e)
    {
        return false;
    }

    /**
     * 
     * @param focusedComponent
     * @param e
     */
    public void processKeyEvent(Component focusedComponent, KeyEvent e)
    {
    }

    /**
     * 
     * @param dispatcher
     */
    public void addKeyEventDispatcher(KeyEventDispatcher dispatcher)
    {
    }

    /**
     * 
     * @param dispatcher
     */
    public void removeKeyEventDispatcher(KeyEventDispatcher dispatcher)
    {
    }

    /**
     * 
     * @param processor
     */
    public void addKeyEventPostProcessor(KeyEventPostProcessor processor)
    {
    }

    /**
     * 
     * @param processor
     */
    public void removeKeyEventPostProcessor(KeyEventPostProcessor processor)
    {
    }

    /**
     * Add a <code>PropertyChangeListener</code> to the list of listeners
     * 
     * @param listener
     *            the listener to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener != null)
        {
            synchronized (this)
            {
                if (changeListeners == null)
                {
                    changeListeners = new PropertyChangeSupport(this);
                }
                changeListeners.addPropertyChangeListener(listener);
            }
        }
    }

    /**
     * Remove a <code>PropertyChangeListener</code> from the list of listeners
     * 
     * @param listener
     *            the listener to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener != null)
        {
            synchronized (this)
            {
                if (changeListeners != null)
                {
                    changeListeners.removePropertyChangeListener(listener);
                }
            }
        }
    }

    /**
     * Notifies all registered <code>PropertyChangeListener</code>s of a change
     * in the given property
     * 
     * @param propertyName
     *            the name of the property that has changed
     * @param oldValue
     *            the old property value
     * @param newValue
     *            the new property value
     */
    synchronized private void notifyPropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        if (changeListeners != null)
        {
            changeListeners.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Add a <code>VetoableChangeListener</code> to the list of listeners
     * 
     * @param listener
     *            the listener to be added
     */
    public void addVetoableChangeListener(VetoableChangeListener listener)
    {
        if (listener != null)
        {
            synchronized (this)
            {
                if (vetoableListeners == null)
                {
                    vetoableListeners = new VetoableChangeSupport(this);
                }
                vetoableListeners.addVetoableChangeListener(listener);
            }
        }
    }

    /**
     * Remove a <code>PropertyChangeListener</code> from the list of listeners
     * 
     * @param listener
     *            the listener to be removed
     */
    public void removeVetoableChangeListener(VetoableChangeListener listener)
    {
        if (listener != null)
        {
            synchronized (this)
            {
                if (vetoableListeners != null)
                {
                    vetoableListeners.removeVetoableChangeListener(listener);
                }
            }
        }
    }

    /**
     * Notifies all registered <code>PropertyChangeListener</code>s of a change
     * in the given property. An exception is thrown if one of the listeners has
     * vetoed the property change.
     * 
     * @param propertyName
     *            the name of the property that has changed
     * @param oldValue
     *            the old property value
     * @param newValue
     *            the new property value
     * @throws PropertyVetoException
     *             if a listener vetoes the change
     */
    synchronized private void notifyVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException
    {
        if (vetoableListeners != null)
        {
            vetoableListeners.fireVetoableChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Called to initiate a focus change
     * 
     * @param c
     *            the <code>Component</code> that is requesting the focus
     * @param temporary
     *            true if this is a temporaray focus request, false otherwise
     * @return true if the request is likely to succeed, false if the request is
     *         guaranteed to fail
     */
    public boolean requestFocus(Component c, boolean temporary)
    {
        // Notify vetoable change listeners
        synchronized (this)
        {
            try
            {
                notifyVetoableChange("focusOwner", focusOwner, c);
            }
            catch (PropertyVetoException e)
            {
                return false;
            }
        }

        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        fm.requestFocus(c, temporary);
        return true;
    }

    /**
     * 
     * @param id
     * @param keystrokes
     */
    public void setDefaultFocusTraversalKeys(int id, Set keystrokes)
    {
    }

    /**
     * 
     * @param policy
     */
    public void setDefaultFocusTraversalPolicy(FocusTraversalPolicy policy)
    {
        defaultFocusTraversalPolicy = policy;
    }

    /**
     * 
     * @param c
     */
    public void upFocusCycle(Component c)
    {
    }

    // Change listeners
    private VetoableChangeSupport vetoableListeners;

    private PropertyChangeSupport changeListeners;
    
    FocusTraversalPolicy defaultFocusTraversalPolicy = new DefaultFocusTraversalPolicy();

    // List of FocusContexts that our application owns
    Vector contexts = new Vector();

    // Permanent focus owner (set when a temporary focus request is made)
    Component permanentFocusOwner = null;

    // Focus owner
    Component focusOwner = null;
}
