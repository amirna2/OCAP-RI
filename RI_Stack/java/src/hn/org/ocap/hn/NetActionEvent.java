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

package org.ocap.hn;

/**
 * This class represents an event generated in response to an action.
 * NetActionEvent instances can only be created by the implementation.
 */
public class NetActionEvent extends java.util.EventObject
{
    /**
     * Action status for a completed action
     *
     * @see #getActionStatus()
     */
    public static final int ACTION_COMPLETED = 0;

    /**
     * <code>ACTION_CANCELED</code> is returned by {@link #getActionStatus()}
     * when the action has been canceled.
     *
     * @see #getActionStatus()
     */
    public static final int ACTION_CANCELED = ACTION_COMPLETED + 1;

    /**
     * <code>ACTION_FAILED</code> is returned by {@link #getActionStatus()} when
     * the action has failed.
     *
     * @see #getActionStatus()
     */
    public static final int ACTION_FAILED = ACTION_CANCELED + 1;

    /**
     * <code>ACTION_STATUS_NOT_AVAILABLE</code> is returned by
     * {@link #getActionStatus()} when the transaction has completed
     * successfully or failed sometime before this method was called and the
     * implementation is no longer maintaining a status for it.
     */
    public static final int ACTION_STATUS_NOT_AVAILABLE = ACTION_FAILED + 1;

    /**
     * <code>ACTION_IN_PROGRESS</code> is returned by {@link #getActionStatus()}
     * when the action is currently on going.
     *
     * @see #getActionStatus()
     */
    public static final int ACTION_IN_PROGRESS = ACTION_STATUS_NOT_AVAILABLE + 1;

    /**
     * Two argument constructor.
     *
     * @param request
     *            - NetActionRequest that instigated the response.
     * @param response
     *            - An object representing the response to the action and which
     *            is specific to the action.
     * @param error
     *            - error code for this event if action failed
     * @param status
     *            - status of the associated net action
     *
     */
    protected NetActionEvent(java.lang.Object request, java.lang.Object response, int error, int status)
    {
        super(request);
    }

    /**
     * Returns the response of the Action. Object is dependent on the Action.
     *
     * @return The response to an asynchronous action.
     */
    public Object getResponse()
    {
        return null;
    }

    /**
     * Returns the ActionRequest which identifies the action instance.
     *
     * @return the NetActionRequest
     */
    public NetActionRequest getActionRequest()
    {
        return null;
    }

    /**
     * Returns the status of the requested action.
     *
     * @return the status of the action; for possible return values see ACTION_*
     *         constants in this class.
     *
     */
    public int getActionStatus()
    {
        return 0;
    }

    /**
     * Gets the error value when getActionStatus returns
     * <code>NetActionEvent.ACTION_FAILED</code>. If the action is not in error
     * this method SHALL return -1. Error code values are dependent on the
     * underlying network protocol error code values.
     *
     * @return The error value; -1 if no error.
     */
    public int getError()
    {
        return -1;
    }
}
