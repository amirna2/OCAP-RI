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

package org.cablelabs.impl.manager;

import org.dvb.application.AppID;
import org.ocap.system.event.ResourceDepletionEvent;

/**
 * The purpose of the <code>ResourceReclamationManager</code> is to implement a
 * subset of the overall <i>resource reclamation</i> policy for the OCAP stack.
 * In particular it is responsible for implementing support for requested
 * resource reclamations within the Java space. This includes:
 * <ul>
 * <li>Forced garbage collection and finalization cycles as a form of
 * non-destructive resource reclamation.
 * <li>Allowing the <i>Monitor Application</i> to be notified of low resource
 * situations via {@link ResourceDepletionEvent} delivery, giving it the chance
 * to reclaim resources.
 * <li>Forced destruction of lower-priority applications in an attempt to make
 * additional resources available.
 * </ul>
 * 
 * @author Aaron Kamienski
 */
public interface ResourceReclamationManager extends Manager
{
    // Currently, no externally accessible methods are exposed via this
    // interface
    // This may change. Some possibilities include:
    // * Allowing registration of callback routines for cache cleanup.
    // * Allowing instigation of resource reclamation.
    // * Retrieval of statistics.

    /**
     * This utility class can be used to manipulate an application's <i>context
     * identifier</i>. It is meant to be used within the context of OCAP
     * resource reclamation.
     * 
     * @author Aaron Kamienski
     */
    public static class ContextID
    {
        /**
         * Create a <i>context id</i> for the application specified by the given
         * parameters.
         * 
         * @param id
         *            the <code>AppID</code> for the given app or
         *            <code>null</code>
         * @param priority
         *            the application priority
         * @return a <i>context id</i> for the given parameters; <code>0L</code>
         *         is returned if <i>id</i> is <code>null</code>
         */
        public static long create(AppID id, int priority)
        {
            if (id == null) return 0L;

            return id.getAID() | (((long) id.getOID() & 0xFFFFFFFF) << 16) | ((long) priority << 48);
        }

        /**
         * Returns the <code>AppID</code> encoded in the given <i>contextId</i>.
         * 
         * @param contextId
         *            the given <i>contextId</i>
         * @return the <code>AppID</code> encoded in the given <i>contextId</i>;
         *         if the <i>contextId</i> is zero, then <code>AppID(0,0)</code>
         *         is still returned
         */
        public static AppID getAppID(long contextId)
        {
            int OID = (int) (contextId >> 16);
            int AID = (int) (contextId & 0xFFFF);

            return new AppID(OID, AID);
        }

        /**
         * Returns the <i>priority</i> encoded in the given <i>contextId</i>.
         * 
         * @param contextId
         *            the given <i>contextId</i>
         * @return the <i>priority</i> encoded in the given <i>contextId</i>; if
         *         the <i>contextId</i> is zero, then a priority of zero is
         *         still returned
         */
        public static int getPriority(long contextId)
        {
            return (short) (contextId >>> 48);
        }

        /**
         * Sets the <i>context id</i> for the {@link Thread#currentThread()
         * current thread}.
         * <p>
         * The <i>context id</i> for all threads defaults to zero unless
         * explicitly set.
         * <p>
         * This would generally be used as follows:
         * 
         * <pre>
         * long oldId = ContextID.set(newId);
         * try
         * {
         *     //...
         * }
         * finally
         * {
         *     ContextID.set(oldId);
         * }
         * </pre>
         * <p>
         * This method is not intended for general-purpose use, but is intended
         * for use by the {@link CallerContextManager} and {@link CallerContext}
         * implementations.
         * 
         * @param contextId
         * @return the previous <i>context id</i> for the current thread
         */
        public static long set(long contextId)
        {
            return nSet(contextId);
        }

        /**
         * Native method used to implement {@link #set(long)}. Sets the
         * <i>context id</i> for the current thread.
         * 
         * @param contextId
         *            the new contextId
         * @return the old contextId
         */
        private static native long nSet(long contextId);
    }
}
