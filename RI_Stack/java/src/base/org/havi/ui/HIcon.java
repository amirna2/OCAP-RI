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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

import org.cablelabs.impl.havi.KeySet;

import java.awt.Component;
import java.awt.event.KeyEvent;

import org.havi.ui.event.HFocusEvent;

/**
 * The {@link org.havi.ui.HIcon HIcon} is a user interface component used to
 * display static graphical content (as {@link org.havi.ui.HStaticIcon
 * HStaticIcon}) which also enables a user to navigate to it, i.e. it can have
 * the input focus. By default it uses the {@link org.havi.ui.HGraphicLook
 * HGraphicLook} to render itself.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>x</td>
 * <td>x-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>y</td>
 * <td>y-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>width</td>
 * <td>width of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>height</td>
 * <td>height of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * 
 * 
 * 
 * <tr>
 * <td>image</td>
 * <td>The image to be used as the content for every state of this component.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setGraphicContent setGraphicContent}</td>
 * <td>{@link org.havi.ui.HVisible#getGraphicContent getGraphicContent}</td>
 * </tr>
 * <tr>
 * <td>imageNormal</td>
 * <td>The image to be used as the content for the
 * {@link org.havi.ui.HState#NORMAL_STATE HState.NORMAL_STATE} state of this
 * component.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setGraphicContent setGraphicContent}</td>
 * <td>{@link org.havi.ui.HVisible#getGraphicContent getGraphicContent}</td>
 * </tr>
 * <tr>
 * <td>imageFocus</td>
 * <td>The image to be used as the content for the focused states of this
 * component.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setGraphicContent setGraphicContent}</td>
 * <td>{@link org.havi.ui.HVisible#getGraphicContent getGraphicContent}</td>
 * </tr>
 * </table>
 * 
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>Associated matte ({@link org.havi.ui.HMatte HMatte}).</td>
 * <td>none (i.e. getMatte() returns <code>null</code>)</td>
 * <td>{@link org.havi.ui.HComponent#setMatte setMatte}</td>
 * <td>{@link org.havi.ui.HComponent#getMatte getMatte}</td>
 * </tr>
 * <tr>
 * <td>The text layout manager responsible for text formatting.</td>
 * <td>An {@link org.havi.ui.HDefaultTextLayoutManager} object.</td>
 * <td> {@link org.havi.ui.HVisible#setTextLayoutManager}</td>
 * <td> {@link org.havi.ui.HVisible#getTextLayoutManager}</td>
 * </tr>
 * 
 * <tr>
 * <td>The background painting mode</td>
 * <td>{@link org.havi.ui.HVisible#NO_BACKGROUND_FILL}</td>
 * 
 * <td>{@link org.havi.ui.HVisible#setBackgroundMode}</td>
 * <td>{@link org.havi.ui.HVisible#getBackgroundMode}</td>
 * </tr>
 * 
 * <tr>
 * <td>The default preferred size</td>
 * <td>not set (i.e. NO_DEFAULT_SIZE) unless specified by <code>width</code> and
 * <code>height</code> parameters</td>
 * <td>{@link org.havi.ui.HVisible#setDefaultSize}</td>
 * <td>{@link org.havi.ui.HVisible#getDefaultSize}</td>
 * </tr>
 * 
 * <tr>
 * <td>The horizontal content alignment</td>
 * <td>{@link org.havi.ui.HVisible#HALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setHorizontalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getHorizontalAlignment}</td>
 * </tr>
 * 
 * <tr>
 * <td>The vertical content alignment</td>
 * <td>{@link org.havi.ui.HVisible#VALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setVerticalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getVerticalAlignment}</td>
 * </tr>
 * 
 * <tr>
 * <td>The content scaling mode</td>
 * <td>{@link org.havi.ui.HVisible#RESIZE_NONE}</td>
 * <td>{@link org.havi.ui.HVisible#setResizeMode}</td>
 * <td>{@link org.havi.ui.HVisible#getResizeMode}</td>
 * </tr>
 * 
 * <tr>
 * <td>The border mode</td>
 * <td><code>true</code></td>
 * <td>{@link org.havi.ui.HVisible#setBordersEnabled}</td>
 * <td>{@link org.havi.ui.HVisible#getBordersEnabled}</td>
 * </tr>
 * 
 * 
 * 
 * 
 * <tr>
 * <td>The default &quot;look&quot; for this class.</td>
 * <td>A platform specific {@link org.havi.ui.HGraphicLook HGraphicLook}</td>
 * <td>{@link org.havi.ui.HIcon#setDefaultLook HIcon.setDefaultLook}</td>
 * <td>{@link org.havi.ui.HIcon#getDefaultLook HIcon.getDefaultLook}</td>
 * </tr>
 * 
 * <tr>
 * <td>The &quot;look&quot; for this object.</td>
 * <td>The {@link org.havi.ui.HGraphicLook HGraphicLook} returned from
 * HIcon.getDefaultLook when this object was created.</td>
 * <td>{@link org.havi.ui.HIcon#setLook HIcon.setLook}</td>
 * <td>{@link org.havi.ui.HIcon#getLook HIcon.getLook}</td>
 * </tr>
 * <tr>
 * <td>The gain focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HIcon#setGainFocusSound setGainFocusSound}</td>
 * <td>{@link org.havi.ui.HIcon#getGainFocusSound getGainFocusSound}</td>
 * </tr>
 * <tr>
 * <td>The lose focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HIcon#setLoseFocusSound setLoseFocusSound}</td>
 * <td>{@link org.havi.ui.HIcon#getLoseFocusSound getLoseFocusSound}</td>
 * </tr>
 * </table>
 * 
 * @see HStaticIcon
 * @see HNavigable
 * @author Rob Beaver
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HIcon extends HStaticIcon implements HNavigable
{

    /**
     * Creates an {@link org.havi.ui.HIcon HIcon} object. See the class
     * description for details of constructor parameters and default values. For
     * constructors which specify content as parameters, see 'State-based
     * content' in HVisible for unspecified content associated with other
     * HStates.
     */
    public HIcon()
    {
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HIcon HIcon} object. See the class
     * description for details of constructor parameters and default values. For
     * constructors which specify content as parameters, see 'State-based
     * content' in HVisible for unspecified content associated with other
     * HStates.
     */
    public HIcon(java.awt.Image image)
    {
        setGraphicContent(image, ALL_STATES);
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HIcon HIcon} object. See the class
     * description for details of constructor parameters and default values. For
     * constructors which specify content as parameters, see 'State-based
     * content' in HVisible for unspecified content associated with other
     * HStates.
     */
    public HIcon(java.awt.Image image, int x, int y, int width, int height)
    {
        super(image, x, y, width, height);
        setGraphicContent(image, ALL_STATES);
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HIcon HIcon} object. See the class
     * description for details of constructor parameters and default values. For
     * constructors which specify content as parameters, see 'State-based
     * content' in HVisible for unspecified content associated with other
     * HStates.
     */
    public HIcon(java.awt.Image imageNormal, java.awt.Image imageFocus, int x, int y, int width, int height)
    {
        super(imageNormal, x, y, width, height);
        setGraphicContent(imageFocus, FOCUSED_STATE);
        iniz();
    }

    /**
     * Code common to all constructors.
     */
    private void iniz()
    {
        moves = new KeySet();

        try
        {
            setLook(getDefaultLook());
        }
        catch (HInvalidLookException ignored)
        {
        }
    }

    /**
     * Sets the default {@link org.havi.ui.HLook HLook} for further
     * {@link org.havi.ui.HIcon HIcon} Components.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook HLook} that will be used by
     *            default when creating a new {@link org.havi.ui.HIcon HIcon}
     *            component. Note that this parameter may be null, in which case
     *            newly created components shall not draw themselves until a
     *            non-null look is set using the
     *            {@link org.havi.ui.HStaticIcon#setLook setLook} method.
     */
    public static void setDefaultLook(HGraphicLook hlook)
    {
        setDefaultLookImpl(PROPERTY_LOOK, hlook);
    }

    /**
     * Returns the currently set default look for {@link org.havi.ui.HIcon
     * HIcon} components.
     * 
     * @return The look that is used by default when creating a new
     *         {@link org.havi.ui.HIcon HIcon} component.
     */
    public static HGraphicLook getDefaultLook()
    {
        return (HGraphicLook) getDefaultLookImpl(PROPERTY_LOOK, DEFAULT_LOOK);
    }

    /**
     * Defines the navigation path from the current
     * {@link org.havi.ui.HNavigable HNavigable} to another
     * {@link org.havi.ui.HNavigable HNavigable} when a particular key is
     * pressed.
     * <p>
     * Note that {@link org.havi.ui.HNavigable#setFocusTraversal
     * setFocusTraversal} is equivalent to multiple calls to
     * {@link org.havi.ui.HNavigable#setMove setMove}, where the key codes
     * <code>VK_UP</code>, <code>VK_DOWN</code>, <code>VK_LEFT</code>,
     * <code>VK_RIGHT</code> are used.
     * 
     * @param keyCode
     *            The key code of the pressed key. Any numerical keycode is
     *            allowed, but the platform may not be able to generate all
     *            keycodes. Application authors should only use keys for which
     *            <code>HRcCapabilities.isSupported()</code> or
     *            <code>HKeyCapabilities.isSupported()</code> returns true.
     * @param target
     *            The target {@link org.havi.ui.HNavigable HNavigable} object
     *            that should be navigated to. If a target is to be removed from
     *            a particular navigation path, then <code>null</code> shall be
     *            specified.
     */
    public void setMove(int keyCode, HNavigable target)
    {
        moves.put(keyCode, target);
    }

    /**
     * Provides the {@link org.havi.ui.HNavigable HNavigable} object that is
     * navigated to when a particular key is pressed.
     * 
     * @param keyCode
     *            The key code of the pressed key.
     * @return Returns the {@link org.havi.ui.HNavigable HNavigable} object or
     *         <code>null</code> if no {@link org.havi.ui.HNavigable HNavigable}
     *         is associated with the keyCode.
     */
    public HNavigable getMove(int keyCode)
    {
        return (HNavigable) moves.get(keyCode);
    }

    /**
     * Set the focus control for an {@link org.havi.ui.HNavigable HNavigable}
     * component. Note {@link org.havi.ui.HNavigable#setFocusTraversal
     * setFocusTraversal} is a convenience function for application programmers
     * where a standard up, down, left and right focus traversal between
     * components is required.
     * <p>
     * Note {@link org.havi.ui.HNavigable#setFocusTraversal setFocusTraversal}
     * is equivalent to multiple calls to {@link org.havi.ui.HNavigable#setMove
     * setMove}, where the key codes VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT are used.
     * <p>
     * Note that this API does not prevent the creation of &quot;isolated&quot;
     * {@link org.havi.ui.HNavigable HNavigable} components --- authors should
     * endeavor to avoid confusing the user.
     * 
     * @param up
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_UP KeyEvent. If there is
     *            no {@link org.havi.ui.HNavigable HNavigable} component to move
     *            &quot;up&quot; to, then null shall be specified.
     * @param down
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_DOWN KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;down&quot; to, then null shall be specified.
     * @param left
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_LEFT KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;left&quot; to, then null shall be specified.
     * @param right
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_RIGHT KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;right&quot; to, then null shall be specified.
     */
    public void setFocusTraversal(HNavigable up, HNavigable down, HNavigable left, HNavigable right)
    {
        setMove(KeyEvent.VK_UP, up);
        setMove(KeyEvent.VK_DOWN, down);
        setMove(KeyEvent.VK_LEFT, left);
        setMove(KeyEvent.VK_RIGHT, right);
    }

    /**
     * Indicates if this component has focus.
     * 
     * @return <code>true</code> if the component has focus, otherwise returns
     *         <code>false</code>.
     */
    public boolean isSelected()
    {
        return (getInteractionState() & FOCUSED_STATE_BIT) != 0;
    }

    /**
     * Associate a sound with gaining focus, i.e. when the
     * {@link org.havi.ui.HNavigable HNavigable} receives a
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} event of type
     * <code>FOCUS_GAINED</code>. This sound will start to be played when an
     * object implementing this interface gains focus. It is not guaranteed to
     * be played to completion. If the object implementing this interface loses
     * focus before the audio completes playing, the audio will be truncated.
     * Applications wishing to ensure the audio is always played to completion
     * must implement special logic to slow down the focus transitions.
     * <p>
     * By default, an {@link org.havi.ui.HNavigable HNavigable} object does not
     * have any gain focus sound associated with it.
     * <p>
     * Note that the ordering of playing sounds is dependent on the order of the
     * focus lost, gained events.
     * 
     * @param sound
     *            the sound to be played, when the component gains focus. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setGainFocusSound(HSound sound)
    {
        gainFocusSound = sound;
    }

    /**
     * Associate a sound with losing focus, i.e. when the
     * {@link org.havi.ui.HNavigable HNavigable} receives a
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} event of type
     * FOCUS_LOST. This sound will start to be played when an object
     * implementing this interface loses focus. It is not guaranteed to be
     * played to completion. It is implementation dependent whether and when
     * this sound will be truncated by any gain focus sound played by the next
     * object to gain focus.
     * <p>
     * By default, an {@link org.havi.ui.HNavigable HNavigable} object does not
     * have any lose focus sound associated with it.
     * <p>
     * Note that the ordering of playing sounds is dependent on the order of the
     * focus lost, gained events.
     * 
     * @param sound
     *            the sound to be played, when the component loses focus. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setLoseFocusSound(HSound sound)
    {
        loseFocusSound = sound;
    }

    /**
     * Get the sound associated with the gain focus event.
     * 
     * @return The sound played when the component gains focus. If no sound is
     *         associated with gaining focus, then null shall be returned.
     */
    public HSound getGainFocusSound()
    {
        return gainFocusSound;
    }

    /**
     * Get the sound associated with the lost focus event.
     * 
     * @return The sound played when the component loses focus. If no sound is
     *         associated with losing focus, then null shall be returned.
     */
    public HSound getLoseFocusSound()
    {
        return loseFocusSound;
    }

    /**
     * Adds the specified {@link org.havi.ui.event.HFocusListener
     * HFocusListener} to receive {@link org.havi.ui.event.HFocusEvent
     * HFocusEvent} events sent from this {@link org.havi.ui.HNavigable
     * HNavigable}: If the listener has already been added further calls will
     * add further references to the listener, which will then receive multiple
     * copies of a single event.
     * 
     * @param l
     *            the HFocusListener to add
     */
    public void addHFocusListener(org.havi.ui.event.HFocusListener l)
    {
        listeners = HEventMulticaster.add(listeners, l);
    }

    /**
     * Removes the specified {@link org.havi.ui.event.HFocusListener
     * HFocusListener} so that it no longer receives
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} events from this
     * {@link org.havi.ui.HNavigable HNavigable}. If the specified listener is
     * not registered, the method has no effect. If multiple references to a
     * single listener have been registered it should be noted that this method
     * will only remove one reference per call.
     * 
     * @param l
     *            the HFocusListener to remove
     */
    public void removeHFocusListener(org.havi.ui.event.HFocusListener l)
    {
        listeners = HEventMulticaster.remove(listeners, l);
    }

    /**
     * Retrieve the set of key codes which this component maps to navigation
     * targets.
     * 
     * @return an array of key codes, or <code>null</code> if no navigation
     *         targets are set on this component.
     */
    public int[] getNavigationKeys()
    {
        return moves.getKeysNull();
    }

    /**
     * Process an {@link org.havi.ui.event.HFocusEvent HFocusEvent} sent to this
     * {@link org.havi.ui.HIcon HIcon}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HFocusEvent HFocusEvent} to
     *            process.
     */

    public void processHFocusEvent(org.havi.ui.event.HFocusEvent evt)
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

                    // first stop lose-focus sound, if it is playing
                    sound = getLoseFocusSound();
                    if (sound != null) sound.stop();

                    // next play gain-focus sound, if applicable
                    sound = getGainFocusSound();
                    if (sound != null) sound.play();
                }
                if (listeners != null) listeners.focusGained(evt);
                break;

            case HFocusEvent.FOCUS_LOST:
                // Leave the focused state.
                if (isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    setInteractionState(state);

                    // first stop gain-focus sound, if it is playing
                    sound = getGainFocusSound();
                    if (sound != null) sound.stop();

                    // next play lose-focus sound, if applicable
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
    }

    /**
     * The set of listeners.
     */
    private org.havi.ui.event.HFocusListener listeners;

    /** The sound played when this component gains focus. */
    private HSound gainFocusSound;

    /** The sound played when this component loses focus. */
    private HSound loseFocusSound;

    /** Hashtable that maps key values to HNavigable movements. */
    private KeySet moves;

    /** Property name for specifying default look. */
    private static final String PROPERTY_LOOK = "org.havi.ui.HIcon.defaultLook";
}
