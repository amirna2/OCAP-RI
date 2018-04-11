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

/**
 * 
 */
package org.cablelabs.impl.media.player;

/**
 * This interface defines synchronous callback methods that are invoked by JMF
 * to notify the implementing class when a player starts and stops decode
 * sessions. A <code>SessionChangeCallback</code> is registered with a player
 * via the {@link ServicePlayer#addSessionChangeCallback(SessionChangeCallback)}
 * method and unregistered via the
 * {@link ServicePlayer#removeSessionChangeCallback(SessionChangeCallback)}
 * method. If multiple {@link SessionChangeCallback}s are registered, their
 * callback methods are invoked in the order in which the callbacks were
 * registered. Callbacks are invoked on the system caller context; the
 * implementation does not maintain caller context-specific callback lists.
 * Unchecked exceptions thrown by these methods will be caught and logged.
 * 
 * @author Michael Schoonover
 */
public interface SessionChangeCallback
{
    /**
     * Indicates that the player is stopping a decode session. It is called
     * immediately before the player stops the session.
     * 
     * @param sessionHandle
     *            - the native handle of the decode session being stopped.
     */
    void notifyStoppingSession(int sessionHandle);

    /**
     * Indicates that the player is attempting to start a decode session. It is
     * called immediately after the player has successfully initiated a decoding
     * session but hasn't yet received asynchronous notification as to whether
     * it was able to complete startup.
     * 
     * @param sessionHandle
     *            - the native handle of the decode session that is being
     *            started.
     */
    void notifyStartingSession(int sessionHandle);

    /**
     * Indicates whether the pending decode session completed successfully. It
     * is called as soon as the player knows the status of the pending
     * selection. This is the same time that the player sends a
     * {@link NormalMediaPresentationEvent} or
     * {@link AlternativeMediaPresentationEvent}.
     * 
     * @param sessionHandle
     *            - the native handle of the decode session that started.
     * @param succeeded
     *            - <code>true</code> means the session started;
     *            <code>false</code>, that it failed.
     */
    void notifySessionComplete(int sessionHandle, boolean succeeded);
}
