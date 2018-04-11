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

import org.cablelabs.gear.data.*;

import java.awt.Component;
import java.awt.AWTEventMulticaster;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

import org.havi.ui.HNavigable;
import org.havi.ui.HActionable;
import org.havi.ui.HLook;
import org.havi.ui.HSound;
import org.havi.ui.event.HFocusEvent;
import org.havi.ui.event.HFocusListener;
import org.havi.ui.event.HActionEvent;
import org.havi.ui.event.HActionListener;

/**
 * <code>Button</code> extends the <code>Label</code> to implement the
 * {@link org.havi.ui.HNavigable} and {@link org.havi.ui.HActionable}
 * interfaces, providing for button-like user interaction.
 * <p>
 * <code>Button</code> is a replacement for both
 * {@link org.havi.ui.HGraphicButton} and {@link org.havi.ui.HTextButton}; the
 * main difference being that it does not restrict itself to one type of data.
 * Whether it is a text or graphic button depends on the type of data and look
 * associated with the component.
 * 
 * @author Aaron Kamienski
 * @version $Id: Button.java,v 1.6 2002/06/03 21:33:15 aaronk Exp $
 */
public class Button extends Label implements HNavigable, HActionable
{
    /*  ************************ Constructors ************************ */

    /**
     * Default constructor. The constructed <code>Button</code> is initialized
     * with no <i>content</i> and the standard look.
     */
    public Button()
    {
        iniz();
    }

    /**
     * Look constructor. The constructed <code>Button</code> is initialized with
     * no <i>content</i> and the given look.
     * 
     * @param look
     *            the look to be used by this component
     */
    public Button(HLook look)
    {
        super(look);
        iniz();
    }

    /**
     * Icon constructor. The constructed <code>Button</code> is initialized with
     * graphic data content and the standard look. The same content is used for
     * all states.
     * 
     * @param icon
     *            the graphic data content
     */
    public Button(GraphicData icon)
    {
        super(icon);
        iniz();
    }

    /**
     * Text constructor. The constructed <code>Button</code> is initialized with
     * string data content and the standard look. The same content is used for
     * all states.
     * 
     * @param text
     *            the string data content
     */
    public Button(String text)
    {
        super(text);
        iniz();
    }

    /**
     * Animation constructor. The constructed <code>Button</code> is initialized
     * with animation data content and the standard look. The same content is
     * used for all states.
     * 
     * @param anim
     *            the animation data content
     */
    public Button(AnimationData anim)
    {
        super(anim);
        iniz();
    }

    /**
     * Constructor which takes sizing and positioning parameters.
     * 
     * @param look
     *            the look to be used by this component
     * @param x
     *            the x-coordinate
     * @param y
     *            the y-coordinate
     * @param width
     *            the width
     * @param height
     *            the height
     */
    public Button(HLook look, int x, int y, int width, int height)
    {
        super(look, x, y, width, height);
        iniz();
    }

    /**
     * Constructor which takes all types of content as well as sizing and
     * positioning parameters.
     * 
     * @param look
     *            the look to be used by this component
     * @param x
     *            the x-coordinate
     * @param y
     *            the y-coordinate
     * @param width
     *            the width
     * @param height
     *            the height
     * @param icon
     *            the graphic data content
     * @param text
     *            the string data content
     * @param anim
     *            the animation data content
     */
    public Button(HLook look, int x, int y, int width, int height, GraphicData icon, String text, AnimationData anim)
    {
        super(look, x, y, width, height, icon, text, anim);
        iniz();
    }

    /**
     * Initialization common to all constructors.
     */
    private void iniz()
    {
        moves = new KeySet();
    }

    /*  ************************ HNavigable ***************************** */

    // Description copied from HNavigable
    public void setMove(int keyCode, HNavigable target)
    {
        moves.put(keyCode, target);
    }

    // Description copied from HNavigable
    public HNavigable getMove(int keyCode)
    {
        return (HNavigable) moves.get(keyCode);
    }

    // Description copied from HNavigable
    public void setFocusTraversal(HNavigable up, HNavigable down, HNavigable left, HNavigable right)
    {
        setMove(KeyEvent.VK_UP, up);
        setMove(KeyEvent.VK_DOWN, down);
        setMove(KeyEvent.VK_LEFT, left);
        setMove(KeyEvent.VK_RIGHT, right);
    }

    // Description copied from HNavigable
    public boolean isSelected()
    {
        return (getInteractionState() & FOCUSED_STATE_BIT) != 0;
    }

    // Description copied from HNavigable
    public void setGainFocusSound(HSound sound)
    {
        gainFocusSound = sound;
    }

    // Description copied from HNavigable
    public void setLoseFocusSound(HSound sound)
    {
        loseFocusSound = sound;
    }

    // Description copied from HNavigable
    public HSound getGainFocusSound()
    {
        return gainFocusSound;
    }

    // Description copied from HNavigable
    public HSound getLoseFocusSound()
    {
        return loseFocusSound;
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
        focusListeners = AWTEventMulticaster.add(focusListeners, l);
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
        focusListeners = AWTEventMulticaster.remove(focusListeners, l);
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
        focusListeners = AWTEventMulticaster.add(focusListeners, l);
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
        focusListeners = AWTEventMulticaster.remove(focusListeners, l);
    }

    // Description copied from HNavigable
    public int[] getNavigationKeys()
    {
        return moves.getKeysNull();
    }

