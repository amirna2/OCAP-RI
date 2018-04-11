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

package org.cablelabs.impl.manager.reclaim;

/**
 * Abstract interface to be implemented by registered <i>Resource
 * Reclamation</i> callbacks.
 * 
 * @author Aaron Kamienski
 */
interface Callback
{
    /**
     * Constant value indicating that reclamation of <i>Java Heap</i> memory is
     * desired to satisfy a current request or to resolve a low-memory
     * situation.
     */
    public static final int TYPE_JAVA = 0;

    /**
     * Constant value indicating that reclamation <i>System</i> memory is
     * desired to satisfy a current request or to resolve a low-memory
     * situation.
     */
    public static final int TYPE_SYSTEM = 1;

    /**
     * Indicates that a memory allocation failure has <i>already</i> occurred
     * for the given <i>type</i>.
     */
    public static final int REASON_FAILURE = 0;

    /**
     * Indicates that available free memory (or overall memory in use) of the
     * given <i>type</i> has fallen below (or grown above) a specific threshold.
     */
    public static final int REASON_THRESHOLD = 1;

    /**
     * The implementation of this method should release applicable resources as
     * appropriate. This method should be invoked until <code>false</code> is
     * returned to release the most amount of memory.
     * 
     * @param type
     *            one of {@link #TYPE_JAVA} or {@link #TYPE_SYSTEM} indicating
     *            the <i>type</i> of memory that is needed
     * @param reason
     *            one of {@link #REASON_FAILURE} or {@link #REASON_THRESHOLD}
     *            indicating the reason for the release request
     * @param contextId
     *            identifies the application context that (implicitly) initiated
     *            the resource reclamation request; zero if not specific to an
     *            application
     * @return <code>false</code> if no resources could be released;
     *         <code>true</code> if resources could be released and there is the
     *         possibility that more could be released yet
     */
    public boolean releaseResources(int type, int reason, long contextId);
}
