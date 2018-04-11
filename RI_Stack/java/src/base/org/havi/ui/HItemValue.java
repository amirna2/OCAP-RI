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
 * This interface is implemented by all HAVi UI components which have some form
 * of selectable content (e.g. a list group).
 * 
 * <h3>Event Behavior</h3>
 * 
 * Subclasses of {@link org.havi.ui.HComponent HComponent} which implement
 * {@link org.havi.ui.HItemValue HItemValue} must respond to
 * {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
 * {@link org.havi.ui.event.HItemEvent HItemEvent} events.
 * 
 * <p>
 * Applications should assume that classes which implement
 * {@link org.havi.ui.HItemValue HItemValue} can generate events of the types
 * {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
 * {@link org.havi.ui.event.HItemEvent HItemEvent} in response to other types of
 * input event.
 * 
 * <p>
 * An application may add one or more {@link org.havi.ui.event.HItemListener
 * HItemListener} listeners to the component. The
 * {@link org.havi.ui.event.HItemListener#selectionChanged selectionChanged}
 * method of the {@link org.havi.ui.event.HItemListener HItemListener} is
 * invoked whenever the selection managed by the {@link org.havi.ui.HItemValue
 * HItemValue} is changed.
 * 
 * <p>
 * HAVi item events are discussed in detail in the
 * {@link org.havi.ui.HSelectionInputPreferred HSelectionInputPreferred}
 * interface description.
 * 
 * <h3>Interaction States</h3>
 * 
 * The following interaction states are valid for this
 * {@link org.havi.ui.HItemValue HItemValue} component:
 * 
 * <p>
 * <ul>
 * <li> {@link org.havi.ui.HState#NORMAL_STATE NORMAL_STATE}
 * <li> {@link org.havi.ui.HState#FOCUSED_STATE FOCUSED_STATE}
 * <li> {@link org.havi.ui.HState#DISABLED_STATE DISABLED_STATE}
 * <li> {@link org.havi.ui.HState#DISABLED_FOCUSED_STATE DISABLED_FOCUSED_STATE}
 * </ul>
 * <p>
 * 
 * The state machine diagram below shows the valid state transitions for an
 * {@link org.havi.ui.HItemValue HItemValue} component.
 * 
 * <p>
 * <table border>
 * <tr>
 * <td><img src="../../../images/HxxxValue_state.gif"></td>
 * </tr>
 * </table>
 * <p>
 * 
 * <h3>Platform Classes</h3>
 * 
 * The following HAVi platform classes implement or inherit the
 * {@link org.havi.ui.HItemValue HItemValue} interface. These classes shall all
 * generate both {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
 * {@link org.havi.ui.event.HItemEvent HItemEvent} events in addition to any
 * other events specified in the respective class descriptions.
 * 
 * <p>
 * <ul>
 * <li> {@link org.havi.ui.HListGroup}
 * </ul>
 * <p>
 * 
 * @see org.havi.ui.HNavigable
 * @see org.havi.ui.HOrientable
 * @see org.havi.ui.HSelectionInputPreferred
 * @see org.havi.ui.event.HItemEvent
 * @see org.havi.ui.event.HItemListener
 */

public interface HItemValue extends HNavigable, HSelectionInputPreferred
{
    /**
     * Adds the specified {@link org.havi.ui.event.HItemListener HItemListener}
     * to receive {@link org.havi.ui.event.HItemEvent HItemEvents} sent from
     * this object. If the listener has already been added further calls will
     * add further references to the listener, which will then receive multiple
     * copies of a single event.
     * 
     * @param l
     *            the {@link org.havi.ui.event.HItemListener HItemListener} to
     *            be notified.
     */
    public void addItemListener(org.havi.ui.event.HItemListener l);

    /**
     * Removes the specified {@link org.havi.ui.event.HItemListener
     * HItemListener} so that it no longer receives
     * {@link org.havi.ui.event.HItemEvent HItemEvents} from this object. If the
     * specified listener is not registered, the method has no effect. If
     * multiple references to a single listener have been registered it should
     * be noted that this method will only remove one reference per call.
     * 
     * @param l
     *            the {@link org.havi.ui.event.HItemListener HItemListener} to
     *            be removed from notification.
     */
    public void removeItemListener(org.havi.ui.event.HItemListener l);

    /**
     * Associate a sound to be played when the selection is modified. The sound
     * is played irrespective of whether an {@link org.havi.ui.event.HItemEvent
     * HItemEvent} is sent to one or more listeners.
     * 
     * @param sound
     *            the sound to be played, when the selection is modified. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setSelectionSound(HSound sound);

    /**
     * Get the sound to be played when the selection changes.
     * 
     * @return The sound played when the selection changes
     */
    public HSound getSelectionSound();

}
