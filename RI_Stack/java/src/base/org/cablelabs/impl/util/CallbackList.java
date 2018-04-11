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

package org.cablelabs.impl.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Vector;

/**
 * An instance of <code>CallbackList</code> maintains a prioritized list of
 * callbacks.
 * <p>
 * All callbacks registered with a single instance of this object must be of the
 * same type, which is indicated by a {@link Class} object passed to the
 * constructor {@link #CallbackList(Class)}. If the type of a callback, added by
 * the {@link #addCallback(Object, int)} or removed by
 * {@link #removeCallback(Object)} does not match the class type passed to the
 * constructor, a {@link ClassCastException} will be thrown by those methods.
 * 
 * @author Todd Earles
 */
public class CallbackList
{
    /**
     * Construct a <code>CallbackList</code> for a specified callback type.
     * 
     * @param callbackClass
     *            All callbacks registered with this list must be instances of
     *            this class.
     * @throws ClassCastException
     *             If the <code>callbackClass</code> is not supported.
     */
    public CallbackList(Class callbackClass)
    {
        // Save arguments
        this.callbackClass = callbackClass;
    }

    /** The callback class for all callbacks maintained by this CallbackList */
    private final Class callbackClass;

    /** The callback list */
    private final Vector callbackList = new Vector();

    /**
     * Add a callback to the list of callbacks. If the specified callback is
     * already registered no action is performed.
     * 
     * @param callback
     *            The callback object to be notified by {@link invokeCallbacks}.
     * @param priority
     *            The priority for this callback where a higher numerical value
     *            indicate a higher priority.
     * @throws ClassCastException
     *             If the <code>callback</code> type is not valid for this list.
     */
    public synchronized void addCallback(Object callback, int priority)
    {
        // Verify callback type is valid
        if (!callbackClass.isInstance(callback)) throw new ClassCastException("Callback not a " + callbackClass);

        // Wrap the callback and priority
        PrioritizedCallback pcb = new PrioritizedCallback(callback, priority);

        // Find the insertion point
        int insertionPoint = Collections.binarySearch(callbackList, pcb);

        // Ignore the request if the callback is already in the list.
        // Otherwise, add the callback to the list.
        if (insertionPoint < 0) callbackList.add(-insertionPoint - 1, pcb);
    }

    /**
     * Remove a callback from the list of callbacks. If the specified callback
     * is not currently registered no action is performed.
     * 
     * @param callback
     *            The callback to be removed
     */
    public synchronized void removeCallback(Object callback)
    {
        PrioritizedCallback pcb = new PrioritizedCallback(callback, 0);
        callbackList.removeElement(pcb);
    }

    /**
     * Invoke the specified notification method on all registered callbacks.
     * 
     * @param method
     *            The notification method to be invoked
     * @param arguments
     *            Arguments to pass to notification method
     * @throws IllegalAccessException
     *             If <code>method</code> is inaccessible
     * @throws InvocationTargetException
     *             If <code>method</code> throws an exception
     */
    public void invokeCallbacks(Method method, Object[] arguments) throws IllegalAccessException,
            InvocationTargetException
    {
        PrioritizedCallback cbList[];

        synchronized (this)
        {
            // Make a copy of the callback list so we can notify without holding
            // a lock on
            // the list.
            cbList = (PrioritizedCallback[]) callbackList.toArray(new PrioritizedCallback[0]);
        }

        // Notify each callback
        for (int i = 0; i < cbList.length; ++i)
        {
            Object cb = cbList[i].callback;
            method.invoke(cb, arguments);
        }
    }

    /**
     * Wraps a callback with a priority.
     */
    private static class PrioritizedCallback implements Comparable
    {
        private final Object callback;

        private final int priority;

        // Constructor
        public PrioritizedCallback(Object callback, int priority)
        {
            this.callback = callback;
            this.priority = priority;
        }

        // Equals does not consider priority and only returns true if the
        // objects
        // being compared are the same instance.
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;
            return callback == ((PrioritizedCallback) obj).callback;
        }

        // Defer to callback object for hash code
        public int hashCode()
        {
            return callback.hashCode();
        }

        // The natural ordering is descending from higher to lower priorities
        // where
        // a higher priority is a greater numerical value.
        public int compareTo(Object obj)
        {
            return ((PrioritizedCallback) obj).priority - priority;
        }
    }
}
