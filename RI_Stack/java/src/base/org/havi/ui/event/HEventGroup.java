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

import org.cablelabs.impl.havi.KeySet;

/**
 * Represents a group of key codes. Groups do not keep a count of the number of
 * times a particular key code is added or removed. Repeatedly adding an event
 * to a group has no effect. Removing an event removes it regardless of the
 * number of times it has been added. Groups are resolved when they are passed
 * into the methods of HScene. Adding or removing events from the group after
 * those method calls does not affect the subscription to those events.
 */
public class HEventGroup
{

    /**
     * Constructor for an empty event group
     */
    public HEventGroup()
    {
        set = new KeySet();
    }

    /**
     * A shortcut to create a new key event type entry in the group. If a key is
     * already in the group, this method has no effect.
     * 
     * @param keycode
     *            the key code.
     */
    public void addKey(int keycode)
    {
        set.put(keycode, this);
    }

    /**
     * The method to remove a key from the group. Removing a key which is not in
     * the group has no effect.
     * 
     * @param keycode
     *            the key code.
     */
    public void removeKey(int keycode)
    {
        set.put(keycode, null);
    }

    /**
     * Adds the key codes for the numeric keys (VK_0, VK_1, VK_2, VK_3, VK_4,
     * VK_5, VK_6, VK_7, VK_8, VK_9). Any key codes already in the group will
     * not be added again.
     */
    public void addAllNumericKeys()
    {
        addAll(numeric);
    }

    /**
     * Adds the key codes for the colour keys (VK_COLORED_KEY_0,
     * VK_COLORED_KEY_1, VK_COLORED_KEY_2, VK_COLORED_KEY_3). Any key codes
     * already in the group will not be added again.
     */
    public void addAllColourKeys()
    {
        addAll(colour);
    }

    /**
     * Adds the key codes for the arrow keys (VK_LEFT, VK_RIGHT, VK_UP,
     * VK_DOWN). Any key codes already in the group will not be added again.
     */
    public void addAllArrowKeys()
    {
        addAll(arrow);
    }

    /**
     * Remove the key codes for the numeric keys (VK_0, VK_1, VK_2, VK_3, VK_4,
     * VK_5, VK_6, VK_7, VK_8, VK_9). Key codes from this set which are not
     * present in the group will be ignored.
     */
    public void removeAllNumericKeys()
    {
        removeAll(numeric);
    }

    /**
     * Removes the key codes for the colour keys (VK_COLORED_KEY_0,
     * VK_COLORED_KEY_1, VK_COLORED_KEY_2, VK_COLORED_KEY_3). Key codes from
     * this set which are not present in the group will be ignored.
     */
    public void removeAllColourKeys()
    {
        removeAll(colour);
    }

    /**
     * Removes the key codes for the arrow keys (VK_LEFT, VK_RIGHT, VK_UP,
     * VK_DOWN). Key codes from this set which are not present in the group will
     * be ignored.
     */
    public void removeAllArrowKeys()
    {
        removeAll(arrow);
    }

    /**
     * Return the key codes contained in this event group.
     */
    public int[] getKeyEvents()
    {
        return set.getKeys();
    }

    /**
     * Add all keys in the given array.
     * 
     * @param keys
     *            array of keys to add
     */
    private void addAll(int keys[])
    {
        for (int i = 0; i < keys.length; ++i)
            addKey(keys[i]);
    }

    /**
     * Remove all keys in the given array.
     * 
     * @param keys
     *            array of keys to remove
     */
    private void removeAll(int keys[])
    {
        for (int i = 0; i < keys.length; ++i)
            removeKey(keys[i]);
    }

    /**
     * An array containing all of the numeric keys.
     * 
     * @see #addAllNumericKeys()
     * @see #removeAllNumericKeys()
     */
    private static final int numeric[] = { HKeyEvent.VK_0, HKeyEvent.VK_1, HKeyEvent.VK_2, HKeyEvent.VK_3,
            HKeyEvent.VK_4, HKeyEvent.VK_5, HKeyEvent.VK_6, HKeyEvent.VK_7, HKeyEvent.VK_8, HKeyEvent.VK_9, };

    /**
     * An array containing all of the colour keys.
     * 
     * @see #addAllColourKeys()
     * @see #removeAllColourKeys()
     */
    private static final int colour[] = { HKeyEvent.VK_COLORED_KEY_0, HKeyEvent.VK_COLORED_KEY_1,
            HKeyEvent.VK_COLORED_KEY_2, HKeyEvent.VK_COLORED_KEY_3, };

    /**
     * An array containing all of the arrow keys.
     * 
     * @see #addAllArrowKeys()
     * @see #removeAllArrowKeys()
     */
    private static final int arrow[] = { HKeyEvent.VK_LEFT, HKeyEvent.VK_RIGHT, HKeyEvent.VK_UP, HKeyEvent.VK_DOWN };

    /**
     * The set of keys that represents this <code>HEventGroup</code>.
     */
    private KeySet set;
}
