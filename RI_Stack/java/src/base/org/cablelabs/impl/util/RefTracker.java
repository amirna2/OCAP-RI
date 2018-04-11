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

import org.apache.log4j.Logger;

/**
 * This class is useful for debugging and verification purposes.
 * <p>
 * This utility class can be used to keep an eye out for the Garbage Collection
 * of certain objects. It depends upon support for the Java2 Reference API (or
 * something similar), and as such may not be available on all platforms. If not
 * available, then the instance returned by {@link #getInstance} will be a dummy
 * implementation.
 * <p>
 * The simplest use is as follows:
 * 
 * <pre>
 * RefTracker.{@link #getInstance() getInstance()}.{@link #track(Object) track(someObject)}
 * </pre>
 * 
 * When all references to <code>someObject</code> are cleared and it becomes (at
 * least) <i>weakly</i> reachable then a message will be written to the
 * <code>"REFTRACKER"</code> Log4J category.
 * 
 * <i>Perhaps it would have a better home in
 * <code>org.cablelabs.impl.debug</code>?</i>
 * 
 * @author Aaron Kamienski
 */
public abstract class RefTracker
{
    /**
     * Returns the singleton <code>RefTracker</code> supported by the current
     * platform.
     * 
     * @param the
     *            singleton <code>RefTracker</code>
     */
    public static RefTracker getInstance()
    {
        return singleton;
    }

    /**
     * Tracks the given <code>Object</code> with this <code>RefTracker</code>
     * When the <code>Object</code> is garbage collected, then a Log4J message
     * will be printed.
     * 
     * @param obj
     *            the <code>Object</code> to track.
     */
    public abstract void track(Object obj);

    /**
     * Singleton instance.
     */
    private static final RefTracker singleton;
    static
    {
        if (!JavaVersion.JAVA_2)
            singleton = new RefTracker0();
        else
        {
            try
            {
                Class cl = Class.forName(RefTracker.class.getName() + "2");
                singleton = (RefTracker) cl.newInstance();
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(e);
                throw new LinkageError("Could not find/create RefTracker impl");
            }
        }
    }

    private static final Logger log = Logger.getLogger(RefTracker.class);
}
