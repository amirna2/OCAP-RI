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

package org.cablelabs.impl.havi;

/**
 * Class which maintains a mapping of <i>keycodes</i> to other objects. The most
 * common use is to maintain the mapping of <code>KeyEvent</code> keycodes to
 * <code>HNavigable</code> components for focus traversals.
 * <p>
 * The class works in much the same was as a <code>Hashtable</code>, but it
 * takes <code>int</code>s as keys instead of <code>Object</code>s and does not
 * provide quite as much functionality.
 * 
 * @author Aaron Kamienski
 * @version $Id: KeySet.java,v 1.3 2002/11/07 21:13:41 aaronk Exp $
 */
public class KeySet
{
    /**
     * Constructs a new, empty <code>KeySet</code>.
     */
    public KeySet()
    {
        table = new Entry[INITIAL_CAPACITY | 1]; // must be odd
    }

    /**
     * Maps the specified key to the specified value in this <code>KeySet</code>
     * . If the <code>value</code> is <code>null</code> then any previously
     * added mapping is effectively removed.
     * 
     * @param key
     *            the <i>keycode</i> key
     * @param value
     *            the value
     * @return the old value
     */
    public synchronized Object put(int key, Object value)
    {
        Entry t[] = table;
        Entry prev = null;
        int hash = (key & 0x7FFFFFFF) % t.length;

        // Find existing entry and update
        for (Entry e = t[hash]; e != null; e = e.next)
        {
            if (e.key == key)
            {
                Object old = e.value;

                // Replace entry
                if (value != null)
                    e.value = value;
                else
                {
                    // Remove entry
                    if (prev == null)
                        t[hash] = e.next;
                    else
                        prev.next = e.next;
                    --size;
                }
                return old;
            }
            prev = e;
        }
        // If attempting to delete, then we are done
        if (value == null) return null;

        // Grow hashtable if necessary
        if (size >= threshold)
        {
            rehash();

            t = table;
            hash = (key & 0x7FFFFFFF) % t.length;
        }

        // Place new entry in table
        Entry e = new Entry(key, value, t[hash]);
        t[hash] = e;
        ++size;

        return null;
    }

    /**
     * Returns the value to which the specified key is mapped in this
     * <code>KeySet</code>.
     * 
     * @param key
     *            the <i>keycode</i> key
     * @return the value to which the specified key is mapped in this
     *         <code>KeySet</code>; <code>null</code> if the key is does not map
     *         to any value in this <code>KeySet</code>
     */
    public synchronized Object get(int key)
    {
        Entry t[] = table;
        int hash = (key & 0x7FFFFFFF) % t.length;

        for (Entry e = t[hash]; e != null; e = e.next)
        {
            if (e.key == key) return e.value;
        }
        return null;
    }

    /**
     * Rehashes the contents of the hashtable into a hashtable with a larger
     * capacity. This method is called automatically when the number of keys in
     * the hashtable exceeds this hashtable's capacity and load factor.
     */
    private void rehash()
    {
        int capacity = table.length;
        int newCapacity = capacity * 2 + 1; // must be odd

        Entry t[] = table;
        Entry newT[] = new Entry[newCapacity];

        for (int i = 0; i < capacity; ++i)
        {
            for (Entry e = t[i]; e != null;)
            {
                Entry newE = e;
                e = e.next;

                int hash = (newE.key & 0x7FFFFFFF) % newCapacity;
                newE.next = newT[hash];
                newT[hash] = newE;
            }
        }
        threshold = (int) (newCapacity * LOAD);
        table = newT;
    }

    /**
     * Returns the set of keys as an array.
     * 
     * @return an <code>int[]</code> containing all of the current keys
     */
    public synchronized int[] getKeys()
    {
        Entry[] t = table;
        int[] array = new int[size];
        int j = 0;

        for (int i = 0; i < t.length; ++i)
        {
            for (Entry e = t[i]; e != null; e = e.next)
                array[j++] = e.key;
        }
        return array;
    }

    /**
     * Returns the set of keys as an array. This is the same as {@link #getKeys}
     * except it returns <code>null</code> instead of an empty
     * <code>int[]</code>.
     * 
     * @return an <code>int[]</code> containing all of the current keys;
     *         <code>null</code> if no keys currently map to values
     */
    public synchronized int[] getKeysNull()
    {
        return (size == 0) ? null : getKeys();
    }

    /**
     * Returns the set of keys as an array of <code>char</code>s.
     * 
     * @return an <code>char[]</code> containing all of the current keys
     */
    public synchronized char[] getChars()
    {
        Entry[] t = table;
        char[] array = new char[size];
        int j = 0;

        for (int i = 0; i < t.length; ++i)
        {
            for (Entry e = t[i]; e != null; e = e.next)
                array[j++] = (char) e.key;
        }
        return array;
    }

    /**
     * Returns the set of keys as an array. This is the same as
     * {@link #getChars} except it returns <code>null</code> instead of an empty
     * <code>char[]</code>.
     * 
     * @return an <code>char[]</code> containing all of the current keys;
     *         <code>null</code> if no keys currently map to values
     */
    public synchronized char[] getCharsNull()
    {
        return (size == 0) ? null : getChars();
    }

    /**
     * Linked list of key/value mappings.
     */
    private class Entry
    {
        int key;

        Object value;

        Entry next;

        /**
         * Creates a new entry in a linked-list of key/value mappings.
         */
        public Entry(int key, Object value, Entry next)
        {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    /** Table of elements. */
    private Entry[] table;

    /** The size of the table. */
    private int size;

    /** The threshold at which the table will be rehashed. */
    private int threshold = (int) (INITIAL_CAPACITY * LOAD);

    /** Initial table capacity. */
    private static final int INITIAL_CAPACITY = 10;

    /** Used to determine the point where we will extend the table. */
    private static final float LOAD = 0.75f;

}
