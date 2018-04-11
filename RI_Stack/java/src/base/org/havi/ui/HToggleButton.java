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

import java.awt.Image;

import org.havi.ui.event.HActionEvent;

/**
 * The {@link org.havi.ui.HToggleButton} is a user interface component
 * representing a &quot;check box&quot;, or with the support of the
 * {@link org.havi.ui.HToggleGroup} class, &quot;radio buttons&quot;. It
 * displays static read-only graphical content. This component can be navigated
 * to, i.e. it can have the input focus, and it can be actioned as defined by
 * the {@link org.havi.ui.HSwitchable HSwitchable} interface. This means that
 * the interaction state persists after {@link org.havi.ui.event.HActionEvent}
 * event processing is complete.
 * 
 * <p>
 * The current switchable state can be manipulated using
 * {@link org.havi.ui.HToggleButton#setSwitchableState} and
 * {@link org.havi.ui.HToggleButton#getSwitchableState}
 * 
 * <p>
 * By default it uses the {@link org.havi.ui.HGraphicLook} class to render
 * itself.
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
 * <td>imageFocused</td>
 * <td>The image to be used as the content for the {@link HState#FOCUSED_STATE}
 * and {@link HState#DISABLED_FOCUSED_STATE} states of this component.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setGraphicContent setGraphicContent}</td>
 * <td>{@link org.havi.ui.HVisible#getGraphicContent getGraphicContent}</td>
 * </tr>
 * <tr>
 * <td>imageActioned</td>
 * <td>The image to be used as the content for the
 * {@link HState#ACTIONED_FOCUSED_STATE} and
 * {@link HState#DISABLED_ACTIONED_FOCUSED_STATE} states of this component.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setGraphicContent setGraphicContent}</td>
 * <td>{@link org.havi.ui.HVisible#getGraphicContent getGraphicContent}</td>
 * </tr>
 * <tr>
 * <td>imageNormalActioned</td>
 * <td>The image to be used as the content for the {@link HState#ACTIONED_STATE}
 * and {@link HState#DISABLED_ACTIONED_STATE} states of this component.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setGraphicContent setGraphicContent}</td>
 * <td>{@link org.havi.ui.HVisible#getGraphicContent getGraphicContent}</td>
 * </tr>
 * 
 * <tr>
 * <td>state</td>
 * <td>The switchable state of this {@link org.havi.ui.HToggleButton}.</td>
 * <td>false</td>
 * <td>{@link org.havi.ui.HToggleButton#setSwitchableState}</td>
 * <td>{@link org.havi.ui.HToggleButton#getSwitchableState}</td>
 * </tr>
 * 
 * <tr>
 * <td>group</td>
 * <td>The {@link org.havi.ui.HToggleGroup} with which to associate this
 * {@link org.havi.ui.HToggleButton}.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HToggleButton#setToggleGroup}</td>
 * <td>{@link org.havi.ui.HToggleButton#getToggleGroup}</td>
 * </tr>
 * 
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
 * <td>{@link org.havi.ui.HToggleButton#setDefaultLook
 * HToggleButton.setDefaultLook}</td>
 * <td>{@link org.havi.ui.HToggleButton#getDefaultLook
 * HToggleButton.getDefaultLook}</td>
 * </tr>
 * 
 * <tr>
 * <td>The &quot;look&quot; for this object.</td>
 * <td>The {@link org.havi.ui.HGraphicLook HGraphicLook} returned from
 * HToggleButton.getDefaultLook when this object was created.</td>
 * <td>{@link org.havi.ui.HToggleButton#setLook HToggleButton.setLook}</td>
 * <td>{@link org.havi.ui.HToggleButton#getLook HToggleButton.getLook}</td>
 * </tr>
 * <tr>
 * <td>The gain focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HToggleButton#setGainFocusSound setGainFocusSound}</td>
 * <td>{@link org.havi.ui.HToggleButton#getGainFocusSound getGainFocusSound}</td>
 * </tr>
 * <tr>
 * <td>The lose focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HToggleButton#setLoseFocusSound setLoseFocusSound}</td>
 * <td>{@link org.havi.ui.HToggleButton#getLoseFocusSound getLoseFocusSound}</td>
 * </tr>
 * <tr>
 * <td>The action sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HToggleButton#setActionSound setActionSound}</td>
 * <td>{@link org.havi.ui.HToggleButton#getActionSound getActionSound}</td>
 * </tr>
 * <tr>
 * <td>The unset action sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HToggleButton#setUnsetActionSound setUnsetActionSound}
 * </td>
 * <td>{@link org.havi.ui.HToggleButton#getUnsetActionSound getUnsetActionSound}
 * </td>
 * </tr>
 * </table>
 * 
 * @see HStaticIcon
 * @see HIcon
 * @see HNavigable
 * @see HActionable
 * @see HSwitchable
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HToggleButton extends HGraphicButton implements HSwitchable
{
    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton()
    {
        super();
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(java.awt.Image image, int x, int y, int width, int height)
    {
        super(image, x, y, width, height);
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(Image image)
    {
        super(image);
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(java.awt.Image image, int x, int y, int width, int height, boolean state)
    {
        this(image, x, y, width, height);
        setSwitchableState(state);
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(java.awt.Image imageNormal, java.awt.Image imageFocused, java.awt.Image imageActioned,
            java.awt.Image imageNormalActioned, int x, int y, int width, int height, boolean state)
    {
        super(imageNormal, imageFocused, imageActioned, x, y, width, height);
        setGraphicContent(imageNormalActioned, ACTIONED_STATE);
        setSwitchableState(state);
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(java.awt.Image imageNormal, java.awt.Image imageFocused, java.awt.Image imageActioned,
            java.awt.Image imageNormalActioned, boolean state)
    {
        super(imageNormal, imageFocused, imageActioned);
        setGraphicContent(imageNormalActioned, ACTIONED_STATE);
        setSwitchableState(state);
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(java.awt.Image image, int x, int y, int width, int height, boolean state, HToggleGroup group)
    {
        this(image, x, y, width, height, state);
        setToggleGroup(group);
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(java.awt.Image image, boolean state, HToggleGroup group)
    {
        this(image);
        setSwitchableState(state);
        setToggleGroup(group);
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(java.awt.Image imageNormal, java.awt.Image imageFocused, java.awt.Image imageActioned,
            java.awt.Image imageNormalActioned, int x, int y, int width, int height, boolean state, HToggleGroup group)
    {
        this(imageNormal, imageFocused, imageActioned, imageNormalActioned, x, y, width, height, state);
        setToggleGroup(group);
    }

    /**
     * Creates an {@link org.havi.ui.HToggleButton} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HToggleButton(java.awt.Image imageNormal, java.awt.Image imageFocused, java.awt.Image imageActioned,
            java.awt.Image imageNormalActioned, boolean state, HToggleGroup group)
    {
        this(imageNormal, imageFocused, imageActioned, imageNormalActioned, state);
        setToggleGroup(group);
    }

    /**
     * Initialization common to all constructors.
     */
    private void iniz()
    {
        try
        {
            setLook(getDefaultLook());
        }
        catch (HInvalidLookException ignored)
        { /* empty */
        }
    }

    /**
     * Associates the {@link org.havi.ui.HToggleButton} with an
     * {@link org.havi.ui.HToggleGroup}. If this
     * {@link org.havi.ui.HToggleButton} is already in a different
     * {@link org.havi.ui.HToggleGroup}, it is first taken out of that group.
     * 
     * @param group
     *            The {@link org.havi.ui.HToggleGroup} the
     *            {@link org.havi.ui.HToggleButton} is to be associated with.
     */
    public void setToggleGroup(HToggleGroup group)
    {
        HToggleGroup oldGroup = toggleGroup;

        // Remove ourselves if already a member of a group
        if (oldGroup != null)
        {
            // If it is the same, don't do anything.
            if (oldGroup == group) return;

            // Remove ourselves
            oldGroup.remove(this);
        }

        // Assign the new toggle group
        toggleGroup = group;
        if (group != null) group.add(this);
    }

    /**
     * Gets the {@link org.havi.ui.HToggleGroup} the
     * {@link org.havi.ui.HToggleButton} is associated with.
     * 
     * @return The {@link org.havi.ui.HToggleGroup} the
     *         {@link org.havi.ui.HToggleButton} is associated with, or null if
     *         the {@link org.havi.ui.HToggleButton} is not associated with an
     *         {@link org.havi.ui.HToggleGroup}.
     */
    public HToggleGroup getToggleGroup()
    {
        return toggleGroup;
    }

    /**
     * Removes the button from the toggle group that it has been added to. This
     * method does nothing if the button had not been previously added to an
     * {@link org.havi.ui.HToggleGroup}.
     */
    public void removeToggleGroup()
    {
        setToggleGroup(null);
    }

    /**
     * Sets the default {@link org.havi.ui.HLook} for further
     * {@link org.havi.ui.HToggleButton} components.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook} that will be used by default
     *            when creating a new {@link org.havi.ui.HToggleButton}
     *            component. Note that this parameter may be null, in which case
     *            newly created components shall not draw themselves until a
     *            non-null look is set using the
     *            {@link org.havi.ui.HStaticIcon#setLook} method.
     */
    public static void setDefaultLook(HGraphicLook hlook)
    {
        setDefaultLookImpl(PROPERTY_LOOK, hlook);
    }

    /**
     * Returns the currently set default {@link org.havi.ui.HLook} for
     * {@link org.havi.ui.HToggleButton} components.
     * 
     * @return The {@link org.havi.ui.HLook} that is used by default when
     *         creating a new {@link org.havi.ui.HToggleButton} component.
     */
    public static HGraphicLook getDefaultLook()
    {
        return (HGraphicLook) getDefaultLookImpl(PROPERTY_LOOK, DEFAULT_LOOK);
    }

    /**
     * Returns the current switchable state of this
     * {@link org.havi.ui.HSwitchable HSwitchable}.
     * 
     * @return the current switchable state of this
     *         {@link org.havi.ui.HSwitchable HSwitchable}.
     */
    public boolean getSwitchableState()
    {
        return (getInteractionState() & ACTIONED_STATE_BIT) != 0;
    }

    /**
     * Sets the current state of the button. Note that ActionListeners are only
     * called when an ACTION_PERFORMED event is received, or if they are called
     * directly, e.g. via <code>processActionEvent</code>, they are not called
     * by {@link org.havi.ui.HToggleButton#setSwitchableState
     * setSwitchableState}.
     */
    public void setSwitchableState(boolean state)
    {
        int old = getInteractionState();
        setInteractionState(state ? (old | ACTIONED_STATE_BIT) : (old & ~ACTIONED_STATE_BIT));
    }

    /**
     * Associate a sound to be played when the interaction state of the
     * {@link org.havi.ui.HSwitchable HSwitchable} makes the following
     * transitions:
     * <p>
     * <ul>
     * <li> {@link org.havi.ui.HState#ACTIONED_STATE ACTIONED_STATE} to
     * {@link org.havi.ui.HState#NORMAL_STATE NORMAL_STATE}
     * <li> {@link org.havi.ui.HState#ACTIONED_FOCUSED_STATE
     * ACTIONED_FOCUSED_STATE} to {@link org.havi.ui.HState#FOCUSED_STATE
     * FOCUSED_STATE}
     * </ul>
     * <p>
     * 
     * @param sound
     *            a sound to be played when the {@link org.havi.ui.HSwitchable
     *            HSwitchable} transitions from an actioned state. If sound
     *            content is already set, the original content is replaced. To
     *            remove the sound specify a null {@link org.havi.ui.HSound
     *            HSound}.
     */
    public void setUnsetActionSound(HSound sound)
    {
        unsetActionSound = sound;
    }

    /**
     * Get the sound to be played when the interaction state of the
     * {@link org.havi.ui.HSwitchable HSwitchable} makes the following
     * transitions:
     * <p>
     * <ul>
     * <li> {@link org.havi.ui.HState#ACTIONED_STATE ACTIONED_STATE} to
     * {@link org.havi.ui.HState#NORMAL_STATE NORMAL_STATE}
     * <li> {@link org.havi.ui.HState#ACTIONED_FOCUSED_STATE
     * ACTIONED_FOCUSED_STATE} to {@link org.havi.ui.HState#FOCUSED_STATE
     * FOCUSED_STATE}
     * </ul>
     * <p>
     * 
     * @return the sound to be played when the {@link org.havi.ui.HSwitchable
     *         HSwitchable} transitions from an actioned state.
     */
    public HSound getUnsetActionSound()
    {
        return unsetActionSound;
    }

    /**
     * Process an {@link org.havi.ui.event.HActionEvent HActionEvent} sent to
     * this {@link HToggleButton}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HActionEvent HActionEvent} to
     *            process.
     */
    public void processHActionEvent(HActionEvent evt)
    {
        int state = getInteractionState();

        // If enabled, then process event
        if ((state & DISABLED_STATE_BIT) == 0)
        {
            // Toggle the ACTIONED_STATE_BIT
            setInteractionState(state ^= ACTIONED_STATE_BIT);

            // Play the action sound if available
            HSound sound = ((state & ACTIONED_STATE_BIT) != 0) ? getActionSound() : getUnsetActionSound();
            if (sound != null) sound.play();

            fireActionEvent(evt);
        }
    }

    /**
     * Overrides {@link HVisible#setInteractionState(int)} to provide for
     * notifying the current {@link HToggleGroup} about the
     * {@link #getSwitchableState() switchable} state.
     * 
     * @param state
     *            the new interaction state
     */
    protected void setInteractionState(int state)
    {
        super.setInteractionState(state);
        if (toggleGroup != null)
        {
            // Make the current selection
            if (getSwitchableState())
                toggleGroup.setCurrent(this);
            // If current selection, unselect
            else if (toggleGroup.getCurrent() == this) toggleGroup.setCurrent(null);
        }
    }

    /**
     * The sound played when this HSwitchable transitions from the
     * {@link HState#ACTIONED_STATE_BIT ACTIONED_STATE}.
     */
    private HSound unsetActionSound = null;

    /**
     * The {@link HToggleGroup} to which this {@link HToggleButton} currently
     * belongs. <code>null</code> if it does not belong to a toggle group.
     */
    private HToggleGroup toggleGroup = null;

    /** Property used to override the default look. */
    private static final String PROPERTY_LOOK = "org.havi.ui.HToggleButton.defaultLook";
}
