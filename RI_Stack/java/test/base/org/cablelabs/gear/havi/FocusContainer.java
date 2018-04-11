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

package org.cablelabs.gear.havi;

import org.havi.ui.HState;
import org.havi.ui.HNavigable;
import org.havi.ui.HContainer;
import org.havi.ui.HSound;
import org.havi.ui.event.HFocusEvent;
import org.havi.ui.event.HFocusListener;
import org.havi.ui.event.HKeyEvent;

import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.AWTEventMulticaster;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.FocusEvent;

/**
 * FocusContainer is an extension of <code>HContainer</code> which implements
 * the <code>HNavigable</code> interface. This class is useful for simplifying
 * component traversals; either explicit or with the use of a layout manager
 * (e.g., {@link SnapLayout}).
 * 
 * When this container receives focus, it simply transfers that focus to the
 * appropriate sub-component. The <i>appropriate</i> sub-component is determined
 * one of two ways:
 * 
 * <ol>
 * <li>The first <code>HNavigable</code> component that was added to the
 * container that is {@link java.awt.Component#isVisible() visible} and
 * {@link java.awt.Component#isFocusTraversable() traversable}, or...
 * <li>The <code>HNavigable</code> component set using the
 * {@link #setFirst(HNavigable)} method.
 * </ol>
 * 
 * In cases where focus cannot be transferred to a contained component, this
 * container will simply maintain focus.
 * 
 * @author Aaron Kamienski
 * @author Jeff Bonin
 * @version $Id: FocusContainer.java,v 1.11 2002/06/03 21:33:15 aaronk Exp $
 */
public class FocusContainer extends HContainer implements HNavigable, HState
{

    /**
     * Creates <code>FocusContainer</code> object with x/y coordinates and
     * width/height dimensions initialized to their default values.
     */
    public FocusContainer()
    {
        super();
        iniz();
    }

    /**
     * Creates <code>FocusContainer</code> object with x/y coordinates and
     * width/height dimensions initialized to the given values.
     * 
     * @param x
     *            initial x-coordinate
     * @param y
     *            initial y-coordinate
     * @param width
     *            initial width
     * @param height
     *            initial height
     */
    public FocusContainer(int x, int y, int width, int height)
    {
        super(x, y, width, height);
        iniz();
    }

