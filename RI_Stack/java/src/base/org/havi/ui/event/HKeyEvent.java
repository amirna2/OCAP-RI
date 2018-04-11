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

package org.havi.ui.event;

import java.awt.Component;

/**
 * An {@link org.havi.ui.event.HKeyEvent HKeyEvent} event is used to interact
 * with a component implementing the {@link org.havi.ui.HKeyboardInputPreferred
 * HKeyboardInputPreferred} interface as follows:
 * 
 * <p>
 * <ul>
 * 
 * <li>An {@link org.havi.ui.event.HKeyEvent HKeyEvent} event may be sent from
 * the HAVi system to inform the component about key-input. The source of the
 * input may be either a real or a virtual keyboard.
 * 
 * <li>An {@link org.havi.ui.event.HKeyEvent HKeyEvent} event is sent from the
 * component to all registered {@link org.havi.ui.event.HKeyListener
 * HKeyListeners} whenever the component received input.
 * 
 * </ul>
 * <p>
 * 
 * All interoperable HAVi components which expect to receive
 * {@link org.havi.ui.event.HKeyEvent HKeyEvent} events must either implement
 * the {@link org.havi.ui.HKeyboardInputPreferred HKeyboardInputPreferred}
 * interface or subclass components providing the <code>processHKeyEvent({@link
 * org.havi.ui.event.HKeyEvent HKeyEvent})</code> method.
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
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @see org.havi.ui.HKeyboardInputPreferred#processHKeyEvent(org.havi.ui.event.HKeyEvent)
 * 
 * @author Aaron Kamienski
 * 
 */
public class HKeyEvent extends HRcEvent
{

    /**
     * Constructs an {@link org.havi.ui.event.HKeyEvent HKeyEvent} object with
     * the specified source component, type, modifiers and key.
     * 
     * @param source
     *            the object where the event originated.
     * @param id
     *            This is the value that will be returned by the event object's
     *            <code>getID()</code> method.
     * @param when
     *            the time stamp for this event.
     * @param modifiers
     *            indication of any modification keys that are active for this
     *            event.
     * @param keyCode
     *            the code of the key associated with this event.
     * @param keyChar
     *            the character representation of the key associated with this
     *            event.
     */
    public HKeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar)
    {
        super(source, id, when, modifiers, keyCode, keyChar);
    }

    /**
     * Constructs an {@link org.havi.ui.event.HKeyEvent HKeyEvent} object with
     * the specified source component, type, modifiers and key.
     * 
     * @param source
     *            the object where the event originated.
     * @param id
     *            This is the value that will be returned by the event object's
     *            <code>getID()</code> method.
     * @param when
     *            the time stamp for this event.
     * @param modifiers
     *            indication of any modification keys that are active for this
     *            event.
     * @param keyCode
     *            the code of the key associated with this event.
     * @deprecated See explanation in java.awt.event.KeyEvent.
     */
    /*
     * public HKeyEvent(Component source, int id, long when, int modifiers, int
     * keyCode) { super(source, id, when, modifiers, keyCode); }
     */

}
