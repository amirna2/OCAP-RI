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

package org.cablelabs.impl.manager.event;

import org.cablelabs.impl.havi.KeySet;

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.dvb.event.UserEvent;

/**
 * An object that represents an instance of a {@link KeyEvent} or
 * {@link UserEvent} (of family {@link org.dvb.event.UserEvent#UEF_KEY_EVENT}).
 * Objects of this type are used to track events to be dispatched to a listener
 * (as listeners are only invoked for specific
 * {@link org.dvb.event.UserEvent#getType() types} of an event).
 * <p>
 * 
 * This class overrides {@link #equals} and {@link #hashCode} allowing it to be
 * used to key a <code>Hashtable</code>.
 * <p>
 * 
 * When creating a <code>Key</code>, possible field values are enforced to
 * ensure consistent operation. When the {@link #type keyType} is
 * {@link KeyEvent#KEY_PRESSED} or {@link KeyEvent#KEY_RELEASED}, the
 * {@link #ch keyChar} is always {@link KeyEvent#CHAR_UNDEFINED}. When the
 * <code>keyType</code> is {@link KeyEvent#KEY_TYPED}, the {@link #code keyCode}
 * is always {@link KeyEvent#VK_UNDEFINED}. This corresponds to the field values
 * enforced by {@link org.dvb.event.UserEvent} and ensures that we don't get
 * caught up comparing values that don't matter. As a result, if
 * <code>KEY_PRESSED</code> includes a <
 * <p>
 * 
 * <h4>Flyweight</h4>
 * The implementation of {@link #createKey(int, int, char)} and similar method
 * implement <code>Key</code> as <i>flyweight</i> if {@link #USE_FLYWEIGHT} is
 * defined as <code>true</code>. This means that repeated calls to
 * <code>createKey()</code> will return the same instance of <code>Key</code>
 * for the same parameters, as long as that <code>Key</code> has not been
 * garbage collected. The point of this is to lessen the use of memory when
 * multiple applications reference the same keys for shared
 * <code>UserEventListener</code>s.
 * <p>
 * Note that when <code>USE_FLYWEIGHT</code> is <code>false</code> all flyweight
 * related code should be removed by compiler as dead-code. If the compiler
 * misses anything, it is the expectation that post-processing (e.g., during
 * obfuscation) cleans up remaining dead code.
 * 
 * @author Aaron Kamienski
 * 
 * @see "Design Patterns: Elements of Reusable Object-Oriented Software"
 */
class Key
{
    /**
     * Creates an instance of Key.
     * 
     * A new <code>Key</code> is returned based upon the given parameters.
     * 
     * @param keyCode
     *            as from {@link KeyEvent#getKeyCode} or
     *            {@link org.dvb.event.UserEvent#getCode}
     * @param keyType
     *            as from {@link AWTEvent#getID KeyEvent.getID()} or
     *            {@link org.dvb.event.UserEvent#getType}
     * @param keyChar
     *            as from {@link KeyEvent#getKeyChar()} or
     *            {@link org.dvb.event.UserEvent#getKeyChar()}
     * 
     * @see Key
     */
    static Key createKey(int keyCode, int keyType, char keyChar)
    {
        if (!USE_FLYWEIGHT)
            return new Key(keyCode, keyType, keyChar);
        else
        {
            int lookup;
            KeySet set;
            // Determine the KeySet and lookup value for the given keyType
            switch (keyType)
            {
                case KeyEvent.KEY_PRESSED:
                    set = pressed;
                    lookup = keyCode;
                    break;
                case KeyEvent.KEY_RELEASED:
                    set = released;
                    lookup = keyCode;
                    break;
                case KeyEvent.KEY_TYPED:
                    set = typed;
                    lookup = keyChar;
                    break;
                default:
                    return new Key(keyCode, keyType, keyChar);
            }
            KeyReference ref = (KeyReference) set.get(lookup);
            Key key = (Key) ((ref != null) ? ref.get() : null);
            // If key wasn't found, create one
            if (key == null)
            {
                // Create a new Key and KeyReference to add to KeySet
                if (DEBUG) System.out.println("CREATE: " + ((char) lookup) + " " + (ref == null));
                key = new Key(keyCode, keyType, keyChar);
                set.put(lookup, new KeyReference(lookup, set, key, queue));

                // Cleanup stale entries in set while we are here
                while ((ref = (KeyReference) queue.poll()) != null)
                    ref.cleanup();
            }
            else if (DEBUG) System.out.println("FLYWEIGHT: " + ((char) lookup));

            return key;
        }
    }

