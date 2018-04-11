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

/*
 * Created on Sep 7, 2006
 */
package org.cablelabs.impl.java;

/**
 * This abstract base class should be extended to implement resource reclamation
 * procedures that should be applied before throwing
 * <code>OutOfMemoryError</code>. A subclass should override
 * {@link #reclaimMemory(int, long)} to implement the resource reclamation
 * process.
 * 
 * @author Aaron Kamienski
 */
public abstract class ReclaimThread extends Thread
{
    /**
     * This method should be overridden by a subclass to perform and manage the
     * resource reclamation process.
     * <p>
     * This method will be invoked (generally from native code) as long as heap
     * allocations continue to fail and this method continues to return
     * non-zero. The return value from a previous call (if non-zero) will be
     * used as input as the <i>level</i> parameter on the next call.
     * 
     * @param level
     *            the current escalation level for reclamation; the lowest
     *            escalation level is 1
     * @param contextId
     *            the identifies the requesting context
     * @return the next escalation level, or zero if no more work is possible
     */
    public int reclaimMemory(int level, long contextId)
    {
        return 0;
    }

    /**
     * Creates an instance of <code>ReclaimThread</code>. Meant to be invoked by
     * a subclass.
     */
    protected ReclaimThread()
    {
        super(getSystemGroup(), "Reclaim");
        setDaemon(true);
    }

    /**
     * This is what is run when the thread starts up.
     */
    public final void run()
    {
        // The bulk of the work is done by perform(), which is a native
        // method. This is not really expected to come back, except when the
        // VM is dying. Any javaland preparatory work should be done prior
        // to the call.
        perform();
    }

    /**
     * This does the main part of the Resource Reclamation work.
     */
    private final native void perform();

    /**
     * Find out what the system group is.
     */
    private static final native ThreadGroup getSystemGroup();
}
