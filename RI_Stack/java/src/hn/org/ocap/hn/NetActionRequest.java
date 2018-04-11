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
 * Created on 28-Oct-2004 byDr. Immo Benjes <immo.benjes@philips.com>,
 * Philips Digital Systems Labs, Redhill, UK
 *
 */
package org.ocap.hn;

/**
 * All asynchronous actions in the Home networking API return an
 * NetActionRequest. The NetActionRequest can be used a) to cancel any pending
 * action or b) to identify which Action got completed.
 *
 * @see NetActionHandler
 * @see NetActionEvent
 */
public interface NetActionRequest
{

    /**
     * Cancels the Action associated with this ActionRequest. Returns false if
     * the action can't be canceled.
     *
     * @return false if action can't be canceled, otherwise returns true.
     */
    public boolean cancel();

    /**
     * Gets the progress of the action in percent (0.0 - 1.0). If the progress
     * of an action can't be determined, -1.0 shall be returned.
     *
     * @return the progress of the action (0.0 - 1.0) or -1.0 if the progress
     *         can't be determined.
     *
     */
    public float getProgress();

    /**
     * Gets the current status of the requested action.
     *
     * @return the current action status; see ACTION_* constants in
     *         <code>NetActionEvent</code> for possible return values.
     *
     */
    public int getActionStatus();

    /**
     * Gets the error value when getActionStatus returns
     * <code>NetActionEvent.ACTION_FAILED</code>. The error code returned will
     * be equivalent to the error code returned by
     * {@link NetActionEvent#getError()} for the NetActionEvent associated
     * with the completion of this action request. If the action is not in error
     * or has not completed, this method SHALL return -1.
     *
     * @return The error value; -1 if no error,
     */
    public int getError();
}