    /**
     * Creates an instance of Key based upon the given <code>UserEvent</code>.
     * 
     * A new <code>Key</code> is returned with the <i>keyCode</i>,
     * <i>keyType</i>, and <i>keyChar</i> from the given <code>UserEvent</code>.
     * 
     * @param key
     *            the <code>UserEvent</code> to pull {@link #code},
     *            {@link #type}, {@link #ch} from
     * 
     * @see Key
     */
    static Key createKey(UserEvent key)
    {
        return createKey(key.getCode(), key.getType(), key.getKeyChar());
    }

    /**
     * Creates an instance of Key based upon the given instance.
     * 
     * A new <code>Key</code> is returned with the <i>keyCode</i> and
     * <i>keyChar</i> from the given <i>key</i> with the given <i>keyType</i>.
     * 
     * @param key
     *            the original key
     * @param keyType
     *            the <i>type</i> of the new key
     * 
     * @see Key
     */
    static Key createKey(Key key, int keyType)
    {
        return createKey(key.code, keyType, key.ch);
    }

    /**
     * Creates an instance of Key.
     * 
     * @param keyCode
     *            as from {@link KeyEvent#getKeyCode} or
     *            {@link org.dvb.event.UserEvent#getCode}
     * @param keyType
     *            as from {@link AWTEvent#getID KeyEvent.getID()} or
     *            {@link org.dvb.event.UserEvent#getType}
     * @param keyChar
     *            as from {@link KeyEvent#getKeyChar()} or
     *            {@link org.dvb.event.UserEvent#getKeyChar()}
     */
    private Key(int keyCode, int keyType, char keyChar)
    {
        type = keyType;
        switch (type)
        {
            case KeyEvent.KEY_PRESSED:
            case KeyEvent.KEY_RELEASED:
                code = keyCode;
                ch = EventMgr.CHAR_UNDEFINED;
                break;
            case KeyEvent.KEY_TYPED:
                code = KeyEvent.VK_UNDEFINED;
                ch = keyChar;
                break;
            default:
                // TODO: Unexpected, what to do?
                code = keyCode;
                ch = keyChar;
        }
    }

    /**
     * Creates an instance of Key.
     * 
     * @param key
     *            the <code>UserEvent</code> to pull {@link #code},
     *            {@link #type}, {@link #ch} from
     */
    Key(UserEvent key)
    {
        this(key.getCode(), key.getType(), key.getKeyChar());
    }

    /**
     * Returns this <code>Key</code> as a <code>UserEvent</code>.
     * 
     * @return a new <code>UserEvent</code> representation of this
     *         <code>Key</code>
     */
    UserEvent getUserEvent()
    {
        switch (type)
        {
            default:
                // Unexpected
            case KeyEvent.KEY_PRESSED:
            case KeyEvent.KEY_RELEASED:
                return new UserEvent("", org.dvb.event.UserEvent.UEF_KEY_EVENT, type, code, 0, 0L);
            case KeyEvent.KEY_TYPED:
                return new UserEvent("", org.dvb.event.UserEvent.UEF_KEY_EVENT, ch, 0L);
        }
    }

    /**
     * Overrides {@link Object#equals}.
     * 
     * @return <code>true</code> if <i>obj</i> is a <code>Key</code> with an
     *         identical {@link #code}, {@link #type}, and {@link #ch}
     */
    public boolean equals(Object obj)
    {
        return (obj instanceof Key) && ((Key) obj).code == code && ((Key) obj).type == type && ((Key) obj).ch == ch;
    }

    /**
     * Overrides {@link Object#hashCode}.
     * 
     * @return hash code derived from {@link #code}, {@link #type}, and
     *         {@link #ch}
     */
    public int hashCode()
    {
        return code ^ (type << 24) ^ (ch << 16);
    }

    /**
     * Overrides {@link Object#toString()}.
     * 
     * @return a <code>String</code> representation of this <code>Key</code>
     */
    public String toString()
    {
        return "Key[" + code + "," + type + ((ch == EventMgr.CHAR_UNDEFINED || ch == 0) ? "]" : (",'" + ch + "']"));
    }

    /**
     * The virtual key code.
     * 
     * @see KeyEvent#getKeyCode()
     * @see org.dvb.event.UserEvent#getCode()
     */
    final int code;

