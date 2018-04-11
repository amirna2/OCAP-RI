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
 * Manager callback interface for caller state changes. Manager code will store
 * one of these with a {@link CallerContext} whenever it wants to receive state
 * change notification for an entity using a manager's related API.
 * <p>
 * A <i>toy</i> example using an anonymous inner class follows:
 * 
 * <pre>
 * CallerContextManager cmgr = ManagerManager.getManager(CallerContextManager.class);
 * CallerContext ctx = cmgr.getCallerContext();
 * final Object data = resourceData;
 * ctx.addCallbackData(new CallbackData()
 * {
 *     public void destroy(CallerContext ctx)
 *     {
 *         revokeResources(ctx, data);
 *         ctx.removeCallbackData(this);
 *     }
 * 
 *     public void pause(CallerContext ctx)
 *     {
 *     }
 * 
 *     public void active(CallerContext ctx)
 *     {
 *     }
 * });
 * </pre>
 * 
 * @see CallerContext
 * @see CallerContextManager
 */
public interface CallbackData
{
    /**
     * Notifies the manager that the given <code>CallerContext</code> has been
     * destroyed.
     * 
     * @param callerContext
     *            the affected caller
     */
    void destroy(CallerContext callerContext);

    /**
     * Notifies the manager that the given <code>CallerContext</code> has been
     * paused.
     * 
     * @param callerContext
     *            the affected caller
     */
    void pause(CallerContext callerContext);

    /**
     * Notifies the manager that the given <code>CallerContext</code> has been
     * made active.
     * 
     * @param callerContext
     *            the affected caller
     */
    void active(CallerContext callerContext);

    /**
     * Provides a simple <code>CallbackData</code> implementation that simply
     * stores a single piece of data and removes itself when {@link #destroy}is
     * called.
     */
    public static class SimpleData implements CallbackData
    {
        /**
         * Creates a new <code>SimpleData</code> object with the associated
         * data.
         */
        public SimpleData(Object data)
        {
            this.data = data;
        }

        /**
         * Returns the data assigned to this <code>CallbackData</code>.
         */
        public Object getData()
        {
            return data;
        }

        /**
         * Assigns the given data to this <code>CallbackData</code>.
         */
        public final void setData(Object data)
        {
            this.data = data;
        }

        /**
         * Simply removes this data from the <code>CallerContext</code>.
         */
        public void destroy(CallerContext callerContext)
        {
            callerContext.removeCallbackData(this);
        }

        public void pause(CallerContext callerContext)
        { /* empty */
        }

        public void active(CallerContext callerContext)
        { /* empty */
        }

        /**
         * The <i>data</i> associated with this <code>SimpleData</code>.
         */
        private Object data;
    }
}
