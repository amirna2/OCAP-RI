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

import org.cablelabs.impl.manager.CallerContext;

/**
 * <code>TaskQueue</code> defines an interface for a simple task execution
 * queue. Tasks (represented by {@link Runnable}) objects may be posted to a
 * <code>TaskQueue</code> for subsequent execution.
 * <p>
 * For example:
 * 
 * <pre>
 * TaskQueue q = ...;
 * 
 * q.post(new Runnable() {
 *     public void run()
 *     {
 *         // code to execute ...
 *     }
 * });
 * </pre>
 * 
 * How a <code>TaskQueue</code> executes its code is not really defined here --
 * instead just the interface. The exact type of <code>TaskQueue</code> used
 * will define how each task is executed. For example, subsequent tasks may
 * execute only after the previously posted task is finished. However, another
 * type of <code>TaskQueue</code> could execute tasks in parallel.
 * 
 * @author Aaron Kamienski
 * 
 * @see CallerContext#createTaskQueue
 */
public interface TaskQueue
{
    /**
     * Post the given <code>Runnable</code> <i>task</i> for execution.
     * 
     * @param task
     *            the task to execute
     * @throws IllegalStateException
     *             if this queue has been {@link #dispose disposed}
     */
    public void post(Runnable task) throws IllegalStateException;

    /**
     * Disposes this task queue, allowing any necessary resources to be
     * released. This should be invoked when the user or owner of the
     * <code>TaskQueue</code> is finished with it and no longer needs it. After
     * a <code>TaskQueue</code> has been disposed, any subsequent {@link #post
     * posts} will throw an <code>IllegalStateException</code>.
     */
    public void dispose();
}
