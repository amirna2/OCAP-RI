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

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A <i>wrapper</i> or <i>decorator</i> for <code>Callback</code>
 * implementations that prevent recursive execution of the wrapped
 * <code>Callback</code>.
 * 
 * @author Aaron Kamienski
 */
class NonRecursiveWrapper implements Callback
{
    /**
     * Creates an instance of NonRecursiveWrapper that wraps the given
     * <code>Callback</code>.
     * 
     * @param callback
     *            <code>Callback</code> to wrap
     * 
     * @see #releaseResources(int, int, long)
     */
    public NonRecursiveWrapper(Callback callback)
    {
        this.callback = callback;
    }

    /**
     * Invokes {@link Callback#releaseResources(int, int, long)} on the
     * <i>wrapped</i> <code>Callback</code>, preventing a recursive call. If
     * <code>Callback.releaseResources()</code> has already been invoked (i.e.,
     * this execution is recursive), then <code>false</code> will be returned
     * and the wrapped <code>Callback</code> will <b>not</b> be invoked.
     * <p>
     * This implementation also ensures that the callback executes in a
     * privileged execution block.
     * 
     * @param type
     *            type passed to wrapped <code>Callback</code>
     * @param reason
     *            reason passed to wrapped <code>Callback</code>
     * @param contextId
     *            contet identifier passed to wrapped <code>Callback</code>
     * @return <code>false</code> if invocation is recursive;
     *         <code>Callback.releaseResources()</code> otherwise
     * 
     * @see org.cablelabs.impl.manager.reclaim.Callback#releaseResources(int,
     *      int, long)
     */
    public boolean releaseResources(final int type, final int reason, final long contextId)
    {
        if (enter()) return false;
        try
        {
            return ((Boolean) AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    return new Boolean(callback.releaseResources(type, reason, contextId));
                }
            })).booleanValue();
        }
        finally
        {
            exit();
        }
    }

    /**
     * Determines whether {@link #releaseResources} has already been entered on
     * this <code>Thread</code> of execution. If not, entry is registered so
     * that subsequent calls on the same <code>Thread</code> will indicate that
     * <code>releaseResources()</code> has already been entered.
     * <p>
     * This and {@link #exit} should be used by following this idiom:
     * 
     * <pre>
     * if (enter()) return;
     * try
     * {
     *     // do stuff
     * }
     * finally
     * {
     *     exit();
     * }
     * </pre>
     * 
     * @return <code>true</code> if this invocation of
     *         <code>releaseResources()</code> if recursive; <code>false</code>
     *         indicates non-recursive
     * 
     * @see #releaseResources(int, int, long)
     * @see #exit
     */
    private boolean enter()
    {
        Object obj = tl.get();
        if (obj != null) return true;
        tl.set(getClass());
        return false;
    }

    /**
     * Clears recursive status set by {@link #enter}. Should only be called if
     * <code>enter()</code> returned <code>false</code>.
     * 
     * @see #releaseResources(int, int, long)
     * @see #enter
     */
    private void exit()
    {
        tl.set(null);
    }

    /** ThreadLocal used to avoid a recursive call. */
    private final ThreadLocal tl = new ThreadLocal();

    /** The wrapped callback. */
    private final Callback callback;
}
