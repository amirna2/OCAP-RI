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
 * This interface is implemented by all HAVi UI components that can be actioned
 * by the user.
 * 
 * 
 * 
 * <h3>Event Behaviour</h3>
 * 
 * Subclasses of {@link org.havi.ui.HComponent HComponent} which implement
 * <code>HActionable</code> must respond to
 * {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
 * {@link org.havi.ui.event.HActionEvent} events.
 * 
 * <p>
 * Applications should assume that classes which implement
 * <code>HActionable</code> can generate events of the types HFocusEvent and
 * <code>HActionEvent</code> in response to other types of input event.
 * 
 * <p>
 * An application may add one or more <code>HActionListener</code> listeners to
 * the component. The <code>actionPerformed</code> method of the
 * <code>HActionListener</code> is invoked whenever the <code>HActionable</code>
 * is actioned.
 * 
 * <p>
 * HAVi action events are discussed in detail in the
 * {@link org.havi.ui.HActionInputPreferred HActionInputPreferred} interface
 * description.
 * 
 * <h3>Interaction States</h3>
 * 
 * The following interaction states are valid for this <code>HActionable</code>
 * component:
 * 
 * <p>
 * <ul>
 * <li> {@link org.havi.ui.HState#NORMAL_STATE NORMAL_STATE}
 * <li> {@link org.havi.ui.HState#FOCUSED_STATE FOCUSED_STATE}
 * <li> {@link org.havi.ui.HState#ACTIONED_STATE ACTIONED_STATE}
 * <li> {@link org.havi.ui.HState#ACTIONED_FOCUSED_STATE ACTIONED_FOCUSED_STATE}
 * <li> {@link org.havi.ui.HState#DISABLED_STATE DISABLED_STATE}
 * <li> {@link org.havi.ui.HState#DISABLED_FOCUSED_STATE DISABLED_FOCUSED_STATE}
 * </ul>
 * <p>
 * 
 * The state machine diagram below shows the valid state transitions for an
 * <code>HActionable</code> component.
 * 
 * <p>
 * <table border>
 * <tr>
 * <td><img src="../../../images/HActionable_state.gif"></td>
 * </tr>
 * </table>
 * <p>
 * 
 * Unlike {@link org.havi.ui.HSwitchable HSwitchable} components, the transition
 * back from an actioned state (i.e. one with the
 * {@link org.havi.ui.HState#ACTIONED_STATE_BIT} bit set) is automatically fired
 * once all registered <code>HActionListener</code> listeners have been called.
 * 
 * <p>
 * A direct consequence of this is that <code>HActionable</code> components can
 * only achieve the {@link org.havi.ui.HState#ACTIONED_STATE ACTIONED_STATE} and
 * {@link org.havi.ui.HState#ACTIONED_FOCUSED_STATE ACTIONED_FOCUSED_STATE}
 * states on a temporary basis.
 * 
 * <p>
 * <code>HActionable</code> components may not be disabled while actioned.
 * 
 * <h3>Platform Classes</h3>
 * 
 * The following HAVi platform classes implement or inherit the
 * <code>HActionable</code> interface. These classes shall all generate both
 * <code>HFocusEvent</code> and <code>HActionEvent</code> events in addition to
 * any other events specified in the respective class descriptions.
 * 
 * <p>
 * <ul>
 * <li> {@link org.havi.ui.HGraphicButton}
 * <li> {@link org.havi.ui.HTextButton}
 * <li> {@link org.havi.ui.HToggleButton}
 * </ul>
 * 
 * @see org.havi.ui.HNavigable
 * @see org.havi.ui.HActionInputPreferred
 * @see org.havi.ui.event.HActionEvent
 * @see org.havi.ui.event.HActionListener
 */

public interface HActionable extends HNavigable, HActionInputPreferred
{
    /**
     * Adds the specified <code>HActionListener</code> to receive
     * <code>HActionEvent</code> events sent from this <code>HActionable</code>.
     * If the listener has already been added further calls will add further
     * references to the listener, which will then receive multiple copies of a
     * single event.
     * 
     * @param l
     *            the HActionListener.
     */
    public void addHActionListener(org.havi.ui.event.HActionListener l);

    /**
     * Removes the specified <code>HActionListener</code> so that it no longer
     * receives <code>HActionEvent</code> events from this
     * <code>HActionable</code>. If the specified listener is not registered,
     * the method has no effect. If multiple references to a single listener
     * have been registered it should be noted that this method will only remove
     * one reference per call.
     * 
     * @param l
     *            the HActionListener.
     */
    public void removeHActionListener(org.havi.ui.event.HActionListener l);

    /**
     * Sets the command name for the <code>HActionEvent</code> event fired by
     * this <code>HActionable</code>.
     * 
     * @param command
     *            a <code>String</code> used to set the action command.
     * @see org.havi.ui.event.HActionEvent#getActionCommand
     */
    public void setActionCommand(String command);

    /**
     * Associate a sound to be played when the interaction state of the
     * <code>HActionable</code> makes the following transitions:
     * <p>
     * <ul>
     * <li> {@link org.havi.ui.HState#NORMAL_STATE NORMAL_STATE} to
     * {@link org.havi.ui.HState#ACTIONED_STATE ACTIONED_STATE}
     * <li> {@link org.havi.ui.HState#FOCUSED_STATE FOCUSED_STATE} to
     * {@link org.havi.ui.HState#ACTIONED_FOCUSED_STATE ACTIONED_FOCUSED_STATE}
     * </ul>
     * <p>
     * 
     * @param sound
     *            the sound to be played, when the component is actioned. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a <code>null</code>
     *            <code>HSound</code>.
     */
    public void setActionSound(HSound sound);

    /**
     * Return the last action sound set by the <code>setActionSound()</code>
     * method or <code>null</code> if no action sound has been set.
     */
    public HSound getActionSound();

}