    /** Common to all constructors. */
    private void iniz()
    {
        moves = new KeySet();

        // Only should be necessary if TRANS_EVENTS==true
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    /**
     * Returns the internal sizing of this container.
     * 
     * @return the <code>Rectangle</code> specifying the internal bounds of this
     *         container.
     */
    public Rectangle getInternalBounds()
    {
        Insets i = getInsets();
        Dimension d = getSize();

        return new Rectangle(i.left, i.top, d.width - i.left - i.right, d.height - i.top - i.bottom);
    }

    /**
     * Retrieves the insets for this container. The insets are defined as
     * <code>(0,0,0,0)</code> by default.
     * 
     * @return the <code>Insets</code> used for this container
     */
    public Insets getInsets()
    {
        return (Insets) insets.clone();
    }

    /**
     * Sets the given insets as the ones to be subsequently used for this
     * container.
     * 
     * @param the
     *            <code>Insets</code> to be used for this container
     * @throws NullPointerException
     *             if <code>insets == null</code>
     */
    public void setInsets(Insets insets)
    {
        this.insets = (Insets) insets.clone();
    }

    // Description copied from HNavigable interface
    public void setMove(int keyCode, HNavigable target)
    {
        moves.put(keyCode, target);
    }

    // Description copied from HNavigable interface
    public HNavigable getMove(int keyCode)
    {
        return (HNavigable) moves.get(keyCode);
    }

    // Description copied from HNavigable interface
    public void setFocusTraversal(HNavigable up, HNavigable down, HNavigable left, HNavigable right)
    {
        setMove(HKeyEvent.VK_UP, up);
        setMove(HKeyEvent.VK_DOWN, down);
        setMove(HKeyEvent.VK_LEFT, left);
        setMove(HKeyEvent.VK_RIGHT, right);
    }

    // Description copied from HNavigable interface
    public boolean isSelected()
    {
        return (state & FOCUSED_STATE_BIT) != 0;
    }

    // Description copied from HNavigable interface
    public void setGainFocusSound(HSound sound)
    {
        gainFocusSound = sound;
    }

    // Description copied from HNavigable interface
    public void setLoseFocusSound(HSound sound)
    {
        loseFocusSound = sound;
    }

    // Description copied from HNavigable interface
    public HSound getGainFocusSound()
    {
        return gainFocusSound;
    }

    // Description copied from HNavigable interface
    public HSound getLoseFocusSound()
    {
        return loseFocusSound;
    }

    // Description copied from HNavigable interface
    public int[] getNavigationKeys()
    {
        return moves.getKeysNull();
    }

    /**
     * Adds the specified {@link HFocusListener} to receive {@link HFocusEvent}
     * events sent from this {@link HNavigable}. If the listener has already
     * been added further calls will add further references to the listener,
     * which will then receive multiple copies of a single event.
     * 
     * @param l
     *            the HFocusListener to add
     */
    public synchronized void addHFocusListener(HFocusListener l)
    {
        listeners = AWTEventMulticaster.add(listeners, l);
    }

    /**
     * Removes the specified {@link HFocusListener} so that it no longer
     * receives {@link HFocusEvent} events from this {@link HNavigable}. If the
     * specified listener is not registered, the method has no effect. If
     * multiple references to a single listener have been registered it should
     * be noted that this method will only remove one reference per call.
     * 
     * @param l
     *            the HFocusListener to remove
     */
    public synchronized void removeHFocusListener(HFocusListener l)
    {
        listeners = AWTEventMulticaster.remove(listeners, l);
    }

    /*
     * NOTE: the reason (add|remove)FocusListener is overridden here is because
     * of a "bug" in Forte/Netbeans that doesn't allow for event subclasses. It
     * figures the "declaring" class of a listener method (e.g., focusGained()
     * or actionPerformed()) and expects that to be the event listener class
     * (e.g., FocusEvent or ActionEvent) instead of the actual listener type
     * (e.g., HFocusEvent or HActionEvent).
     */

    /**
     * <i>Use {@link #addHFocusListener} instead</i>.
     * <p>
     * Adds the specified {@link FocusListener} to receive
     * {@link java.awt.event.FocusEvent} events sent from this component. If the
     * listener has already been added further calls will add further references
     * to the listener, which will then receive multiple copies of a single
     * event.
     * <p>
     * It should be noted that only <code>HFocusEvents</code> will be received
     * by added <code>FocusListeners</code>; no <i>plain</i> AWT
     * <code>FocusEvents</code> can be expected.
     * 
     * @param l
     *            the FocusListener.
     * @see #addHFocusListener(HFocusListener)
     */
    public synchronized void addFocusListener(FocusListener l)
    {
        listeners = AWTEventMulticaster.add(listeners, l);
    }

    /**
     * <i>Use {@link #removeHFocusListener} instead</i>.
     * <p>
     * Removes the specified {@link FocusListener}so that it no longer receives
     * {@link java.awt.event.FocusEvent} events from this component. If the
     * specified listener is not registered, the method has no effect. If
     * multiple references to a single listener have been registered it should
     * be noted that this method will only remove one reference per call.
     * 
     * @param l
     *            the FocusListener.
     * @see #removeHFocusListener(HFocusListener)
     */
    public synchronized void removeFocusListener(FocusListener l)
    {
        listeners = AWTEventMulticaster.remove(listeners, l);
    }

    // Description copied from HNavigable interface
    public void processHFocusEvent(HFocusEvent evt)
    {
        HSound sound = null;

        switch (evt.getID())
        {
            case HFocusEvent.FOCUS_GAINED:
                // Enter the focused state.
                if (!isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    sound = getGainFocusSound();
                    if (sound != null) sound.play();
                }
                if (listeners != null) listeners.focusGained(evt);

                HNavigable first;
                if ((first = getFirst()) != null) ((Component) first).requestFocus();
                break;

            case HFocusEvent.FOCUS_LOST:
                // Leave the focused state.
                if (isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    sound = getLoseFocusSound();
                    if (sound != null) sound.play();
                }
                if (listeners != null) listeners.focusLost(evt);
                break;

            case HFocusEvent.FOCUS_TRANSFER:
                // Transfer focus.
                int id = evt.getTransferId();
                HNavigable target;
                if (id != HFocusEvent.NO_TRANSFER_ID && (target = getMove(id)) != null)
                    ((Component) target).requestFocus();
                // Does not notify listeners.
                break;
        }
        // super.processHFocusEvent(e);
    }

    /**
     * Translates <i>low-level</i> AWT events into <i>high-level</i> HAVi events
     * and dispatches them to the appropriate handler. Since
     * <code>HContainer</code> is not specified to perform event translation as
     * <code>HComponent</code> is, the implementation of
     * <code>FocusContainer</code> cannot depend on such.
     * 
     * @param e
     *            the AWTEvent to process (and potentially translate)
     */
    protected void processEvent(AWTEvent evt)
    {
        // HNavigationInputPreferred
        if (TRANS_EVENTS)
        {
            HFocusEvent e = null;
            if (evt instanceof FocusEvent && !(evt instanceof HFocusEvent))
            {
                e = new HFocusEvent((Component) evt.getSource(), evt.getID());
            }
            else if (evt instanceof KeyEvent && !(evt instanceof HKeyEvent) && evt.getID() == KeyEvent.KEY_PRESSED)
            {
                e = new HFocusEvent((Component) evt.getSource(), HFocusEvent.FOCUS_TRANSFER,
                        ((KeyEvent) evt).getKeyCode());
            }
            if (e != null) processHFocusEvent(e);
        }
        super.processEvent(evt);
    }

    /**
     * Returns whether this component is focus traversable or not. By default,
     * it is (because it implements the <code>HNavigable</code> interface), but
     * this can be changed.
     * 
     * @return whether this component is focus traversable or not
     */
    public boolean isFocusTraversable()
    {
        return traversable;
    }

    /**
     * Sets the focus traversable status of this component. By default, it is
     * <code>true</code> (because it implements the <code>HNavigable</code>
     * interface), but this can be changed.
     * 
     * @param traversable
     *            whether this component should be focus traversable or not
     */
    public void setFocusTraversable(boolean traversable)
    {
        this.traversable = traversable;
    }

    /**
     * The <code>FocusContainer</code>, when it gains focus, will subsequently
     * give focus to the <i>first</i> component that was added to it. This
     * method can be used to override this default. It instructs the focus
     * container to give focus to the specified component instead (as long as it
     * is contained in this container and visible).
     * 
     * @param first
     *            the component that should receive focus when this
     *            <code>FocusContainer</code> receives focus instead of the
     *            first added component.
     */
    public void setFirst(HNavigable first)
    {
        this.first = first;
    }

    /**
     * The <code>FocusContainer</code>, when it gains focus, will subsequently
     * give focus to the <i>first</i> component that was added to it. If no
     * <i>first</i> component is set, then the set of components is searched for
     * an applicable match. It is a requirement that the component be both a
     * child of this container and currently visible.
     * 
     * @return returns the designated component that should receive focus when
     *         this <code>FocusContainer</code> receives focus.
     *         <code>null</code> if none is set and none can be found. Defaults
     *         to the first <code>HNavigable</code> component added to this
     *         <code>FocusContainer</code>.
     */
    public HNavigable getFirst()
    {
        return (first != null && ((Component) first).getParent() == this && isValidFirst((Component) first)) ? first
                : findFirst();
    }

    /**
     * The <code>FocusContainer</code>, when it gains focus, will subsequently
     * give focus to the <i>first</i> component that was added to it. This
     * method locates the <i>first</i> component that fits the bill.
     */
    private HNavigable findFirst()
    {
        Component c[] = getComponents();
        HNavigable newFirst = null;

        for (int i = 0; i < c.length; ++i)
        {
            if ((c[i] instanceof HNavigable) && isValidFirst(c[i]))
            {
                newFirst = (HNavigable) c[i];
                break;
            }
        }
        return newFirst;
    }

    /**
     * Returns <code>true</code> if the given <code>Component</code> is a valid
     * <i>first</i> component for this container. I.e., should focus be
     * transferred to the given component when focus is transferred to this
     * container.
     * 
     * @param a
     *            <code>Component</code> assumed to be a child of this container
     * @return <code>true</code> if the given <code>Component</code> is a valid
     *         <i>first</i> component for this container
     */
    private boolean isValidFirst(Component c)
    {
        return c.isFocusTraversable() && c.isVisible();
    }

    /**
     * Sets the current state of this component. Mostly for testing purposes.
     * 
     * @param state
     *            the <code>HState</code> state for this component
     * @see #getState()
     */
    final void setState(int state)
    {
        this.state = state;
    }

    /**
     * Retrieves the current state of this component. Mostly for testing
     * purposes.
     * 
     * @param the
     *            current <code>HState</code> state for this component
     * @see #setState(int)
     */
    final int getState()
    {
        return state;
    }

    /**
     * Sets the user-defined preferred size. If set to <code>null</code> (the
     * default) then the <code>preferred size</code> is determined by
     * {@link java.awt.Container#getPreferredSize() super.preferredSize}.
     * 
     * @param the
     *            new user-defined preferred size or <code>null</code> if the
     *            default should be used
     */
    public void setPreferredSize(Dimension size)
    {
        preferred = (size == null) ? size : new Dimension(size);
    }

    /**
     * Returns the preferred size of this container. If a
     * {@link #setPreferredSize(Dimension) user-defined} preferred size was set,
     * this will be returned; otherwise the default (as specified by
     * <code>Container.getPreferredSize()</code>) will be returned.
     * 
     * @param the
     *            preferred size of this container
     */
    public Dimension getPreferredSize()
    {
        return (preferred == null) ? super.getPreferredSize() : new Dimension(preferred);
    }

    /**
     * Holds focus traversals for this HNavigable.
     */
    private KeySet moves;

    /**
     * Sound to play when focus is gained.
     */
    private HSound gainFocusSound;

    /**
     * Sound to play when focus is lost.
     */
    private HSound loseFocusSound;

    /**
     * Current state. Must maintain this ourself, as we don't extend from
     * HVisible.
     */
    private int state = NORMAL_STATE;

    /**
     * The navigable component who is first in-line for focus inheritance.
     */
    private HNavigable first;

    /**
     * The insets used for this container.
     */
    private Insets insets = new Insets(0, 0, 0, 0);

    /**
     * User-defined preferred size.
     */
    private Dimension preferred;

    /** Installed HFocusListeners/FocusListeners. */
    protected FocusListener listeners;

    /** Focus traversable property. */
    private boolean traversable = true;

    /** Whether events should be translated or not. */
    private static final boolean TRANS_EVENTS = true;
}