    /**
     * The type of key event.
     * 
     * @see KeyEvent#getID()
     * @see org.dvb.event.UserEvent#getType()
     */
    final int type;

    /**
     * The key char (for when {@link #type} is {@link KeyEvent#KEY_TYPED}).
     * 
     * @see KeyEvent#getKeyChar()
     * @see org.dvb.event.UserEvent#getKeyChar()
     */
    final char ch;

    /**
     * If <code>true</code>, then the Flyweight pattern is used.
     * <code>Keys</code> are managed in three <code>KeySet</code>s, one for each
     * <i>type</i> of <code>Key</code>.
     * <p>
     * This can be set to <code>true</code> if it seems as if too many
     * <code>Key</code> objects are being created. Otherwise, because of the
     * extra code involved, it is suggested that it stay <code>false</code>.
     * Note that if <code>false</code> all dead code is expected to be removed
     * by the compiler and post-compilation obfuscation/optimizer steps. I.e.,
     * there should be no cost to leaving this code in place when disabled at
     * compile-time.
     */
    private static final boolean USE_FLYWEIGHT = false;

    /**
     * A simple <code>Reference</code> extension that allows for cleanup
     * following collection. Extends <code>WeakReference</code> so that keys are
     * forgotten as soon as they are no longer needed (or referenced elsewhere).
     * <p>
     * Only used if {@link #USE_FLYWEIGHT} is <code>true</code>.
     * 
     * @author Aaron Kamienski
     */
    private static class KeyReference extends WeakReference
    {
        /**
         * Creates an instance of KeyReference.
         * 
         * @param lookup
         *            the <i>keyCode</i> or <i>keyChar</i> used to perform
         *            lookups
         * @param key
         *            the <code>Key</code> object being referenced
         */
        KeyReference(int lookup, KeySet set, Key key, ReferenceQueue queue)
        {
            super(key, queue);
            this.set = set;
            this.lookup = lookup;
        }

        /**
         * Returns the <code>Key</code> referenced by this
         * <code>Reference</code> object or <code>null</code> if it has since
         * been collected.
         * 
         * @return <code>Key</code> or <code>null</code>
         */
        Key getKey()
        {
            return (Key) get();
        }

        /**
         * Removes the this old reference from the associated
         * <code>KeySet</code>.
         */
        void cleanup()
        {
            synchronized (set)
            {
                if (set.get(lookup) == this)
                {
                    set.put(lookup, null);
                    if (DEBUG) System.out.println("CLEANUP: " + ((char) lookup));
                }
                else if (DEBUG) System.out.println("NO CLEANUP: " + ((char) lookup));
            }
        }

        private KeySet set;

        private int lookup;
    }

    /**
     * The set of <code>KEY_PRESSED</code> <code>Key</code>s as a
     * <i>flyweight</i>.
     * <p>
     * Only used if {@link #USE_FLYWEIGHT} is <code>true</code>.
     */
    private static final KeySet pressed = USE_FLYWEIGHT ? new KeySet() : null;

    /**
     * The set of <code>KEY_RELEASED</code> <code>Key</code>s as a
     * <i>flyweight</i>.
     * <p>
     * Only used if {@link #USE_FLYWEIGHT} is <code>true</code>.
     */
    private static final KeySet released = USE_FLYWEIGHT ? new KeySet() : null;

    /**
     * The set of <code>KEY_TYPED</code> <code>Key</code>s as a
     * <i>flyweight</i>.
     * <p>
     * Only used if {@link #USE_FLYWEIGHT} is <code>true</code>.
     */
    private static final KeySet typed = USE_FLYWEIGHT ? new KeySet() : null;

    /**
     * The <code>ReferenceQueue</code> used to clean up the {@link #pressed},
     * {@link #released}, and {@link #typed} <code>KeySet</code>s after
     * <code>Key</code>s are collected.
     * <p>
     * Only used if {@link #USE_FLYWEIGHT} is <code>true</code>.
     */
    private static final ReferenceQueue queue = USE_FLYWEIGHT ? new ReferenceQueue() : null;

    /**
     * Enable <i>debug</i> output for flyweight. Basically allows print-out of
     * flyweight operations (creation, lookup, etc). The output goes to
     * <code>System.out</code> to avoid any logging. This should generally be
     * set to <code>false</code> at all times except during
     * development/debugging.
     */
    private static final boolean DEBUG = USE_FLYWEIGHT && false;
}
