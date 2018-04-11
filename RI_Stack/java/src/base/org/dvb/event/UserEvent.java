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

package org.dvb.event;

import org.cablelabs.impl.havi.HaviToolkit;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Represents a user event. A user event is defined by a family, a type and
 * either a code or a character. Unless stated otherwise, all constants used in
 * the specification of this class are defined in
 * <code>java.awt.event.KeyEvent</code> and its parent classes.
 */
public class UserEvent extends java.util.EventObject
{

    /**
     * the family for events that are coming from the remote control or from the
     * keyboard.
     */
    public static final int UEF_KEY_EVENT = 1;

    /**
     * Constructor for a new UserEvent object representing a key being pressed.
     * 
     * @param source
     *            the <code>EventManager</code> which is the source of the event
     * @param family
     *            the event family.
     * @param type
     *            the event type. Either one of KEY_PRESSED or KEY_RELEASED.
     * @param code
     *            the event code. One of the constants whose name begins in
     *            "VK_" defined in java.awt.event.KeyEvent or
     *            org.havi.ui.event.HRcEvent.
     * @param modifiers
     *            the modifiers active when the key was pressed. These have the
     *            same semantics as modifiers in
     *            <code>java.awt.event.KeyEvent</code>.
     * @param when
     *            a long integer that specifys the time the event occurred
     * 
     */
    public UserEvent(Object source, int family, int type, int code, int modifiers, long when)
    {
        super(source);

        this.family = family;
        this.type = type;
        this.code = code;
        this.keyChar = CHAR_UNDEFINED;
        this.modifiers = modifiers;
        this.when = when;
    }

    /**
     * Constructor for a new UserEvent object representing a key being typed.
     * This is the combination of a key being pressed and then being released.
     * The type of UserEvents created with this constructor shall be KEY_TYPED.
     * Key combinations which do not result in characters, such as keys like the
     * red key on a remote control, shall not generate KEY_TYPED events.
     * <code>KEY_TYPED</code> events shall have no modifiers and hence shall not
     * report any modifiers as being down.
     * 
     * @param source
     *            the <code>EventManager</code> which is the source of the event
     * @param family
     *            the event family.
     * @param keyChar
     *            the character typed
     * @param when
     *            a long integer that specifys the time the event occurred
     * @since MHP 1.0.1
     */
    public UserEvent(Object source, int family, char keyChar, long when)
    {
        super(source);

        this.family = family;
        this.type = KeyEvent.KEY_TYPED;
        this.code = KeyEvent.VK_UNDEFINED;
        this.keyChar = keyChar;
        this.modifiers = 0;
        this.when = when;
    }

    /**
     * Returns the event family. Could be UEF_KEY_EVENT.
     * 
     * @return an int representing the event family.
     */
    public int getFamily()
    {
        return family;
    }

    /**
     * Returns the event type. Could be KEY_PRESSED, KEY_RELEASED or KEY_TYPED.
     * 
     * @return an int representing the event type.
     */
    public int getType()
    {
        return type;
    }

    /**
     * Returns the event code. For KEY_TYPED events, the code is VK_UNDEFINED.
     * 
     * @return an int representing the event code.
     */
    public int getCode()
    {
        return code;
    }

    /**
     * Returns the character associated with the key in this event. If no valid
     * Unicode character exists for this key event, keyChar is CHAR_UNDEFINED.
     * 
     * @return a character
     * @since MHP 1.0.1
     */
    public char getKeyChar()
    {
        return keyChar;
    }

    /**
     * Returns the modifiers flag for this event. This method shall return 0 for
     * UserEvents constructed using a constructor which does not include an
     * input parameter specifying the modifiers.
     * 
     * @return the modifiers flag for this event
     * @since MHP 1.0.1
     */
    public int getModifiers()
    {
        return modifiers;
    }

    /**
     * Returns whether or not the Shift modifier is down on this event. This
     * method shall return false for UserEvents constructed using a constructor
     * which does not include an input parameter specifying the modifiers.
     * 
     * @return whether the Shift modifier is down on this event
     * @since MHP 1.0.1
     */
    public boolean isShiftDown()
    {
        return (modifiers & InputEvent.SHIFT_MASK) != 0;
    }

    /**
     * Returns whether or not the Control modifier is down on this event. This
     * method shall return false for UserEvents constructed using a constructor
     * which does not include an input parameter specifying the modifiers.
     * 
     * @return whether the Control modifier is down on this event
     * @since MHP 1.0.1
     */
    public boolean isControlDown()
    {
        return (modifiers & InputEvent.CTRL_MASK) != 0;
    }

    /**
     * Returns whether or not the Meta modifier is down on this event. This
     * method shall return false for UserEvents constructed using a constructor
     * which does not include an input parameter specifying the modifiers.
     * 
     * @return whether the Meta modifier is down on this event
     * @since MHP 1.0.1
     */
    public boolean isMetaDown()
    {
        return (modifiers & InputEvent.META_MASK) != 0;
    }

    /**
     * Returns whether or not the Alt modifier is down on this event. This
     * method shall return false for UserEvents constructed using a constructor
     * which does not include an input parameter specifying the modifiers.
     * 
     * @return whether the Alt modifier is down on this event
     * @since MHP 1.0.1
     */
    public boolean isAltDown()
    {
        return (modifiers & InputEvent.ALT_MASK) != 0;
    }

    /**
     * Returns the timestamp of when this event occurred.
     * 
     * @return a long
     * @since MHP 1.0.2
     */
    public long getWhen()
    {
        return when;
    }

    private int family;

    private int type;

    protected int code;

    protected char keyChar;

    protected int modifiers;

    private long when;

    /**
     * The value to be used for {@link KeyEvent#CHAR_UNDEFINED}. This is used
     * rather than the actual value because the required value may be different
     * than the actual value. OCAP-1.0 requires a value of zero be used because
     * that was the value as defined in JDK 1.1.8 (upon which OCAP-1.0 is
     * based). Java2 (including J2ME PBP 1.0) changed this value to 65535.
     */
    private static final char CHAR_UNDEFINED = HaviToolkit.getCharUndefined();
}
