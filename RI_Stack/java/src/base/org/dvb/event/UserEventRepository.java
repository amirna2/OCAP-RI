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

import java.util.Enumeration;
import java.awt.event.KeyEvent;
import org.havi.ui.event.HRcEvent;

/**
 * The application will use this class to define the events that it wants to
 * receive. Events that are able to be put in the repository are defined in the
 * UserEvent class.
 * <p>
 * Where a repository includes a <code>KEY_PRESSED</code> type event without the
 * <code>KEY_RELEASED</code> type event for the same key code or vice versa then
 * exclusive reservations shall be made for both event types but only the one
 * requested shall be received by the listener. Where a repository includes a
 * <code>KEY_TYPED</code> event without the corresponding
 * <code>KEY_PRESSED</code> and <code>KEY_RELEASED</code> events (excluding
 * <code>KEY_PRESSED</code> or <code>KEY_RELEASED</code> events for modifiers),
 * when an exclusive reservation is requested, it shall also be made for those
 * corresponding <code>KEY_PRESSED</code> and <code>KEY_RELEASED</code> events
 * but only the requested event shall be received by the listener.
 * <p>
 * Repositories do not keep a count of the number of times a particular user
 * event is added or removed. Repeatedly adding an event to a repository has no
 * effect. Removing an event removes it regardless of the number of times it has
 * been added. For example,
 * org.dvb.event.UserEventRepository.addUserEvent(UserEvent event) does nothing
 * in case that the event is already in the repository. Events are considered to
 * be already in the repository if an event with the same triplet of family,
 * type and code is already in the repository.
 * <p>
 * If an application loses exclusive access to a repository, it shall lose
 * access to all events defined in that repository. Repositories are resolved
 * when they are passed into the methods of EventManager. Adding or removing
 * events from the repository after those method calls does not affect the
 * subscription to those events.
 * <p>
 * Unless stated otherwise, all constants used in the specification of this
 * class are defined in <code>java.awt.event.KeyEvent</code> and its parent
 * classes and not in this class.
 * 
 * @see UserEvent
 */
public class UserEventRepository extends RepositoryDescriptor
{
    /**
     * The method to construct a new UserEventRepository.
     * 
     * @param name
     *            the name of the repository.
     */
    public UserEventRepository(String name)
    {
        super(name);
    }

    /**
     * Adds the given user event to the repository. The values of the modifiers
     * (if any) in the <code>UserEvent</code> shall be ignored by the MHP
     * terminal. The value of the source used to construct the specified
     * <code>UserEvent</code> shall be ignored by the MHP terminal when the
     * <code>UserEventRepository</code> is used to specify events which an
     * application wants to receive.
     * 
     * @param event
     *            the user event to be added in the repository.
     */
    public void addUserEvent(UserEvent event)
    {
        if (event == null) throw new NullPointerException("UserEvent cannot be null");
        store.put(new HashAdapter(event), event);
    }

    /**
     * Returns the list of the user events that are in the repository.
     * 
     * @return an array which contains the user events that are in the
     *         repository.
     */
    public UserEvent[] getUserEvent()
    {
        synchronized (store)
        {
            UserEvent[] array = new UserEvent[store.size()];
            Enumeration keys = store.keys();
            for (int i = 0; keys.hasMoreElements(); ++i)
            {
                Object key = keys.nextElement();
                array[i] = (UserEvent) store.get(key);
                if (array[i] == null) throw new RuntimeException("key maps to null: " + key);
            }
            return array;
        }
    }

    /**
     * Remove a user event from the repository. Removing a user event which is
     * not in the repository shall have no effect.
     * 
     * @param event
     *            the event to be removed from the repository.
     */
    public void removeUserEvent(UserEvent event)
    {
        store.remove(new HashAdapter(event));
    }

