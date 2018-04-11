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
 * of adjustable numerical value (e.g. a range control).
 * 
 * 
 * <h3>Event Behavior</h3>
 * 
 * Subclasses of {@link org.havi.ui.HComponent} which implement
 * <code>HAdjustmentValue</code> must respond to
 * {@link org.havi.ui.event.HFocusEvent} and
 * {@link org.havi.ui.event.HAdjustmentEvent} events.
 * 
 * <p>
 * Applications should assume that classes which implement
 * <code>HAdjustmentValue</code> can generate events of the types
 * <code>HFocusEvent</code> and <code>HAdjustmentEvent</code> in response to
 * other types of input event.
 * 
 * <p>
 * An application may add one or more <code>HAdjustmentListener</code> listeners
 * to the component. The
 * {@link org.havi.ui.event.HAdjustmentListener#valueChanged} method of the
 * <code>HAdjustmentListener</code> is invoked whenever the value of the
 * <code>HAdjustmentValue</code> is modified.
 * 
 * <p>
 * HAVi adjustment events are discussed in detail in the
 * {@link org.havi.ui.HAdjustmentInputPreferred} interface description.
 * 
 * <h3>Interaction States</h3>
 * 
 * The following interaction states are valid for this
 * <code>HAdjustmentValue</code> component:
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
 * <code>HAdjustmentValue</code> component.
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
 * <code>HAdjustmentValue</code> interface. These classes shall all generate
 * both <code>HFocusEvent</code> and <code>HAdjustmentEvent</code> events in
 * addition to any other events specified in the respective class descriptions.
 * 
 * <p>
 * <ul>
 * <li> {@link org.havi.ui.HRangeValue}
 * </ul>
 * <p>
 * 
 * @see org.havi.ui.HNavigable
 * @see org.havi.ui.HOrientable
 * @see org.havi.ui.HAdjustmentInputPreferred
 * @see org.havi.ui.event.HAdjustmentEvent
 * @see org.havi.ui.event.HAdjustmentListener
 */

public interface HAdjustmentValue extends HNavigable, HAdjustmentInputPreferred
{
    /**
     * Set the unit increment for this <code>HAdjustmentValue</code>.
     * 
     * @param increment
     *            the amount by which the value of the
     *            <code>HAdjustmentValue</code> should change when an
     *            {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_LESS} or
     *            {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_MORE} event
     *            is received. Values of <code>increment</code> less than one
     *            shall be treated as a value of one.
     */
    public void setUnitIncrement(int increment);

    /**
     * Get the unit increment for this <code>HAdjustmentValue</code>.
     * <code>1</code> shall be returned if this method is called before its
     * corresponding set method.
     * 
     * @return the increment value for this <code>HAdjustmentValue</code>.
     */
    public int getUnitIncrement();

    /**
     * Set the block increment for this <code>HAdjustmentValue</code>.
     * 
     * @param increment
     *            the amount by which the value of the
     *            <code>HAdjustmentValue</code> should change when an
     *            {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_PAGE_LESS} or
     *            {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_PAGE_MORE}
     *            event is received. Values of <code>increment</code> less than
     *            one shall be treated as a value of one.
     */
    public void setBlockIncrement(int increment);

    /**
     * Get the block increment for this <code>HAdjustmentValue</code>.
     * <code>1</code> shall be returned if this method is called before its
     * corresponding set method.
     * 
     * @return the block increment value for this <code>HAdjustmentValue</code>.
     */
    public int getBlockIncrement();

    /**
     * Adds the specified HAdjustmentListener to receive
     * <code>HAdjustmentEvents</code> sent from this object. If the listener has
     * already been added further calls will add further references to the
     * listener, which will then receive multiple copies of a single event.
     * 
     * @param l
     *            the HAdjustmentListener to be notified.
     */
    public void addAdjustmentListener(org.havi.ui.event.HAdjustmentListener l);

    /**
     * Removes the specified HAdjustmentListener so that it no longer receives
     * <code>HAdjustmentEvents</code> from this object. If the specified
     * listener is not registered, the method has no effect. If multiple
     * references to a single listener have been registered it should be noted
     * that this method will only remove one reference per call.
     * 
     * @param l
     *            the HAdjustmentListener to be removed from notification.
     */
    public void removeAdjustmentListener(org.havi.ui.event.HAdjustmentListener l);

    /**
     * Associate a sound to be played when the value is modified. The sound is
     * played irrespective of whether an <code>HAdjustmentEvent</code> is sent
     * to one or more listeners.
     * 
     * @param sound
     *            the sound to be played, when the value is modified. If sound
     *            content is already set, the original content is replaced. To
     *            remove the sound specify a null {@link org.havi.ui.HSound}.
     */
    public void setAdjustmentSound(HSound sound);

    /**
     * Get the sound to be played when the value changes. <code>null</code>
     * shall be returned if this method is called before its corresponding set
     * method.
     * 
     * @return The sound played when the value changes
     */
    public HSound getAdjustmentSound();

}
