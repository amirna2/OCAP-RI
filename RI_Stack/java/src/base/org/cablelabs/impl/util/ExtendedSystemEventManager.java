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

import org.ocap.system.event.SystemEvent;
import org.ocap.system.event.SystemEventListener;
import org.ocap.system.event.SystemEventManager;

/**
 * Implementation-specific extension to <code>SystemEventManager</code>. This
 * provides additional methods that are used within the implementation.
 * 
 * @author Aaron Kamienski
 * 
 * @see SystemEventManager
 */
public interface ExtendedSystemEventManager
{
    /**
     * Indicates that no listener was installed for the given event.
     * 
     * @see #log(SystemEvent, long)
     */
    public static final int NOLISTENER = 0;

    /**
     * Indicates that a listener was invoked successfully within the given
     * timeout period.
     * 
     * @see #log(SystemEvent, long)
     */
    public static final int SUCCESS = 1;

    /**
     * Indicates that listener invocation did not return within the given
     * timeout period.
     * 
     * @see #log(SystemEvent, long)
     */
    public static final int TIMEOUT = 2;

    /**
     * A version of {@link SystemEventManager#log} that synchronously invokes
     * the appropriate listener's
     * {@link SystemEventListener#notifyEvent(SystemEvent)} method and blocks
     * until it returns or the timeout period passes.
     * 
     * @param event
     *            the event to log
     * @param timeout
     *            the timeout period in milliseconds
     * 
     * @return the status of the invocation; will be one of {@link #NOLISTENER},
     *         {@link #SUCCESS}, or {@link #TIMEOUT},
     * 
     * @throws java.lang.IllegalArgumentException
     *             if the event parameter is an instance of an application
     *             defined class (i.e., applications cannot define their own
     *             subclasses of SystemEvent and use them with this method. This
     *             is due to implementation and security issues).
     */
    public int log(SystemEvent event, long timeout) throws IllegalArgumentException;
}
