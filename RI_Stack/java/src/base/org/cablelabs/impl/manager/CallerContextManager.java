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

/**
 * A <code>Manager</code> that is used to retrieve a <code>CallerContext</code>
 * object, corresponding to the current logical VM.
 * <p>
 * The <code>CallerContextManager</code> implementation works in concert with
 * the <code>ApplicationManager</code> implementation. Where the
 * <code>ApplicationManager</code> provides methods for creating and controlling
 * applications, the <code>CallerContextManager</code> is used to work with the
 * calling context (i.e., the application). The intention is to separate the
 * <i>logical VM</i> idea (embodied in the <code>CallerContext</code>) from the
 * actual application management. Where areas within the implementation need to
 * access the <i>logical VM</i> (i.e., the caller's context), they don't need
 * access to the various APIs provided by the <code>ApplicationManager</code>
 * for application management.
 * 
 * <p>
 * Example usage follows:
 * 
 * <pre>
 * CallerContextManager cmgr = ManagerManager.getManager(CallerContextManager.class);
 * 
 * CallerContext ctx = cmgr.getCurrentContext();
 * </pre>
 * 
 * @see CallerContext
 * @see CallbackData
 * @see ApplicationManager
 * @see ManagerManager
 */
public interface CallerContextManager extends Manager
{
    /**
     * Gets the current <code>CallerContext</code>, representing the current
     * logical VM. Care must be taken to ensure that the intended thread context
     * is where the call is made from.
     * 
     * @return the current <code>CallerContext</code> as determined by current
     *         thread of execution
     */
    CallerContext getCurrentContext();

    /**
     * Gets the <i>system</i> <code>CallerContext</code>, representing the
     * logical VM for the system implementation. This is provided as a
     * convenience to those <code>Manager</code>s which may be called from
     * non-application contexts.
     * <p>
     * When the calling context <i>is</i> an application context, then:
     * 
     * <pre>
     *     getCurrentContext() != getSystemContext();
     * </pre>
     * 
     * When the calling context is <i>not</i> an application context, then:
     * 
     * <pre>
     *     getCurrentContext() == getSystemContext();
     * </pre>
     * 
     * @return the <i>system</i> <code>CallerContext</code> as would be returned
     *         by {@link #getCurrentContext} if the current thread of execution
     *         was part of the <i>system</i> context
     */
    CallerContext getSystemContext();
}
