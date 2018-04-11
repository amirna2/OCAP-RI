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

package org.ocap.event;

import org.ocap.system.MonitorAppPermission;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * Represents a user event. A user event is defined by a family, a type and
 * either a code or a character. Unless stated otherwise, all constants used in
 * this class are defined in <code>org.ocap.ui.event.OcRcEvent</code>,
 * <code>java.awt.event.KeyEvent</code> and their parent classes.
 */
//findbugs complains about this pattern - shadowing superclass' name.
//Unfortunately, its a common pattern in the RI (so we ignore it).
public class UserEvent extends org.dvb.event.UserEvent
{
    /**
     * Constructor for a new UserEvent object representing a key being pressed.
     * 
     * @param source
     *            the <code>EventManager</code> which is the source of the
     *            event.
     * @param family
     *            the event family.
     * @param type
     *            the event type. Either one of KEY_PRESSED or KEY_RELEASED.
     * @param code
     *            the event code. One of the constants whose name begins in
     *            "VK_" defined in java.ui.event.KeyEvent, org.havi.ui.event or
     *            org.ocap.ui.event.OcRcEvent.
     * @param modifiers
     *            the modifiers active when the key was pressed. These have the
     *            same semantics as modifiers in
     *            <code>java.awt.event.KeyEvent</code>.
     * @param when
     *            a long integer that specifies the time the event occurred.
     * 
     */
    public UserEvent(Object source, int family, int type, int code, int modifiers, long when)
    {
        super(source, family, type, code, modifiers, when);
    }

    /**
     * Constructor for a new UserEvent object representing a key being typed.
     * This is the combination of a key being pressed and then being released.
     * The type of UserEvents created with this constructor shall be KEY_TYPED.
     * Key combinations which do not result in characters, such as action keys
     * like F1, shall not generate KEY_TYPED events.
     * 
     * @param source
     *            the <code>EventManager</code> which is the source of the event
     * @param family
     *            the event family.
     * @param keyChar
     *            the character typed
     * @since MHP 1.0.1
     * @param when
     *            a long integer that specifies the time the event occurred
     */
    public UserEvent(Object source, int family, char keyChar, long when)
    {
        super(source, family, keyChar, when);
    }

    /**
     * Modifies the event code. For KEY_TYPED events, the code is VK_UNDEFINED.
     * 
     * @throws SecurityException
     *             if the caller does not have monitorapplication permission
     *             ("filterUserEvents").
     * 
     * @since OCAP 1.0
     * 
     */
    public void setCode(int code)
    {
        checkPermission();
        this.code = code;
    }

    /**
     * Modifies the character associated with the key in this event. If no valid
     * Unicode character exists for this key event, keyChar must be
     * CHAR_UNDEFINED.
     * 
     * @throws SecurityException
     *             if the caller does not have monitorapplication permission
     *             ("filterUserEvents").
     * 
     * @since OCAP 1.0
     */
    public void setKeyChar(char keychar)
    {
        checkPermission();
        this.keyChar = keychar;
    }

    /**
     * Called by <code>setCode()</code> and <code>setKeyChar</code> to verify
     * adequate permissions.
     * 
     * @throws SecurityException
     *             if the caller does not have monitorapplication permission
     *             ("filterUserEvents").
     */
    private void checkPermission()
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("filterUserEvents"));
    }
}
