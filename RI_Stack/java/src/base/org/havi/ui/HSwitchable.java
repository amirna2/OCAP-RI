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

/**
 * This interface is implemented for all user interface components that can be
 * actioned such that they &quot;toggle&quot; on and off and maintain the chosen
 * state.
 * 
 * <h3>Event Behavior</h3>
 * 
 * Subclasses of {@link org.havi.ui.HComponent HComponent} which implement
 * {@link org.havi.ui.HSwitchable HSwitchable} must respond to
 * {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
 * {@link org.havi.ui.event.HActionEvent HActionEvent} events.
 * 
 * <p>
 * Applications should assume that classes which implement
 * {@link org.havi.ui.HSwitchable HSwitchable} can generate events of the types
 * {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
 * {@link org.havi.ui.event.HActionEvent HActionEvent} in response to other
 * types of input event.
 * 
 * <p>
 * An application may add one or more {@link org.havi.ui.event.HActionListener
 * HActionListener} listeners to the component. The
 * {@link org.havi.ui.event.HActionListener#actionPerformed actionPerformed}
 * method of the {@link org.havi.ui.event.HActionListener HActionListener} is
 * invoked whenever the {@link org.havi.ui.HSwitchable HSwitchable} is actioned.
 * 
 * <p>
 * HAVi action events are discussed in detail in the
 * {@link org.havi.ui.HActionInputPreferred HActionInputPreferred} interface
 * description.
 * 
 * <h3>Interaction States</h3>
 * 
 * The following interaction states are valid for this
 * {@link org.havi.ui.HSwitchable HSwitchable} component:
 * 
 * <p>
 * <ul>
 * <li> {@link org.havi.ui.HState#NORMAL_STATE NORMAL_STATE}
 * <li> {@link org.havi.ui.HState#FOCUSED_STATE FOCUSED_STATE}
 * <li> {@link org.havi.ui.HState#ACTIONED_STATE ACTIONED_STATE}
 * <li> {@link org.havi.ui.HState#ACTIONED_FOCUSED_STATE ACTIONED_FOCUSED_STATE}
 * <li> {@link org.havi.ui.HState#DISABLED_STATE DISABLED_STATE}
 * <li> {@link org.havi.ui.HState#DISABLED_FOCUSED_STATE DISABLED_FOCUSED_STATE}
 * <li> {@link org.havi.ui.HState#DISABLED_ACTIONED_STATE
 * DISABLED_ACTIONED_STATE}
 * <li> {@link org.havi.ui.HState#DISABLED_ACTIONED_FOCUSED_STATE
 * DISABLED_ACTIONED_FOCUSED_STATE}
 * </ul>
 * <p>
 * 
 * The state machine diagram below shows the valid state transitions for an
 * {@link org.havi.ui.HSwitchable HSwitchable} component.
 * 
 * <p>
 * <table border>
 * <tr>
 * <td><img src="../../../images/HSwitchable_state.gif"></td>
 * </tr>
 * </table>
 * <p>
 * 
 * Unlike {@link org.havi.ui.HActionable HActionable} components there are no
 * automatic transitions to other states. Actioned states (i.e. those with the
 * {@link org.havi.ui.HState#ACTIONED_STATE_BIT ACTIONED_STATE_BIT} bit set may
 * persist after any registered {@link org.havi.ui.event.HActionListener
 * HActionListener} listeners have been called, until a further
 * {@link org.havi.ui.event.HActionEvent HActionEvent} is received.
 * 
 * <h3>Platform Classes</h3>
 * 
 * The following HAVi platform classes implement or inherit the
 * {@link org.havi.ui.HSwitchable HSwitchable} interface. These classes shall
 * all generate both {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
 * {@link org.havi.ui.event.HActionEvent HActionEvent} events in addition to any
 * other events specified in the respective class descriptions.
 * 
 * <p>
 * <ul>
 * <li> {@link org.havi.ui.HToggleButton HToggleButton}
 * </ul>
 * 
 * @see org.havi.ui.HNavigable
 * @see org.havi.ui.HActionable
 * @see org.havi.ui.HActionInputPreferred
 * @see org.havi.ui.event.HActionEvent
 * @see org.havi.ui.event.HActionListener
 */

public interface HSwitchable extends HActionable
{
    /**
     * Returns the current switchable state of this
     * {@link org.havi.ui.HSwitchable HSwitchable}.
     * 
     * @return the current switchable state of this
     *         {@link org.havi.ui.HSwitchable HSwitchable}.
     */
    public boolean getSwitchableState();

    /**
     * Sets the current state of the button. Note that ActionListeners are only
     * called when an ACTION_PERFORMED event is received, or if they are called
     * directly, e.g. via <code>processActionEvent</code>, they are not called
     * by {@link org.havi.ui.HToggleButton#setSwitchableState
     * setSwitchableState}.
     */
    public void setSwitchableState(boolean state);

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
    public void setUnsetActionSound(HSound sound);

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
    public HSound getUnsetActionSound();
}
