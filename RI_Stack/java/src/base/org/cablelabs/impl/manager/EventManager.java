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

import java.awt.AWTEvent;

/**
 * A <code>Manager</code> that provides the system's event management
 * functionality. The <code>EventManager</code> implementation is used to
 * provide access to MHP/OCAP's {@link org.ocap.event.EventManager EventManager}
 * object.
 * 
 * @see CallerContextManager
 * @see ManagerManager
 * @see org.ocap.event
 * @see org.dvb.event
 * 
 * @author Aaron Kamienski
 */
public interface EventManager extends Manager, org.cablelabs.impl.awt.EventDispatcher
{
    /**
     * Returns an instance of the <code>EventManager</code> that may be be used
     * to control event dispatch as provided by the API. A different instance
     * may be returned for each <i>CallerContext</i>. I.e., the returned
     * <code>EventManager</code> may simply be a wrapper/proxy object that
     * references the original singleton instance or it may be the singleton
     * instance itself.
     * 
     * @returns org.ocap.event.EventManager instance
     */
    public org.ocap.event.EventManager getEventManager();

    /**
     * Dispatches the given <code>AWTEvent</code> according to the decision tree
     * shown in Annex K of the OCAP-1.0 specification. This handles sending the
     * event to the <i>MonitorApp</i> for filtering, exclusive access dispatch
     * of AWT events, focused application dispatch of AWT events, and dispatch
     * of {@link org.dvb.event.UserEvent}s.
     * <p>
     * This method should do the following:
     * <ol>
     * <li>Send event to registered {@link org.ocap.event.UserEventFilter
     * filters}.
     * <li>If no application has registered exclusive access then:
     * <ol>
     * <li>The event is sent to the currently focused application component via
     * the application's event queue.
     * <li>The event is sent to applications registered through the
     * EventRepository for shared access.
     * </ol>
     * <li>If an application has registered exclusive access to this event:
     * <ol>
     * <li>If access was registered through
     * {@link org.ocap.event.EventManager#addExclusiveAccessToAWTEvent}, then
     * the event is posted to the application's event queue (to be sent to the
     * currently focused component).
     * <li>Otherwise the event is dispatched via the registered
     * {@link org.dvb.event.UserEventListener}.
     * </ol>
     * </ol>
     * <p>
     * This method needs access to the application-specific event queue.
     * 
     * @see "OCAP-1.0 Annex K - OCAP User Event Input API"
     * @see org.dvb.event
     * @see org.ocap.event
     */
    public void dispatch(AWTEvent e);
}