    /**
     * Adds the specified keycode to the repository. Keycodes added in this way
     * shall be listed in the list of user events returned by the
     * <code>getUserEvent</code> method. If a key is already in the repository,
     * this method has no effect. After calling this method, the keycode shall
     * be present for both the <code>KEY_PRESSED</code> and
     * <code>KEY_RELEASED</code> modes.
     * 
     * @param keycode
     *            the key code.
     */
    public void addKey(int keycode)
    {
        synchronized (store)
        {
            addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, keycode, 0, 0));
            addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_RELEASED, keycode, 0, 0));
        }
    }

    /**
     * The method to remove a key from the repository. Removing a key which is
     * not in the repository has no effect. After calling this method, the
     * keycode shall not be present for both the <code>KEY_PRESSED</code> and
     * <code>KEY_RELEASED</code> modes.
     * 
     * @param keycode
     *            the key code.
     */
    public void removeKey(int keycode)
    {
        synchronized (store)
        {
            removeUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, keycode, 0, 0));
            removeUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_RELEASED, keycode, 0, 0));
        }
    }

    /**
     * Adds the key codes for the numeric keys (VK_0, VK_1, VK_2, VK_3, VK_4,
     * VK_5, VK_6, VK_7, VK_8, VK_9). Any key codes already in the repository
     * will not be added again. After calling this method, the keycodes shall be
     * present for both the <code>KEY_PRESSED</code> and
     * <code>KEY_RELEASED</code> modes.
     */
    public void addAllNumericKeys()
    {
        synchronized (store)
        {
            for (int i = KeyEvent.VK_0; i <= KeyEvent.VK_9; ++i)
                addKey(i);
        }
    }

    /**
     * Adds the key codes for the colour keys (VK_COLORED_KEY_0,
     * VK_COLORED_KEY_1, VK_COLORED_KEY_2, VK_COLORED_KEY_3). Any key codes
     * already in the repository will not be added again. After calling this
     * method, the keycodes shall be present for both the
     * <code>KEY_PRESSED</code> and <code>KEY_RELEASED</code> modes.
     */
    public void addAllColourKeys()
    {
        synchronized (store)
        {
            for (int i = HRcEvent.VK_COLORED_KEY_0; i <= HRcEvent.VK_COLORED_KEY_3; ++i)
                addKey(i);
        }
    }

    /**
     * Adds the key codes for the arrow keys (VK_LEFT, VK_RIGHT, VK_UP,
     * VK_DOWN). Any key codes already in the repository will not be added
     * again. After calling this method, the keycodes shall be present for both
     * the <code>KEY_PRESSED</code> and <code>KEY_RELEASED</code> modes.
     */
    public void addAllArrowKeys()
    {
        synchronized (store)
        {
            addKey(KeyEvent.VK_UP);
            addKey(KeyEvent.VK_DOWN);
            addKey(KeyEvent.VK_LEFT);
            addKey(KeyEvent.VK_RIGHT);
        }
    }

    /**
     * Remove the key codes for the numeric keys (VK_0, VK_1, VK_2, VK_3, VK_4,
     * VK_5, VK_6, VK_7, VK_8, VK_9). Key codes from this set which are not
     * present in the repository will be ignored. After calling this method, the
     * keycodes shall not be present for both the <code>KEY_PRESSED</code> and
     * <code>KEY_RELEASED</code> modes.
     */
    public void removeAllNumericKeys()
    {
        synchronized (store)
        {
            for (int i = KeyEvent.VK_0; i <= KeyEvent.VK_9; ++i)
                removeKey(i);
        }
    }

    /**
     * Removes the key codes for the colour keys (VK_COLORED_KEY_0,
     * VK_COLORED_KEY_1, VK_COLORED_KEY_2, VK_COLORED_KEY_3). Key codes from
     * this set which are not present in the repository will be ignored. After
     * calling this method, the keycodes shall not be present for both the
     * <code>KEY_PRESSED</code> and <code>KEY_RELEASED</code> modes.
     */
    public void removeAllColourKeys()
    {
        synchronized (store)
        {
            for (int i = HRcEvent.VK_COLORED_KEY_0; i <= HRcEvent.VK_COLORED_KEY_3; ++i)
                removeKey(i);
        }
    }

    /**
     * Removes the key codes for the arrow keys (VK_LEFT, VK_RIGHT, VK_UP,
     * VK_DOWN). Key codes from this set which are not present in the repository
     * will be ignored. After calling this method, the keycodes shall not be
     * present for both the <code>KEY_PRESSED</code> and
     * <code>KEY_RELEASED</code> modes.
     */
    public void removeAllArrowKeys()
    {
        synchronized (store)
        {
            removeKey(KeyEvent.VK_UP);
            removeKey(KeyEvent.VK_DOWN);
            removeKey(KeyEvent.VK_LEFT);
            removeKey(KeyEvent.VK_RIGHT);
        }
    }

    /**
     * Class used to wrap <code>UserEvent</code> objects, adapting them with a
     * appropriate {@link #equals} and {@link #hashCode} implementations. This
     * is necessary because <code>UserEvent</code> does not define the
     * <code>equals()</code> and <code>hashCode</code> methods which are
     * necessary if a <code>Hashtable</code> (or similar data structure) to be
     * used to store the set of events -- without duplicates.
     * 
     * @author Aaron Kamienski
     */
    private static class HashAdapter
    {
        private UserEvent e;

        public HashAdapter(UserEvent e)
        {
            this.e = e;
        }

        /**
         * @return <code>true</code> if <i>family</i>, <i>type</i>, <i>code</i>,
         *         and <i>keyChar</i> are the same; <code>false</code>
         *         otherwise. Ignores <i>source</i>, <i>modifiers</i>, and
         *         <i>when</i>
         */
        public boolean equals(Object obj)
        {
            HashAdapter a;
            return (this == obj)
                    || ((obj instanceof HashAdapter) && (a = (HashAdapter) obj) != null
                            && (e.getCode() == a.e.getCode()) && (e.getKeyChar() == a.e.getKeyChar())
                            && (e.getType() == a.e.getType()) && (e.getFamily() == a.e.getFamily()));
        }

        /**
         * @return <i>hashcode</i> based on <i>family</i>, <i>type</i>,
         *         <i>code</i>, and <i>keyChar</i>; ignores <i>source</i>,
         *         <i>modifiers</i>, and <i>when</i>
         */
        public int hashCode()
        {
            return e.getCode() ^ (e.getType() << 8) ^ (e.getKeyChar() << 16) ^ (e.getFamily() << 24);
        }

        public int hashCode2()
        {
            return e.getCode() ^ (e.getType() ^ (e.getKeyChar() ^ (e.getFamily() << 8) << 8) << 8);
        }
    }

    /**
     * Storage for UserEvents.
     */
    private java.util.Hashtable store = new java.util.Hashtable();
}