    // Description copied from HNavigable
    public void processHFocusEvent(HFocusEvent evt)
    {
        HSound sound = null;
        int state = getInteractionState();

        switch (evt.getID())
        {
            case HFocusEvent.FOCUS_GAINED:
                // Enter the focused state.
                if (!isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    setInteractionState(state);
                    sound = getGainFocusSound();
                    if (sound != null) sound.play();
                }
                if (focusListeners != null) focusListeners.focusGained(evt);
                break;

            case HFocusEvent.FOCUS_LOST:
                // Leave the focused state.
                if (isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    setInteractionState(state);
                    sound = getLoseFocusSound();
                    if (sound != null) sound.play();
                }
                if (focusListeners != null) focusListeners.focusLost(evt);
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
    }

    /*  ************************* HActionable ************************** */

    /**
     * Adds the specified {@link HActionListener} to receive
     * {@link HActionEvent} events sent from this {@link HActionable}. If the
     * listener has already been added further calls will add further references
     * to the listener, which will then receive multiple copies of a single
     * event.
     * 
     * @param l
     *            the HActionListener.
     */
    public synchronized void addHActionListener(HActionListener l)
    {
        actionListeners = AWTEventMulticaster.add(actionListeners, l);
    }

    /**
     * Removes the specified {@link HActionListener} so that it no longer
     * receives {@link HActionEvent} events from this {@link HActionable}. If
     * the specified listener is not registered, the method has no effect. If
     * multiple references to a single listener have been registered it should
     * be noted that this method will only remove one reference per call.
     * 
     * @param l
     *            the HActionListener.
     */
    public synchronized void removeHActionListener(HActionListener l)
    {
        actionListeners = AWTEventMulticaster.remove(actionListeners, l);
    }

    /*
     * NOTE: the reason (add|remove)ActionListener is overridden here is because
     * of a "bug" in Forte/Netbeans that doesn't allow for event subclasses. It
     * figures the "declaring" class of a listener method (e.g., focusGained()
     * or actionPerformed()) and expects that to be the event listener class
     * (e.g., FocusEvent or ActionEvent) instead of the actual listener type
     * (e.g., HFocusEvent or HActionEvent).
     */

    /**
     * <i>Use {@link #addHActionListener} instead</i>.
     * <p>
     * Adds the specified {@link ActionListener} to receive
     * {@link java.awt.event.ActionEvent} events sent from this component. If
     * the listener has already been added further calls will add further
     * references to the listener, which will then receive multiple copies of a
     * single event.
     * <p>
     * It should be noted that only <code>HActionEvents</code> will be received
     * by added <code>ActionListeners</code>; no <i>plain</i> AWT
     * <code>ActionEvents</code> can be expected.
     * 
     * @param l
     *            the ActionListener.
     * @see #addHActionListener(HActionListener)
     */
    public synchronized void addActionListener(ActionListener l)
    {
        actionListeners = AWTEventMulticaster.add(actionListeners, l);
    }

    /**
     * <i>Use {@link #removeHActionListener} instead</i>.
     * <p>
     * Removes the specified {@link ActionListener} so that it no longer
     * receives {@link java.awt.event.ActionEvent} events from this component.
     * If the specified listener is not registered, the method has no effect. If
     * multiple references to a single listener have been registered it should
     * be noted that this method will only remove one reference per call.
     * 
     * @param l
     *            the ActionListener.
     * @see #removeHActionListener(HActionListener)
     */
    public synchronized void removeActionListener(ActionListener l)
    {
        actionListeners = AWTEventMulticaster.remove(actionListeners, l);
    }

    // Description copied from superclass/interface
    public void setActionCommand(java.lang.String command)
    {
        actionCommand = command;
    }

    // Description copied from superclass/interface
    public java.lang.String getActionCommand()
    {
        return actionCommand;
    }

    // Description copied from superclass/interface
    public void setActionSound(HSound sound)
    {
        actionSound = sound;
    }

    // Description copied from superclass/interface
    public HSound getActionSound()
    {
        return actionSound;
    }

    // Description copied from HActionable
    public void processHActionEvent(HActionEvent evt)
    {
        int state = getInteractionState();

        // If enabled, then process event
        if (isEnabled())
        {
            // Enter the ACTIONED_[FOCUSED_]STATE
            setInteractionState(state |= ACTIONED_STATE_BIT);

            // Play the action sound if available
            HSound sound = getActionSound();
            if (sound != null) sound.play();

            // Notify Listeners
            fireActionEvent(evt);

            // Transition back to the original state
            // Regrab state in case changed on listener call
            setInteractionState(getInteractionState() & ~ACTIONED_STATE_BIT);
        }
    }

    /**
     * Notifies all listeners of the fired <code>HActionEvent</code>.
     * 
     * @param e
     *            the action event
     */
    protected void fireActionEvent(HActionEvent e)
    {
        if (actionListeners != null) actionListeners.actionPerformed(e);
    }

    /*  ************************* Other ************************** */

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
     * ActionListener (multicaster).
     */
    private ActionListener actionListeners;

    /**
     * FocusListener (multicaster).
     */
    private FocusListener focusListeners;

    /** The sound played when this component gains focus. */
    private HSound gainFocusSound;

    /** The sound played when this component loses focus. */
    private HSound loseFocusSound;

    /** Hashtable that maps key values to HNavigable movements. */
    private KeySet moves;

    /** Focus traversable property. */
    private boolean traversable = true;

    /**
     * The command name for the java.awt.event.ActionEvent fired by this object.
     */
    private String actionCommand;

    /** The sound played when this component fires its action event. */
    private HSound actionSound;

}
